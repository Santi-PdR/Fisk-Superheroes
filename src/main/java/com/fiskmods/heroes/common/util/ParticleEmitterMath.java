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

    /** Obsidian's first-person chain uses the eye and rotates with both pitch and yaw. */
    public static Vec3 firstPersonChainOffset(Vec3 offset, float scale, float yaw, float pitch)
    {
        Vec3 local = new Vec3(offset.x, -offset.y, -offset.z).scale(scale / 16.0D);
        return orient(local.xRot((float) Math.toRadians(-pitch)), yaw);
    }

    public static Vec3 orient(Vec3 vector, float yaw)
    {
        double radians = Math.toRadians(-yaw);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        return new Vec3(vector.x * cos + vector.z * sin, vector.y, vector.z * cos - vector.x * sin);
    }

    /** Quadratic chain curve: the control point lies on the caster's current view ray. */
    public static Vec3 telekinesisChainPoint(Vec3 start, Vec3 eye, Vec3 look, Vec3 target, double progress)
    {
        Vec3 control = eye.add(look.scale(start.distanceTo(target)));
        double t = Math.max(0.0D, Math.min(1.0D, progress));
        double inverse = 1.0D - t;
        return start.scale(inverse * inverse).add(control.scale(2.0D * inverse * t)).add(target.scale(t * t));
    }

    /** Legacy spacing is 0.15 blocks; cap pathological external-pack distances. */
    public static int telekinesisChainCount(double length)
    {
        if (!Double.isFinite(length) || length < 0.0D) return 0;
        return (int) Math.min(256.0D, Math.floor(length / 0.15D) + 1.0D);
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
