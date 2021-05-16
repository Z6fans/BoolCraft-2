package net.minecraft.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.EntityPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Project;

public class EntityRenderer
{
    /** A reference to the Minecraft object. */
    private final Minecraft minecraft;

    /** Previous frame time in milliseconds */
    private long prevFrameTime;
    private final RenderGlobal renderGlobal;
    
    /** The current GL viewport */
    private static final IntBuffer viewport = GLAllocation.createDirectIntBuffer(16);

    /** The current GL modelview matrix */
    private static final FloatBuffer modelview = GLAllocation.createDirectFloatBuffer(16);

    /** The current GL projection matrix */
    private static final FloatBuffer projection = GLAllocation.createDirectFloatBuffer(16);

    /** The computed view object coordinates */
    private static final FloatBuffer objectCoords = GLAllocation.createDirectFloatBuffer(3);

    public EntityRenderer(Minecraft mc, RenderGlobal rg)
    {
        this.prevFrameTime = Minecraft.getSystemTime();
        this.minecraft = mc;
        this.renderGlobal = rg;
    }

    /**
     * Will update any inputs that effect the camera angle (mouse) and then render the world and GUI
     */
    public void updateCameraAndRender(EntityPlayer player, double partialTickTime)
    {
        boolean isDisplayActive = Display.isActive();

        if (!isDisplayActive)
        {
            if (Minecraft.getSystemTime() - this.prevFrameTime > 500L)
            {
                this.minecraft.displayInGameMenu();
            }
        }
        else
        {
            this.prevFrameTime = Minecraft.getSystemTime();
        }

        if (this.minecraft.getInGameHasFocus() && isDisplayActive)
        {
            float mouseDX = (float)Mouse.getDX();
            float mouseDY = (float)Mouse.getDY();

            this.minecraft.thePlayer.setAngles(mouseDX, mouseDY);
        }

        int scaledWidth = this.minecraft.getScaledWidth();
        int scaledHeight = this.minecraft.getScaledHeight();
        final int mouseX = Mouse.getX() * scaledWidth / this.minecraft.displayWidth;
        final int mouseY = scaledHeight - Mouse.getY() * scaledHeight / this.minecraft.displayHeight - 1;

        if (player != null)
        {
        	GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.5F);
            this.minecraft.computeMouseOver(partialTickTime);
            
            double partialX = player.getPartialPosX(partialTickTime);
            double partialY = player.getPartialPosY(partialTickTime);
            double partialZ = player.getPartialPosZ(partialTickTime);

            GL11.glViewport(0, 0, this.minecraft.displayWidth, this.minecraft.displayHeight);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glEnable(GL11.GL_CULL_FACE);
            
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            /**FOV*/
            Project.gluPerspective(110, (float)this.minecraft.displayWidth / (float)this.minecraft.displayHeight, 0.05F, 512.0F);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glRotatef(0.0F, 0.0F, 0.0F, 1.0F);
            GL11.glTranslatef(0.0F, 0.0F, -0.1F);
            GL11.glRotatef((float)player.getPartialRotationPitch(partialTickTime), 1.0F, 0.0F, 0.0F);
            GL11.glRotatef((float)player.getPartialRotationYaw(partialTickTime) + 180.0F, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(0.0F, player.getYOffset() - 1.62F, 0.0F);
            
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
            float var2 = (float)((viewport.get(0) + viewport.get(2)) / 2);
            float var3 = (float)((viewport.get(1) + viewport.get(3)) / 2);
            GLU.gluUnProject(var2, var3, 0.0F, modelview, projection, viewport, objectCoords);

            this.renderGlobal.clipRenderersByFrustum(partialX, partialY, partialZ);
            this.renderGlobal.updateRenderers(player);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_LIGHT0);
            GL11.glDisable(GL11.GL_LIGHT1);
            GL11.glDisable(GL11.GL_COLOR_MATERIAL);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            this.renderGlobal.sortAndRender(player, 0, (double)partialTickTime);
            GL11.glShadeModel(GL11.GL_FLAT);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDepthMask(false);

            if (this.minecraft.getMouseOver() != null)
            {
                MovingObjectPosition rayTraceHit = this.minecraft.getMouseOver();
                GL11.glLineWidth(2.0F);
                double d = 0.002F;
                Block block = this.minecraft.worldServer.getBlock(rayTraceHit.blockX, rayTraceHit.blockY, rayTraceHit.blockZ);
                double playerX = player.getPartialPosX(partialTickTime);
                double playerY = player.getPartialPosY(partialTickTime);
                double playerZ = player.getPartialPosZ(partialTickTime);
                int meta = this.minecraft.worldServer.getBlockMetadata(rayTraceHit.blockX, rayTraceHit.blockY, rayTraceHit.blockZ);
                AxisAlignedBB aabb = block.generateCubicBoundingBox(rayTraceHit.blockX, rayTraceHit.blockY, rayTraceHit.blockZ, meta).expand(d, d, d).getOffsetBoundingBox(-playerX, -playerY, -playerZ);
                Tessellator tess = Tessellator.instance;
                tess.startDrawing(3);
                tess.setColor_I(0xFF000000);
                tess.addVertex(aabb.minX, aabb.minY, aabb.minZ);
                tess.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
                tess.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
                tess.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
                tess.addVertex(aabb.minX, aabb.minY, aabb.minZ);
                tess.draw();
                tess.startDrawing(3);
                tess.setColor_I(0xFF000000);
                tess.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
                tess.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
                tess.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
                tess.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
                tess.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
                tess.draw();
                tess.startDrawing(1);
                tess.setColor_I(0xFF000000);
                tess.addVertex(aabb.minX, aabb.minY, aabb.minZ);
                tess.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
                tess.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
                tess.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
                tess.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
                tess.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
                tess.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
                tess.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
                tess.draw();
            }

            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            GL11.glColorMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT);
            GL11.glDepthMask(false);

            this.renderGlobal.sortAndRender(player, 1, (double)partialTickTime);

            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        	
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            int sWidth = this.minecraft.getScaledWidth();
            int sHeight = this.minecraft.getScaledHeight();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, this.minecraft.getScaledWidth(), this.minecraft.getScaledHeight(), 0.0D, 0.0D, 1.0D);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            drawRect(sWidth / 2 - 41 - 1 + this.minecraft.currentItem * 20, sHeight - 22 - 1, sWidth / 2 - 41 - 1 + this.minecraft.currentItem * 20 + 24, sHeight, 0x44CCCCCC);
            drawRect(sWidth / 2 - 4, sHeight / 2 - 4, sWidth / 2 + 6, sHeight / 2 + 6, 0x44CCCCCC);
            for (int index = 0; index < 4; ++index)
            {
                int x = sWidth / 2 - 40 + index * 20 + 2;
                int y = sHeight - 16 - 3;
                int color = 0xFF000000;
                switch(index){
                case 0: color = 0xFF505050; break;
                case 1: color = 0xFF39EEEE; break;
                case 2: color = 0xFFEE39E4; break;
                case 3: color = 0xFFE91A64; break;
                default: break;
                }
                drawRect(x, y, x + 16, y + 16, color);
            }
        }
        else
        {
            GL11.glViewport(0, 0, this.minecraft.displayWidth, this.minecraft.displayHeight);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, this.minecraft.getScaledWidth(), this.minecraft.getScaledHeight(), 0.0D, 0.0D, 1.0D);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
        }

        if (this.minecraft.currentScreen != null)
        {
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

            try
            {
                this.minecraft.currentScreen.drawScreen(mouseX, mouseY);
            }
            catch (Throwable t)
            {
                throw new RuntimeException("Rendering screen", t);
            }
        }
    }
    
    /**
     * Draws a solid color rectangle with the specified coordinates and color. Args: x1, y1, x2, y2, color
     */
    private static void drawRect(int x1, int y1, int x2, int y2, int color)
    {
        Tessellator tessellator = Tessellator.instance;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(770, 771);
        tessellator.startDrawing(7);
        tessellator.setColor_I(color);
        tessellator.addVertex(x1, y2, 0);
        tessellator.addVertex(x2, y2, 0);
        tessellator.addVertex(x2, y1, 0);
        tessellator.addVertex(x1, y1, 0);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
