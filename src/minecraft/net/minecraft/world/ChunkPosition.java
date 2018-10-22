package net.minecraft.world;

public class ChunkPosition
{
    public final int xCoord;
    public final int yCoord;
    public final int zCoord;
    private static final String __OBFID = "CL_00000132";

    public ChunkPosition(int x, int y, int z)
    {
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ChunkPosition))
        {
            return false;
        }
        else
        {
            ChunkPosition otherChunkPosition = (ChunkPosition)other;
            return otherChunkPosition.xCoord == this.xCoord && otherChunkPosition.yCoord == this.yCoord && otherChunkPosition.zCoord == this.zCoord;
        }
    }

    public int hashCode()
    {
        return this.xCoord * 8976890 + this.yCoord * 981131 + this.zCoord;
    }
}
