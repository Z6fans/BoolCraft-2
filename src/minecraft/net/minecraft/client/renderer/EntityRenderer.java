package net.minecraft.client.renderer;

import net.minecraft.client.EntityPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

public class EntityRenderer
{
    /** A reference to the Minecraft object. */
    private final Minecraft minecraft;

    /** Previous frame time in milliseconds */
    private long prevFrameTime;
    private final RenderGlobal renderGlobal;

    public EntityRenderer(Minecraft mc, RenderGlobal rg)
    {
        this.prevFrameTime = Minecraft.getSystemTime();
        this.minecraft = mc;
        this.renderGlobal = rg;
    }

    /**
     * Updates the entity renderer
     */
    public void updateRenderer()
    {
        if (this.minecraft.renderViewEntity == null)
        {
            this.minecraft.renderViewEntity = this.minecraft.thePlayer;
        }
    }

    /**
     * Will update any inputs that effect the camera angle (mouse) and then render the world and GUI
     */
    public void updateCameraAndRender(float partialTickTime)
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

        final ScaledResolution sr = new ScaledResolution(this.minecraft.displayWidth, this.minecraft.displayHeight);
        int scaledWidth = sr.getScaledWidth();
        int scaledHeight = sr.getScaledHeight();
        final int mouseX = Mouse.getX() * scaledWidth / this.minecraft.displayWidth;
        final int mouseY = scaledHeight - Mouse.getY() * scaledHeight / this.minecraft.displayHeight - 1;

        if (this.minecraft.worldClient != null)
        {
        	GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.5F);

            if (this.minecraft.renderViewEntity == null)
            {
                this.minecraft.renderViewEntity = this.minecraft.thePlayer;
            }
            
            this.minecraft.computeMouseOver(partialTickTime);
            EntityPlayer player = this.minecraft.renderViewEntity;
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
            GL11.glRotatef(player.getPartialRotationPitch(partialTickTime), 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(player.getPartialRotationYaw(partialTickTime) + 180.0F, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(0.0F, player.getYOffset() - 1.62F, 0.0F);
            
            ActiveRenderInfo.updateRenderInfo();

            Frustrum frustrum = new Frustrum();
            frustrum.setPosition(partialX, partialY, partialZ);
            this.renderGlobal.clipRenderersByFrustum(frustrum, partialTickTime);
            this.renderGlobal.updateRenderers(player);
            RenderHelper.disableStandardItemLighting();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            this.renderGlobal.sortAndRender(player, 0, (double)partialTickTime);
            GL11.glShadeModel(GL11.GL_FLAT);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();

            if (this.minecraft.getMouseOver() != null)
            {
                GL11.glDisable(GL11.GL_ALPHA_TEST);
                this.renderGlobal.drawSelectionBox(player, this.minecraft.getMouseOver(), partialTickTime);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
            }

            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(770, 1, 1, 0);
            GL11.glDisable(GL11.GL_BLEND);
            RenderHelper.disableStandardItemLighting();
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_CULL_FACE);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glNormal3f(0.0F, -1.0F, 0.0F);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            GL11.glColorMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDepthMask(false);

            this.renderGlobal.sortAndRender(player, 1, (double)partialTickTime);

            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        	
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            ScaledResolution resolution = new ScaledResolution(this.minecraft.displayWidth, this.minecraft.displayHeight);
            int sWidth = resolution.getScaledWidth();
            int sHeight = resolution.getScaledHeight();
            this.setupOverlayRendering();
            drawRect(sWidth / 2 - 41 - 1 + this.minecraft.currentItem * 20, sHeight - 22 - 1, sWidth / 2 - 41 - 1 + this.minecraft.currentItem * 20 + 24, sHeight, 0x44CCCCCC);
            drawRect(sWidth / 2 - 4, sHeight / 2 - 4, sWidth / 2 + 6, sHeight / 2 + 6, 0x44CCCCCC);
            for (int index = 0; index < 4; ++index)
            {
                int x = sWidth / 2 - 40 + index * 20 + 2;
                int y = sHeight - 16 - 3;
                int color = 0xFF000000;
                switch(index){
                case 0: color = 0xFF444444; break;
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
            this.setupOverlayRendering();
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
                throw new ReportedException(CrashReport.makeCrashReport(t, "Rendering screen"));
            }
        }
    }
    
    /**
     * Draws a solid color rectangle with the specified coordinates and color. Args: x1, y1, x2, y2, color
     */
    private static void drawRect(int x1, int y1, int x2, int y2, int color)
    {
        float alpha	= (float)(color >> 24 & 255) / 255.0F;
        float red	= (float)(color >> 16 & 255) / 255.0F;
        float green	= (float)(color >> 8  & 255) / 255.0F;
        float blue	= (float)(color 	  & 255) / 255.0F;
        Tessellator tessellator = Tessellator.instance;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glColor4f(red, green, blue, alpha);
        tessellator.startDrawingQuads();
        tessellator.addVertex((double)x1, (double)y2, 0.0D);
        tessellator.addVertex((double)x2, (double)y2, 0.0D);
        tessellator.addVertex((double)x2, (double)y1, 0.0D);
        tessellator.addVertex((double)x1, (double)y1, 0.0D);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * Setup orthogonal projection for rendering GUI screen overlays
     */
    public void setupOverlayRendering()
    {
    	GL11.glEnable(GL11.GL_TEXTURE_2D);
        ScaledResolution sr = new ScaledResolution(this.minecraft.displayWidth, this.minecraft.displayHeight);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, sr.getScaledWidth(), sr.getScaledHeight(), 0.0D, 0.0D, 1.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }
}
