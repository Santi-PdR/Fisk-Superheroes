package com.fiskmods.heroes.common.util;

/** Segment positions for the legacy cape mesh, measured in model units. */
public final class CapeMeshMath
{
    private CapeMeshMath() {}

    public static float segmentY(int index, float lengthPixels, int segments)
    {
        return index * (lengthPixels / 16.0F / segments);
    }
}
