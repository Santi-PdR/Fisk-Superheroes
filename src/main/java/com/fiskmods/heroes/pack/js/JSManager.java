package com.fiskmods.heroes.pack.js;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.DataVar;

import net.minecraft.world.entity.Entity;

/**
 * The {@code manager} object passed to tick handlers. Provides the data manipulation helpers the
 * pack scripts use to drive their state machines.
 */
public class JSManager
{
    /** Increases or decreases a normalized timer using one duration in both directions. */
    public void incrementData(JSEntity entity, String key, float ticks, boolean condition)
    {
        incrementData(entity, key, ticks, ticks, condition);
    }

    /** Changes a normalized timer at independent rates while the supplied conditions hold. */
    public void incrementData(JSEntity entity, String key, float ticksIncr, float ticksDecr, boolean condition)
    {
        incrementData(entity, key, ticksIncr, ticksDecr, condition, !condition);
    }

    public void incrementData(JSEntity entity, String key, float ticks, boolean incr, boolean decr)
    {
        incrementData(entity, key, ticks, ticks, incr, decr);
    }

    /** Original six-argument helper used by cooldown/boost scripts. Timers stay in [0, 1]. */
    public void incrementData(JSEntity entity, String key, float ticksIncr, float ticksDecr,
            boolean incr, boolean decr)
    {
        DataVar<?> variable = var(key);
        if (variable == null)
        {
            throw new IllegalArgumentException("Unknown data entry: '" + key + "'");
        }
        if (variable.getType() != com.fiskmods.heroes.common.data.DataType.FLOAT
                && variable.getType() != com.fiskmods.heroes.common.data.DataType.FLOAT_INTERP)
        {
            throw new IllegalArgumentException("Cannot increment non-float data variable '" + key + "'");
        }

        Entity target = entity.unwrap();
        float value = current(target, key);
        if (incr && value < 1.0F)
        {
            value += 1.0F / safeDuration(ticksIncr);
        }
        else if (decr && value > 0.0F)
        {
            value -= 1.0F / safeDuration(ticksDecr);
        }
        set(target, key, value, 1.0F);
    }

    private static float safeDuration(float ticks)
    {
        return ticks > 0.0F ? ticks : 1.0F;
    }

    public void setData(JSEntity entity, String key, Object value)
    {
        JSEntity.apply(data(entity.unwrap()), var(key), value);
    }

    public void setDataWithNotify(JSEntity entity, String key, Object value)
    {
        setData(entity, key, value);
        DataVar<?> var = var(key);

        if (var != null)
        {
            data(entity.unwrap()).getData().markDirty(var);
        }
    }

    public float getData(JSEntity entity, String key)
    {
        return current(entity.unwrap(), key);
    }

    /* --- internal helpers --- */

    private static SHPlayerData data(Entity entity)
    {
        return SHDataCapabilities.getPlayer(entity);
    }

    private static DataVar<?> var(String key)
    {
        return com.fiskmods.heroes.common.data.DataRegistry.INSTANCE.get(key);
    }

    private static float current(Entity entity, String key)
    {
        SHPlayerData data = data(entity);
        DataVar<?> var = var(key);

        if (data == null || var == null)
        {
            return 0;
        }

        Object value = data.getData().get(var);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    private static void set(Entity entity, String key, float value, float max)
    {
        SHPlayerData data = data(entity);
        DataVar<?> var = var(key);

        if (data == null || var == null)
        {
            return;
        }

        float clamped = Math.max(0.0F, Math.min(max, value));
        JSEntity.apply(data, var, clamped);
    }
}
