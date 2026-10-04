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

    private int fuseTicks;
    private int smokeTicks;

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
                isExplosive(gadget) ? 1.15F : 2.0F, 1.0F);
        if (isExplosive(gadget))
        {
            fuseTicks = number(config, "fuseTime", 40);
        }
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(GADGET, "");
        entityData.define(CONFIG, "{}");
        entityData.define(SMOKING, false);
    }

    @Override
    protected Item getDefaultItem()
    {
        return Items.SNOWBALL;
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

    private static boolean isExplosive(String gadget)
    {
        return gadget.endsWith(":grenade") || gadget.endsWith(":freeze_grenade");
    }

    @Override
    protected float getGravity()
    {
        return 0.03F;
    }

    @Override
    public void tick()
    {
        if (level().isClientSide)
        {
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
            detonate();
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
        if (isExplosive(id))
        {
            armOnImpact();
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
        if (isExplosive(gadget()))
        {
            armOnImpact();
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

    private void armOnImpact()
    {
        if (config().has("isInstant") && config().get("isInstant").getAsBoolean())
        {
            detonate();
            return;
        }
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        if (fuseTicks <= 0) fuseTicks = number(config(), "fuseTime", 40);
    }

    private void detonate()
    {
        if (!(level() instanceof ServerLevel serverLevel))
        {
            discard();
            return;
        }

        JsonObject properties = config();
        float radius = Math.max(1.0F, number(properties, "radius", gadget().endsWith(":freeze_grenade") ? 4 : 8));
        JsonObject profile = child(properties, "damageProfile");
        float maxDamage = DamageGroups.profileDamage(profile, gadget().endsWith(":freeze_grenade") ? 16.0F : 30.0F);
        boolean affectsOwner = properties.has("affectsUser") && properties.get("affectsUser").getAsBoolean();
        var bounds = getBoundingBox().inflate(radius);

        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, bounds))
        {
            if (target == getOwner() && !affectsOwner) continue;
            double distance = target.distanceTo(this);
            if (distance > radius) continue;

            float damage = maxDamage * (float) (1.0D - distance / radius);
            if (damage > 0.0F && getOwner() instanceof LivingEntity attacker)
            {
                DamageGroups.withDamageProfile(profile, () -> target.hurt(
                        attacker instanceof Player player ? damageSources().playerAttack(player) : damageSources().mobAttack(attacker), damage));
            }

            if (gadget().endsWith(":freeze_grenade"))
            {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        entityData.set(GADGET, tag.getString("Gadget"));
        entityData.set(CONFIG, tag.getString("Config"));
        fuseTicks = tag.getInt("FuseTicks");
        smokeTicks = tag.getInt("SmokeTicks");
        entityData.set(SMOKING, smokeTicks > 0);
    }
}
