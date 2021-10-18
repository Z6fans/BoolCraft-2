package net.minecraft.client.renderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class RenderGlobal
{
    private final List<WorldRenderer> worldRenderersToUpdate = new ArrayList<WorldRenderer>();
    private WorldRenderer[] worldRenderers;
    private final int rrad = 16;
    private final int renderChunksXZ = this.rrad * 2 + 1;
    private final int renderChunksTall = 16;

    /** OpenGL render lists base */
    private final int glRenderListBase;

    /** World renderers check index */
    private int worldRenderersCheckIndex;

    /** List of OpenGL lists for the current render pass */
    private final List<WorldRenderer> glRenderLists = new ArrayList<WorldRenderer>();

    /** All render lists (fixed length 4) */
    private final RenderList[] allRenderLists = new RenderList[] {new RenderList(), new RenderList(), new RenderList(), new RenderList()};

    private int prevChunkSortX = -999;
    private int prevChunkSortY = -999;
    private int prevChunkSortZ = -999;

    /**
     * The offset used to determine if a renderer is one of the sixteenth that are being updated this frame
     */
    private int frustumCheckOffset;

    public RenderGlobal()
    {
        this.glRenderListBase = GLAllocation.generateDisplayLists(34 * 34 * 16 * 3);
    }

    /**
     * set null to clear
     */
    public void setWorldAndLoadRenderers(World world)
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

        for (int i = 0; i < this.worldRenderersToUpdate.size(); ++i)
        {
            this.worldRenderersToUpdate.get(i).needsUpdate = false;
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
                    this.worldRenderers[(z * this.renderChunksTall + y) * this.renderChunksXZ + x] = new WorldRenderer(world, x, y, z, this.glRenderListBase + var2, chunkIndex++);
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

            if (wr.needsUpdate && !this.worldRenderersToUpdate.contains(wr))
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
                        
                        int bx = cx * 16 - (dx / (this.renderChunksXZ * 16)) * this.renderChunksXZ * 16;
                        int bz = cz * 16 - (dz / (this.renderChunksXZ * 16)) * this.renderChunksXZ * 16;
                        
                        WorldRenderer var14 = this.worldRenderers[(cz * this.renderChunksTall + cy) * this.renderChunksXZ + cx];
                        boolean var15 = var14.needsUpdate;
                        var14.setPosition(bx, cy * 16, bz);

                        if (!var15 && var14.needsUpdate)
                        {
                            this.worldRenderersToUpdate.add(var14);
                        }
                    }
                }
            }
        }
        
        this.glRenderLists.clear();

        for (WorldRenderer renderer : this.worldRenderers)
        {
        	if (!renderer.shouldSkip() && renderer.getInFrustrum())
            {
                if (renderer.getGLCallList() >= 0)
                {
                    this.glRenderLists.add(renderer);
                }
            }
        }
        
        int nextList = 0;

        for (int i = 0; i < this.allRenderLists.length; ++i)
        {
            this.allRenderLists[i].resetList();
        }

        for (int i = 0; i < this.glRenderLists.size(); ++i)
        {
            WorldRenderer renderer = this.glRenderLists.get(i);
            int whichList = -1;

            for (int j = 0; j < nextList; ++j)
            {
                if (this.allRenderLists[j].rendersChunk(renderer.posXMinus, renderer.posZMinus))
                {
                    whichList = j;
                }
            }

            if (whichList < 0)
            {
                whichList = nextList++;
                this.allRenderLists[whichList].setupRenderList(renderer.posXMinus, renderer.posZMinus, ppos.x, ppos.y, ppos.z);
            }

            this.allRenderLists[whichList].addGLRenderList(renderer.getGLCallList());
        }

        for (int i = 0; i < this.allRenderLists.length; ++i)
        {
            this.allRenderLists[i].callLists();
        }
    }

    /**
     * Updates some of the renderers sorted by distance from the player
     */
    public void updateRenderers(EntityPlayer player, Vec3 ppos)
    {
    	for (int i = 0; i < this.worldRenderers.length; ++i)
        {
            if (!this.worldRenderers[i].skipAllRenderPasses() && (!this.worldRenderers[i].getInFrustrum() || (i + this.frustumCheckOffset & 15) == 0))
            {
                this.worldRenderers[i].updateInFrustum(ppos.x, ppos.y, ppos.z);
            }
        }

        ++this.frustumCheckOffset;
        
        RenderSorter rs = new RenderSorter(player);
        WorldRenderer rendererArray = null;
        ArrayList<WorldRenderer> rendererList = null;
        int initialSize = this.worldRenderersToUpdate.size();

        for (int i = 0; i < initialSize; ++i)
        {
        	WorldRenderer wr = this.worldRenderersToUpdate.get(i);

            if (wr != null)
            {
            	if (wr.quadranceToPlayer(player) > 272.0F)
                {
            		if (rendererArray == null || rs.compare(rendererArray, wr) <= 0)
                    	rendererArray = wr;
                    continue;
                }

                if (rendererList == null)
                {
                    rendererList = new ArrayList<WorldRenderer>();
                }

                rendererList.add(wr);
                this.worldRenderersToUpdate.set(i, null);
            }
        }

        if (rendererList != null)
        {
            if (rendererList.size() > 1)
            {
                Collections.sort(rendererList, rs);
            }

            for (int i = rendererList.size() - 1; i >= 0; --i)
            {
            	WorldRenderer wr = rendererList.get(i);
                wr.updateRenderer();
                wr.needsUpdate = false;
            }
        }

    	if (!rendererArray.getInFrustrum())
        {
            rendererArray = null;
        }
    	else
    	{
    		rendererArray.updateRenderer();
            rendererArray.needsUpdate = false;
    	}
        
        int i = 0;
        int var11 = 0;

        for (int var12 = this.worldRenderersToUpdate.size(); i != var12; ++i)
        {
            WorldRenderer wr = (WorldRenderer)this.worldRenderersToUpdate.get(i);

            if (wr != null)
            {
                if (!(wr == null || wr == rendererArray))
                {
                    if (var11 != i)
                    {
                        this.worldRenderersToUpdate.set(var11, wr);
                    }

                    ++var11;
                }
            }
        }
        
        --i;
        
        while (i >= var11)
        {
        	this.worldRenderersToUpdate.remove(i);
        	--i;
        }
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

                    int var19 = (var18 * this.renderChunksTall + var16) * this.renderChunksXZ + var14;
                    WorldRenderer renderer = this.worldRenderers[var19];

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
        GLAllocation.deleteDisplayLists(this.glRenderListBase);
    }
    
    private class RenderSorter implements Comparator<WorldRenderer>
    {
        /** The entity (usually the player) that the camera is inside. */
        private final EntityPlayer baseEntity;

        private RenderSorter(EntityPlayer entity)
        {
            this.baseEntity = entity;
        }

        public int compare(WorldRenderer wr1, WorldRenderer wr2)
        {
            if (wr1.getInFrustrum() && !wr2.getInFrustrum())
            {
                return 1;
            }
            else if (wr2.getInFrustrum() && !wr1.getInFrustrum())
            {
                return -1;
            }
            else
            {
                double quad1 = (double)wr1.quadranceToPlayer(this.baseEntity);
                double quad2 = (double)wr2.quadranceToPlayer(this.baseEntity);
                return quad1 < quad2 ? 1 : (quad1 > quad2 ? -1 : (wr1.chunkIndex < wr2.chunkIndex ? 1 : -1));
            }
        }
    }
}
