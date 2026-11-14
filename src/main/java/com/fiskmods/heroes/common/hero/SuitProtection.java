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

        // Drowning, falling and suffocation received half protection in 1.7.10.
        // Indirect magic was explicitly protected despite bypassing vanilla armor.
        float sourceScale = 1.0F;
        if (source.is(DamageTypes.DROWN) || source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL))
            sourceScale = 0.5F;
        else if (!source.is(DamageTypes.INDIRECT_MAGIC) && source.is(DamageTypeTags.BYPASSES_ARMOR))
            return 1.0F;

        var value = SHAttributes.collect(entity, data.getHeroType()).get(HeroAttribute.DAMAGE_REDUCTION);
        double reduction = value != null ? Math.max(0.0D, Math.min(1.0D, value.amount())) : 0.0D;
        reduction *= sourceScale;
        if (source.is(DamageTypeTags.IS_FIRE))
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
}
