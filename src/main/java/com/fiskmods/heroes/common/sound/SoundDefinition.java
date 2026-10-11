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

    private boolean loop;
    private boolean scale = true;
    private boolean isStatic;
    private int fadeIn;
    private int fadeOut;
    private int delay;
    private double range = 16.0D;

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
            case "loop" -> definition.loop = value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() && value.getAsBoolean();
            case "scale" -> definition.scale = !value.isJsonPrimitive() || value.getAsBoolean();
            case "isStatic" -> definition.isStatic = value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() && value.getAsBoolean();
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

    private static int asInt(@Nullable JsonElement json)
    {
        return json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber() ? json.getAsInt() : 0;
    }

    /** Applies the values of a parent definition to everything this one does not override. */
    public void inherit(SoundDefinition parent)
    {
        if (sound == null)
        {
            sound = parent.sound;
        }

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

        if (!loop)
        {
            loop = parent.loop;
        }

        if (fadeIn == 0)
        {
            fadeIn = parent.fadeIn;
        }

        if (fadeOut == 0)
        {
            fadeOut = parent.fadeOut;
        }

        if (delay == 0)
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
        return fadeIn;
    }

    public int getFadeOut()
    {
        return fadeOut;
    }

    public int getDelay()
    {
        return delay;
    }

    public boolean isLoop()
    {
        return loop;
    }

    public boolean isScale()
    {
        return scale;
    }

    public boolean isStatic()
    {
        return isStatic;
    }

    public double getRange()
    {
        return range;
    }

    public void setRange(double range)
    {
        this.range = range;
    }

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
