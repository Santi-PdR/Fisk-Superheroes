package com.fiskmods.heroes.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Expanding cast effect that assigns a temporary gravity factor to nearby entities. */
public final class GravityWaveEntity extends Entity
{
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(GravityWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GRAVITY = SynchedEntityData.defineId(GravityWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SIDE = SynchedEntityData.defineId(GravityWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(GravityWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> INCLUDE_CASTER = SynchedEntityData.defineId(GravityWaveEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int LIFETIME_TICKS = 30;
    private static final int APPLY_TICK = 1;

    public GravityWaveEntity(EntityType<? extends GravityWaveEntity> type, Level level)
    {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public GravityWaveEntity(Level level, LivingEntity caster, double x, double y, double z, int side,
            float radius, float gravity, boolean includeCaster)
    {
        this(ModEntities.GRAVITY_WAVE.get(), level);
        entityData.set(RADIUS, Math.max(0.0F, radius));
        entityData.set(GRAVITY, gravity);
        entityData.set(SIDE, side);
        entityData.set(CASTER_ID, caster == null ? -1 : caster.getId());
        entityData.set(INCLUDE_CASTER, includeCaster);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(RADIUS, 0.0F);
        entityData.define(GRAVITY, 1.0F);
        entityData.define(SIDE, 1);
        entityData.define(CASTER_ID, -1);
        entityData.define(INCLUDE_CASTER, false);
    }

    @Override
    public void tick()
    {
        super.tick();
        setDeltaMovement(0.0D, 0.0D, 0.0D);

        if (!level().isClientSide && tickCount == APPLY_TICK && level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
        {
            Entity caster = getCaster();
            AABB area = getBoundingBox().inflate(getRadius());
            for (Entity target : serverLevel.getEntities(this, area, Entity::isAlive))
            {
                if (!entityData.get(INCLUDE_CASTER) && (target == caster || target.getVehicle() == caster)) continue;
                GravityEffectHandler.setGravity(serverLevel, target, entityData.get(GRAVITY));
            }
        }

        if (tickCount >= LIFETIME_TICKS) discard();
    }

    public float getRadius()
    {
        return entityData.get(RADIUS);
    }

    public float getProgress(float partialTick)
    {
        return Math.min((tickCount + partialTick) / 16.0F, 1.0F);
    }

    public int getSide()
    {
        return entityData.get(SIDE);
    }

    public Entity getCaster()
    {
        int id = entityData.get(CASTER_ID);
        return id < 0 ? null : level().getEntity(id);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        tag.putFloat("Radius", entityData.get(RADIUS));
        tag.putFloat("Gravity", entityData.get(GRAVITY));
        tag.putInt("Side", entityData.get(SIDE));
        tag.putBoolean("IncludeCaster", entityData.get(INCLUDE_CASTER));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        entityData.set(RADIUS, tag.getFloat("Radius"));
        entityData.set(GRAVITY, tag.getFloat("Gravity"));
        entityData.set(SIDE, tag.getInt("Side"));
        entityData.set(INCLUDE_CASTER, tag.getBoolean("IncludeCaster"));
    }

    @Override
    public boolean isPickable()
    {
        return false;
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }
}
