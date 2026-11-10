package com.fiskmods.heroes.common.entity.projectile;

import com.fiskmods.heroes.common.entity.ModEntities;
import com.fiskmods.heroes.common.hero.modifier.DamageGroups;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Runtime projectile for pack-defined, belt-carried gadgets. */
public final class EquipmentProjectileEntity extends ThrowableItemProjectile
{
    private static final EntityDataAccessor<String> GADGET = SynchedEntityData.defineId(
            EquipmentProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> CONFIG = SynchedEntityData.defineId(
            EquipmentProjectileEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SMOKING = SynchedEntityData.defineId(
            EquipmentProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> RICOCHET_BOUNCES = SynchedEntityData.defineId(
            EquipmentProjectileEntity.class, EntityDataSerializers.BYTE);

    private int fuseTicks;
    private int smokeTicks;
    private float grenadeSpin;
    private float previousGrenadeSpin;

    public EquipmentProjectileEntity(EntityType<? extends EquipmentProjectileEntity> type, Level level)
    {
        super(type, level);
    }

    public EquipmentProjectileEntity(Player owner, String gadget, JsonObject config, ItemStack displayItem, float yawOffset)
    {
        super(ModEntities.EQUIPMENT_PROJECTILE.get(), owner, owner.level());
        entityData.set(GADGET, gadget);
        entityData.set(CONFIG, config.toString());
        setItem(displayItem.copy());
        setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
        shootFromRotation(owner, owner.getXRot(), owner.getYRot() + yawOffset, 0.0F,
                velocityFor(gadget), 1.0F);
        if (isTimedGrenade(gadget))
        {
            fuseTicks = Math.max(1, number(config, "fuseTime", 40));
        }
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(GADGET, "");
        entityData.define(CONFIG, "{}");
        entityData.define(SMOKING, false);
        entityData.define(RICOCHET_BOUNCES, (byte) 0);
    }

    @Override
    protected Item getDefaultItem()
    {
        return Items.SNOWBALL;
    }

    public boolean isFreezeGrenade()
    {
        return isFreezeGrenade(gadget());
    }

    public boolean isTimedGrenade()
    {
        return isTimedGrenade(gadget());
    }

    public float grenadeSpin(float partialTick)
    {
        return net.minecraft.util.Mth.lerp(partialTick, previousGrenadeSpin, grenadeSpin);
    }

    private String gadget()
    {
        return entityData.get(GADGET);
    }

    private JsonObject config()
    {
        try
        {
            return JsonParser.parseString(entityData.get(CONFIG)).getAsJsonObject();
        }
        catch (RuntimeException ignored)
        {
            return new JsonObject();
        }
    }

    private static boolean isTimedGrenade(String gadget)
    {
        return gadget.endsWith(":grenade");
    }

    private static boolean isFreezeGrenade(String gadget)
    {
        return gadget.endsWith(":freeze_grenade");
    }

    /** Launch speeds match the original projectile classes (EntityBatarang, EntityThrowingStar,
     * AbstractEntityWeb and EntityGrenade). */
    private static float velocityFor(String gadget)
    {
        if (gadget.endsWith(":batarang")) return 3.5F;
        if (gadget.endsWith(":throwing_star")) return 2.5F;
        if (isFreezeGrenade(gadget) || gadget.endsWith(":smoke_pellet")) return 1.25F;
        if (isTimedGrenade(gadget)) return 1.75F;
        if (gadget.endsWith(":sticky_web") || gadget.endsWith(":impact_web")
                || gadget.endsWith(":rapid_webs") || gadget.endsWith(":ricochet_web")) return 7.5F;
        return 1.75F;
    }

    @Override
    protected float getGravity()
    {
        if (isFreezeGrenade(gadget())) return 0.05F;
        if (isTimedGrenade(gadget())) return 0.075F;
        if (gadget().endsWith(":smoke_pellet")) return 0.025F;
        return 0.03F;
    }

    @Override
    public void tick()
    {
        if (level().isClientSide)
        {
            previousGrenadeSpin = grenadeSpin;
            if (isTimedGrenade()) grenadeSpin -= (float) getDeltaMovement().length() * 20.0F;
            super.tick();
            return;
        }

        if (entityData.get(SMOKING))
        {
            emitSmoke();
            if (--smokeTicks <= 0) discard();
            return;
        }

        if (fuseTicks > 0 && --fuseTicks == 0)
        {
            if (isFreezeGrenade(gadget())) detonateFreeze(position()); // Legacy saved armed grenade.
            else detonate();
            return;
        }

        super.tick();
        if (tickCount > 600) discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit)
    {
        if (level().isClientSide) return;
        String id = gadget();
        if (isFreezeGrenade(id))
        {
            detonateFreeze(hit.getLocation());
            return;
        }
        if (isTimedGrenade(id))
        {
            bounceOrDetonate(hit.getLocation(), null);
            return;
        }
        if (id.endsWith(":smoke_pellet"))
        {
            startSmoke();
            return;
        }

        Entity target = hit.getEntity();
        if (target instanceof LivingEntity living)
        {
            JsonObject properties = config();
            JsonObject profile = child(properties, "damageProfile");
            float damage = DamageGroups.profileDamage(profile, 0.0F);
            if (damage > 0.0F && getOwner() instanceof LivingEntity attacker)
            {
                DamageGroups.withDamageProfile(profile, () -> living.hurt(
                        attacker instanceof Player player ? damageSources().playerAttack(player) : damageSources().mobAttack(attacker), damage));
            }

            if (id.endsWith(":sticky_web") || id.endsWith(":impact_web") || id.endsWith(":rapid_webs")
                    || id.endsWith(":ricochet_web"))
            {
                int duration = Math.max(20, number(properties, "dissolveTime", 100));
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, duration, 4));
            }
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit)
    {
        if (level().isClientSide) return;
        if (gadget().endsWith(":ricochet_web"))
        {
            if (!ricochet(hit)) discard();
        }
        else if (isFreezeGrenade(gadget()))
        {
            var face = hit.getDirection();
            detonateFreeze(hit.getLocation().add(face.getStepX() * 0.02D,
                    face.getStepY() * 0.02D, face.getStepZ() * 0.02D));
        }
        else if (isTimedGrenade(gadget()))
        {
            bounceOrDetonate(hit.getLocation(), hit.getDirection());
        }
        else if (gadget().endsWith(":smoke_pellet"))
        {
            startSmoke();
        }
        else
        {
            discard();
        }
    }

    /** The original Ricochet Web reflects once from a face with air immediately behind it. */
    private boolean ricochet(BlockHitResult hit)
    {
        if (entityData.get(RICOCHET_BOUNCES) > 0) return false;

        net.minecraft.core.Direction face = hit.getDirection();
        if (!level().getBlockState(hit.getBlockPos().relative(face)).isAir()) return false;

        Vec3 velocity = getDeltaMovement();
        double x = face.getStepX() != 0 ? -velocity.x : velocity.x;
        double y = face.getStepY() != 0 ? -velocity.y : velocity.y;
        double z = face.getStepZ() != 0 ? -velocity.z : velocity.z;
        setDeltaMovement(x * 0.6D, y * 0.6D, z * 0.6D);
        Vec3 point = hit.getLocation().add(face.getStepX() * 0.02D, face.getStepY() * 0.02D, face.getStepZ() * 0.02D);
        setPos(point.x, point.y, point.z);
        entityData.set(RICOCHET_BOUNCES, (byte) 1);

        if (level() instanceof ServerLevel serverLevel)
        {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    point.x, point.y, point.z, 4, 0.08D, 0.08D, 0.08D, 0.01D);
        }
        com.fiskmods.heroes.common.sound.SHSounds.play(this,
                new net.minecraft.resources.ResourceLocation("fiskheroes", "entity_web_bounce"),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.9F + random.nextFloat() * 0.5F);
        return true;
    }

    /** Timed grenades bounce with damped velocity, while instant variants explode on contact. */
    private void bounceOrDetonate(Vec3 contact, @javax.annotation.Nullable net.minecraft.core.Direction face)
    {
        if (config().has("isInstant") && config().get("isInstant").getAsBoolean())
        {
            setPos(contact.x, contact.y, contact.z);
            detonate();
            return;
        }
        Vec3 velocity = getDeltaMovement();
        if (face == null)
        {
            velocity = velocity.scale(-0.3D);
        }
        else
        {
            velocity = new Vec3(face.getStepX() != 0 ? -velocity.x : velocity.x,
                    face.getStepY() != 0 ? -velocity.y : velocity.y,
                    face.getStepZ() != 0 ? -velocity.z : velocity.z).scale(0.3D);
            contact = contact.add(face.getStepX() * 0.02D, face.getStepY() * 0.02D, face.getStepZ() * 0.02D);
        }
        if (velocity.lengthSqr() < 0.001D) velocity = Vec3.ZERO;
        setPos(contact.x, contact.y, contact.z);
        setDeltaMovement(velocity);
        if (Boolean.getBoolean("fiskheroes.ciEquipmentDiagnostics") && tickCount < 8)
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Timed grenade bounced: velocity={}", velocity);
    }

    private void detonateFreeze(Vec3 center)
    {
        if (level() instanceof ServerLevel serverLevel)
        {
            JsonObject properties = config();
            JsonObject profile = child(properties, "damageProfile");
            float damage = Math.max(0.0F, DamageGroups.profileDamage(profile, 16.0F));
            boolean affectsOwner = properties.has("affectsUser") && properties.get("affectsUser").getAsBoolean();
            if (Boolean.getBoolean("fiskheroes.ciFreezeGrenadeDiagnostics"))
                com.fiskmods.heroes.FiskHeroes.LOGGER.info("Freeze grenade impact: center={}, owner={}", center, getOwner());
            FreezeBurst.detonate(serverLevel, this, getOwner(), center, 3.0D, damage, profile, affectsOwner, false);
        }
        discard();
    }

    private void detonate()
    {
        if (!(level() instanceof ServerLevel serverLevel))
        {
            discard();
            return;
        }

        JsonObject properties = config();
        float radius = Math.max(0.1F, Math.min(64.0F, decimal(properties, "radius", 3.0F)));
        JsonObject profile = child(properties, "damageProfile");
        float maxDamage = DamageGroups.profileDamage(profile, 30.0F);
        boolean affectsOwner = properties.has("affectsUser") && properties.get("affectsUser").getAsBoolean();
        if (Boolean.getBoolean("fiskheroes.ciEquipmentDiagnostics"))
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Grenade explosion: center={}, radius={}", position(), radius);
        var bounds = getBoundingBox().inflate(radius);

        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        serverLevel.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.PLAYERS, 4.0F, 0.7F * (1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F));
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, bounds))
        {
            if (target == getOwner() && !affectsOwner) continue;
            double distance = target.distanceTo(this);
            if (distance > radius) continue;

            // Vanilla's block-density sampling gives partial exposure at edges and zero
            // behind a solid wall, as in the original GrenadeExplosion.
            double exposure = net.minecraft.world.level.Explosion.getSeenPercent(position(), target);
            double strength = (1.0D - distance / radius) * exposure;
            if (strength <= 0.0D) continue;
            float damage = maxDamage * (float) strength;
            if (damage > 0.0F && getOwner() instanceof LivingEntity attacker)
            {
                DamageGroups.withDamageProfile(profile, () -> target.hurt(
                        attacker instanceof Player player ? damageSources().playerAttack(player) : damageSources().mobAttack(attacker), damage));
            }
            Vec3 direction = target.getEyePosition().subtract(position());
            if (direction.lengthSqr() > 1.0E-8D)
            {
                Vec3 impulse = direction.normalize().scale(strength);
                target.push(impulse.x, impulse.y, impulse.z);
                target.hurtMarked = true; // synchronize the velocity to multiplayer clients
            }

        }

        discard();
    }

    private void emitSmoke()
    {
        if (level() instanceof ServerLevel serverLevel)
        {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    getX() + (random.nextDouble() - 0.5D) * 2.0D, getY() + random.nextDouble(),
                    getZ() + (random.nextDouble() - 0.5D) * 2.0D, 3, 0.15D, 0.1D, 0.15D, 0.015D);
        }
    }

    private void startSmoke()
    {
        entityData.set(SMOKING, true);
        smokeTicks = 80;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
    }

    private static JsonObject child(JsonObject object, String key)
    {
        return object.has(key) && object.get(key).isJsonObject() ? object.getAsJsonObject(key) : new JsonObject();
    }

    private static float decimal(JsonObject object, String key, float fallback)
    {
        try
        {
            float value = object.has(key) ? object.get(key).getAsFloat() : fallback;
            return Float.isFinite(value) ? value : fallback;
        }
        catch (RuntimeException ignored)
        {
            return fallback;
        }
    }

    private static int number(JsonObject object, String key, int fallback)
    {
        try
        {
            return object.has(key) ? object.get(key).getAsInt() : fallback;
        }
        catch (RuntimeException ignored)
        {
            return fallback;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putString("Gadget", gadget());
        tag.putString("Config", entityData.get(CONFIG));
        tag.putInt("FuseTicks", fuseTicks);
        tag.putInt("SmokeTicks", smokeTicks);
        tag.putByte("RicochetBounces", entityData.get(RICOCHET_BOUNCES));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        entityData.set(GADGET, tag.getString("Gadget"));
        entityData.set(CONFIG, tag.getString("Config"));
        fuseTicks = tag.getInt("FuseTicks");
        smokeTicks = tag.getInt("SmokeTicks");
        entityData.set(RICOCHET_BOUNCES, tag.getByte("RicochetBounces"));
        entityData.set(SMOKING, smokeTicks > 0);
    }
}
