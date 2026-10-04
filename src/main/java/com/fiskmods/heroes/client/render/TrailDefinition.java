package com.fiskmods.heroes.client.render;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/** Resolved data from {@code assets/<namespace>/models/trails/<name>.json}. */
public record TrailDefinition(ResourceLocation id, int fade, JsonObject lightning, JsonObject flicker,
        JsonObject particles, JsonObject blur, JsonObject constants)
{
    static TrailDefinition from(ResourceLocation id, JsonObject json)
    {
        int fade = json.has("fade") ? Math.max(1, json.get("fade").getAsInt()) : 10;
        return new TrailDefinition(id, fade, object(json, "lightning"), object(json, "flicker"),
                object(json, "particles"), object(json, "blur"), object(json, "constants"));
    }

    private static JsonObject object(JsonObject json, String name)
    {
        return json.has(name) && json.get(name).isJsonObject() ? json.getAsJsonObject(name).deepCopy() : null;
    }

    /** Resolves an inherited symbolic value such as {@code @COLOR}. */
    public String resolveConstant(String value)
    {
        if (value == null || !value.startsWith("@")) return value;
        String key = value.substring(1);
        return constants.has(key) ? constants.get(key).getAsString() : value;
    }

    public TrailDefinition withId(ResourceLocation value)
    {
        return new TrailDefinition(value, fade, lightning, flicker, particles, blur, constants);
    }
}
