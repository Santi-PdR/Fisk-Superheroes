package com.fiskmods.heroes.common.entity.projectile;

import com.fiskmods.heroes.common.entity.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Captain Cold's projectile: a small freeze burst on impact, rather than a timed grenade. */
public final class ColdBallEntity extends ThrowableProjectile
{
    private static final double BLOCK_RADIUS = 1.0D;

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
            // Vanilla snowflakes are much larger and longer lived than the original
            // short cryo smoke; avoid obscuring the orb or accumulating while stationary.
            if (tickCount % 3 == 0 && getDeltaMovement().lengthSqr() > 1.0E-4D)
                level().addParticle(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(),
                        (random.nextDouble() - 0.5D) * 0.02D,
                        (random.nextDouble() - 0.5D) * 0.02D,
                        (random.nextDouble() - 0.5D) * 0.02D);
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
        FreezeBurst.detonate(level, this, getOwner(), center, BLOCK_RADIUS, 6.0F, null, false, diagnostics);
    }
}
