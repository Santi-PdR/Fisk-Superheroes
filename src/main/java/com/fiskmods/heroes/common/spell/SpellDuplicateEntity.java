package com.fiskmods.heroes.common.spell;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Owner-following, non-solid spell decoy placed around the selected target. */
public final class SpellDuplicateEntity extends Mob
{
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(SpellDuplicateEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(SpellDuplicateEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ROTATION_OFFSET = SynchedEntityData.defineId(SpellDuplicateEntity.class, EntityDataSerializers.FLOAT);
    private float spread;

    public SpellDuplicateEntity(EntityType<? extends SpellDuplicateEntity> type, Level level)
    {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public SpellDuplicateEntity(EntityType<? extends SpellDuplicateEntity> type, Level level,
            LivingEntity owner, LivingEntity target, float rotationOffset)
    {
        this(type, level);
        entityData.set(OWNER_ID, owner.getId());
        entityData.set(TARGET_ID, target.getId());
        entityData.set(ROTATION_OFFSET, net.minecraft.util.Mth.wrapDegrees(rotationOffset));
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(OWNER_ID, -1);
        entityData.define(TARGET_ID, -1);
        entityData.define(ROTATION_OFFSET, 0.0F);
    }

    @Override
    protected void registerGoals()
    {
    }

    @Override
    public void tick()
    {
        super.tick();
        LivingEntity owner = getOwner();
        LivingEntity target = getTarget();
        if (owner == null || target == null || !owner.isAlive() || !target.isAlive()
                || distanceTo(target) > 32.0F || !hasSpellcasting(owner))
        {
            if (tickCount > 5 || !level().isClientSide) discard();
            return;
        }

        float offset = entityData.get(ROTATION_OFFSET);
        spread = Math.min(Math.abs(offset), spread + 10.0F);
        float eased = Math.min(1.0F, spread / Math.max(1.0F, Math.abs(offset)));
        eased = eased * eased * (3.0F - 2.0F * eased);
        float angle = (float) Math.toRadians(Math.copySign(Math.abs(offset) * eased, offset));
        Vec3 center = target.position();
        Vec3 ownerOffset = owner.position().subtract(center);
        double x = center.x + ownerOffset.x * Math.cos(angle) - ownerOffset.z * Math.sin(angle);
        double z = center.z + ownerOffset.z * Math.cos(angle) + ownerOffset.x * Math.sin(angle);
        setPos(x, owner.getY(), z);
        setYRot(owner.getYRot() - (float) Math.toDegrees(angle));
        setXRot(owner.getXRot());
        setShiftKeyDown(owner.isShiftKeyDown());
        setSprinting(owner.isSprinting());
        setDeltaMovement(Vec3.ZERO);
        fallDistance = 0.0F;
    }

    private boolean hasSpellcasting(LivingEntity owner)
    {
        var hero = com.fiskmods.heroes.common.hero.HeroTracker.getHeroType(owner);
        return hero != null && !ModifierSpellcasting.getSpells(hero).getSpells().isEmpty();
    }

    public LivingEntity getOwner()
    {
        Entity owner = level().getEntity(entityData.get(OWNER_ID));
        return owner instanceof LivingEntity living ? living : null;
    }

    public LivingEntity getTarget()
    {
        Entity target = level().getEntity(entityData.get(TARGET_ID));
        return target instanceof LivingEntity living ? living : null;
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public boolean isPickable()
    {
        return true;
    }

    @Override
    public boolean isAlliedTo(Entity other)
    {
        LivingEntity owner = getOwner();
        return other == owner || owner != null && owner.isAlliedTo(other) || super.isAlliedTo(other);
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        if (level().isClientSide || isInvulnerableTo(source)) return false;
        discard();
        return true;
    }

    @Override
    public boolean shouldDropExperience()
    {
        return false;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt("OwnerId", entityData.get(OWNER_ID));
        tag.putInt("TargetId", entityData.get(TARGET_ID));
        tag.putFloat("RotationOffset", entityData.get(ROTATION_OFFSET));
        tag.putFloat("Spread", spread);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        entityData.set(OWNER_ID, tag.getInt("OwnerId"));
        entityData.set(TARGET_ID, tag.getInt("TargetId"));
        entityData.set(ROTATION_OFFSET, tag.getFloat("RotationOffset"));
        spread = tag.getFloat("Spread");
    }
}
