package net.minecraft.client.renderer;

import net.minecraft.util.AxisAlignedBB;

public class Frustrum
{
    private ClippingHelper clippingHelper = ClippingHelper.getInstance();
    private double xPosition;
    private double yPosition;
    private double zPosition;
    private static final String __OBFID = "CL_00000976";

    public void setPosition(double p_78547_1_, double p_78547_3_, double p_78547_5_)
    {
        this.xPosition = p_78547_1_;
        this.yPosition = p_78547_3_;
        this.zPosition = p_78547_5_;
    }

    /**
     * Returns true if the bounding box is inside all 6 clipping planes, otherwise returns false.
     */
    public boolean isBoundingBoxInFrustum(AxisAlignedBB p_78546_1_)
    {
    	return this.clippingHelper.isBoxInFrustum(p_78546_1_.minX - this.xPosition, p_78546_1_.minY - this.yPosition, p_78546_1_.minZ - this.zPosition, p_78546_1_.maxX - this.xPosition, p_78546_1_.maxY - this.yPosition, p_78546_1_.maxZ - this.zPosition);
    }
}
