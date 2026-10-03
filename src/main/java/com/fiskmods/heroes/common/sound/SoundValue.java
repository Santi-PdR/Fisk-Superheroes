package com.fiskmods.heroes.common.sound;

import javax.annotation.Nullable;

import com.fiskmods.heroes.common.data.DataRegistry;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.DataVar;
import com.fiskmods.heroes.pack.ScriptFunction;
import com.fiskmods.heroes.pack.js.JSExpressions;
import com.google.gson.JsonElement;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * A number or boolean inside a sound definition.
 * <p>
 * The original pack writes these values in four ways and every one of them is understood here, as
 * it was by the original mod:
 * <ul>
 * <li>a literal ({@code 0.4}, {@code true}),</li>
 * <li>the name of a data variable ({@code "fiskheroes:energy_projection"}), read off the entity,</li>
 * <li>a JavaScript expression ({@code "0.35 - Math.random() * 0.1"}), compiled once and evaluated
 * per entity,</li>
 * <li>{@code null}, meaning "not set", so the value is inherited from the definition's parent.</li>
 * </ul>
 */
public final class SoundValue
{
    public static final SoundValue UNSET = new SoundValue(null, null, null, false);

    /** Number or Boolean; {@code null} when the value is not a literal. */
    private final Object literal;
    @Nullable
    private final DataVar<?> variable;
    @Nullable
    private final ScriptFunction script;
    private final boolean set;

    private SoundValue(@Nullable Object literal, @Nullable DataVar<?> variable, @Nullable ScriptFunction script, boolean set)
    {
        this.literal = literal;
        this.variable = variable;
        this.script = script;
        this.set = set;
    }

    public static SoundValue parse(@Nullable JsonElement json)
    {
        if (json == null || json.isJsonNull() || !json.isJsonPrimitive())
        {
            // Sound definitions also use "null" for "inherit this one"
            return UNSET;
        }

        com.google.gson.JsonPrimitive primitive = json.getAsJsonPrimitive();

        if (primitive.isBoolean())
        {
            return new SoundValue(primitive.getAsBoolean(), null, null, true);
        }

        if (primitive.isNumber())
        {
            return new SoundValue(primitive.getAsFloat(), null, null, true);
        }

        String text = primitive.getAsString();

        if (text.isEmpty() || text.equals("null") || text.equals("none"))
        {
            return UNSET;
        }

        DataVar<?> variable = resolveVariable(text);

        if (variable != null)
        {
            return new SoundValue(null, variable, null, true);
        }

        ScriptFunction function = JSExpressions.compile(text);

        if (function != null)
        {
            return new SoundValue(null, null, function, true);
        }

        return UNSET;
    }

    @Nullable
    private static DataVar<?> resolveVariable(String text)
    {
        if (!text.contains(":"))
        {
            return null;
        }

        ResourceLocation id = ResourceLocation.tryParse(text);
        return id != null ? DataRegistry.INSTANCE.get(id) : null;
    }

    public boolean isSet()
    {
        return set;
    }

    @Nullable
    private Object evaluate(@Nullable Entity entity)
    {
        if (variable != null)
        {
            SHPlayerData data = entity != null ? SHDataCapabilities.getPlayer(entity) : null;
            return data != null ? data.getData().get((DataVar) variable) : variable.getDefault();
        }

        if (script != null)
        {
            return entity != null ? script.call(entity) : null;
        }

        return literal;
    }

    public boolean getBoolean(@Nullable Entity entity, boolean fallback)
    {
        Object value = evaluate(entity);

        if (value instanceof Boolean b)
        {
            return b;
        }

        return value instanceof Number n ? n.floatValue() != 0.0F : fallback;
    }

    public float getFloat(@Nullable Entity entity, float fallback)
    {
        Object value = evaluate(entity);

        if (value instanceof Number n)
        {
            return n.floatValue();
        }

        return value instanceof Boolean b ? (b ? 1.0F : 0.0F) : fallback;
    }
}
