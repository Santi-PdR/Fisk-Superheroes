package com.fiskmods.heroes.pack.js;

import javax.annotation.Nullable;
import javax.script.ScriptEngine;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.pack.ScriptFunction;

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
    private final ScriptFunction function;

    private SoundOverride(ScriptFunction function)
    {
        this.function = function;
    }

    @Nullable
    public static SoundOverride compile(@Nullable String source)
    {
        if (source == null || source.isEmpty())
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
            JSContext.evaluate(engine, source, "sound override");
            ScriptFunction function = JSContext.wrap(JSContext.get(engine, "continuePlaying"));

            if (function == null)
            {
                FiskHeroes.LOGGER.warn("Sound override has no continuePlaying function");
            }

            return function != null ? new SoundOverride(function) : null;
        }
        catch (Exception e)
        {
            FiskHeroes.LOGGER.warn("Could not compile a sound override: {}", e.toString());
            return null;
        }
    }

    /** Runs the function with the entity and the playing sound; returns whether it keeps playing. */
    public boolean continuePlaying(Entity entity, Object sound)
    {
        Object value = function.call(new JSEntity(entity), sound);
        return value instanceof Boolean b ? b : value != null;
    }
}
