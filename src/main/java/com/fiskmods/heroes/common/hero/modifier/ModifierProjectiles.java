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
import com.google.gson.JsonElement;

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

        int chargeTime = entry.getInt(entity, PowerProperty.CHARGE_TIME);

        if (chargeTime > 0 && data.getData().get(Vars.BEAM_CHARGE) < chargeTime)
        {
            data.getData().set(Vars.BEAM_CHARGING, true);
            return;
        }

        fire(entity, entry, data, level, false);
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        // Instant projections can share an ability key with a charged beam. Only a charged
        // projection owns these shared values, so an instant one's key-up must leave them alone.
        if (entry.getInt(entity, PowerProperty.CHARGE_TIME) > 0)
        {
            data.getData().set(Vars.BEAM_CHARGING, false);
            data.getData().set(Vars.BEAM_CHARGE, 0.0F);
        }
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        int chargeTime = entry.getInt(entity, PowerProperty.CHARGE_TIME);
        if (chargeTime <= 0)
        {
            return;
        }

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
        float range = entry.getFloat(entity, PowerProperty.RANGE);
        JsonElement damageProfile = entry.get(entity, PowerProperty.DAMAGE_PROFILE);
        float damage = DamageGroups.profileDamage(damageProfile, entry.getFloat(entity, PowerProperty.AMOUNT));

        if (damage <= 0)
        {
            damage = 6.0F;
        }

        if ("energy_bolt".equals(entry.getModifier().getId().getPath()))
        {
            var bolt = new com.fiskmods.heroes.common.entity.projectile.EnergyBoltEntity(entity,
                    damageProfile, charged ? damage * 1.6F : damage,
                    entry.getBoolean(entity, PowerProperty.IS_EXPLOSIVE));
            level.addFreshEntity(bolt);
            AbilityData.playSound(entity, entry, "SHOOT");
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLAZE_SHOOT,
                    SoundSource.PLAYERS, 1.0F, charged ? 0.8F : 1.2F);
            return;
        }

        if ("icicles".equals(entry.getModifier().getId().getPath()))
        {
            int quantity = Math.max(1, Math.min(16, entry.getInt(entity, PowerProperty.QUANTITY)));
            float spread = entry.getFloat(entity, PowerProperty.SPREAD);
            for (int i = 0; i < quantity; ++i)
            {
                var icicle = new com.fiskmods.heroes.common.entity.projectile.IcicleEntity(entity,
                        damageProfile, damage, spread);
                level.addFreshEntity(icicle);
            }
            AbilityData.playSound(entity, entry, "SHOOT");
            return;
        }

        if ("fireball".equals(entry.getModifier().getId().getPath()))
        {
            var fireball = new com.fiskmods.heroes.common.entity.projectile.FireBlastEntity(entity,
                    damageProfile, damage, entry.getFloat(entity, PowerProperty.RADIUS));
            level.addFreshEntity(fireball);
            AbilityData.playSound(entity, entry, "SHOOT");
            return;
        }

        if ("canary_cry".equals(entry.getModifier().getId().getPath()))
        {
            var wave = new com.fiskmods.heroes.common.entity.projectile.SonicWaveEntity(entity,
                    damageProfile, damage, entry.getFloat(entity, PowerProperty.KNOCKBACK),
                    entry.getBoolean(entity, PowerProperty.CAN_BREAK_GLASS));
            level.addFreshEntity(wave);
            AbilityData.playSound(entity, entry, "SHOOT");
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS, 0.6F, 1.8F);
            return;
        }

        Vec3 start = entity.getEyePosition();
        Vec3 direction = entity.getLookAngle();
        Vec3 end = start.add(direction.scale(range));

        HitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getHitResultOnViewVector(entity, e -> e != entity && e.isAlive(), range);
        var blockHit = level.clip(new net.minecraft.world.level.ClipContext(start, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, entity));
        if (blockHit.getType() != HitResult.Type.MISS
                && (hit.getType() == HitResult.Type.MISS
                        || start.distanceToSqr(blockHit.getLocation()) < start.distanceToSqr(hit.getLocation())))
        {
            hit = blockHit;
        }

        if (hit.getType() != HitResult.Type.MISS)
        {
            end = hit.getLocation();
        }

        data.getData().set(Vars.HEAT_VISION_LENGTH, start.distanceTo(end));
        data.getData().set(Vars.ENERGY_PROJECTION_TIMER, 1.0F);

        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target)
        {
            float attackDamage = damage * (charged ? 2.0F : 1.0F);
            DamageGroups.applyProfileDamage(target, entity, entity.damageSources().indirectMagic(entity, entity),
                    attackDamage, damageProfile);

            if (entry.getBoolean(entity, PowerProperty.IS_EXPLOSIVE))
            {
                level.explode(entity, target.getX(), target.getY(), target.getZ(), 1.0F, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            }
        }

        // Visual tracer
        if (entity.level() instanceof ServerLevel serverLevel)
        {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    end.x, end.y, end.z, 8, direction.x * 0.2D, direction.y * 0.2D, direction.z * 0.2D, 0.05D);
        }

        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, charged ? 0.8F : 1.2F);
        AbilityData.playSound(entity, entry, "SHOOT");
    }
}

/** Sustained Canary scream: the original emits one expanding wave for every held tick. */
class ModifierSonicWaves extends Modifier
{
    ModifierSonicWaves(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        data.getData().set(Vars.SONIC_WAVES, true);
        AbilityData.playSound(entity, entry, "SHOOT");
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        data.getData().set(Vars.SONIC_WAVES, false);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!data.getData().get(Vars.SONIC_WAVES) || !(entity.level() instanceof ServerLevel level))
        {
            return;
        }

        JsonElement damageProfile = entry.get(entity, PowerProperty.DAMAGE_PROFILE);
        float damage = DamageGroups.profileDamage(damageProfile, entry.getFloat(entity, PowerProperty.AMOUNT));
        if (damage <= 0.0F) damage = 7.0F;

        var wave = new com.fiskmods.heroes.common.entity.projectile.SonicWaveEntity(entity,
                damageProfile, damage, entry.getFloat(entity, PowerProperty.KNOCKBACK),
                entry.getBoolean(entity, PowerProperty.CAN_BREAK_GLASS));
        level.addFreshEntity(wave);

        // The original scream lifts a falling wearer when aimed steeply down; creative flight is
        // exempt from this movement assist.
        if (!(entity instanceof Player player) || !player.getAbilities().instabuild)
        {
            float lift = Math.max(0.0F, entity.getXRot() - 45.0F) / 45.0F;
            entity.fallDistance = Math.max(0.0F, entity.fallDistance - 0.4F * lift);
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0D, 0.05D * lift, 0.0D));
        }
    }
}

/** Charges the wearer's next melee strike with the power's configured energy damage. */
class ModifierEnergyManipulation extends Modifier
{
    static final String KEY = "CHARGE_ENERGY";

    ModifierEnergyManipulation(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        data.getData().set(Vars.ENERGY_CHARGING, true);
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        data.getData().set(Vars.ENERGY_CHARGING, false);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        int chargeTime = Math.max(1, entry.getInt(entity, PowerProperty.CHARGE_TIME));
        boolean charging = data.getData().get(Vars.ENERGY_CHARGING);
        float charge = data.getData().get(Vars.ENERGY_CHARGE);
        float step = charging ? 1.0F / chargeTime : 1.0F / (chargeTime * 4.0F);
        data.getData().set(Vars.ENERGY_CHARGE,
                net.minecraft.util.Mth.clamp(charge + (charging ? step : -step), 0.0F, 1.0F));
    }

    @Override
    public float modifyOutgoingDamage(LivingEntity entity, ModifierEntry entry,
            net.minecraft.world.entity.Entity target, net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        if (entity.level().isClientSide || source.getEntity() != entity || source.getDirectEntity() != entity
                || !source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                        && !source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK))
        {
            return amount;
        }

        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);
        if (data == null) return amount;

        float charge = data.getData().get(Vars.ENERGY_CHARGE);
        if (charge <= 0.0F) return amount;

        JsonElement damageProfile = entry.get(entity, PowerProperty.DAMAGE_PROFILE);
        float bonus = DamageGroups.profileDamage(damageProfile, entry.getFloat(entity, PowerProperty.AMOUNT));
        if (bonus <= 0.0F) return amount;

        data.getData().set(Vars.ENERGY_CHARGE, 0.0F);
        data.getData().set(Vars.ENERGY_CHARGING, false);
        if (target instanceof LivingEntity victim && entity.level() instanceof ServerLevel level)
        {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                    12, 0.35D, 0.35D, 0.35D, 0.08D);
            level.playSound(null, victim.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.PLAYERS, 0.6F, 1.4F);
        }
        return amount + bonus * charge;
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
        int chargeTime = entry.getInt(entity, PowerProperty.CHARGE_TIME);
        float charge = data.getData().get(Vars.BEAM_CHARGE);

        if (data.getData().get(Vars.BEAM_CHARGING))
        {
            charge = Math.min(chargeTime, charge + 1.0F);
            data.getData().set(Vars.BEAM_CHARGE, charge);

            if (charge >= chargeTime && entity.level() instanceof ServerLevel level)
            {
                int duration = entry.getInt(entity, PowerProperty.DURATION);

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
        data.getData().set(Vars.HEAT_VISION, true);
        if (entity.level() instanceof ServerLevel level)
        {
            fire(entity, entry, data, level, false);
        }
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        data.getData().set(Vars.HEAT_VISION, false);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean active = data.getData().get(Vars.HEAT_VISION);
        float timer = data.getData().get(Vars.HEAT_VISION_TIMER);
        float nextTimer = net.minecraft.util.Mth.approach(timer, active ? 1.0F : 0.0F, 0.2F);
        if (nextTimer != timer)
        {
            data.getData().set(Vars.HEAT_VISION_TIMER, nextTimer);
        }

        if (active && entity.tickCount % 10 == 0 && entity.level() instanceof ServerLevel level)
        {
            fire(entity, entry, data, level, false);
        }
        else if (!active && nextTimer == 0.0F && data.getData().get(Vars.HEAT_VISION_LENGTH) != 0.0D)
        {
            data.getData().set(Vars.HEAT_VISION_LENGTH, 0.0D);
        }
    }

    @Override
    protected void fire(LivingEntity entity, ModifierEntry entry, SHPlayerData data, ServerLevel level, boolean charged)
    {
        if (!(entity instanceof Player player))
        {
            return;
        }

        float range = entry.getFloat(entity, PowerProperty.RANGE);
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 end = start.add(direction.scale(range));
        HitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getHitResultOnViewVector(
                player, e -> e != player && e.isAlive() && !player.isAlliedTo(e), range);
        var blockHit = level.clip(new net.minecraft.world.level.ClipContext(start, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS
                && (hit.getType() == HitResult.Type.MISS
                        || start.distanceToSqr(blockHit.getLocation()) < start.distanceToSqr(hit.getLocation())))
        {
            hit = blockHit;
        }
        data.getData().set(Vars.HEAT_VISION_LENGTH, start.distanceTo(hit.getLocation()));

        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target)
        {
            JsonElement damageProfile = entry.get(player, PowerProperty.DAMAGE_PROFILE);
            float damage = DamageGroups.profileDamage(damageProfile, entry.getFloat(player, PowerProperty.AMOUNT));
            if (damage <= 0.0F) damage = 4.0F;
            float attackDamage = damage;
            DamageGroups.withDamageProfile(damageProfile, () -> target.hurt(player.damageSources().indirectMagic(player, player), attackDamage));
            target.setSecondsOnFire(5);
        }

        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, start.x + direction.x, start.y + direction.y, start.z + direction.z, 4, direction.x * 0.3D, direction.y * 0.3D, direction.z * 0.3D, 0.02D);
    }
}
