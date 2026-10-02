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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Toggled/instant suit abilities: transformations, cooldowns, shields, blades, intangibility,
 * size manipulation, teleportation and grappling.
 */
final class AbilityData
{
    /** Resolves a data variable referenced by a power JSON entry such as {@code "toggleData"}. */
    static DataVar<Boolean> toggle(JsonObject json)
    {
        return resolve(json, "toggleData");
    }

    static DataVar<Float> timer(JsonObject json)
    {
        return resolve(json, "timerData");
    }

    static DataVar<Float> cooldown(JsonObject json)
    {
        return resolve(json, "cooldownData");
    }

    @SuppressWarnings("unchecked")
    private static <T> DataVar<T> resolve(JsonObject json, String key)
    {
        if (json == null || !json.has(key))
        {
            return null;
        }

        return (DataVar<T>) com.fiskmods.heroes.common.data.DataRegistry.INSTANCE.get(json.get(key).getAsString());
    }

    static JsonObject object(JsonElement element)
    {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    static void playSound(LivingEntity entity, ModifierEntry entry, String trigger)
    {
        JsonElement sounds = entry.get(PowerProperty.SOUND_EVENTS);

        if (sounds == null || !sounds.isJsonObject())
        {
            return;
        }

        JsonElement sound = sounds.getAsJsonObject().get(trigger);

        if (sound == null)
        {
            return;
        }

        // Sound entries may be a single id or a list of variants; one is picked at random, as the
        // original dispatcher did.
        String id;

        if (sound.isJsonArray())
        {
            var list = sound.getAsJsonArray();

            if (list.isEmpty())
            {
                return;
            }

            id = list.get(entity.getRandom().nextInt(list.size())).getAsString();
        }
        else
        {
            id = sound.getAsString();
        }

        ResourceLocation location = ResourceLocation.tryParse(id);

        if (location != null)
        {
            com.fiskmods.heroes.common.sound.SHSounds.play(entity, location, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}

/** Transformation abilities (nanites, steel, shapeshifting): a timed toggle with cooldown. */
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
        if (entry.getBoolean(PowerProperty.IS_TOGGLE))
        {
            boolean state = !entry.isToggled(entity);
            entry.setToggled(entity, state);
            data.getData().set(Vars.SHIELD, state);
        }
        else
        {
            data.getData().set(Vars.SHIELD, true);
            data.getData().set(Vars.SHIELD_COOLDOWN, (short) 10);
        }

        AbilityData.playSound(entity, entry, "ENABLE");
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!entry.getBoolean(PowerProperty.IS_TOGGLE))
        {
            data.getData().set(Vars.SHIELD, false);
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
        return data != null && data.getData().get(Vars.INTANGIBLE) && !entry.getBoolean(PowerProperty.IS_ABSOLUTE);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (data.getData().get(Vars.INTANGIBLE))
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
        float min = entry.getFloat(PowerProperty.MIN_SIZE);
        float max = entry.getFloat(PowerProperty.MAX_SIZE);
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

        float range = entry.getFloat(PowerProperty.RANGE);
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

        float range = entry.getFloat(PowerProperty.RANGE);
        var hit = player.pick(range, 0.0F, false);

        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS)
        {
            Vec3 target = hit.getLocation();
            Vec3 delta = target.subtract(player.position()).normalize().scale(1.2D);

            player.setDeltaMovement(delta);
            player.hasImpulse = true;
            player.fallDistance = 0.0F;
            data.getData().set(Vars.WEB_SWINGING, true);
            data.getData().set(Vars.WEB_RAPPEL, true);
        }
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (data.getData().get(Vars.WEB_SWINGING) && entity.onGround())
        {
            data.getData().set(Vars.WEB_SWINGING, false);
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
        return data != null && data.getData().get(Vars.SHADOWFORM);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (data.getData().get(Vars.SHADOWFORM))
        {
            entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 10, 0, true, false, false));
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, Math.max(motion.y, -0.08D), motion.z);
            entity.fallDistance = 0.0F;
        }
    }
}

/** Telekinesis: hurls nearby entities away from the wearer. */
class ModifierTelekinesis extends Modifier
{
    ModifierTelekinesis(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        float range = entry.getFloat(PowerProperty.RANGE);

        for (LivingEntity target : entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(range), e -> e != entity))
        {
            Vec3 delta = target.position().subtract(entity.position()).normalize().scale(1.5D);
            target.setDeltaMovement(delta.x, Math.max(0.4D, delta.y), delta.z);
            target.hurtMarked = true;
            target.hurt(entity.damageSources().magic(), 2.0F);
        }

        entity.level().playSound(null, entity.blockPosition(), net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.8F);
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
            data.getData().set(Vars.PUNCH_TIMER, Math.min(entry.getInt(PowerProperty.CHARGE_TIME), timer + 1));
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
        float radius = Math.min(entry.getFloat(PowerProperty.RADIUS), 16.0F);
        float knockback = entry.getFloat(PowerProperty.KNOCKBACK);

        for (LivingEntity target : entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius), e -> e != entity))
        {
            Vec3 delta = target.position().subtract(entity.position()).normalize().scale(Math.max(0.4F, knockback));
            target.setDeltaMovement(delta.x, 0.4D, delta.z);
            target.hurtMarked = true;
        }

        if (entry.getBoolean(PowerProperty.CAN_DO_GRIEFING) && entity.level() instanceof ServerLevel level)
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
