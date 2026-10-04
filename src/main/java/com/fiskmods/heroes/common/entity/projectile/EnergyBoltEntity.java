package com.fiskmods.heroes.common.entity.projectile;

import com.fiskmods.heroes.common.entity.ModEntities;
import com.fiskmods.heroes.common.hero.modifier.DamageGroups;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Physical, pack-colored energy bolt corresponding to the original EntityEnergyBolt. */
public final class EnergyBoltEntity extends ThrowableProjectile
{
    private String damageProfile = "{}";
    private float damage = 6.0F;
    private boolean explosive;

    public EnergyBoltEntity(EntityType<? extends EnergyBoltEntity> type, Level level)
    {
        super(type, level);
    }

    public EnergyBoltEntity(LivingEntity shooter, JsonElement profile, float damage, boolean explosive)
    {
        super(ModEntities.ENERGY_BOLT.get(), shooter, shooter.level());
        this.damageProfile = profile == null ? "{}" : profile.toString();
        this.damage = Math.max(0.0F, damage);
        this.explosive = explosive;
        setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, 4.0F, 0.0F);
    }

    @Override
    protected void defineSynchedData()
    {
    }

    @Override
    protected float getGravity()
    {
        return 0.001F;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!level().isClientSide && tickCount > 120) discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit)
    {
        if (level() instanceof ServerLevel serverLevel)
        {
            if (hit.getEntity() instanceof LivingEntity target && getOwner() instanceof LivingEntity shooter)
            {
                JsonElement profile = profile();
                DamageGroups.withDamageProfile(profile, () -> target.hurt(
                        shooter.damageSources().indirectMagic(this, shooter), damage));
            }
            impact(serverLevel, hit.getLocation());
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit)
    {
        if (level() instanceof ServerLevel serverLevel) impact(serverLevel, hit.getLocation());
        discard();
    }

    private void impact(ServerLevel level, net.minecraft.world.phys.Vec3 position)
    {
        if (explosive)
        {
            level.explode(getOwner(), position.x, position.y, position.z, 1.25F, false,
                    Level.ExplosionInteraction.NONE);
        }
        else
        {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    position.x, position.y, position.z, 8, 0.05D, 0.05D, 0.05D, 0.01D);
        }
    }

    private JsonElement profile()
    {
        try
        {
            return JsonParser.parseString(damageProfile);
        }
        catch (RuntimeException ignored)
        {
            return null;
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putString("DamageProfile", damageProfile);
        tag.putFloat("Damage", damage);
        tag.putBoolean("Explosive", explosive);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        damageProfile = tag.getString("DamageProfile");
        damage = tag.contains("Damage") ? tag.getFloat("Damage") : 6.0F;
        explosive = tag.getBoolean("Explosive");
    }
}
