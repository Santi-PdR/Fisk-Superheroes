package com.fiskmods.heroes.common.util;

/** RGB midpoint of a pack cloud's hexadecimal start/end colors. */
public final class CloudColorMath
{
    private CloudColorMath() { }

    public static float[] hexGradient(String start, String end)
    {
        Integer a = hexColor(start);
        Integer b = hexColor(end);
        if (a == null || b == null) return null;
        return new float[] {
                (((a >> 16) & 255) + ((b >> 16) & 255)) / 510.0F,
                (((a >> 8) & 255) + ((b >> 8) & 255)) / 510.0F,
                ((a & 255) + (b & 255)) / 510.0F
        };
    }

    public static Integer hexColor(String value)
    {
        if (value == null) return null;
        String text = value.startsWith("0x") || value.startsWith("0X") ? value.substring(2)
                : value.startsWith("#") ? value.substring(1) : value;
        if (!text.matches("[0-9a-fA-F]{6}")) return null;
        return Integer.parseInt(text, 16);
    }
}
