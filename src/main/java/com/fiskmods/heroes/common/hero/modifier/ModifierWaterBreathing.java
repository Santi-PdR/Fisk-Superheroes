package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * The wearer breathes underwater. Rather than short-circuiting the vanilla air supply (which would
 * desync the client's bubble HUD), water breathing is maintained as a constant effect, exactly as
 * the original mod did.
 */
public class ModifierWaterBreathing extends Modifier
{
    public ModifierWaterBreathing(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (entity.isInWater() && entity.getAirSupply() < entity.getMaxAirSupply() - 1)
        {
            entity.setAirSupply(entity.getMaxAirSupply());
        }

        if (entity.hasEffect(MobEffects.WATER_BREATHING))
        {
            MobEffectInstance effect = entity.getEffect(MobEffects.WATER_BREATHING);

            if (effect != null && effect.getDuration() < 10)
            {
                entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, true, false, false));
            }
        }
        else
        {
            entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, true, false, false));
        }
    }
}
