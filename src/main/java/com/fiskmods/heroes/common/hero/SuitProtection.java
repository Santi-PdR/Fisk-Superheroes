package com.fiskmods.heroes.common.hero;

import com.fiskmods.heroes.common.config.SHConfig;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.hero.attribute.SHAttributes;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

/** Original tier protection, applied before vanilla armor and enchantments. */
public final class SuitProtection
{
    private SuitProtection() {}

    public static double tierReduction(int tier)
    {
        if (tier < 1 || tier > 10) return 0.0D;
        return tierReduction(tier, SHConfig.DAMAGE_REDUCTION_BASELINE.get(), SHConfig.DAMAGE_REDUCTION_FACTOR.get());
    }

    /** Kept independent of config so the original formula and rounding can be tested. */
    public static double tierReduction(int tier, double baseline, double factor)
    {
        if (tier < 1 || tier > 10) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D,
                Math.round((100.0D - (100.0D - baseline) * factor / Math.pow(factor, tier)) * 10.0D) / 1000.0D));
    }

    public static float damageMultiplier(LivingEntity entity, DamageSource source)
    {
        var data = SHDataCapabilities.getPlayer(entity);
        if (data == null || data.getHeroType() == null) return 1.0F;

        // MCP 1.7.10 mappings identify the original exceptions as onFire, lava
        // and magic: each receives half protection even if armor-bypassing.
        // Indirect magic receives full protection; other bypassing sources receive none.
        float sourceScale = 1.0F;
        if (source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.LAVA) || source.is(DamageTypes.MAGIC))
            sourceScale = 0.5F;
        else if (!source.is(DamageTypes.INDIRECT_MAGIC) && source.is(DamageTypeTags.BYPASSES_ARMOR))
            return 1.0F;

        var value = SHAttributes.collect(entity, data.getHeroType()).get(HeroAttribute.DAMAGE_REDUCTION);
        double reduction = value != null ? Math.max(0.0D, Math.min(1.0D, value.amount())) : 0.0D;
        reduction *= sourceScale;
        if (isFireDamage(source))
        {
            for (var entry : data.getHeroType().getPowerContainer().getEntries())
            {
                if (entry.getModifier() == com.fiskmods.heroes.common.hero.modifier.Modifiers.FIRE_WEAKNESS
                        && entry.isEnabled() && entry.isModifierEnabled(entity, data))
                {
                    reduction *= 0.5D;
                }
            }
        }
        return (float) (1.0D - reduction);
    }

    /** Legacy extended damage reports fire at a FIRE fraction of at least one half. */
    private static boolean isFireDamage(DamageSource source)
    {
        var profile = com.fiskmods.heroes.common.hero.modifier.DamageGroups.currentDamageProfile();
        if (profile == null && source.getEntity() instanceof net.minecraft.world.entity.player.Player attacker
                && source.getDirectEntity() == attacker
                && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)))
        {
            var attackerData = SHDataCapabilities.getPlayer(attacker);
            if (attackerData != null && attackerData.getHeroType() != null)
                profile = attackerData.getHeroType().getMeleeDamageProfile(attacker);
        }
        return profile != null ? profile.getOrDefault("FIRE", 0.0D) >= 0.5D : source.is(DamageTypeTags.IS_FIRE);
    }
}
