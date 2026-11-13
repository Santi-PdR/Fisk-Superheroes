package com.fiskmods.heroes.common.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix3fc;
import org.joml.Vector3f;

/** Coordinate conventions used by the original pack-driven particle emitters. */
public final class ParticleEmitterMath
{
    private ParticleEmitterMath() {}

    /**
     * First-person offsets are relative to the local player's eye, not a model part. Anchored
     * offsets are in model pixels; unanchored offsets are already in blocks. The legacy client
     * used its local player's eye-height position as the origin and did not capture the arm pose.
     */
    public static Vec3 firstPersonOffset(Vec3 offset, boolean anchored, boolean mirroredSide, float scale, float yaw)
    {
        double units = scale * (anchored ? 1.0D / 16.0D : 1.0D);
        Vec3 local = new Vec3((mirroredSide ? offset.x : -offset.x) * units,
                -offset.y * units, -offset.z * units);
        return orient(local, yaw);
    }

    public static Vec3 orient(Vec3 vector, float yaw)
    {
        double radians = Math.toRadians(-yaw);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        return new Vec3(vector.x * cos + vector.z * sin, vector.y, vector.z * cos - vector.x * sin);
    }

    /** Undo the current view rotation before translating a render-layer point into world space. */
    public static Vec3 worldFromRenderTransform(Matrix4f transform, Matrix3fc inverseView, Vec3 camera, Vec3 modelPoint)
    {
        Vector3f point = new Vector3f((float) modelPoint.x, (float) modelPoint.y, (float) modelPoint.z);
        transform.transformPosition(point);
        point.mul(inverseView);
        return camera.add(point.x, point.y, point.z);
    }

    /** Original custom particles use the lesser of the pack distance and render distance. */
    public static boolean inRange(Vec3 source, Vec3 viewer, double maxDistance, int renderDistanceChunks)
    {
        double distance = Math.max(0.0D, Math.min(maxDistance, renderDistanceChunks * 16.0D));
        return source.distanceToSqr(viewer) <= distance * distance;
    }
}
