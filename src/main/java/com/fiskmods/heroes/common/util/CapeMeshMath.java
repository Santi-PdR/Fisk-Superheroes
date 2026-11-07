package com.fiskmods.heroes.common.util;

/** Geometry for the legacy two-sided cape texture and its gliding-wing transition. */
public final class CapeMeshMath
{
    private CapeMeshMath() {}

    public static float segmentY(int index, float lengthPixels, int segments)
    {
        return index * (lengthPixels / 16.0F / segments);
    }

    /** The source narrows the deployed wings when falling steeply. */
    public static float wingFlare(float animation, double verticalVelocity, double speed)
    {
        float deployed = Math.max(0.0F, Math.min(1.0F, (animation - 0.5F) * 2.0F));
        float motion = verticalVelocity < 0.0D && speed > 1.0E-6D
                ? (float) Math.pow(Math.min(1.0D, -verticalVelocity / speed), 1.5D) : 0.0F;
        return deployed * (1.0F - motion * (1.0F - 1.0F / 6.0F));
    }

    /** Half-width of each of the five outer wing vertices, in model units. */
    public static float wingOuterX(int row, float width, float flare)
    {
        float f = Math.max(0.0F, Math.min(1.0F, flare));
        float[] extra = {2.75F, 2.25F, 0.9F, 0.25F, 0.0F};
        return width * (0.5F * (1.0F - f) + extra[row] * f);
    }

    /** Height for an outer wing vertex. Center vertices stay at the unflared heights. */
    public static float wingOuterY(int row, float length, float flare)
    {
        float f = Math.max(0.0F, Math.min(1.0F, flare));
        return switch (row)
        {
            case 0 -> -0.4F * f;
            case 1 -> length * 0.3F;
            case 2 -> length * (1.0F - 0.4F * f);
            case 3 -> length * (1.0F - 0.1F * f);
            case 4 -> length * (1.0F + 0.4F * f);
            default -> throw new IllegalArgumentException("wing row " + row);
        };
    }
}
