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
    private World theWorld;
    private final List<WorldRenderer> worldRenderersToUpdate = new ArrayList<WorldRenderer>();
    private WorldRenderer[] sortedWorldRenderers;
    private WorldRenderer[] worldRenderers;
    private int renderChunksWide;
    private int renderChunksTall;
    private int renderChunksDeep;

    /** OpenGL render lists base */
    private final int glRenderListBase;

    /** Minimum block X */
    private int minBlockX;

    /** Minimum block Y */
    private int minBlockY;

    /** Minimum block Z */
    private int minBlockZ;

    /** Maximum block X */
    private int maxBlockX;

    /** Maximum block Y */
    private int maxBlockY;

    /** Maximum block Z */
    private int maxBlockZ;

    /** World renderers check index */
    private int worldRenderersCheckIndex;

    /** List of OpenGL lists for the current render pass */
    private final List<WorldRenderer> glRenderLists = new ArrayList<WorldRenderer>();

    /** All render lists (fixed length 4) */
    private final RenderList[] allRenderLists = new RenderList[] {new RenderList(), new RenderList(), new RenderList(), new RenderList()};

    /**
     * Previous x position when the renderers were sorted. (Once the distance moves more than 4 units they will be
     * resorted)
     */
    private double prevSortX = -9999.0D;

    /**
     * Previous y position when the renderers were sorted. (Once the distance moves more than 4 units they will be
     * resorted)
     */
    private double prevSortY = -9999.0D;

    /**
     * Previous Z position when the renderers were sorted. (Once the distance moves more than 4 units they will be
     * resorted)
     */
    private double prevSortZ = -9999.0D;
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
        this.prevSortX = -9999.0D;
        this.prevSortY = -9999.0D;
        this.prevSortZ = -9999.0D;
        this.prevChunkSortX = -9999;
        this.prevChunkSortY = -9999;
        this.prevChunkSortZ = -9999;
        this.theWorld = world;
        
        if (this.worldRenderers != null)
        {
            for (int i = 0; i < this.worldRenderers.length; ++i)
            {
                this.worldRenderers[i].stopRendering();
            }
        }

        this.renderChunksWide = 33;
        this.renderChunksTall = 16;
        this.renderChunksDeep = 33;
        this.worldRenderers = new WorldRenderer[this.renderChunksWide * this.renderChunksTall * this.renderChunksDeep];
        this.sortedWorldRenderers = new WorldRenderer[this.renderChunksWide * this.renderChunksTall * this.renderChunksDeep];
        this.minBlockX = 0;
        this.minBlockY = 0;
        this.minBlockZ = 0;
        this.maxBlockX = this.renderChunksWide;
        this.maxBlockY = this.renderChunksTall;
        this.maxBlockZ = this.renderChunksDeep;

        for (int i = 0; i < this.worldRenderersToUpdate.size(); ++i)
        {
            this.worldRenderersToUpdate.get(i).needsUpdate = false;
        }

        this.worldRenderersToUpdate.clear();
        
        int var2 = 0;
        int var3 = 0;

        for (int x = 0; x < this.renderChunksWide; ++x)
        {
            for (int y = 0; y < this.renderChunksTall; ++y)
            {
                for (int z = 0; z < this.renderChunksDeep; ++z)
                {
                    this.worldRenderers[(z * this.renderChunksTall + y) * this.renderChunksWide + x] = new WorldRenderer(this.theWorld, x * 16, y * 16, z * 16, this.glRenderListBase + var2);
                    this.worldRenderers[(z * this.renderChunksTall + y) * this.renderChunksWide + x].setInFrustrum();
                    this.worldRenderers[(z * this.renderChunksTall + y) * this.renderChunksWide + x].chunkIndex = var3++;
                    this.worldRenderers[(z * this.renderChunksTall + y) * this.renderChunksWide + x].markDirty();
                    this.sortedWorldRenderers[(z * this.renderChunksTall + y) * this.renderChunksWide + x] = this.worldRenderers[(z * this.renderChunksTall + y) * this.renderChunksWide + x];
                    this.worldRenderersToUpdate.add(this.worldRenderers[(z * this.renderChunksTall + y) * this.renderChunksWide + x]);
                    var2 += 3;
                }
            }
        }
    }

    /**
     * Goes through all the renderers setting new positions on them and those that have their position changed are
     * adding to be updated
     */
    private void markRenderersForNewPosition(int p_72722_1_, int p_72722_2_, int p_72722_3_)
    {
        p_72722_1_ -= 8;
        p_72722_2_ -= 8;
        p_72722_3_ -= 8;
        this.minBlockX = Integer.MAX_VALUE;
        this.minBlockY = Integer.MAX_VALUE;
        this.minBlockZ = Integer.MAX_VALUE;
        this.maxBlockX = Integer.MIN_VALUE;
        this.maxBlockY = Integer.MIN_VALUE;
        this.maxBlockZ = Integer.MIN_VALUE;
        int var4 = this.renderChunksWide * 16;
        int var5 = var4 / 2;

        for (int var6 = 0; var6 < this.renderChunksWide; ++var6)
        {
            int var7 = var6 * 16;
            int var8 = var7 + var5 - p_72722_1_;

            if (var8 < 0)
            {
                var8 -= var4 - 1;
            }

            var8 /= var4;
            var7 -= var8 * var4;

            if (var7 < this.minBlockX)
            {
                this.minBlockX = var7;
            }

            if (var7 > this.maxBlockX)
            {
                this.maxBlockX = var7;
            }

            for (int var9 = 0; var9 < this.renderChunksDeep; ++var9)
            {
                int var10 = var9 * 16;
                int var11 = var10 + var5 - p_72722_3_;

                if (var11 < 0)
                {
                    var11 -= var4 - 1;
                }

                var11 /= var4;
                var10 -= var11 * var4;

                if (var10 < this.minBlockZ)
                {
                    this.minBlockZ = var10;
                }

                if (var10 > this.maxBlockZ)
                {
                    this.maxBlockZ = var10;
                }

                for (int var12 = 0; var12 < this.renderChunksTall; ++var12)
                {
                    int var13 = var12 * 16;

                    if (var13 < this.minBlockY)
                    {
                        this.minBlockY = var13;
                    }

                    if (var13 > this.maxBlockY)
                    {
                        this.maxBlockY = var13;
                    }

                    WorldRenderer var14 = this.worldRenderers[(var9 * this.renderChunksTall + var12) * this.renderChunksWide + var6];
                    boolean var15 = var14.needsUpdate;
                    var14.setPosition(var7, var13, var10);

                    if (!var15 && var14.needsUpdate)
                    {
                        this.worldRenderersToUpdate.add(var14);
                    }
                }
            }
        }
    }

    /**
     * Sorts all renderers based on the passed in entity. Args: entityLiving, ppos
     */
    public void sortAndRender(EntityPlayer player, Vec3 ppos)
    {
        for (int var5 = 0; var5 < 10; ++var5)
        {
            this.worldRenderersCheckIndex = (this.worldRenderersCheckIndex + 1) % this.worldRenderers.length;
            WorldRenderer var6 = this.worldRenderers[this.worldRenderersCheckIndex];

            if (var6.needsUpdate && !this.worldRenderersToUpdate.contains(var6))
            {
                this.worldRenderersToUpdate.add(var6);
            }
        }
        
        double var11 = player.getPosX() - this.prevSortX;
        double var13 = player.getPosY() - this.prevSortY;
        double var15 = player.getPosZ() - this.prevSortZ;

        if (this.prevChunkSortX != player.getChunkCoordX() || this.prevChunkSortY != player.getChunkCoordY() || this.prevChunkSortZ != player.getChunkCoordZ() || var11 * var11 + var13 * var13 + var15 * var15 > 16.0D)
        {
            this.prevSortX = player.getPosX();
            this.prevSortY = player.getPosY();
            this.prevSortZ = player.getPosZ();
            this.prevChunkSortX = player.getChunkCoordX();
            this.prevChunkSortY = player.getChunkCoordY();
            this.prevChunkSortZ = player.getChunkCoordZ();
            this.markRenderersForNewPosition(MathHelper.floor_double(player.getPosX()), MathHelper.floor_double(player.getPosY()), MathHelper.floor_double(player.getPosZ()));
        }
        
        this.glRenderLists.clear();

        for (WorldRenderer renderer : this.sortedWorldRenderers)
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
            WorldRenderer renderer = (WorldRenderer)this.glRenderLists.get(i);
            int whichList = -1;

            for (int j = 0; j < nextList; ++j)
            {
                if (this.allRenderLists[j].rendersChunk(renderer.posXMinus, renderer.posYMinus, renderer.posZMinus))
                {
                    whichList = j;
                }
            }

            if (whichList < 0)
            {
                whichList = nextList++;
                this.allRenderLists[whichList].setupRenderList(renderer.posXMinus, renderer.posYMinus, renderer.posZMinus, ppos.x, ppos.y, ppos.z);
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
            int var14 = x % this.renderChunksWide;

            if (var14 < 0)
            {
                var14 += this.renderChunksWide;
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
                    int var18 = z % this.renderChunksDeep;

                    if (var18 < 0)
                    {
                        var18 += this.renderChunksDeep;
                    }

                    int var19 = (var18 * this.renderChunksTall + var16) * this.renderChunksWide + var14;
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
