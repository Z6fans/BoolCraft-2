package net.minecraft.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.EntityPlayer;
import net.minecraft.client.WorldClient;
import net.minecraft.util.AxisAlignedBB;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

public class WorldRenderer
{
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
    private boolean isInFrustrum;

    /** Should this renderer skip this render pass */
    private boolean skipRenderPass = true;

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
            this.rendererBoundingBox = AxisAlignedBB.getBoundingBox(x - 6, y - 6, z - 6, x + 22, y + 22, z + 22);
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

            this.skipRenderPass = true;

            RenderBlocks renderBlocks = new RenderBlocks(this.worldObj);
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
                                GL11.glNewList(this.glRenderList, GL11.GL_COMPILE);
                                GL11.glPushMatrix();
                                GL11.glTranslatef((float)this.posXClip, (float)this.posYClip, (float)this.posZClip);
                                float var2 = 1.000001F;
                                GL11.glTranslatef(-8.0F, -8.0F, -8.0F);
                                GL11.glScalef(var2, var2, var2);
                                GL11.glTranslatef(8.0F, 8.0F, 8.0F);
                                tessellator.startDrawing(7);
                                tessellator.setTranslation((double)(-this.posX), (double)(-this.posY), (double)(-this.posZ));
                            }

                            doRenderPass |= renderBlocks.renderBlockByRenderType(block, x, y, z);
                        }
                    }
                }
            }

            if (doRenderPass)
            {
                this.skipRenderPass = false;
            }

            if (doPostRenderBlocks)
            {
            	tessellator.draw();
                GL11.glPopMatrix();
                GL11.glEndList();
                tessellator.setTranslation(0.0D, 0.0D, 0.0D);
            }
            else
            {
                doRenderPass = false;
            }
            
            this.isInitialized = true;
        }
    }

    /**
     * Returns the distance of this chunk renderer to the entity without performing the final normalizing square root,
     * for performance reasons.
     */
    public float quadranceToPlayer(EntityPlayer player)
    {
        float dx = (float)(player.getPosX() - (double)this.posXPlus);
        float dy = (float)(player.getPosY() - (double)this.posYPlus);
        float dz = (float)(player.getPosZ() - (double)this.posZPlus);
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * When called this renderer won't draw anymore until its gets initialized again
     */
    private void setDontDraw()
    {
        this.skipRenderPass = true;
        this.isInFrustrum = false;
        this.isInitialized = false;
    }

    public void stopRendering()
    {
        this.setDontDraw();
        this.worldObj = null;
    }
    
    public int getGLCallList()
    {
    	return this.isInFrustrum && !this.skipRenderPass ? this.glRenderList : -1;
    }

    public void updateInFrustum(double x, double y, double z)
    {
    	this.projectionMatrixBuffer.clear();
        this.modelviewMatrixBuffer.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, this.projectionMatrixBuffer);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, this.modelviewMatrixBuffer);
        this.projectionMatrixBuffer.flip().limit(16);
        this.projectionMatrixBuffer.get(this.projectionMatrix);
        this.modelviewMatrixBuffer.flip().limit(16);
        this.modelviewMatrixBuffer.get(this.modelviewMatrix);
        this.clippingMatrix[ 0] = this.modelviewMatrix[ 0] * this.projectionMatrix[0] + this.modelviewMatrix[ 1] * this.projectionMatrix[4] + this.modelviewMatrix[2] * this.projectionMatrix[8] + this.modelviewMatrix[3] * this.projectionMatrix[12];
        this.clippingMatrix[ 1] = this.modelviewMatrix[ 0] * this.projectionMatrix[1] + this.modelviewMatrix[ 1] * this.projectionMatrix[5] + this.modelviewMatrix[2] * this.projectionMatrix[9] + this.modelviewMatrix[3] * this.projectionMatrix[13];
        this.clippingMatrix[ 2] = this.modelviewMatrix[ 0] * this.projectionMatrix[2] + this.modelviewMatrix[ 1] * this.projectionMatrix[6] + this.modelviewMatrix[2] * this.projectionMatrix[10] + this.modelviewMatrix[3] * this.projectionMatrix[14];
        this.clippingMatrix[ 3] = this.modelviewMatrix[ 0] * this.projectionMatrix[3] + this.modelviewMatrix[ 1] * this.projectionMatrix[7] + this.modelviewMatrix[2] * this.projectionMatrix[11] + this.modelviewMatrix[3] * this.projectionMatrix[15];
        this.clippingMatrix[ 4] = this.modelviewMatrix[ 4] * this.projectionMatrix[0] + this.modelviewMatrix[ 5] * this.projectionMatrix[4] + this.modelviewMatrix[6] * this.projectionMatrix[8] + this.modelviewMatrix[7] * this.projectionMatrix[12];
        this.clippingMatrix[ 5] = this.modelviewMatrix[ 4] * this.projectionMatrix[1] + this.modelviewMatrix[ 5] * this.projectionMatrix[5] + this.modelviewMatrix[6] * this.projectionMatrix[9] + this.modelviewMatrix[7] * this.projectionMatrix[13];
        this.clippingMatrix[ 6] = this.modelviewMatrix[ 4] * this.projectionMatrix[2] + this.modelviewMatrix[ 5] * this.projectionMatrix[6] + this.modelviewMatrix[6] * this.projectionMatrix[10] + this.modelviewMatrix[7] * this.projectionMatrix[14];
        this.clippingMatrix[ 7] = this.modelviewMatrix[ 4] * this.projectionMatrix[3] + this.modelviewMatrix[ 5] * this.projectionMatrix[7] + this.modelviewMatrix[6] * this.projectionMatrix[11] + this.modelviewMatrix[7] * this.projectionMatrix[15];
        this.clippingMatrix[ 8] = this.modelviewMatrix[ 8] * this.projectionMatrix[0] + this.modelviewMatrix[ 9] * this.projectionMatrix[4] + this.modelviewMatrix[10] * this.projectionMatrix[8] + this.modelviewMatrix[11] * this.projectionMatrix[12];
        this.clippingMatrix[ 9] = this.modelviewMatrix[ 8] * this.projectionMatrix[1] + this.modelviewMatrix[ 9] * this.projectionMatrix[5] + this.modelviewMatrix[10] * this.projectionMatrix[9] + this.modelviewMatrix[11] * this.projectionMatrix[13];
        this.clippingMatrix[10] = this.modelviewMatrix[ 8] * this.projectionMatrix[2] + this.modelviewMatrix[ 9] * this.projectionMatrix[6] + this.modelviewMatrix[10] * this.projectionMatrix[10] + this.modelviewMatrix[11] * this.projectionMatrix[14];
        this.clippingMatrix[11] = this.modelviewMatrix[ 8] * this.projectionMatrix[3] + this.modelviewMatrix[ 9] * this.projectionMatrix[7] + this.modelviewMatrix[10] * this.projectionMatrix[11] + this.modelviewMatrix[11] * this.projectionMatrix[15];
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
        
        double minX = this.rendererBoundingBox.minX - x;
        double minY = this.rendererBoundingBox.minY - y;
        double minZ = this.rendererBoundingBox.minZ - z;
        double maxX = this.rendererBoundingBox.maxX - x;
        double maxY = this.rendererBoundingBox.maxY - y;
        double maxZ = this.rendererBoundingBox.maxZ - z;
        
        this.isInFrustrum = true;
    	
        for (int i = 0; i < 6; ++i)
        {
        	this.isInFrustrum &= !((double)this.frustum[i][0] * minX + (double)this.frustum[i][1] * minY + (double)this.frustum[i][2] * minZ + (double)this.frustum[i][3] <= 0.0D && (double)this.frustum[i][0] * maxX + (double)this.frustum[i][1] * minY + (double)this.frustum[i][2] * minZ + (double)this.frustum[i][3] <= 0.0D && (double)this.frustum[i][0] * minX + (double)this.frustum[i][1] * maxY + (double)this.frustum[i][2] * minZ + (double)this.frustum[i][3] <= 0.0D && (double)this.frustum[i][0] * maxX + (double)this.frustum[i][1] * maxY + (double)this.frustum[i][2] * minZ + (double)this.frustum[i][3] <= 0.0D && (double)this.frustum[i][0] * minX + (double)this.frustum[i][1] * minY + (double)this.frustum[i][2] * maxZ + (double)this.frustum[i][3] <= 0.0D && (double)this.frustum[i][0] * maxX + (double)this.frustum[i][1] * minY + (double)this.frustum[i][2] * maxZ + (double)this.frustum[i][3] <= 0.0D && (double)this.frustum[i][0] * minX + (double)this.frustum[i][1] * maxY + (double)this.frustum[i][2] * maxZ + (double)this.frustum[i][3] <= 0.0D && (double)this.frustum[i][0] * maxX + (double)this.frustum[i][1] * maxY + (double)this.frustum[i][2] * maxZ + (double)this.frustum[i][3] <= 0.0D);
        }
    }

    /**
     * Checks if all render passes are to be skipped. Returns false if the renderer is not initialized
     */
    public boolean skipAllRenderPasses()
    {
        return this.isInitialized && this.skipRenderPass;
    }

    /**
     * Marks the current renderer data as dirty and needing to be updated.
     */
    public void markDirty()
    {
        this.needsUpdate = true;
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
    
    public boolean getInFrustrum()
    {
    	return this.isInFrustrum;
    }
    
    public void setInFrustrum()
    {
    	this.isInFrustrum = true;
    }
    
    public boolean shouldSkip()
    {
    	return this.skipRenderPass;
    }
}
