package com.fiskmods.heroes.common.data.var;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.DataRegistry;
import com.fiskmods.heroes.common.data.DataType;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * The mod's data variables.
 * <p>
 * The first block is the full built-in registry of the original 2.4.0 release (the names and types
 * come from the original data mapping); the second block holds the variables the bundled hero pack
 * declares for itself ({@code fiskheroes:dyn/*}, driven by the pack scripts); the last block is
 * port-internal bookkeeping that has no 1.7.10 counterpart.
 */
public final class Vars
{
  /* --- Built-in data variables (from the 2.4.0 data mapping) --- */
    public static final DataVar<Float> ABILITY_COOLDOWNS = register("ability_cooldowns", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Byte> AIM_SPRINT_COOLDOWN = register("aim_sprint_cooldown", DataType.BYTE, (byte) 0, false);
    public static final DataVar<Float> AIMED_TIMER = register("aimed_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> AIMING = register("aiming", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> AIMING_TIMER = register("aiming_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> BARREL_ROLL_TIMER = register("barrel_roll_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> BEAM_CHARGE = register("beam_charge", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> BEAM_CHARGING = register("beam_charging", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> BEAM_SHOOTING = register("beam_shooting", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> BEAM_SHOOTING_TIMER = register("beam_shooting_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> BLADE = register("blade", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> BLADE_TIMER = register("blade_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> CRYO_CHARGE = register("cryo_charge", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> CRYO_CHARGING = register("cryo_charging", DataType.BOOLEAN, false, false);
    public static final DataVar<String> CURRENT_ARROW = register("current_arrow", DataType.STRING, "", false);
    public static final DataVar<String> DISGUISE = register("disguise", DataType.STRING, "", false);
    public static final DataVar<Float> ENERGY_CHARGE = register("energy_charge", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> ENERGY_CHARGING = register("energy_charging", DataType.BOOLEAN, false, false);
    public static final DataVar<Boolean> ENERGY_PROJECTION = register("energy_projection", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> ENERGY_PROJECTION_TIMER = register("energy_projection_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Byte> EQUIPPED_AMMO_BAG = register("equipped_ammo_bag", DataType.BYTE, (byte) -1, false);
    public static final DataVar<String> EQUIPPED_QUIVER = register("equipped_quiver", DataType.STRING, "", false);
    public static final DataVar<Byte> EQUIPPED_QUIVER_SLOT = register("equipped_quiver_slot", DataType.BYTE, (byte) -1, false);
    public static final DataVar<Byte> EQUIPPED_TACHYON_DEVICE_SLOT = register("equipped_tachyon_device_slot", DataType.BYTE, (byte) -1, false);
    public static final DataVar<Byte> FLIGHT_ANIMATION = register("flight_animation", DataType.BYTE, (byte) 0, false);
    public static final DataVar<Float> FLIGHT_BOOST_TIMER = register("flight_boost_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> FLIGHT_DIVING = register("flight_diving", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> FLIGHT_TIMER = register("flight_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> FLYING = register("flying", DataType.BOOLEAN, false, false);
    public static final DataVar<Boolean> GLIDE_FLYING = register("glide_flying", DataType.BOOLEAN, false, false);
    public static final DataVar<Boolean> GLIDING = register("gliding", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> GLIDING_TIMER = register("gliding_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> GRAB_DISTANCE = register("grab_distance", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Integer> GRAB_ID = register("grab_id", DataType.INT, -1, false);
    public static final DataVar<Integer> GRABBED_BY = register("grabbed_by", DataType.INT, -1, false);
    public static final DataVar<Float> GRAVITY_AMOUNT = register("gravity_amount", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> GRAVITY_MANIP = register("gravity_manip", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> GUN_SHOOTING_TIMER = register("gun_shooting_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> HAT_TIP = register("hat_tip", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> HEAT_VISION = register("heat_vision", DataType.BOOLEAN, false, false);
    public static final DataVar<Double> HEAT_VISION_LENGTH = register("heat_vision_length", DataType.DOUBLE, 0.0D, false);
    public static final DataVar<Float> HEAT_VISION_TIMER = register("heat_vision_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> HORIZONTAL_BOW = register("horizontal_bow", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> HORIZONTAL_BOW_TIMER = register("horizontal_bow_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> HOVERING = register("hovering", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> INTANGIBILITY_TIMER = register("intangibility_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> INTANGIBLE = register("intangible", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> INVISIBILITY_TIMER = register("invisibility_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> INVISIBLE = register("invisible", DataType.BOOLEAN, false, false);
    public static final DataVar<Boolean> IS_SWING_IN_PROGRESS = register("is_swing_in_progress", DataType.BOOLEAN, false, false);
    public static final DataVar<Boolean> JETPACKING = register("jetpacking", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> JETPACKING_TIMER = register("jetpacking_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> LEVITATE_TIMER = register("levitate_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> LIGHTSOUT = register("lightsout", DataType.BOOLEAN, false, false);
    public static final DataVar<Integer> LIGHTSOUT_ID = register("lightsout_id", DataType.INT, 0, false);
    public static final DataVar<Float> LIGHTSOUT_TIMER = register("lightsout_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> MASK_OPEN = register("mask_open", DataType.BOOLEAN, false, false);
    public static final DataVar<Byte> MASK_OPEN_TIMER = register("mask_open_timer", DataType.BYTE, (byte) 0, false);
    public static final DataVar<Float> MASK_OPEN_TIMER2 = register("mask_open_timer2", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> METAL_HEAT = register("metal_heat", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Short> METAL_HEAT_COOLDOWN = register("metal_heat_cooldown", DataType.SHORT, (short) 0, false);
    public static final DataVar<Boolean> MOVING = register("moving", DataType.BOOLEAN, false, false);
    public static final DataVar<Boolean> PENETRATE_MARTIAN_INVIS = register("penetrate_martian_invis", DataType.BOOLEAN, false, false);
    public static final DataVar<Integer> PLAYER_ANIMATION = register("player_animation", DataType.INT, 0, false);
    public static final DataVar<Float> PREV_AIMED_TIMER = register("prev_aimed_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_AIMING_TIMER = register("prev_aiming_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_BARREL_ROLL_TIMER = register("prev_barrel_roll_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_BEAM_CHARGE = register("prev_beam_charge", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_BEAM_SHOOTING = register("prev_beam_shooting", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_BEAM_SHOOTING_TIMER = register("prev_beam_shooting_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_BLADE_TIMER = register("prev_blade_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_ENERGY_CHARGE = register("prev_energy_charge", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_ENERGY_PROJECTION_TIMER = register("prev_energy_projection_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_FLIGHT_BOOST_TIMER = register("prev_flight_boost_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_FLIGHT_TIMER = register("prev_flight_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_GLIDING_TIMER = register("prev_gliding_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_GUN_SHOOTING_TIMER = register("prev_gun_shooting_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_HAT_TIP = register("prev_hat_tip", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Double> PREV_HEAT_VISION_LENGTH = register("prev_heat_vision_length", DataType.DOUBLE, 0.0D, false);
    public static final DataVar<Float> PREV_HEAT_VISION_TIMER = register("prev_heat_vision_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_HORIZONTAL_BOW_TIMER = register("prev_horizontal_bow_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_INTANGIBILITY_TIMER = register("prev_intangibility_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_INVISIBILITY_TIMER = register("prev_invisibility_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_JETPACKING_TIMER = register("prev_jetpacking_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_LEVITATE_TIMER = register("prev_levitate_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_LIGHTSOUT_TIMER = register("prev_lightsout_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_MASK_OPEN_TIMER2 = register("prev_mask_open_timer2", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_METAL_HEAT = register("prev_metal_heat", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> PREV_ON_GROUND = register("prev_on_ground", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> PREV_PUNCH_CHARGE = register("prev_punch_charge", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_PUNCHMODE_TIMER = register("prev_punchmode_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_RECOIL = register("prev_recoil", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_RELOAD_TIMER = register("prev_reload_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_SCALE = register("prev_scale", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_SCOPE_TIMER = register("prev_scope_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_SHADOWFORM_TIMER = register("prev_shadowform_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_SHIELD_BLOCKING_TIMER = register("prev_shield_blocking_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_SHIELD_TIMER = register("prev_shield_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_SPELLCAST_TIMER = register("prev_spellcast_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_SWING_PROGRESS = register("prev_swing_progress", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_TACHYON_CHARGE = register("prev_tachyon_charge", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_TELEPORT_TIMER = register("prev_teleport_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_TENTACLE_EXTEND_TIMER = register("prev_tentacle_extend_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_TREADMILL_LIMB_FACTOR = register("prev_treadmill_limb_factor", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_TREADMILL_LIMB_PROGRESS = register("prev_treadmill_limb_progress", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Byte> PREV_UTILITY_BELT_TYPE = register("prev_utility_belt_type", DataType.BYTE, (byte) -1, false);
    public static final DataVar<Float> PREV_VEL9_CONVERT = register("prev_vel9_convert", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_WEAPON_ANIMATION_SPIN_TIMER = register("prev_weapon_animation_spin_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_WEAPON_ANIMATION_TIMER = register("prev_weapon_animation_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Integer> PREV_WEAPON_COOLDOWN = register("prev_weapon_cooldown", DataType.INT, 0, false);
    public static final DataVar<Float> PREV_WEB_AIM_LEFT_TIMER = register("prev_web_aim_left_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_WEB_AIM_RIGHT_TIMER = register("prev_web_aim_right_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_WEB_RAPPEL_TIMER = register("prev_web_rappel_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_WEB_SWINGING_TIMER = register("prev_web_swinging_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PREV_WING_ANIMATION_TIMER = register("prev_wing_animation_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> PUNCH_CHARGE = register("punch_charge", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> PUNCH_CHARGED = register("punch_charged", DataType.BOOLEAN, false, false);
    public static final DataVar<Boolean> PUNCHMODE = register("punchmode", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> PUNCHMODE_TIMER = register("punchmode_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<String> QR_ORIGIN = register("qr_origin", DataType.STRING, "", false);
    public static final DataVar<Float> QR_TIMER = register("qr_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> RECOIL = register("recoil", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> RELOAD_TIMER = register("reload_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> SCALE = register("scale", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> SCOPE_TIMER = register("scope_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> SCOPING = register("scoping", DataType.BOOLEAN, false, false);
    public static final DataVar<Byte> SELECTED_ARROW = register("selected_arrow", DataType.BYTE, (byte) 0, false);
    public static final DataVar<Boolean> SHADOWFORM = register("shadowform", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> SHADOWFORM_TIMER = register("shadowform_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> SHAPE_SHIFT_TIMER = register("shape_shift_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> SHAPE_SHIFTING = register("shape_shifting", DataType.BOOLEAN, false, false);
    public static final DataVar<String> SHAPE_SHIFTING_FROM = register("shape_shifting_from", DataType.STRING, "", false);
    public static final DataVar<String> SHAPE_SHIFTING_TO = register("shape_shifting_to", DataType.STRING, "", false);
    public static final DataVar<Boolean> SHIELD = register("shield", DataType.BOOLEAN, false, false);
    public static final DataVar<Boolean> SHIELD_BLOCKING = register("shield_blocking", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> SHIELD_BLOCKING_TIMER = register("shield_blocking_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Short> SHIELD_COOLDOWN = register("shield_cooldown", DataType.SHORT, (short) 0, false);
    public static final DataVar<Float> SHIELD_DAMAGE = register("shield_damage", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> SHIELD_TIMER = register("shield_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Byte> SIZE_STATE = register("size_state", DataType.BYTE, (byte) 0, false);
    public static final DataVar<Boolean> SLOW_MOTION = register("slow_motion", DataType.BOOLEAN, false, false);
    public static final DataVar<Boolean> SONIC_WAVES = register("sonic_waves", DataType.BOOLEAN, false, false);
    public static final DataVar<Byte> SPEED = register("speed", DataType.BYTE, (byte) 4, false);
    public static final DataVar<Float> SPEED_EXPERIENCE_BAR = register("speed_experience_bar", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Byte> SPEED_EXPERIENCE_LEVEL = register("speed_experience_level", DataType.BYTE, (byte) 0, false);
    public static final DataVar<Integer> SPEED_EXPERIENCE_TOTAL = register("speed_experience_total", DataType.INT, 0, false);
    public static final DataVar<Integer> SPEED_LEVEL_UP_COOLDOWN = register("speed_level_up_cooldown", DataType.INT, 0, false);
    public static final DataVar<Boolean> SPEED_SPRINTING = register("speed_sprinting", DataType.BOOLEAN, false, false);
    public static final DataVar<Boolean> SPEEDING = register("speeding", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> SPELL_FRACTION = register("spell_fraction", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> SPELLCAST_TIMER = register("spellcast_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> SPODERMEN = register("spodermen", DataType.BOOLEAN, false, false);
    public static final DataVar<Boolean> SUIT_OPEN = register("suit_open", DataType.BOOLEAN, false, false);
    public static final DataVar<Byte> SUIT_OPEN_TIMER = register("suit_open_timer", DataType.BYTE, (byte) 0, false);
    public static final DataVar<Boolean> SUPERHERO_LANDING = register("superhero_landing", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> SWING_PROGRESS = register("swing_progress", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Byte> SWING_PROGRESS_INT = register("swing_progress_int", DataType.BYTE, (byte) 0, false);
    public static final DataVar<Float> TACHYON_CHARGE = register("tachyon_charge", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> TACHYON_DEVICE_ON = register("tachyon_device_on", DataType.BOOLEAN, false, false);
    public static final DataVar<Boolean> TELEKINESIS = register("telekinesis", DataType.BOOLEAN, false, false);
    public static final DataVar<Byte> TELEPORT_DELAY = register("teleport_delay", DataType.BYTE, (byte) 0, false);
    public static final DataVar<String> TELEPORT_DEST = register("teleport_dest", DataType.STRING, "", false);
    public static final DataVar<Float> TELEPORT_TIMER = register("teleport_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> TENTACLE_EXTEND_TIMER = register("tentacle_extend_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Boolean> TENTACLE_LIFT = register("tentacle_lift", DataType.BOOLEAN, false, false);
    public static final DataVar<String> TENTACLES = register("tentacles", DataType.STRING, "", false);
    public static final DataVar<Boolean> TENTACLES_RETRACTING = register("tentacles_retracting", DataType.BOOLEAN, false, false);
    public static final DataVar<Short> TICKS_SINCE_GUNSHOT = register("ticks_since_gunshot", DataType.SHORT, (short) 0, false);
    public static final DataVar<Short> TICKS_SINCE_SHIELD_DAMAGED = register("ticks_since_shield_damaged", DataType.SHORT, (short) 0, false);
    public static final DataVar<Short> TICKS_SINCE_SPRINTING = register("ticks_since_sprinting", DataType.SHORT, (short) 0, false);
    public static final DataVar<Short> TICKS_SINCE_SWINGING = register("ticks_since_swinging", DataType.SHORT, (short) 0, false);
    public static final DataVar<Short> TICKS_SINCE_WEBSHOOT = register("ticks_since_webshoot", DataType.SHORT, (short) 0, false);
    public static final DataVar<Short> TIME_SINCE_DAMAGED = register("time_since_damaged", DataType.SHORT, (short) 0, false);
    public static final DataVar<Byte> TONFA_STATE = register("tonfa_state", DataType.BYTE, (byte) 0, false);
    public static final DataVar<Boolean> TREADMILL_DECREASING = register("treadmill_decreasing", DataType.BOOLEAN, true, false);
    public static final DataVar<Float> TREADMILL_LIMB_FACTOR = register("treadmill_limb_factor", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> TREADMILL_LIMB_PROGRESS = register("treadmill_limb_progress", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Byte> UTILITY_BELT_TYPE = register("utility_belt_type", DataType.BYTE, (byte) -1, false);
    public static final DataVar<Float> VEL9_CONVERT = register("vel9_convert", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Byte> WALL_CRAWLING = register("wall_crawling", DataType.BYTE, (byte) 0, false);
    public static final DataVar<Float> WEAPON_ANIMATION_SPIN_TIMER = register("weapon_animation_spin_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> WEAPON_ANIMATION_TIMER = register("weapon_animation_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> WEB_AIM_LEFT_TIMER = register("web_aim_left_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> WEB_AIM_RIGHT_TIMER = register("web_aim_right_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> WEB_RAPPEL_TIMER = register("web_rappel_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Integer> WEB_ROPE_ID = register("web_rope_id", DataType.INT, -1, false);
    public static final DataVar<Boolean> WEB_SWINGING = register("web_swinging", DataType.BOOLEAN, false, false);
    public static final DataVar<Float> WEB_SWINGING_TIMER = register("web_swinging_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> WING_ANIMATION_TIMER = register("wing_animation_timer", DataType.FLOAT, 0.0F, false);
  /* --- Pack-declared dynamic variables (fiskheroes:dyn/*) --- */
    public static final DataVar<Float> STEELED = register("dyn/steeled", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> STEEL_TIMER = register("dyn/steel_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> STEEL_COOLDOWN = register("dyn/steel_cooldown", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> NANITES = register("dyn/nanites", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> NANITE_TIMER = register("dyn/nanite_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> NANITE_COOLDOWN = register("dyn/nanite_cooldown", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> GIANT_MODE = register("dyn/giant_mode", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> GIANT_MODE_TIMER = register("dyn/giant_mode_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> GIANT_MODE_COOLDOWN = register("dyn/giant_mode_cooldown", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> SHADOWFORM_COOLDOWN = register("dyn/shadowform_cooldown", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> INTANGIBILITY_COOLDOWN = register("dyn/intangibility_cooldown", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> BOOSTER_TIMER = register("dyn/booster_timer", DataType.FLOAT, 0.0F, true);
    public static final DataVar<Float> BOOSTER_R_TIMER = register("dyn/booster_r_timer", DataType.FLOAT, 0.0F, true);
    public static final DataVar<Float> BOOSTER_L_TIMER = register("dyn/booster_l_timer", DataType.FLOAT, 0.0F, true);
    public static final DataVar<Float> SUPERHERO_LANDING_TICKS = register("dyn/superhero_landing_ticks", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> SUPERHERO_LANDING_TIMER = register("dyn/superhero_landing_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> SHRINK_TIMER = register("dyn/shrink_timer", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> FLIGHT_SUPER_BOOST = register("dyn/flight_super_boost", DataType.FLOAT, 0.0F, true);
    public static final DataVar<Float> FLIGHT_SUPER_BOOST_TIMER = register("dyn/flight_super_boost_timer", DataType.FLOAT, 0.0F, true);
    public static final DataVar<Float> WING_TIMER = register("dyn/wing_timer", DataType.FLOAT, 0.0F, true);
    public static final DataVar<Float> SUPER_BOOST_COOLDOWN = register("dyn/super_boost_cooldown", DataType.FLOAT, 0.0F, false);
    public static final DataVar<Float> SUPER_BOOST_TIMEOUT = register("dyn/super_boost_timeout", DataType.FLOAT, 0.0F, true);
    public static final DataVar<Float> SPEED_SPRINT_TIMER = register("dyn/speed_sprint_timer", DataType.FLOAT, 0.0F, true);
  /* --- Built-in data variables (from the 2.4.0 data mapping) --- */
    public static final DataVar<Boolean> NO_GRAVITY = register("no_gravity", DataType.BOOLEAN, false, false);
    public static final DataVar<Integer> PUNCH_TIMER = register("punch_timer", DataType.INT, 0, false);
    public static final DataVar<Boolean> WEB_RAPPEL = register("web_rappel", DataType.BOOLEAN, false, true);
    public static final DataVar<Float> GIANT_TIMER = register("giant_timer", DataType.FLOAT_INTERP, 0.0F, true);
    public static final DataVar<Boolean> DISGUISED = register("disguised", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> SENTRY_MODE = register("sentry_mode", DataType.BOOLEAN, false, true);
    public static final DataVar<Boolean> METAL_SKIN = register("metal_skin", DataType.BOOLEAN, false, true);

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
