package com.fiskmods.heroes.common.hero.ability;

import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.fiskmods.heroes.common.hero.modifier.ModifierTentacles;
import com.fiskmods.heroes.pack.ScriptFunction;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Server-side ability activation. The client only reports "ability key N was pressed"; all of the
 * validation and the actual effect happen here, so the client cannot cheat abilities.
 */
public final class AbilityHandler
{
    /** Non-toggle modifiers started by a key-down, grouped until their matching key-up. */
    private static final Map<UUID, Map<Integer, List<ModifierEntry>>> HELD_ABILITIES = new HashMap<>();
    private static final Map<UUID, Map<Integer, Set<String>>> SERVER_PRESSED_KEYS = new HashMap<>();
    private static final Map<UUID, Map<Integer, Set<String>>> CLIENT_PRESSED_KEYS = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> ABILITY_COOLDOWNS = new HashMap<>();

    private AbilityHandler()
    {
    }

    public static void onAbilityKey(ServerPlayer player, int index, boolean pressed)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(player);

        if (data == null)
        {
            return;
        }

        HeroIteration iteration = data.getHero();

        if (!pressed)
        {
            release(player, data, index);
            if (index == 0) return;
        }

        if (iteration == null)
        {
            return;
        }

        Hero hero = iteration.getHero();

        if (index == 0)
        {
            if (pressed && !iteration.isMaskDisabled() && hero.hasProperty(player, "MASK_TOGGLE"))
            {
                boolean open = data.getData().get(com.fiskmods.heroes.common.data.var.Vars.MASK_OPEN);
                data.getData().set(com.fiskmods.heroes.common.data.var.Vars.MASK_OPEN, !open);
            }
            return;
        }

        Set<String> keys = hero.getKeyBindsMatching(index);

        if (!pressed)
        {
            return;
        }

        for (String key : keys)
        {
            if (!hero.isKeyBindEnabled(player, key))
            {
                continue;
            }

            setPressedKey(SERVER_PRESSED_KEYS, player, index, key, true);

            ScriptFunction function = hero.getKeyBindFuncs().get(key);

            if (function != null)
            {
                function.call(new com.fiskmods.heroes.pack.js.JSEntity(player), new com.fiskmods.heroes.pack.js.JSManager());
                continue;
            }

            ModifierEntry entry = activate(player, data, hero, key);
            if (entry != null && !entry.getBoolean(player, PowerProperty.IS_TOGGLE))
            {
                HELD_ABILITIES.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                        .computeIfAbsent(index, ignored -> new ArrayList<>()).add(entry);
            }
        }
    }

    /** Runs the default key-down behaviour and returns the started modifier, if any. */
    private static ModifierEntry activate(ServerPlayer player, SHPlayerData data, Hero hero, String key)
    {
        ModifierEntry entry = findModifier(hero, key);

        // Señor Cactus's pack key invokes the separate spike_burst modifier.
        if (entry == null && "SHOOT_SPIKES".equals(key))
        {
            entry = findModifier(hero, "spike_burst");
        }

        // The four Doctor Octopus actions are driven by one power entry. Their key names do not
        // match the modifier id, so route them through the modifier's action dispatcher.
        if (entry == null && key.startsWith("TENTACLE_"))
        {
            entry = findModifier(hero, "tentacles");
        }

        if (entry == null)
        {
            return null;
        }

        if (!entry.isEnabled() || !entry.isModifierEnabled(player, data))
        {
            return null;
        }

        int cooldown = entry.getInt(player, PowerProperty.COOLDOWN_TIME);
        if (cooldown > 0)
        {
            String cooldownKey = entry.getModifier().getId() + ":" + key;
            long now = player.level().getGameTime();
            Map<String, Long> playerCooldowns = ABILITY_COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
            if (playerCooldowns.getOrDefault(cooldownKey, 0L) > now)
            {
                return null;
            }
            playerCooldowns.put(cooldownKey, now + cooldown);
        }

        if (entry.getModifier() instanceof ModifierTentacles tentacles)
        {
            tentacles.activateKey(player, entry, data, key);
            return null;
        }

        entry.getModifier().onActivate(player, entry, data);
        return entry;
    }

    /** Delivers key-up to the exact entries that accepted the corresponding key-down. */
    private static void release(ServerPlayer player, SHPlayerData data, int index)
    {
        clearPressedIndex(SERVER_PRESSED_KEYS, player, index);
        Map<Integer, List<ModifierEntry>> byIndex = HELD_ABILITIES.get(player.getUUID());
        if (byIndex == null) return;

        List<ModifierEntry> entries = byIndex.remove(index);
        if (entries != null)
        {
            for (ModifierEntry entry : entries)
            {
                entry.getModifier().onToggle(player, entry, data);
            }
        }

        if (byIndex.isEmpty()) HELD_ABILITIES.remove(player.getUUID());
    }

    public static void clear(ServerPlayer player)
    {
        SERVER_PRESSED_KEYS.remove(player.getUUID());
        ABILITY_COOLDOWNS.remove(player.getUUID());
        Map<Integer, List<ModifierEntry>> byIndex = HELD_ABILITIES.remove(player.getUUID());
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (byIndex == null || data == null) return;

        for (List<ModifierEntry> entries : byIndex.values())
        {
            for (ModifierEntry entry : entries)
            {
                entry.getModifier().onToggle(player, entry, data);
            }
        }
    }

    /** Drops the local pressed-key snapshot when the worn hero changes. */
    public static void clearClient(Entity entity)
    {
        CLIENT_PRESSED_KEYS.remove(entity.getUUID());
    }

    /** Mirrors the physical key state on the client so local render and update hooks can query it. */
    public static void setClientKeyState(Entity entity, int index, boolean pressed)
    {
        HeroIteration iteration = HeroTracker.getHero(entity);
        if (iteration == null || !pressed)
        {
            clearPressedIndex(CLIENT_PRESSED_KEYS, entity, index);
            return;
        }

        Hero hero = iteration.getHero();
        clearPressedIndex(CLIENT_PRESSED_KEYS, entity, index);
        for (String key : hero.getKeyBindsMatching(index))
        {
            setPressedKey(CLIENT_PRESSED_KEYS, entity, index, key, true);
        }
    }

    /** Resolves whether the named ability key is currently held on this logical side. */
    public static boolean isKeyPressed(Entity entity, String key)
    {
        HeroIteration iteration = HeroTracker.getHero(entity);
        if (iteration == null || !iteration.getHero().hasKeyBind(key)
                || !iteration.getHero().isKeyBindEnabled(entity, key))
        {
            return false;
        }

        Map<UUID, Map<Integer, Set<String>>> states = entity.level().isClientSide ? CLIENT_PRESSED_KEYS : SERVER_PRESSED_KEYS;
        Map<Integer, Set<String>> byIndex = states.get(entity.getUUID());
        if (byIndex == null) return false;

        Set<String> names = byIndex.get(iteration.getHero().getKeyBinding(key));
        return names != null && names.contains(key);
    }

    private static void setPressedKey(Map<UUID, Map<Integer, Set<String>>> states, Entity entity,
            int index, String key, boolean pressed)
    {
        Map<Integer, Set<String>> byIndex = states.computeIfAbsent(entity.getUUID(), ignored -> new HashMap<>());
        Set<String> names = byIndex.computeIfAbsent(index, ignored -> new java.util.HashSet<>());
        if (pressed) names.add(key);
        else names.remove(key);
    }

    private static void clearPressedIndex(Map<UUID, Map<Integer, Set<String>>> states, Entity entity, int index)
    {
        Map<Integer, Set<String>> byIndex = states.get(entity.getUUID());
        if (byIndex == null) return;
        byIndex.remove(index);
        if (byIndex.isEmpty()) states.remove(entity.getUUID());
    }

    /** Finds the modifier of a hero whose {@code key} property matches the ability name. */
    public static ModifierEntry findModifier(Hero hero, String key)
    {
        for (ModifierEntry entry : hero.getPowerContainer().getEntries())
        {
            String property = entry.get(PowerProperty.KEY);

            if (key.equals(property) || key.equalsIgnoreCase(entry.getModifier().getId().getPath()))
            {
                return entry;
            }
        }

        return null;
    }

    /** Whether the hero currently has any ability bound to the given key index. */
    public static boolean hasAbility(Hero hero, int index)
    {
        return hero != null && hero.hasKeyBinding(index);
    }
}
