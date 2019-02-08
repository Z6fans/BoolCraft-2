package net.minecraft.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.ChunkCache;

public class RenderBlocks
{
    /** The IBlockAccess used by this instance of RenderBlocks */
    private ChunkCache chunkCache;

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

    public RenderBlocks(ChunkCache p_i1251_1_)
    {
        this.chunkCache = p_i1251_1_;
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
        	Tessellator.instance.setColorOpaque_I(block.colorMultiplier(this.chunkCache, x, y, z));
            AxisAlignedBB aabb = block.generateCubicBoundingBox(0, 0, 0);
        	this.renderMinX = aabb.minX;
            this.renderMaxX = aabb.maxX;
            this.renderMinY = aabb.minY;
            this.renderMaxY = aabb.maxY;
            this.renderMinZ = aabb.minZ;
            this.renderMaxZ = aabb.maxZ;
            return rt == 0 ? this.renderStandardBlock(block, x, y, z, false) : (rt == 5 ? this.renderBlockRedstoneWire(block, x, y, z) : (rt == 12 ? this.renderBlockLever(block, x, y, z) : false));
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
        int var6 = this.chunkCache.getBlockMetadata(x, y, z) & 7;

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
        
        this.renderStandardBlock(block, x, y, z, true);
        return true;
    }

    private boolean renderBlockRedstoneWire(Block block, int x, int y, int z)
    {
        Tessellator var5 = Tessellator.instance;
        boolean var19 = BlockRedstoneWire.shouldConnect(this.chunkCache, x - 1, y, z, true) || !this.chunkCache.getBlock(x - 1, y, z).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, x - 1, y - 1, z, false);
        boolean var20 = BlockRedstoneWire.shouldConnect(this.chunkCache, x + 1, y, z, true) || !this.chunkCache.getBlock(x + 1, y, z).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, x + 1, y - 1, z, false);
        boolean var21 = BlockRedstoneWire.shouldConnect(this.chunkCache, x, y, z - 1, true) || !this.chunkCache.getBlock(x, y, z - 1).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, x, y - 1, z - 1, false);
        boolean var22 = BlockRedstoneWire.shouldConnect(this.chunkCache, x, y, z + 1, true) || !this.chunkCache.getBlock(x, y, z + 1).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, x, y - 1, z + 1, false);

        if (!this.chunkCache.getBlock(x, y + 1, z).isSolid())
        {
            if (this.chunkCache.getBlock(x - 1, y, z).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, x - 1, y + 1, z, false))
            {
                var19 = true;
            }

            if (this.chunkCache.getBlock(x + 1, y, z).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, x + 1, y + 1, z, false))
            {
                var20 = true;
            }

            if (this.chunkCache.getBlock(x, y, z - 1).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, x, y + 1, z - 1, false))
            {
                var21 = true;
            }

            if (this.chunkCache.getBlock(x, y, z + 1).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, x, y + 1, z + 1, false))
            {
                var22 = true;
            }
        }

        float var23 = (float)(x + 0);
        float var24 = (float)(x + 1);
        float var25 = (float)(z + 0);
        float var26 = (float)(z + 1);

        if (!var19)
        {
            var23 += 0.3125F;
        }

        if (!var20)
        {
            var24 -= 0.3125F;
        }

        if (!var21)
        {
            var25 += 0.3125F;
        }

        if (!var22)
        {
            var26 -= 0.3125F;
        }

        var5.addVertex((double)var24, (double)y + 0.015625D, (double)var26);
        var5.addVertex((double)var24, (double)y + 0.015625D, (double)var25);
        var5.addVertex((double)var23, (double)y + 0.015625D, (double)var25);
        var5.addVertex((double)var23, (double)y + 0.015625D, (double)var26);

        if (!this.chunkCache.getBlock(x, y + 1, z).isSolid())
        {
            if (this.chunkCache.getBlock(x - 1, y, z).isSolid() && this.chunkCache.getBlock(x - 1, y + 1, z) == Block.redstone_wire)
            {
                var5.addVertex((double)x + 0.015625D, (double)((float)(y + 1) + 0.021875F), (double)(z + 1 - 0.3125F));
                var5.addVertex((double)x + 0.015625D, (double)(y + 0), (double)(z + 1 - 0.3125F));
                var5.addVertex((double)x + 0.015625D, (double)(y + 0), (double)(z + 0.3125F));
                var5.addVertex((double)x + 0.015625D, (double)((float)(y + 1) + 0.021875F), (double)(z + 0.3125F));
            }

            if (this.chunkCache.getBlock(x + 1, y, z).isSolid() && this.chunkCache.getBlock(x + 1, y + 1, z) == Block.redstone_wire)
            {
                var5.addVertex((double)(x + 1) - 0.015625D, (double)(y + 0), (double)(z + 1 - 0.3125F));
                var5.addVertex((double)(x + 1) - 0.015625D, (double)((float)(y + 1) + 0.021875F), (double)(z + 1 - 0.3125F));
                var5.addVertex((double)(x + 1) - 0.015625D, (double)((float)(y + 1) + 0.021875F), (double)(z + 0.3125F));
                var5.addVertex((double)(x + 1) - 0.015625D, (double)(y + 0), (double)(z + 0.3125F));
            }

            if (this.chunkCache.getBlock(x, y, z - 1).isSolid() && this.chunkCache.getBlock(x, y + 1, z - 1) == Block.redstone_wire)
            {
                var5.addVertex((double)(x + 1 - 0.3125F), (double)(y + 0), (double)z + 0.015625D);
                var5.addVertex((double)(x + 1 - 0.3125F), (double)((float)(y + 1) + 0.021875F), (double)z + 0.015625D);
                var5.addVertex((double)(x + 0.3125F), (double)((float)(y + 1) + 0.021875F), (double)z + 0.015625D);
                var5.addVertex((double)(x + 0.3125F), (double)(y + 0), (double)z + 0.015625D);
            }

            if (this.chunkCache.getBlock(x, y, z + 1).isSolid() && this.chunkCache.getBlock(x, y + 1, z + 1) == Block.redstone_wire)
            {
                var5.addVertex((double)(x + 1 - 0.3125F), (double)((float)(y + 1) + 0.021875F), (double)(z + 1) - 0.015625D);
                var5.addVertex((double)(x + 1 - 0.3125F), (double)(y + 0), (double)(z + 1) - 0.015625D);
                var5.addVertex((double)(x + 0.3125F), (double)(y + 0), (double)(z + 1) - 0.015625D);
                var5.addVertex((double)(x + 0.3125F), (double)((float)(y + 1) + 0.021875F), (double)(z + 1) - 0.015625D);
            }
        }

        return true;
    }

    private boolean renderStandardBlock(Block block, int x, int y, int z, boolean renderAllFaces)
    {
        boolean var9 = false;

        if (renderAllFaces || block.shouldSideBeRendered(this.chunkCache, x, y - 1, z, 0))
        {
            this.renderFaceYNeg(block, (double)x, (double)y, (double)z);
            var9 = true;
        }

        if (renderAllFaces || block.shouldSideBeRendered(this.chunkCache, x, y + 1, z, 1))
        {
            this.renderFaceYPos(block, (double)x, (double)y, (double)z);
            var9 = true;
        }

        if (renderAllFaces || block.shouldSideBeRendered(this.chunkCache, x, y, z - 1, 2))
        {
            this.renderFaceZNeg(block, (double)x, (double)y, (double)z);
            var9 = true;
        }

        if (renderAllFaces || block.shouldSideBeRendered(this.chunkCache, x, y, z + 1, 3))
        {
            this.renderFaceZPos(block, (double)x, (double)y, (double)z);
            var9 = true;
        }

        if (renderAllFaces || block.shouldSideBeRendered(this.chunkCache, x - 1, y, z, 4))
        {
            this.renderFaceXNeg(block, (double)x, (double)y, (double)z);
            var9 = true;
        }

        if (renderAllFaces || block.shouldSideBeRendered(this.chunkCache, x + 1, y, z, 5))
        {
            this.renderFaceXPos(block, (double)x, (double)y, (double)z);
            var9 = true;
        }

        return var9;
    }

    private void renderFaceYNeg(Block p_147768_1_, double p_147768_2_, double p_147768_4_, double p_147768_6_)
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

    private void renderFaceYPos(Block p_147806_1_, double p_147806_2_, double p_147806_4_, double p_147806_6_)
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

    private void renderFaceZNeg(Block p_147761_1_, double p_147761_2_, double p_147761_4_, double p_147761_6_)
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

    private void renderFaceZPos(Block p_147734_1_, double p_147734_2_, double p_147734_4_, double p_147734_6_)
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

    private void renderFaceXNeg(Block p_147798_1_, double p_147798_2_, double p_147798_4_, double p_147798_6_)
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

    private void renderFaceXPos(Block p_147764_1_, double p_147764_2_, double p_147764_4_, double p_147764_6_)
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
