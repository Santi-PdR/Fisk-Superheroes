package com.fiskmods.heroes.common.util;

/** Shared timing for the port's synchronized tentacle action visuals. */
public final class TentacleVisualMath
{
    public static final int NONE = 0;
    public static final int JAB = 1;
    public static final int GRAB = 2;
    public static final int CHARGE = 3;
    public static final int STRIKE = 4;
    public static final int ACTION_TICKS = 16;

    private TentacleVisualMath() { }

    public static float pulse(float progress)
    {
        return (float) Math.sin(Math.PI * Math.max(0.0F, Math.min(1.0F, progress)));
    }

    public static float reach(int action, float progress)
    {
        return action == JAB || action == GRAB || action == STRIKE ? pulse(progress) : 0.0F;
    }

    public static float grab(int action, float progress)
    {
        return action == GRAB ? pulse(progress) : 0.0F;
    }

    public static float strike(int action, float progress)
    {
        float value = Math.max(0.0F, Math.min(1.0F, progress));
        return action == CHARGE ? value : action == STRIKE ? 1.0F - value : 0.0F;
    }
}
