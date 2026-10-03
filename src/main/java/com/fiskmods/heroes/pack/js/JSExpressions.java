package com.fiskmods.heroes.pack.js;

import javax.script.ScriptEngine;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.pack.ScriptFunction;

/**
 * Compiles the small JavaScript expressions which may appear as values inside power, model and
 * sound files (for example {@code "0.125 * entity.getInterpolatedData('fiskheroes:aimed_timer')"}).
 * <p>
 * Each expression is compiled once into a function of {@code entity}, mirroring the original mod's
 * script-valued properties.
 */
public final class JSExpressions
{
    private static final String SOURCE_PREFIX = "function __expr(entity) { return (";
    private static final String SOURCE_SUFFIX = "); }";

    private JSExpressions()
    {
    }

    public static ScriptFunction compile(String expression)
    {
        if (expression == null || expression.isEmpty())
        {
            return null;
        }

        ScriptEngine engine = JSContext.createEngine();

        if (engine == null)
        {
            return null;
        }

        try
        {
            JSContext.evaluate(engine, SOURCE_PREFIX + expression + SOURCE_SUFFIX, "expression");
            return JSContext.wrap(JSContext.get(engine, "__expr"));
        }
        catch (Exception e)
        {
            FiskHeroes.LOGGER.warn("Could not compile script expression '{}': {}", expression, e.toString());
            return null;
        }
    }
}
