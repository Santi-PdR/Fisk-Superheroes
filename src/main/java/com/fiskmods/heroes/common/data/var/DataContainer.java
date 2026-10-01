package com.fiskmods.heroes.common.data.var;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fiskmods.heroes.common.data.DataRegistry;
import com.fiskmods.heroes.common.data.DataType;

import net.minecraft.nbt.CompoundTag;

/**
 * Holds the data variable values of a single entity. Values are keyed by {@link DataVar} identity;
 * changes are tracked so they can be synchronized to tracking clients.
 */
public class DataContainer
{
    private final Map<DataVar<?>, Object> values = new LinkedHashMap<>();
    private final List<DataVar<?>> dirty = new ArrayList<>();

    public boolean isEmpty()
    {
        return values.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(DataVar<T> var)
    {
        Object value = values.get(var);
        return value != null ? (T) value : var.getDefault();
    }

    public <T> void set(DataVar<T> var, T value)
    {
        Object prev = values.get(var);
        values.put(var, value);

        if (!equals(prev, value))
        {
            markDirty(var);
        }
    }

    private static boolean equals(Object a, Object b)
    {
        return a == null ? b == null : a.equals(b);
    }

    public void markDirty(DataVar<?> var)
    {
        if (!dirty.contains(var))
        {
            dirty.add(var);
        }
    }

    public void setRaw(DataVar<?> var, Object value)
    {
        values.put(var, value);
    }

    public List<DataVar<?>> getDirty()
    {
        return dirty;
    }

    public void clearDirty()
    {
        dirty.clear();
    }

    public Map<DataVar<?>, Object> values()
    {
        return values;
    }

    /** Resets every variable flagged with {@code resetWithoutSuit} back to its default value. */
    public void resetWithoutSuit()
    {
        for (Map.Entry<DataVar<?>, Object> e : values.entrySet())
        {
            if (e.getKey().shouldResetWithoutSuit())
            {
                setRaw(e.getKey(), e.getKey().getDefault());
                markDirty(e.getKey());
            }
        }
    }

    public void resetAll()
    {
        for (DataVar<?> var : values.keySet())
        {
            setRaw(var, var.getDefault());
            markDirty(var);
        }
    }

    public void writeTo(CompoundTag tag)
    {
        for (Map.Entry<DataVar<?>, Object> e : values.entrySet())
        {
            DataVar<?> var = e.getKey();

            if (e.getValue() != null && !e.getValue().equals(var.getDefault()))
            {
                var.write(tag, var.getName().toString(), e.getValue());
            }
        }
    }

    public void readFrom(CompoundTag tag)
    {
        values.clear();

        for (DataVar<?> var : DataRegistry.INSTANCE)
        {
            String key = var.getName().toString();

            if (tag.contains(key))
            {
                values.put(var, var.read(tag, key));
            }
        }
    }

    public void copyTo(DataContainer target)
    {
        target.values.clear();
        target.values.putAll(values);
        target.dirty.clear();
        target.dirty.addAll(dirty);
    }

    /** Serializes dirty variables into a compound tag for network sync. */
    public CompoundTag writeDirty()
    {
        CompoundTag tag = new CompoundTag();

        for (DataVar<?> var : dirty)
        {
            Object value = values.get(var);

            if (value != null)
            {
                var.write(tag, var.getName().toString(), value);
            }
        }

        return tag;
    }

    public void readUpdate(CompoundTag tag)
    {
        for (Map.Entry<DataVar<?>, Object> e : values.entrySet())
        {
            DataVar<?> var = e.getKey();
            String key = var.getName().toString();

            if (tag.contains(key))
            {
                values.put(var, var.read(tag, key));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void setFromString(DataVar<T> var, String value)
    {
        DataType<T> type = var.getType();

        try
        {
            if (type == DataType.FLOAT || type == DataType.FLOAT_INTERP)
            {
                set(var, (T) Float.valueOf(Float.parseFloat(value)));
            }
            else if (type == DataType.INT)
            {
                set(var, (T) Integer.valueOf(Integer.parseInt(value)));
            }
            else if (type == DataType.BYTE)
            {
                set(var, (T) Byte.valueOf(Byte.parseByte(value)));
            }
            else if (type == DataType.BOOLEAN)
            {
                set(var, (T) Boolean.valueOf(Boolean.parseBoolean(value)));
            }
            else if (type == DataType.STRING)
            {
                set(var, (T) value);
            }
        }
        catch (Exception e)
        {
            // Ignore malformed script values
        }
    }
}
