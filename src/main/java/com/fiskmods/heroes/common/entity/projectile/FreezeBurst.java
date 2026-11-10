package com.fiskmods.heroes.common.entity.projectile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fiskmods.heroes.common.config.SHConfig;
import com.fiskmods.heroes.common.hero.modifier.DamageGroups;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared approximation of the original impact-triggered freezing explosion. */
final class FreezeBurst
{
    private FreezeBurst() {}

    static void detonate(ServerLevel level, Entity projectile, Entity owner, Vec3 center,
            double blockRadius, float damage, JsonObject profile, boolean affectsOwner, boolean diagnostics)
    {
        double entityRadius = blockRadius * 2.0D;
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z,
                (int) (35 * blockRadius), 0.35D * blockRadius, 0.35D * blockRadius, 0.35D * blockRadius, 0.05D);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z,
                (int) (12 * blockRadius), 0.2D * blockRadius, 0.2D * blockRadius, 0.2D * blockRadius, 0.05D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(entityRadius), e -> (affectsOwner || e != owner) && e.isAlive()))
        {
            double distance = target.getBoundingBox().getCenter().distanceTo(center);
            boolean exposed = visible(level, projectile, center, target.getBoundingBox().getCenter());
            if (diagnostics) com.fiskmods.heroes.FiskHeroes.LOGGER.info("Freeze burst target: name={}, position={}, distance={}, exposed={}",
                    target.getName().getString(), target.position(), distance, exposed);
            if (distance > entityRadius || !exposed) continue;
            float strength = (float) (1.0D - distance / entityRadius);
            if (owner instanceof LivingEntity shooter && damage > 0.0F)
            {
                Runnable hit = () -> target.hurt(level.damageSources().indirectMagic(projectile, shooter), damage * strength);
                if (profile != null && profile.has("types")) DamageGroups.withDamageProfile(profile, hit);
                else DamageGroups.withDamageProfile(Map.of("COLD", 0.5D, "EXPLOSION", 0.6D), hit);
            }
            target.clearFire();
            target.setTicksFrozen(Math.min(target.getTicksFrozen() + 140, target.getTicksRequiredToFreeze() + 40));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        }
        if (SHConfig.ALLOW_GRIEFING.get()) freezeNearby(level, projectile, owner, center, blockRadius);
    }

    private static void freezeNearby(ServerLevel level, Entity projectile, Entity owner, Vec3 center, double radius)
    {
        BlockPos origin = BlockPos.containing(center);
        int extent = (int) Math.ceil(radius + 0.5D);
        List<BlockPos> water = new ArrayList<>();
        List<BlockPos> snow = new ArrayList<>();
        // Evaluate visibility before placing blocks, so newly formed ice cannot
        // erroneously shield other positions in the same burst.
        for (BlockPos cursor : BlockPos.betweenClosed(origin.offset(-extent, -extent, -extent),
                origin.offset(extent, extent, extent)))
        {
            BlockPos pos = cursor.immutable();
            Vec3 point = Vec3.atCenterOf(pos);
            if (point.distanceToSqr(center) > radius * radius || !level.hasChunkAt(pos)) continue;
            if (owner instanceof ServerPlayer player && !level.mayInteract(player, pos)) continue;
            var state = level.getBlockState(pos);
            if ((!state.is(Blocks.WATER) && !(state.isAir() && Blocks.SNOW.defaultBlockState().canSurvive(level, pos)))
                    || !visible(level, projectile, center, point)) continue;
            if (state.is(Blocks.WATER)) water.add(pos);
            else snow.add(pos);
        }
        for (BlockPos pos : water)
            if (level.getBlockState(pos).is(Blocks.WATER)) level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
        for (BlockPos pos : snow)
            if (level.isEmptyBlock(pos) && Blocks.SNOW.defaultBlockState().canSurvive(level, pos))
                level.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
    }

    private static boolean visible(ServerLevel level, Entity projectile, Vec3 from, Vec3 to)
    {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, projectile)).getType() == HitResult.Type.MISS;
    }
}
