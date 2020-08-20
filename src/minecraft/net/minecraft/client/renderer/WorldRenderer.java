package net.minecraft.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.EntityPlayer;
import net.minecraft.client.WorldClient;
import net.minecraft.util.AxisAlignedBB;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

public class WorldRenderer
{
    private TesselatorVertexState vertexState;

    /** Reference to the World object. */
    private WorldClient worldObj;
    private final int glRenderList;
    private final static Tessellator tessellator = Tessellator.instance;
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
    public final boolean[] skipRenderPass = new boolean[2];

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
    
    private final float[][] frustum = new float[16][16];
    private final float[] projectionMatrix = new float[16];
    private final float[] modelviewMatrix = new float[16];
    private final float[] clippingMatrix = new float[16];
    private final FloatBuffer projectionMatrixBuffer = GLAllocation.createDirectFloatBuffer(16);
    private final FloatBuffer modelviewMatrixBuffer = GLAllocation.createDirectFloatBuffer(16);

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
            float off = 6.0F;
            this.rendererBoundingBox = AxisAlignedBB.getBoundingBox((double)((float)x - off), (double)((float)y - off), (double)((float)z - off), (double)((float)(x + 16) + off), (double)((float)(y + 16) + off), (double)((float)(z + 16) + off));
            GL11.glNewList(this.glRenderList + 2, GL11.GL_COMPILE);
            AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox((double)((float)this.posXClip - off), (double)((float)this.posYClip - off), (double)((float)this.posZClip - off), (double)((float)(this.posXClip + 16) + off), (double)((float)(this.posYClip + 16) + off), (double)((float)(this.posZClip + 16) + off));
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
    public void updateRenderer(EntityPlayer player)
    {
        if (this.needsUpdate)
        {
            this.needsUpdate = false;

            for (int i = 0; i < 2; ++i)
            {
                this.skipRenderPass[i] = true;
            }

            RenderBlocks renderBlocks = new RenderBlocks(this.worldObj);
            this.vertexState = null;
            boolean doRenderPass = false;
            boolean doPostRenderBlocks = false;

            for (int y = this.posY; y < this.posY + 16; ++y)
            {
                for (int z = this.posZ; z < this.posZ + 16; ++z)
                {
                    for (int x = this.posX; x < this.posX + 16; ++x)
                    {
                        Block block = this.worldObj.getBlock(x, y, z);

                        if (!block.isReplaceable())
                        {
                            if (!doPostRenderBlocks)
                            {
                                doPostRenderBlocks = true;
                                this.preRenderBlocks(0);
                            }

                            doRenderPass |= renderBlocks.renderBlockByRenderType(block, x, y, z);
                        }
                    }
                }
            }

            if (doRenderPass)
            {
                this.skipRenderPass[0] = false;
            }

            if (doPostRenderBlocks)
            {
                this.postRenderBlocks(0, player);
            }
            else
            {
                doRenderPass = false;
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

    private void postRenderBlocks(int p_147891_1_, EntityPlayer p_147891_2_)
    {
        if (p_147891_1_ == 1 && !this.skipRenderPass[p_147891_1_])
        {
            this.vertexState = tessellator.getVertexState((float)p_147891_2_.getPosX(), (float)p_147891_2_.getPosY(), (float)p_147891_2_.getPosZ());
        }

        tessellator.draw();
        GL11.glPopMatrix();
        GL11.glEndList();
        tessellator.setTranslation(0.0D, 0.0D, 0.0D);
    }

    public void updateRendererSort(EntityPlayer p_147889_1_)
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
    public float quadranceToPlayer(EntityPlayer player)
    {
        float var2 = (float)(player.getPosX() - (double)this.posXPlus);
        float var3 = (float)(player.getPosY() - (double)this.posYPlus);
        float var4 = (float)(player.getPosZ() - (double)this.posZPlus);
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

    public void updateInFrustum(double x, double y, double z)
    {
    	this.initFrustrum();
        this.isInFrustum = this.isBoxInFrustum(this.rendererBoundingBox.minX - x, this.rendererBoundingBox.minY - y, this.rendererBoundingBox.minZ - z, this.rendererBoundingBox.maxX - x, this.rendererBoundingBox.maxY - y, this.rendererBoundingBox.maxZ - z);
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
    
    /**
     * Returns true if the box is inside all 6 clipping planes, otherwise returns false.
     */
    private boolean isBoxInFrustum(double p_78553_1_, double p_78553_3_, double p_78553_5_, double p_78553_7_, double p_78553_9_, double p_78553_11_)
    {
        for (int i = 0; i < 6; ++i)
        {
            if ((double)this.frustum[i][0] * p_78553_1_ + (double)this.frustum[i][1] * p_78553_3_ + (double)this.frustum[i][2] * p_78553_5_ + (double)this.frustum[i][3] <= 0.0D && (double)this.frustum[i][0] * p_78553_7_ + (double)this.frustum[i][1] * p_78553_3_ + (double)this.frustum[i][2] * p_78553_5_ + (double)this.frustum[i][3] <= 0.0D && (double)this.frustum[i][0] * p_78553_1_ + (double)this.frustum[i][1] * p_78553_9_ + (double)this.frustum[i][2] * p_78553_5_ + (double)this.frustum[i][3] <= 0.0D && (double)this.frustum[i][0] * p_78553_7_ + (double)this.frustum[i][1] * p_78553_9_ + (double)this.frustum[i][2] * p_78553_5_ + (double)this.frustum[i][3] <= 0.0D && (double)this.frustum[i][0] * p_78553_1_ + (double)this.frustum[i][1] * p_78553_3_ + (double)this.frustum[i][2] * p_78553_11_ + (double)this.frustum[i][3] <= 0.0D && (double)this.frustum[i][0] * p_78553_7_ + (double)this.frustum[i][1] * p_78553_3_ + (double)this.frustum[i][2] * p_78553_11_ + (double)this.frustum[i][3] <= 0.0D && (double)this.frustum[i][0] * p_78553_1_ + (double)this.frustum[i][1] * p_78553_9_ + (double)this.frustum[i][2] * p_78553_11_ + (double)this.frustum[i][3] <= 0.0D && (double)this.frustum[i][0] * p_78553_7_ + (double)this.frustum[i][1] * p_78553_9_ + (double)this.frustum[i][2] * p_78553_11_ + (double)this.frustum[i][3] <= 0.0D)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Normalize the frustum.
     */
    private void normalizeFrustrum(int i)
    {
        float norm = (float)Math.sqrt(this.frustum[i][0] * this.frustum[i][0] + this.frustum[i][1] * this.frustum[i][1] + this.frustum[i][2] * this.frustum[i][2]);
        this.frustum[i][0] /= norm;
        this.frustum[i][1] /= norm;
        this.frustum[i][2] /= norm;
        this.frustum[i][3] /= norm;
    }

    private void initFrustrum()
    {
        this.projectionMatrixBuffer.clear();
        this.modelviewMatrixBuffer.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, this.projectionMatrixBuffer);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, this.modelviewMatrixBuffer);
        this.projectionMatrixBuffer.flip().limit(16);
        this.projectionMatrixBuffer.get(this.projectionMatrix);
        this.modelviewMatrixBuffer.flip().limit(16);
        this.modelviewMatrixBuffer.get(this.modelviewMatrix);
        this.clippingMatrix[0] = this.modelviewMatrix[0] * this.projectionMatrix[0] + this.modelviewMatrix[1] * this.projectionMatrix[4] + this.modelviewMatrix[2] * this.projectionMatrix[8] + this.modelviewMatrix[3] * this.projectionMatrix[12];
        this.clippingMatrix[1] = this.modelviewMatrix[0] * this.projectionMatrix[1] + this.modelviewMatrix[1] * this.projectionMatrix[5] + this.modelviewMatrix[2] * this.projectionMatrix[9] + this.modelviewMatrix[3] * this.projectionMatrix[13];
        this.clippingMatrix[2] = this.modelviewMatrix[0] * this.projectionMatrix[2] + this.modelviewMatrix[1] * this.projectionMatrix[6] + this.modelviewMatrix[2] * this.projectionMatrix[10] + this.modelviewMatrix[3] * this.projectionMatrix[14];
        this.clippingMatrix[3] = this.modelviewMatrix[0] * this.projectionMatrix[3] + this.modelviewMatrix[1] * this.projectionMatrix[7] + this.modelviewMatrix[2] * this.projectionMatrix[11] + this.modelviewMatrix[3] * this.projectionMatrix[15];
        this.clippingMatrix[4] = this.modelviewMatrix[4] * this.projectionMatrix[0] + this.modelviewMatrix[5] * this.projectionMatrix[4] + this.modelviewMatrix[6] * this.projectionMatrix[8] + this.modelviewMatrix[7] * this.projectionMatrix[12];
        this.clippingMatrix[5] = this.modelviewMatrix[4] * this.projectionMatrix[1] + this.modelviewMatrix[5] * this.projectionMatrix[5] + this.modelviewMatrix[6] * this.projectionMatrix[9] + this.modelviewMatrix[7] * this.projectionMatrix[13];
        this.clippingMatrix[6] = this.modelviewMatrix[4] * this.projectionMatrix[2] + this.modelviewMatrix[5] * this.projectionMatrix[6] + this.modelviewMatrix[6] * this.projectionMatrix[10] + this.modelviewMatrix[7] * this.projectionMatrix[14];
        this.clippingMatrix[7] = this.modelviewMatrix[4] * this.projectionMatrix[3] + this.modelviewMatrix[5] * this.projectionMatrix[7] + this.modelviewMatrix[6] * this.projectionMatrix[11] + this.modelviewMatrix[7] * this.projectionMatrix[15];
        this.clippingMatrix[8] = this.modelviewMatrix[8] * this.projectionMatrix[0] + this.modelviewMatrix[9] * this.projectionMatrix[4] + this.modelviewMatrix[10] * this.projectionMatrix[8] + this.modelviewMatrix[11] * this.projectionMatrix[12];
        this.clippingMatrix[9] = this.modelviewMatrix[8] * this.projectionMatrix[1] + this.modelviewMatrix[9] * this.projectionMatrix[5] + this.modelviewMatrix[10] * this.projectionMatrix[9] + this.modelviewMatrix[11] * this.projectionMatrix[13];
        this.clippingMatrix[10] = this.modelviewMatrix[8] * this.projectionMatrix[2] + this.modelviewMatrix[9] * this.projectionMatrix[6] + this.modelviewMatrix[10] * this.projectionMatrix[10] + this.modelviewMatrix[11] * this.projectionMatrix[14];
        this.clippingMatrix[11] = this.modelviewMatrix[8] * this.projectionMatrix[3] + this.modelviewMatrix[9] * this.projectionMatrix[7] + this.modelviewMatrix[10] * this.projectionMatrix[11] + this.modelviewMatrix[11] * this.projectionMatrix[15];
        this.clippingMatrix[12] = this.modelviewMatrix[12] * this.projectionMatrix[0] + this.modelviewMatrix[13] * this.projectionMatrix[4] + this.modelviewMatrix[14] * this.projectionMatrix[8] + this.modelviewMatrix[15] * this.projectionMatrix[12];
        this.clippingMatrix[13] = this.modelviewMatrix[12] * this.projectionMatrix[1] + this.modelviewMatrix[13] * this.projectionMatrix[5] + this.modelviewMatrix[14] * this.projectionMatrix[9] + this.modelviewMatrix[15] * this.projectionMatrix[13];
        this.clippingMatrix[14] = this.modelviewMatrix[12] * this.projectionMatrix[2] + this.modelviewMatrix[13] * this.projectionMatrix[6] + this.modelviewMatrix[14] * this.projectionMatrix[10] + this.modelviewMatrix[15] * this.projectionMatrix[14];
        this.clippingMatrix[15] = this.modelviewMatrix[12] * this.projectionMatrix[3] + this.modelviewMatrix[13] * this.projectionMatrix[7] + this.modelviewMatrix[14] * this.projectionMatrix[11] + this.modelviewMatrix[15] * this.projectionMatrix[15];
        this.frustum[0][0] = this.clippingMatrix[3] - this.clippingMatrix[0];
        this.frustum[0][1] = this.clippingMatrix[7] - this.clippingMatrix[4];
        this.frustum[0][2] = this.clippingMatrix[11] - this.clippingMatrix[8];
        this.frustum[0][3] = this.clippingMatrix[15] - this.clippingMatrix[12];
        this.normalizeFrustrum(0);
        this.frustum[1][0] = this.clippingMatrix[3] + this.clippingMatrix[0];
        this.frustum[1][1] = this.clippingMatrix[7] + this.clippingMatrix[4];
        this.frustum[1][2] = this.clippingMatrix[11] + this.clippingMatrix[8];
        this.frustum[1][3] = this.clippingMatrix[15] + this.clippingMatrix[12];
        this.normalizeFrustrum(1);
        this.frustum[2][0] = this.clippingMatrix[3] + this.clippingMatrix[1];
        this.frustum[2][1] = this.clippingMatrix[7] + this.clippingMatrix[5];
        this.frustum[2][2] = this.clippingMatrix[11] + this.clippingMatrix[9];
        this.frustum[2][3] = this.clippingMatrix[15] + this.clippingMatrix[13];
        this.normalizeFrustrum(2);
        this.frustum[3][0] = this.clippingMatrix[3] - this.clippingMatrix[1];
        this.frustum[3][1] = this.clippingMatrix[7] - this.clippingMatrix[5];
        this.frustum[3][2] = this.clippingMatrix[11] - this.clippingMatrix[9];
        this.frustum[3][3] = this.clippingMatrix[15] - this.clippingMatrix[13];
        this.normalizeFrustrum(3);
        this.frustum[4][0] = this.clippingMatrix[3] - this.clippingMatrix[2];
        this.frustum[4][1] = this.clippingMatrix[7] - this.clippingMatrix[6];
        this.frustum[4][2] = this.clippingMatrix[11] - this.clippingMatrix[10];
        this.frustum[4][3] = this.clippingMatrix[15] - this.clippingMatrix[14];
        this.normalizeFrustrum(4);
        this.frustum[5][0] = this.clippingMatrix[3] + this.clippingMatrix[2];
        this.frustum[5][1] = this.clippingMatrix[7] + this.clippingMatrix[6];
        this.frustum[5][2] = this.clippingMatrix[11] + this.clippingMatrix[10];
        this.frustum[5][3] = this.clippingMatrix[15] + this.clippingMatrix[14];
        this.normalizeFrustrum(5);
    }
}
