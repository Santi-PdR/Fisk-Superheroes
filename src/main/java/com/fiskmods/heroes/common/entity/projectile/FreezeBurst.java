package com.fiskmods.heroes.common.entity.projectile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fiskmods.heroes.common.config.SHConfig;
import com.fiskmods.heroes.common.hero.modifier.DamageGroups;
import com.fiskmods.heroes.common.util.ProtectedBlockChanges;
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
        // The original samples the whole target hitbox (not just its center), so
        // partial cover attenuates damage, temperature change and knockback alike.
        for (Entity target : level.getEntities(projectile, new AABB(center, center).inflate(entityRadius),
                e -> (affectsOwner || e != owner) && e.isAlive()))
        {
            double distance = target.position().distanceTo(center);
            if (distance >= entityRadius) continue;
            Vec3 direction = target.getEyePosition().subtract(center);
            if (direction.lengthSqr() < 1.0E-8D) continue;
            double exposure = net.minecraft.world.level.Explosion.getSeenPercent(center, target);
            double strength = (1.0D - distance / entityRadius) * exposure;
            if (diagnostics) com.fiskmods.heroes.FiskHeroes.LOGGER.info("Freeze burst target: name={}, position={}, distance={}, exposure={}, strength={}",
                    target.getName().getString(), target.position(), distance, exposure, strength);
            if (strength <= 0.0D) continue;
            if (damage > 0.0F && !(target instanceof net.minecraft.world.entity.decoration.HangingEntity))
            {
                // Like the original, ownerless bursts still inflict damage, but
                // their block changes remain fail-closed without a player identity.
                var type = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                                com.fiskmods.heroes.FiskHeroes.id("freeze_explosion")));
                Entity shooter = owner instanceof LivingEntity ? owner : null;
                var source = shooter == null ? new net.minecraft.world.damagesource.DamageSource(type)
                        : new net.minecraft.world.damagesource.DamageSource(type, shooter, shooter);
                Runnable hit = () -> target.hurt(source, damage * (float) strength);
                if (profile != null && profile.has("types")) DamageGroups.withDamageProfile(profile, hit);
                else DamageGroups.withDamageProfile(Map.of("COLD", 0.5D, "EXPLOSION", 0.6D), hit);
            }
            if (target instanceof LivingEntity living && damage > 0.0F)
            {
                // Original cooling is damage/2 times exposure, further reduced by
                // COLD resistance. Vanilla frozen ticks are only an approximation.
                double cold = strength * damage / 9.0D
                        * com.fiskmods.heroes.common.util.ColdExposure.susceptibility(living);
                if (cold > 0.0D)
                {
                    int frozen = Math.max(1, (int) Math.min(180.0D, Math.round(140.0D * cold)));
                    living.clearFire();
                    living.setTicksFrozen(Math.min(living.getTicksFrozen() + frozen, living.getTicksRequiredToFreeze() + 40));
                    int duration = Math.max(1, (int) Math.min(100.0D, Math.round(100.0D * cold)));
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 2));
                }
            }
            // 1.7.10 also dampens explosion velocity through Blast Protection.
            double knockback = target instanceof LivingEntity living
                    ? net.minecraft.world.item.enchantment.ProtectionEnchantment.getExplosionKnockbackAfterDampener(living, strength)
                    : strength;
            Vec3 impulse = direction.normalize().scale(knockback);
            target.push(impulse.x, impulse.y, impulse.z);
            target.hurtMarked = true; // send the server-authoritative impulse to players
        }
        if (SHConfig.ALLOW_GRIEFING.get()) freezeNearby(level, projectile, owner, center, blockRadius);
    }

    private static void freezeNearby(ServerLevel level, Entity projectile, Entity owner, Vec3 center, double radius)
    {
        // A missing/disconnected owner must not silently bypass player claim checks.
        // Until non-player ownership has an explicit protection policy, fail closed.
        if (!(owner instanceof ServerPlayer player)) return;
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
            if (!level.getWorldBorder().isWithinBounds(pos)) continue;
            if (!level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, net.minecraft.core.Direction.UP, player.getMainHandItem())) continue;
            var state = level.getBlockState(pos);
            if ((!state.is(Blocks.WATER) && !(state.isAir() && Blocks.SNOW.defaultBlockState().canSurvive(level, pos)))
                    || !visible(level, projectile, center, point)) continue;
            if (state.is(Blocks.WATER)) water.add(pos);
            else snow.add(pos);
        }
        for (BlockPos pos : water)
        {
            var state = level.getBlockState(pos);
            if (!state.is(Blocks.WATER)) continue;
            if (ProtectedBlockChanges.canBreak(player, pos, state))
                ProtectedBlockChanges.placeBlock(player, pos, net.minecraft.core.Direction.UP, Blocks.ICE.defaultBlockState());
        }
        for (BlockPos pos : snow)
        {
            if (!level.isEmptyBlock(pos) || !Blocks.SNOW.defaultBlockState().canSurvive(level, pos)
                    || !level.getEntities(null, new AABB(pos)).isEmpty()) continue;
            ProtectedBlockChanges.placeBlock(player, pos, net.minecraft.core.Direction.UP, Blocks.SNOW.defaultBlockState());
        }
    }

    private static boolean visible(ServerLevel level, Entity projectile, Vec3 from, Vec3 to)
    {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, projectile)).getType() == HitResult.Type.MISS;
    }
}
