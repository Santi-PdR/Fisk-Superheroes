package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
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
        case "TENTACLE_JAB" -> attack(entity, entry, entry.get(PowerProperty.DAMAGE_PROFILE),
                entry.getFloat(PowerProperty.RANGE), "JAB_START");
        case "TENTACLE_GRAB" -> grab(entity, entry);
        case "TENTACLE_STRIKE" ->
        {
            if (entity instanceof Player player)
            {
                player.getPersistentData().putLong(STRIKE_START_TICK, player.level().getGameTime());
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

        JsonObject strike = object(entry.get(PowerProperty.TENTACLE_STRIKE));
        if (strike == null) return;

        int chargeTime = Math.max(1, strike.has("chargeTime") ? strike.get("chargeTime").getAsInt() : 20);
        float charge = net.minecraft.util.Mth.clamp((player.level().getGameTime() - started) / (float) chargeTime, 0.0F, 1.0F);
        float damageScale = charge * charge;
        // The source only releases the attack after the tentacle's squared charge exceeds 0.3.
        if (damageScale <= 0.3F) return;

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
        data.getData().set(Vars.TENTACLES_ACTIVE, deployed);
        data.getData().set(Vars.TENTACLES_RETRACTING, !deployed);
        data.getData().set(Vars.TENTACLE_EXTEND_TIMER, deployed ? 0.0F : 20.0F);
        entry.setToggled(entity, deployed);
        AbilityData.playSound(entity, entry, deployed ? "ENABLE" : "DISABLE");
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
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        return player.level().getEntitiesOfClass(LivingEntity.class, search,
                target -> target != player && target.isAlive() && target.isPickable()).stream()
                .filter(target -> {
                    Vec3 delta = target.getBoundingBox().getCenter().subtract(eye);
                    double along = delta.dot(look);
                    if (along < 0.0D || along > range) return false;
                    Vec3 nearest = eye.add(look.scale(along));
                    return target.getBoundingBox().inflate(0.55D).contains(nearest);
                })
                .min(java.util.Comparator.comparingDouble(target -> target.distanceToSqr(player)))
                .orElse(null);
    }

    private static JsonObject object(JsonElement element)
    {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }
}
