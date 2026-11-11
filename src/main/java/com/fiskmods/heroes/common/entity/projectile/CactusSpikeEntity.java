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

/** Small, fixed-damage spike emitted by the Cactus Arrow's impact burst. */
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
        if (!(hit.getEntity() instanceof LivingEntity target) || !(level() instanceof ServerLevel))
        {
            discard();
            return;
        }

        // The original spike sometimes reset hurt resistance before applying its small hit,
        // which lets several spikes in one cactus burst connect instead of collapsing into one.
        if (random.nextBoolean()) target.invulnerableTime = 0;
        if (getOwner() instanceof LivingEntity owner)
        {
            DamageGroups.applyProfileDamage(target, owner,
                    level().damageSources().thrown(this, owner),
                    DamageGroups.profileDamage(damageProfile, DAMAGE), damageProfile);
        }
        discard();
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
        return profile;
    }
}
