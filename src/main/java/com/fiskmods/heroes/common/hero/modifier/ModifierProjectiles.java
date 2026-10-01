package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Energy projection family: bolts, blasts, beams and eye lasers. All of them share the same shape
 * (aim, charge, fire, cool down); the power JSON tunes range, charge time and cooldown.
 */
class ModifierEnergyProjection extends Modifier
{
    ModifierEnergyProjection(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!(entity.level() instanceof ServerLevel level))
        {
            return;
        }

        int chargeTime = entry.getInt(PowerProperty.CHARGE_TIME);

        if (chargeTime > 0 && data.getData().get(Vars.BEAM_CHARGE) < chargeTime)
        {
            data.getData().set(Vars.BEAM_CHARGING, true);
            return;
        }

        fire(entity, entry, data, level, false);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        int chargeTime = entry.getInt(PowerProperty.CHARGE_TIME);
        float charge = data.getData().get(Vars.BEAM_CHARGE);

        if (data.getData().get(Vars.BEAM_CHARGING))
        {
            charge = Math.min(chargeTime, charge + 1.0F);
            data.getData().set(Vars.BEAM_CHARGE, charge);

            if (charge >= chargeTime && entity.level() instanceof ServerLevel level)
            {
                data.getData().set(Vars.BEAM_CHARGING, false);
                fire(entity, entry, data, level, true);
            }
        }
        else if (charge > 0)
        {
            data.getData().set(Vars.BEAM_CHARGE, Math.max(0.0F, charge - 2.0F));
        }
    }

    /** Fires a hitscan energy shot along the entity's look vector. */
    protected void fire(LivingEntity entity, ModifierEntry entry, SHPlayerData data, ServerLevel level, boolean charged)
    {
        float range = entry.getFloat(PowerProperty.RANGE);
        float damage = entry.getFloat(PowerProperty.AMOUNT);

        if (damage <= 0)
        {
            damage = 6.0F;
        }

        Vec3 start = entity.getEyePosition();
        Vec3 direction = entity.getLookAngle();
        Vec3 end = start.add(direction.scale(range));

        HitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getHitResultOnViewVector(entity, e -> e != entity && e.isAlive(), range);

        if (hit.getType() != HitResult.Type.MISS)
        {
            end = hit.getLocation();
        }

        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target)
        {
            target.hurt(entity.damageSources().indirectMagic(entity, entity), damage * (charged ? 2.0F : 1.0F));

            if (entry.getBoolean(PowerProperty.IS_EXPLOSIVE))
            {
                level.explode(entity, target.getX(), target.getY(), target.getZ(), 1.0F, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            }
        }

        // Visual tracer
        if (entity.level() instanceof ServerLevel serverLevel)
        {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, start.x + direction.x, start.y + direction.y, start.z + direction.z, 8, direction.x * 0.2D, direction.y * 0.2D, direction.z * 0.2D, 0.05D);
        }

        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, charged ? 0.8F : 1.2F);
        AbilityData.playSound(entity, entry, "SHOOT");
    }
}

/** Charged beam: a sustained beam that damages everything along its path. */
class ModifierChargedBeam extends ModifierEnergyProjection
{
    ModifierChargedBeam(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        data.getData().set(Vars.BEAM_CHARGING, true);
        AbilityData.playSound(entity, entry, "CHARGE");
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        data.getData().set(Vars.BEAM_CHARGING, false);
        data.getData().set(Vars.BEAM_CHARGE, 0.0F);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        int chargeTime = entry.getInt(PowerProperty.CHARGE_TIME);
        float charge = data.getData().get(Vars.BEAM_CHARGE);

        if (data.getData().get(Vars.BEAM_CHARGING))
        {
            charge = Math.min(chargeTime, charge + 1.0F);
            data.getData().set(Vars.BEAM_CHARGE, charge);

            if (charge >= chargeTime && entity.level() instanceof ServerLevel level)
            {
                int duration = entry.getInt(PowerProperty.DURATION);

                if (duration <= 0 || entity.tickCount % Math.max(1, duration / 8) == 0)
                {
                    fire(entity, entry, data, level, true);
                }
            }
        }
    }
}

/** Heat vision: continuous, low damage beam from the eyes while the key is held. */
class ModifierHeatVision extends ModifierEnergyProjection
{
    ModifierHeatVision(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (entity.level() instanceof ServerLevel level)
        {
            fire(entity, entry, data, level, false);
        }
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (entity.tickCount % 10 == 0 && entity.level() instanceof ServerLevel level)
        {
            fire(entity, entry, data, level, false);
        }
    }

    @Override
    protected void fire(LivingEntity entity, ModifierEntry entry, SHPlayerData data, ServerLevel level, boolean charged)
    {
        if (!(entity instanceof Player player))
        {
            return;
        }

        float range = entry.getFloat(PowerProperty.RANGE);
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        HitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getHitResultOnViewVector(player, e -> e != player, range);

        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target)
        {
            target.hurt(player.damageSources().indirectMagic(player, player), 4.0F);
            target.setSecondsOnFire(5);
        }

        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, start.x + direction.x, start.y + direction.y, start.z + direction.z, 4, direction.x * 0.3D, direction.y * 0.3D, direction.z * 0.3D, 0.02D);
    }
}
