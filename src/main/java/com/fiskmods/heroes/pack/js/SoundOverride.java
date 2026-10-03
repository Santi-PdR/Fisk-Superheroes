package com.fiskmods.heroes.pack.js;

import javax.annotation.Nullable;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import com.fiskmods.heroes.FiskHeroes;

import net.minecraft.world.entity.Entity;

/**
 * The {@code continuePlaying(entity, sound)} function a sound definition may point at with its
 * {@code override} property.
 * <p>
 * The bundled pack uses these functions to make looping suit sounds follow their owner: they read
 * the entity's velocity and interpolated data, push a volume and pitch onto the sound object and
 * return whether the sound should keep playing.
 */
public final class SoundOverride
{
    private final Scriptable scope;
    private final Function function;

    private SoundOverride(Scriptable scope, Function function)
    {
        this.scope = scope;
        this.function = function;
    }

    @Nullable
    public static SoundOverride compile(@Nullable String source)
    {
        if (source == null || source.isEmpty())
        {
            return null;
        }

        Context cx = JSContext.enter();

        try
        {
            Scriptable scope = JSContext.createScope(cx);
            cx.evaluateString(scope, source, "sound override", 1, null);
            Object value = scope.get("continuePlaying", scope);

            if (value instanceof Function function)
            {
                return new SoundOverride(scope, function);
            }

            FiskHeroes.LOGGER.warn("Sound override has no continuePlaying function");
        }
        catch (Exception e)
        {
            FiskHeroes.LOGGER.warn("Could not compile a sound override: {}", e.toString());
        }
        finally
        {
            Context.exit();
        }

        return null;
    }

    /** Runs the function with the entity and the playing sound; returns whether it keeps playing. */
    public boolean continuePlaying(Entity entity, Object sound)
    {
        Context cx = JSContext.enter();

        try
        {
            Object result = function.call(cx, scope, scope, new Object[] { new JSEntity(entity), sound });
            Object value = JSContext.unwrap(result);
            return value instanceof Boolean b ? b : Boolean.TRUE.equals(value);
        }
        catch (Exception e)
        {
            FiskHeroes.LOGGER.error("Error while running a sound override", e);
            return true;
        }
        finally
        {
            Context.exit();
        }
    }
}
