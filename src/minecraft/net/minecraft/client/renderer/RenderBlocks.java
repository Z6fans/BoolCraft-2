package net.minecraft.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.world.WorldServer;
import net.minecraft.util.AxisAlignedBB;

public class RenderBlocks
{
    /** The IBlockAccess used by this instance of RenderBlocks */
    private final WorldServer world;

    /** The minimum X value for rendering (default 0.0). */
    private double renderMinX;

    /** The maximum X value for rendering (default 1.0). */
    private double renderMaxX;

    /** The minimum Y value for rendering (default 0.0). */
    private double renderMinY;

    /** The maximum Y value for rendering (default 1.0). */
    private double renderMaxY;

    /** The minimum Z value for rendering (default 0.0). */
    private double renderMinZ;

    /** The maximum Z value for rendering (default 1.0). */
    private double renderMaxZ;

    public RenderBlocks(WorldServer wc)
    {
        this.world = wc;
    }

    public boolean renderBlockByRenderType(Block block, int x, int y, int z)
    {
        AxisAlignedBB aabb = block.generateCubicBoundingBox(0, 0, 0, this.world.getBlockMetadata(x, y, z));
    	this.renderMinX = aabb.minX;
        this.renderMaxX = aabb.maxX;
        this.renderMinY = aabb.minY;
        this.renderMaxY = aabb.maxY;
        this.renderMinZ = aabb.minZ;
        this.renderMaxZ = aabb.maxZ;
        
        switch (block.getRenderType())
        {
        case 0: return this.renderStandardBlock(block, x, y, z);
        case 5: return this.renderBlockRedstoneWire(block, x, y, z);
        default: return false;
        }
    }

    private boolean renderBlockRedstoneWire(Block block, int x, int y, int z)
    {
        Tessellator tess = Tessellator.instance;
        
        tess.setColor_I(block.colorMultiplier(this.world, x, y, z, 0));
        
        tess.addVertex(x + 1, y + 0.01, z + 1);
        tess.addVertex(x + 1, y + 0.01, z + 0);
        tess.addVertex(x + 0, y + 0.01, z + 0);
        tess.addVertex(x + 0, y + 0.01, z + 1);

        if (!this.world.getBlock(x, y + 1, z).isSolid())
        {
            if (this.world.getBlock(x - 1, y, z).isSolid() && this.world.getBlock(x - 1, y + 1, z) == Block.redstone_wire)
            {
                tess.addVertex(x + 0.01, y + 1, z + 1);
                tess.addVertex(x + 0.01, y + 0, z + 1);
                tess.addVertex(x + 0.01, y + 0, z + 0);
                tess.addVertex(x + 0.01, y + 1, z + 0);
            }

            if (this.world.getBlock(x + 1, y, z).isSolid() && this.world.getBlock(x + 1, y + 1, z) == Block.redstone_wire)
            {
                tess.addVertex(x + 0.99, y + 0, z + 1);
                tess.addVertex(x + 0.99, y + 1, z + 1);
                tess.addVertex(x + 0.99, y + 1, z + 0);
                tess.addVertex(x + 0.99, y + 0, z + 0);
            }

            if (this.world.getBlock(x, y, z - 1).isSolid() && this.world.getBlock(x, y + 1, z - 1) == Block.redstone_wire)
            {
                tess.addVertex(x + 1, y + 0, z + 0.01);
                tess.addVertex(x + 1, y + 1, z + 0.01);
                tess.addVertex(x + 0, y + 1, z + 0.01);
                tess.addVertex(x + 0, y + 0, z + 0.01);
            }

            if (this.world.getBlock(x, y, z + 1).isSolid() && this.world.getBlock(x, y + 1, z + 1) == Block.redstone_wire)
            {
                tess.addVertex(x + 1, y + 1, z + 0.99);
                tess.addVertex(x + 1, y + 0, z + 0.99);
                tess.addVertex(x + 0, y + 0, z + 0.99);
                tess.addVertex(x + 0, y + 1, z + 0.99);
            }
        }

        return true;
    }

    private boolean renderStandardBlock(Block block, int x, int y, int z)
    {
        boolean didRender = false;

    	Tessellator.instance.setColor_I(block.colorMultiplier(this.world, x, y, z, 2));
        if (!this.world.getBlock(x, y - 1, z).isSolid() || this.renderMinY > 0.0D)
        {
            this.renderFaceYNeg(x, y, z);
            didRender = true;
        }

    	Tessellator.instance.setColor_I(block.colorMultiplier(this.world, x, y, z, 0));
        if (!this.world.getBlock(x, y + 1, z).isSolid() || this.renderMaxY < 1.0D)
        {
            this.renderFaceYPos(x, y, z);
            didRender = true;
        }

    	Tessellator.instance.setColor_I(block.colorMultiplier(this.world, x, y, z, 1));
        if (!this.world.getBlock(x, y, z - 1).isSolid() || this.renderMinZ > 0.0D)
        {
            this.renderFaceZNeg(x, y, z);
            didRender = true;
        }

        if (!this.world.getBlock(x, y, z + 1).isSolid() || this.renderMaxZ < 1.0D)
        {
            this.renderFaceZPos(x, y, z);
            didRender = true;
        }

        if (!this.world.getBlock(x - 1, y, z).isSolid() || this.renderMinX > 0.0D)
        {
            this.renderFaceXNeg(x, y, z);
            didRender = true;
        }

        if (!this.world.getBlock(x + 1, y, z).isSolid() || this.renderMaxX < 1.0D)
        {
            this.renderFaceXPos(x, y, z);
            didRender = true;
        }

        return didRender;
    }

    private void renderFaceYNeg(double x, double y, double z)
    {
        Tessellator tess = Tessellator.instance;

        double xmin = x + this.renderMinX;
        double xmax = x + this.renderMaxX;
        double ymin = y + this.renderMinY;
        double zmin = z + this.renderMinZ;
        double zmax = z + this.renderMaxZ;

        tess.addVertex(xmin, ymin, zmax);
        tess.addVertex(xmin, ymin, zmin);
        tess.addVertex(xmax, ymin, zmin);
        tess.addVertex(xmax, ymin, zmax);
    }

    private void renderFaceYPos(double x, double y, double z)
    {
        Tessellator tess = Tessellator.instance;

        double xmin = x + this.renderMinX;
        double xmax = x + this.renderMaxX;
        double ymax = y + this.renderMaxY;
        double zmin = z + this.renderMinZ;
        double zmax = z + this.renderMaxZ;

        tess.addVertex(xmax, ymax, zmax);
        tess.addVertex(xmax, ymax, zmin);
        tess.addVertex(xmin, ymax, zmin);
        tess.addVertex(xmin, ymax, zmax);
    }

    private void renderFaceZNeg(double x, double y, double z)
    {
        Tessellator tess = Tessellator.instance;

        double xmin = x + this.renderMinX;
        double xmax = x + this.renderMaxX;
        double ymin = y + this.renderMinY;
        double ymax = y + this.renderMaxY;
        double zmin = z + this.renderMinZ;

        tess.addVertex(xmin, ymax, zmin);
        tess.addVertex(xmax, ymax, zmin);
        tess.addVertex(xmax, ymin, zmin);
        tess.addVertex(xmin, ymin, zmin);
    }

    private void renderFaceZPos(double x, double y, double z)
    {
        Tessellator tess = Tessellator.instance;

        double xmin = x + this.renderMinX;
        double xmax = x + this.renderMaxX;
        double ymin = y + this.renderMinY;
        double ymax = y + this.renderMaxY;
        double zmax = z + this.renderMaxZ;

        tess.addVertex(xmin, ymax, zmax);
        tess.addVertex(xmin, ymin, zmax);
        tess.addVertex(xmax, ymin, zmax);
        tess.addVertex(xmax, ymax, zmax);
    }

    private void renderFaceXNeg(double x, double y, double z)
    {
        Tessellator tess = Tessellator.instance;

        double xmin = x + this.renderMinX;
        double ymin = y + this.renderMinY;
        double ymax = y + this.renderMaxY;
        double zmin = z + this.renderMinZ;
        double zmax = z + this.renderMaxZ;

        tess.addVertex(xmin, ymax, zmax);
        tess.addVertex(xmin, ymax, zmin);
        tess.addVertex(xmin, ymin, zmin);
        tess.addVertex(xmin, ymin, zmax);
    }

    private void renderFaceXPos(double x, double y, double z)
    {
        Tessellator tess = Tessellator.instance;

        double xmax = x + this.renderMaxX;
        double ymin = y + this.renderMinY;
        double ymax = y + this.renderMaxY;
        double zmin = z + this.renderMinZ;
        double zmax = z + this.renderMaxZ;

        tess.addVertex(xmax, ymin, zmax);
        tess.addVertex(xmax, ymin, zmin);
        tess.addVertex(xmax, ymax, zmin);
        tess.addVertex(xmax, ymax, zmax);
    }
}
