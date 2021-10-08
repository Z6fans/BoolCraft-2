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
     * Returns a bounding box expanded by the specified vector (if negative numbers are given it will shrink). Args: d
     */
    public AxisAlignedBB expand(double d)
    {
        return new AxisAlignedBB(this.minX - d, this.minY - d, this.minZ - d, this.maxX + d, this.maxY + d, this.maxZ + d);
    }

    /**
     * Offsets the current bounding box by the specified coordinates. Args: x, y, z
     */
    public AxisAlignedBB offset(double x, double y, double z)
    {
        return new AxisAlignedBB(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }
}
