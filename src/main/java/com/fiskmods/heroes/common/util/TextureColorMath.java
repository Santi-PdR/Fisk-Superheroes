package com.fiskmods.heroes.common.util;

/** Pixel operations for the original suit texture definitions. Pixels are NativeImage ABGR. */
public final class TextureColorMath
{
    private TextureColorMath() {}

    /** Original XOR treats the second image as an alpha mask, rather than a bitwise XOR. */
    public static int xor(int source, int mask)
    {
        return (mask & 0xFF000000) != 0 ? 0 : source;
    }

    /** Original OR replaces a pixel whenever the overlay has any alpha. */
    public static int or(int source, int overlay)
    {
        return (overlay & 0xFF000000) != 0 ? overlay : source;
    }

    /** Shift RGB hue by degrees, preserving the original alpha and NativeImage channel order. */
    public static int hue(int abgr, float degrees)
    {
        int alpha = abgr & 0xFF000000;
        int r = abgr & 255;
        int g = (abgr >>> 8) & 255;
        int b = (abgr >>> 16) & 255;
        float max = Math.max(r, Math.max(g, b)) / 255.0F;
        float min = Math.min(r, Math.min(g, b)) / 255.0F;
        float delta = max - min;
        float hue;
        if (delta < 1.0E-5F) hue = 0.0F;
        else if (max == r / 255.0F) hue = ((g - b) / 255.0F / delta) % 6.0F;
        else if (max == g / 255.0F) hue = (b - r) / 255.0F / delta + 2.0F;
        else hue = (r - g) / 255.0F / delta + 4.0F;
        hue = (hue / 6.0F + degrees / 360.0F) % 1.0F;
        if (hue < 0.0F) hue += 1.0F;
        float saturation = max <= 0.0F ? 0.0F : delta / max;
        float h = hue * 6.0F;
        int sector = (int) h;
        float f = h - sector;
        float p = max * (1.0F - saturation);
        float q = max * (1.0F - saturation * f);
        float t = max * (1.0F - saturation * (1.0F - f));
        float rr, gg, bb;
        switch (sector % 6)
        {
            case 0 -> { rr = max; gg = t; bb = p; }
            case 1 -> { rr = q; gg = max; bb = p; }
            case 2 -> { rr = p; gg = max; bb = t; }
            case 3 -> { rr = p; gg = q; bb = max; }
            case 4 -> { rr = t; gg = p; bb = max; }
            default -> { rr = max; gg = p; bb = q; }
        }
        return alpha | (Math.round(bb * 255.0F) << 16) | (Math.round(gg * 255.0F) << 8) | Math.round(rr * 255.0F);
    }
}
