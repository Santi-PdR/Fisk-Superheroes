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

/** Loads and resolves the original model trail data, including parent resources. */
public final class TrailRegistry extends SimplePreparableReloadListener<Map<ResourceLocation, JsonObject>>
{
    public static final TrailRegistry INSTANCE = new TrailRegistry();
    private static final Map<ResourceLocation, TrailDefinition> TRAILS = new HashMap<>();

    private TrailRegistry() {}

    @Nullable
    public static TrailDefinition get(ResourceLocation id)
    {
        return TRAILS.get(id);
    }

    @Override
    protected Map<ResourceLocation, JsonObject> prepare(ResourceManager manager, ProfilerFiller profiler)
    {
        Map<ResourceLocation, JsonObject> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources("models/trails",
                path -> path.getPath().endsWith(".json")).entrySet())
        {
            try (var reader = entry.getValue().openAsReader())
            {
                ResourceLocation id = new ResourceLocation(entry.getKey().getNamespace(),
                        entry.getKey().getPath().substring("models/trails/".length(), entry.getKey().getPath().length() - 5));
                result.put(id, JsonParser.parseReader(reader).getAsJsonObject());
            }
            catch (Exception e)
            {
                FiskHeroes.LOGGER.error("Failed to read trail resource {}", entry.getKey(), e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonObject> source, ResourceManager manager, ProfilerFiller profiler)
    {
        Map<ResourceLocation, JsonObject> resolved = new HashMap<>();
        for (ResourceLocation id : source.keySet()) resolve(id, source, resolved, 0);

        TRAILS.clear();
        resolved.forEach((id, json) ->
        {
            try
            {
                TRAILS.put(id, TrailDefinition.from(id, json));
            }
            catch (Exception e)
            {
                FiskHeroes.LOGGER.error("Failed to parse trail resource {}", id, e);
            }
        });
        FiskHeroes.LOGGER.info("Loaded {} trail resources", TRAILS.size());
    }

    @Nullable
    private static JsonObject resolve(ResourceLocation id, Map<ResourceLocation, JsonObject> source,
            Map<ResourceLocation, JsonObject> resolved, int depth)
    {
        if (depth > 16) return null;
        JsonObject cached = resolved.get(id);
        if (cached != null) return cached;
        JsonObject own = source.get(id);
        if (own == null) return null;

        JsonObject result = new JsonObject();
        if (own.has("parent"))
        {
            ResourceLocation parentId = ResourceLocation.tryParse(own.get("parent").getAsString());
            JsonObject parent = parentId != null ? resolve(parentId, source, resolved, depth + 1) : null;
            if (parent != null) merge(result, parent);
        }
        merge(result, own);
        result.remove("parent");
        resolved.put(id, result);
        return result;
    }

    /** Merge nested effect and constant objects while child scalar values override the parent. */
    private static void merge(JsonObject into, JsonObject from)
    {
        for (Map.Entry<String, com.google.gson.JsonElement> entry : from.entrySet())
        {
            String key = entry.getKey();
            if ("parent".equals(key)) continue;
            if (entry.getValue().isJsonObject())
            {
                JsonObject target = into.has(key) && into.get(key).isJsonObject()
                        ? into.getAsJsonObject(key) : new JsonObject();
                merge(target, entry.getValue().getAsJsonObject());
                into.add(key, target);
            }
            else
            {
                into.add(key, entry.getValue().deepCopy());
            }
        }
    }
}
