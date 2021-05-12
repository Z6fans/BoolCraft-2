package net.minecraft.client.renderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.EntityPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.WorldServer;
import net.minecraft.util.MathHelper;

public class RenderGlobal
{
    private WorldServer theWorld;
    private final List<WorldRenderer> worldRenderersToUpdate = new ArrayList<WorldRenderer>();
    private WorldRenderer[] sortedWorldRenderers;
    private WorldRenderer[] worldRenderers;
    private int renderChunksWide;
    private int renderChunksTall;
    private int renderChunksDeep;

    /** OpenGL render lists base */
    private final int glRenderListBase;

    /** A reference to the Minecraft object. */
    private final Minecraft mc;

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
    private double prevRenderSortX = -9999.0D;
    private double prevRenderSortY = -9999.0D;
    private double prevRenderSortZ = -9999.0D;
    private int prevChunkSortX = -999;
    private int prevChunkSortY = -999;
    private int prevChunkSortZ = -999;

    /**
     * The offset used to determine if a renderer is one of the sixteenth that are being updated this frame
     */
    private int frustumCheckOffset;

    public RenderGlobal(Minecraft minecraft)
    {
        this.mc = minecraft;
        this.glRenderListBase = GLAllocation.generateDisplayLists(34 * 34 * 16 * 3);
    }

    /**
     * set null to clear
     */
    public void setWorldAndLoadRenderers(WorldServer world)
    {
        this.prevSortX = -9999.0D;
        this.prevSortY = -9999.0D;
        this.prevSortZ = -9999.0D;
        this.prevRenderSortX = -9999.0D;
        this.prevRenderSortY = -9999.0D;
        this.prevRenderSortZ = -9999.0D;
        this.prevChunkSortX = -9999;
        this.prevChunkSortY = -9999;
        this.prevChunkSortZ = -9999;
        this.theWorld = world;
        this.loadRenderers();
    }

    /**
     * Loads all the renderers and sets up the basic settings usage
     */
    private void loadRenderers()
    {
        if (this.theWorld != null)
        {
            int var1;

            if (this.worldRenderers != null)
            {
                for (var1 = 0; var1 < this.worldRenderers.length; ++var1)
                {
                    this.worldRenderers[var1].stopRendering();
                }
            }

            var1 = 33;
            this.renderChunksWide = var1;
            this.renderChunksTall = 16;
            this.renderChunksDeep = var1;
            this.worldRenderers = new WorldRenderer[this.renderChunksWide * this.renderChunksTall * this.renderChunksDeep];
            this.sortedWorldRenderers = new WorldRenderer[this.renderChunksWide * this.renderChunksTall * this.renderChunksDeep];
            int var2 = 0;
            int var3 = 0;
            this.minBlockX = 0;
            this.minBlockY = 0;
            this.minBlockZ = 0;
            this.maxBlockX = this.renderChunksWide;
            this.maxBlockY = this.renderChunksTall;
            this.maxBlockZ = this.renderChunksDeep;
            int var4;

            for (var4 = 0; var4 < this.worldRenderersToUpdate.size(); ++var4)
            {
                this.worldRenderersToUpdate.get(var4).needsUpdate = false;
            }

            this.worldRenderersToUpdate.clear();

            for (var4 = 0; var4 < this.renderChunksWide; ++var4)
            {
                for (int var5 = 0; var5 < this.renderChunksTall; ++var5)
                {
                    for (int var6 = 0; var6 < this.renderChunksDeep; ++var6)
                    {
                        this.worldRenderers[(var6 * this.renderChunksTall + var5) * this.renderChunksWide + var4] = new WorldRenderer(this.theWorld, var4 * 16, var5 * 16, var6 * 16, this.glRenderListBase + var2);
                        this.worldRenderers[(var6 * this.renderChunksTall + var5) * this.renderChunksWide + var4].setInFrustrum();
                        this.worldRenderers[(var6 * this.renderChunksTall + var5) * this.renderChunksWide + var4].chunkIndex = var3++;
                        this.worldRenderers[(var6 * this.renderChunksTall + var5) * this.renderChunksWide + var4].markDirty();
                        this.sortedWorldRenderers[(var6 * this.renderChunksTall + var5) * this.renderChunksWide + var4] = this.worldRenderers[(var6 * this.renderChunksTall + var5) * this.renderChunksWide + var4];
                        this.worldRenderersToUpdate.add(this.worldRenderers[(var6 * this.renderChunksTall + var5) * this.renderChunksWide + var4]);
                        var2 += 3;
                    }
                }
            }

            if (this.theWorld != null)
            {
                EntityPlayer var7 = this.mc.renderViewEntity;

                if (var7 != null)
                {
                    this.markRenderersForNewPosition(MathHelper.floor_double(var7.getPosX()), MathHelper.floor_double(var7.getPosY()), MathHelper.floor_double(var7.getPosZ()));
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
     * Sorts all renderers based on the passed in entity. Args: entityLiving, renderPass, partialTickTime
     */
    public void sortAndRender(EntityPlayer player, int renderPass, double ptt)
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

        double var17 = player.getPosX() - this.prevRenderSortX;
        double var19 = player.getPosY() - this.prevRenderSortY;
        double var21 = player.getPosZ() - this.prevRenderSortZ;

        if (var17 * var17 + var19 * var19 + var21 * var21 > 1.0D)
        {
            this.prevRenderSortX = player.getPosX();
            this.prevRenderSortY = player.getPosY();
            this.prevRenderSortZ = player.getPosZ();
        }

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_LIGHT0);
        GL11.glDisable(GL11.GL_LIGHT1);
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
        
        this.glRenderLists.clear();

        for (int i = 0; i < this.sortedWorldRenderers.length; i++)
        {
        	int j = i;
        	
        	if (renderPass == 1)
        	{
        		j = this.sortedWorldRenderers.length - 1 - i;
        	}
        	
            if (renderPass != 1 && !this.sortedWorldRenderers[j].shouldSkip() && this.sortedWorldRenderers[j].getInFrustrum())
            {
                if (this.sortedWorldRenderers[j].getGLCallList() >= 0)
                {
                    this.glRenderLists.add(this.sortedWorldRenderers[j]);
                }
            }
        }

        double ppx = this.mc.renderViewEntity.getPartialPosX(ptt);
        double ppy = this.mc.renderViewEntity.getPartialPosY(ptt);
        double ppz = this.mc.renderViewEntity.getPartialPosZ(ptt);
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
                this.allRenderLists[whichList].setupRenderList(renderer.posXMinus, renderer.posYMinus, renderer.posZMinus, ppx, ppy, ppz);
            }

            this.allRenderLists[whichList].addGLRenderList(renderPass == 1 ? -1 : renderer.getGLCallList());
        }

        for (int i = 0; i < this.allRenderLists.length; ++i)
        {
            this.allRenderLists[i].callLists();
        }
    }

    /**
     * Updates some of the renderers sorted by distance from the player
     */
    public void updateRenderers(EntityPlayer player)
    {
        RenderSorter rs = new RenderSorter(player);
        WorldRenderer[] rendererArray = new WorldRenderer[2];
        ArrayList<WorldRenderer> rendererList = null;
        int initialSize = this.worldRenderersToUpdate.size();
        label136:

        for (int i = 0; i < initialSize; ++i)
        {
        	WorldRenderer wr = (WorldRenderer)this.worldRenderersToUpdate.get(i);

            if (wr != null)
            {
            	if (wr.quadranceToPlayer(player) > 272.0F)
                {
            		int var11;
                    for (var11 = 0; var11 < 2 && (rendererArray[var11] == null || rs.compare(rendererArray[var11], wr) <= 0); ++var11){;}

                    --var11;

                    if (var11 > 0)
                    {
                        int var12 = var11;

                        while (true)
                        {
                            --var12;

                            if (var12 == 0)
                            {
                                rendererArray[var11] = wr;
                                continue label136;
                            }

                            rendererArray[var12 - 1] = rendererArray[var12];
                        }
                    }

                    continue;
                }

                if (rendererList == null)
                {
                    rendererList = new ArrayList<WorldRenderer>();
                }

                rendererList.add(wr);
                this.worldRenderersToUpdate.set(i, (WorldRenderer)null);
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
            	WorldRenderer wr = (WorldRenderer)rendererList.get(i);
                wr.updateRenderer(player);
                wr.needsUpdate = false;
            }
        }

        for (int i = 1; i >= 0; --i)
        {
            WorldRenderer wr = rendererArray[i];

            if (wr != null)
            {
                if (!wr.getInFrustrum() && i != 1)
                {
                    rendererArray[i] = null;
                    rendererArray[0] = null;
                    break;
                }

                rendererArray[i].updateRenderer(player);
                rendererArray[i].needsUpdate = false;
            }
        }
        
        int i = 0;
        int var11 = 0;

        for (int var12 = this.worldRenderersToUpdate.size(); i != var12; ++i)
        {
            WorldRenderer wr = (WorldRenderer)this.worldRenderersToUpdate.get(i);

            if (wr != null)
            {
                if (!(wr == rendererArray[0] || wr == rendererArray[1]))
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
    private void markBlocksForUpdate(int x1, int y1, int z1, int x2, int y2, int z2)
    {
        int var7 = x1 < 0 ? -((-x1 - 1) / 16) - 1 : x1 / 16;
        int var8 = y1 < 0 ? -((-y1 - 1) / 16) - 1 : y1 / 16;
        int var9 = z1 < 0 ? -((-z1 - 1) / 16) - 1 : z1 / 16;
        int var10 = x2 < 0 ? -((-x2 - 1) / 16) - 1 : x2 / 16;
        int var11 = y2 < 0 ? -((-y2 - 1) / 16) - 1 : y2 / 16;
        int var12 = z2 < 0 ? -((-z2 - 1) / 16) - 1 : z2 / 16;

        for (int var13 = var7; var13 <= var10; ++var13)
        {
            int var14 = var13 % this.renderChunksWide;

            if (var14 < 0)
            {
                var14 += this.renderChunksWide;
            }

            for (int var15 = var8; var15 <= var11; ++var15)
            {
                int var16 = var15 % this.renderChunksTall;

                if (var16 < 0)
                {
                    var16 += this.renderChunksTall;
                }

                for (int var17 = var9; var17 <= var12; ++var17)
                {
                    int var18 = var17 % this.renderChunksDeep;

                    if (var18 < 0)
                    {
                        var18 += this.renderChunksDeep;
                    }

                    int var19 = (var18 * this.renderChunksTall + var16) * this.renderChunksWide + var14;
                    WorldRenderer var20 = this.worldRenderers[var19];

                    if (var20 != null && !var20.needsUpdate)
                    {
                        this.worldRenderersToUpdate.add(var20);
                        var20.markDirty();
                    }
                }
            }
        }
    }

    /**
     * On the client, re-renders the block. On the server, sends the block to the client (which will re-render it),
     * including the tile entity description packet if applicable. Args: x, y, z
     */
    public void markBlockForUpdate(int x, int y, int z)
    {
        this.markBlocksForUpdate(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
    }

    /**
     * On the client, re-renders all blocks in this range, inclusive. On the server, does nothing. Args: min x, min y,
     * min z, max x, max y, max z
     */
    public void markBlockRangeForRenderUpdate(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
    {
        this.markBlocksForUpdate(minX - 1, minY - 1, minZ - 1, maxX + 1, maxY + 1, maxZ + 1);
    }

    /**
     * Checks all renderers that previously weren't in the frustum and 1/16th of those that previously were in the
     * frustum for frustum clipping Args: frustum
     */
    public void clipRenderersByFrustum(double x, double y, double z)
    {
        for (int i = 0; i < this.worldRenderers.length; ++i)
        {
            if (!this.worldRenderers[i].skipAllRenderPasses() && (!this.worldRenderers[i].getInFrustrum() || (i + this.frustumCheckOffset & 15) == 0))
            {
                this.worldRenderers[i].updateInFrustum(x, y, z);
            }
        }

        ++this.frustumCheckOffset;
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
