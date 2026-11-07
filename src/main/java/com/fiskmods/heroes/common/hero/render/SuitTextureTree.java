package com.fiskmods.heroes.common.hero.render;

import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Ordered texture selector from the original hero model format. */
public final class SuitTextureTree
{
    private SuitTextureTree() {}

    /** Returns null when no branch matched; the literal "null" is a matched, empty texture. */
    @Nullable
    public static String resolve(@Nullable JsonElement node, Function<String, String> selectors)
    {
        if (node == null || node.isJsonNull()) return null;
        if (node.isJsonPrimitive()) return node.getAsString();
        if (!node.isJsonObject()) return null;

        for (Map.Entry<String, JsonElement> entry : node.getAsJsonObject().entrySet())
        {
            if (entry.getKey().equals("default"))
            {
                String result = resolve(entry.getValue(), selectors);
                if (result != null) return result;
            }
            else if (entry.getValue().isJsonObject())
            {
                String selected = selectors.apply(entry.getKey());
                JsonObject branches = entry.getValue().getAsJsonObject();
                JsonElement branch = selected != null ? branches.get(selected) : null;
                String result = resolve(branch, selectors);
                if (result == null) result = resolve(branches.get("default"), selectors);
                if (result != null) return result;
            }
        }
        return null;
    }
}
