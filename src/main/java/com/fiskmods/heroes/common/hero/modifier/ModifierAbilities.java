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
        JsonObject transformation = AbilityData.object(entry.get(PowerProperty.TRANSFORMATION));
        DataVar<Boolean> toggleData = AbilityData.toggle(transformation);
        boolean transformed = toggleData != null ? data.getData().get(toggleData) : entry.isToggled(entity);
        boolean next = !entry.getBoolean(entity, PowerProperty.IS_TOGGLE) || !transformed;
        entry.setToggled(entity, next);

        if (toggleData != null)
        {
            data.getData().set(toggleData, next);
            data.getData().markDirty(toggleData);
        }

        AbilityData.playSound(entity, entry, next ? "ENABLE" : "DISABLE");
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!entry.getBoolean(entity, PowerProperty.IS_TOGGLE))
        {
            DataVar<Boolean> toggleData = AbilityData.toggle(AbilityData.object(entry.get(PowerProperty.TRANSFORMATION)));
            if (toggleData != null) data.getData().set(toggleData, false);
            entry.setToggled(entity, false);
            AbilityData.playSound(entity, entry, "DISABLE");
        }
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
            float step = 1.0F / Math.max(1, time);
            float next = net.minecraft.util.Mth.clamp(timer + (active ? step : -step), 0.0F, 1.0F);

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
        if (duration < 0)
        {
            data.getData().set(cooldownData, 0.0F);
            return;
        }
        float step = 1.0F / Math.max(1, duration);
        if (active)
        {
            if (value <= 0.0F) AbilityData.playSound(entity, entry, "START");
            value = Math.min(1.0F, value + step);
            if (value >= 1.0F)
            {
                data.getData().set(toggleData, false);
                AbilityData.playSound(entity, entry, "TIMEOUT");
            }
        }
        else if (!active && value > 0.0F)
        {
            float recovery = cooldown.has("recovery") ? cooldown.get("recovery").getAsFloat() : 1.0F;
            value = Math.max(0.0F, value - step * Math.max(0.0F, recovery));
            if (value == 0.0F) AbilityData.playSound(entity, entry, "RECOVER");
        }
        data.getData().set(cooldownData, net.minecraft.util.Mth.clamp(value, 0.0F, 1.0F));
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
        }
        else
        {
            data.getData().set(Vars.SHIELD, true);
        }

    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!entry.getBoolean(entity, PowerProperty.IS_TOGGLE))
        {
            data.getData().set(Vars.SHIELD, false);
        }
    }

    @Override
    public boolean isImmuneTo(LivingEntity entity, ModifierEntry entry, net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);
        if (data == null || !data.getData().get(Vars.SHIELD) || !data.getData().get(Vars.SHIELD_BLOCKING)
                || data.getData().get(Vars.SHIELD_COOLDOWN) > 0 || amount <= 0
                || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) return false;
        var direct = source.getDirectEntity();
        if (direct == null) return false;

        JsonObject coverage = AbilityData.object(entry.get(entity, PowerProperty.COVERAGE));
        double dx = direct.getX() - entity.getX();
        double dy = direct.getY() + direct.getBbHeight() * 0.5D - entity.getEyeY();
        double dz = direct.getZ() - entity.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = -(float) Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        float dyaw = Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - entity.getYRot() + number(coverage, "yawOffset", 0)));
        float dpitch = Math.abs(net.minecraft.util.Mth.wrapDegrees(pitch - entity.getXRot() + number(coverage, "pitchOffset", 0)));
        if (dyaw >= number(coverage, "yawRadius", 35) || dpitch >= number(coverage, "pitchRadius", 35)) return false;

        JsonObject shield = AbilityData.object(entry.get(entity, PowerProperty.SHIELD));
        float maximum = number(shield, "health", shield == null ? 80 : 100);
        float damage = data.getData().get(Vars.SHIELD_DAMAGE) + amount;
        data.getData().set(Vars.SHIELD_DAMAGE, damage);
        data.getData().set(Vars.TICKS_SINCE_SHIELD_DAMAGED, (short) 0);
        boolean broken = damage >= maximum;
        if (broken) data.getData().set(Vars.SHIELD_COOLDOWN,
                (short) Math.max(0, Math.min(Short.MAX_VALUE, (int) number(shield, "cooldown", 100))));
        if (!entity.level().isClientSide) AbilityData.playSound(entity, entry, broken ? "DISARM" : "DEFLECT");
        if (source.getEntity() instanceof LivingEntity attacker && direct == attacker && attacker != entity)
        {
            float knockback = entry.getFloat(entity, PowerProperty.KNOCKBACK);
            if (broken) knockback = Math.max(knockback * 1.5F, 0.3F);
            if (knockback > 0) attacker.knockback(Math.min(amount, 20) * knockback,
                    entity.getX() - attacker.getX(), entity.getZ() - attacker.getZ());
        }
        // As in the original, the hit which breaks the shield is not absorbed.
        return !broken;
    }

    private static float number(JsonObject object, String key, float fallback)
    {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                && object.get(key).getAsJsonPrimitive().isNumber() ? object.get(key).getAsFloat() : fallback;
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        var vars = data.getData();
        boolean previousBlocking = vars.get(Vars.SHIELD_BLOCKING);
        boolean wantsBlock = !entry.getBoolean(entity, PowerProperty.IS_TOGGLE)
                || entity instanceof Player player && com.fiskmods.heroes.common.data.PlayerInputTracker.isUsing(player);
        boolean blocking = vars.get(Vars.SHIELD) && wantsBlock && vars.get(Vars.SHIELD_COOLDOWN) == 0;
        vars.set(Vars.SHIELD_BLOCKING, blocking);
        if (blocking != previousBlocking && !entity.level().isClientSide)
        {
            com.fiskmods.heroes.FiskHeroes.LOGGER.debug("Suit shield blocking changed: entity={}, blocking={}", entity.getName().getString(), blocking);
            AbilityData.playSound(entity, entry, blocking ? "BLOCK_START" : "BLOCK_STOP");
        }
        vars.set(Vars.SHIELD_TIMER, net.minecraft.util.Mth.approach(vars.get(Vars.SHIELD_TIMER),
                vars.get(Vars.SHIELD) ? 1.0F : 0.0F, 0.1F));
        vars.set(Vars.SHIELD_BLOCKING_TIMER, net.minecraft.util.Mth.approach(vars.get(Vars.SHIELD_BLOCKING_TIMER),
                vars.get(Vars.SHIELD_BLOCKING) && vars.get(Vars.SHIELD_COOLDOWN) == 0 ? 1.0F : 0.0F, 0.2F));
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
        boolean state = !entry.getBoolean(entity, PowerProperty.IS_TOGGLE) || !data.getData().get(Vars.INTANGIBLE);
        setActive(entity, entry, data, state);
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!entry.getBoolean(entity, PowerProperty.IS_TOGGLE)) setActive(entity, entry, data, false);
    }

    private void setActive(LivingEntity entity, ModifierEntry entry, SHPlayerData data, boolean state)
    {
        entry.setToggled(entity, state);
        data.getData().set(Vars.INTANGIBLE, state);
        if (entity instanceof Player player) IntangibilityPhysics.update(player, data);
    }

    @Override
    public boolean isImmuneTo(LivingEntity entity, ModifierEntry entry, net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);
        if (data == null || !data.getData().get(Vars.INTANGIBLE) || entity.hasEffect(ModEffects.PHASE_SUPPRESSANT.get()))
            return false;
        if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) return true;
        if (!entry.getBoolean(entity, PowerProperty.IS_ABSOLUTE)) return false;
        return source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)
                || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                || isMelee(source);
    }

    private static boolean isMelee(net.minecraft.world.damagesource.DamageSource source)
    {
        return source.getEntity() != null && source.getEntity() == source.getDirectEntity()
                && (source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                        || source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)
                        || source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK_NO_AGGRO)
                        || source.is(net.minecraft.world.damagesource.DamageTypes.STING));
    }

    @Override
    public float modifyOutgoingDamage(LivingEntity entity, ModifierEntry entry, Entity target,
            net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);
        return data != null && data.getData().get(Vars.INTANGIBLE)
                && !entity.hasEffect(ModEffects.PHASE_SUPPRESSANT.get())
                && entry.getBoolean(entity, PowerProperty.IS_ABSOLUTE) && isMelee(source) ? 0.0F : amount;
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean active = data.getData().get(Vars.INTANGIBLE)
                && !entity.hasEffect(ModEffects.PHASE_SUPPRESSANT.get());
        float timer = data.getData().get(Vars.INTANGIBILITY_TIMER);
        data.getData().set(Vars.INTANGIBILITY_TIMER,
                net.minecraft.util.Mth.approach(timer, active ? 1.0F : 0.0F, 0.1F));
        if (entity instanceof Player player) IntangibilityPhysics.update(player, data);
        if (active)
        {
            entity.fallDistance = 0.0F;
            if (entity.level().getBlockState(entity.blockPosition()).isAir())
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0D, 0.5D, 1.0D));
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
        if (data.getData().get(Vars.SIZE_STATE) != 0) return;
        float min = Math.max(0.01F, entry.getFloat(entity, PowerProperty.MIN_SIZE));
        float max = Math.max(min, entry.getFloat(entity, PowerProperty.MAX_SIZE));
        float scale = data.getData().get(Vars.SCALE);
        byte direction = (byte) (scale > (min + max) * 0.5F ? -1 : 1);
        data.getData().set(Vars.SIZE_STATE, direction);
        AbilityData.playSound(entity, entry, direction > 0 ? "GROW" : "SHRINK");
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!(entity instanceof Player)) return;
        float min = Math.max(0.01F, entry.getFloat(entity, PowerProperty.MIN_SIZE));
        float max = Math.max(min, entry.getFloat(entity, PowerProperty.MAX_SIZE));
        float scale = Math.max(0.01F, data.getData().get(Vars.SCALE));
        byte direction = data.getData().get(Vars.SIZE_STATE);
        if (direction != 0 && max > min)
        {
            boolean instant = entry.getBoolean(entity, PowerProperty.IS_INSTANT);
            float step = (max - min) / (instant ? 7.0F : 60.0F);
            if (!instant && direction < 0) step *= 2.0F;
            float next = net.minecraft.util.Mth.clamp(scale + Math.signum(direction) * step, min, max);
            if (next <= scale || canGrow(entity, scale, next))
            {
                data.getData().set(Vars.SCALE, next);
                if (next == min || next == max) data.getData().set(Vars.SIZE_STATE, (byte) 0);
                scale = next;
            }
        }
        float timer = data.getData().get(Vars.SHRINK_TIMER);
        float target = scale < 1.0F ? 1.0F : 0.0F;
        data.getData().set(Vars.SHRINK_TIMER, timer + (target - timer) * 0.2F);
    }

    private static boolean canGrow(LivingEntity entity, float oldScale, float nextScale)
    {
        double width = entity.getBbWidth() * nextScale / oldScale;
        double height = entity.getBbHeight() * nextScale / oldScale;
        var box = new net.minecraft.world.phys.AABB(entity.getX() - width / 2, entity.getY(), entity.getZ() - width / 2,
                entity.getX() + width / 2, entity.getY() + height, entity.getZ() + width / 2);
        return entity.level().noCollision(entity, box);
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

            // Check the complete, scaled player body, not only the feet block.
            // This also respects partial collision shapes and avoids teleporting into ceilings.
            var candidate = entity.getBoundingBox().move(tx - entity.getX(), ty - entity.getY(), tz - entity.getZ());
            if (!level.noCollision(entity, candidate)) break;

            x = tx;
            y = ty;
            z = tz;
        }

        if (entity.distanceToSqr(x, y, z) < 1.0E-8D) return;
        entity.teleportTo(x, y, z);
        entity.fallDistance = 0.0F;
        JsonElement sounds = entry.get(PowerProperty.SOUND_EVENTS);
        if (sounds != null && sounds.isJsonObject() && sounds.getAsJsonObject().has("TELEPORT"))
            AbilityData.playSound(entity, entry, "TELEPORT");
        else
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
        // Cooldowns can turn the data variable off without a key press. The variable,
        // not the separately cached toggle, is the authoritative current state.
        boolean state = !data.getData().get(Vars.SHADOWFORM);
        entry.setToggled(entity, state);
        data.getData().set(Vars.SHADOWFORM, state);
        data.getData().markDirty(Vars.SHADOWFORM);
        if (entity instanceof Player player) InvisibilityState.update(player, data);
        // DataVarShadowform in the original adds a 0.55 upward impulse when enabled
        // on the ground. Send a one-shot velocity update to the controlling client;
        // continuous flight itself remains client-predicted.
        if (state && entity.isAlive() && entity.onGround() && !entity.level().isClientSide)
        {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0D, 0.55D, 0.0D));
            entity.hurtMarked = true;
        }
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
        if (entity instanceof Player player) InvisibilityState.update(player, data);

        if (data.getData().get(Vars.SHADOWFORM))
        {
            entity.clearFire();
        }
    }
}

/** Hold-to-grab telekinesis from the original pack: aim at a permitted target, pull it close, release it or crush it. */
class ModifierTelekinesis extends Modifier
{
    private static final String CHICKEN_CRUSH_TICK = "FiskHeroesTelekinesisCrushTick";
    private static final String GRABBED_BY_TAG = TelekinesisState.GRABBED_BY_TAG;

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
                        && TelekinesisState.canClaim(entity, target)))
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
        TelekinesisState.release(entity, data);
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
        if (!entity.getMainHandItem().isEmpty()) return;
        boolean state = !entry.getBoolean(entity, PowerProperty.IS_TOGGLE) || !data.getData().get(Vars.PUNCHMODE);
        entry.setToggled(entity, state);
        data.getData().set(Vars.PUNCHMODE, state);
        if (!state) data.getData().set(Vars.PUNCH_CHARGED, false);
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!entry.getBoolean(entity, PowerProperty.IS_TOGGLE))
        {
            entry.setToggled(entity, false);
            data.getData().set(Vars.PUNCHMODE, false);
            data.getData().set(Vars.PUNCH_CHARGED, false);
        }
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean mode = data.getData().get(Vars.PUNCHMODE);
        if (mode && !entity.getMainHandItem().isEmpty())
        {
            mode = false;
            entry.setToggled(entity, false);
            data.getData().set(Vars.PUNCHMODE, false);
        }
        if (!mode) data.getData().set(Vars.PUNCH_CHARGED, false);
        float timer = data.getData().get(Vars.PUNCHMODE_TIMER);
        data.getData().set(Vars.PUNCHMODE_TIMER, net.minecraft.util.Mth.clamp(timer + (mode ? 0.2F : -0.2F), 0.0F, 1.0F));
        float charge = data.getData().get(Vars.PUNCH_CHARGE);
        boolean charging = mode && (entity.isShiftKeyDown() || data.getData().get(Vars.PUNCH_CHARGED));
        float step = 1.0F / Math.max(1, entry.getInt(entity, PowerProperty.CHARGE_TIME));
        charge = net.minecraft.util.Mth.clamp(charge + (charging ? step : -step), 0.0F, 1.0F);
        data.getData().set(Vars.PUNCH_CHARGE, charge);
        if (mode && entity.isShiftKeyDown() && charge >= 1.0F && !data.getData().get(Vars.PUNCH_CHARGED))
        {
            data.getData().set(Vars.PUNCH_CHARGED, true);
            AbilityData.playSound(entity, entry, "CHARGED");
        }
    }

    @Override
    public float modifyOutgoingDamage(LivingEntity entity, ModifierEntry entry, net.minecraft.world.entity.Entity target,
            net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);
        if (!entity.level().isClientSide && amount > 0.0F && data != null && data.getData().get(Vars.PUNCH_CHARGED))
        {
            data.getData().set(Vars.PUNCH_CHARGED, false);
            AbilityData.playSound(entity, entry, "PUNCH");
        }
        return amount;
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
        JsonElement damageProfile = entry.get(entity, PowerProperty.DAMAGE_PROFILE);
        float damage = DamageGroups.profileDamage(damageProfile, entry.getFloat(entity, PowerProperty.AMOUNT));

        for (LivingEntity target : entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius), e -> e != entity))
        {
            if (target.distanceToSqr(entity) > radius * radius) continue;

            if (damage > 0.0F)
            {
                DamageGroups.applyProfileDamage(target, entity,
                        entity.damageSources().explosion(entity, entity), damage, damageProfile);
            }

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
