package com.fiskmods.heroes.common.hero.modifier;

import java.util.ArrayList;
import java.util.List;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.DataVar;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Toggled/instant suit abilities: transformations, cooldowns, shields, blades, intangibility,
 * size manipulation, teleportation and grappling.
 */

class ModifierTransformation extends Modifier
{
    ModifierTransformation(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean transformed = entry.isToggled(entity);
        boolean next = !transformed;
        entry.setToggled(entity, next);

        JsonObject transformation = AbilityData.object(entry.get(PowerProperty.TRANSFORMATION));
        DataVar<Boolean> toggleData = AbilityData.toggle(transformation);

        if (toggleData != null)
        {
            data.getData().set(toggleData, next);
            data.getData().markDirty(toggleData);
        }

        AbilityData.playSound(entity, entry, next ? "ENABLE" : "DISABLE");
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        JsonObject transformation = AbilityData.object(entry.get(PowerProperty.TRANSFORMATION));

        if (transformation == null)
        {
            return;
        }

        DataVar<Boolean> toggleData = AbilityData.toggle(transformation);
        DataVar<Float> timerData = AbilityData.timer(transformation);
        int time = transformation.has("time") ? transformation.get("time").getAsInt() : 10;

        if (toggleData != null && timerData != null)
        {
            boolean active = data.getData().get(toggleData);
            float timer = data.getData().get(timerData);
            float next = active ? Math.min(time, timer + 1.0F) : Math.max(0.0F, timer - 1.0F);

            if (next != timer)
            {
                data.getData().set(timerData, next);
            }
        }
    }
}

/** Cooldown modifier: counts down a data variable and forces the paired toggle off when it ends. */
class ModifierCooldown extends Modifier
{
    ModifierCooldown(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        JsonObject cooldown = AbilityData.object(entry.get(PowerProperty.COOLDOWN));

        if (cooldown == null)
        {
            return;
        }

        DataVar<Boolean> toggleData = AbilityData.toggle(cooldown);
        DataVar<Float> cooldownData = AbilityData.cooldown(cooldown);
        int duration = cooldown.has("duration") ? cooldown.get("duration").getAsInt() : 100;

        if (toggleData == null || cooldownData == null)
        {
            return;
        }

        boolean active = data.getData().get(toggleData);
        float value = data.getData().get(cooldownData);

        if (active && value < duration)
        {
            value += 1.0F;
            data.getData().set(cooldownData, value);

            if (value >= duration)
            {
                data.getData().set(toggleData, false);
                data.getData().markDirty(toggleData);
                AbilityData.playSound(entity, entry, "TIMEOUT");

                // Switch the matching toggle modifier off as well
                for (ModifierEntry other : data.getHeroType().getPowerContainer().getEntries())
                {
                    if (other.isToggled(entity) && other.getProperties().containsValue(toggleData))
                    {
                        other.setToggled(entity, false);
                    }
                }
            }
        }
        else if (!active && value > 0)
        {
            float recovery = cooldown.has("recovery") ? cooldown.get("recovery").getAsFloat() : 1.0F;
            data.getData().set(cooldownData, Math.max(0.0F, value - recovery));
        }
    }
}

/** Energy shields: absorb damage from the covered directions and reflect projectiles. */
class ModifierShield extends Modifier
{
    ModifierShield(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (entry.getBoolean(entity, PowerProperty.IS_TOGGLE))
        {
            boolean state = !entry.isToggled(entity);
            entry.setToggled(entity, state);
            data.getData().set(Vars.SHIELD, state);
            data.getData().set(Vars.SHIELD_BLOCKING, state);
        }
        else
        {
            data.getData().set(Vars.SHIELD, true);
            data.getData().set(Vars.SHIELD_BLOCKING, true);
            data.getData().set(Vars.SHIELD_COOLDOWN, (short) 10);
        }

        AbilityData.playSound(entity, entry, "BLOCK_START");
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!entry.getBoolean(entity, PowerProperty.IS_TOGGLE))
        {
            data.getData().set(Vars.SHIELD, false);
            data.getData().set(Vars.SHIELD_BLOCKING, false);
            data.getData().set(Vars.SHIELD_COOLDOWN, (short) 20);
        }
    }

    @Override
    public boolean isImmuneTo(LivingEntity entity, ModifierEntry entry, net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);

        if (data == null || !data.getData().get(Vars.SHIELD))
        {
            return false;
        }

        JsonObject shield = AbilityData.object(entry.get(PowerProperty.SHIELD));
        float health = shield != null && shield.has("health") ? shield.get("health").getAsFloat() : 100.0F;

        // The shield absorbs the hit from the data pool and reflects it
        // The shield keeps a health pool: absorbing a hit draws from it and the pool refills at
        // the rate the power declares. The pool is stored in the data variable the original used.
        short current = data.getData().get(Vars.SHIELD_COOLDOWN);
        double incoming = amount * 10.0D;
        double remaining = health - incoming;

        if (remaining > 0)
        {
            data.getData().set(Vars.SHIELD_COOLDOWN, (short) Math.max(current, Math.round(health - remaining)));
            AbilityData.playSound(entity, entry, "DEFLECT");
            return true;
        }

        return false;
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        float blocking = data.getData().get(Vars.SHIELD_BLOCKING_TIMER);
        float nextBlocking = net.minecraft.util.Mth.approach(blocking,
                data.getData().get(Vars.SHIELD_BLOCKING) ? 1.0F : 0.0F, 0.2F);
        if (nextBlocking != blocking)
        {
            data.getData().set(Vars.SHIELD_BLOCKING_TIMER, nextBlocking);
        }

        JsonObject shield = AbilityData.object(entry.get(PowerProperty.SHIELD));
        int cooldown = shield != null && shield.has("cooldown") ? shield.get("cooldown").getAsInt() : 60;

        short value = data.getData().get(Vars.SHIELD_COOLDOWN);

        if (value > 0)
        {
            data.getData().set(Vars.SHIELD_COOLDOWN, (short) Math.max(0.0D, value - (110.0D / Math.max(1, cooldown))));
        }
    }
}

/** Plasma blades and claws. */
class ModifierBlade extends Modifier
{
    ModifierBlade(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean state = !entry.isToggled(entity);
        entry.setToggled(entity, state);
        data.getData().set(Vars.BLADE, state);
        AbilityData.playSound(entity, entry, state ? "ENABLE" : "DISABLE");
    }

    @Override
    public float modifyDamage(LivingEntity entity, ModifierEntry entry, net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        return amount;
    }
}

/** Phase through matter. */
class ModifierIntangibility extends Modifier
{
    ModifierIntangibility(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean state = !entry.isToggled(entity);
        entry.setToggled(entity, state);
        data.getData().set(Vars.INTANGIBLE, state);
        AbilityData.playSound(entity, entry, state ? "ENABLE" : "DISABLE");
    }

    @Override
    public boolean isImmuneTo(LivingEntity entity, ModifierEntry entry, net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);
        return data != null && data.getData().get(Vars.INTANGIBLE)
                && !entity.hasEffect(ModEffects.PHASE_SUPPRESSANT.get())
                && !entry.getBoolean(entity, PowerProperty.IS_ABSOLUTE);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean active = data.getData().get(Vars.INTANGIBLE)
                && !entity.hasEffect(ModEffects.PHASE_SUPPRESSANT.get());
        float timer = data.getData().get(Vars.INTANGIBILITY_TIMER);
        data.getData().set(Vars.INTANGIBILITY_TIMER,
                net.minecraft.util.Mth.approach(timer, active ? 1.0F : 0.0F, 0.2F));

        if (active)
        {
            entity.noPhysics = true;
            entity.fallDistance = 0.0F;

            if (entity.level().getBlockState(entity.blockPosition()).isAir())
            {
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0D, 0.5D, 1.0D));
            }
        }
        else
        {
            entity.noPhysics = false;
        }
    }
}

/** Ant-Man style size manipulation. */
class ModifierSizeManipulation extends Modifier
{
    ModifierSizeManipulation(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        float scale = data.getData().get(Vars.SCALE);
        float min = entry.getFloat(entity, PowerProperty.MIN_SIZE);
        float max = entry.getFloat(entity, PowerProperty.MAX_SIZE);
        float next = scale <= (min + max) * 0.5F ? max : min;

        data.getData().set(Vars.SCALE, next);
        data.getData().set(Vars.SIZE_STATE, (byte) (next > 1.0F ? 1 : next < 1.0F ? -1 : 0));
        data.getData().markDirty(Vars.SCALE);
        data.getData().markDirty(Vars.SIZE_STATE);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        float scale = data.getData().get(Vars.SCALE);
        float timer = data.getData().get(Vars.SHRINK_TIMER);
        float target = scale < 1.0F ? 1.0F : 0.0F;
        data.getData().set(Vars.SHRINK_TIMER, timer + (target - timer) * 0.2F);
    }
}

/** Teleportation: blinks the wearer in the direction they are looking. */
class ModifierTeleportation extends Modifier
{
    ModifierTeleportation(ResourceLocation id)
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

        data.getData().set(Vars.TELEPORT_TIMER, 1.0F);

        float range = entry.getFloat(entity, PowerProperty.RANGE);
        Vec3 look = entity.getLookAngle();
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        for (double d = 1.0D; d <= range; d += 0.5D)
        {
            double tx = entity.getX() + look.x * d;
            double ty = entity.getY() + look.y * d;
            double tz = entity.getZ() + look.z * d;

            if (!level.getBlockState(net.minecraft.core.BlockPos.containing(tx, ty, tz)).isAir())
            {
                break;
            }

            x = tx;
            y = ty;
            z = tz;
        }

        entity.teleportTo(x, y, z);
        entity.level().playSound(null, entity.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 1.2F);
    }

}

/** Web swinging / grappling: pulls the wearer towards the targeted block. */
class ModifierGrapple extends Modifier
{
    ModifierGrapple(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!(entity instanceof Player player))
        {
            return;
        }

        float range = entry.getFloat(entity, PowerProperty.RANGE);
        var hit = player.pick(range, 0.0F, false);

        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS)
        {
            Vec3 target = hit.getLocation();
            double speed = Math.max(0.1D, entry.getFloat(entity, PowerProperty.SPEED));
            Vec3 delta = target.subtract(player.position()).normalize().scale(1.2D * speed);

            player.setDeltaMovement(delta);
            player.hasImpulse = true;
            player.fallDistance = 0.0F;
            AbilityData.playSound(player, entry, "SHOOT");
        }
    }

}

/** Shadow form: the wearer becomes a cloud of shadow particles. */
class ModifierShadowform extends Modifier
{
    ModifierShadowform(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean state = !entry.isToggled(entity);
        entry.setToggled(entity, state);
        data.getData().set(Vars.SHADOWFORM, state);
        data.getData().markDirty(Vars.SHADOWFORM);
        AbilityData.playSound(entity, entry, state ? "ENABLE" : "DISABLE");
    }

    @Override
    public boolean isImmuneTo(LivingEntity entity, ModifierEntry entry, net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);
        if (data == null || !data.getData().get(Vars.SHADOWFORM)
                || entity.hasEffect(ModEffects.PHASE_SUPPRESSANT.get())) return false;

        // Shadowform's reference behavior only blocks attacks when its absolute variant is
        // enabled. Ordinary fall damage is amplified while shadowed; melee and projectiles are
        // not accidentally converted into blanket immunity.
        if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) return true;
        return entry.getBoolean(entity, PowerProperty.IS_ABSOLUTE)
                && (isMeleeAttack(source) || source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)
                        || source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION));
    }

    @Override
    public float modifyDamage(LivingEntity entity, ModifierEntry entry,
            net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);
        return data != null && data.getData().get(Vars.SHADOWFORM)
                && !entity.hasEffect(ModEffects.PHASE_SUPPRESSANT.get())
                && source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) ? amount * 2.0F : amount;
    }

    @Override
    public float modifyOutgoingDamage(LivingEntity entity, ModifierEntry entry, Entity target,
            net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);
        return data != null && data.getData().get(Vars.SHADOWFORM)
                && !entity.hasEffect(ModEffects.PHASE_SUPPRESSANT.get()) && isMeleeAttack(source) ? 0.0F : amount;
    }

    private static boolean isMeleeAttack(net.minecraft.world.damagesource.DamageSource source)
    {
        return source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                || source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        float timer = data.getData().get(Vars.SHADOWFORM_TIMER);
        data.getData().set(Vars.SHADOWFORM_TIMER,
                net.minecraft.util.Mth.approach(timer, data.getData().get(Vars.SHADOWFORM) ? 1.0F : 0.0F, 0.2F));

        if (data.getData().get(Vars.SHADOWFORM))
        {
            entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 10, 0, true, false, false));
            entity.clearFire();
        }
    }
}

/** Hold-to-grab telekinesis from the original pack: aim at a permitted target, pull it close, release it or crush it. */
class ModifierTelekinesis extends Modifier
{
    private static final String CHICKEN_CRUSH_TICK = "FiskHeroesTelekinesisCrushTick";
    private static final String GRABBED_BY_TAG = "FiskHeroesGrabbedBy";

    ModifierTelekinesis(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean state = entry.getBoolean(entity, PowerProperty.IS_TOGGLE)
                ? !entry.isToggled(entity) : true;
        entry.setToggled(entity, state);
        data.getData().set(Vars.TELEKINESIS, state);
        if (state)
        {
            AbilityData.playSound(entity, entry, "GRAB");
        }
        else
        {
            release(entity, data);
            AbilityData.playSound(entity, entry, "RELEASE");
        }
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!entry.getBoolean(entity, PowerProperty.IS_TOGGLE))
        {
            entry.setToggled(entity, false);
            data.getData().set(Vars.TELEKINESIS, false);
            release(entity, data);
            AbilityData.playSound(entity, entry, "RELEASE");
        }
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!data.getData().get(Vars.TELEKINESIS) || entity.level().isClientSide)
        {
            return;
        }

        Level level = entity.level();
        float range = Math.max(0.1F, entry.getFloat(entity, PowerProperty.RANGE));
        int grabId = data.getData().get(Vars.GRAB_ID);
        Entity grabbed = grabId >= 0 ? level.getEntity(grabId) : null;

        if (grabbed != null && (!grabbed.isAlive() || !canGrab(entry, grabbed)
                || grabbed.getPersistentData().getInt(GRABBED_BY_TAG) != entity.getId()))
        {
            releaseTarget(entity, grabbed);
            grabbed = null;
        }

        if (grabbed == null)
        {
            grabbed = findTarget(entity, entry, range);
            if (grabbed == null)
            {
                data.getData().set(Vars.GRAB_ID, -1);
                data.getData().set(Vars.GRAB_DISTANCE, 0.0F);
                return;
            }

            double distance = entity.getEyePosition().distanceTo(grabbed.position());
            data.getData().set(Vars.GRAB_DISTANCE,
                    net.minecraft.util.Mth.clamp((float) Math.max(0.0D, distance - 2.0D) / Math.max(1.0F, range - 2.0F), 0.0F, 1.0F));
            data.getData().set(Vars.GRAB_ID, grabbed.getId());
            grabbed.getPersistentData().putInt(GRABBED_BY_TAG, entity.getId());
        }

        pullTowardPlayer(entity, grabbed, range, data.getData().get(Vars.GRAB_DISTANCE));
        if (entity.isShiftKeyDown())
        {
            crushTarget(entity, grabbed, entry);
        }
    }

    private static Entity findTarget(LivingEntity entity, ModifierEntry entry, float range)
    {
        Vec3 start = entity.getEyePosition();
        Vec3 look = entity.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(range));
        BlockHitResult block = entity.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        double maxDistance = block.getType() == HitResult.Type.MISS
                ? range : start.distanceTo(block.getLocation());
        AABB search = entity.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        Entity best = null;
        double nearest = maxDistance;

        for (Entity candidate : entity.level().getEntities(entity, search,
                target -> target.isAlive() && canGrab(entry, target)
                        && (!target.getPersistentData().contains(GRABBED_BY_TAG)
                                || target.getPersistentData().getInt(GRABBED_BY_TAG) == entity.getId())))
        {
            var hit = candidate.getBoundingBox().inflate(0.2D).clip(start, end);
            if (hit.isEmpty()) continue;
            double distance = start.distanceTo(hit.get());
            if (distance < nearest)
            {
                nearest = distance;
                best = candidate;
            }
        }

        return best;
    }

    private static boolean canGrab(ModifierEntry entry, Entity target)
    {
        if (target instanceof HangingEntity)
        {
            return false;
        }

        JsonObject permissions = AbilityData.object(entry.get(PowerProperty.CAN_GRAB));
        if (target instanceof LivingEntity)
        {
            return flag(permissions, "mobs", true);
        }
        if (target instanceof ItemEntity)
        {
            return flag(permissions, "items", true);
        }
        if (target instanceof Projectile)
        {
            return flag(permissions, "projectiles", true);
        }
        return flag(permissions, "inanimates", true);
    }

    private static boolean flag(JsonObject json, String name, boolean fallback)
    {
        return json == null || !json.has(name) ? fallback : json.get(name).getAsBoolean();
    }

    private static void pullTowardPlayer(LivingEntity entity, Entity grabbed, float range, float normalizedDistance)
    {
        double distance = 2.0D + Math.max(0.0D, range - 2.0D) * normalizedDistance;
        Vec3 center = grabbed.position().add(0.0D, grabbed.getBbHeight() * 0.5D, 0.0D);
        Vec3 desired = entity.getEyePosition().add(entity.getViewVector(1.0F).scale(distance));
        Vec3 velocity = grabbed.getDeltaMovement().scale(0.8D).add(desired.subtract(center).scale(0.2D));
        grabbed.setDeltaMovement(velocity);
        grabbed.hurtMarked = true;
        grabbed.fallDistance = 0.0F;
    }

    private static void crushTarget(LivingEntity entity, Entity target, ModifierEntry entry)
    {
        JsonObject permissions = AbilityData.object(entry.get(PowerProperty.TELEKINESIS));
        if (target instanceof Projectile && flag(permissions, "crushThrowables", true))
        {
            target.discard();
        }
        else if (target instanceof Creeper creeper && flag(permissions, "explodeCreepers", true))
        {
            creeper.ignite();
        }
        else if (target instanceof Chicken chicken && flag(permissions, "squeezeChickens", true))
        {
            long next = chicken.getPersistentData().getLong(CHICKEN_CRUSH_TICK);
            if (entity.level().getGameTime() >= next)
            {
                chicken.getPersistentData().putLong(CHICKEN_CRUSH_TICK, entity.level().getGameTime() + 20L);
                chicken.hurt(entity.damageSources().inWall(), 1.0F);
            }
        }
        else if (target instanceof ItemEntity item && flag(permissions, "crushMelons", true)
                && item.getItem().is(Items.MELON))
        {
            int slices = item.getItem().getCount() * 9;
            while (slices > 0)
            {
                int count = Math.min(slices, Items.MELON_SLICE.getMaxStackSize());
                entity.level().addFreshEntity(new ItemEntity(entity.level(), item.getX(), item.getY(), item.getZ(),
                        new ItemStack(Items.MELON_SLICE, count)));
                slices -= count;
            }
            target.discard();
        }
        else if (!(target instanceof LivingEntity) && !(target instanceof ItemEntity)
                && flag(permissions, "destroyInanimates", true))
        {
            target.hurt(entity.damageSources().inWall(), 1.0F);
        }
    }

    private static void release(LivingEntity entity, SHPlayerData data)
    {
        int id = data.getData().get(Vars.GRAB_ID);
        Entity grabbed = id >= 0 ? entity.level().getEntity(id) : null;
        if (grabbed != null)
        {
            releaseTarget(entity, grabbed);
        }
        data.getData().set(Vars.GRAB_ID, -1);
        data.getData().set(Vars.GRAB_DISTANCE, 0.0F);
    }

    private static void releaseTarget(LivingEntity entity, Entity grabbed)
    {
        if (grabbed.getPersistentData().getInt(GRABBED_BY_TAG) == entity.getId())
        {
            grabbed.getPersistentData().remove(GRABBED_BY_TAG);
        }
    }
}

/** Charged punch: an energy-empowered melee attack. */
class ModifierChargedPunch extends Modifier
{
    ModifierChargedPunch(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean state = !entry.isToggled(entity);
        entry.setToggled(entity, state);
        data.getData().set(Vars.PUNCHMODE, state);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!data.getData().get(Vars.PUNCHMODE))
        {
            return;
        }

        int timer = data.getData().get(Vars.PUNCH_TIMER);

        if (entity instanceof Player player && player.getAttackStrengthScale(0.0F) >= 1.0F)
        {
            data.getData().set(Vars.PUNCH_TIMER, Math.min(entry.getInt(entity, PowerProperty.CHARGE_TIME), timer + 1));
        }
    }
}

/** Abilities which destroy terrain around the wearer. */
class ModifierGriefing extends Modifier
{
    ModifierGriefing(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        float radius = Math.min(entry.getFloat(entity, PowerProperty.RADIUS), 16.0F);
        float knockback = entry.getFloat(entity, PowerProperty.KNOCKBACK);

        for (LivingEntity target : entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius), e -> e != entity))
        {
            Vec3 delta = target.position().subtract(entity.position()).normalize().scale(Math.max(0.4F, knockback));
            target.setDeltaMovement(delta.x, 0.4D, delta.z);
            target.hurtMarked = true;
        }

        if (entry.getBoolean(entity, PowerProperty.CAN_DO_GRIEFING) && entity.level() instanceof ServerLevel level)
        {
            net.minecraft.core.BlockPos origin = entity.blockPosition();

            for (int i = 0; i < 24; ++i)
            {
                net.minecraft.core.BlockPos pos = origin.offset(entity.getRandom().nextInt(7) - 3, -1 + entity.getRandom().nextInt(2), entity.getRandom().nextInt(7) - 3);
                var state = level.getBlockState(pos);

                if (!state.isAir() && state.getDestroySpeed(level, pos) < 1.0F && state.getBlock() != Blocks.BEDROCK)
                {
                    level.destroyBlock(pos, true, entity);
                }
            }
        }

        entity.level().playSound(null, entity.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 0.6F);
    }
}
