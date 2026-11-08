package com.fiskmods.heroes.common.util;

/** Glider limb offsets from the original player renderer. Angles are in radians. */
public final class GlidingPoseMath
{
    private GlidingPoseMath() { }

    public static float curve(float value)
    {
        value = Math.max(0.0F, Math.min(1.0F, value));
        return (float) ((Math.sin((value * 2.0F - 1.0F) * Math.PI / 2.0D) + 1.0D) * 0.5D);
    }

    public static float spread(float timer, double verticalVelocity, double speed)
    {
        double level = verticalVelocity < 0.0D && speed > 1.0E-8D
                ? 1.0D - Math.pow(Math.min(1.0D, -verticalVelocity / speed), 1.5D) : 1.0D;
        return (float) (curve(timer) * (level * Math.PI / 2.0D + (1.0D - level) * Math.PI / 12.0D));
    }
}
