package net.minecraft.client.renderer;

import net.minecraft.client.EntityPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;

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
        this.minecraft = mc;
        this.renderGlobal = rg;
    }

    /**
     * Will update any inputs that effect the camera angle (mouse) and then render the world and GUI
     */
    public void updateCameraAndRender(WorldServer world, EntityPlayer player, int currentItem, double ptt)
    {
    	if (this.minecraft.getInGameHasFocus() && Display.isActive())
        {
            player.setAngles(Mouse.getDX(), Mouse.getDY());
        }
        
        GL11.glViewport(0, 0, this.minecraft.displayWidth, this.minecraft.displayHeight);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        Project.gluPerspective(110, (float)this.minecraft.displayWidth / (float)this.minecraft.displayHeight, 0.05F, 512.0F); //FOV
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glRotatef((float)player.getPartialRotationPitch(ptt), 1.0F, 0.0F, 0.0F);
        GL11.glRotatef((float)player.getPartialRotationYaw(ptt) + 180.0F, 0.0F, 1.0F, 0.0F);
        
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        GLU.gluUnProject((viewport.get(0) + viewport.get(2)) / 2,
        		         (viewport.get(1) + viewport.get(3)) / 2,
        		         0.0F, modelview, projection, viewport, objectCoords);

        Vec3 ppos = player.pttPos(ptt);
        this.renderGlobal.updateRenderers(player, ppos.x, ppos.y, ppos.z);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_LIGHT0);
        GL11.glDisable(GL11.GL_LIGHT1);
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        this.renderGlobal.sortAndRender(player, ptt);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDepthMask(false);

        this.minecraft.computeMouseOver(ptt);
        
        if (this.minecraft.getMouseOver() != null)
        {
            MovingObjectPosition hit = this.minecraft.getMouseOver();
            GL11.glLineWidth(2.0F);
            int meta = world.getBlockMetadata(hit.x, hit.y, hit.z);
            AxisAlignedBB aabb = world.getBlock(hit.x, hit.y, hit.z).generateCubicBoundingBox(hit.x, hit.y, hit.z, meta).expand(0.002F).offset(-ppos.x, -ppos.y, -ppos.z);
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
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, this.minecraft.displayWidth, this.minecraft.displayHeight, 0.0D, 0.0D, 1.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        int sWidth = this.minecraft.displayWidth;
        int sHeight = this.minecraft.displayHeight;
        drawRect(sWidth / 2 - 41 - 1 + currentItem * 20, sHeight - 22 - 1, sWidth / 2 - 41 - 1 + currentItem * 20 + 24, sHeight, 0x44CCCCCC);
        drawRect(sWidth / 2 - 4, sHeight / 2 - 4, sWidth / 2 + 6, sHeight / 2 + 6, 0x44CCCCCC);
        int[] colores = {0xFF505050, 0xFF39EEEE, 0xFFEE39E4, 0xFFE91A64};
        for (int i = 0; i < 4; ++i)
        {
            int x = sWidth / 2 - 40 + i * 20 + 2;
            drawRect(x, sHeight - 19, x + 16, sHeight - 3, colores[i]);
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
