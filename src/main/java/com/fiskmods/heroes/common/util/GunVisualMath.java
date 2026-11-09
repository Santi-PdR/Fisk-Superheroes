package com.fiskmods.heroes.common.util;

/** Deterministic visual-only hand alternation and short effect envelopes. */
public final class GunVisualMath
{
    private GunVisualMath() {}

    public static boolean useOffhand(boolean dual, boolean offhandEmpty, int magazineSize, int ammoBeforeShot)
    {
        if (!dual || !offhandEmpty || magazineSize <= 0 || ammoBeforeShot <= 0) return false;
        int ammo = Math.min(magazineSize, ammoBeforeShot);
        return ((magazineSize - ammo) & 1) != 0;
    }

    public static float envelope(float age, float duration)
    {
        if (!Float.isFinite(age) || !Float.isFinite(duration) || age < 0.0F || duration <= 0.0F || age >= duration)
            return 0.0F;
        return 1.0F - age / duration;
    }
}
