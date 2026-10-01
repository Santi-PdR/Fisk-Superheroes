package com.fiskmods.heroes.client.render;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Client-side registry of suit models. Models live in {@code assets/<domain>/models/heroes} and are
 * reloaded together with the other client resources.
 */
public class HeroModelRegistry extends SimplePreparableReloadListener<Map<ResourceLocation, JsonObject>>
{
    public static final HeroModelRegistry INSTANCE = new HeroModelRegistry();

    private static final Map<ResourceLocation, HeroModelData> MODELS = new HashMap<>();

    @Nullable
    public static HeroModelData get(ResourceLocation id)
    {
        return MODELS.get(id);
    }

    @Override
    protected Map<ResourceLocation, JsonObject> prepare(ResourceManager resourceManager, ProfilerFiller profiler)
    {
        Map<ResourceLocation, JsonObject> map = new HashMap<>();

        for (Map.Entry<ResourceLocation, Resource> entry : resourceManager.listResources("models/heroes", path -> path.getPath().endsWith(".json")).entrySet())
        {
            try
            {
                JsonObject json = JsonParser.parseReader(entry.getValue().openAsReader()).getAsJsonObject();
                ResourceLocation id = new ResourceLocation(entry.getKey().getNamespace(), entry.getKey().getPath().substring("models/heroes/".length(), entry.getKey().getPath().length() - ".json".length()));
                map.put(id, json);
            }
            catch (Exception e)
            {
                FiskHeroes.LOGGER.error("Failed to read hero model {}", entry.getKey(), e);
            }
        }

        return map;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonObject> object, ResourceManager resourceManager, ProfilerFiller profiler)
    {
        MODELS.clear();

        for (Map.Entry<ResourceLocation, JsonObject> entry : object.entrySet())
        {
            try
            {
                MODELS.put(entry.getKey(), HeroModelData.parse(entry.getKey(), entry.getValue()));
            }
            catch (Exception e)
            {
                FiskHeroes.LOGGER.error("Failed to parse hero model {}", entry.getKey(), e);
            }
        }

        FiskHeroes.LOGGER.info("Loaded {} hero suit models", MODELS.size());
    }
}
