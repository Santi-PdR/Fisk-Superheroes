package com.fiskmods.heroes.common.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Server-authoritative version of the original 60-tick Earth Swallowing trap.
 * It lifts a selected target out of the ground, then slams and damages it.
 */
public final class EarthCrackEntity extends Entity
{
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(EarthCrackEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(EarthCrackEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(EarthCrackEntity.class, EntityDataSerializers.FLOAT);
    private static final int DURATION_TICKS = 60;

    public EarthCrackEntity(EntityType<? extends EarthCrackEntity> type, Level level)
    {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public EarthCrackEntity(EntityType<? extends EarthCrackEntity> type, Level level, LivingEntity caster,
            LivingEntity target, float damage)
    {
        this(type, level);
        entityData.set(CASTER_ID, caster.getId());
        entityData.set(TARGET_ID, target.getId());
        entityData.set(DAMAGE, Math.max(0.0F, damage));
        setPos(target.getX(), target.getY(), target.getZ());
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(CASTER_ID, -1);
        entityData.define(TARGET_ID, -1);
        entityData.define(DAMAGE, 14.0F);
    }

    @Override
    public void tick()
    {
        super.tick();
        LivingEntity target = getLiving(TARGET_ID);
        LivingEntity caster = getLiving(CASTER_ID);
        boolean expired = tickCount > DURATION_TICKS;

        if (level().isClientSide)
        {
            if (target != null && target.isAlive()) emitCrackParticles(target);
            if (expired) discard();
            return;
        }

        if (target == null || !target.isAlive() || expired)
        {
            if (expired && target != null && target.isAlive() && caster != null)
            {
                target.hurt(caster.damageSources().magic(), entityData.get(DAMAGE));
                level().playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.STONE_BREAK,
                        net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.7F);
            }
            discard();
            return;
        }

        target.setDeltaMovement(0.0D, target.getDeltaMovement().y, 0.0D);
        target.fallDistance = 0.0F;
        if (tickCount < DURATION_TICKS / 2.5F)
        {
            double crest = Math.sin(Math.min(tickCount * 2.5D / DURATION_TICKS, 1.0D) * Math.PI);
            target.setDeltaMovement(0.0D, target.getDeltaMovement().y + 0.075D + 0.04D * crest, 0.0D);
        }
        else if (tickCount < DURATION_TICKS - 5)
        {
            target.setDeltaMovement(0.0D, target.getDeltaMovement().y * 0.9D + 0.075D, 0.0D);
        }
        else
        {
            target.setDeltaMovement(0.0D, -0.65D, 0.0D);
        }
        target.hasImpulse = true;
    }

    private LivingEntity getLiving(EntityDataAccessor<Integer> accessor)
    {
        Entity entity = level().getEntity(entityData.get(accessor));
        return entity instanceof LivingEntity living ? living : null;
    }

    public int getTargetId()
    {
        return entityData.get(TARGET_ID);
    }

    private void emitCrackParticles(LivingEntity target)
    {
        if (tickCount % 3 != 0) return;
        BlockPos ground = target.blockPosition().below();
        BlockState state = level().getBlockState(ground);
        if (state.isAir()) return;
        for (int i = 0; i < 8; i++)
        {
            double angle = (tickCount * 0.17D) + (Math.PI * 2.0D * i / 8.0D);
            double radius = 0.65D + random.nextDouble() * 0.35D;
            level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    target.getX() + Math.cos(angle) * radius, target.getY() + 0.05D,
                    target.getZ() + Math.sin(angle) * radius,
                    Math.cos(angle) * 0.05D, 0.12D + random.nextDouble() * 0.12D,
                    Math.sin(angle) * 0.05D);
        }
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
    {
        entityData.set(CASTER_ID, tag.getInt("Caster"));
        entityData.set(TARGET_ID, tag.getInt("Target"));
        entityData.set(DAMAGE, tag.getFloat("Damage"));
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
    {
        tag.putInt("Caster", entityData.get(CASTER_ID));
        tag.putInt("Target", entityData.get(TARGET_ID));
        tag.putFloat("Damage", entityData.get(DAMAGE));
    }
}
