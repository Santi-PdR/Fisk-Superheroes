package com.fiskmods.heroes.common.hero.render;

import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Runtime value of a .tx.json getter. Its values list describes preloaded permutations,
 * not a lookup table or a range to clamp the interpreter result to. */
public final class TextureGetterValue
{
    private TextureGetterValue() { }

    public static Object evaluate(JsonElement getter, Function<String, Object> interpreter)
    {
        if (getter == null || !getter.isJsonObject()) return 0;
        JsonObject json = getter.getAsJsonObject();
        if (!json.has("key") || !json.get("key").isJsonPrimitive()) return 0;
        Object value = interpreter.apply(json.get("key").getAsString());
        return value != null ? value : 0;
    }
}
