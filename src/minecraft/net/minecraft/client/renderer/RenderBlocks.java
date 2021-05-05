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
        int rt = block.getRenderType();

        if (rt == -1)
        {
            return false;
        }
        else
        {
            AxisAlignedBB aabb = block.generateCubicBoundingBox(0, 0, 0);
        	this.renderMinX = aabb.minX;
            this.renderMaxX = aabb.maxX;
            this.renderMinY = aabb.minY;
            this.renderMaxY = aabb.maxY;
            this.renderMinZ = aabb.minZ;
            this.renderMaxZ = aabb.maxZ;
            return rt == 0 ? this.renderStandardBlock(x, y, z) : (rt == 5 ? this.renderBlockRedstoneWire(block, x, y, z) : (rt == 12 ? this.renderBlockLever(block, x, y, z) : false));
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
            this.setRenderBounds((double)(0.5F - d), 0.0D, (double)(0.5F - d), (double)(0.5F + d), (double)d, (double)(0.5F + d));
        }
        else if (var6 == 6)
        {
            this.setRenderBounds((double)(0.5F - d), 0.0D, (double)(0.5F - d), (double)(0.5F + d), (double)d, (double)(0.5F + d));
        }
        else if (var6 == 4)
        {
            this.setRenderBounds((double)(0.5F - d), (double)(0.5F - d), (double)(1.0F - d), (double)(0.5F + d), (double)(0.5F + d), 1.0D);
        }
        else if (var6 == 3)
        {
            this.setRenderBounds((double)(0.5F - d), (double)(0.5F - d), 0.0D, (double)(0.5F + d), (double)(0.5F + d), (double)d);
        }
        else if (var6 == 2)
        {
            this.setRenderBounds((double)(1.0F - d), (double)(0.5F - d), (double)(0.5F - d), 1.0D, (double)(0.5F + d), (double)(0.5F + d));
        }
        else if (var6 == 1)
        {
            this.setRenderBounds(0.0D, (double)(0.5F - d), (double)(0.5F - d), (double)d, (double)(0.5F + d), (double)(0.5F + d));
        }
        else if (var6 == 0)
        {
            this.setRenderBounds((double)(0.5F - d), (double)(1.0F - d), (double)(0.5F - d), (double)(0.5F + d), 1.0D, (double)(0.5F + d));
        }
        else if (var6 == 7)
        {
            this.setRenderBounds((double)(0.5F - d), (double)(1.0F - d), (double)(0.5F - d), (double)(0.5F + d), 1.0D, (double)(0.5F + d));
        }
        
        Tessellator.instance.setColorOpaque_I(block.colorMultiplier(this.world, x, y, z));
        this.renderFaceYNeg((double)x, (double)y, (double)z);
        this.renderFaceYPos((double)x, (double)y, (double)z);
        this.renderFaceZNeg((double)x, (double)y, (double)z);
        this.renderFaceZPos((double)x, (double)y, (double)z);
        this.renderFaceXNeg((double)x, (double)y, (double)z);
        this.renderFaceXPos((double)x, (double)y, (double)z);

        return true;
    }

    private boolean renderBlockRedstoneWire(Block block, int x, int y, int z)
    {
        Tessellator tess = Tessellator.instance;
        
        Tessellator.instance.setColorOpaque_I(block.colorMultiplier(this.world, x, y, z));
        
        tess.addVertex((double)(x + 1), (double)y + 0.01D, (double)(z + 1));
        tess.addVertex((double)(x + 1), (double)y + 0.01D, (double)(z + 0));
        tess.addVertex((double)(x + 0), (double)y + 0.01D, (double)(z + 0));
        tess.addVertex((double)(x + 0), (double)y + 0.01D, (double)(z + 1));

        if (!this.world.getBlock(x, y + 1, z).isSolid())
        {
            if (this.world.getBlock(x - 1, y, z).isSolid() && this.world.getBlock(x - 1, y + 1, z) == Block.redstone_wire)
            {
                tess.addVertex((double)x + 0.01D, (double)(y + 1), (double)(z + 1));
                tess.addVertex((double)x + 0.01D, (double)(y + 0), (double)(z + 1));
                tess.addVertex((double)x + 0.01D, (double)(y + 0), (double)(z + 0));
                tess.addVertex((double)x + 0.01D, (double)(y + 1), (double)(z + 0));
            }

            if (this.world.getBlock(x + 1, y, z).isSolid() && this.world.getBlock(x + 1, y + 1, z) == Block.redstone_wire)
            {
                tess.addVertex((double)(x + 1) - 0.01D, (double)(y + 0), (double)(z + 1));
                tess.addVertex((double)(x + 1) - 0.01D, (double)(y + 1), (double)(z + 1));
                tess.addVertex((double)(x + 1) - 0.01D, (double)(y + 1), (double)(z + 0));
                tess.addVertex((double)(x + 1) - 0.01D, (double)(y + 0), (double)(z + 0));
            }

            if (this.world.getBlock(x, y, z - 1).isSolid() && this.world.getBlock(x, y + 1, z - 1) == Block.redstone_wire)
            {
                tess.addVertex((double)(x + 1), (double)(y + 0), (double)z + 0.01D);
                tess.addVertex((double)(x + 1), (double)(y + 1), (double)z + 0.01D);
                tess.addVertex((double)(x + 0), (double)(y + 1), (double)z + 0.01D);
                tess.addVertex((double)(x + 0), (double)(y + 0), (double)z + 0.01D);
            }

            if (this.world.getBlock(x, y, z + 1).isSolid() && this.world.getBlock(x, y + 1, z + 1) == Block.redstone_wire)
            {
                tess.addVertex((double)(x + 1), (double)(y + 1), (double)(z + 1) - 0.01D);
                tess.addVertex((double)(x + 1), (double)(y + 0), (double)(z + 1) - 0.01D);
                tess.addVertex((double)(x + 0), (double)(y + 0), (double)(z + 1) - 0.01D);
                tess.addVertex((double)(x + 0), (double)(y + 1), (double)(z + 1) - 0.01D);
            }
        }

        return true;
    }

    private boolean renderStandardBlock(int x, int y, int z)
    {
        boolean var9 = false;
        
        int shift = ((x + y + z)%2 + 2)%2 == 0 ? 0x060000 : 0x000006;

    	Tessellator.instance.setColorOpaque_I(0x404040 + shift);
        if (!this.world.getBlock(x, y - 1, z).isSolid())
        {
            this.renderFaceYNeg((double)x, (double)y, (double)z);
            var9 = true;
        }

    	Tessellator.instance.setColorOpaque_I(0x606060 + shift);
        if (!this.world.getBlock(x, y + 1, z).isSolid())
        {
            this.renderFaceYPos((double)x, (double)y, (double)z);
            var9 = true;
        }

    	Tessellator.instance.setColorOpaque_I(0x505050 + shift);
        if (!this.world.getBlock(x, y, z - 1).isSolid())
        {
            this.renderFaceZNeg((double)x, (double)y, (double)z);
            var9 = true;
        }

        if (!this.world.getBlock(x, y, z + 1).isSolid())
        {
            this.renderFaceZPos((double)x, (double)y, (double)z);
            var9 = true;
        }

        if (!this.world.getBlock(x - 1, y, z).isSolid())
        {
            this.renderFaceXNeg((double)x, (double)y, (double)z);
            var9 = true;
        }

        if (!this.world.getBlock(x + 1, y, z).isSolid())
        {
            this.renderFaceXPos((double)x, (double)y, (double)z);
            var9 = true;
        }

        return var9;
    }

    private void renderFaceYNeg(double p_147768_2_, double p_147768_4_, double p_147768_6_)
    {
        Tessellator var9 = Tessellator.instance;

        double var26 = p_147768_2_ + this.renderMinX;
        double var28 = p_147768_2_ + this.renderMaxX;
        double var30 = p_147768_4_ + this.renderMinY;
        double var32 = p_147768_6_ + this.renderMinZ;
        double var34 = p_147768_6_ + this.renderMaxZ;

        var9.addVertex(var26, var30, var34);
        var9.addVertex(var26, var30, var32);
        var9.addVertex(var28, var30, var32);
        var9.addVertex(var28, var30, var34);
    }

    private void renderFaceYPos(double p_147806_2_, double p_147806_4_, double p_147806_6_)
    {
        Tessellator var9 = Tessellator.instance;

        double var26 = p_147806_2_ + this.renderMinX;
        double var28 = p_147806_2_ + this.renderMaxX;
        double var30 = p_147806_4_ + this.renderMaxY;
        double var32 = p_147806_6_ + this.renderMinZ;
        double var34 = p_147806_6_ + this.renderMaxZ;

        var9.addVertex(var28, var30, var34);
        var9.addVertex(var28, var30, var32);
        var9.addVertex(var26, var30, var32);
        var9.addVertex(var26, var30, var34);
    }

    private void renderFaceZNeg(double p_147761_2_, double p_147761_4_, double p_147761_6_)
    {
        Tessellator var9 = Tessellator.instance;

        double var26 = p_147761_2_ + this.renderMinX;
        double var28 = p_147761_2_ + this.renderMaxX;
        double var30 = p_147761_4_ + this.renderMinY;
        double var32 = p_147761_4_ + this.renderMaxY;
        double var34 = p_147761_6_ + this.renderMinZ;

        var9.addVertex(var26, var32, var34);
        var9.addVertex(var28, var32, var34);
        var9.addVertex(var28, var30, var34);
        var9.addVertex(var26, var30, var34);
    }

    private void renderFaceZPos(double p_147734_2_, double p_147734_4_, double p_147734_6_)
    {
        Tessellator var9 = Tessellator.instance;

        double var26 = p_147734_2_ + this.renderMinX;
        double var28 = p_147734_2_ + this.renderMaxX;
        double var30 = p_147734_4_ + this.renderMinY;
        double var32 = p_147734_4_ + this.renderMaxY;
        double var34 = p_147734_6_ + this.renderMaxZ;

        var9.addVertex(var26, var32, var34);
        var9.addVertex(var26, var30, var34);
        var9.addVertex(var28, var30, var34);
        var9.addVertex(var28, var32, var34);
    }

    private void renderFaceXNeg(double p_147798_2_, double p_147798_4_, double p_147798_6_)
    {
        Tessellator var9 = Tessellator.instance;

        double var26 = p_147798_2_ + this.renderMinX;
        double var28 = p_147798_4_ + this.renderMinY;
        double var30 = p_147798_4_ + this.renderMaxY;
        double var32 = p_147798_6_ + this.renderMinZ;
        double var34 = p_147798_6_ + this.renderMaxZ;

        var9.addVertex(var26, var30, var34);
        var9.addVertex(var26, var30, var32);
        var9.addVertex(var26, var28, var32);
        var9.addVertex(var26, var28, var34);
    }

    private void renderFaceXPos(double p_147764_2_, double p_147764_4_, double p_147764_6_)
    {
        Tessellator var9 = Tessellator.instance;

        double var26 = p_147764_2_ + this.renderMaxX;
        double var28 = p_147764_4_ + this.renderMinY;
        double var30 = p_147764_4_ + this.renderMaxY;
        double var32 = p_147764_6_ + this.renderMinZ;
        double var34 = p_147764_6_ + this.renderMaxZ;

        var9.addVertex(var26, var28, var34);
        var9.addVertex(var26, var28, var32);
        var9.addVertex(var26, var30, var32);
        var9.addVertex(var26, var30, var34);
    }
}
