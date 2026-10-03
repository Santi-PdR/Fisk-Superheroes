package com.fiskmods.heroes.client.texture;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.pack.ScriptFunction;
import com.fiskmods.heroes.pack.js.JSExpressions;
import com.fiskmods.heroes.pack.js.JSEntity;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;

/**
 * Turns a model resource entry into the concrete texture to draw for an entity.
 * <p>
 * A resource may be a plain sprite, or a {@code .tx.json} definition which either
 * <ul>
 * <li>selects among pre-rendered variants using the {@code getters} (the value of a data variable
 * or a script expression), or</li>
 * <li>generates a texture from sources with {@code STITCH}/{@code XOR}/{@code OR}/{@code HUE}.</li>
 * </ul>
 * Both forms are resolved here, with the results cached per texture id.
 */
public final class TextureResolver
{
    private static final Map<ResourceLocation, JsonObject> DEFINITIONS = new HashMap<>();

    private TextureResolver()
    {
    }

    public static void clear()
    {
        DEFINITIONS.clear();
    }

    /** Resolves the texture a resource name points to for the given entity. */
    @Nullable
    public static ResourceLocation resolve(String resource, Entity entity)
    {
        if (resource == null || resource.equals("null"))
        {
            return null;
        }

        ResourceLocation id = parse(resource);

        if (id == null)
        {
            return null;
        }

        if (!id.getPath().endsWith(".tx.json"))
        {
            return SHTextures.sprite(id.getNamespace(), id.getPath());
        }

        JsonObject json = definition(id);

        if (json == null)
        {
            return null;
        }

        Map<String, String> values = new HashMap<>();

        if (json.has("getters"))
        {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("getters").entrySet())
            {
                values.put(e.getKey(), String.valueOf(select(e.getValue(), entity)));
            }
        }

        JsonElement texture = json.get("texture");

        if (texture == null)
        {
            return null;
        }

        if (texture.isJsonPrimitive())
        {
            // A selector: the template names the variant that a getter picked
            String path = substitute(texture.getAsString(), values);
            return SHTextures.sprite(id.getNamespace(), path);
        }

        JsonObject stitch = texture.getAsJsonObject();
        String out = stitch.has("out") ? substitute(stitch.get("out").getAsString(), values) : null;

        if (out == null)
        {
            return null;
        }

        ResourceLocation output = SHTextures.sprite(id.getNamespace(), out);
        ResourceLocation cached = SHTextures.get(output.toString());

        if (cached != null)
        {
            return cached;
        }

        try
        {
            String in = stitch.has("in") && stitch.get("in").isJsonPrimitive()
                    ? substitute(stitch.get("in").getAsString(), values)
                    : null;

            com.mojang.blaze3d.platform.NativeImage base = in != null
                    ? SHTextures.read(SHTextures.sprite(id.getNamespace(), in))
                    : new com.mojang.blaze3d.platform.NativeImage(64, 32, true);

            JsonObject resolved = substitute(stitch, values);
            com.mojang.blaze3d.platform.NativeImage result = SHTextures.apply(base, resolved);

            if (result != base)
            {
                base.close();
            }

            SHTextures.register(output, result);
            SHTextures.put(output.toString(), output);
            return output;
        }
        catch (Exception e)
        {
            FiskHeroes.LOGGER.warn("Could not generate texture {}: {}", output, e.toString());
            return null;
        }
    }

    /* --- Internals --- */

    private static ResourceLocation parse(String value)
    {
        ResourceLocation id = ResourceLocation.tryParse(value);
        return id != null ? id : new ResourceLocation(FiskHeroes.MODID, value);
    }

    /** Evaluates one getter: a data variable, a script expression, or nothing. */
    private static Object select(JsonElement getter, Entity entity)
    {
        if (!getter.isJsonObject())
        {
            return 0;
        }

        JsonObject json = getter.getAsJsonObject();
        String key = json.has("key") ? json.get("key").getAsString() : "";
        int index = key.indexOf(':') == -1 ? -1 : key.substring(key.indexOf(':') + 1).indexOf(':');

        // Longest-prefix match: anything that is not a known data variable is a script expression
        Object value = evaluate(key, entity);

        if (json.has("values"))
        {
            JsonElement values = json.get("values");

            if (values.isJsonArray())
            {
                int i = value instanceof Number n ? n.intValue() : 0;
                var array = values.getAsJsonArray();
                return array.isEmpty() ? 0 : array.get(Math.max(0, Math.min(array.size() - 1, i))).getAsInt();
            }

            if (values.isJsonObject())
            {
                int min = values.getAsJsonObject().has("min") ? values.getAsJsonObject().get("min").getAsInt() : 0;
                int max = values.getAsJsonObject().has("max") ? values.getAsJsonObject().get("max").getAsInt() : 0;
                int i = value instanceof Number n ? n.intValue() : min;
                return Math.max(min, Math.min(max, i));
            }
        }

        return value != null ? value : index;
    }

    /** Reads a data variable, or evaluates the key as a script expression. */
    private static Object evaluate(String key, Entity entity)
    {
        if (key == null || key.isEmpty())
        {
            return 0;
        }

        Object data = JSEntity.read(entity, key);

        if (data != null)
        {
            return data instanceof Number || data instanceof Boolean ? data : 0;
        }

        ScriptFunction function = JSExpressions.compile(key);

        if (function != null)
        {
            Object result = function.call(new JSEntity(entity));
            return result instanceof Number || result instanceof Boolean ? result : 0;
        }

        return 0;
    }

    private static JsonObject definition(ResourceLocation id)
    {
        JsonObject cached = DEFINITIONS.get(id);

        if (cached != null)
        {
            return cached;
        }

        ResourceLocation file = new ResourceLocation(id.getNamespace(), "textures/heroes/" + id.getPath());

        try
        {
            Resource resource = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(file).orElse(null);

            if (resource == null)
            {
                return null;
            }

            JsonObject json = com.google.gson.JsonParser.parseReader(resource.openAsReader()).getAsJsonObject();
            DEFINITIONS.put(id, json);
            return json;
        }
        catch (Exception e)
        {
            FiskHeroes.LOGGER.warn("Could not read texture definition {}: {}", file, e.toString());
            DEFINITIONS.put(id, new JsonObject());
            return null;
        }
    }

    private static String substitute(String template, Map<String, String> values)
    {
        String result = template;

        for (Map.Entry<String, String> e : values.entrySet())
        {
            result = result.replace("<" + e.getKey() + ">", e.getValue());
        }

        return result;
    }

    private static JsonObject substitute(JsonObject json, Map<String, String> values)
    {
        JsonObject copy = new JsonObject();

        for (Map.Entry<String, JsonElement> e : json.entrySet())
        {
            JsonElement value = e.getValue();

            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString())
            {
                copy.addProperty(e.getKey(), substitute(value.getAsString(), values));
            }
            else if (value.isJsonArray())
            {
                var array = value.getAsJsonArray();
                var newArray = new com.google.gson.JsonArray();

                for (JsonElement entry : array)
                {
                    newArray.add(entry.isJsonObject() ? substitute(entry.getAsJsonObject(), values) : entry);
                }

                copy.add(e.getKey(), newArray);
            }
            else
            {
                copy.add(e.getKey(), value);
            }
        }

        return copy;
    }
}
