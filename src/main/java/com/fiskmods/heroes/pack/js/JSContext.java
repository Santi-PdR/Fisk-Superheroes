package com.fiskmods.heroes.pack.js;

import java.lang.reflect.Method;
import java.util.Map;

import javax.annotation.Nullable;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.pack.ScriptFunction;

/**
 * The scripting engine the hero pack is evaluated with.
 * <p>
 * The original mod used Nashorn, which the JDK dropped in Java 15; Forge 1.20.1 ships
 * {@code org.openjdk.nashorn:nashorn-core} so the very same engine (and therefore the very same
 * script semantics) is available again, on the client, the dedicated server, in development and in
 * production. Scripts are evaluated once and the functions the pack hands to the mod are wrapped
 * into {@link ScriptFunction}s so common code never touches the engine directly.
 */
public final class JSContext
{
    private static boolean reported;

    private JSContext()
    {
    }

    /** Creates an engine with the standard objects available to pack scripts. */
    @Nullable
    public static ScriptEngine createEngine()
    {
        // The pack scripts are ES6 (arrow functions, template literals, let/const), which Nashorn
        // only accepts with the es6 option; the plain factory route defaults to ES5.
        ScriptEngine engine = createFallbackEngine();

        if (engine == null)
        {
            engine = new ScriptEngineManager().getEngineByName("nashorn");
        }

        if (engine == null)
        {
            if (!reported)
            {
                reported = true;
                FiskHeroes.LOGGER.error("No JavaScript engine is available: the hero pack cannot be loaded. "
                        + "Make sure org.openjdk.nashorn:nashorn-core is present (Forge ships it).");
            }

            return null;
        }

        try
        {
            Bindings bindings = engine.createBindings();
            bindings.put("console", new JSConsole());
            engine.setBindings(bindings, javax.script.ScriptContext.ENGINE_SCOPE);
        }
        catch (Exception e)
        {
            FiskHeroes.LOGGER.warn("Could not prepare the script bindings: {}", e.toString());
        }

        return engine;
    }

    /** Nashorn is usually discovered through the script engine service loader; this is the direct route. */
    @Nullable
    private static ScriptEngine createFallbackEngine()
    {
        try
        {
            Class<?> factory = Class.forName("org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory");
            Object instance = factory.getDeclaredConstructor().newInstance();
            Method method = factory.getMethod("getScriptEngine", String[].class);
            return (ScriptEngine) method.invoke(instance, (Object) new String[] { "--language=es6" });
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    /** Evaluates a script, making its top level declarations available on the engine. */
    public static Object evaluate(ScriptEngine engine, String source, String name) throws Exception
    {
        return engine.eval(source);
    }

    public static Object get(ScriptEngine engine, String name)
    {
        return engine.get(name);
    }

    /** Puts a Java object into the script's global scope. */
    public static void put(ScriptEngine engine, String name, Object value)
    {
        engine.put(name, value);
    }

    /**
     * Wraps a JavaScript function (as Java receives it from the engine) into a {@link ScriptFunction}.
     * <p>
     * Nashorn hands functions over as {@code ScriptObjectMirror} objects; the call is made
     * reflectively so this class has no hard link to a specific engine build.
     */
    @Nullable
    public static ScriptFunction wrap(Object value)
    {
        if (value == null)
        {
            return null;
        }

        Method call = findCallMethod(value.getClass());

        if (call == null)
        {
            return null;
        }

        return args ->
        {
            try
            {
                if (call.getParameterCount() == 2)
                {
                    return call.invoke(value, null, args);
                }

                return call.invoke(value, (Object) args);
            }
            catch (Exception e)
            {
                FiskHeroes.LOGGER.error("Error while running a hero pack function", e);
                return null;
            }
        };
    }

    public static boolean isFunction(Object value)
    {
        return value != null && findCallMethod(value.getClass()) != null;
    }

    @Nullable
    private static Method findCallMethod(Class<?> type)
    {
        Method best = null;

        for (Method method : type.getMethods())
        {
            if (!method.getName().equals("call"))
            {
                continue;
            }

            Class<?>[] parameters = method.getParameterTypes();

            if (parameters.length == 1 && parameters[0] == Object[].class)
            {
                return method;
            }

            if (parameters.length == 2 && parameters[1] == Object[].class)
            {
                best = method;
            }
        }

        return best;
    }

    public static boolean isArray(Object value)
    {
        return value instanceof java.util.List || value != null && value.getClass().isArray();
    }

    /** Reads a JavaScript object (a Nashorn mirror implements {@link Map}) as name/value pairs. */
    public static Map<String, Object> asMap(Object value)
    {
        if (value instanceof Map<?, ?> map)
        {
            Map<String, Object> result = new java.util.LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet())
            {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }

            return result;
        }

        return Map.of();
    }

    /** Converts engine objects back into plain Java values. */
    public static Object unwrap(Object value)
    {
        if (value instanceof Number || value instanceof Boolean || value instanceof String || value == null)
        {
            return value;
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
