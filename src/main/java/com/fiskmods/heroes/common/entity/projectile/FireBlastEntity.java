package com.fiskmods.heroes.common.entity.projectile;

import com.fiskmods.heroes.common.entity.ModEntities;
import com.fiskmods.heroes.common.hero.modifier.DamageGroups;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Physical fireball corresponding to the original EntityFireBlast. */
public final class FireBlastEntity extends ThrowableProjectile
{
    private String damageProfile = "{}";
    private float damage = 6.0F;
    private float blastRadius = 2.5F;

    public FireBlastEntity(EntityType<? extends FireBlastEntity> type, Level level)
    {
        super(type, level);
    }

    public FireBlastEntity(LivingEntity shooter, JsonElement profile, float damage, float blastRadius)
    {
        super(ModEntities.FIRE_BLAST.get(), shooter, shooter.level());
        this.damageProfile = profile == null ? "{}" : profile.toString();
        this.damage = Math.max(0.0F, damage);
        this.blastRadius = Math.max(0.0F, blastRadius);
        setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, 1.5F, 0.0F);
    }

    @Override
    protected void defineSynchedData()
    {
    }

    @Override
    protected float getGravity()
    {
        return 0.01F;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (level().isClientSide)
        {
            level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                    getX(), getY(), getZ(), 0.0D, 0.01D, 0.0D);
            level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
        else if (tickCount > 1200)
        {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit)
    {
        if (level() instanceof ServerLevel serverLevel)
        {
            Vec3 position = hit.getLocation();
            if (hit.getEntity() instanceof LivingEntity target && getOwner() instanceof LivingEntity shooter)
            {
                DamageGroups.applyProfileDamage(target, shooter,
                        shooter.damageSources().indirectMagic(this, shooter), damage, profile());
            }
            explode(serverLevel, position, hit.getEntity());
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit)
    {
        if (level() instanceof ServerLevel serverLevel) explode(serverLevel, hit.getLocation(), null);
        discard();
    }

    private void explode(ServerLevel level, Vec3 position, net.minecraft.world.entity.Entity directHit)
    {
        if (blastRadius > 0.0F && damage > 0.0F)
        {
            var bounds = new net.minecraft.world.phys.AABB(position, position).inflate(blastRadius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                    entity -> entity != getOwner() && entity != directHit && entity.isAlive()))
            {
                double distance = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(position);
                float falloff = (float) Math.max(0.0D, 1.0D - Math.min(blastRadius, distance) / blastRadius);
                float scaledDamage = Math.max(1.0F, damage * falloff);
                if (getOwner() instanceof LivingEntity shooter)
                {
                    DamageGroups.applyProfileDamage(target, shooter,
                            shooter.damageSources().indirectMagic(this, shooter), scaledDamage, profile());
                }
            }
        }

        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                position.x, position.y, position.z, 24, 0.35D, 0.35D, 0.35D, 0.04D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                position.x, position.y, position.z, 10, 0.2D, 0.2D, 0.2D, 0.02D);
        level.playSound(null, position.x, position.y, position.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 1.5F, 0.9F);
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
        tag.putFloat("BlastRadius", blastRadius);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        damageProfile = tag.getString("DamageProfile");
        damage = tag.contains("Damage") ? tag.getFloat("Damage") : 6.0F;
        blastRadius = tag.contains("BlastRadius") ? tag.getFloat("BlastRadius") : 2.5F;
    }
}
