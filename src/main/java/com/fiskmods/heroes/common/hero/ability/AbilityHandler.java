package com.fiskmods.heroes.common.hero.ability;

import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
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
    /** Aim-gated powers sharing the AIM key are retried until aim settles or the key is released. */
    private static final Map<UUID, Map<Integer, Set<String>>> PENDING_AIM_ABILITIES = new HashMap<>();
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

        if (pressed && player.hasEffect(com.fiskmods.heroes.common.hero.modifier.ModEffects.TUTRIDIUM.get()))
        {
            return;
        }

        HeroIteration iteration = data.getHero();

        if (pressed && isServerIndexPressed(player, index))
        {
            return;
        }

        if (!pressed)
        {
            activatePendingAimAbilities(player, data, iteration, index);
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
        if (pressed)
        {
            com.fiskmods.heroes.FiskHeroes.LOGGER.debug("Ability input from {}: hero={}, index={}, matching={}",
                    player.getGameProfile().getName(), iteration.getRegistryName(), index, keys);
        }
        if (hero.getKeyBinding("AIM") == index)
        {
            updateAiming(player, data, hero, pressed);
        }

        if (!pressed)
        {
            return;
        }

        for (String key : keys)
        {
            // Record the physical press even while a keybind is temporarily gated. Mysterio's
            // ENERGY_PROJECTION shares AIM's key and becomes enabled only after the aim animation
            // settles; a transition-only packet otherwise loses that shot permanently.
            setPressedKey(SERVER_PRESSED_KEYS, player, index, key, true);
            if (!hero.isKeyBindEnabled(player, key))
            {
                if (isAimGatedAbility(hero, index, key))
                {
                    PENDING_AIM_ABILITIES.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                            .computeIfAbsent(index, ignored -> new java.util.LinkedHashSet<>()).add(key);
                }
                continue;
            }

            ScriptFunction function = hero.getKeyBindFuncs().get(key);

            if (function != null)
            {
                function.call(new com.fiskmods.heroes.pack.js.JSEntity(player), new com.fiskmods.heroes.pack.js.JSManager());
                continue;
            }

            if ("GUN_RELOAD".equals(key)
                    && player.getMainHandItem().getItem() instanceof com.fiskmods.heroes.common.item.ItemGun gun)
            {
                gun.reload(player, hero);
                continue;
            }

            if ("QUIVER_CYCLE".equals(key)
                    && player.getMainHandItem().is(com.fiskmods.heroes.common.item.ModItems.COMPOUND_BOW.get())
                    && !com.fiskmods.heroes.common.item.ItemQuiver.findQuiver(player).isEmpty())
            {
                int selected = Byte.toUnsignedInt(data.getData().get(Vars.SELECTED_ARROW)) % 5;
                data.getData().set(Vars.SELECTED_ARROW, (byte) ((selected + 1) % 5));
                com.fiskmods.heroes.common.item.ItemQuiver.updatePlayerData(player);
                player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 1.2F);
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

    /** Retries an aim-gated shared-key ability after the aim state has had time to settle. */
    public static void tickHeldAbilities(ServerPlayer player)
    {
        Map<Integer, Set<String>> byIndex = PENDING_AIM_ABILITIES.get(player.getUUID());
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        HeroIteration iteration = data != null ? data.getHero() : null;
        if (byIndex == null) return;
        if (iteration == null)
        {
            PENDING_AIM_ABILITIES.remove(player.getUUID());
            return;
        }

        Hero hero = iteration.getHero();
        for (Map.Entry<Integer, Set<String>> pending : new HashMap<>(byIndex).entrySet())
        {
            for (String key : new java.util.ArrayList<>(pending.getValue()))
            {
                if (!hero.isKeyBindEnabled(player, key)) continue;
                pending.getValue().remove(key);
                startAbility(player, data, hero, pending.getKey(), key);
            }
            if (pending.getValue().isEmpty()) byIndex.remove(pending.getKey());
        }
        if (byIndex.isEmpty()) PENDING_AIM_ABILITIES.remove(player.getUUID());
    }

    /** A short HUD click should still perform the shared-key shot when aim has not settled yet. */
    private static void activatePendingAimAbilities(ServerPlayer player, SHPlayerData data,
            HeroIteration iteration, int index)
    {
        Map<Integer, Set<String>> byIndex = PENDING_AIM_ABILITIES.get(player.getUUID());
        Set<String> pending = byIndex != null ? byIndex.remove(index) : null;
        if (byIndex != null && byIndex.isEmpty()) PENDING_AIM_ABILITIES.remove(player.getUUID());
        if (pending == null || iteration == null
                || player.hasEffect(com.fiskmods.heroes.common.hero.modifier.ModEffects.TUTRIDIUM.get())) return;
        Hero hero = iteration.getHero();
        for (String key : pending) startAbility(player, data, hero, index, key);
    }

    private static void startAbility(ServerPlayer player, SHPlayerData data, Hero hero, int index, String key)
    {
        ModifierEntry entry = activate(player, data, hero, key);
        if (entry != null && !entry.getBoolean(player, PowerProperty.IS_TOGGLE))
        {
            HELD_ABILITIES.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                    .computeIfAbsent(index, ignored -> new ArrayList<>()).add(entry);
        }
    }

    private static boolean isAimGatedAbility(Hero hero, int index, String key)
    {
        return "ENERGY_PROJECTION".equals(key) && hero.getKeyBinding("AIM") == index;
    }

    private static boolean isServerIndexPressed(ServerPlayer player, int index)
    {
        Map<Integer, Set<String>> byIndex = SERVER_PRESSED_KEYS.get(player.getUUID());
        return byIndex != null && byIndex.containsKey(index);
    }

    /** Runs the default key-down behaviour and returns the started modifier, if any. */
    private static ModifierEntry activate(ServerPlayer player, SHPlayerData data, Hero hero, String key)
    {
        String modifierId = switch (key)
        {
            case "CHARGE_ENERGY" -> "energy_manipulation";
            case "CHARGE_ICE" -> "cryo_charge";
            case "SHOOT_SPIKES" -> "spike_burst";
            case "TELEPORT" -> "teleportation";
            case "MINIATURIZE_SUIT", "SIZE_MANIPULATION" -> "size_manipulation";
            default -> key.startsWith("TENTACLE_") ? "tentacles" : null;
        };
        boolean miniaturizeOnly = "MINIATURIZE_SUIT".equals(key);
        ModifierEntry entry = findEnabledModifier(hero, key, modifierId, miniaturizeOnly, player, data);

        if (entry == null) return null;

        int cooldown = entry.getInt(player, PowerProperty.COOLDOWN_TIME);
        if (cooldown > 0)
        {
            String cooldownKey = entry.getCard() + ":" + key;
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
            // Strike charge is completed by ModifierTentacles.onToggle when this key is released.
            return "TENTACLE_STRIKE".equals(key) ? entry : null;
        }

        entry.getModifier().onActivate(player, entry, data);
        return entry;
    }

    /** Resolves a pack key against enabled modifier cards, including powers with multiple cards. */
    private static ModifierEntry findEnabledModifier(Hero hero, String key, String modifierId,
            boolean miniaturizeOnly, ServerPlayer player, SHPlayerData data)
    {
        for (ModifierEntry entry : hero.getPowerContainer().getEntries())
        {
            String path = entry.getModifier().getId().getPath();
            // Some pack-defined keys are expressed through the entity's script state. Resolve
            // them for this player instead of comparing only the literal/default value.
            String configuredKey = entry.get(player, PowerProperty.KEY);
            boolean matches = key.equals(configuredKey)
                    || sameAbilityName(key, configuredKey)
                    || sameAbilityName(key, path)
                    || modifierId != null && sameAbilityName(modifierId, path);
            if (!matches) continue;

            if (miniaturizeOnly && entry.getCard() != null && !entry.getCard().endsWith("|small")) continue;
            if (entry.isEnabled() && entry.isModifierEnabled(player, data)) return entry;
        }
        return null;
    }

    /** Compares pack key names such as SHADOWDOME with modifier ids such as shadow_dome. */
    private static boolean sameAbilityName(String first, String second)
    {
        return first != null && second != null
                && first.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase(second.replaceAll("[^A-Za-z0-9]", ""));
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
        PENDING_AIM_ABILITIES.remove(player.getUUID());
        ABILITY_COOLDOWNS.remove(player.getUUID());
        Map<Integer, List<ModifierEntry>> byIndex = HELD_ABILITIES.remove(player.getUUID());
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (data != null)
        {
            data.getData().set(Vars.AIMING, false);
            data.getData().set(Vars.TELEKINESIS, false);
            data.getData().set(Vars.GRAB_ID, -1);
            data.getData().set(Vars.GRAB_DISTANCE, 0.0F);
        }
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
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);
        if (data != null)
        {
            data.getData().set(Vars.AIMING, false);
            data.getData().set(Vars.TELEKINESIS, false);
            data.getData().set(Vars.GRAB_ID, -1);
            data.getData().set(Vars.GRAB_DISTANCE, 0.0F);
        }
    }

    /** Mirrors the physical key state on the client so local render and update hooks can query it. */
    public static void setClientKeyState(Entity entity, int index, boolean pressed)
    {
        HeroIteration iteration = HeroTracker.getHero(entity);

        if (iteration != null && iteration.getHero().getKeyBinding("AIM") == index)
        {
            SHPlayerData data = SHDataCapabilities.getPlayer(entity);
            if (data != null)
            {
                data.getData().set(Vars.AIMING,
                        iteration != null && shouldAim(entity, iteration.getHero(), pressed));
            }
        }

        if (iteration != null && iteration.getHero().getKeyBindsMatching(index).contains("TELEKINESIS"))
        {
            SHPlayerData data = SHDataCapabilities.getPlayer(entity);
            if (data != null)
            {
                boolean active = pressed && iteration.getHero().isKeyBindEnabled(entity, "TELEKINESIS");
                data.getData().set(Vars.TELEKINESIS, active);
                if (!active)
                {
                    data.getData().set(Vars.GRAB_ID, -1);
                    data.getData().set(Vars.GRAB_DISTANCE, 0.0F);
                }
            }
        }

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

    /** Resolves the pack's held AIM key into the synchronized state used by guns and suit poses. */
    private static void updateAiming(ServerPlayer player, SHPlayerData data, Hero hero, boolean pressed)
    {
        data.getData().set(Vars.AIMING, shouldAim(player, hero, pressed));
    }

    private static boolean shouldAim(Entity entity, Hero hero, boolean pressed)
    {
        if (!pressed || !hero.hasKeyBind("AIM") || !hero.isKeyBindEnabled(entity, "AIM"))
        {
            return false;
        }

        ScriptFunction canAim = hero.getFunction("canAim");
        return canAim == null || canAim.callBoolean(new com.fiskmods.heroes.pack.js.JSEntity(entity));
    }

    /** Advances the normalized aim animation timer on both logical sides. */
    public static void tickAiming(Entity entity, SHPlayerData data)
    {
        HeroIteration iteration = HeroTracker.getHero(entity);
        boolean aiming = iteration != null && shouldAim(entity, iteration.getHero(), isKeyPressed(entity, "AIM"));
        if (data.getData().get(Vars.AIMING) != aiming)
        {
            data.getData().set(Vars.AIMING, aiming);
        }

        float target = data.getData().get(Vars.AIMING) ? 1.0F : 0.0F;
        float current = data.getData().get(Vars.AIMING_TIMER);
        float next = net.minecraft.util.Mth.approach(current, target, 0.2F);
        if (next != current)
        {
            data.getData().set(Vars.AIMING_TIMER, next);
        }
    }

    /** Resolves whether the named ability key is currently held on this logical side. */
    public static boolean isKeyPressed(Entity entity, String key)
    {
        if (entity instanceof net.minecraft.world.entity.LivingEntity living
                && living.hasEffect(com.fiskmods.heroes.common.hero.modifier.ModEffects.TUTRIDIUM.get())) return false;
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
        return findModifier(hero, key, null);
    }

    /** Resolves script-valued keys for the current wearer before matching the modifier. */
    public static ModifierEntry findModifier(Hero hero, String key, Entity entity)
    {
        for (ModifierEntry entry : hero.getPowerContainer().getEntries())
        {
            String property = entry.get(entity, PowerProperty.KEY);

            if (key.equals(property) || sameAbilityName(key, property)
                    || sameAbilityName(key, entry.getModifier().getId().getPath()))
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
