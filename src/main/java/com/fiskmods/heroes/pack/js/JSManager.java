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
    public void incrementData(JSEntity entity, String key, float max, boolean flag)
    {
        incrementData(entity, key, 1.0F, max, flag);
    }

    public void incrementData(JSEntity entity, String key, float amount, float max, boolean flag)
    {
        set(entity.unwrap(), key, current(entity.unwrap(), key) + (flag ? amount : -Math.max(1.0F, amount)), max);
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
