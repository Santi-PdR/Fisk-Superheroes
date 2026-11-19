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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
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
        Vec3 previousPosition = position();
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
        else if (tickCount < 11 && level() instanceof ServerLevel serverLevel)
        {
            // The final four ticks are visual fade only in the original wave.
            applyWave(serverLevel, radius, position().subtract(previousPosition));
            if (breakGlass) breakGlass(serverLevel, radius,
                    com.fiskmods.heroes.common.config.SHConfig.ALLOW_GRIEFING.get(),
                    com.fiskmods.heroes.common.config.SHConfig.ALLOW_SONIC_WAVE_GRIEFING.get());
        }
    }

    private void applyWave(ServerLevel level, float radius, Vec3 displacement)
    {
        Entity owner = getOwner();
        var affected = getBoundingBox().inflate(radius);
        for (Entity target : level.getEntities(this, affected,
                entity -> entity != owner && entity.isAlive() && !(entity instanceof SonicWaveEntity)))
        {
            if (target instanceof ThrownShieldEntity shield) shield.dislodgeBySonicWave();
            if (target instanceof IcicleEntity icicle) icicle.shatter();
            if (target.isRemoved()) continue;
            if (target instanceof LivingEntity living)
            {
                float damageScale = Math.max(0.0F, 1.0F - tickCount / 25.0F);
                DamageGroups.applyProfileDamage(living, owner instanceof LivingEntity attacker ? attacker : null,
                        level.damageSources().sonicBoom(owner), damage * damageScale, profile());
            }
            if (knockback > 0.0F)
            {
                double volume = target.getBbWidth() * target.getBbWidth() * target.getBbHeight();
                // Avoid infinite impulse for marker entities with a zero-sized hitbox.
                double force = knockback / Math.sqrt(Math.max(0.0001D, volume))
                        * (1.0D - tickCount / 15.0D);
                target.setDeltaMovement(target.getDeltaMovement().add(displacement.scale(force)));
                target.hurtMarked = true;
            }
        }
    }

    private void breakGlass(ServerLevel level, float radius, boolean allowGriefing, boolean allowSonicGriefing)
    {
        if (!breakGlass || !allowGriefing || !allowSonicGriefing
                || !level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)
                || !(getOwner() instanceof ServerPlayer player) || !player.mayBuild()) return;

        var bounds = getBoundingBox().inflate(radius);
        var min = net.minecraft.core.BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        var max = net.minecraft.core.BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);
        for (net.minecraft.core.BlockPos cursor : net.minecraft.core.BlockPos.betweenClosed(min, max))
        {
            var pos = cursor.immutable();
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, net.minecraft.core.Direction.UP, player.getMainHandItem())) continue;
            var state = level.getBlockState(pos);
            if (!(state.is(net.minecraftforge.common.Tags.Blocks.GLASS) || state.is(net.minecraftforge.common.Tags.Blocks.GLASS_PANES))
                    || state.getDestroySpeed(level, pos) < 0.0F) continue;
            // Legacy chance is one in ten times the rounded-up distance; clamp
            // zero distance so a wave centered in a block cannot call nextInt(0).
            int distance = Math.max(1, net.minecraft.util.Mth.ceil(Math.sqrt(distanceToSqr(
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D))));
            if (level.random.nextInt(10 * distance) == 0
                    && com.fiskmods.heroes.common.util.ProtectedBlockChanges.canBreak(player, pos, state))
                level.destroyBlock(pos, true, player);
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
