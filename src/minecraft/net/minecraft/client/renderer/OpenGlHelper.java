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
    private static final String __OBFID = "CL_00001179";

    /**
     * Initializes the texture constants to be used when rendering lightmap values
     */
    public static void initializeTextures()
    {
        ContextCapabilities var0 = GLContext.getCapabilities();
        field_153215_z = var0.GL_ARB_multitexture && !var0.OpenGL13;
        defaultTexUnit = 33984;
        lightmapTexUnit = 33985;
        field_153211_u = var0.GL_EXT_blend_func_separate && !var0.OpenGL14;
        openGL14 = var0.OpenGL14 || var0.GL_EXT_blend_func_separate;
    }

    /**
     * Sets the current lightmap texture to the specified OpenGL constant
     */
    public static void setClientActiveTexture(int p_77472_0_)
    {
        if (field_153215_z)
        {
            ARBMultitexture.glClientActiveTextureARB(p_77472_0_);
        }
        else
        {
            GL13.glClientActiveTexture(p_77472_0_);
        }
    }

    public static void glBlendFunc(int p_148821_0_, int p_148821_1_, int p_148821_2_, int p_148821_3_)
    {
        if (openGL14)
        {
            if (field_153211_u)
            {
                EXTBlendFuncSeparate.glBlendFuncSeparateEXT(p_148821_0_, p_148821_1_, p_148821_2_, p_148821_3_);
            }
            else
            {
                GL14.glBlendFuncSeparate(p_148821_0_, p_148821_1_, p_148821_2_, p_148821_3_);
            }
        }
        else
        {
            GL11.glBlendFunc(p_148821_0_, p_148821_1_);
        }
    }
}
