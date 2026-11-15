package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

/** Shared normalized heat handling for metal skin and heat-transfer attacks. */
public final class MetalSkinHeat
{
    public static final int LAVA_HEAT_TICKS = 120;
    public static final int HEAT_GUN_TICKS = 170;
    public static final int BLAZE_ARROW_TICKS = 4;
    private static final int FULL_HEAT_RECOVERY_TICKS = 200;

    private MetalSkinHeat()
    {
    }

    /** Check the live modifier predicate, not a potentially one-tick-old cached flag. */
    public static boolean hasEnabledMetalSkin(LivingEntity entity, SHPlayerData data)
    {
        if (data == null || data.getHeroType() == null) return false;
        var entry = data.getHeroType().getPowerContainer().getEntry(com.fiskmods.heroes.FiskHeroes.id("metal_skin"));
        return entry != null && entry.isEnabled() && entry.isModifierEnabled(entity, data);
    }

    public static void add(LivingEntity entity, float amount)
    {
        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);
        if (!Float.isFinite(amount) || amount <= 0.0F || !hasEnabledMetalSkin(entity, data)) return;

        float heat = data.getData().get(Vars.METAL_HEAT);
        data.getData().set(Vars.METAL_HEAT, net.minecraft.util.Mth.clamp(heat + amount, 0.0F, 1.0F));
    }

    /** Mirrors the original cooling, lava exposure and temporary loss of fire immunity. */
    public static void tick(LivingEntity entity, SHPlayerData data)
    {
        if (entity.level().isClientSide) return;
        boolean metalSkin = data.getData().get(Vars.METAL_SKIN);

        float previousHeat = data.getData().get(Vars.METAL_HEAT);
        float heat = previousHeat;
        short cooldown = data.getData().get(Vars.METAL_HEAT_COOLDOWN);

        if (metalSkin && entity.isInWater())
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
        else if (metalSkin && entity.isInLava())
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
