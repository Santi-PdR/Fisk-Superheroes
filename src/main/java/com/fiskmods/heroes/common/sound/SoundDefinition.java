package com.fiskmods.heroes.common.sound;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

/**
 * A sound event as defined by the hero pack's {@code events/sounds/*.json} files.
 * <p>
 * The original mod shipped no audio files: every definition names a sound in the external sound
 * repository, which the mod downloaded on first launch. The properties here are exactly the ones
 * those files carry ({@code parent}, {@code volume}, {@code pitch}, {@code loop}, {@code scale},
 * {@code isStatic}, {@code fadeIn}, {@code fadeOut}, {@code delay}, {@code condition} and
 * {@code override}), including the script-valued forms, since a good part of the shipped pack uses
 * expressions such as {@code "0.35 - Math.random() * 0.1"} for volume and pitch.
 */
public class SoundDefinition
{
    public static final ResourceLocation BASE = new ResourceLocation("fiskheroes", "sound_base");

    private final ResourceLocation id;
    private ResourceLocation sound;
    private ResourceLocation parent;

    private SoundValue volume = SoundValue.unset();
    private SoundValue pitch = SoundValue.unset();
    private SoundCondition condition = SoundCondition.ALWAYS;

    // Unset must remain distinct from explicit false/zero until inheritance completes.
    @Nullable private Boolean loop;
    @Nullable private Boolean scale;
    @Nullable private Boolean isStatic;
    @Nullable private Integer fadeIn;
    @Nullable private Integer fadeOut;
    @Nullable private Integer delay;
    private double range = 16.0D;
    private Target target = Target.ALL_IN_RANGE;

    public enum Target { ONLY_SELF, OTHERS_IN_RANGE, ALL_IN_RANGE, ALL_OTHERS, ALL }

    private ResourceLocation overridePath;
    private String overrideSource;

    public SoundDefinition(ResourceLocation id)
    {
        this.id = id;
    }

    public static SoundDefinition parse(ResourceLocation id, JsonObject json)
    {
        SoundDefinition definition = new SoundDefinition(id);

        for (var entry : json.entrySet())
        {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            switch (key)
            {
            case "parent" -> definition.parent = readId(value);
            case "sound" -> definition.sound = readId(value);
            case "override" -> definition.overridePath = readId(value);
            case "condition" -> definition.condition = SoundCondition.read(value);
            case "volume" -> definition.volume = SoundValue.parse(value);
            case "pitch" -> definition.pitch = SoundValue.parse(value);
            case "loop" -> definition.loop = asBoolean(value);
            case "scale" -> definition.scale = asBoolean(value);
            case "isStatic" -> definition.isStatic = asBoolean(value);
            case "fadeIn" -> definition.fadeIn = asInt(value);
            case "fadeOut" -> definition.fadeOut = asInt(value);
            case "delay" -> definition.delay = asInt(value);
            default -> { }
            }
        }

        return definition;
    }

    @Nullable
    private static ResourceLocation readId(@Nullable JsonElement json)
    {
        if (json == null || !json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString())
        {
            return null;
        }

        String text = json.getAsString();

        if (text.isEmpty() || text.equals("null") || text.equals("none"))
        {
            return null;
        }

        if (text.indexOf(':') == -1)
        {
            text = "fiskheroes:" + text;
        }

        return ResourceLocation.tryParse(text);
    }

    @Nullable
    private static Integer asInt(@Nullable JsonElement json)
    {
        return json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber() ? json.getAsInt() : null;
    }

    @Nullable
    private static Boolean asBoolean(@Nullable JsonElement json)
    {
        return json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean() ? json.getAsBoolean() : null;
    }

    /** Applies the values of a parent definition to everything this one does not override. */
    public void inherit(SoundDefinition parent)
    {
        // The original inherits playback properties, but not the sound path itself.
        if (!volume.isSet())
        {
            volume = parent.volume;
        }

        if (!pitch.isSet())
        {
            pitch = parent.pitch;
        }

        if (condition == SoundCondition.ALWAYS)
        {
            condition = parent.condition;
        }

        if (loop == null) loop = parent.loop;
        if (scale == null) scale = parent.scale;
        if (isStatic == null) isStatic = parent.isStatic;

        if (fadeIn == null)
        {
            fadeIn = parent.fadeIn;
        }

        if (fadeOut == null)
        {
            fadeOut = parent.fadeOut;
        }

        if (delay == null)
        {
            delay = parent.delay;
        }

        if (overridePath == null)
        {
            overridePath = parent.overridePath;
            overrideSource = parent.overrideSource;
        }
    }

    public ResourceLocation getId()
    {
        return id;
    }

    public ResourceLocation getSound()
    {
        return sound;
    }

    public ResourceLocation getParent()
    {
        return parent;
    }

    public SoundValue getVolume()
    {
        return volume;
    }

    public SoundValue getPitch()
    {
        return pitch;
    }

    public SoundCondition getCondition()
    {
        return condition;
    }

    public int getFadeIn()
    {
        return fadeIn != null ? fadeIn : 0;
    }

    public int getFadeOut()
    {
        return fadeOut != null ? fadeOut : 0;
    }

    public int getDelay()
    {
        return delay != null ? delay : 0;
    }

    public boolean isLoop()
    {
        return Boolean.TRUE.equals(loop);
    }

    public boolean isScale()
    {
        return scale == null || scale;
    }

    public boolean isStatic()
    {
        return Boolean.TRUE.equals(isStatic);
    }

    public double getRange()
    {
        return range;
    }

    public void setRange(double range)
    {
        this.range = range;
    }

    /** Reads the pack manifest's numeric range or object with distance and target. */
    public void setRangeSpec(JsonElement spec)
    {
        if (spec == null || spec.isJsonNull())
        {
            range = Double.POSITIVE_INFINITY;
            target = Target.ALL;
        }
        else if (spec.isJsonObject())
        {
            JsonObject object = spec.getAsJsonObject();
            if (object.has("distance")) range = object.get("distance").isJsonNull()
                    ? Double.POSITIVE_INFINITY : object.get("distance").getAsDouble();
            if (object.has("target"))
                try { target = Target.valueOf(object.get("target").getAsString()); }
                catch (IllegalArgumentException ignored) { target = Target.ALL_IN_RANGE; }
        }
        else if (spec.isJsonPrimitive()) range = spec.getAsDouble();
        if (range < 0) range = Double.POSITIVE_INFINITY;
    }

    /** The source is self only when the listener is the entity that emitted the sound. */
    public boolean acceptsListener(boolean self, double distanceSquared)
    {
        if (self) return target != Target.OTHERS_IN_RANGE && target != Target.ALL_OTHERS;
        if (target == Target.ONLY_SELF) return false;
        return target == Target.ALL || target == Target.ALL_OTHERS || distanceSquared <= range * range;
    }

    /** Audience for an attached loop delivered to a tracking client; distance is applied locally. */
    public boolean acceptsTrackedLoopListener(boolean self)
    {
        return self ? target != Target.OTHERS_IN_RANGE && target != Target.ALL_OTHERS : target != Target.ONLY_SELF;
    }

    public Target getTarget() { return target; }

    @Nullable
    public ResourceLocation getOverridePath()
    {
        return overridePath;
    }

    /** The source of the {@code continuePlaying(entity, sound)} function, when one is set. */
    @Nullable
    public String getOverrideSource()
    {
        return overrideSource;
    }

    public void setOverrideSource(@Nullable String source)
    {
        this.overrideSource = source;
    }
}
