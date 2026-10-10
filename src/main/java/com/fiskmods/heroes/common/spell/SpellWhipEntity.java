package com.fiskmods.heroes.common.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Short-lived caster-to-target tether created by the Eldritch Whip spell. */
public final class SpellWhipEntity extends Entity
{
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(SpellWhipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(SpellWhipEntity.class, EntityDataSerializers.INT);
    private static final int LIFETIME = 40;
    private JsonObject burnProfile = new JsonObject();
    private int burnFrequency = 20;
    private boolean attackHeld;

    public SpellWhipEntity(EntityType<? extends SpellWhipEntity> type, Level level)
    {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public SpellWhipEntity(EntityType<? extends SpellWhipEntity> type, Level level,
            LivingEntity caster, LivingEntity target, JsonObject burnProfile, int burnFrequency)
    {
        this(type, level);
        entityData.set(CASTER_ID, caster.getId());
        entityData.set(TARGET_ID, target.getId());
        this.burnProfile = burnProfile != null ? burnProfile.deepCopy() : new JsonObject();
        this.burnFrequency = Math.max(1, burnFrequency);
        setPos(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(CASTER_ID, -1);
        entityData.define(TARGET_ID, -1);
    }

    @Override
    public void tick()
    {
        super.tick();
        LivingEntity caster = getLiving(CASTER_ID);
        LivingEntity target = getLiving(TARGET_ID);
        if (caster == null || target == null || !caster.isAlive() || !target.isAlive()
                || distanceToSqr(caster) > 1024.0D || tickCount >= LIFETIME
                || !caster.getMainHandItem().isEmpty())
        {
            discard();
            return;
        }

        setPos(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
        if (!level().isClientSide)
        {
            boolean nowHeld = com.fiskmods.heroes.common.hero.ability.AbilityHandler.isKeyPressed(caster, "AIM");
            if (nowHeld && !attackHeld)
            {
                var pull = caster.position().subtract(target.position()).normalize();
                target.setDeltaMovement(target.getDeltaMovement().add(pull.x * 2.0D,
                        pull.y * 0.5D, pull.z * 2.0D));
                target.hasImpulse = true;
            }
            attackHeld = nowHeld;

            // The original tether damps the target's horizontal motion while it is attached.
            target.setDeltaMovement(target.getDeltaMovement().multiply(0.5D, 1.0D, 0.5D));
            if (tickCount % burnFrequency == 0
                    && com.fiskmods.heroes.common.hero.modifier.DamageGroups.profileDamage(burnProfile, 0.0F) > 0.0F)
            {
                com.fiskmods.heroes.common.hero.modifier.DamageGroups.applyProfileDamage(target, caster,
                        caster.damageSources().magic(),
                        com.fiskmods.heroes.common.hero.modifier.DamageGroups.profileDamage(burnProfile, 0.0F),
                        burnProfile);
            }
        }
    }

    private LivingEntity getLiving(EntityDataAccessor<Integer> accessor)
    {
        Entity entity = level().getEntity(entityData.get(accessor));
        return entity instanceof LivingEntity living ? living : null;
    }

    public LivingEntity getCaster()
    {
        return getLiving(CASTER_ID);
    }

    public LivingEntity getTarget()
    {
        return getLiving(TARGET_ID);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        tag.putInt("Caster", entityData.get(CASTER_ID));
        tag.putInt("Target", entityData.get(TARGET_ID));
        tag.putString("BurnProfile", burnProfile.toString());
        tag.putInt("BurnFrequency", burnFrequency);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        entityData.set(CASTER_ID, tag.getInt("Caster"));
        entityData.set(TARGET_ID, tag.getInt("Target"));
        try
        {
            burnProfile = tag.contains("BurnProfile")
                    ? JsonParser.parseString(tag.getString("BurnProfile")).getAsJsonObject() : new JsonObject();
        }
        catch (RuntimeException ignored)
        {
            burnProfile = new JsonObject();
        }
        burnFrequency = Math.max(1, tag.getInt("BurnFrequency"));
    }
}
