package com.fiskmods.heroes.common.util;

/** The eight textured quads of the 1.7.10 SHRenderHooks.renderFire mesh.
 * Coordinates include its negative scale and 180 degree X rotation: one unit of
 * width here is the complete flame width, and positive Y is the flame tip.
 */
public final class LegacyFlameGeometry
{
    // Each vertex is x, y, z, u, v. UVs are normalized within one animation frame.
    // The first four quads use layer 0; the last four use layer 1.
    private static final float[][] QUADS = {
        { .3F,1,.5F,1,0, -.2F,0,.5F,1,1, -.2F,0,-.5F,0,1, .3F,1,-.5F,0,0 },
        { -.3F,1,-.5F,1,0, .2F,0,-.5F,1,1, .2F,0,.5F,0,1, -.3F,1,.5F,0,0 },
        { .5F,1,.4F,0,0, .5F,0,.5F,0,1, -.5F,0,.5F,1,1, -.5F,1,.4F,1,0 },
        { -.5F,1,-.4F,0,0, -.5F,0,-.5F,0,1, .5F,0,-.5F,1,1, .5F,1,-.4F,1,0 },
        { -.5F,1,.3F,1,0, -.5F,0,-.2F,1,1, .5F,0,-.2F,0,1, .5F,1,.3F,0,0 },
        { .5F,1,-.3F,1,0, .5F,0,.2F,1,1, -.5F,0,.2F,0,1, -.5F,1,-.3F,0,0 },
        { .4F,1,-.5F,0,0, .5F,0,-.5F,0,1, .5F,0,.5F,1,1, .4F,1,.5F,1,0 },
        { -.4F,1,.5F,0,0, -.5F,0,.5F,0,1, -.5F,0,-.5F,1,1, -.4F,1,-.5F,1,0 }
    };

    private LegacyFlameGeometry() { }
    public static int quadCount() { return QUADS.length; }
    public static int layer(int quad) { return quad / 4; }
    public static float component(int quad, int vertex, int component) { return QUADS[quad][vertex * 5 + component]; }

    /** FiskMath.curve(x), as used by both original booster renderers. */
    public static float curve(float progress)
    {
        float p = Math.max(0, Math.min(1, progress));
        return (float) ((1.0D - Math.cos(Math.PI * p)) * .5D);
    }
}
