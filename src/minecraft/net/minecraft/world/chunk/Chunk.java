package net.minecraft.world.chunk;

import net.minecraft.block.Block;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;

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
    private boolean isLoaded;

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

    public Block getBlock(int x, int y, int z)
    {
        return Block.getBlockById(this.storageArray[y << 8 | z << 4 | x] & 0xF);
    }

    /**
     * Return the metadata corresponding to the given coordinates inside a chunk.
     */
    public int getBlockMetadata(int x, int y, int z)
    {
        return (this.storageArray[y << 8 | z << 4 | x] & 0xF0) >> 4;
    }
    
    public boolean setBlockAndMetaServer(WorldServer world, int x, int y, int z, Block block, int newMeta)
    {
        Block oldBlock = this.getBlock(x, y, z);
        int oldMeta = this.getBlockMetadata(x, y, z);

        if (oldBlock != block || oldMeta != newMeta)
        {
            int trueX = this.xPosition * 16 + x;
            int trueZ = this.zPosition * 16 + z;

            oldBlock.breakBlock(world, trueX, y, trueZ, oldBlock, oldMeta);

            this.storageArray[y << 8 | z << 4 | x] = (byte)(((newMeta & 0xF) << 4) | (Block.getIdFromBlock(block) & 0xF));

            block.onBlockAdded(world, trueX, y, trueZ);

            this.isModified = true;
            return true;
        }
        
        return false;
    }

    /**
     * Set the metadata of a block in the chunk
     */
    public boolean setBlockMetadata(int x, int y, int z, int newMeta)
    {
    	int oldMeta = (this.storageArray[y << 8 | z << 4 | x] & 0xF0) >> 4;

        if (oldMeta != newMeta)
        {
            this.isModified = true;
            this.storageArray[y << 8 | z << 4 | x] = (byte)((this.storageArray[y << 8 | z << 4 | x] & 0xF) | ((newMeta & 0xF) << 4));
            return true;
        }
        
        return false;
    }

    public void setLoaded()
    {
        this.isLoaded = true;
    }

    public boolean getLoaded()
    {
        return this.isLoaded;
    }

    /**
     * Gets a ChunkCoordIntPair representing the Chunk's position.
     */
    public ChunkCoordIntPair getChunkCoordIntPair()
    {
        return new ChunkCoordIntPair(this.xPosition, this.zPosition);
    }

    public void setStorageArrays(byte[] storageArray)
    {
        this.storageArray = storageArray;
    }
}
