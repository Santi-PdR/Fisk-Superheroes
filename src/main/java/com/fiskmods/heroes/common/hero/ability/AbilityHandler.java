package com.fiskmods.heroes.common.hero.ability;

import java.util.Set;

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

        for (String key : keys)
        {
            if (!hero.isKeyBindEnabled(player, key))
            {
                continue;
            }

            ScriptFunction function = hero.getKeyBindFuncs().get(key);

            if (function != null)
            {
                function.call(player, pressed);
                continue;
            }

            activate(player, data, hero, key, pressed);
        }
    }

    /** Runs the default behaviour of an ability key: toggle or activate the modifier bound to it. */
    private static void activate(ServerPlayer player, SHPlayerData data, Hero hero, String key, boolean pressed)
    {
        ModifierEntry entry = findModifier(hero, key);

        if (entry == null)
        {
            return;
        }

        boolean toggle = entry.getBoolean(PowerProperty.IS_TOGGLE);

        if (pressed)
        {
            entry.getModifier().onActivate(player, entry, data);
        }
        else if (!toggle)
        {
            entry.getModifier().onToggle(player, entry, data);
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
