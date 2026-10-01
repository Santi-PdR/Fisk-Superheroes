package com.fiskmods.heroes.pack.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.pack.ScriptFunction;

/**
 * Thin wrapper around Rhino.
 * <p>
 * The original mod evaluated hero scripts with Nashorn, which was removed from the JDK in Java 15.
 * Rhino is the maintained ECMAScript engine used here instead; the pack scripts themselves are
 * unchanged.
 */
public final class JSContext
{
    private JSContext()
    {
    }

    /** Creates a fresh scope with the standard objects available to pack scripts. */
    public static Scriptable createScope(Context cx)
    {
        Scriptable scope = cx.initStandardObjects();
        ScriptableObject.putProperty(scope, "console", new JSConsole());
        return scope;
    }

    public static Context enter()
    {
        Context cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        cx.setOptimizationLevel(-1);
        return cx;
    }

    /** Wraps a Rhino function so common code can invoke it without depending on Rhino directly. */
    public static ScriptFunction wrap(Scriptable scope, Object value)
    {
        if (!(value instanceof Function function))
        {
            return null;
        }

        return args -> call(scope, function, args);
    }

    public static Object call(Scriptable scope, Function function, Object... args)
    {
        Context cx = enter();

        try
        {
            return unwrap(function.call(cx, scope, scope, args));
        }
        catch (Exception e)
        {
            FiskHeroes.LOGGER.error("Error while running a hero pack function", e);
            return null;
        }
        finally
        {
            Context.exit();
        }
    }

    /** Converts Rhino's wrapper types back into plain Java values. */
    public static Object unwrap(Object value)
    {
        if (value == null || value == Context.getUndefinedValue() || value == Scriptable.NOT_FOUND)
        {
            return null;
        }

        if (value instanceof org.mozilla.javascript.NativeJavaObject javaObject)
        {
            return javaObject.unwrap();
        }

        if (value instanceof org.mozilla.javascript.ConsString cons)
        {
            return cons.toString();
        }

        return value;
    }

    /** A minimal console object for pack scripts which log during loading. */
    public static class JSConsole
    {
        public void log(Object message)
        {
            FiskHeroes.LOGGER.info("[heropack] {}", message);
        }

        public void warn(Object message)
        {
            FiskHeroes.LOGGER.warn("[heropack] {}", message);
        }

        public void error(Object message)
        {
            FiskHeroes.LOGGER.error("[heropack] {}", message);
        }
    }
}
