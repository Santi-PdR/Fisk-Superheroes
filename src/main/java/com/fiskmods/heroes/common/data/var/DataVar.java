package com.fiskmods.heroes.common.data.var;

import com.fiskmods.heroes.common.data.DataType;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * A named, typed value stored per-entity by the hero pack data system. Hero scripts address these
 * with {@code entity.getData("fiskheroes:dyn/flight_super_boost")}.
 */
public class DataVar<T>
{
    private final ResourceLocation name;
    private final DataType<T> type;
    private final T defaultValue;
    private final boolean resetWithoutSuit;

    public DataVar(ResourceLocation name, DataType<T> type, T defaultValue, boolean resetWithoutSuit)
    {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue != null ? defaultValue : type.getDefaultValue();
        this.resetWithoutSuit = resetWithoutSuit;
    }

    public ResourceLocation getName()
    {
        return name;
    }

    public DataType<T> getType()
    {
        return type;
    }

    public T getDefault()
    {
        return defaultValue;
    }

    public boolean shouldResetWithoutSuit()
    {
        return resetWithoutSuit;
    }

    public void write(CompoundTag tag, String key, Object value)
    {
        writeValue(tag, key, (T) value);
    }

    @SuppressWarnings("unchecked")
    private void writeValue(CompoundTag tag, String key, T value)
    {
        if (value == null)
        {
            return;
        }

        if (value instanceof Boolean)
        {
            tag.putBoolean(key, (Boolean) value);
        }
        else if (value instanceof Byte)
        {
            tag.putByte(key, (Byte) value);
        }
        else if (value instanceof Integer)
        {
            tag.putInt(key, (Integer) value);
        }
        else if (value instanceof Float)
        {
            tag.putFloat(key, (Float) value);
        }
        else if (value instanceof String)
        {
            tag.putString(key, (String) value);
        }
    }

    public T read(CompoundTag tag, String key)
    {
        if (!tag.contains(key))
        {
            return defaultValue;
        }

        try
        {
            if (defaultValue instanceof Boolean && tag.contains(key, Tag.TAG_BYTE))
            {
                return (T) Boolean.valueOf(tag.getBoolean(key));
            }
            if (defaultValue instanceof Byte && tag.contains(key, Tag.TAG_BYTE))
            {
                return (T) Byte.valueOf(tag.getByte(key));
            }
            if (defaultValue instanceof Integer && tag.contains(key, Tag.TAG_INT))
            {
                return (T) Integer.valueOf(tag.getInt(key));
            }
            if (defaultValue instanceof Float && (tag.contains(key, Tag.TAG_FLOAT) || tag.contains(key, Tag.TAG_INT)))
            {
                return (T) Float.valueOf(tag.getFloat(key));
            }
            if (defaultValue instanceof String && tag.contains(key, Tag.TAG_STRING))
            {
                return (T) tag.getString(key);
            }
        }
        catch (Exception e)
        {
            // Corrupt or mismatched entry: fall back to the default
        }

        return defaultValue;
    }

    @Override
    public String toString()
    {
        return String.valueOf(name);
    }
}
