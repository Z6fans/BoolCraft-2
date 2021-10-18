package net.minecraft.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.util.AxisAlignedBB;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

public class WorldRenderer
{
    /** Reference to the World object. */
    private World world;
    private final int glRenderList;
    private final static Tesselator tessellator = Tesselator.instance;
    private int posX;
    private int posY;
    private int posZ;

    /** Pos X minus */
    public int posXMinus;

    /** Pos Z minus */
    public int posZMinus;
    private boolean inFrustum;

    /** Should this renderer skip this render pass */
    private boolean skipRenderPass = true;

    /** Boolean for whether this renderer needs to be updated or not */
    public boolean needsUpdate;

    /** Chunk index */
    public int chunkIndex;
    private boolean isInitialized;
    
    private final double[][] frustum = new double[16][16];
    private final float[] proj = new float[16];
    private final float[] model = new float[16];
    private final FloatBuffer projBuffer = GLAllocation.createDirectFloatBuffer(16);
    private final FloatBuffer modelBuffer = GLAllocation.createDirectFloatBuffer(16);

    public WorldRenderer(World world, int cx, int cy, int cz, int renderList, int ci)
    {
        this.world = world;
        this.glRenderList = renderList;
        this.posX = -999;
        this.setPosition(cx * 16, cy * 16, cz * 16);
        this.needsUpdate = false;
    	this.inFrustum = true;
    	this.chunkIndex = ci;
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
            this.posXMinus = x - (x & 1023);
            this.posZMinus = z - (z & 1023);
            this.markDirty();
        }
    }

    /**
     * Will update this chunk renderer
     */
    public void updateRenderer()
    {
        if (this.needsUpdate)
        {
            this.needsUpdate = false;
            GL11.glNewList(this.glRenderList, GL11.GL_COMPILE);
            GL11.glPushMatrix();
            GL11.glTranslatef((float)(this.posX & 1023), (float)this.posY, (float)(this.posZ & 1023));
            tessellator.startDrawing();
            tessellator.setTranslation(-this.posX, -this.posY, -this.posZ);

            for (int y = this.posY; y < this.posY + 16; ++y) {
                for (int z = this.posZ; z < this.posZ + 16; ++z) {
                    for (int x = this.posX; x < this.posX + 16; ++x) {
                    	int bm = this.world.getBlocMeta(x, y, z);
                    	int block = bm & 0xF;
                    	int meta = bm >> 4;
                    	
                        AxisAlignedBB aabb = Block.getBlockById(block).generateCubicBoundingBox(meta);
                        
                    	Tesselator tess = Tesselator.instance;
                        
                        if (block == 2) {
                        	tess.setColor_I(getColor(block, meta, x, y, z, 0));
                            
                            tess.addVertex(x + 1, y + 0.01, z + 1);
                            tess.addVertex(x + 1, y + 0.01, z + 0);
                            tess.addVertex(x + 0, y + 0.01, z + 0);
                            tess.addVertex(x + 0, y + 0.01, z + 1);

                            if (!this.world.isSolid(x, y + 1, z))
                            {
                                if (this.world.isWire(x - 1, y + 1, z))
                                {
                                    tess.addVertex(x + 0.01, y + 1, z + 1);
                                    tess.addVertex(x + 0.01, y + 0, z + 1);
                                    tess.addVertex(x + 0.01, y + 0, z + 0);
                                    tess.addVertex(x + 0.01, y + 1, z + 0);
                                }

                                if (this.world.isWire(x + 1, y + 1, z))
                                {
                                    tess.addVertex(x + 0.99, y + 0, z + 1);
                                    tess.addVertex(x + 0.99, y + 1, z + 1);
                                    tess.addVertex(x + 0.99, y + 1, z + 0);
                                    tess.addVertex(x + 0.99, y + 0, z + 0);
                                }

                                if (this.world.isWire(x, y + 1, z - 1))
                                {
                                    tess.addVertex(x + 1, y + 0, z + 0.01);
                                    tess.addVertex(x + 1, y + 1, z + 0.01);
                                    tess.addVertex(x + 0, y + 1, z + 0.01);
                                    tess.addVertex(x + 0, y + 0, z + 0.01);
                                }

                                if (this.world.isWire(x, y + 1, z + 1))
                                {
                                    tess.addVertex(x + 1, y + 1, z + 0.99);
                                    tess.addVertex(x + 1, y + 0, z + 0.99);
                                    tess.addVertex(x + 0, y + 0, z + 0.99);
                                    tess.addVertex(x + 0, y + 1, z + 0.99);
                                }
                            }
                        } else if (block == 1 || block == 3 || block == 4) {
                        	double xmin = x + aabb.minX;
                            double xmax = x + aabb.maxX;
                            double ymin = y + aabb.minY;
                            double ymax = y + aabb.maxY;
                            double zmin = z + aabb.minZ;
                            double zmax = z + aabb.maxZ;

                            tess.setColor_I(getColor(block, meta, x, y, z, 2));
                            if (!this.world.isSolid(x, y - 1, z) || aabb.minY > 0.0D)
                            {
                            	tess.addVertex(xmin, ymin, zmax);
                                tess.addVertex(xmin, ymin, zmin);
                                tess.addVertex(xmax, ymin, zmin);
                                tess.addVertex(xmax, ymin, zmax);
                            }

                            tess.setColor_I(getColor(block, meta, x, y, z, 0));
                            if (!this.world.isSolid(x, y + 1, z) || aabb.maxY < 1.0D)
                            {
                            	tess.addVertex(xmax, ymax, zmax);
                                tess.addVertex(xmax, ymax, zmin);
                                tess.addVertex(xmin, ymax, zmin);
                                tess.addVertex(xmin, ymax, zmax);
                            }

                            tess.setColor_I(getColor(block, meta, x, y, z, 1));
                            if (!this.world.isSolid(x, y, z - 1) || aabb.minZ > 0.0D)
                            {
                            	tess.addVertex(xmin, ymax, zmin);
                                tess.addVertex(xmax, ymax, zmin);
                                tess.addVertex(xmax, ymin, zmin);
                                tess.addVertex(xmin, ymin, zmin);
                            }

                            if (!this.world.isSolid(x, y, z + 1) || aabb.maxZ < 1.0D)
                            {
                            	tess.addVertex(xmin, ymax, zmax);
                                tess.addVertex(xmin, ymin, zmax);
                                tess.addVertex(xmax, ymin, zmax);
                                tess.addVertex(xmax, ymax, zmax);
                            }

                            if (!this.world.isSolid(x - 1, y, z) || aabb.minX > 0.0D)
                            {
                            	tess.addVertex(xmin, ymax, zmax);
                                tess.addVertex(xmin, ymax, zmin);
                                tess.addVertex(xmin, ymin, zmin);
                                tess.addVertex(xmin, ymin, zmax);
                            }

                            if (!this.world.isSolid(x + 1, y, z) || aabb.maxX < 1.0D)
                            {
                            	tess.addVertex(xmax, ymin, zmax);
                                tess.addVertex(xmax, ymin, zmin);
                                tess.addVertex(xmax, ymax, zmin);
                                tess.addVertex(xmax, ymax, zmax);
                            }
                        }
                    }
                }
            }
            
            this.skipRenderPass = false;

            tessellator.draw();
            GL11.glPopMatrix();
            GL11.glEndList();
            tessellator.setTranslation(0.0D, 0.0D, 0.0D);
            
            this.isInitialized = true;
        }
    }
    
    private static int getColor(int block, int meta, int x, int y, int z, int said)
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

    /**
     * Returns the distance of this chunk renderer to the entity without performing the final normalizing square root,
     * for performance reasons.
     */
    public float quadranceToPlayer(EntityPlayer player)
    {
        float dx = (float)(player.getPosX() - (double)this.posX - 8D);
        float dy = (float)(player.getPosY() - (double)this.posY - 8D);
        float dz = (float)(player.getPosZ() - (double)this.posZ - 8D);
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * When called this renderer won't draw anymore until its gets initialized again
     */
    public void setDontDraw()
    {
        this.skipRenderPass = true;
        this.inFrustum = false;
        this.isInitialized = false;
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
        
        double[] xs = {this.posX - 6 - x, this.posX + 22 - x};
        double[] ys = {this.posY - 6 - y, this.posY + 22 - y};
        double[] zs = {this.posZ - 6 - z, this.posZ + 22 - z};
        
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
    
    public boolean shouldSkip()
    {
    	return this.skipRenderPass;
    }
}
