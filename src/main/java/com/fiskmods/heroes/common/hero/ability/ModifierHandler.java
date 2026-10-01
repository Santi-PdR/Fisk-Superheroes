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
