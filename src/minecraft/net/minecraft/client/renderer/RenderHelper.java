package net.minecraft.client.renderer;

import org.lwjgl.opengl.GL11;

public class RenderHelper
{
    /**
     * Disables the OpenGL lighting properties enabled by enableStandardItemLighting
     */
    public static void disableStandardItemLighting()
    {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_LIGHT0);
        GL11.glDisable(GL11.GL_LIGHT1);
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
    }
}
