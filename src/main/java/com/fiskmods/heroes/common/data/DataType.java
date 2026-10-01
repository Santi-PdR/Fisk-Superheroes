package com.fiskmods.heroes.common.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Typed descriptor for a hero-pack data variable. This is the 1.20.1 counterpart of the
 * data types the original mod used for the {@code dataVars} section of heropack.json.
 */
public final class DataType<T>
{
    public static final DataType<Boolean> BOOLEAN = new DataType<>("BOOLEAN", Boolean.class, Boolean.FALSE,
            (buf, v) -> buf.writeBoolean(v), buf -> buf.readBoolean(), Boolean::booleanValue, JsonElement::getAsBoolean);
    public static final DataType<Byte> BYTE = new DataType<>("BYTE", Byte.class, (byte) 0,
            FriendlyByteBuf::writeByte, ByteBuf::readByte, JsonElement::getAsByte, e -> (byte) e.getAsInt());
    public static final DataType<Integer> INT = new DataType<>("INT", Integer.class, 0,
            FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt, JsonElement::getAsInt);
    public static final DataType<Float> FLOAT = new DataType<>("FLOAT", Float.class, 0.0F,
            ByteBuf::writeFloat, ByteBuf::readFloat, JsonElement::getAsFloat);
    /** A float which is interpolated over time on the client when rendered. */
    public static final DataType<Float> FLOAT_INTERP = new DataType<>("FLOAT_INTERP", Float.class, 0.0F,
            ByteBuf::writeFloat, ByteBuf::readFloat, JsonElement::getAsFloat);
    public static final DataType<String> STRING = new DataType<>("STRING", String.class, "",
            (buf, v) -> buf.writeUtf(v), b -> b.readUtf(32767), JsonElement::getAsString);

    private static final DataType<?>[] TYPES = { BOOLEAN, BYTE, INT, FLOAT, FLOAT_INTERP, STRING };

    public interface TypeWriter<T>
    {
        void write(ByteBuf buf, T value);
    }

    public interface TypeReader<T>
    {
        T read(ByteBuf buf);
    }

    public interface TypeParser<T>
    {
        T parse(JsonElement json);
    }

    private final String name;
    private final Class<T> typeClass;
    private final T defaultValue;
    private final TypeWriter<T> writer;
    private final TypeReader<T> reader;
    private final TypeParser<T> parser;

    private DataType(String name, Class<T> typeClass, T defaultValue, TypeWriter<T> writer, TypeReader<T> reader, TypeParser<T> parser)
    {
        this.name = name;
        this.typeClass = typeClass;
        this.defaultValue = defaultValue;
        this.writer = writer;
        this.reader = reader;
        this.parser = parser;
    }

    public String getName()
    {
        return name;
    }

    public Class<T> getTypeClass()
    {
        return typeClass;
    }

    public T getDefaultValue()
    {
        return defaultValue;
    }

    public void write(ByteBuf buf, T value)
    {
        writer.write(buf, value);
    }

    @SuppressWarnings("unchecked")
    public T read(ByteBuf buf)
    {
        return reader.read(buf);
    }

    @SuppressWarnings("unchecked")
    public T parse(JsonElement json)
    {
        if (json == null || json.isJsonNull())
        {
            return defaultValue;
        }

        try
        {
            return parser.parse(json);
        }
        catch (Exception e)
        {
            return defaultValue;
        }
    }

    public JsonElement toJson(T value)
    {
        if (value instanceof Boolean)
        {
            return new JsonPrimitive((Boolean) value);
        }

        if (value instanceof Number)
        {
            return new JsonPrimitive((Number) value);
        }

        return new JsonPrimitive(String.valueOf(value));
    }

    @Override
    public String toString()
    {
        return name;
    }

    public static DataType<?> byName(String name)
    {
        for (DataType<?> type : TYPES)
        {
            if (type.name.equalsIgnoreCase(name))
            {
                return type;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T copy(DataType<T> type, Object value)
    {
        return value == null ? type.getDefaultValue() : (T) value;
    }
}
