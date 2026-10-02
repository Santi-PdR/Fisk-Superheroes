package com.fiskmods.heroes.pack.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.pack.ScriptFunction;

/**
 * Compiles the small JavaScript expressions which may appear as values inside power and model
 * files (for example {@code "0.125 * entity.getInterpolatedData('fiskheroes:aimed_timer')"}).
 * <p>
 * Each expression is compiled once into a function of {@code entity}, mirroring the original mod's
 * script-valued properties.
 */
public final class JSExpressions
{
    private static final String SOURCE_PREFIX = "function __expr(entity) { return (";

    private JSExpressions()
    {
    }

    public static ScriptFunction compile(String expression)
    {
        if (expression == null || expression.isEmpty())
        {
            return null;
        }

        Context cx = JSContext.enter();

        try
        {
            Scriptable scope = JSContext.createScope(cx);
            Script script = cx.compileString(SOURCE_PREFIX + expression + "); }", "expression", 1, null);
            script.exec(cx, scope);
            Object value = scope.get("__expr", scope);

            if (value instanceof org.mozilla.javascript.Function function)
            {
                return args -> JSContext.call(scope, function, args);
            }
        }
        catch (Exception e)
        {
            FiskHeroes.LOGGER.warn("Could not compile script expression '{}': {}", expression, e.toString());
        }
        finally
        {
            Context.exit();
        }

        return null;
    }
}
