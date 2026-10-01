package com.fiskmods.heroes.common.sound;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

/**
 * A sound event as defined by the hero pack's {@code events/sounds/*.json} files.
 * <p>
 * The original mod shipped no audio files: every definition names a sound in the external sound
 * repository, which the mod downloaded on first launch. The properties here are exactly the ones
 * those files carry ({@code parent}, {@code volume}, {@code pitch}, {@code loop}, {@code fadeIn},
 * {@code fadeOut}, {@code delay}, {@code condition}, {@code start}, {@code continue},
 * {@code override} and {@code scale}).
 */
public class SoundDefinition
{
    public static final ResourceLocation BASE = new ResourceLocation("fiskheroes", "sound_base");

    private final ResourceLocation id;
    private ResourceLocation sound;
    private ResourceLocation parent;
    private Map<String, ResourceLocation> start = new LinkedHashMap<>();
    private Map<String, ResourceLocation> resume = new LinkedHashMap<>();
    private Map<String, ResourceLocation> override = new LinkedHashMap<>();
    private float volume = 1.0F;
    private float pitch = 1.0F;
    private int fadeIn;
    private int fadeOut;
    private int delay;
    private boolean loop;
    private ResourceLocation condition;
    private double range = 16.0D;

    public SoundDefinition(ResourceLocation id)
    {
        this.id = id;
    }

    public static SoundDefinition parse(ResourceLocation id, JsonObject json)
    {
        SoundDefinition definition = new SoundDefinition(id);

        if (json.has("sound"))
        {
            definition.sound = ResourceLocation.tryParse(json.get("sound").getAsString());
        }

        if (json.has("parent"))
        {
            definition.parent = ResourceLocation.tryParse(json.get("parent").getAsString());
        }

        for (Map.Entry<String, JsonElement> e : json.entrySet())
        {
            switch (e.getKey())
            {
            case "volume":
                definition.volume = e.getValue().getAsFloat();
                break;
            case "pitch":
                definition.pitch = e.getValue().getAsFloat();
                break;
            case "fadeIn":
                definition.fadeIn = e.getValue().getAsInt();
                break;
            case "fadeOut":
                definition.fadeOut = e.getValue().getAsInt();
                break;
            case "delay":
                definition.delay = e.getValue().getAsInt();
                break;
            case "loop":
                definition.loop = e.getValue().getAsBoolean();
                break;
            case "condition":
                definition.condition = ResourceLocation.tryParse(e.getValue().getAsString());
                break;
            case "start":
                readMap(e.getValue(), definition.start);
                break;
            case "continue":
                readMap(e.getValue(), definition.resume);
                break;
            case "override":
                readMap(e.getValue(), definition.override);
                break;
            default:
                break;
            }
        }

        return definition;
    }

    private static void readMap(JsonElement json, Map<String, ResourceLocation> map)
    {
        if (json.isJsonObject())
        {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject().entrySet())
            {
                ResourceLocation id = ResourceLocation.tryParse(e.getValue().getAsString());

                if (id != null)
                {
                    map.put(e.getKey(), id);
                }
            }
        }
    }

    /** Applies the values of a parent definition to everything this one does not override. */
    public void inherit(SoundDefinition parent)
    {
        if (sound == null)
        {
            sound = parent.sound;
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

        if (!loop)
        {
            loop = parent.loop;
        }

        if (condition == null)
        {
            condition = parent.condition;
        }

        if (start.isEmpty())
        {
            start = parent.start;
        }

        if (resume.isEmpty())
        {
            resume = parent.resume;
        }

        if (override.isEmpty())
        {
            override = parent.override;
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

    public Map<String, ResourceLocation> getStart()
    {
        return start;
    }

    public Map<String, ResourceLocation> getResume()
    {
        return resume;
    }

    public Map<String, ResourceLocation> getOverride()
    {
        return override;
    }

    /** The {@code override} entry for a variant, or this definition's own sound. */
    public ResourceLocation resolveSound(String variant)
    {
        if (variant != null)
        {
            ResourceLocation start = start.get(variant);

            if (start != null)
            {
                return start;
            }

            ResourceLocation overridden = override.get(variant);

            if (overridden != null)
            {
                return overridden;
            }
        }

        return sound;
    }

    public float getVolume()
    {
        return volume;
    }

    public float getPitch()
    {
        return pitch;
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

    public ResourceLocation getCondition()
    {
        return condition;
    }

    public double getRange()
    {
        return range;
    }

    public void setRange(double range)
    {
        this.range = range;
    }
}
