package net.minecraft.util;

public class AxisAlignedBB
{
    public final double minX;
    public final double minY;
    public final double minZ;
    public final double maxX;
    public final double maxY;
    public final double maxZ;

    public AxisAlignedBB(double p_i2300_1_, double p_i2300_3_, double p_i2300_5_, double p_i2300_7_, double p_i2300_9_, double p_i2300_11_)
    {
        this.minX = p_i2300_1_;
        this.minY = p_i2300_3_;
        this.minZ = p_i2300_5_;
        this.maxX = p_i2300_7_;
        this.maxY = p_i2300_9_;
        this.maxZ = p_i2300_11_;
    }

    /**
     * Adds the coordinates to the bounding box extending it if the point lies outside the current ranges. Args: x, y, z
     */
    public AxisAlignedBB addCoord(double x, double y, double z)
    {
        return new AxisAlignedBB(x < 0.0D ? this.minX + x : this.minX,
        		                 y < 0.0D ? this.minY + y : this.minY,
        		                 z < 0.0D ? this.minZ + z : this.minZ,
        		                 x > 0.0D ? this.maxX + x : this.maxX,
        		                 y > 0.0D ? this.maxY + y : this.maxY,
        		                 z > 0.0D ? this.maxZ + z : this.maxZ);
    }

    /**
     * Returns a bounding box expanded by the specified vector (if negative numbers are given it will shrink). Args: d
     */
    public AxisAlignedBB expand(double d)
    {
        return new AxisAlignedBB(this.minX - d, this.minY - d, this.minZ - d, this.maxX + d, this.maxY + d, this.maxZ + d);
    }

    /**
     * Returns whether the given bounding box intersects with this one. Args: axisAlignedBB
     */
    public boolean intersectsWith(double x, double y, double z)
    {
        return x + 1 > this.minX && x < this.maxX && y + 1 > this.minY && y < this.maxY && z + 1 > this.minZ && z < this.maxZ;
    }

    /**
     * Offsets the current bounding box by the specified coordinates. Args: x, y, z
     */
    public AxisAlignedBB offset(double x, double y, double z)
    {
        return new AxisAlignedBB(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }
}
