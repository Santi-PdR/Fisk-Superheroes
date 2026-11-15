package com.fiskmods.heroes.common.util;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Approximate the original temperature system's COLD resistance for vanilla frozen ticks. */
public final class ColdExposure
{
    private ColdExposure() {}

    public static float susceptibility(LivingEntity target)
    {
        var data = SHDataCapabilities.getPlayer(target);
        if (data != null && data.getHeroType() != null)
        {
            var resistance = data.getHeroType().getPowerContainer().getEntry(FiskHeroes.id("damage_resistance"));
            if (resistance != null && resistance.isEnabled() && resistance.isModifierEnabled(target, data)
                    && "COLD".equalsIgnoreCase(resistance.get(PowerProperty.DAMAGE_TYPE)))
            {
                float factor = resistance.getFloat(target, PowerProperty.FACTOR);
                if (Float.isFinite(factor)) return Mth.clamp(1.0F - factor, 0.0F, 1.0F);
            }
        }
        return 1.0F;
    }
    /** Add exposure without reducing frozen ticks already supplied by another effect. */
    public static void addFrozenTicks(LivingEntity target, int amount)
    {
        int current = target.getTicksFrozen();
        int cap = target.getTicksRequiredToFreeze() + 40;
        if (amount > 0 && current < cap)
            target.setTicksFrozen((int) Math.min((long) current + amount, cap));
    }

}
