package com.fiskmods.heroes.common.spell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Flying Mysterio illusion that orbits its target and fires short bursts of illusion shots. */
public final class IllusionDroneEntity extends Mob implements Enemy
{
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(IllusionDroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(IllusionDroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ORBIT_OFFSET = SynchedEntityData.defineId(IllusionDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORBIT_RADIUS = SynchedEntityData.defineId(IllusionDroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SHOOTING = SynchedEntityData.defineId(IllusionDroneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TARGETLESS = SynchedEntityData.defineId(IllusionDroneEntity.class, EntityDataSerializers.BOOLEAN);
    private JsonObject damageProfile = new JsonObject();
    private int cloakTicks;
    private int shootCooldown;
    private int shootTicks;

    public IllusionDroneEntity(EntityType<? extends IllusionDroneEntity> type, Level level)
    {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public IllusionDroneEntity(EntityType<? extends IllusionDroneEntity> type, Level level,
            LivingEntity owner, LivingEntity target, float offset, float radius, JsonObject profile)
    {
        this(type, level);
        entityData.set(OWNER_ID, owner.getId());
        entityData.set(TARGET_ID, target.getId());
        entityData.set(ORBIT_OFFSET, offset);
        entityData.set(ORBIT_RADIUS, Math.max(0.5F, radius));
        damageProfile = profile.deepCopy();
        setPos(target.getX(), target.getY() + 1.0D, target.getZ());
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 15.0D).add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 64.0D).add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(OWNER_ID, -1);
        entityData.define(TARGET_ID, -1);
        entityData.define(ORBIT_OFFSET, 0.0F);
        entityData.define(ORBIT_RADIUS, 5.0F);
        entityData.define(SHOOTING, false);
        entityData.define(TARGETLESS, false);
        JsonObject defaultProfile = new JsonObject();
        defaultProfile.addProperty("damage", 1.5F);
        JsonObject types = new JsonObject();
        types.addProperty("BULLET", 1.0F);
        defaultProfile.add("types", types);
        damageProfile = defaultProfile;
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
            entityData.set(TARGETLESS, true);
            entityData.set(SHOOTING, false);
            if (++cloakTicks > 10) discard();
            return;
        }

        cloakTicks = Math.max(0, cloakTicks - 1);
        setPos(orbitPosition(target));
        faceTarget(target);
        setDeltaMovement(Vec3.ZERO);

        if (level().isClientSide) return;
        if (shootTicks > 0) shootTicks--;
        else if (shootCooldown > 0) shootCooldown--;
        boolean visibleTarget = target.isAlive() && !target.isInvisible() && hasLineOfSight(target)
                && distanceTo(target) < 12.0F;
        if (shootCooldown == 0 && shootTicks == 0 && visibleTarget)
        {
            shootCooldown = 10 + random.nextInt(30);
            shootTicks = 40;
            entityData.set(SHOOTING, true);
        }
        if (shootTicks == 0 || !visibleTarget) entityData.set(SHOOTING, false);
        if (entityData.get(SHOOTING) && tickCount % 3 == 0) fireAtTarget(target, owner);
    }

    private Vec3 orbitPosition(LivingEntity target)
    {
        double angle = Math.toRadians(entityData.get(ORBIT_OFFSET) + tickCount * 1.5D);
        double radius = entityData.get(ORBIT_RADIUS);
        double y = target.getY() + Math.max(1.0D, target.getBbHeight() * 0.65D);
        return new Vec3(target.getX() + Math.cos(angle) * radius, y, target.getZ() + Math.sin(angle) * radius);
    }

    private void faceTarget(LivingEntity target)
    {
        Vec3 delta = target.getEyePosition().subtract(position());
        setYRot((float) (Math.atan2(delta.z, delta.x) * (180.0D / Math.PI)) - 90.0F);
        setXRot((float) (-(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)) * (180.0D / Math.PI))));
    }

    private void fireAtTarget(LivingEntity target, LivingEntity owner)
    {
        Vec3 start = getEyePosition();
        Vec3 direction = target.getEyePosition().subtract(start).normalize();
        Vec3 end = start.add(direction.scale(64.0D));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level(), this, start, end,
                getBoundingBox().expandTowards(direction.scale(64.0D)).inflate(1.0D), entity -> entity == target);
        if (hit != null && target.invulnerableTime < 15)
        {
            target.invulnerableTime = 0;
            float damage = com.fiskmods.heroes.common.hero.modifier.DamageGroups.profileDamage(damageProfile, 1.5F);
            com.fiskmods.heroes.common.hero.modifier.DamageGroups.withDamageProfile(damageProfile,
                    () -> target.hurt(level().damageSources().mobAttack(owner), damage));
            if (level() instanceof ServerLevel server)
            {
                server.sendParticles(ParticleTypes.CRIT, hit.getLocation().x, hit.getLocation().y,
                        hit.getLocation().z, 5, 0.15D, 0.15D, 0.15D, 0.03D);
            }
        }
        if (level() instanceof ServerLevel server)
        {
            server.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.5F,
                    1.2F + random.nextFloat() * 0.2F);
        }
    }

    private boolean hasSpellcasting(LivingEntity owner)
    {
        var hero = com.fiskmods.heroes.common.hero.HeroTracker.getHeroType(owner);
        return hero != null && !ModifierSpellcasting.getSpells(hero).getSpells().isEmpty();
    }

    public LivingEntity getOwner()
    {
        Entity entity = level().getEntity(entityData.get(OWNER_ID));
        return entity instanceof LivingEntity living ? living : null;
    }

    public LivingEntity getTarget()
    {
        Entity entity = level().getEntity(entityData.get(TARGET_ID));
        return entity instanceof LivingEntity living ? living : null;
    }

    public boolean isShooting()
    {
        return entityData.get(SHOOTING);
    }

    public boolean isTargetless()
    {
        return entityData.get(TARGETLESS);
    }

    public void setOrbitRadius(float radius)
    {
        entityData.set(ORBIT_RADIUS, Math.max(0.5F, radius));
    }

    @Override
    public MobType getMobType()
    {
        return MobType.UNDEFINED;
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity other)
    {
        LivingEntity owner = getOwner();
        return other == owner || other instanceof IllusionDroneEntity || owner != null && owner.isAlliedTo(other)
                || super.isAlliedTo(other);
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        if (level().isClientSide || isInvulnerableTo(source)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt("OwnerId", entityData.get(OWNER_ID));
        tag.putInt("TargetId", entityData.get(TARGET_ID));
        tag.putFloat("OrbitOffset", entityData.get(ORBIT_OFFSET));
        tag.putFloat("OrbitRadius", entityData.get(ORBIT_RADIUS));
        tag.putString("DamageProfile", damageProfile.toString());
        tag.putInt("ShootCooldown", shootCooldown);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        entityData.set(OWNER_ID, tag.getInt("OwnerId"));
        entityData.set(TARGET_ID, tag.getInt("TargetId"));
        entityData.set(ORBIT_OFFSET, tag.getFloat("OrbitOffset"));
        entityData.set(ORBIT_RADIUS, Math.max(0.5F, tag.getFloat("OrbitRadius")));
        if (tag.contains("DamageProfile", net.minecraft.nbt.Tag.TAG_STRING))
        {
            try
            {
                var parsed = JsonParser.parseString(tag.getString("DamageProfile"));
                if (parsed.isJsonObject()) damageProfile = parsed.getAsJsonObject();
            }
            catch (RuntimeException ignored)
            {
                // Keep the default bullet profile when saved spell data is malformed.
            }
        }
        shootCooldown = tag.getInt("ShootCooldown");
    }
}
