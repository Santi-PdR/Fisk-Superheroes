package com.fiskmods.heroes.common.entity.projectile;

import com.fiskmods.heroes.common.entity.ModEntities;
import com.fiskmods.heroes.common.hero.modifier.DamageGroups;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Expanding, short-lived sound wave used by Canary's cry. */
public final class SonicWaveEntity extends ThrowableProjectile
{
    private String damageProfile = "{}";
    private float damage = 7.0F;
    private float knockback = 0.025F;
    private boolean breakGlass;

    public SonicWaveEntity(EntityType<? extends SonicWaveEntity> type, Level level)
    {
        super(type, level);
    }

    public SonicWaveEntity(LivingEntity shooter, JsonElement profile, float damage, float knockback,
            boolean breakGlass)
    {
        super(ModEntities.SONIC_WAVE.get(), shooter, shooter.level());
        this.damageProfile = profile == null ? "{}" : profile.toString();
        this.damage = Math.max(0.0F, damage);
        this.knockback = Math.max(0.0F, knockback);
        this.breakGlass = breakGlass;
        setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, 1.0F, 0.0F);
    }

    @Override
    protected void defineSynchedData()
    {
    }

    @Override
    protected float getGravity()
    {
        return 0.0F;
    }

    @Override
    public void tick()
    {
        super.tick();
        if (tickCount >= 15)
        {
            discard();
            return;
        }

        float radius = getRadius(0.0F);
        if (level().isClientSide)
        {
            for (int i = 0; i < 5; ++i)
            {
                double angle = (tickCount * 0.9D) + (Math.PI * 2.0D * i / 5.0D);
                double sideX = -getLookAngle().z;
                double sideZ = getLookAngle().x;
                double sideLength = Math.max(0.001D, Math.sqrt(sideX * sideX + sideZ * sideZ));
                double x = getX() + sideX / sideLength * Math.cos(angle) * radius;
                double y = getY() + Math.sin(angle) * radius;
                double z = getZ() + sideZ / sideLength * Math.cos(angle) * radius;
                level().addParticle(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM,
                        x, y, z, 0.0D, 0.0D, 0.0D);
            }
        }
        else if (level() instanceof ServerLevel serverLevel)
        {
            applyWave(serverLevel, radius);
            if (breakGlass) breakGlass(serverLevel, radius);
        }
    }

    private void applyWave(ServerLevel level, float radius)
    {
        Entity owner = getOwner();
        var affected = getBoundingBox().inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, affected,
                entity -> entity != owner && entity.isAlive()))
        {
            float damageScale = Math.max(0.0F, 1.0F - tickCount / 25.0F);
            DamageGroups.applyProfileDamage(target, owner instanceof LivingEntity living ? living : null,
                    level.damageSources().sonicBoom(owner), damage * damageScale, profile());

            if (knockback > 0.0F)
            {
                double volume = target.getBbWidth() * target.getBbWidth() * target.getBbHeight();
                double force = knockback / Math.max(0.25D, Math.sqrt(volume))
                        * (1.0D - tickCount / 15.0D);
                Vec3 away = target.position().subtract(xo, yo, zo).normalize();
                target.setDeltaMovement(target.getDeltaMovement().add(away.scale(force)));
                target.hurtMarked = true;
            }
        }
    }

    private void breakGlass(ServerLevel level, float radius)
    {
        Entity owner = getOwner();
        if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)
                || owner instanceof Player player && !player.mayBuild())
        {
            return;
        }

        var bounds = getBoundingBox().inflate(radius);
        var min = net.minecraft.core.BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        var max = net.minecraft.core.BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(min, max))
        {
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof AbstractGlassBlock && level.random.nextInt(10) == 0)
            {
                level.destroyBlock(pos, true, owner);
            }
        }
    }

    public float getRadius(float partialTick)
    {
        float time = tickCount + partialTick;
        return Math.max(0.125F, time / (4.0F + time * time / 20.0F) - 0.25F);
    }

    public float getOpacity(float partialTick)
    {
        return 0.2F * Math.max(0.0F, 1.0F - (tickCount + partialTick) / 15.0F);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit)
    {
        discard();
    }

    @Override
    protected void onHitEntity(net.minecraft.world.phys.EntityHitResult hit)
    {
        // The wave passes through targets; damage is applied over its expanding lifetime.
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
        tag.putFloat("Knockback", knockback);
        tag.putBoolean("BreakGlass", breakGlass);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        damageProfile = tag.getString("DamageProfile");
        damage = tag.getFloat("Damage");
        knockback = tag.getFloat("Knockback");
        breakGlass = tag.getBoolean("BreakGlass");
    }
}
