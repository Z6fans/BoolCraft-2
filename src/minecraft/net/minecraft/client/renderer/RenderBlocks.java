package net.minecraft.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.WorldClient;
import net.minecraft.util.AxisAlignedBB;

public class RenderBlocks
{
    /** The IBlockAccess used by this instance of RenderBlocks */
    private final WorldClient world;

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

    public RenderBlocks(WorldClient wc)
    {
        this.world = wc;
    }

    public boolean renderBlockByRenderType(Block block, int x, int y, int z)
    {
        AxisAlignedBB aabb = block.generateCubicBoundingBox(0, 0, 0);
    	this.renderMinX = aabb.minX;
        this.renderMaxX = aabb.maxX;
        this.renderMinY = aabb.minY;
        this.renderMaxY = aabb.maxY;
        this.renderMinZ = aabb.minZ;
        this.renderMaxZ = aabb.maxZ;
        
        switch (block.getRenderType())
        {
        case 0: return this.renderStandardBlock(x, y, z);
        case 5: return this.renderBlockRedstoneWire(block, x, y, z);
        case 12: return this.renderBlockLever(block, x, y, z);
        default: return false;
        }
    }

    private void setRenderBounds(double minx, double miny, double minz, double maxx, double maxy, double maxz)
    {
    	this.renderMinX = minx;
        this.renderMaxX = maxx;
        this.renderMinY = miny;
        this.renderMaxY = maxy;
        this.renderMinZ = minz;
        this.renderMaxZ = maxz;
    }

    private boolean renderBlockLever(Block block, int x, int y, int z)
    {
        int var6 = this.world.getBlockMetadata(x, y, z) & 7;

        float d = 0.1875F;

        if (var6 == 5)
        {
            this.setRenderBounds((0.5F - d), 0.0D, (0.5F - d), (0.5F + d), d, (0.5F + d));
        }
        else if (var6 == 6)
        {
            this.setRenderBounds((0.5F - d), 0.0D, (0.5F - d), (0.5F + d), d, (0.5F + d));
        }
        else if (var6 == 4)
        {
            this.setRenderBounds((0.5F - d), (0.5F - d), (1.0F - d), (0.5F + d), (0.5F + d), 1.0D);
        }
        else if (var6 == 3)
        {
            this.setRenderBounds((0.5F - d), (0.5F - d), 0.0D, (0.5F + d), (0.5F + d), d);
        }
        else if (var6 == 2)
        {
            this.setRenderBounds((1.0F - d), (0.5F - d), (0.5F - d), 1.0D, (0.5F + d), (0.5F + d));
        }
        else if (var6 == 1)
        {
            this.setRenderBounds(0.0D, (0.5F - d), (0.5F - d), d, (0.5F + d), (0.5F + d));
        }
        else if (var6 == 0)
        {
            this.setRenderBounds((0.5F - d), (1.0F - d), (0.5F - d), (0.5F + d), 1.0D, (0.5F + d));
        }
        else if (var6 == 7)
        {
            this.setRenderBounds((0.5F - d), (1.0F - d), (0.5F - d), (0.5F + d), 1.0D, (0.5F + d));
        }
        
        Tessellator.instance.setColor_I(block.colorMultiplier(this.world, x, y, z));
        this.renderFaceYNeg(x, y, z);
        this.renderFaceYPos(x, y, z);
        this.renderFaceZNeg(x, y, z);
        this.renderFaceZPos(x, y, z);
        this.renderFaceXNeg(x, y, z);
        this.renderFaceXPos(x, y, z);

        return true;
    }

    private boolean renderBlockRedstoneWire(Block block, int x, int y, int z)
    {
        Tessellator tess = Tessellator.instance;
        
        tess.setColor_I(block.colorMultiplier(this.world, x, y, z));
        
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

    private boolean renderStandardBlock(int x, int y, int z)
    {
        boolean didRender = false;
        
        int shift = (x + y + z) % 2 == 0 ? 0x060000 : 0x000006;

    	Tessellator.instance.setColor_I(0xFF404040 | shift);
        if (!this.world.getBlock(x, y - 1, z).isSolid())
        {
            this.renderFaceYNeg(x, y, z);
            didRender = true;
        }

    	Tessellator.instance.setColor_I(0xFF606060 | shift);
        if (!this.world.getBlock(x, y + 1, z).isSolid())
        {
            this.renderFaceYPos(x, y, z);
            didRender = true;
        }

    	Tessellator.instance.setColor_I(0xFF505050 | shift);
        if (!this.world.getBlock(x, y, z - 1).isSolid())
        {
            this.renderFaceZNeg(x, y, z);
            didRender = true;
        }

        if (!this.world.getBlock(x, y, z + 1).isSolid())
        {
            this.renderFaceZPos(x, y, z);
            didRender = true;
        }

        if (!this.world.getBlock(x - 1, y, z).isSolid())
        {
            this.renderFaceXNeg(x, y, z);
            didRender = true;
        }

        if (!this.world.getBlock(x + 1, y, z).isSolid())
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
