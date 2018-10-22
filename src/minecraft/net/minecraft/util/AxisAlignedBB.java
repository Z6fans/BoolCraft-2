package net.minecraft.util;

public class AxisAlignedBB
{
    public double minX;
    public double minY;
    public double minZ;
    public double maxX;
    public double maxY;
    public double maxZ;
    private static final String __OBFID = "CL_00000607";

    /**
     * Returns a bounding box with the specified bounds. Args: minX, minY, minZ, maxX, maxY, maxZ
     */
    public static AxisAlignedBB getBoundingBox(double p_72330_0_, double p_72330_2_, double p_72330_4_, double p_72330_6_, double p_72330_8_, double p_72330_10_)
    {
        return new AxisAlignedBB(p_72330_0_, p_72330_2_, p_72330_4_, p_72330_6_, p_72330_8_, p_72330_10_);
    }

    private AxisAlignedBB(double p_i2300_1_, double p_i2300_3_, double p_i2300_5_, double p_i2300_7_, double p_i2300_9_, double p_i2300_11_)
    {
        this.minX = p_i2300_1_;
        this.minY = p_i2300_3_;
        this.minZ = p_i2300_5_;
        this.maxX = p_i2300_7_;
        this.maxY = p_i2300_9_;
        this.maxZ = p_i2300_11_;
    }

    /**
     * Sets the bounds of the bounding box. Args: minX, minY, minZ, maxX, maxY, maxZ
     */
    public AxisAlignedBB setBounds(double p_72324_1_, double p_72324_3_, double p_72324_5_, double p_72324_7_, double p_72324_9_, double p_72324_11_)
    {
        this.minX = p_72324_1_;
        this.minY = p_72324_3_;
        this.minZ = p_72324_5_;
        this.maxX = p_72324_7_;
        this.maxY = p_72324_9_;
        this.maxZ = p_72324_11_;
        return this;
    }

    /**
     * Adds the coordinates to the bounding box extending it if the point lies outside the current ranges. Args: x, y, z
     */
    public AxisAlignedBB addCoord(double p_72321_1_, double p_72321_3_, double p_72321_5_)
    {
        double var7 = this.minX;
        double var9 = this.minY;
        double var11 = this.minZ;
        double var13 = this.maxX;
        double var15 = this.maxY;
        double var17 = this.maxZ;

        if (p_72321_1_ < 0.0D)
        {
            var7 += p_72321_1_;
        }

        if (p_72321_1_ > 0.0D)
        {
            var13 += p_72321_1_;
        }

        if (p_72321_3_ < 0.0D)
        {
            var9 += p_72321_3_;
        }

        if (p_72321_3_ > 0.0D)
        {
            var15 += p_72321_3_;
        }

        if (p_72321_5_ < 0.0D)
        {
            var11 += p_72321_5_;
        }

        if (p_72321_5_ > 0.0D)
        {
            var17 += p_72321_5_;
        }

        return getBoundingBox(var7, var9, var11, var13, var15, var17);
    }

    /**
     * Returns a bounding box expanded by the specified vector (if negative numbers are given it will shrink). Args: x,
     * y, z
     */
    public AxisAlignedBB expand(double p_72314_1_, double p_72314_3_, double p_72314_5_)
    {
        double var7 = this.minX - p_72314_1_;
        double var9 = this.minY - p_72314_3_;
        double var11 = this.minZ - p_72314_5_;
        double var13 = this.maxX + p_72314_1_;
        double var15 = this.maxY + p_72314_3_;
        double var17 = this.maxZ + p_72314_5_;
        return getBoundingBox(var7, var9, var11, var13, var15, var17);
    }

    /**
     * Returns a bounding box offseted by the specified vector (if negative numbers are given it will shrink). Args: x,
     * y, z
     */
    public AxisAlignedBB getOffsetBoundingBox(double p_72325_1_, double p_72325_3_, double p_72325_5_)
    {
        return getBoundingBox(this.minX + p_72325_1_, this.minY + p_72325_3_, this.minZ + p_72325_5_, this.maxX + p_72325_1_, this.maxY + p_72325_3_, this.maxZ + p_72325_5_);
    }

    /**
     * if instance and the argument bounding boxes overlap in the Y and Z dimensions, calculate the offset between them
     * in the X dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
     * calculated offset.  Otherwise return the calculated offset.
     */
    public double calculateXOffset(AxisAlignedBB p_72316_1_, double p_72316_2_)
    {
        if (p_72316_1_.maxY > this.minY && p_72316_1_.minY < this.maxY)
        {
            if (p_72316_1_.maxZ > this.minZ && p_72316_1_.minZ < this.maxZ)
            {
                double var4;

                if (p_72316_2_ > 0.0D && p_72316_1_.maxX <= this.minX)
                {
                    var4 = this.minX - p_72316_1_.maxX;

                    if (var4 < p_72316_2_)
                    {
                        p_72316_2_ = var4;
                    }
                }

                if (p_72316_2_ < 0.0D && p_72316_1_.minX >= this.maxX)
                {
                    var4 = this.maxX - p_72316_1_.minX;

                    if (var4 > p_72316_2_)
                    {
                        p_72316_2_ = var4;
                    }
                }

                return p_72316_2_;
            }
            else
            {
                return p_72316_2_;
            }
        }
        else
        {
            return p_72316_2_;
        }
    }

    /**
     * if instance and the argument bounding boxes overlap in the X and Z dimensions, calculate the offset between them
     * in the Y dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
     * calculated offset.  Otherwise return the calculated offset.
     */
    public double calculateYOffset(AxisAlignedBB p_72323_1_, double p_72323_2_)
    {
        if (p_72323_1_.maxX > this.minX && p_72323_1_.minX < this.maxX)
        {
            if (p_72323_1_.maxZ > this.minZ && p_72323_1_.minZ < this.maxZ)
            {
                double var4;

                if (p_72323_2_ > 0.0D && p_72323_1_.maxY <= this.minY)
                {
                    var4 = this.minY - p_72323_1_.maxY;

                    if (var4 < p_72323_2_)
                    {
                        p_72323_2_ = var4;
                    }
                }

                if (p_72323_2_ < 0.0D && p_72323_1_.minY >= this.maxY)
                {
                    var4 = this.maxY - p_72323_1_.minY;

                    if (var4 > p_72323_2_)
                    {
                        p_72323_2_ = var4;
                    }
                }

                return p_72323_2_;
            }
            else
            {
                return p_72323_2_;
            }
        }
        else
        {
            return p_72323_2_;
        }
    }

    /**
     * if instance and the argument bounding boxes overlap in the Y and X dimensions, calculate the offset between them
     * in the Z dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
     * calculated offset.  Otherwise return the calculated offset.
     */
    public double calculateZOffset(AxisAlignedBB p_72322_1_, double p_72322_2_)
    {
        if (p_72322_1_.maxX > this.minX && p_72322_1_.minX < this.maxX)
        {
            if (p_72322_1_.maxY > this.minY && p_72322_1_.minY < this.maxY)
            {
                double var4;

                if (p_72322_2_ > 0.0D && p_72322_1_.maxZ <= this.minZ)
                {
                    var4 = this.minZ - p_72322_1_.maxZ;

                    if (var4 < p_72322_2_)
                    {
                        p_72322_2_ = var4;
                    }
                }

                if (p_72322_2_ < 0.0D && p_72322_1_.minZ >= this.maxZ)
                {
                    var4 = this.maxZ - p_72322_1_.minZ;

                    if (var4 > p_72322_2_)
                    {
                        p_72322_2_ = var4;
                    }
                }

                return p_72322_2_;
            }
            else
            {
                return p_72322_2_;
            }
        }
        else
        {
            return p_72322_2_;
        }
    }

    /**
     * Returns whether the given bounding box intersects with this one. Args: axisAlignedBB
     */
    public boolean intersectsWith(AxisAlignedBB p_72326_1_)
    {
        return p_72326_1_.maxX > this.minX && p_72326_1_.minX < this.maxX ? (p_72326_1_.maxY > this.minY && p_72326_1_.minY < this.maxY ? p_72326_1_.maxZ > this.minZ && p_72326_1_.minZ < this.maxZ : false) : false;
    }

    /**
     * Offsets the current bounding box by the specified coordinates. Args: x, y, z
     */
    public AxisAlignedBB offset(double p_72317_1_, double p_72317_3_, double p_72317_5_)
    {
        this.minX += p_72317_1_;
        this.minY += p_72317_3_;
        this.minZ += p_72317_5_;
        this.maxX += p_72317_1_;
        this.maxY += p_72317_3_;
        this.maxZ += p_72317_5_;
        return this;
    }

    public String toString()
    {
        return "box[" + this.minX + ", " + this.minY + ", " + this.minZ + " -> " + this.maxX + ", " + this.maxY + ", " + this.maxZ + "]";
    }
}
