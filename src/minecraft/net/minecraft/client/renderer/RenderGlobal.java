package net.minecraft.client.renderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.player.EntityPlayerSP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;

public class RenderGlobal
{
    private WorldClient theWorld;
    private List<WorldRenderer> worldRenderersToUpdate = new ArrayList<WorldRenderer>();
    private WorldRenderer[] sortedWorldRenderers;
    private WorldRenderer[] worldRenderers;
    private int renderChunksWide;
    private int renderChunksTall;
    private int renderChunksDeep;

    /** OpenGL render lists base */
    private int glRenderListBase;

    /** A reference to the Minecraft object. */
    private Minecraft mc;

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
    private List<WorldRenderer> glRenderLists = new ArrayList<WorldRenderer>();

    /** All render lists (fixed length 4) */
    private RenderList[] allRenderLists = new RenderList[] {new RenderList(), new RenderList(), new RenderList(), new RenderList()};

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
    public void setWorldAndLoadRenderers(WorldClient worldClient)
    {
        if (this.theWorld != null)
        {
            this.theWorld.setRenderer(null);
        }

        this.prevSortX = -9999.0D;
        this.prevSortY = -9999.0D;
        this.prevSortZ = -9999.0D;
        this.prevRenderSortX = -9999.0D;
        this.prevRenderSortY = -9999.0D;
        this.prevRenderSortZ = -9999.0D;
        this.prevChunkSortX = -9999;
        this.prevChunkSortY = -9999;
        this.prevChunkSortZ = -9999;
        this.theWorld = worldClient;

        if (worldClient != null)
        {
            worldClient.setRenderer(this);
            this.loadRenderers();
        }
    }

    /**
     * Loads all the renderers and sets up the basic settings usage
     */
    public void loadRenderers()
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
                        this.worldRenderers[(var6 * this.renderChunksTall + var5) * this.renderChunksWide + var4].isInFrustum = true;
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
                EntityPlayerSP var7 = this.mc.renderViewEntity;

                if (var7 != null)
                {
                    this.markRenderersForNewPosition(MathHelper.floor_double(var7.posX), MathHelper.floor_double(var7.posY), MathHelper.floor_double(var7.posZ));
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
    public int sortAndRender(EntityPlayerSP p_72719_1_, int p_72719_2_, double p_72719_3_)
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
        
        double var11 = p_72719_1_.posX - this.prevSortX;
        double var13 = p_72719_1_.posY - this.prevSortY;
        double var15 = p_72719_1_.posZ - this.prevSortZ;

        if (this.prevChunkSortX != p_72719_1_.chunkCoordX || this.prevChunkSortY != p_72719_1_.chunkCoordY || this.prevChunkSortZ != p_72719_1_.chunkCoordZ || var11 * var11 + var13 * var13 + var15 * var15 > 16.0D)
        {
            this.prevSortX = p_72719_1_.posX;
            this.prevSortY = p_72719_1_.posY;
            this.prevSortZ = p_72719_1_.posZ;
            this.prevChunkSortX = p_72719_1_.chunkCoordX;
            this.prevChunkSortY = p_72719_1_.chunkCoordY;
            this.prevChunkSortZ = p_72719_1_.chunkCoordZ;
            this.markRenderersForNewPosition(MathHelper.floor_double(p_72719_1_.posX), MathHelper.floor_double(p_72719_1_.posY), MathHelper.floor_double(p_72719_1_.posZ));
        }

        double var17 = p_72719_1_.posX - this.prevRenderSortX;
        double var19 = p_72719_1_.posY - this.prevRenderSortY;
        double var21 = p_72719_1_.posZ - this.prevRenderSortZ;
        int var23;

        if (var17 * var17 + var19 * var19 + var21 * var21 > 1.0D)
        {
            this.prevRenderSortX = p_72719_1_.posX;
            this.prevRenderSortY = p_72719_1_.posY;
            this.prevRenderSortZ = p_72719_1_.posZ;

            for (var23 = 0; var23 < 27; ++var23)
            {
                this.sortedWorldRenderers[var23].updateRendererSort(p_72719_1_);
            }
        }

        RenderHelper.disableStandardItemLighting();
        var23 = this.renderSortedRenderers(0, this.sortedWorldRenderers.length, p_72719_2_, p_72719_3_);
        return var23;
    }

    /**
     * Renders the sorted renders for the specified render pass. Args: startRenderer, numRenderers, renderPass,
     * partialTickTime
     */
    private int renderSortedRenderers(int p_72724_1_, int p_72724_2_, int p_72724_3_, double p_72724_4_)
    {
        this.glRenderLists.clear();
        int var6 = 0;
        int var7 = p_72724_1_;
        int var8 = p_72724_2_;
        byte var9 = 1;

        if (p_72724_3_ == 1)
        {
            var7 = this.sortedWorldRenderers.length - 1 - p_72724_1_;
            var8 = this.sortedWorldRenderers.length - 1 - p_72724_2_;
            var9 = -1;
        }

        for (int var10 = var7; var10 != var8; var10 += var9)
        {
            if (!this.sortedWorldRenderers[var10].skipRenderPass[p_72724_3_] && this.sortedWorldRenderers[var10].isInFrustum)
            {
                int var11 = this.sortedWorldRenderers[var10].getGLCallListForPass(p_72724_3_);

                if (var11 >= 0)
                {
                    this.glRenderLists.add(this.sortedWorldRenderers[var10]);
                    ++var6;
                }
            }
        }

        EntityPlayerSP var22 = this.mc.renderViewEntity;
        double var23 = var22.lastTickPosX + (var22.posX - var22.lastTickPosX) * p_72724_4_;
        double var13 = var22.lastTickPosY + (var22.posY - var22.lastTickPosY) * p_72724_4_;
        double var15 = var22.lastTickPosZ + (var22.posZ - var22.lastTickPosZ) * p_72724_4_;
        int var17 = 0;
        int var18;

        for (var18 = 0; var18 < this.allRenderLists.length; ++var18)
        {
            this.allRenderLists[var18].resetList();
        }

        int var20;
        int var21;

        for (var18 = 0; var18 < this.glRenderLists.size(); ++var18)
        {
            WorldRenderer var19 = (WorldRenderer)this.glRenderLists.get(var18);
            var20 = -1;

            for (var21 = 0; var21 < var17; ++var21)
            {
                if (this.allRenderLists[var21].rendersChunk(var19.posXMinus, var19.posYMinus, var19.posZMinus))
                {
                    var20 = var21;
                }
            }

            if (var20 < 0)
            {
                var20 = var17++;
                this.allRenderLists[var20].setupRenderList(var19.posXMinus, var19.posYMinus, var19.posZMinus, var23, var13, var15);
            }

            this.allRenderLists[var20].addGLRenderList(var19.getGLCallListForPass(p_72724_3_));
        }
        this.renderAllRenderLists(p_72724_3_, p_72724_4_);
        return var6;
    }

    /**
     * Render all render lists
     */
    private void renderAllRenderLists(int p_72733_1_, double p_72733_2_)
    {
        for (int var4 = 0; var4 < this.allRenderLists.length; ++var4)
        {
            this.allRenderLists[var4].callLists();
        }
    }

    /**
     * Updates some of the renderers sorted by distance from the player
     */
    public boolean updateRenderers(EntityPlayerSP player)
    {
        byte var3 = 2;
        RenderSorter var4 = new RenderSorter(player);
        WorldRenderer[] var5 = new WorldRenderer[var3];
        ArrayList<WorldRenderer> var6 = null;
        int var7 = this.worldRenderersToUpdate.size();
        int var8 = 0;
        int var9;
        WorldRenderer var10;
        int var11;
        int var12;
        label136:

        for (var9 = 0; var9 < var7; ++var9)
        {
            var10 = (WorldRenderer)this.worldRenderersToUpdate.get(var9);

            if (var10 != null)
            {
            	if (var10.quadranceToPlayer(player) > 272.0F)
                {
                    for (var11 = 0; var11 < var3 && (var5[var11] == null || var4.compare(var5[var11], var10) <= 0); ++var11)
                    {
                        ;
                    }

                    --var11;

                    if (var11 > 0)
                    {
                        var12 = var11;

                        while (true)
                        {
                            --var12;

                            if (var12 == 0)
                            {
                                var5[var11] = var10;
                                continue label136;
                            }

                            var5[var12 - 1] = var5[var12];
                        }
                    }

                    continue;
                }

                if (var6 == null)
                {
                    var6 = new ArrayList<WorldRenderer>();
                }

                ++var8;
                var6.add(var10);
                this.worldRenderersToUpdate.set(var9, (WorldRenderer)null);
            }
        }

        if (var6 != null)
        {
            if (var6.size() > 1)
            {
                Collections.sort(var6, var4);
            }

            for (var9 = var6.size() - 1; var9 >= 0; --var9)
            {
                var10 = (WorldRenderer)var6.get(var9);
                var10.updateRenderer(player);
                var10.needsUpdate = false;
            }
        }
        var9 = 0;
        int var16;

        for (var16 = var3 - 1; var16 >= 0; --var16)
        {
            WorldRenderer var17 = var5[var16];

            if (var17 != null)
            {
                if (!var17.isInFrustum && var16 != var3 - 1)
                {
                    var5[var16] = null;
                    var5[0] = null;
                    break;
                }

                var5[var16].updateRenderer(player);
                var5[var16].needsUpdate = false;
                ++var9;
            }
        }
        var16 = 0;
        var11 = 0;

        for (var12 = this.worldRenderersToUpdate.size(); var16 != var12; ++var16)
        {
            WorldRenderer var13 = (WorldRenderer)this.worldRenderersToUpdate.get(var16);

            if (var13 != null)
            {
                boolean var14 = false;

                for (int var15 = 0; var15 < var3 && !var14; ++var15)
                {
                    if (var13 == var5[var15])
                    {
                        var14 = true;
                    }
                }

                if (!var14)
                {
                    if (var11 != var16)
                    {
                        this.worldRenderersToUpdate.set(var11, var13);
                    }

                    ++var11;
                }
            }
        }

        while (true)
        {
            --var16;

            if (var16 < var11)
            {
                return var7 == var8 + var9;
            }

            this.worldRenderersToUpdate.remove(var16);
        }
    }

    /**
     * Draws the selection box for the player. Args: entityPlayer, rayTraceHit, i, itemStack, partialTickTime
     */
    public void drawSelectionBox(EntityPlayerSP player, MovingObjectPosition rayTraceHit, float partialTickTime)
    {
    	GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
        GL11.glLineWidth(2.0F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDepthMask(false);
        float var5 = 0.002F;
        Block var6 = this.theWorld.getBlock(rayTraceHit.blockX, rayTraceHit.blockY, rayTraceHit.blockZ);

        if (!var6.isReplaceable())
        {
            double var7 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTickTime;
            double var9 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTickTime;
            double var11 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTickTime;
            drawOutlinedBoundingBox(var6.generateCubicBoundingBox(rayTraceHit.blockX, rayTraceHit.blockY, rayTraceHit.blockZ).expand((double)var5, (double)var5, (double)var5).getOffsetBoundingBox(-var7, -var9, -var11));
        }

        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * Draws lines for the edges of the bounding box.
     */
    private static void drawOutlinedBoundingBox(AxisAlignedBB aabb)
    {
        Tessellator var2 = Tessellator.instance;
        var2.startDrawing(3);
        var2.addVertex(aabb.minX, aabb.minY, aabb.minZ);
        var2.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
        var2.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
        var2.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
        var2.addVertex(aabb.minX, aabb.minY, aabb.minZ);
        var2.draw();
        var2.startDrawing(3);
        var2.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
        var2.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
        var2.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
        var2.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
        var2.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
        var2.draw();
        var2.startDrawing(1);
        var2.addVertex(aabb.minX, aabb.minY, aabb.minZ);
        var2.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
        var2.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
        var2.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
        var2.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
        var2.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
        var2.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
        var2.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
        var2.draw();
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
    public void markBlockForUpdate(int p_147586_1_, int p_147586_2_, int p_147586_3_)
    {
        this.markBlocksForUpdate(p_147586_1_ - 1, p_147586_2_ - 1, p_147586_3_ - 1, p_147586_1_ + 1, p_147586_2_ + 1, p_147586_3_ + 1);
    }

    /**
     * On the client, re-renders all blocks in this range, inclusive. On the server, does nothing. Args: min x, min y,
     * min z, max x, max y, max z
     */
    public void markBlockRangeForRenderUpdate(int p_147585_1_, int p_147585_2_, int p_147585_3_, int p_147585_4_, int p_147585_5_, int p_147585_6_)
    {
        this.markBlocksForUpdate(p_147585_1_ - 1, p_147585_2_ - 1, p_147585_3_ - 1, p_147585_4_ + 1, p_147585_5_ + 1, p_147585_6_ + 1);
    }

    /**
     * Checks all renderers that previously weren't in the frustum and 1/16th of those that previously were in the
     * frustum for frustum clipping Args: frustum, partialTickTime
     */
    public void clipRenderersByFrustum(Frustrum p_72729_1_, float p_72729_2_)
    {
        for (int var3 = 0; var3 < this.worldRenderers.length; ++var3)
        {
            if (!this.worldRenderers[var3].skipAllRenderPasses() && (!this.worldRenderers[var3].isInFrustum || (var3 + this.frustumCheckOffset & 15) == 0))
            {
                this.worldRenderers[var3].updateInFrustum(p_72729_1_);
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
}
