package com.fiskmods.heroes.common.util;

import net.minecraft.world.phys.Vec3;

/** Spherical lattice and 12-tick expansion/contraction used by the original breach. */
public final class TeleportBurstMath
{
    private TeleportBurstMath() { }
    public static int latitudeCount(boolean firstPerson) { return firstPerson ? 5 : 10; }
    public static int count(boolean firstPerson) { int n = latitudeCount(firstPerson); return n * n * 2; }
    public static Vec3 offset(int latitude, int longitude, boolean firstPerson, float width, float height)
    {
        int n = latitudeCount(firstPerson);
        Vec3 direction = new Vec3(0, 1.5D, 0).xRot((float) (Math.PI * (latitude + 1) / n))
                .yRot((float) (Math.PI * (longitude + 1) / n));
        return new Vec3(direction.x * width, direction.y * height * 0.5D, direction.z * width);
    }
}
