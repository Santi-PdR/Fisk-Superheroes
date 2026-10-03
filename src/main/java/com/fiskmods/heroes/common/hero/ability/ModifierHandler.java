package com.fiskmods.heroes.common.hero.ability;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Drives every active modifier of an entity: ticking, damage interception and lifecycle hooks.
 */
public final class ModifierHandler
{
    private ModifierHandler()
    {
    }

    public static void tick(LivingEntity entity)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);

        if (data == null)
        {
            return;
        }

        HeroTracker.update(entity);

        if (data.getHeroType() == null)
        {
            return;
        }

        boolean integrated = entity.level() instanceof net.minecraft.server.level.ServerLevel;

        if (integrated && data.getHeroType().getTickHandler() != null)
        {
            try
            {
                data.getHeroType().getTickHandler().call(
                        new com.fiskmods.heroes.pack.js.JSEntity(entity), new com.fiskmods.heroes.pack.js.JSManager());
            }
            catch (Exception e)
            {
                com.fiskmods.heroes.FiskHeroes.LOGGER.error("Error running hero tick handler for {}", data.getHeroType().getName(), e);
            }
        }

        for (ModifierEntry entry : data.getHeroType().getPowerContainer().getEntries())
        {
            if (!entry.isEnabled() || !entry.isModifierEnabled(entity, data))
            {
                continue;
            }

            try
            {
                if (integrated)
                {
                    dispatchSounds(entity, entry, data);
                    entry.getModifier().tick(entity, entry, data);
                }
                else
                {
                    entry.getModifier().tickClient(entity, entry, data);
                }
            }
            catch (Exception e)
            {
                com.fiskmods.heroes.FiskHeroes.LOGGER.error("Error ticking modifier {}", entry.getModifier().getName(), e);
            }
        }
    }

    /**
     * Plays the modifier's enable/disable sound when its state variable flips, and keeps the
     * looping variant running while the state is on. Mirrors the original's data-driven sound
     * dispatch (the sound definitions declare which of their entries loop and how they fade).
     */
    private static void dispatchSounds(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        com.fiskmods.heroes.common.data.var.DataVar<Boolean> state = entry.getModifier().getSoundState();

        if (state == null)
        {
            return;
        }

        boolean value = data.getData().get(state);
        Boolean previous = data.getSoundState(entry.getModifier().getId());

        if (previous == null || previous != value)
        {
            data.setSoundState(entry.getModifier().getId(), value);
            com.fiskmods.heroes.common.hero.modifier.AbilityData.playSound(entity, entry, value ? "ENABLE" : "DISABLE");
        }
    }

    /** Applies every immunity and resistance of the wearer; returns the resulting damage amount. */
    public static float modifyDamage(LivingEntity entity, DamageSource source, float amount)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);

        if (data == null || data.getHeroType() == null)
        {
            return amount;
        }

        if (data.isInvulnerable())
        {
            return 0.0F;
        }

        java.util.Map<String, Double> profile = null;
        net.minecraft.world.entity.Entity attacker = source.getEntity();
        if (attacker instanceof Player player && source.getDirectEntity() == attacker)
        {
            SHPlayerData attackerData = SHDataCapabilities.getPlayer(player);
            if (attackerData != null && attackerData.getHeroType() != null)
            {
                profile = attackerData.getHeroType().getMeleeDamageProfile(attacker);
            }
        }

        com.fiskmods.heroes.common.hero.modifier.DamageGroups.useDamageProfile(profile);
        try
        {
            float result = amount;

            for (ModifierEntry entry : data.getHeroType().getPowerContainer().getEntries())
            {
                if (!entry.isEnabled() || !entry.isModifierEnabled(entity, data))
                {
                    continue;
                }

                if (entry.getModifier().isImmuneTo(entity, entry, source, amount))
                {
                    return 0.0F;
                }

                result = entry.getModifier().modifyDamage(entity, entry, source, result);
            }

            return Math.max(0.0F, result);
        }
        finally
        {
            com.fiskmods.heroes.common.hero.modifier.DamageGroups.clearDamageProfile();
        }
    }

    public static void onJump(LivingEntity entity)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);

        if (data == null || data.getHeroType() == null)
        {
            return;
        }

        for (ModifierEntry entry : data.getHeroType().getPowerContainer().getEntries())
        {
            String path = entry.getModifier().getId().getPath();

            if ("leaping".equals(path))
            {
                com.fiskmods.heroes.common.hero.modifier.ModifierLeaping.onJump(entity, entry);
            }
        }
    }

    public static boolean hasModifier(Player player, String id)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(player);

        if (data == null || data.getHeroType() == null)
        {
            return false;
        }

        return data.getHeroType().getPowerContainer().has(id);
    }
}
