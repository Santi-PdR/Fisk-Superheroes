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
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.fiskmods.heroes.pack.ScriptFunction;

import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side ability activation. The client only reports "ability key N was pressed"; all of the
 * validation and the actual effect happen here, so the client cannot cheat abilities.
 */
public final class AbilityHandler
{
    /** Non-toggle modifiers started by a key-down, grouped until their matching key-up. */
    private static final Map<UUID, Map<Integer, List<ModifierEntry>>> HELD_ABILITIES = new HashMap<>();

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

        if (entry == null)
        {
            return null;
        }

        entry.getModifier().onActivate(player, entry, data);
        return entry;
    }

    /** Delivers key-up to the exact entries that accepted the corresponding key-down. */
    private static void release(ServerPlayer player, SHPlayerData data, int index)
    {
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
