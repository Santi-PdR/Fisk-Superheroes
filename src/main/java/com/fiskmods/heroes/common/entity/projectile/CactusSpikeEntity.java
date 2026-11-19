package com.fiskmods.heroes.common.entity.projectile;

import com.fiskmods.heroes.common.entity.ModEntities;
import com.fiskmods.heroes.common.hero.modifier.DamageGroups;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** Spike projectile shared by Señor Cactus and Cactus Arrow. */
public final class CactusSpikeEntity extends ThrowableItemProjectile
{
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> SPIKE_DAMAGE =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    com.fiskmods.heroes.FiskHeroes.id("cactus_spike"));
    private static final int MAX_AGE = 200;
    private static final float DAMAGE = 1.5F;
    private static final JsonObject DAMAGE_PROFILE = createDamageProfile();
    private JsonObject damageProfile = DAMAGE_PROFILE.deepCopy();

    public CactusSpikeEntity(EntityType<? extends CactusSpikeEntity> type, Level level)
    {
        super(type, level);
    }

    public CactusSpikeEntity(LivingEntity owner)
    {
        super(ModEntities.CACTUS_SPIKE.get(), owner, owner.level());
        setItem(Items.CACTUS.getDefaultInstance());
        setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
    }

    /** Recreate the 1.7 throwable constructor for one aimed or radial spike. */
    public void launchLikeLegacy(LivingEntity shooter, float pitch, float yaw)
    {
        float radians = yaw / 180.0F * (float) Math.PI;
        setPos(shooter.getX() - net.minecraft.util.Mth.cos(radians) * 0.16F,
                shooter.getEyeY() - 0.10000000149011612D,
                shooter.getZ() - net.minecraft.util.Mth.sin(radians) * 0.16F);
        var motion = legacyLaunchVelocity(pitch, yaw, random);
        setDeltaMovement(motion);
        setYRot((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
        setXRot((float) Math.toDegrees(Math.atan2(motion.y, motion.horizontalDistance())));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    /** Vanilla 1.7 normalizes first, then adds Gaussian spread, then scales by 4.5. */
    public static net.minecraft.world.phys.Vec3 legacyLaunchVelocity(float pitch, float yaw, net.minecraft.util.RandomSource random)
    {
        float yr = yaw / 180.0F * (float) Math.PI;
        float pr = pitch / 180.0F * (float) Math.PI;
        double x = -net.minecraft.util.Mth.sin(yr) * net.minecraft.util.Mth.cos(pr) * 0.4F;
        double y = -net.minecraft.util.Mth.sin(pr) * 0.4F;
        double z = net.minecraft.util.Mth.cos(yr) * net.minecraft.util.Mth.cos(pr) * 0.4F;
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        double spread = 0.007499999832361937D;
        return new net.minecraft.world.phys.Vec3(
                (x / length + random.nextGaussian() * spread) * 4.5D,
                (y / length + random.nextGaussian() * spread) * 4.5D,
                (z / length + random.nextGaussian() * spread) * 4.5D);
    }

    @Override
    protected Item getDefaultItem()
    {
        return Items.CACTUS;
    }

    @Override
    protected float getGravity()
    {
        return 0.0175F;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!level().isClientSide && tickCount > MAX_AGE) discard();
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity target)
    {
        // In 1.7 EntityThrowable ignores only its thrower for the first four
        // airborne ticks. Modern projectiles instead wait until they leave the
        // owner's vehicle and can then hit the owner immediately, even at age 2.
        return target.canBeHitByProjectile() && (target != getOwner() || tickCount >= 5);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit)
    {
        if (!(level() instanceof ServerLevel)) return;
        var target = hit.getEntity();
        // The original sometimes clears hurt resistance and only consumes the spike on a successful hit.
        if (random.nextBoolean()) target.invulnerableTime = 0;
        var owner = getOwner();
        var type = level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(SPIKE_DAMAGE);
        // The legacy spike source is an indirect, armor-blockable projectile,
        // attributed to its shooter and with the standard 0.3 exhaustion.
        var source = new net.minecraft.world.damagesource.DamageSource(type, this, owner);
        float damage = DamageGroups.profileDamage(damageProfile, DAMAGE);
        boolean accepted;
        if (target instanceof LivingEntity living)
        {
            accepted = DamageGroups.applyProfileDamage(living, owner instanceof LivingEntity ? (LivingEntity) owner : null,
                    source, damage, damageProfile);
        }
        else
        {
            boolean[] hitAccepted = { false };
            DamageGroups.withDamageProfile(damageProfile, () -> hitAccepted[0] = target.hurt(source, damage));
            accepted = hitAccepted[0];
        }
        if (accepted) discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit)
    {
        discard();
    }

    public void setDamageProfile(com.google.gson.JsonElement profile)
    {
        damageProfile = profile != null && profile.isJsonObject() ? profile.getAsJsonObject().deepCopy() : DAMAGE_PROFILE.deepCopy();
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putString("DamageProfile", damageProfile.toString());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.contains("DamageProfile", net.minecraft.nbt.Tag.TAG_STRING))
        {
            try { setDamageProfile(com.google.gson.JsonParser.parseString(tag.getString("DamageProfile"))); }
            catch (com.google.gson.JsonParseException ignored) { setDamageProfile(null); }
        }
    }

    private static JsonObject createDamageProfile()
    {
        JsonObject profile = new JsonObject();
        profile.addProperty("damage", DAMAGE);
        JsonObject types = new JsonObject();
        types.addProperty("CACTUS", 1.0D);
        types.addProperty("SHARP", 1.0D);
        profile.add("types", types);
        JsonObject properties = new JsonObject();
        properties.addProperty("REDUCE_KNOCKBACK", 0.75D);
        profile.add("properties", properties);
        return profile;
    }
}
