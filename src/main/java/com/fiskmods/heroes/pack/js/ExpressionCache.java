package com.fiskmods.heroes.pack.js;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.fiskmods.heroes.pack.ScriptFunction;

/**
 * Per-owner expression cache. Cache the compiled function, not its result: the latter depends
 * on the entity and may change every frame. Owners must discard/clear it on resource reload.
 * This is deliberately not shared between the integrated server and client threads.
 */
public final class ExpressionCache
{
    private final Map<String, Optional<ScriptFunction>> functions = new HashMap<>();
    private static final boolean DIAGNOSTICS = Boolean.getBoolean("fiskheroes.ciPerformanceDiagnostics");
    private final Function<String, ScriptFunction> compiler;
    private final String diagnosticName;
    private int lookups;
    private int compilations;

    public ExpressionCache()
    {
        this(JSExpressions::compile, null);
    }

    public ExpressionCache(String diagnosticName)
    {
        this(JSExpressions::compile, diagnosticName);
    }

    public ExpressionCache(Function<String, ScriptFunction> compiler)
    {
        this(compiler, null);
    }

    private ExpressionCache(Function<String, ScriptFunction> compiler, String diagnosticName)
    {
        this.compiler = compiler;
        this.diagnosticName = diagnosticName;
    }

    @Nullable
    public ScriptFunction get(String expression)
    {
        ScriptFunction result = functions.computeIfAbsent(expression, key -> {
            if (DIAGNOSTICS) ++compilations;
            return Optional.ofNullable(compiler.apply(key));
        }).orElse(null);
        if (DIAGNOSTICS && diagnosticName != null && ++lookups == 100)
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Expression cache: owner={}, lookups={}, compilations={}",
                    diagnosticName, lookups, compilations);
        return result;
    }

    public void clear()
    {
        functions.clear();
        lookups = 0;
        compilations = 0;
    }
}
