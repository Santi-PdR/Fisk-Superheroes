package com.fiskmods.heroes.common.data;

/**
 * The fraction of the way through the current client tick.
 * <p>
 * Common code needs it to interpolate data variables the way the original mod did
 * ({@code DataVarInterp}): values such as the flight timer are rendered smoothly between ticks.
 * The client updates it once per frame; on a dedicated server it stays at 1.
 */
public final class RenderTickInfo
{
    private static float partialTicks = 1.0F;

    private RenderTickInfo()
    {
    }

    public static void set(float partialTicks)
    {
        RenderTickInfo.partialTicks = partialTicks;
    }

    public static float get()
    {
        return partialTicks;
    }
}
