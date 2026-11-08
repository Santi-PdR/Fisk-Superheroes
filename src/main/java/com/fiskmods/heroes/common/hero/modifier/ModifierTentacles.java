package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.util.TentacleVisualMath;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-side controller for Doctor Octopus's mechanical-arm power. */
public final class ModifierTentacles extends Modifier
{
    private static final String STRIKE_START_TICK = "FiskHeroesTentacleStrikeStart";
    private static final String STRIKE_CHARGED_SOUND = "FiskHeroesTentacleStrikeChargedSound";

    public ModifierTentacles(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        setDeployed(entity, entry, data, !data.getData().get(Vars.TENTACLES_ACTIVE));
    }

    /** Dispatches one of the distinct keybinds backed by the single tentacles power entry. */
    public void activateKey(LivingEntity entity, ModifierEntry entry, SHPlayerData data, String key)
    {
        if ("TENTACLES".equals(key))
        {
            onActivate(entity, entry, data);
            return;
        }

        if (!data.getData().get(Vars.TENTACLES_ACTIVE))
        {
            return;
        }

        switch (key)
        {
        case "TENTACLE_JAB" ->
        {
            startVisual(entity, data, TentacleVisualMath.JAB, true);
            attack(entity, entry, entry.get(PowerProperty.DAMAGE_PROFILE), entry.getFloat(PowerProperty.RANGE), "JAB_START");
        }
        case "TENTACLE_GRAB" ->
        {
            startVisual(entity, data, TentacleVisualMath.GRAB, true);
            grab(entity, entry);
        }
        case "TENTACLE_STRIKE" ->
        {
            if (entity instanceof Player player)
            {
                player.getPersistentData().putLong(STRIKE_START_TICK, player.level().getGameTime());
                startVisual(entity, data, TentacleVisualMath.CHARGE, true);
                player.getPersistentData().remove(STRIKE_CHARGED_SOUND);
                AbilityData.playSound(entity, entry, "STRIKE_START");
            }
        }
        default -> { }
        }
    }

    /** The original strike charges while held and launches when the key is released. */
    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!(entity instanceof Player player)) return;

        var persistent = player.getPersistentData();
        if (!persistent.contains(STRIKE_START_TICK)) return;

        long started = persistent.getLong(STRIKE_START_TICK);
        persistent.remove(STRIKE_START_TICK);
        persistent.remove(STRIKE_CHARGED_SOUND);
        if (!data.getData().get(Vars.TENTACLES_ACTIVE) || !entry.isEnabled()
                || !entry.isModifierEnabled(player, data)
                || player.hasEffect(ModEffects.TUTRIDIUM.get()))
        {
            clearVisual(data);
            return;
        }

        JsonObject strike = object(entry.get(PowerProperty.TENTACLE_STRIKE));
        if (strike == null) { clearVisual(data); return; }

        int chargeTime = Math.max(1, strike.has("chargeTime") ? strike.get("chargeTime").getAsInt() : 20);
        float charge = net.minecraft.util.Mth.clamp((player.level().getGameTime() - started) / (float) chargeTime, 0.0F, 1.0F);
        float damageScale = charge * charge;
        // The source only releases the attack after the tentacle's squared charge exceeds 0.3.
        if (damageScale <= 0.3F) { clearVisual(data); return; }
        startVisual(entity, data, TentacleVisualMath.STRIKE, false);

        LivingEntity target = findTarget(player, entry.getFloat(PowerProperty.RANGE));
        if (target == null) return;

        JsonElement profile = strike.get("damageProfile");
        float damage = DamageGroups.profileDamage(profile, 17.0F) * damageScale;
        DamageGroups.withDamageProfile(profile,
                () -> target.hurt(player.damageSources().playerAttack(player), damage));
        AbilityData.playSound(entity, entry, "STRIKE_IMPACT");
    }

    private static void setDeployed(LivingEntity entity, ModifierEntry entry, SHPlayerData data, boolean deployed)
    {
        if (!deployed) cancelStrike(entity);
        data.getData().set(Vars.TENTACLES_ACTIVE, deployed);
        data.getData().set(Vars.TENTACLES_RETRACTING, !deployed);
        data.getData().set(Vars.TENTACLE_EXTEND_TIMER, deployed ? 0.0F : 20.0F);
        entry.setToggled(entity, deployed);
        AbilityData.playSound(entity, entry, deployed ? "ENABLE" : "DISABLE");
    }

    private static void startVisual(LivingEntity entity, SHPlayerData data, int action, boolean chooseArm)
    {
        if (entity.level().isClientSide) return;
        if (chooseArm) data.getData().set(Vars.TENTACLE_VISUAL_INDEX, Math.floorMod(entity.tickCount, 4));
        data.getData().set(Vars.TENTACLE_VISUAL_ACTION, action);
        data.getData().set(Vars.TENTACLE_VISUAL_TIMER, 0.0F);
    }

    private static void clearVisual(SHPlayerData data)
    {
        data.getData().set(Vars.TENTACLE_VISUAL_ACTION, TentacleVisualMath.NONE);
        data.getData().set(Vars.TENTACLE_VISUAL_TIMER, 0.0F);
    }

    /** Cancel rather than release a charged attack on retraction, suit change or disconnect. */
    public static void cancelStrike(LivingEntity entity)
    {
        if (entity instanceof Player player)
        {
            player.getPersistentData().remove(STRIKE_START_TICK);
            player.getPersistentData().remove(STRIKE_CHARGED_SOUND);
            if (!player.level().isClientSide)
            {
                SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
                if (data != null) clearVisual(data);
            }
        }
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!entity.level().isClientSide && entity instanceof Player player)
        {
            var persistent = player.getPersistentData();
            long started = persistent.getLong(STRIKE_START_TICK);
            JsonObject strike = object(entry.get(PowerProperty.TENTACLE_STRIKE));
            int chargeTime = strike != null && strike.has("chargeTime")
                    ? Math.max(1, strike.get("chargeTime").getAsInt()) : 20;
            if (started > 0L && !persistent.getBoolean(STRIKE_CHARGED_SOUND)
                    && player.level().getGameTime() - started >= chargeTime)
            {
                persistent.putBoolean(STRIKE_CHARGED_SOUND, true);
                AbilityData.playSound(player, entry, "STRIKE_CHARGED");
            }
        }

        if (!entity.level().isClientSide)
        {
            int action = data.getData().get(Vars.TENTACLE_VISUAL_ACTION);
            if (action == TentacleVisualMath.CHARGE && entity instanceof Player player)
            {
                if (!player.getPersistentData().contains(STRIKE_START_TICK)) clearVisual(data);
                else
                {
                    JsonObject strike = object(entry.get(PowerProperty.TENTACLE_STRIKE));
                    int duration = strike != null && strike.has("chargeTime") ? Math.max(1, strike.get("chargeTime").getAsInt()) : 20;
                    long start = player.getPersistentData().getLong(STRIKE_START_TICK);
                    data.getData().set(Vars.TENTACLE_VISUAL_TIMER,
                            net.minecraft.util.Mth.clamp((player.level().getGameTime() - start) / (float) duration, 0.0F, 1.0F));
                }
            }
            else if (action != TentacleVisualMath.NONE)
            {
                float timer = data.getData().get(Vars.TENTACLE_VISUAL_TIMER) + 1.0F / TentacleVisualMath.ACTION_TICKS;
                if (timer >= 1.0F) clearVisual(data);
                else data.getData().set(Vars.TENTACLE_VISUAL_TIMER, timer);
            }
        }

        boolean deployed = data.getData().get(Vars.TENTACLES_ACTIVE);
        float timer = data.getData().get(Vars.TENTACLE_EXTEND_TIMER);
        float next = deployed ? Math.min(20.0F, timer + 1.0F) : Math.max(0.0F, timer - 1.0F);
        if (next != timer)
        {
            data.getData().set(Vars.TENTACLE_EXTEND_TIMER, next);
        }
        if (!deployed && next == 0.0F && data.getData().get(Vars.TENTACLES_RETRACTING))
        {
            data.getData().set(Vars.TENTACLES_RETRACTING, false);
        }
    }

    private static void attack(LivingEntity entity, ModifierEntry entry, JsonElement profile, float range, String sound)
    {
        if (!(entity instanceof Player player) || range <= 0.0F) return;
        LivingEntity target = findTarget(player, range);
        if (target == null) return;

        float damage = DamageGroups.profileDamage(profile, 8.0F);
        AbilityData.playSound(entity, entry, sound);
        DamageGroups.withDamageProfile(profile, () -> target.hurt(player.damageSources().playerAttack(player), damage));
    }

    private static void grab(LivingEntity entity, ModifierEntry entry)
    {
        if (!(entity instanceof Player player)) return;
        LivingEntity target = findTarget(player, entry.getFloat(PowerProperty.RANGE));
        if (target == null) return;

        AbilityData.playSound(entity, entry, "GRAB_START");
        Vec3 pull = player.position().add(0.0D, 0.8D, 0.0D).subtract(target.position()).normalize().scale(0.8D);
        target.setDeltaMovement(pull);
        target.hurtMarked = true;
    }

    private static LivingEntity findTarget(Player player, float range)
    {
        if (range <= 0.0F) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));
        // The original moving tentacles collide with blocks before searching for entities.
        // Clip the instant approximation as well, so a jab, grab or strike cannot reach
        // through an opaque collision shape. Fluids do not block the arm.
        var wall = player.level().clip(new net.minecraft.world.level.ClipContext(eye, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (wall.getType() != net.minecraft.world.phys.HitResult.Type.MISS) end = wall.getLocation();
        final Vec3 rayEnd = end;
        AABB search = player.getBoundingBox().expandTowards(rayEnd.subtract(eye)).inflate(1.0D);
        return player.level().getEntitiesOfClass(LivingEntity.class, search,
                target -> target != player && target.isAlive() && target.isPickable()).stream()
                .filter(target -> target.getBoundingBox().inflate(0.55D).contains(eye)
                        || target.getBoundingBox().inflate(0.55D).clip(eye, rayEnd).isPresent())
                .min(java.util.Comparator.comparingDouble(target -> {
                    AABB box = target.getBoundingBox().inflate(0.55D);
                    return box.contains(eye) ? 0.0D
                            : box.clip(eye, rayEnd).map(eye::distanceToSqr).orElse(Double.MAX_VALUE);
                }))
                .orElse(null);
    }

    private static JsonObject object(JsonElement element)
    {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }
}
