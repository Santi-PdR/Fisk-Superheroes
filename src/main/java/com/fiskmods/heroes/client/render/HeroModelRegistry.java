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

    /** Returns iteration-specific model data, inheriting the base hero model as fallback. */
    @Nullable
    public static HeroModelData get(com.fiskmods.heroes.common.hero.HeroIteration iteration)
    {
        if (iteration == null)
        {
            return null;
        }

        HeroModelData model = MODELS.get(iteration.getRegistryName());
        return model != null ? model : MODELS.get(iteration.getHero().getRegistryName());
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

    private static void resolveParent(HeroModelData model, int depth)
    {
        if (depth > 8 || model.getParent() == null)
        {
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(model.getParent());

        if (id == null)
        {
            return;
        }

        HeroModelData parent = MODELS.get(id);

        if (parent == null)
        {
            return;
        }

        if (parent.getParent() != null)
        {
            resolveParent(parent, depth + 1);
        }

        model.inherit(parent);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonObject> object, ResourceManager resourceManager, ProfilerFiller profiler)
    {
        MODELS.clear();
        com.fiskmods.heroes.client.texture.TextureResolver.clear();
        com.fiskmods.heroes.client.texture.SHTextures.clear();

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

        // Models inherit from their parent; the chain is at most a couple of levels deep
        for (HeroModelData model : MODELS.values())
        {
            resolveParent(model, 0);
        }

        FiskHeroes.LOGGER.info("Loaded {} hero suit models", MODELS.size());
    }
}
