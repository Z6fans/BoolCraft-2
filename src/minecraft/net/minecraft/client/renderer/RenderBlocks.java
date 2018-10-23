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

    private void setRenderBounds(double minx, double miny, double minz, double maxx, double maxy, double maxz)
    {
    	this.renderMinX = minx;
        this.renderMaxX = maxx;
        this.renderMinY = miny;
        this.renderMaxY = maxy;
        this.renderMinZ = minz;
        this.renderMaxZ = maxz;
    }
    
    public void setRenderBoundsFromAABB(AxisAlignedBB aabb){
    	this.renderMinX = aabb.minX;
        this.renderMaxX = aabb.maxX;
        this.renderMinY = aabb.minY;
        this.renderMaxY = aabb.maxY;
        this.renderMinZ = aabb.minZ;
        this.renderMaxZ = aabb.maxZ;
    }

    public boolean renderBlockByRenderType(Block p_147805_1_, int p_147805_2_, int p_147805_3_, int p_147805_4_)
    {
        int var5 = p_147805_1_.getRenderType();

        if (var5 == -1)
        {
            return false;
        }
        else
        {
        	Tessellator.instance.setColorOpaque_I(p_147805_1_.colorMultiplier(this.chunkCache, p_147805_2_, p_147805_3_, p_147805_4_));
            this.setRenderBoundsFromAABB(p_147805_1_.generateCubicBoundingBox(0, 0, 0));
            return var5 == 0 ? this.renderStandardBlock(p_147805_1_, p_147805_2_, p_147805_3_, p_147805_4_, false) : (var5 == 5 ? this.renderBlockRedstoneWire(p_147805_1_, p_147805_2_, p_147805_3_, p_147805_4_) : (var5 == 12 ? this.renderBlockLever(p_147805_1_, p_147805_2_, p_147805_3_, p_147805_4_) : false));
        }
    }

    private boolean renderBlockLever(Block p_147790_1_, int p_147790_2_, int p_147790_3_, int p_147790_4_)
    {
        int var6 = this.chunkCache.getBlockMetadata(p_147790_2_, p_147790_3_, p_147790_4_) & 7;

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
        
        this.renderStandardBlock(p_147790_1_, p_147790_2_, p_147790_3_, p_147790_4_, true);
        return true;
    }

    private boolean renderBlockRedstoneWire(Block p_147788_1_, int p_147788_2_, int p_147788_3_, int p_147788_4_)
    {
        Tessellator var5 = Tessellator.instance;
        boolean var19 = BlockRedstoneWire.shouldConnect(this.chunkCache, p_147788_2_ - 1, p_147788_3_, p_147788_4_, true) || !this.chunkCache.getBlock(p_147788_2_ - 1, p_147788_3_, p_147788_4_).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, p_147788_2_ - 1, p_147788_3_ - 1, p_147788_4_, false);
        boolean var20 = BlockRedstoneWire.shouldConnect(this.chunkCache, p_147788_2_ + 1, p_147788_3_, p_147788_4_, true) || !this.chunkCache.getBlock(p_147788_2_ + 1, p_147788_3_, p_147788_4_).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, p_147788_2_ + 1, p_147788_3_ - 1, p_147788_4_, false);
        boolean var21 = BlockRedstoneWire.shouldConnect(this.chunkCache, p_147788_2_, p_147788_3_, p_147788_4_ - 1, true) || !this.chunkCache.getBlock(p_147788_2_, p_147788_3_, p_147788_4_ - 1).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, p_147788_2_, p_147788_3_ - 1, p_147788_4_ - 1, false);
        boolean var22 = BlockRedstoneWire.shouldConnect(this.chunkCache, p_147788_2_, p_147788_3_, p_147788_4_ + 1, true) || !this.chunkCache.getBlock(p_147788_2_, p_147788_3_, p_147788_4_ + 1).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, p_147788_2_, p_147788_3_ - 1, p_147788_4_ + 1, false);

        if (!this.chunkCache.getBlock(p_147788_2_, p_147788_3_ + 1, p_147788_4_).isSolid())
        {
            if (this.chunkCache.getBlock(p_147788_2_ - 1, p_147788_3_, p_147788_4_).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, p_147788_2_ - 1, p_147788_3_ + 1, p_147788_4_, false))
            {
                var19 = true;
            }

            if (this.chunkCache.getBlock(p_147788_2_ + 1, p_147788_3_, p_147788_4_).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, p_147788_2_ + 1, p_147788_3_ + 1, p_147788_4_, false))
            {
                var20 = true;
            }

            if (this.chunkCache.getBlock(p_147788_2_, p_147788_3_, p_147788_4_ - 1).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, p_147788_2_, p_147788_3_ + 1, p_147788_4_ - 1, false))
            {
                var21 = true;
            }

            if (this.chunkCache.getBlock(p_147788_2_, p_147788_3_, p_147788_4_ + 1).isSolid() && BlockRedstoneWire.shouldConnect(this.chunkCache, p_147788_2_, p_147788_3_ + 1, p_147788_4_ + 1, false))
            {
                var22 = true;
            }
        }

        float var23 = (float)(p_147788_2_ + 0);
        float var24 = (float)(p_147788_2_ + 1);
        float var25 = (float)(p_147788_4_ + 0);
        float var26 = (float)(p_147788_4_ + 1);

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

        var5.addVertex((double)var24, (double)p_147788_3_ + 0.015625D, (double)var26);
        var5.addVertex((double)var24, (double)p_147788_3_ + 0.015625D, (double)var25);
        var5.addVertex((double)var23, (double)p_147788_3_ + 0.015625D, (double)var25);
        var5.addVertex((double)var23, (double)p_147788_3_ + 0.015625D, (double)var26);

        if (!this.chunkCache.getBlock(p_147788_2_, p_147788_3_ + 1, p_147788_4_).isSolid())
        {
            if (this.chunkCache.getBlock(p_147788_2_ - 1, p_147788_3_, p_147788_4_).isSolid() && this.chunkCache.getBlock(p_147788_2_ - 1, p_147788_3_ + 1, p_147788_4_) == Block.redstone_wire)
            {
                var5.addVertex((double)p_147788_2_ + 0.015625D, (double)((float)(p_147788_3_ + 1) + 0.021875F), (double)(p_147788_4_ + 1 - 0.3125F));
                var5.addVertex((double)p_147788_2_ + 0.015625D, (double)(p_147788_3_ + 0), (double)(p_147788_4_ + 1 - 0.3125F));
                var5.addVertex((double)p_147788_2_ + 0.015625D, (double)(p_147788_3_ + 0), (double)(p_147788_4_ + 0.3125F));
                var5.addVertex((double)p_147788_2_ + 0.015625D, (double)((float)(p_147788_3_ + 1) + 0.021875F), (double)(p_147788_4_ + 0.3125F));
            }

            if (this.chunkCache.getBlock(p_147788_2_ + 1, p_147788_3_, p_147788_4_).isSolid() && this.chunkCache.getBlock(p_147788_2_ + 1, p_147788_3_ + 1, p_147788_4_) == Block.redstone_wire)
            {
                var5.addVertex((double)(p_147788_2_ + 1) - 0.015625D, (double)(p_147788_3_ + 0), (double)(p_147788_4_ + 1 - 0.3125F));
                var5.addVertex((double)(p_147788_2_ + 1) - 0.015625D, (double)((float)(p_147788_3_ + 1) + 0.021875F), (double)(p_147788_4_ + 1 - 0.3125F));
                var5.addVertex((double)(p_147788_2_ + 1) - 0.015625D, (double)((float)(p_147788_3_ + 1) + 0.021875F), (double)(p_147788_4_ + 0.3125F));
                var5.addVertex((double)(p_147788_2_ + 1) - 0.015625D, (double)(p_147788_3_ + 0), (double)(p_147788_4_ + 0.3125F));
            }

            if (this.chunkCache.getBlock(p_147788_2_, p_147788_3_, p_147788_4_ - 1).isSolid() && this.chunkCache.getBlock(p_147788_2_, p_147788_3_ + 1, p_147788_4_ - 1) == Block.redstone_wire)
            {
                var5.addVertex((double)(p_147788_2_ + 1 - 0.3125F), (double)(p_147788_3_ + 0), (double)p_147788_4_ + 0.015625D);
                var5.addVertex((double)(p_147788_2_ + 1 - 0.3125F), (double)((float)(p_147788_3_ + 1) + 0.021875F), (double)p_147788_4_ + 0.015625D);
                var5.addVertex((double)(p_147788_2_ + 0.3125F), (double)((float)(p_147788_3_ + 1) + 0.021875F), (double)p_147788_4_ + 0.015625D);
                var5.addVertex((double)(p_147788_2_ + 0.3125F), (double)(p_147788_3_ + 0), (double)p_147788_4_ + 0.015625D);
            }

            if (this.chunkCache.getBlock(p_147788_2_, p_147788_3_, p_147788_4_ + 1).isSolid() && this.chunkCache.getBlock(p_147788_2_, p_147788_3_ + 1, p_147788_4_ + 1) == Block.redstone_wire)
            {
                var5.addVertex((double)(p_147788_2_ + 1 - 0.3125F), (double)((float)(p_147788_3_ + 1) + 0.021875F), (double)(p_147788_4_ + 1) - 0.015625D);
                var5.addVertex((double)(p_147788_2_ + 1 - 0.3125F), (double)(p_147788_3_ + 0), (double)(p_147788_4_ + 1) - 0.015625D);
                var5.addVertex((double)(p_147788_2_ + 0.3125F), (double)(p_147788_3_ + 0), (double)(p_147788_4_ + 1) - 0.015625D);
                var5.addVertex((double)(p_147788_2_ + 0.3125F), (double)((float)(p_147788_3_ + 1) + 0.021875F), (double)(p_147788_4_ + 1) - 0.015625D);
            }
        }

        return true;
    }

    private boolean renderStandardBlock(Block p_147736_1_, int p_147736_2_, int p_147736_3_, int p_147736_4_, boolean renderAllFaces)
    {
        boolean var9 = false;

        if (renderAllFaces || p_147736_1_.shouldSideBeRendered(this.chunkCache, p_147736_2_, p_147736_3_ - 1, p_147736_4_, 0))
        {
            this.renderFaceYNeg(p_147736_1_, (double)p_147736_2_, (double)p_147736_3_, (double)p_147736_4_);
            var9 = true;
        }

        if (renderAllFaces || p_147736_1_.shouldSideBeRendered(this.chunkCache, p_147736_2_, p_147736_3_ + 1, p_147736_4_, 1))
        {
            this.renderFaceYPos(p_147736_1_, (double)p_147736_2_, (double)p_147736_3_, (double)p_147736_4_);
            var9 = true;
        }

        if (renderAllFaces || p_147736_1_.shouldSideBeRendered(this.chunkCache, p_147736_2_, p_147736_3_, p_147736_4_ - 1, 2))
        {
            this.renderFaceZNeg(p_147736_1_, (double)p_147736_2_, (double)p_147736_3_, (double)p_147736_4_);
            var9 = true;
        }

        if (renderAllFaces || p_147736_1_.shouldSideBeRendered(this.chunkCache, p_147736_2_, p_147736_3_, p_147736_4_ + 1, 3))
        {
            this.renderFaceZPos(p_147736_1_, (double)p_147736_2_, (double)p_147736_3_, (double)p_147736_4_);
            var9 = true;
        }

        if (renderAllFaces || p_147736_1_.shouldSideBeRendered(this.chunkCache, p_147736_2_ - 1, p_147736_3_, p_147736_4_, 4))
        {
            this.renderFaceXNeg(p_147736_1_, (double)p_147736_2_, (double)p_147736_3_, (double)p_147736_4_);
            var9 = true;
        }

        if (renderAllFaces || p_147736_1_.shouldSideBeRendered(this.chunkCache, p_147736_2_ + 1, p_147736_3_, p_147736_4_, 5))
        {
            this.renderFaceXPos(p_147736_1_, (double)p_147736_2_, (double)p_147736_3_, (double)p_147736_4_);
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
