package com.fiskmods.heroes.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

/**
 * A hero's suit model description, loaded from
 * {@code assets/fiskheroes/models/heroes/<hero>.json}.
 * <p>
 * The format is the one shipped with the original mod: a {@code resources} map of texture
 * references, a {@code texture} map assigning a texture to each armour slot and {@code showModel}
 * lists deciding which player model parts each piece draws.
 */
public class HeroModelData
{
    private final ResourceLocation hero;
    private final Map<String, ResourceLocation> textures = new HashMap<>();
    private final Map<Integer, String> slotTextures = new HashMap<>();
    private final Map<Integer, List<String>> showModel = new HashMap<>();
    private final List<String> fixHatLayer = new ArrayList<>();
    private final Map<String, JsonObject> custom = new HashMap<>();

    public HeroModelData(ResourceLocation hero)
    {
        this.hero = hero;
    }

    public ResourceLocation getHero()
    {
        return hero;
    }

    public static HeroModelData parse(ResourceLocation hero, JsonObject json)
    {
        HeroModelData data = new HeroModelData(hero);

        if (json.has("resources"))
        {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("resources").entrySet())
            {
                data.textures.put(e.getKey(), new ResourceLocation(hero.getNamespace(), "textures/heroes/" + e.getValue().getAsString() + ".png"));
            }
        }

        if (json.has("texture"))
        {
            JsonObject texture = json.getAsJsonObject("texture");
            String fallback = texture.has("default") ? texture.get("default").getAsString() : null;

            if (texture.has("renderLayer"))
            {
                for (Map.Entry<String, JsonElement> e : texture.getAsJsonObject("renderLayer").entrySet())
                {
                    int slot = slot(e.getKey());

                    if (slot != -1)
                    {
                        data.slotTextures.put(slot, e.getValue().getAsString());
                    }
                }
            }

            for (int slot = 0; slot < 4; ++slot)
            {
                data.slotTextures.putIfAbsent(slot, fallback);
            }
        }

        if (json.has("showModel"))
        {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("showModel").entrySet())
            {
                List<String> slots = new ArrayList<>();

                for (JsonElement element : e.getValue().getAsJsonArray())
                {
                    slots.add(element.getAsString());
                }

                data.showModel.put(e.getKey().hashCode(), slots);
            }
        }

        if (json.has("fixHatLayer"))
        {
            for (JsonElement element : json.getAsJsonArray("fixHatLayer"))
            {
                data.fixHatLayer.add(element.getAsString());
            }
        }

        if (json.has("custom"))
        {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("custom").entrySet())
            {
                data.custom.put(e.getKey(), e.getValue().getAsJsonObject());
            }
        }

        return data;
    }

    private static int slot(String name)
    {
        return switch (name.toUpperCase(java.util.Locale.ROOT))
        {
            case "HELMET", "HEAD" -> 0;
            case "CHESTPLATE", "CHEST" -> 1;
            case "LEGGINGS", "LEGS" -> 2;
            case "BOOTS", "FEET" -> 3;
            default -> -1;
        };
    }

    /** The texture used by the given armour slot, or null when the model has no texture for it. */
    @Nullable
    public ResourceLocation getTexture(int slot)
    {
        String key = slotTextures.get(slot);
        return key != null ? textures.get(key) : null;
    }

    /** Whether the given player model part should be drawn for the given armour slot. */
    public boolean showsModelPart(int slot, String part)
    {
        for (Map.Entry<Integer, List<String>> e : showModel.entrySet())
        {
            if (!e.getKey().equals(part.hashCode()))
            {
                continue;
            }

            String slotName = switch (slot)
            {
                case 0 -> "HELMET";
                case 1 -> "CHESTPLATE";
                case 2 -> "LEGGINGS";
                default -> "BOOTS";
            };

            return e.getValue().contains(slotName);
        }

        return true;
    }

    public List<String> getFixHatLayer()
    {
        return fixHatLayer;
    }

    public Map<String, JsonObject> getCustom()
    {
        return custom;
    }

    public static HeroModelData load(@Nullable String name)
    {
        if (name == null)
        {
            return null;
        }

        ResourceLocation id = name.indexOf(':') == -1 ? new ResourceLocation(FiskHeroes.MODID, name) : ResourceLocation.tryParse(name);
        return id != null ? HeroModelRegistry.get(id) : null;
    }
}
