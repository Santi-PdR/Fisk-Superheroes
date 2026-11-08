package com.fiskmods.heroes.common.util;

/** A two-sided triangle encoded as two degenerate quads for Minecraft's QUADS buffers. */
public final class TriangleQuad
{
    private static final int[] INDICES = {0, 1, 2, 2, 2, 1, 0, 0};

    private TriangleQuad() { }

    public static int vertexCount() { return INDICES.length; }
    public static int corner(int vertex) { return INDICES[vertex]; }
    public static boolean backFace(int vertex) { return vertex >= 4; }
}
