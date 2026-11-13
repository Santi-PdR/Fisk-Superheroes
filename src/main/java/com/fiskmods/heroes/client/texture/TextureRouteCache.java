package com.fiskmods.heroes.client.texture;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import net.minecraft.resources.ResourceLocation;

/** Caches only resource routing, never the entity-dependent result of a texture definition. */
public final class TextureRouteCache
{
    private static final int MAX_ROUTES_PER_LAYER = 2048;
    private static final boolean DIAGNOSTICS = Boolean.getBoolean("fiskheroes.ciPerformanceDiagnostics");
    private final List<Map<String, Optional<Route>>> routes = List.of(new HashMap<>(), new HashMap<>(), new HashMap<>());
    private final Predicate<ResourceLocation> exists;
    private final Function<ResourceLocation, ResourceLocation> sprite;
    private int lookups;
    private int resolutions;

    public record Route(ResourceLocation id, boolean definition) {}

    public TextureRouteCache(Predicate<ResourceLocation> exists, Function<ResourceLocation, ResourceLocation> sprite)
    {
        this.exists = exists;
        this.sprite = sprite;
    }

    /** The three groups are no layer, layer 1, and layer 2; all non-legging slots share layer 1. */
    @Nullable
    public Route resolve(String resource, int armorSlot)
    {
        if (resource == null || resource.equals("null")) return null;
        Map<String, Optional<Route>> cache = routes.get(armorSlot < 0 ? 0 : armorSlot == 2 ? 2 : 1);
        Optional<Route> cached = cache.get(resource);
        if (DIAGNOSTICS)
        {
            ++lookups;
            if (cached == null) ++resolutions;
            if (lookups == 100)
                FiskHeroes.LOGGER.info("Texture route cache: lookups={}, resolutions={}", lookups, resolutions);
        }
        if (cached != null) return cached.orElse(null);
        Route result = find(resource, armorSlot);
        if (cache.size() < MAX_ROUTES_PER_LAYER) cache.put(resource, Optional.ofNullable(result));
        return result;
    }

    public void clear()
    {
        routes.forEach(Map::clear);
        lookups = 0;
        resolutions = 0;
    }

    @Nullable
    private Route find(String resource, int armorSlot)
    {
        ResourceLocation id = ResourceLocation.tryParse(resource.indexOf(':') < 0 ? FiskHeroes.MODID + ":" + resource : resource);
        if (id == null) return null;
        if (!id.getPath().endsWith(".tx.json"))
        {
            ResourceLocation definition = withPath(id, id.getPath() + ".tx.json");
            if (exists.test(texturePath(definition))) id = definition;
            else if (!exists.test(texturePath(withPath(id, id.getPath() + ".png"))) && armorSlot >= 0)
            {
                String suffix = armorSlot == 2 ? "_layer2" : "_layer1";
                ResourceLocation variant = withPath(id, id.getPath() + suffix);
                if (exists.test(texturePath(withPath(variant, variant.getPath() + ".tx.json"))))
                    id = withPath(variant, variant.getPath() + ".tx.json");
                else if (exists.test(texturePath(withPath(variant, variant.getPath() + ".png")))) id = variant;
            }
        }
        boolean definition = id.getPath().endsWith(".tx.json");
        return new Route(definition ? id : sprite.apply(id), definition);
    }

    private static ResourceLocation withPath(ResourceLocation id, String path)
    {
        return new ResourceLocation(id.getNamespace(), path);
    }

    private static ResourceLocation texturePath(ResourceLocation id)
    {
        return new ResourceLocation(id.getNamespace(), "textures/heroes/" + id.getPath());
    }
}
