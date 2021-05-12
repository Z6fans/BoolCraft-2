package net.minecraft.client.renderer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;

public class Tessellator
{
    /** The byte buffer used for GL allocation. */
    private final ByteBuffer byteBuffer;

    /** The same memory as byteBuffer, but referenced as an integer buffer. */
    private final IntBuffer intBuffer;

    /** The same memory as byteBuffer, but referenced as an float buffer. */
    private final FloatBuffer floatBuffer;

    /** Raw integer array. */
    private final int[] rawBuffer;

    /**
     * The number of vertices to be drawn in the next draw call. Reset to 0 between draw calls.
     */
    private int vertexCount;

    /** The color (RGBA) value to be used for the following draw call. */
    private int color;

    /** The index into the raw buffer to be used for the next data. */
    private int rawBufferIndex;

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
        this.rawBuffer = new int[buffSize];
    }

    /**
     * Draws the data set up in this tessellator and resets the state to prepare for new drawing.
     */
    public void draw()
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
                this.byteBuffer.limit(this.rawBufferIndex * 4);
                this.byteBuffer.position(12);
                GL11.glColorPointer(4, true, 16, this.byteBuffer);
                GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
                this.floatBuffer.position(0);
                GL11.glVertexPointer(3, 16, this.floatBuffer);
                GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glDrawArrays(this.drawMode, 0, this.vertexCount);
                GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
            }

            this.reset();
        }
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
     * Resets tessellator state and prepares for drawing (with the specified draw mode).
     */
    public void startDrawing(int mode)
    {
        if (this.isDrawing)
        {
            throw new IllegalStateException("Already tesselating!");
        }
        else
        {
            this.isDrawing = true;
            this.reset();
            this.drawMode = mode;
        }
    }

    /**
     * Adds a vertex with the specified x,y,z to the current draw call. It will trigger a draw() if the buffer gets
     * full.
     */
    public void addVertex(double x, double y, double z)
    {
        this.rawBuffer[this.rawBufferIndex + 0] = Float.floatToRawIntBits((float)(x + this.xOffset));
        this.rawBuffer[this.rawBufferIndex + 1] = Float.floatToRawIntBits((float)(y + this.yOffset));
        this.rawBuffer[this.rawBufferIndex + 2] = Float.floatToRawIntBits((float)(z + this.zOffset));
        this.rawBuffer[this.rawBufferIndex + 3] = this.color;
        this.rawBufferIndex += 4;
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
    public void setColor_I(int color)
    {
    	int a = color >> 24 & 255;
    	int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;

        this.color = a << 24 | b << 16 | g << 8 | r;
    }

    /**
     * Sets the translation for all vertices in the current draw call.
     */
    public void setTranslation(double xOff, double yOff, double zOff)
    {
        this.xOffset = xOff;
        this.yOffset = yOff;
        this.zOffset = zOff;
    }
}
