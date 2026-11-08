package com.fiskmods.heroes.common.util;

/** Original tonfa flip curve, expressed independently of the renderer. */
public final class TonfaPoseMath
{
    private TonfaPoseMath() {}

    public static int mask(boolean left) { return left ? 1 : 2; }

    public static byte flip(byte state, boolean left)
    {
        return (byte) (state ^ mask(left));
    }

    public static float rotation(byte state, boolean left, float swingProgress, boolean swinging)
    {
        float progress = Float.isFinite(swingProgress) ? Math.max(0.0F, Math.min(1.0F, swingProgress)) : 0.0F;
        if (progress == 0.0F && !swinging) progress = 1.0F;
        progress = Math.min(progress * 1.5F, 1.0F);
        if ((state & mask(left)) != 0) progress -= 1.0F;
        float rotation = 1.0F + progress * 180.0F;
        return left ? rotation : -rotation;
    }
}
