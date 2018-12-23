package net.minecraft.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.player.EntityPlayerSP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.ChunkCache;

import org.lwjgl.opengl.GL11;

public class WorldRenderer
{
    private TesselatorVertexState vertexState;

    /** Reference to the World object. */
    private WorldClient worldObj;
    private int glRenderList = -1;
    private static Tessellator tessellator = Tessellator.instance;
    private int posX;
    private int posY;
    private int posZ;

    /** Pos X minus */
    public int posXMinus;

    /** Pos Y minus */
    public int posYMinus;

    /** Pos Z minus */
    public int posZMinus;

    /** Pos X clipped */
    private int posXClip;

    /** Pos Y clipped */
    private int posYClip;

    /** Pos Z clipped */
    private int posZClip;
    public boolean isInFrustum;

    /** Should this renderer skip this render pass */
    public boolean[] skipRenderPass = new boolean[2];

    /** Pos X plus */
    private int posXPlus;

    /** Pos Y plus */
    private int posYPlus;

    /** Pos Z plus */
    private int posZPlus;

    /** Boolean for whether this renderer needs to be updated or not */
    public boolean needsUpdate;

    /** Axis aligned bounding box */
    private AxisAlignedBB rendererBoundingBox;

    /** Chunk index */
    public int chunkIndex;
    private boolean isInitialized;

    public WorldRenderer(WorldClient world, int x, int y, int z, int renderList)
    {
        this.worldObj = world;
        this.vertexState = null;
        this.glRenderList = renderList;
        this.posX = -999;
        this.setPosition(x, y, z);
        this.needsUpdate = false;
    }

    /**
     * Sets a new position for the renderer and setting it up so it can be reloaded with the new data for that position
     */
    public void setPosition(int x, int y, int z)
    {
        if (x != this.posX || y != this.posY || z != this.posZ)
        {
            this.setDontDraw();
            this.posX = x;
            this.posY = y;
            this.posZ = z;
            this.posXPlus = x + 8;
            this.posYPlus = y + 8;
            this.posZPlus = z + 8;
            this.posXClip = x & 1023;
            this.posYClip = y;
            this.posZClip = z & 1023;
            this.posXMinus = x - this.posXClip;
            this.posYMinus = y - this.posYClip;
            this.posZMinus = z - this.posZClip;
            float var4 = 6.0F;
            this.rendererBoundingBox = AxisAlignedBB.getBoundingBox((double)((float)x - var4), (double)((float)y - var4), (double)((float)z - var4), (double)((float)(x + 16) + var4), (double)((float)(y + 16) + var4), (double)((float)(z + 16) + var4));
            GL11.glNewList(this.glRenderList + 2, GL11.GL_COMPILE);
            AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox((double)((float)this.posXClip - var4), (double)((float)this.posYClip - var4), (double)((float)this.posZClip - var4), (double)((float)(this.posXClip + 16) + var4), (double)((float)(this.posYClip + 16) + var4), (double)((float)(this.posZClip + 16) + var4));
            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
            tess.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
            tess.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
            tess.addVertex(aabb.minX, aabb.minY, aabb.minZ);
            tess.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
            tess.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
            tess.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
            tess.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
            tess.addVertex(aabb.minX, aabb.minY, aabb.minZ);
            tess.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
            tess.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
            tess.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
            tess.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
            tess.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
            tess.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
            tess.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
            tess.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
            tess.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
            tess.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
            tess.addVertex(aabb.minX, aabb.minY, aabb.minZ);
            tess.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
            tess.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
            tess.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
            tess.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
            tess.draw();
            GL11.glEndList();
            this.markDirty();
        }
    }

    /**
     * Will update this chunk renderer
     */
    public void updateRenderer(EntityPlayerSP player)
    {
        if (this.needsUpdate)
        {
            this.needsUpdate = false;
            int xmin = this.posX;
            int ymin = this.posY;
            int zmin = this.posZ;
            int xmax = this.posX + 16;
            int ymax = this.posY + 16;
            int zmax = this.posZ + 16;

            for (int var8 = 0; var8 < 2; ++var8)
            {
                this.skipRenderPass[var8] = true;
            }
            
            ChunkCache cache = new ChunkCache(this.worldObj, xmin - 1, ymin - 1, zmin - 1, xmax + 1, ymax + 1, zmax + 1, 1);

            if (!cache.extendedLevelsInChunkCache())
            {
                RenderBlocks var16 = new RenderBlocks(cache);
                this.vertexState = null;
                boolean var19 = false;
                boolean var20 = false;

                for (int var21 = ymin; var21 < ymax; ++var21)
                {
                    for (int var22 = zmin; var22 < zmax; ++var22)
                    {
                        for (int var23 = xmin; var23 < xmax; ++var23)
                        {
                            Block var24 = cache.getBlock(var23, var21, var22);

                            if (!var24.isReplaceable())
                            {
                                if (!var20)
                                {
                                    var20 = true;
                                    this.preRenderBlocks(0);
                                }

                                var19 |= var16.renderBlockByRenderType(var24, var23, var21, var22);
                            }
                        }
                    }
                }

                if (var19)
                {
                    this.skipRenderPass[0] = false;
                }

                if (var20)
                {
                    this.postRenderBlocks(0, player);
                }
                else
                {
                    var19 = false;
                }
            }
            this.isInitialized = true;
        }
    }

    private void preRenderBlocks(int p_147890_1_)
    {
        GL11.glNewList(this.glRenderList + p_147890_1_, GL11.GL_COMPILE);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)this.posXClip, (float)this.posYClip, (float)this.posZClip);
        float var2 = 1.000001F;
        GL11.glTranslatef(-8.0F, -8.0F, -8.0F);
        GL11.glScalef(var2, var2, var2);
        GL11.glTranslatef(8.0F, 8.0F, 8.0F);
        tessellator.startDrawingQuads();
        tessellator.setTranslation((double)(-this.posX), (double)(-this.posY), (double)(-this.posZ));
    }

    private void postRenderBlocks(int p_147891_1_, EntityPlayerSP p_147891_2_)
    {
        if (p_147891_1_ == 1 && !this.skipRenderPass[p_147891_1_])
        {
            this.vertexState = tessellator.getVertexState((float)p_147891_2_.posX, (float)p_147891_2_.posY, (float)p_147891_2_.posZ);
        }

        tessellator.draw();
        GL11.glPopMatrix();
        GL11.glEndList();
        tessellator.setTranslation(0.0D, 0.0D, 0.0D);
    }

    public void updateRendererSort(EntityPlayerSP p_147889_1_)
    {
        if (this.vertexState != null && !this.skipRenderPass[1])
        {
            this.preRenderBlocks(1);
            tessellator.setVertexState(this.vertexState);
            this.postRenderBlocks(1, p_147889_1_);
        }
    }

    /**
     * Returns the distance of this chunk renderer to the entity without performing the final normalizing square root,
     * for performance reasons.
     */
    public float quadranceToPlayer(EntityPlayerSP player)
    {
        float var2 = (float)(player.posX - (double)this.posXPlus);
        float var3 = (float)(player.posY - (double)this.posYPlus);
        float var4 = (float)(player.posZ - (double)this.posZPlus);
        return var2 * var2 + var3 * var3 + var4 * var4;
    }

    /**
     * When called this renderer won't draw anymore until its gets initialized again
     */
    private void setDontDraw()
    {
        for (int var1 = 0; var1 < 2; ++var1)
        {
            this.skipRenderPass[var1] = true;
        }

        this.isInFrustum = false;
        this.isInitialized = false;
        this.vertexState = null;
    }

    public void stopRendering()
    {
        this.setDontDraw();
        this.worldObj = null;
    }

    /**
     * Takes in the pass the call list is being requested for. Args: renderPass
     */
    public int getGLCallListForPass(int p_78909_1_)
    {
        return !this.isInFrustum ? -1 : (!this.skipRenderPass[p_78909_1_] ? this.glRenderList + p_78909_1_ : -1);
    }

    public void updateInFrustum(Frustrum p_78908_1_)
    {
        this.isInFrustum = p_78908_1_.isBoundingBoxInFrustum(this.rendererBoundingBox);
    }

    /**
     * Checks if all render passes are to be skipped. Returns false if the renderer is not initialized
     */
    public boolean skipAllRenderPasses()
    {
        return !this.isInitialized ? false : this.skipRenderPass[0] && this.skipRenderPass[1];
    }

    /**
     * Marks the current renderer data as dirty and needing to be updated.
     */
    public void markDirty()
    {
        this.needsUpdate = true;
    }
}
