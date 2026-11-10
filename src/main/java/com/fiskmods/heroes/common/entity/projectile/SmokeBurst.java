package com.fiskmods.heroes.common.entity.projectile;

import com.fiskmods.heroes.common.hero.HeroTracker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared approximation of the original smoke-pellet and smoke-arrow burst. */
public final class SmokeBurst
{
    private SmokeBurst() {}

    public static void detonate(ServerLevel level, Vec3 center)
    {
        final double radius = 2.5D;
        AABB bounds = new AABB(center.x - radius, center.y - radius / 2.0D, center.z - radius,
                center.x + radius, center.y + radius / 2.0D, center.z + radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds))
        {
            if (!HeroTracker.hasHero(target)) continue;
            double dx = target.getX() - center.x;
            double dy = (target.getY() + target.getBbHeight() / 2.0D - center.y) * 2.0D;
            double dz = target.getZ() - center.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz) / radius;
            if (distance <= 1.0D)
            {
                // Vanilla invisibility is an approximation of the original custom stealth effect.
                target.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int) Math.ceil(100.0D + distance * 60.0D)));
            }
        }
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y, center.z, 300, 0.3D, 0.15D, 0.3D, 0.0D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXTINGUISH_FIRE,
                SoundSource.PLAYERS, 2.0F, 0.9F + level.random.nextFloat() * 0.2F);
        if (Boolean.getBoolean("fiskheroes.ciEquipmentDiagnostics"))
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Smoke burst: center={}", center);
    }
}
