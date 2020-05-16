package net.minecraft.client.renderer;

public class TesselatorVertexState
{
    private final int[] rawBuffer;
    private final int rawBufferIndex;
    private final int vertexCount;
    private final boolean hasBrightness;
    private final boolean hasNormals;
    private final boolean hasColor;

    public TesselatorVertexState(int[] rawBuff, int rawBuffInd, int vertCount, boolean hasBright, boolean hasNorm, boolean hasCol)
    {
        this.rawBuffer = rawBuff;
        this.rawBufferIndex = rawBuffInd;
        this.vertexCount = vertCount;
        this.hasBrightness = hasBright;
        this.hasNormals = hasNorm;
        this.hasColor = hasCol;
    }

    public int[] getRawBuffer()
    {
        return this.rawBuffer;
    }

    public int getRawBufferIndex()
    {
        return this.rawBufferIndex;
    }

    public int getVertexCount()
    {
        return this.vertexCount;
    }

    public boolean getHasBrightness()
    {
        return this.hasBrightness;
    }

    public boolean getHasNormals()
    {
        return this.hasNormals;
    }

    public boolean getHasColor()
    {
        return this.hasColor;
    }
}
