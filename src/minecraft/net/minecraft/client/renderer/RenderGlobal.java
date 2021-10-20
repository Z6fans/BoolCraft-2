package net.minecraft.client.renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class RenderGlobal
{
    private final Set<WorldRenderer> worldRenderersToUpdate = new HashSet<WorldRenderer>();
    private WorldRenderer[] worldRenderers;
    private final int renderChunksXZ = 33;
    private final int renderChunksTall = 16;

    /** OpenGL render lists base */
    private final int glRenderListBase;

    /** World renderers check index */
    private int worldRenderersCheckIndex;

    private int prevChunkSortX = -999;
    private int prevChunkSortY = -999;
    private int prevChunkSortZ = -999;
    
    private final int numLists = 34 * 34 * 16 * 3;

    public RenderGlobal()
    {
        this.glRenderListBase = GL11.glGenLists(this.numLists);
    }

    /**
     * set null to clear
     */
    public void setWorldAndLoadRenderers()
    {
        this.prevChunkSortX = -9999;
        this.prevChunkSortY = -9999;
        this.prevChunkSortZ = -9999;
        
        if (this.worldRenderers != null)
        {
            for (int i = 0; i < this.worldRenderers.length; ++i)
            {
                this.worldRenderers[i].setDontDraw();
            }
        }
        
        this.worldRenderers = new WorldRenderer[this.renderChunksXZ * this.renderChunksTall * this.renderChunksXZ];

        for (WorldRenderer wr : this.worldRenderersToUpdate)
        {
            wr.needsUpdate = false;
        }

        this.worldRenderersToUpdate.clear();
        
        int var2 = 0;
        int chunkIndex = 0;

        for (int x = 0; x < this.renderChunksXZ; ++x)
        {
            for (int y = 0; y < this.renderChunksTall; ++y)
            {
                for (int z = 0; z < this.renderChunksXZ; ++z)
                {
                    this.worldRenderers[(z * this.renderChunksTall + y) * this.renderChunksXZ + x] = new WorldRenderer(x, y, z, this.glRenderListBase + var2, chunkIndex++);
                    this.worldRenderers[(z * this.renderChunksTall + y) * this.renderChunksXZ + x].markDirty();
                    this.worldRenderersToUpdate.add(this.worldRenderers[(z * this.renderChunksTall + y) * this.renderChunksXZ + x]);
                    var2 += 3;
                }
            }
        }
    }

    /**
     * Sorts all renderers based on the passed in entity. Args: entityLiving, ppos
     */
    public void sortAndRender(EntityPlayer player, Vec3 ppos)
    {
        for (int i = 0; i < 10; ++i)
        {
            this.worldRenderersCheckIndex = (this.worldRenderersCheckIndex + 1) % this.worldRenderers.length;
            WorldRenderer wr = this.worldRenderers[this.worldRenderersCheckIndex];

            if (wr.needsUpdate)
            {
                this.worldRenderersToUpdate.add(wr);
            }
        }

        if (this.prevChunkSortX != player.getChunkCoordX() || this.prevChunkSortY != player.getChunkCoordY() || this.prevChunkSortZ != player.getChunkCoordZ())
        {
            this.prevChunkSortX = player.getChunkCoordX();
            this.prevChunkSortY = player.getChunkCoordY();
            this.prevChunkSortZ = player.getChunkCoordZ();

            for (int cx = 0; cx < this.renderChunksXZ; ++cx)
            {
                for (int cz = 0; cz < this.renderChunksXZ; ++cz)
                {
                    for (int cy = 0; cy < this.renderChunksTall; ++cy)
                    {
                        int dx = (cx * 16) + ((this.renderChunksXZ + 1) * 8) - MathHelper.floor_double(player.getPosX());
                        int dz = (cz * 16) + ((this.renderChunksXZ + 1) * 8) - MathHelper.floor_double(player.getPosZ());

                        if (dx < 0)
                        {
                            dx -= (this.renderChunksXZ * 16) - 1;
                        }
                        
                        if (dz < 0)
                        {
                            dz -= (this.renderChunksXZ * 16) - 1;
                        }
                        
                        dx /= 16;
                        dz /= 16;
                        
                        WorldRenderer var14 = this.worldRenderers[(cz * this.renderChunksTall + cy) * this.renderChunksXZ + cx];
                        boolean var15 = var14.needsUpdate;
                        var14.setPosition(cx - (dx / this.renderChunksXZ) * this.renderChunksXZ, cy, cz - (dz / this.renderChunksXZ) * this.renderChunksXZ);

                        if (!var15 && var14.needsUpdate)
                        {
                            this.worldRenderersToUpdate.add(var14);
                        }
                    }
                }
            }
        }
        
        IntBuffer glLists = ByteBuffer.allocateDirect(this.renderChunksXZ * this.renderChunksTall * this.renderChunksXZ * 4)
        		.order(ByteOrder.nativeOrder()).asIntBuffer();
        
        Arrays
		.stream(this.worldRenderers)
		.filter(WorldRenderer::shouldRender)
		.forEach(r->glLists.put(r.getGLCallList()));
        
        glLists.flip();
        
    	GL11.glPushMatrix();
        GL11.glTranslatef(-(float)ppos.x, -(float)ppos.y, -(float)ppos.z);
        GL11.glCallLists(glLists);
        GL11.glPopMatrix();
    }

    /**
     * Updates some of the renderers sorted by distance from the player
     */
    public void updateRenderers(World world, EntityPlayer player, Vec3 ppos)
    {
    	for (int i = 0; i < this.worldRenderers.length; ++i)
        {
            this.worldRenderers[i].setInFrustum();
        }
    	
        WorldRenderer rendererArray = null;
        Set<WorldRenderer> rendererList = new HashSet<WorldRenderer>();

        for (WorldRenderer wr : this.worldRenderersToUpdate)
        {
        	if (wr.quadranceToPlayer(player) > 272.0F)
            {
        		if (rendererArray == null || this.rscompare(rendererArray, wr, player))
        		{
                	rendererArray = wr;
        		}
            }
        	else
        	{
        		rendererList.add(wr);
        	}
        }
        
        this.worldRenderersToUpdate.removeAll(rendererList);
        this.worldRenderersToUpdate.remove(rendererArray);

        for (WorldRenderer wr : rendererList)
        {
        	wr.updateRenderer(world);
            wr.needsUpdate = false;
        }

        rendererArray.updateRenderer(world);
        rendererArray.needsUpdate = false;
    }
    
    private boolean rscompare(WorldRenderer renderArray, WorldRenderer wr, EntityPlayer player)
    {
        float quad1 = renderArray.quadranceToPlayer(player);
        float quad2 = wr.quadranceToPlayer(player);
        
        return quad1 > quad2 || (quad1 == quad2 && renderArray.chunkIndex >= wr.chunkIndex);
    }

    /**
     * Marks the blocks in the given range for update
     */
    public void markChunksForUpdate(int x1, int y1, int z1, int x2, int y2, int z2)
    {
        for (int x = x1; x <= x2; ++x)
        {
            int var14 = x % this.renderChunksXZ;

            if (var14 < 0)
            {
                var14 += this.renderChunksXZ;
            }

            for (int y = y1; y <= y2; ++y)
            {
                int var16 = y % this.renderChunksTall;

                if (var16 < 0)
                {
                    var16 += this.renderChunksTall;
                }

                for (int z = z1; z <= z2; ++z)
                {
                    int var18 = z % this.renderChunksXZ;

                    if (var18 < 0)
                    {
                        var18 += this.renderChunksXZ;
                    }

                    WorldRenderer renderer = this.worldRenderers[(var18 * this.renderChunksTall + var16) * this.renderChunksXZ + var14];

                    if (renderer != null && !renderer.needsUpdate)
                    {
                        this.worldRenderersToUpdate.add(renderer);
                        renderer.markDirty();
                    }
                }
            }
        }
    }

    /**
     * Deletes all display lists
     */
    public void deleteAllDisplayLists()
    {
    	GL11.glDeleteLists(this.glRenderListBase, this.numLists);
    }
}
