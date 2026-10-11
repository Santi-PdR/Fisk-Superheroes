package com.fiskmods.heroes.pack;

/**
 * A callable declared inside a hero pack script. Hero scripts hand functions to the mod
 * (tick handlers, predicates, keybind callbacks) which are invoked from common code without
 * directly depending on the scripting engine.
 */
@FunctionalInterface
public interface ScriptFunction
{
    Object call(Object... args);

    default boolean callBoolean(Object... args)
    {
        return Boolean.TRUE.equals(call(args));
    }

    default float callFloat(Object... args)
    {
        Object value = call(args);

        if (value instanceof Number)
        {
            return ((Number) value).floatValue();
        }

        if (value instanceof Boolean)
        {
            return (Boolean) value ? 1 : 0;
        }

        return 0;
    }

    default int callInt(Object... args)
    {
        Object value = call(args);

        if (value instanceof Number)
        {
            return ((Number) value).intValue();
        }

        if (value instanceof Boolean)
        {
            return (Boolean) value ? 1 : 0;
        }

        return 0;
    }

    default String callString(Object... args)
    {
        Object value = call(args);
        return value == null ? null : String.valueOf(value);
    }
}
