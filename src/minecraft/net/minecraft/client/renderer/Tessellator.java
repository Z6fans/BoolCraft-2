package net.minecraft.client.renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Comparator;
import java.util.PriorityQueue;

import org.lwjgl.opengl.GL11;

public class Tessellator
{
    /** The byte buffer used for GL allocation. */
    private final ByteBuffer byteBuffer;

    /** The same memory as byteBuffer, but referenced as an integer buffer. */
    private final IntBuffer intBuffer;

    /** The same memory as byteBuffer, but referenced as an float buffer. */
    private final FloatBuffer floatBuffer;

    /** The same memory as byteBuffer, but referenced as an short buffer. */
    private final ShortBuffer shortBuffer;

    /** Raw integer array. */
    private final int[] rawBuffer;

    /**
     * The number of vertices to be drawn in the next draw call. Reset to 0 between draw calls.
     */
    private int vertexCount;
    private int brightness;

    /** The color (RGBA) value to be used for the following draw call. */
    private int color;

    /**
     * Whether the current draw object for this tessellator has color values.
     */
    private boolean hasColor;
    private boolean hasBrightness;

    /**
     * Whether the current draw object for this tessellator has normal values.
     */
    private boolean hasNormals;

    /** The index into the raw buffer to be used for the next data. */
    private int rawBufferIndex;

    /** Disables all color information for the following draw call. */
    private boolean isColorDisabled;

    /** The draw mode currently being used by the tessellator. */
    private int drawMode;

    /**
     * An offset to be applied along the x-axis for all vertices in this draw call.
     */
    private double xOffset;

    /**
     * An offset to be applied along the y-axis for all vertices in this draw call.
     */
    private double yOffset;

    /**
     * An offset to be applied along the z-axis for all vertices in this draw call.
     */
    private double zOffset;

    /** The static instance of the Tessellator. */
    public static final Tessellator instance = new Tessellator(2097152);

    /** Whether this tessellator is currently in draw mode. */
    private boolean isDrawing;

    /** The size of the buffers used (in integers). */
    private final int bufferSize;

    private Tessellator(int buffSize)
    {
        this.bufferSize = buffSize;
        this.byteBuffer = GLAllocation.createDirectByteBuffer(buffSize * 4);
        this.intBuffer = this.byteBuffer.asIntBuffer();
        this.floatBuffer = this.byteBuffer.asFloatBuffer();
        this.shortBuffer = this.byteBuffer.asShortBuffer();
        this.rawBuffer = new int[buffSize];
    }

    /**
     * Draws the data set up in this tessellator and resets the state to prepare for new drawing.
     */
    public int draw()
    {
        if (!this.isDrawing)
        {
            throw new IllegalStateException("Not tesselating!");
        }
        else
        {
            this.isDrawing = false;

            if (this.vertexCount > 0)
            {
                this.intBuffer.clear();
                this.intBuffer.put(this.rawBuffer, 0, this.rawBufferIndex);
                this.byteBuffer.position(0);
                this.byteBuffer.limit(this.rawBufferIndex * 4);

                if (this.hasBrightness)
                {
                    OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
                    this.shortBuffer.position(14);
                    GL11.glTexCoordPointer(2, 32, this.shortBuffer);
                    GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                    OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                }

                if (this.hasColor)
                {
                    this.byteBuffer.position(20);
                    GL11.glColorPointer(4, true, 32, this.byteBuffer);
                    GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
                }

                if (this.hasNormals)
                {
                    this.byteBuffer.position(24);
                    GL11.glNormalPointer(32, this.byteBuffer);
                    GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                }

                this.floatBuffer.position(0);
                GL11.glVertexPointer(3, 32, this.floatBuffer);
                GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glDrawArrays(this.drawMode, 0, this.vertexCount);
                GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);

                if (this.hasBrightness)
                {
                    OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
                    GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                    OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                }

                if (this.hasColor)
                {
                    GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
                }

                if (this.hasNormals)
                {
                    GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
                }
            }

            int var1 = this.rawBufferIndex * 4;
            this.reset();
            return var1;
        }
    }

    public TesselatorVertexState getVertexState(float p_147564_1_, float p_147564_2_, float p_147564_3_)
    {
        int[] var4 = new int[this.rawBufferIndex];
        PriorityQueue<Integer> var5 = new PriorityQueue<Integer>(this.rawBufferIndex, new QuadComparator(this.rawBuffer, p_147564_1_ + (float)this.xOffset, p_147564_2_ + (float)this.yOffset, p_147564_3_ + (float)this.zOffset));
        byte var6 = 32;
        int var7;

        for (var7 = 0; var7 < this.rawBufferIndex; var7 += var6)
        {
            var5.add(Integer.valueOf(var7));
        }

        for (var7 = 0; !var5.isEmpty(); var7 += var6)
        {
            int var8 = ((Integer)var5.remove()).intValue();

            for (int var9 = 0; var9 < var6; ++var9)
            {
                var4[var7 + var9] = this.rawBuffer[var8 + var9];
            }
        }

        System.arraycopy(var4, 0, this.rawBuffer, 0, var4.length);
        return new TesselatorVertexState(var4, this.rawBufferIndex, this.vertexCount, this.hasBrightness, this.hasNormals, this.hasColor);
    }

    public void setVertexState(TesselatorVertexState p_147565_1_)
    {
        System.arraycopy(p_147565_1_.getRawBuffer(), 0, this.rawBuffer, 0, p_147565_1_.getRawBuffer().length);
        this.rawBufferIndex = p_147565_1_.getRawBufferIndex();
        this.vertexCount = p_147565_1_.getVertexCount();
        this.hasBrightness = p_147565_1_.getHasBrightness();
        this.hasColor = p_147565_1_.getHasColor();
        this.hasNormals = p_147565_1_.getHasNormals();
    }

    /**
     * Clears the tessellator state in preparation for new drawing.
     */
    private void reset()
    {
        this.vertexCount = 0;
        this.byteBuffer.clear();
        this.rawBufferIndex = 0;
    }

    /**
     * Sets draw mode in the tessellator to draw quads.
     */
    public void startDrawingQuads()
    {
        this.startDrawing(7);
    }

    /**
     * Resets tessellator state and prepares for drawing (with the specified draw mode).
     */
    public void startDrawing(int p_78371_1_)
    {
        if (this.isDrawing)
        {
            throw new IllegalStateException("Already tesselating!");
        }
        else
        {
            this.isDrawing = true;
            this.reset();
            this.drawMode = p_78371_1_;
            this.hasNormals = false;
            this.hasColor = false;
            this.hasBrightness = false;
            this.isColorDisabled = false;
        }
    }

    public void setBrightness(int p_78380_1_)
    {
        this.hasBrightness = true;
        this.brightness = p_78380_1_;
    }

    /**
     * Adds a vertex with the specified x,y,z to the current draw call. It will trigger a draw() if the buffer gets
     * full.
     */
    public void addVertex(double p_78377_1_, double p_78377_3_, double p_78377_5_)
    {
        if (this.hasBrightness)
        {
            this.rawBuffer[this.rawBufferIndex + 7] = this.brightness;
        }

        if (this.hasColor)
        {
            this.rawBuffer[this.rawBufferIndex + 5] = this.color;
        }

        if (this.hasNormals)
        {
            this.rawBuffer[this.rawBufferIndex + 6] = 0;
        }

        this.rawBuffer[this.rawBufferIndex + 0] = Float.floatToRawIntBits((float)(p_78377_1_ + this.xOffset));
        this.rawBuffer[this.rawBufferIndex + 1] = Float.floatToRawIntBits((float)(p_78377_3_ + this.yOffset));
        this.rawBuffer[this.rawBufferIndex + 2] = Float.floatToRawIntBits((float)(p_78377_5_ + this.zOffset));
        this.rawBufferIndex += 8;
        ++this.vertexCount;

        if (this.vertexCount % 4 == 0 && this.rawBufferIndex >= this.bufferSize - 32)
        {
            this.draw();
            this.isDrawing = true;
        }
    }

    /**
     * Sets the color to the given opaque value (stored as byte values packed in an integer).
     */
    public void setColorOpaque_I(int color)
    {
        if (!this.isColorDisabled)
        {
        	int r = color >> 16 & 255;
            int g = color >> 8 & 255;
            int b = color & 255;
            this.hasColor = true;

            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN)
            {
                this.color = 255 << 24 | b << 16 | g << 8 | r;
            }
            else
            {
                this.color = r << 24 | g << 16 | b << 8 | 255;
            }
        }
    }

    /**
     * Sets the translation for all vertices in the current draw call.
     */
    public void setTranslation(double p_78373_1_, double p_78373_3_, double p_78373_5_)
    {
        this.xOffset = p_78373_1_;
        this.yOffset = p_78373_3_;
        this.zOffset = p_78373_5_;
    }
    
    private class QuadComparator implements Comparator<Integer>
    {
        private float field_147630_a;
        private float field_147628_b;
        private float field_147629_c;
        private int[] field_147627_d;

        public QuadComparator(int[] p_i45077_1_, float p_i45077_2_, float p_i45077_3_, float p_i45077_4_)
        {
            this.field_147627_d = p_i45077_1_;
            this.field_147630_a = p_i45077_2_;
            this.field_147628_b = p_i45077_3_;
            this.field_147629_c = p_i45077_4_;
        }

        public int compare(Integer p_compare_1_, Integer p_compare_2_)
        {
            float var3 = Float.intBitsToFloat(this.field_147627_d[p_compare_1_.intValue()]) - this.field_147630_a;
            float var4 = Float.intBitsToFloat(this.field_147627_d[p_compare_1_.intValue() + 1]) - this.field_147628_b;
            float var5 = Float.intBitsToFloat(this.field_147627_d[p_compare_1_.intValue() + 2]) - this.field_147629_c;
            float var6 = Float.intBitsToFloat(this.field_147627_d[p_compare_1_.intValue() + 8]) - this.field_147630_a;
            float var7 = Float.intBitsToFloat(this.field_147627_d[p_compare_1_.intValue() + 9]) - this.field_147628_b;
            float var8 = Float.intBitsToFloat(this.field_147627_d[p_compare_1_.intValue() + 10]) - this.field_147629_c;
            float var9 = Float.intBitsToFloat(this.field_147627_d[p_compare_1_.intValue() + 16]) - this.field_147630_a;
            float var10 = Float.intBitsToFloat(this.field_147627_d[p_compare_1_.intValue() + 17]) - this.field_147628_b;
            float var11 = Float.intBitsToFloat(this.field_147627_d[p_compare_1_.intValue() + 18]) - this.field_147629_c;
            float var12 = Float.intBitsToFloat(this.field_147627_d[p_compare_1_.intValue() + 24]) - this.field_147630_a;
            float var13 = Float.intBitsToFloat(this.field_147627_d[p_compare_1_.intValue() + 25]) - this.field_147628_b;
            float var14 = Float.intBitsToFloat(this.field_147627_d[p_compare_1_.intValue() + 26]) - this.field_147629_c;
            float var15 = Float.intBitsToFloat(this.field_147627_d[p_compare_2_.intValue()]) - this.field_147630_a;
            float var16 = Float.intBitsToFloat(this.field_147627_d[p_compare_2_.intValue() + 1]) - this.field_147628_b;
            float var17 = Float.intBitsToFloat(this.field_147627_d[p_compare_2_.intValue() + 2]) - this.field_147629_c;
            float var18 = Float.intBitsToFloat(this.field_147627_d[p_compare_2_.intValue() + 8]) - this.field_147630_a;
            float var19 = Float.intBitsToFloat(this.field_147627_d[p_compare_2_.intValue() + 9]) - this.field_147628_b;
            float var20 = Float.intBitsToFloat(this.field_147627_d[p_compare_2_.intValue() + 10]) - this.field_147629_c;
            float var21 = Float.intBitsToFloat(this.field_147627_d[p_compare_2_.intValue() + 16]) - this.field_147630_a;
            float var22 = Float.intBitsToFloat(this.field_147627_d[p_compare_2_.intValue() + 17]) - this.field_147628_b;
            float var23 = Float.intBitsToFloat(this.field_147627_d[p_compare_2_.intValue() + 18]) - this.field_147629_c;
            float var24 = Float.intBitsToFloat(this.field_147627_d[p_compare_2_.intValue() + 24]) - this.field_147630_a;
            float var25 = Float.intBitsToFloat(this.field_147627_d[p_compare_2_.intValue() + 25]) - this.field_147628_b;
            float var26 = Float.intBitsToFloat(this.field_147627_d[p_compare_2_.intValue() + 26]) - this.field_147629_c;
            float var27 = (var3 + var6 + var9 + var12) * 0.25F;
            float var28 = (var4 + var7 + var10 + var13) * 0.25F;
            float var29 = (var5 + var8 + var11 + var14) * 0.25F;
            float var30 = (var15 + var18 + var21 + var24) * 0.25F;
            float var31 = (var16 + var19 + var22 + var25) * 0.25F;
            float var32 = (var17 + var20 + var23 + var26) * 0.25F;
            float var33 = var27 * var27 + var28 * var28 + var29 * var29;
            float var34 = var30 * var30 + var31 * var31 + var32 * var32;
            return Float.compare(var34, var33);
        }
    }
}
