package com.fiskmods.heroes.common.hero.power;

import javax.annotation.Nullable;

import com.fiskmods.heroes.pack.ScriptFunction;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.minecraft.world.entity.Entity;

/**
 * One property of a {@link ModifierEntry}.
 * <p>
 * Power files may give a property either as a literal or as a JavaScript expression which is
 * evaluated per entity, e.g. {@code "speed": "0.125 * entity.getInterpolatedData('fiskheroes:aimed_timer')"}.
 * The original mod did exactly this, so both forms are supported here: literals are parsed once and
 * expressions are compiled once and evaluated on demand.
 */
public class PropertyValue<T>
{
    private final PowerProperty<T> property;
    private final Object literal;
    @Nullable
    private final ScriptFunction function;

    private PropertyValue(PowerProperty<T> property, Object literal, @Nullable ScriptFunction function)
    {
        this.property = property;
        this.literal = literal;
        this.function = function;
    }

    /** Builds a default value that is already parsed and must not pass through JSON/script parsing. */
    public static <T> PropertyValue<T> ofLiteral(PowerProperty<T> property, T value)
    {
        return new PropertyValue<>(property, value, null);
    }

    public static <T> PropertyValue<T> of(PowerProperty<T> property, JsonElement json)
    {
        if (json == null || json.isJsonNull())
        {
            return new PropertyValue<>(property, property.getDefault(), null);
        }

        // A string that is not a literal of the property's type is a script expression.
        if (json.isJsonPrimitive())
        {
            JsonPrimitive primitive = json.getAsJsonPrimitive();

            if (primitive.isNumber() || primitive.isBoolean())
            {
                return new PropertyValue<>(property, property.parse(json), null);
            }

            String text = primitive.getAsString();
            Object literal = property.tryParseLiteral(text);

            if (literal != null)
            {
                return new PropertyValue<>(property, literal, null);
            }

            ScriptFunction function = com.fiskmods.heroes.pack.js.JSExpressions.compile(text);

            if (function != null)
            {
                return new PropertyValue<>(property, null, function);
            }

            return new PropertyValue<>(property, property.getDefault(), null);
        }

        return new PropertyValue<>(property, property.parse(json), null);
    }

    public PowerProperty<T> getProperty()
    {
        return property;
    }

    public boolean isDynamic()
    {
        return function != null;
    }

    /** The literal value, or the default when this property is an expression. */
    public T literal()
    {
        return literal != null ? (T) literal : property.getDefault();
    }

    /** Resolves the value for an entity, evaluating the expression when there is one. */
    public T resolve(Entity entity)
    {
        if (function == null)
        {
            return literal();
        }

        Object result = function.call(entity);
        return property.fromScript(result);
    }
}
