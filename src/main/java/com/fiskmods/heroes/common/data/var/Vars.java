package com.fiskmods.heroes.common.data.var;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.DataRegistry;
import com.fiskmods.heroes.common.data.DataType;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * The mod's built-in data variables, plus the helpers the rest of the code uses to read them.
 * Hero packs may declare additional variables; these are the ones the gameplay systems rely on.
 */
public final class Vars
{
    private static final ResourceLocation MODID = new ResourceLocation(FiskHeroes.MODID, "internal");

    /* --- Speedster --- */
    public static final DataVar<Byte> SPEED = register("speed", DataType.BYTE, (byte) 4, false);
    public static final DataVar<Boolean> SPEEDING = register("speeding", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> SLOW_MOTION = register("slow_motion", DataType.BOOLEAN, false, true);
    public static final DataVar<Byte> SPEED_EXPERIENCE_LEVEL = register("speed_experience_level", DataType.BYTE, (byte) 0, false);
    public static final DataVar<Integer> SPEED_EXPERIENCE_TOTAL = register("speed_experience_total", DataType.INT, 0, false);
    public static final DataVar<Float> SPEED_EXPERIENCE_BAR = register("speed_experience_bar", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> TREADMILL_DECREASING = register("treadmill_decreasing", DataType.BOOLEAN, true, false);

    /* --- Suit states --- */
    public static final DataVar<Boolean> FLYING = register("flying", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> HOVERING = register("hovering", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> INTANGIBLE = register("intangible", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> INVISIBLE = register("invisible", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> SHIELD = register("shield", DataType.BOOLEAN, false, true);
    public static final DataVar<Float> SHIELD_COOLDOWN = register("shield_cooldown", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> BLADE = register("blade", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> PUNCHMODE = register("punchmode", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> SHADOWFORM = register("shadowform", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> MASK_OPEN = register("mask_open", DataType.BOOLEAN, false, true);
    public static final DataVar<Float> MASK_OPEN_TIMER = register("mask_open_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Boolean> SUIT_OPEN = register("suit_open", DataType.BOOLEAN, false, true);
    public static final DataVar<Float> SUIT_OPEN_TIMER = register("suit_open_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Boolean> GLIDING = register("gliding", DataType.BOOLEAN, false, true);
    public static final DataVar<Float> GLIDING_TIMER = register("gliding_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Boolean> WEB_SWINGING = register("web_swinging", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> AIMING = register("aiming", DataType.BOOLEAN, false, true);
    public static final DataVar<Integer> AIMING_TIMER = register("aiming_timer", DataType.INT, 0, true);
    public static final DataVar<Boolean> HORIZONTAL_BOW = register("horizontal_bow", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> SHIELD_BLOCKING = register("shield_blocking", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> LIGHTSOUT = register("lightsout", DataType.BOOLEAN, false, true);

    /* --- Size / shapes --- */
    public static final DataVar<Float> SCALE = register("scale", DataType.FLOAT, 1.0F, true);
    public static final DataVar<Integer> SIZE_STATE = register("size_state", DataType.INT, 0, true);
    public static final DataVar<Byte> SIZE_STATE_BYTE = register("size_state_byte", DataType.BYTE, (byte) 0, true);
    public static final DataVar<Float> SHRINK_TIMER = register("shrink_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Float> GIANT_TIMER = register("giant_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Boolean> GIANT_MODE = register("giant_mode", DataType.BOOLEAN, false, true);
    public static final DataVar<Float> GIANT_MODE_TIMER = register("giant_mode_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Float> GIANT_MODE_COOLDOWN = register("giant_mode_cooldown", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> STEELED = register("steeled", DataType.BOOLEAN, false, true);
    public static final DataVar<Float> STEEL_TIMER = register("steel_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Float> STEEL_COOLDOWN = register("steel_cooldown", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> NANITES = register("nanites", DataType.BOOLEAN, false, true);
    public static final DataVar<Float> NANITE_TIMER = register("nanite_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Float> NANITE_COOLDOWN = register("nanite_cooldown", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> METAL_SKIN = register("metal_skin", DataType.BOOLEAN, false, true);
    public static final DataVar<Float> METAL_HEAT = register("metal_heat", DataType.FLOAT, 0.0F, true);
    public static final DataVar<Boolean> SHAPE_SHIFT = register("shape_shift", DataType.BOOLEAN, false, true);
    public static final DataVar<Float> SHAPE_SHIFT_TIMER = register("shape_shift_timer", DataType.FLOAT_INTERP, 0.0F, true);

    /* --- Flight / boosts --- */
    public static final DataVar<Float> BOOSTER_TIMER = register("booster_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Float> BOOSTER_LEFT_TIMER = register("booster_left_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Float> BOOSTER_RIGHT_TIMER = register("booster_right_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Byte> FLIGHT_SUPER_BOOST = register("flight_super_boost", DataType.BYTE, (byte) 0, true);
    public static final DataVar<Float> FLIGHT_SUPER_BOOST_TIMER = register("flight_super_boost_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Float> SUPER_BOOST_COOLDOWN = register("super_boost_cooldown", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Byte> SUPER_BOOST_TIMEOUT = register("super_boost_timeout", DataType.BYTE, (byte) 0, true);
    public static final DataVar<Float> WING_TIMER = register("wing_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Float> JETPACK_TIMER = register("jetpacking_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Boolean> JETPACKING = register("jetpacking", DataType.BOOLEAN, false, true);
    public static final DataVar<Float> SPEED_SPRINT_TIMER = register("speed_sprint_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Float> BARREL_ROLL_TIMER = register("barrel_roll_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Float> BARREL_ROLL = register("barrel_roll", DataType.FLOAT, 0.0F, true);

    /* --- Combat --- */
    public static final DataVar<Boolean> BEAM_CHARGING = register("beam_charging", DataType.BOOLEAN, false, true);
    public static final DataVar<Float> BEAM_CHARGE = register("beam_charge", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Integer> PUNCH_TIMER = register("punch_timer", DataType.INT, 0, true);
    public static final DataVar<Float> RECOIL = register("recoil", DataType.FLOAT, 0.0F, true);
    public static final DataVar<Boolean> GRABBED = register("grabbed", DataType.BOOLEAN, false, true);
    public static final DataVar<Integer> GRAB_ID = register("grab_id", DataType.INT, 0, true);
    public static final DataVar<Float> GRAB_DISTANCE = register("grab_distance", DataType.FLOAT, 0.0F, true);
    public static final DataVar<Boolean> BLOCKING = register("blocking", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> WALL_CRAWLING = register("wall_crawling", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> WEB_RAPPEL = register("web_rappel", DataType.BOOLEAN, false, true);
    public static final DataVar<Float> WEB_RAPPEL_TIMER = register("web_rappel_timer", DataType.FLOAT_INTERP, 0.0F, true);

    /* --- Misc --- */
    public static final DataVar<Integer> PLAYER_ANIMATION = register("player_animation", DataType.INT, 0, true);
    public static final DataVar<Integer> SELECTED_ARROW = register("selected_arrow", DataType.INT, 0, false);
    public static final DataVar<Float> TICKS_SINCE = register("ticks_since", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> INVULNERABLE = register("invulnerable", DataType.BOOLEAN, false, false);
    public static final DataVar<Boolean> FLIGHT_LOCKED = register("flight_locked", DataType.BOOLEAN, false, false);
    public static final DataVar<Boolean> GRAVITY_DISABLED = register("gravity_disabled", DataType.BOOLEAN, false, false);
    public static final DataVar<Boolean> NO_GRAVITY = register("no_gravity", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> LIGHTS_OUT = register("lights_out", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> HOVER = register("hover", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> SENTRY_MODE = register("sentry_mode", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> CRYO_CHARGING = register("cryo_charging", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> DISGUISED = register("disguised", DataType.BOOLEAN, false, true);
    public static final DataVar<Float> SPEED_DISINTEGRATION = register("speed_disintegration", DataType.FLOAT, 0.0F, true);

    private Vars()
    {
    }

    /**
     * Forces this class to initialise so that every built-in variable is present in the
     * {@link DataRegistry}. Called before hero packs are loaded.
     */
    public static void ensureRegistered()
    {
        // Static initialisation performs the registration.
    }

    private static <T> DataVar<T> register(String name, DataType<T> type, T defaultValue, boolean resetWithoutSuit)
    {
        DataVar<?> var = DataRegistry.INSTANCE.register(new ResourceLocation(FiskHeroes.MODID, name), type, resetWithoutSuit);
        return (DataVar<T>) var;
    }

    public static boolean getToggleState(LivingEntity entity, ResourceLocation id)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);

        if (data != null)
        {
            return data.isToggleEnabled(id);
        }

        return false;
    }

    public static void setToggleState(LivingEntity entity, ResourceLocation id, boolean state)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);

        if (data != null)
        {
            data.setToggleEnabled(id, state);
        }
    }

    /* --- Convenience accessors used by the gameplay code --- */

    public static float getScale(LivingEntity entity)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);
        return data != null ? data.getData().get(SCALE) : 1.0F;
    }

    public static boolean getBoolean(LivingEntity entity, DataVar<Boolean> var)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);
        return data != null && data.getData().get(var);
    }

    public static float getFloat(LivingEntity entity, DataVar<Float> var)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);
        return data != null ? data.getData().get(var) : var.getDefault();
    }
}
