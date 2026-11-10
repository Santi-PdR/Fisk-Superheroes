package com.fiskmods.heroes.common.entity.projectile;

import com.fiskmods.heroes.common.config.SHConfig;
import com.fiskmods.heroes.common.entity.ModEntities;
import com.fiskmods.heroes.common.hero.modifier.DamageGroups;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Captain Cold's projectile: a small freeze burst on impact, rather than a timed grenade. */
public final class ColdBallEntity extends ThrowableProjectile
{
    private static final double BLOCK_RADIUS = 1.0D;
    private static final double ENTITY_RADIUS = 2.0D;

    public ColdBallEntity(EntityType<? extends ColdBallEntity> type, Level level)
    {
        super(type, level);
    }

    public ColdBallEntity(LivingEntity shooter)
    {
        super(ModEntities.COLD_BALL.get(), shooter, shooter.level());
        setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, 1.5F, 0.0F);
    }

    @Override protected void defineSynchedData() {}
    @Override protected float getGravity() { return 0.03F; }

    @Override
    public void tick()
    {
        super.tick();
        if (level().isClientSide && !isRemoved())
        {
            for (int i = 0; i < 3; i++)
                level().addParticle(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(),
                        (random.nextDouble() - 0.5D) * 0.06D,
                        (random.nextDouble() - 0.5D) * 0.06D,
                        (random.nextDouble() - 0.5D) * 0.06D);
        }
        else if (!level().isClientSide && tickCount > 1200) discard();
    }

    @Override protected void onHitEntity(EntityHitResult hit)
    {
        if (level() instanceof ServerLevel server) explode(server, hit.getLocation());
        discard();
    }

    @Override protected void onHitBlock(BlockHitResult hit)
    {
        if (level() instanceof ServerLevel server)
        {
            var face = hit.getDirection();
            explode(server, hit.getLocation().add(face.getStepX() * 0.02D,
                    face.getStepY() * 0.02D, face.getStepZ() * 0.02D));
        }
        discard();
    }

    private void explode(ServerLevel level, Vec3 center)
    {
        boolean diagnostics = Boolean.getBoolean("fiskheroes.ciColdBallDiagnostics");
        if (diagnostics) com.fiskmods.heroes.FiskHeroes.LOGGER.info("Cold ball impact: center={}, owner={}", center, getOwner());
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z, 35, 0.35D, 0.35D, 0.35D, 0.05D);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 12, 0.2D, 0.2D, 0.2D, 0.05D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(ENTITY_RADIUS), e -> e != getOwner() && e.isAlive()))
        {
            double distance = target.getBoundingBox().getCenter().distanceTo(center);
            boolean exposed = visible(level, center, target.getBoundingBox().getCenter());
            if (diagnostics) com.fiskmods.heroes.FiskHeroes.LOGGER.info("Cold ball target: name={}, position={}, distance={}, exposed={}",
                    target.getName().getString(), target.position(), distance, exposed);
            if (distance > ENTITY_RADIUS || !exposed) continue;
            float strength = (float) (1.0D - distance / ENTITY_RADIUS);
            if (getOwner() instanceof LivingEntity shooter)
                DamageGroups.withDamageProfile(java.util.Map.of("COLD", 0.5D, "EXPLOSION", 0.6D),
                        () -> target.hurt(level.damageSources().indirectMagic(this, shooter), 6.0F * strength));
            target.clearFire();
            target.setTicksFrozen(Math.min(target.getTicksFrozen() + 140, target.getTicksRequiredToFreeze() + 40));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        }
        if (SHConfig.ALLOW_GRIEFING.get()) freezeNearby(level, center);
    }

    private void freezeNearby(ServerLevel level, Vec3 center)
    {
        BlockPos origin = BlockPos.containing(center);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-2, -2, -2), origin.offset(2, 2, 2)))
        {
            Vec3 point = Vec3.atCenterOf(pos);
            if (point.distanceToSqr(center) > BLOCK_RADIUS * BLOCK_RADIUS) continue;
            if (getOwner() instanceof ServerPlayer player && !level.mayInteract(player, pos)) continue;
            var state = level.getBlockState(pos);
            if (state.is(Blocks.WATER))
            {
                if (visible(level, center, point)) level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
            }
            else if (state.isAir() && Blocks.SNOW.defaultBlockState().canSurvive(level, pos)
                    && visible(level, center, point))
            {
                level.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
            }
        }
    }

    private boolean visible(ServerLevel level, Vec3 from, Vec3 to)
    {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }
}
