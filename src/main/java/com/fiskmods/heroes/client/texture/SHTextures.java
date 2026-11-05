package com.fiskmods.heroes.client.texture;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fiskmods.heroes.FiskHeroes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * Runtime texture generation for the {@code .tx.json} stitch definitions shipped with the pack.
 * <p>
 * The original mod combined source sprites into the textures a suit needs (fusing a helmet shell
 * with a visor, adding glowing layers, recolouring). The sources are all shipped as ordinary PNGs,
 * so the same textures are reproduced here from the same inputs with the same operations
 * ({@code STITCH}, {@code XOR}, {@code OR}, {@code HUE}).
 */
public final class SHTextures
{
    private static final Map<ResourceLocation, Boolean> GENERATED = new HashMap<>();
    private static final Map<String, ResourceLocation> CACHE = new LinkedHashMap<>();

    private SHTextures()
    {
    }

    public static void clear()
    {
        GENERATED.clear();
        CACHE.clear();
    }

    public static ResourceLocation get(String key)
    {
        return CACHE.get(key);
    }

    public static void put(String key, ResourceLocation id)
    {
        CACHE.put(key, id);
    }

    /** Registers a generated image under {@code id} and returns it. */
    public static ResourceLocation register(ResourceLocation id, com.mojang.blaze3d.platform.NativeImage image)
    {
        DynamicTexture texture = new DynamicTexture(image);
        texture.setFilter(false, false);
        Minecraft.getInstance().getTextureManager().register(id, texture);
        GENERATED.put(id, Boolean.TRUE);
        return id;
    }

    public static boolean isGenerated(ResourceLocation id)
    {
        return GENERATED.containsKey(id);
    }

    /* --- Sources --- */

    public static ResourceLocation sprite(String namespace, String path)
    {
        // Texture selectors in original .tx.json files may contain a fully-qualified
        // resource name (for example "fiskheroes:generated/mysterio_mask_1"). Treat its
        // namespace and path separately before placing it under textures/heroes; appending
        // the whole selector to that directory creates an invalid ResourceLocation and can
        // crash entity rendering.
        ResourceLocation qualified = ResourceLocation.tryParse(path);
        if (qualified != null && path.indexOf(':') >= 0)
        {
            namespace = qualified.getNamespace();
            path = qualified.getPath();
        }

        if (path.endsWith(".png"))
        {
            path = path.substring(0, path.length() - 4);
        }
        return new ResourceLocation(namespace, "textures/heroes/" + path + ".png");
    }

    public static com.mojang.blaze3d.platform.NativeImage read(ResourceLocation texture) throws java.io.IOException
    {
        String path = texture.getPath();

        if (!path.endsWith(".png"))
        {
            path = path + ".png";
        }

        ResourceLocation file = new ResourceLocation(texture.getNamespace(), path);
        Resource resource = Minecraft.getInstance().getResourceManager().getResource(file).orElseThrow(
                () -> new java.io.FileNotFoundException(file.toString()));

        try (InputStream in = resource.open())
        {
            return com.mojang.blaze3d.platform.NativeImage.read(in);
        }
    }

    /**
     * Applies the transformations of a stitch definition.
     *
     * @param input  the base image (its ownership passes to this method)
     * @param stitch the parsed stitch description
     * @return the combined image
     */
    public static com.mojang.blaze3d.platform.NativeImage apply(com.mojang.blaze3d.platform.NativeImage input, JsonObject stitch)
    {
        com.mojang.blaze3d.platform.NativeImage result = input;

        for (JsonElement element : asArray(stitch.get("transform")))
        {
            if (!element.isJsonObject())
            {
                continue;
            }

            JsonObject transform = element.getAsJsonObject();
            String operation = transform.has("operation") ? transform.get("operation").getAsString().toUpperCase(java.util.Locale.ROOT) : "";
            String supply = transform.has("supply") ? transform.get("supply").getAsString() : null;

            try
            {
                com.mojang.blaze3d.platform.NativeImage previous = result;
                switch (operation)
                {
                case "STITCH":
                    result = stitch(result, supply);
                    break;
                case "XOR":
                    result = combine(result, supply, SHTextures::xor);
                    break;
                case "OR":
                    result = combine(result, supply, SHTextures::or);
                    break;
                case "HUE":
                    result = hue(result, supply != null ? Float.parseFloat(supply) : 0.0F);
                    break;
                default:
                    FiskHeroes.LOGGER.warn("Unknown texture operation {}", operation);
                    break;
                }
                if (result != previous) previous.close();
            }
            catch (Exception e)
            {
                FiskHeroes.LOGGER.warn("Could not apply texture operation {}: {}", operation, e.toString());
            }
        }

        return result;
    }

    private static JsonArray asArray(JsonElement element)
    {
        if (element == null)
        {
            return new JsonArray();
        }

        if (element.isJsonArray())
        {
            return element.getAsJsonArray();
        }

        JsonArray array = new JsonArray();
        array.add(element);
        return array;
    }

    /* --- Operations --- */

    private static com.mojang.blaze3d.platform.NativeImage combine(com.mojang.blaze3d.platform.NativeImage base, String supply,
            java.util.function.IntBinaryOperator operation) throws java.io.IOException
    {
        ResourceLocation sprite = parse(supply);

        if (sprite == null)
        {
            return base;
        }

        // Supply paths in .tx.json are relative to textures/heroes, just like the base.
        try (com.mojang.blaze3d.platform.NativeImage overlay = read(sprite(sprite.getNamespace(), sprite.getPath())))
        {
            int width = Math.max(base.getWidth(), overlay.getWidth());
            int height = Math.max(base.getHeight(), overlay.getHeight());
            boolean resize = width != base.getWidth() || height != base.getHeight();
            com.mojang.blaze3d.platform.NativeImage result = resize
                    ? new com.mojang.blaze3d.platform.NativeImage(width, height, true) : base;
            for (int x = 0; x < width; ++x)
            {
                for (int y = 0; y < height; ++y)
                {
                    // The original resized both images to the largest dimensions using nearest-neighbor.
                    int under = base.getPixelRGBA(x * base.getWidth() / width, y * base.getHeight() / height);
                    int over = overlay.getPixelRGBA(x * overlay.getWidth() / width, y * overlay.getHeight() / height);
                    result.setPixelRGBA(x, y, operation.applyAsInt(under, over));
                }
            }
            return result;
        }
    }

    private static int xor(int under, int over)
    {
        return com.fiskmods.heroes.common.util.TextureColorMath.xor(under, over);
    }

    private static int or(int under, int over)
    {
        return com.fiskmods.heroes.common.util.TextureColorMath.or(under, over);
    }

    private static com.mojang.blaze3d.platform.NativeImage hue(com.mojang.blaze3d.platform.NativeImage base, float degrees)
    {
        for (int x = 0; x < base.getWidth(); ++x)
        {
            for (int y = 0; y < base.getHeight(); ++y)
            {
                int color = base.getPixelRGBA(x, y);
                base.setPixelRGBA(x, y, com.fiskmods.heroes.common.util.TextureColorMath.hue(color, degrees));
            }
        }
        return base;
    }

    /** Copies the regions described by a stitcher definition onto a new 64x32 texture. */
    private static com.mojang.blaze3d.platform.NativeImage stitch(com.mojang.blaze3d.platform.NativeImage source, String stitcher) throws java.io.IOException
    {
        ResourceLocation id = parse(stitcher);

        if (id == null)
        {
            return source;
        }

        ResourceLocation file = new ResourceLocation(id.getNamespace(), "textures/stitchers/" + id.getPath() + ".json");
        Resource resource = Minecraft.getInstance().getResourceManager().getResource(file).orElse(null);

        if (resource == null)
        {
            return source;
        }

        JsonObject json = com.google.gson.JsonParser.parseReader(resource.openAsReader()).getAsJsonObject();
        int width = json.has("dimensions") ? json.getAsJsonArray("dimensions").get(0).getAsInt() : source.getWidth();
        int height = json.has("dimensions") ? json.getAsJsonArray("dimensions").get(1).getAsInt() : source.getHeight();
        com.mojang.blaze3d.platform.NativeImage result = new com.mojang.blaze3d.platform.NativeImage(width, height, true);

        for (JsonElement element : asArray(json.get("stitch")))
        {
            JsonObject entry = element.getAsJsonObject();
            int w = entry.getAsJsonArray("size").get(0).getAsInt();
            int h = entry.getAsJsonArray("size").get(1).getAsInt();
            int fromX = entry.getAsJsonArray("from").get(0).getAsInt();
            int fromY = entry.getAsJsonArray("from").get(1).getAsInt();
            int toX = entry.getAsJsonArray("to").get(0).getAsInt();
            int toY = entry.getAsJsonArray("to").get(1).getAsInt();
            boolean mirror = entry.has("transforms") && entry.get("transforms").toString().contains("MIRROR_X");

            for (int x = 0; x < w; ++x)
            {
                for (int y = 0; y < h; ++y)
                {
                    int sourceX = fromX + (mirror ? w - 1 - x : x);
                    int sourceY = fromY + y;

                    if (sourceX < source.getWidth() && sourceY < source.getHeight())
                    {
                        result.setPixelRGBA(toX + x, toY + y, source.getPixelRGBA(sourceX, sourceY));
                    }
                }
            }
        }

        return result;
    }

    private static ResourceLocation parse(String value)
    {
        if (value == null || value.isEmpty())
        {
            return null;
        }

        return ResourceLocation.tryParse(value.indexOf(':') < 0 ? FiskHeroes.MODID + ":" + value : value);
    }
}
