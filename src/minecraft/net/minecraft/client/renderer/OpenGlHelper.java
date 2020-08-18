package net.minecraft.client.renderer;

import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.EXTBlendFuncSeparate;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

public class OpenGlHelper
{
    /**
     * An OpenGL constant corresponding to GL_TEXTURE0, used when setting data pertaining to auxiliary OpenGL texture
     * units.
     */
    public static int defaultTexUnit;

    /**
     * An OpenGL constant corresponding to GL_TEXTURE1, used when setting data pertaining to auxiliary OpenGL texture
     * units.
     */
    public static int lightmapTexUnit;
    private static boolean field_153215_z;
    private static boolean openGL14;
    private static boolean field_153211_u;

    /**
     * Initializes the texture constants to be used when rendering lightmap values
     */
    public static void initializeTextures()
    {
        ContextCapabilities capabilities = GLContext.getCapabilities();
        field_153215_z = capabilities.GL_ARB_multitexture && !capabilities.OpenGL13;
        defaultTexUnit = 33984;
        lightmapTexUnit = 33985;
        field_153211_u = capabilities.GL_EXT_blend_func_separate && !capabilities.OpenGL14;
        openGL14 = capabilities.OpenGL14 || capabilities.GL_EXT_blend_func_separate;
    }

    /**
     * Sets the current lightmap texture to the specified OpenGL constant
     */
    public static void setClientActiveTexture(int texture)
    {
        if (field_153215_z)
        {
            ARBMultitexture.glClientActiveTextureARB(texture);
        }
        else
        {
            GL13.glClientActiveTexture(texture);
        }
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
