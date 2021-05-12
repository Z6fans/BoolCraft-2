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
    private ExtendedBlockStorage[] storageArrays;

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
        this.storageArrays = new ExtendedBlockStorage[16];
        this.xPosition = x;
        this.zPosition = z;
    }

    /**
     * Returns the topmost ExtendedBlockStorage instance for this Chunk that actually contains a block.
     */
    public int getTopFilledSegment()
    {
        for (int var1 = this.storageArrays.length - 1; var1 >= 0; --var1)
        {
            if (this.storageArrays[var1] != null)
            {
                return this.storageArrays[var1].getYLocation();
            }
        }

        return 0;
    }

    /**
     * Returns the ExtendedBlockStorage array for this Chunk.
     */
    public ExtendedBlockStorage[] getBlockStorageArray()
    {
        return this.storageArrays;
    }

    public Block getBlock(final int x, final int y, final int z)
    {
        Block block = Block.air;

        if (y >> 4 < this.storageArrays.length)
        {
            ExtendedBlockStorage storage = this.storageArrays[y >> 4];

            if (storage != null)
            {
                block = storage.getBlock(x, y & 15, z);
            }
        }

        return block;
    }

    /**
     * Return the metadata corresponding to the given coordinates inside a chunk.
     */
    public int getBlockMetadata(int x, int y, int z)
    {
        if (y >> 4 >= this.storageArrays.length)
        {
            return 0;
        }
        else
        {
            ExtendedBlockStorage storage = this.storageArrays[y >> 4];
            return storage != null ? storage.getExtBlockMetadata(x, y & 15, z) : 0;
        }
    }
    
    public boolean setBlockAndMetaServer(WorldServer world, int localX, int y, int localZ, Block block, int meta)
    {
        Block oldBlock = this.getBlock(localX, y, localZ);
        int oldMeta = this.getBlockMetadata(localX, y, localZ);

        if (oldBlock == block && oldMeta == meta)
        {
            return false;
        }
        else
        {
            ExtendedBlockStorage storageArray = this.storageArrays[y >> 4];

            if (storageArray == null)
            {
                if (block == Block.air)
                {
                    return false;
                }

                storageArray = this.storageArrays[y >> 4] = new ExtendedBlockStorage(y >> 4 << 4);
            }

            int trueX = this.xPosition * 16 + localX;
            int trueZ = this.zPosition * 16 + localZ;

            storageArray.setBlock(localX, y & 15, localZ, block);

            oldBlock.breakBlock(world, trueX, y, trueZ, oldBlock, oldMeta);

            if (storageArray.getBlock(localX, y & 15, localZ) != block)
            {
                return false;
            }
            else
            {
                storageArray.setExtBlockMetadata(localX, y & 15, localZ, meta);

                block.onBlockAdded(world, trueX, y, trueZ);

                this.isModified = true;
                return true;
            }
        }
    }

    /**
     * Set the metadata of a block in the chunk
     */
    public boolean setBlockMetadata(int x, int y, int z, int newMeta)
    {
        ExtendedBlockStorage storage = this.storageArrays[y >> 4];

        if (storage == null)
        {
            return false;
        }
        else
        {
            int oldMeta = storage.getExtBlockMetadata(x, y & 15, z);

            if (oldMeta == newMeta)
            {
                return false;
            }
            else
            {
                this.isModified = true;
                storage.setExtBlockMetadata(x, y & 15, z, newMeta);

                return true;
            }
        }
    }

    /**
     * Sets the isModified flag for this Chunk
     */
    public void setChunkModified()
    {
        this.isModified = true;
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

    public void setStorageArrays(ExtendedBlockStorage[] storageArray)
    {
        this.storageArrays = storageArray;
    }
}
