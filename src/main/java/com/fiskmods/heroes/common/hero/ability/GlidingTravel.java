package com.fiskmods.heroes.common.hero.ability;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.modifier.ModEffects;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** The original gliding travel calculation, adapted to modern movement and collision handling. */
public final class GlidingTravel
{
    private static boolean loggedClientTravel;
    private GlidingTravel() {}

    /** Called at the start of LivingEntity.travel; true means this method moved the player. */
    public static boolean travel(LivingEntity entity)
    {
        if (!(entity instanceof Player player) || entity.level().isClientSide && !player.isLocalPlayer()
                || player.onGround() || player.isPassenger() || player.isShiftKeyDown()
                || player.getAbilities().flying || player.isFallFlying() || player.hasEffect(ModEffects.TUTRIDIUM.get())) return false;
        var data = SHDataCapabilities.getPlayer(player);
        var hero = data != null ? data.getHeroType() : null;
        var entry = hero != null ? hero.getPowerContainer().getEntry(FiskHeroes.id("gliding")) : null;
        if (entry == null || !entry.isEnabled() || !entry.isModifierEnabled(player, data)
                || !data.getData().get(Vars.GLIDING)) return false;
        boolean powered = entry.getBoolean(player, PowerProperty.IS_POWERED);
        if (player.isInWater() && !powered) return false;
        if (entry.getBoolean(player, PowerProperty.IS_CONTROLLED)
                && !data.getData().get(Vars.GLIDE_FLYING)) return false;

        JsonElement definition = entry.get(player, PowerProperty.GLIDING);
        JsonObject config = definition != null && definition.isJsonObject() ? definition.getAsJsonObject() : null;
        double speed = number(config, "speed", 1.0D);
        double lift = number(config, "lift", 1.0D);
        double gravity = number(config, "gravity", 1.0D);
        double stall = number(config, "stall", 0.0D);
        Vec3 before = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(before.x * before.x + before.z * before.z);
        double effectiveGravity = gravity * (1.0D + stall * (1.0D - Math.min(horizontalSpeed / 2.0D, 1.0D)));
        if (before.y > -0.5D * effectiveGravity) player.fallDistance = 0.0F;
        Vec3 motion = step(before, player.getLookAngle(), player.getXRot(), speed, lift, gravity, stall,
                powered && player.isSprinting(), entry.getFloat(player, PowerProperty.BOOST_SPEED));
        if (player.level().isClientSide && !loggedClientTravel)
        {
            loggedClientTravel = true;
            FiskHeroes.LOGGER.info("Local gliding travel for {}: horizontal={}, vertical={}",
                    player.getGameProfile().getName(), Math.sqrt(motion.x * motion.x + motion.z * motion.z), motion.y);
        }
        player.setDeltaMovement(motion);
        player.move(MoverType.SELF, motion);
        return true;
    }

    /** Pure velocity step using the 1.7.10 equations; vanilla travel must not also run this tick. */
    public static Vec3 step(Vec3 motion, Vec3 look, float pitchDegrees, double speed, double lift,
            double gravity, double stall, boolean boost, double boostSpeed)
    {
        double pitch = Math.toRadians(pitchDegrees);
        double cos = Math.cos(pitch);
        double x = motion.x, y = motion.y, z = motion.z;
        if (boost)
        {
            double thrust = boostSpeed + (pitch < 0.0D ? (1.0D - cos) * 0.1D : 0.0D);
            x = x * 0.98D + look.x * thrust;
            y = y * 0.98D + look.y * thrust;
            z = z * 0.98D + look.z * thrust;
        }
        double velocity = Math.sqrt(x * x + z * z);
        if (stall > 0.0D)
        {
            double slow = 1.0D - Math.min(velocity / 2.0D, 1.0D);
            lift *= 1.0D - slow * stall;
            gravity *= 1.0D + slow * stall;
        }
        y -= 0.08D - cos * cos * (0.08D - 0.02D * gravity);
        if (cos > 1.0E-6D)
        {
            if (y < 0.0D)
            {
                double dive = -y * 0.1D * cos;
                y += dive * cos;
                x += look.x * dive;
                z += look.z * dive;
            }
            if (pitch < 0.0D)
            {
                double climb = velocity * Math.sin(pitch) * 0.04D * lift;
                y -= climb * 3.2D;
                x += look.x * climb / cos * speed;
                z += look.z * climb / cos * speed;
            }
            x += (look.x / cos * velocity - x) * 0.1D * speed;
            z += (look.z / cos * velocity - z) * 0.1D * speed;
        }
        return new Vec3(x * 0.99D, y * 0.98D, z * 0.99D);
    }

    private static double number(JsonObject config, String key, double fallback)
    {
        if (config == null || !config.has(key) || !config.get(key).isJsonPrimitive()
                || !config.getAsJsonPrimitive(key).isNumber()) return fallback;
        double value = config.get(key).getAsDouble();
        return Double.isFinite(value) ? value : fallback;
    }
}
