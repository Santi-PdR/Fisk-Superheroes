package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

/** Shared normalized heat handling for metal skin and heat-transfer attacks. */
public final class MetalSkinHeat
{
    private static final int LAVA_HEAT_TICKS = 100;
    private static final int FULL_HEAT_RECOVERY_TICKS = 200;

    private MetalSkinHeat()
    {
    }

    public static void add(LivingEntity entity, float amount)
    {
        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);
        if (data == null || !data.getData().get(Vars.METAL_SKIN) || amount <= 0.0F) return;

        float heat = data.getData().get(Vars.METAL_HEAT);
        data.getData().set(Vars.METAL_HEAT, net.minecraft.util.Mth.clamp(heat + amount, 0.0F, 1.0F));
    }

    /** Mirrors the original cooling, lava exposure and temporary loss of fire immunity. */
    public static void tick(LivingEntity entity, SHPlayerData data)
    {
        if (entity.level().isClientSide || !data.getData().get(Vars.METAL_SKIN)) return;

        float previousHeat = data.getData().get(Vars.METAL_HEAT);
        float heat = previousHeat;
        short cooldown = data.getData().get(Vars.METAL_HEAT_COOLDOWN);

        if (entity.isInWater())
        {
            if (heat > 0.0F)
            {
                heat = Math.max(0.0F, heat - 1.0F / 30.0F);
                if (entity.tickCount % 2 == 0)
                {
                    entity.level().playSound(null, entity.blockPosition(), net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F,
                            0.8F + 0.6F * (1.0F - heat) + (entity.getRandom().nextFloat() - entity.getRandom().nextFloat()) * 0.4F);
                }
                if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
                {
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                            entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                            3, entity.getBbWidth() * 0.25D, entity.getBbHeight() * 0.25D,
                            entity.getBbWidth() * 0.25D, 0.0D);
                }
            }
        }
        else if (entity.isInLava())
        {
            heat = Math.min(1.0F, heat + 1.0F / LAVA_HEAT_TICKS);
        }

        if (heat >= 1.0F && cooldown <= 0)
        {
            cooldown = FULL_HEAT_RECOVERY_TICKS;
        }
        if (cooldown > 0) --cooldown;
        if (heat == previousHeat && heat > 0.0F && cooldown <= 0)
        {
            heat = Math.max(0.0F, heat - 1.0F / 300.0F);
        }

        data.getData().set(Vars.METAL_HEAT, net.minecraft.util.Mth.clamp(heat, 0.0F, 1.0F));
        data.getData().set(Vars.METAL_HEAT_COOLDOWN, cooldown);
    }
}
