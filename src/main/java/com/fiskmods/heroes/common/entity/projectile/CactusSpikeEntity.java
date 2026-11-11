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
    protected void onHitEntity(EntityHitResult hit)
    {
        if (!(level() instanceof ServerLevel)) return;
        var target = hit.getEntity();
        // The original sometimes clears hurt resistance and only consumes the spike on a successful hit.
        if (random.nextBoolean()) target.invulnerableTime = 0;
        var owner = getOwner();
        var source = level().damageSources().thrown(this, owner);
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
