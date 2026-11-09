package com.fiskmods.heroes.common.util;

/** The original staff's sinusoidal attack motion, bounded for interpolated render input. */
public final class StaffSwingMath
{
    private StaffSwingMath() {}

    public static float amount(float progress)
    {
        if (!Float.isFinite(progress)) return 0.0F;
        float bounded = Math.max(0.0F, Math.min(1.0F, progress));
        return (float) Math.sin(bounded * Math.PI);
    }
}
