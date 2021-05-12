package net.minecraft.client.renderer;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.EXTBlendFuncSeparate;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

public class OpenGlHelper
{
    private static boolean openGL14;
    private static boolean field_153211_u;

    /**
     * Initializes the texture constants to be used when rendering lightmap values
     */
    public static void initializeTextures()
    {
        ContextCapabilities capabilities = GLContext.getCapabilities();
        field_153211_u = capabilities.GL_EXT_blend_func_separate && !capabilities.OpenGL14;
        openGL14 = capabilities.OpenGL14 || capabilities.GL_EXT_blend_func_separate;
    }

    public static void glBlendFunc(int srgb, int drgb, int sa, int da)
    {
        if (openGL14)
        {
            if (field_153211_u)
            {
                EXTBlendFuncSeparate.glBlendFuncSeparateEXT(srgb, drgb, sa, da);
            }
            else
            {
                GL14.glBlendFuncSeparate(srgb, drgb, sa, da);
            }
        }
        else
        {
            GL11.glBlendFunc(srgb, drgb);
        }
    }
}
