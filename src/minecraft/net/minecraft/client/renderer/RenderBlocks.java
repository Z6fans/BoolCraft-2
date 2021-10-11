package net.minecraft.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.util.AxisAlignedBB;

public class RenderBlocks
{
    /** The IBlockAccess used by this instance of RenderBlocks */
    private final World world;

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

    public RenderBlocks(World wc)
    {
        this.world = wc;
    }

    public boolean renderBlockByRenderType(int bm, int x, int y, int z)
    {
    	int block = bm & 0xF;
    	int meta = bm >> 4;
    	
        AxisAlignedBB aabb = Block.getBlockById(block).generateCubicBoundingBox(meta);
    	this.renderMinX = aabb.minX;
        this.renderMaxX = aabb.maxX;
        this.renderMinY = aabb.minY;
        this.renderMaxY = aabb.maxY;
        this.renderMinZ = aabb.minZ;
        this.renderMaxZ = aabb.maxZ;
        
        switch (block)
        {
        case 2: return this.renderBlockRedstoneWire(block, meta, x, y, z);
        case 1: case 3: case 4: return this.renderStandardBlock(block, meta, x, y, z);
        default: return false;
        }
    }
    
    private int getColor(int block, int meta, int x, int y, int z, int said)
    {
    	int scale[] = {0, 136, 181, 204, 218, 227, 233, 238, 242, 245, 247, 249, 251, 253, 254, 255};
    	
    	switch (block)
    	{
    	case 1: return (said == 0 ? 0xFF606060 : said == 1 ? 0xFF505050 : 0xFF404040) | ((x + y + z) % 2 == 0 ? 0x060000 : 0x000006);
    	case 2: return 0xFF000000 | (0x101 * scale[meta]);
    	case 3: return (meta & 8) > 0 ? 0xFFEE39E4 : 0xFF701B6C;
    	case 4: return (meta & 8) > 0 ? 0xFFE91A64 : 0xFF5B0A27;
    	default: return 0xFF000000;
    	}
    }

    private boolean renderBlockRedstoneWire(int block, int meta, int x, int y, int z)
    {
        Tesselator tess = Tesselator.instance;
        
        tess.setColor_I(this.getColor(block, meta, x, y, z, 0));
        
        tess.addVertex(x + 1, y + 0.01, z + 1);
        tess.addVertex(x + 1, y + 0.01, z + 0);
        tess.addVertex(x + 0, y + 0.01, z + 0);
        tess.addVertex(x + 0, y + 0.01, z + 1);

        if (!this.world.isSolid(x, y + 1, z))
        {
            if (this.world.isSolid(x - 1, y, z) && this.world.isWire(x - 1, y + 1, z))
            {
                tess.addVertex(x + 0.01, y + 1, z + 1);
                tess.addVertex(x + 0.01, y + 0, z + 1);
                tess.addVertex(x + 0.01, y + 0, z + 0);
                tess.addVertex(x + 0.01, y + 1, z + 0);
            }

            if (this.world.isSolid(x + 1, y, z) && this.world.isWire(x + 1, y + 1, z))
            {
                tess.addVertex(x + 0.99, y + 0, z + 1);
                tess.addVertex(x + 0.99, y + 1, z + 1);
                tess.addVertex(x + 0.99, y + 1, z + 0);
                tess.addVertex(x + 0.99, y + 0, z + 0);
            }

            if (this.world.isSolid(x, y, z - 1) && this.world.isWire(x, y + 1, z - 1))
            {
                tess.addVertex(x + 1, y + 0, z + 0.01);
                tess.addVertex(x + 1, y + 1, z + 0.01);
                tess.addVertex(x + 0, y + 1, z + 0.01);
                tess.addVertex(x + 0, y + 0, z + 0.01);
            }

            if (this.world.isSolid(x, y, z + 1) && this.world.isWire(x, y + 1, z + 1))
            {
                tess.addVertex(x + 1, y + 1, z + 0.99);
                tess.addVertex(x + 1, y + 0, z + 0.99);
                tess.addVertex(x + 0, y + 0, z + 0.99);
                tess.addVertex(x + 0, y + 1, z + 0.99);
            }
        }

        return true;
    }

    private boolean renderStandardBlock(int block, int meta, int x, int y, int z)
    {
        boolean didRender = false;

    	Tesselator.instance.setColor_I(this.getColor(block, meta, x, y, z, 2));
        if (!this.world.isSolid(x, y - 1, z) || this.renderMinY > 0.0D)
        {
            this.renderFaceYNeg(x, y, z);
            didRender = true;
        }

    	Tesselator.instance.setColor_I(this.getColor(block, meta, x, y, z, 0));
        if (!this.world.isSolid(x, y + 1, z) || this.renderMaxY < 1.0D)
        {
            this.renderFaceYPos(x, y, z);
            didRender = true;
        }

    	Tesselator.instance.setColor_I(this.getColor(block, meta, x, y, z, 1));
        if (!this.world.isSolid(x, y, z - 1) || this.renderMinZ > 0.0D)
        {
            this.renderFaceZNeg(x, y, z);
            didRender = true;
        }

        if (!this.world.isSolid(x, y, z + 1) || this.renderMaxZ < 1.0D)
        {
            this.renderFaceZPos(x, y, z);
            didRender = true;
        }

        if (!this.world.isSolid(x - 1, y, z) || this.renderMinX > 0.0D)
        {
            this.renderFaceXNeg(x, y, z);
            didRender = true;
        }

        if (!this.world.isSolid(x + 1, y, z) || this.renderMaxX < 1.0D)
        {
            this.renderFaceXPos(x, y, z);
            didRender = true;
        }

        return didRender;
    }

    private void renderFaceYNeg(double x, double y, double z)
    {
        Tesselator tess = Tesselator.instance;

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
        Tesselator tess = Tesselator.instance;

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
        Tesselator tess = Tesselator.instance;

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
        Tesselator tess = Tesselator.instance;

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
        Tesselator tess = Tesselator.instance;

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
        Tesselator tess = Tesselator.instance;

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
