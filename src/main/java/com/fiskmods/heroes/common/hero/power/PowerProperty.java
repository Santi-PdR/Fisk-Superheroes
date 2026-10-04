package com.fiskmods.heroes.common.hero.power;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.fiskmods.heroes.common.spell.SpellSet;

/**
 * A tunable property of a {@link Modifier}. Power JSON files adjust these per power, e.g.
 * {@code "speed": 0.4} on {@code fiskheroes:controlled_flight}.
 */
public class PowerProperty<T>
{
    private static final Map<String, PowerProperty<?>> REGISTRY = new LinkedHashMap<>();

    public static final PowerProperty<Boolean> AFFECTS_USER = create("affectsUser", Boolean.FALSE);
    public static final PowerProperty<Boolean> CAN_BOOST = create("canBoost", Boolean.FALSE);
    public static final PowerProperty<Boolean> CAN_BREAK_GLASS = create("canBreakGlass", Boolean.FALSE);
    public static final PowerProperty<Boolean> CAN_DO_GRIEFING = create("canDoGriefing", Boolean.FALSE);
    public static final PowerProperty<Boolean> CAN_JUMP_ACTIVATE = create("canJumpActivate", Boolean.FALSE);
    public static final PowerProperty<Boolean> CAN_REACH_MOON = create("canReachMoon", Boolean.FALSE);
    public static final PowerProperty<Boolean> CAN_ROLL = create("canRoll", Boolean.FALSE);
    public static final PowerProperty<Boolean> CAN_USE_TACHYONS = create("canUseTachyons", Boolean.FALSE);
    public static final PowerProperty<Boolean> CAN_USE_TREADMILL = create("canUseTreadmill", Boolean.FALSE);
    public static final PowerProperty<Boolean> IS_ABSOLUTE = create("isAbsolute", Boolean.FALSE);
    public static final PowerProperty<Boolean> IS_CONTROLLED = create("isControlled", Boolean.FALSE);
    public static final PowerProperty<Boolean> IS_EXPLOSIVE = create("isExplosive", Boolean.FALSE);
    public static final PowerProperty<Boolean> IS_INSTANT = create("isInstant", Boolean.FALSE);
    public static final PowerProperty<Boolean> IS_POWERED = create("isPowered", Boolean.FALSE);
    public static final PowerProperty<Boolean> IS_TOGGLE = create("isToggle", Boolean.FALSE);
    public static final PowerProperty<Boolean> HIDE_HUD = create("hideHud", Boolean.FALSE);

    public static final PowerProperty<Float> AMOUNT = create("amount", 0.0F);
    public static final PowerProperty<Float> BOOST_SPEED = create("boostSpeed", 0.0F);
    public static final PowerProperty<Float> CHAIN_RADIUS = create("chainRadius", 0.0F);
    public static final PowerProperty<Float> DIVE_SPEED_RETENTION = create("diveSpeedRetention", 0.0F);
    public static final PowerProperty<Float> FACTOR = create("factor", 1.0F);
    public static final PowerProperty<Float> KNOCKBACK = create("knockback", 0.0F);
    public static final PowerProperty<Float> MAX_GRAVITY = create("maxGravity", 0.0F);
    public static final PowerProperty<Float> MAX_SIZE = create("maxSize", 1.0F);
    public static final PowerProperty<Float> MIN_GRAVITY = create("minGravity", 0.0F);
    public static final PowerProperty<Float> MIN_SIZE = create("minSize", 0.0F);
    public static final PowerProperty<Float> RADIUS = create("radius", 0.0F);
    public static final PowerProperty<Float> RANGE = create("range", 0.0F);
    public static final PowerProperty<Float> SPEED = create("speed", 0.0F);
    public static final PowerProperty<Float> SPREAD = create("spread", 0.0F);
    public static final PowerProperty<Float> HEALTH = create("health", 0.0F);

    public static final PowerProperty<Integer> CHARGE_TIME = create("chargeTime", 0);
    public static final PowerProperty<Integer> COOLDOWN_TIME = create("cooldownTime", 0);
    public static final PowerProperty<Integer> DELAY = create("delay", 0);
    public static final PowerProperty<Integer> DURATION = create("duration", 0);
    public static final PowerProperty<Integer> QUANTITY = create("quantity", 1);

    public static final PowerProperty<String> KEY = create("key", "");
    public static final PowerProperty<String> DAMAGE_TYPE = create("damageType", "");
    public static final PowerProperty<String> COVERAGE = create("coverage", "");
    public static final PowerProperty<String> COLLISION = create("collision", "");

    /** Complex values which are interpreted by the owning modifier. */
    public static final PowerProperty<JsonElement> EQUIPMENT = create("equipment", null);
    public static final PowerProperty<JsonElement> SHIELD = create("shield", null);
    public static final PowerProperty<SpellSet> SPELLS = create("spells", SpellSet.EMPTY);
    public static final PowerProperty<JsonElement> TRANSFORMATION = create("transformation", null);
    public static final PowerProperty<JsonElement> TELEKINESIS = create("telekinesis", null);
    public static final PowerProperty<JsonElement> TENTACLES = create("tentacles", null);
    public static final PowerProperty<JsonElement> TENTACLE_STRIKE = create("tentacleStrike", null);
    public static final PowerProperty<JsonElement> GLIDING = create("gliding", null);
    public static final PowerProperty<JsonElement> CLIMBING = create("climbing", null);
    public static final PowerProperty<JsonElement> BARREL_ROLL = create("barrelRoll", null);
    public static final PowerProperty<JsonElement> LEAP_AMOUNT = create("leapAmount", null);
    public static final PowerProperty<JsonElement> POTION_EFFECTS = create("potionEffects", null);
    public static final PowerProperty<JsonElement> COOLDOWN = create("cooldown", null);
    public static final PowerProperty<JsonElement> DAMAGE_BONUS = create("damageBonus", null);
    public static final PowerProperty<JsonElement> DAMAGE_PROFILE = create("damageProfile", null);
    public static final PowerProperty<JsonElement> SOUND_EVENTS = create("soundEvents", null);

    private final String name;
    private final T defaultValue;
    private final Class<T> typeClass;

    @SuppressWarnings("unchecked")
    private PowerProperty(String name, T defaultValue)
    {
        this.name = name;
        this.defaultValue = defaultValue;
        this.typeClass = defaultValue != null ? (Class<T>) defaultValue.getClass() : (Class<T>) JsonElement.class;
    }

    private static <T> PowerProperty<T> create(String name, T defaultValue)
    {
        PowerProperty<T> property = new PowerProperty<>(name, defaultValue);
        REGISTRY.put(name, property);
        return property;
    }

    public String getName()
    {
        return name;
    }

    public T getDefault()
    {
        return defaultValue;
    }

    public Class<T> getTypeClass()
    {
        return typeClass;
    }

    /** Parses a JSON value into this property's type, falling back to the default. */
    @SuppressWarnings("unchecked")
    public T parse(JsonElement json)
    {
        if (json == null || json.isJsonNull())
        {
            return defaultValue;
        }

        try
        {
            if (typeClass == Boolean.class)
            {
                return (T) Boolean.valueOf(json.getAsBoolean());
            }
            if (typeClass == Float.class)
            {
                return (T) Float.valueOf(json.getAsFloat());
            }
            if (typeClass == Integer.class)
            {
                return (T) Integer.valueOf(json.getAsInt());
            }
            if (typeClass == String.class)
            {
                return (T) json.getAsString();
            }
            if (typeClass == JsonElement.class)
            {
                return (T) json;
            }
            if (typeClass == SpellSet.class)
            {
                return (T) SpellSet.parse(json);
            }
        }
        catch (Exception e)
        {
            return defaultValue;
        }

        return defaultValue;
    }

    /** Parses a plain string literal of this property's type, or null when it is not one. */
    @SuppressWarnings("unchecked")
    public T tryParseLiteral(String text)
    {
        if (text == null)
        {
            return null;
        }

        try
        {
            if (typeClass == Boolean.class)
            {
                return "true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text) ? (T) Boolean.valueOf(text) : null;
            }
            if (typeClass == Float.class)
            {
                return (T) Float.valueOf(text);
            }
            if (typeClass == Integer.class)
            {
                return (T) Integer.valueOf(text);
            }
            if (typeClass == String.class)
            {
                return (T) text;
            }
        }
        catch (NumberFormatException e)
        {
            return null;
        }

        return null;
    }

    /** Converts the result of a script expression into this property's type. */
    @SuppressWarnings("unchecked")
    public T fromScript(Object value)
    {
        if (value == null)
        {
            return defaultValue;
        }

        try
        {
            if (typeClass == Boolean.class)
            {
                return (T) Boolean.valueOf(value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value)));
            }
            if (typeClass == Float.class)
            {
                return (T) Float.valueOf(value instanceof Number n ? n.floatValue() : Float.parseFloat(String.valueOf(value)));
            }
            if (typeClass == Integer.class)
            {
                return (T) Integer.valueOf(value instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(value)));
            }
            if (typeClass == String.class)
            {
                return (T) String.valueOf(value);
            }
        }
        catch (Exception e)
        {
            return defaultValue;
        }

        return defaultValue;
    }

    public JsonElement toJson(T value)
    {
        if (value instanceof Boolean b)
        {
            return new JsonPrimitive(b);
        }

        if (value instanceof Number n)
        {
            return new JsonPrimitive(n);
        }

        if (value instanceof String s)
        {
            return new JsonPrimitive(s);
        }

        if (value instanceof SpellSet spells)
        {
            return spells.toJson();
        }

        return value instanceof JsonElement e ? e : null;
    }

    public static PowerProperty<?> byName(String name)
    {
        return REGISTRY.get(name);
    }

    public static Map<String, PowerProperty<?>> getRegistry()
    {
        return REGISTRY;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
