package net.minecraft.client.renderer;

import net.minecraft.util.AxisAlignedBB;

public class Frustrum
{
    private final ClippingHelper clippingHelper = ClippingHelper.getInstance();
    private double xPosition;
    private double yPosition;
    private double zPosition;

    public void setPosition(double x, double y, double z)
    {
        this.xPosition = x;
        this.yPosition = y;
        this.zPosition = z;
    }

    /**
     * Returns true if the bounding box is inside all 6 clipping planes, otherwise returns false.
     */
    public boolean isBoundingBoxInFrustum(AxisAlignedBB aabb)
    {
    	return this.clippingHelper.isBoxInFrustum(aabb.minX - this.xPosition, aabb.minY - this.yPosition, aabb.minZ - this.zPosition, aabb.maxX - this.xPosition, aabb.maxY - this.yPosition, aabb.maxZ - this.zPosition);
    }
}
