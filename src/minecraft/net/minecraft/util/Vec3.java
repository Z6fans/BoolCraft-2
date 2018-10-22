package net.minecraft.util;

public class Vec3
{
    /** X coordinate of Vec3D */
    public double x;

    /** Y coordinate of Vec3D */
    public double y;

    /** Z coordinate of Vec3D */
    public double z;
    private static final String __OBFID = "CL_00000612";

    /**
     * Static method for creating a new Vec3D given the three x,y,z values. This is only called from the other static
     * method which creates and places it in the list.
     */
    public static Vec3 createVectorHelper(double xCoord, double yCoord, double zCoord)
    {
        return new Vec3(xCoord, yCoord, zCoord);
    }

    private Vec3(double xCoord, double yCoord, double zCoord)
    {
        if (xCoord == -0.0D)
        {
            xCoord = 0.0D;
        }

        if (yCoord == -0.0D)
        {
            yCoord = 0.0D;
        }

        if (zCoord == -0.0D)
        {
            zCoord = 0.0D;
        }

        this.x = xCoord;
        this.y = yCoord;
        this.z = zCoord;
    }

    /**
     * Adds the specified x,y,z vector components to this vector and returns the resulting vector. Does not change this
     * vector.
     */
    public Vec3 addVector(double xCoord, double yCoord, double zCoord)
    {
        return createVectorHelper(this.x + xCoord, this.y + yCoord, this.z + zCoord);
    }

    /**
     * The square of the Euclidean distance between this and the specified vector.
     */
    public double quadranceTo(Vec3 v)
    {
        double var2 = v.x - this.x;
        double var4 = v.y - this.y;
        double var6 = v.z - this.z;
        return var2 * var2 + var4 * var4 + var6 * var6;
    }

    /**
     * Returns a new vector with x value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    public Vec3 getIntermediateWithXValue(Vec3 v, double xCoord)
    {
        double var4 = v.x - this.x;
        double var6 = v.y - this.y;
        double var8 = v.z - this.z;

        if (var4 * var4 < 1.0000000116860974E-7D)
        {
            return null;
        }
        else
        {
            double var10 = (xCoord - this.x) / var4;
            return var10 >= 0.0D && var10 <= 1.0D ? createVectorHelper(this.x + var4 * var10, this.y + var6 * var10, this.z + var8 * var10) : null;
        }
    }

    /**
     * Returns a new vector with y value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    public Vec3 getIntermediateWithYValue(Vec3 v, double yCoord)
    {
        double var4 = v.x - this.x;
        double var6 = v.y - this.y;
        double var8 = v.z - this.z;

        if (var6 * var6 < 1.0000000116860974E-7D)
        {
            return null;
        }
        else
        {
            double var10 = (yCoord - this.y) / var6;
            return var10 >= 0.0D && var10 <= 1.0D ? createVectorHelper(this.x + var4 * var10, this.y + var6 * var10, this.z + var8 * var10) : null;
        }
    }

    /**
     * Returns a new vector with z value equal to the second parameter, along the line between this vector and the
     * passed in vector, or null if not possible.
     */
    public Vec3 getIntermediateWithZValue(Vec3 v, double zCoord)
    {
        double var4 = v.x - this.x;
        double var6 = v.y - this.y;
        double var8 = v.z - this.z;

        if (var8 * var8 < 1.0000000116860974E-7D)
        {
            return null;
        }
        else
        {
            double var10 = (zCoord - this.z) / var8;
            return var10 >= 0.0D && var10 <= 1.0D ? createVectorHelper(this.x + var4 * var10, this.y + var6 * var10, this.z + var8 * var10) : null;
        }
    }

    public String toString()
    {
        return "(" + this.x + ", " + this.y + ", " + this.z + ")";
    }
}
