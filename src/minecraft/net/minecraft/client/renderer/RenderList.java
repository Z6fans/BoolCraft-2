package net.minecraft.client.renderer;

import java.nio.IntBuffer;
import org.lwjgl.opengl.GL11;

public class RenderList
{
    /**
     * The location of the 16x16x16 render chunk rendered by this RenderList.
     */
    private int renderChunkX;
    private int renderChunkY;
    private int renderChunkZ;

    /**
     * The in-world location of the camera, used to translate the world into the proper position for rendering.
     */
    private double cameraX;
    private double cameraY;
    private double cameraZ;

    /** A list of OpenGL render list IDs rendered by this RenderList. */
    private final IntBuffer glLists = GLAllocation.createDirectIntBuffer(65536);

    /**
     * Does this RenderList contain properly-initialized and current data for rendering?
     */
    private boolean valid;

    /** Has glLists been flipped to make it ready for reading yet? */
    private boolean bufferFlipped;

    public void setupRenderList(int chunkX, int chunkY, int chunkZ, double camX, double camY, double camZ)
    {
        this.valid = true;
        this.glLists.clear();
        this.renderChunkX = chunkX;
        this.renderChunkY = chunkY;
        this.renderChunkZ = chunkZ;
        this.cameraX = camX;
        this.cameraY = camY;
        this.cameraZ = camZ;
    }

    public boolean rendersChunk(int x, int y, int z)
    {
        return this.valid && x == this.renderChunkX && y == this.renderChunkY && z == this.renderChunkZ;
    }

    public void addGLRenderList(int i)
    {
        this.glLists.put(i);

        if (this.glLists.remaining() == 0)
        {
            this.callLists();
        }
    }

    public void callLists()
    {
        if (this.valid)
        {
            if (!this.bufferFlipped)
            {
                this.glLists.flip();
                this.bufferFlipped = true;
            }

            if (this.glLists.remaining() > 0)
            {
                GL11.glPushMatrix();
                GL11.glTranslatef((float)((double)this.renderChunkX - this.cameraX), (float)((double)this.renderChunkY - this.cameraY), (float)((double)this.renderChunkZ - this.cameraZ));
                GL11.glCallLists(this.glLists);
                GL11.glPopMatrix();
            }
        }
    }

    /**
     * Resets this RenderList to an uninitialized state.
     */
    public void resetList()
    {
        this.valid = false;
        this.bufferFlipped = false;
    }
}
