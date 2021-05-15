package net.minecraft.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.EntityPlayer;
import net.minecraft.world.WorldServer;
import net.minecraft.util.AxisAlignedBB;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

public class WorldRenderer
{
    /** Reference to the World object. */
    private WorldServer worldObj;
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
    private boolean inFrustum;

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
    private AxisAlignedBB aabb;

    /** Chunk index */
    public int chunkIndex;
    private boolean isInitialized;
    
    private final double[][] frustum = new double[16][16];
    private final float[] proj = new float[16];
    private final float[] model = new float[16];
    private final FloatBuffer projBuffer = GLAllocation.createDirectFloatBuffer(16);
    private final FloatBuffer modelBuffer = GLAllocation.createDirectFloatBuffer(16);

    public WorldRenderer(WorldServer world, int x, int y, int z, int renderList)
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
            this.aabb = AxisAlignedBB.getBoundingBox(x - 6, y - 6, z - 6, x + 22, y + 22, z + 22);
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
                                tessellator.startDrawing(7);
                                tessellator.setTranslation(-this.posX, -this.posY, -this.posZ);
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
        this.inFrustum = false;
        this.isInitialized = false;
    }

    public void stopRendering()
    {
        this.setDontDraw();
        this.worldObj = null;
    }
    
    public int getGLCallList()
    {
    	return this.inFrustum && !this.skipRenderPass ? this.glRenderList : -1;
    }

    public void updateInFrustum(double x, double y, double z)
    {
    	this.projBuffer.clear();
        this.modelBuffer.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, this.projBuffer);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, this.modelBuffer);
        this.projBuffer.flip().limit(16);
        this.projBuffer.get(this.proj);
        this.modelBuffer.flip().limit(16);
        this.modelBuffer.get(this.model);
        
        double[] clip = new double[16];
        
        for (int i = 0; i < 16; i++) for (int j = 0; j < 4; j++)
        	clip[i] += this.model[(i & 12) + j] * this.proj[(i & 3) + (j << 2)]; //matrix mult
        
        for (int i = 0; i < 6; i++)
        {
        	for (int j = 0; j < 4; j++)
        		this.frustum[i][j] = clip[3 + (j << 2)] + clip[(j << 2) + (i >> 1)] * ((i & 1) == 0 ? -1 : 1);
        	
        	double norm = Math.sqrt(this.frustum[i][0] * this.frustum[i][0]
		                          + this.frustum[i][1] * this.frustum[i][1]
		                          + this.frustum[i][2] * this.frustum[i][2]);
        	
        	for (int j = 0; j < 4; j++) this.frustum[i][j] /= norm;
        }
        
        double[] xs = {this.aabb.minX - x, this.aabb.maxX - x};
        double[] ys = {this.aabb.minY - y, this.aabb.maxY - y};
        double[] zs = {this.aabb.minZ - x, this.aabb.maxZ - z};
        
        this.inFrustum = true;
    	
        for (int i = 0; i < 6; i++)
        {
        	boolean t = false;
        	
        	for (int j = 0; j < 8; j++)
            	t |= this.frustum[i][0] * xs[j & 1]
            	   + this.frustum[i][1] * ys[(j >> 1) & 1]
            	   + this.frustum[i][2] * zs[(j >> 2) & 1]
            	   + this.frustum[i][3] > 0.0D;
            	   
            this.inFrustum &= t;
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
    
    public boolean getInFrustrum()
    {
    	return this.inFrustum;
    }
    
    public void setInFrustrum()
    {
    	this.inFrustum = true;
    }
    
    public boolean shouldSkip()
    {
    	return this.skipRenderPass;
    }
}
