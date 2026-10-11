package com.fiskmods.heroes.pack.js;

import net.minecraft.world.phys.Vec3;

/** A vector handed to pack scripts. */
public class JSVector
{
    private final Vec3 vector;

    public JSVector(Vec3 vector)
    {
        this.vector = vector;
    }

    public double x()
    {
        return vector.x;
    }

    public double y()
    {
        return vector.y;
    }

    public double z()
    {
        return vector.z;
    }

    public double length()
    {
        return vector.length();
    }

    public double lengthSqr()
    {
        return vector.lengthSqr();
    }

    public JSVector add(double x, double y, double z)
    {
        return new JSVector(vector.add(x, y, z));
    }

    public JSVector scale(double scale)
    {
        return new JSVector(vector.scale(scale));
    }

    public JSVector normalize()
    {
        return new JSVector(vector.normalize());
    }

    @Override
    public String toString()
    {
        return vector.toString();
    }
}
