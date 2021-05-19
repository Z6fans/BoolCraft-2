package net.minecraft.world;

public class Chunk
{
    /**
     * Used to store block IDs, block MSBs, Sky-light maps, Block-light maps, and metadata. Each entry corresponds to a
     * logical segment of 16x16x16 blocks, stacked vertically.
     */
    private byte[] storageArray;

    /** The x coordinate of the chunk. */
    public final int xPosition;

    /** The z coordinate of the chunk. */
    public final int zPosition;

    /**
     * Set to true if the chunk has been modified and needs to be updated internally.
     */
    public boolean isModified;

    public Chunk(int x, int z)
    {
        this.storageArray = new byte[0x10000];
        this.xPosition = x;
        this.zPosition = z;
    }

    /**
     * Returns the ExtendedBlockStorage array for this Chunk.
     */
    public byte[] getBlockStorageArray()
    {
        return this.storageArray;
    }
    
    public int getBlocMeta(int x, int y, int z)
    {
    	return this.storageArray[y << 8 | z << 4 | x] & 0xFF;
    }
    
    public void setBlocMeta(int x, int y, int z, int bm)
    {
    	this.storageArray[y << 8 | z << 4 | x] = (byte)bm;
        this.isModified = true;
    }

    public void setStorageArrays(byte[] storageArray)
    {
        this.storageArray = storageArray;
    }
}