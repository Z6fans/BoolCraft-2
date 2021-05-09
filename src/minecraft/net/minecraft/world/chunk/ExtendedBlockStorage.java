package net.minecraft.world.chunk;

import net.minecraft.block.Block;

public class ExtendedBlockStorage
{
    /**
     * Contains the bottom-most Y block represented by this ExtendedBlockStorage. Typically a multiple of 16.
     */
    private final int yBase;

    /**
     * A total count of the number of non-air blocks in this block storage's Chunk.
     */
    private int blockRefCount;

    /**
     * Contains the least significant 8 bits of each block ID belonging to this block storage's parent Chunk.
     */
    private byte[] blockLSBArray;

    /**
     * Contains the most significant 4 bits of each block ID belonging to this block storage's parent Chunk.
     */
    private NibbleArray blockMSBArray;

    /**
     * Stores the metadata associated with blocks in this ExtendedBlockStorage.
     */
    private NibbleArray blockMetadataArray;

    public ExtendedBlockStorage(int baseY)
    {
        this.yBase = baseY;
        this.blockLSBArray = new byte[4096];
        this.blockMetadataArray = new NibbleArray(this.blockLSBArray.length, 4);
    }

    public Block getBlock(int x, int y, int z)
    {
        int id = this.blockLSBArray[y << 8 | z << 4 | x] & 255;

        if (this.blockMSBArray != null)
        {
            id |= this.blockMSBArray.get(x, y, z) << 8;
        }

        return Block.getBlockById(id);
    }

    public void setBlock(int localX, int localY, int localZ, Block newBlock)
    {
        int oldID = this.blockLSBArray[localY << 8 | localZ << 4 | localX] & 255;

        if (this.blockMSBArray != null)
        {
            oldID |= this.blockMSBArray.get(localX, localY, localZ) << 8;
        }

        Block oldBlock = Block.getBlockById(oldID);

        if (oldBlock != Block.air)
        {
            --this.blockRefCount;
        }

        if (newBlock != Block.air)
        {
            ++this.blockRefCount;
        }

        int newID = Block.getIdFromBlock(newBlock);
        this.blockLSBArray[localY << 8 | localZ << 4 | localX] = (byte)(newID & 255);

        if (newID > 255)
        {
            if (this.blockMSBArray == null)
            {
                this.blockMSBArray = new NibbleArray(this.blockLSBArray.length, 4);
            }

            this.blockMSBArray.set(localX, localY, localZ, (newID & 0xF00) >> 8);
        }
        else if (this.blockMSBArray != null)
        {
            this.blockMSBArray.set(localX, localY, localZ, 0);
        }
    }

    /**
     * Returns the metadata associated with the block at the given coordinates in this ExtendedBlockStorage.
     */
    public int getExtBlockMetadata(int p_76665_1_, int p_76665_2_, int p_76665_3_)
    {
        return this.blockMetadataArray.get(p_76665_1_, p_76665_2_, p_76665_3_);
    }

    /**
     * Sets the metadata of the Block at the given coordinates in this ExtendedBlockStorage to the given metadata.
     */
    public void setExtBlockMetadata(int p_76654_1_, int p_76654_2_, int p_76654_3_, int p_76654_4_)
    {
        this.blockMetadataArray.set(p_76654_1_, p_76654_2_, p_76654_3_, p_76654_4_);
    }

    /**
     * Returns whether or not this block storage's Chunk is fully empty, based on its internal reference count.
     */
    public boolean isEmpty()
    {
        return this.blockRefCount == 0;
    }

    /**
     * Returns the Y location of this ExtendedBlockStorage.
     */
    public int getYLocation()
    {
        return this.yBase;
    }

    public void removeInvalidBlocks()
    {
        this.blockRefCount = 0;

        for (int var1 = 0; var1 < 16; ++var1)
        {
            for (int var2 = 0; var2 < 16; ++var2)
            {
                for (int var3 = 0; var3 < 16; ++var3)
                {
                    Block var4 = this.getBlock(var1, var2, var3);

                    if (var4 != Block.air)
                    {
                        ++this.blockRefCount;
                    }
                }
            }
        }
    }

    public byte[] getBlockLSBArray()
    {
        return this.blockLSBArray;
    }

    /**
     * Returns the block ID MSB (bits 11..8) array for this storage array's Chunk.
     */
    public NibbleArray getBlockMSBArray()
    {
        return this.blockMSBArray;
    }

    public NibbleArray getMetadataArray()
    {
        return this.blockMetadataArray;
    }

    /**
     * Sets the array of block ID least significant bits for this ExtendedBlockStorage.
     */
    public void setBlockLSBArray(byte[] p_76664_1_)
    {
        this.blockLSBArray = p_76664_1_;
    }

    /**
     * Sets the array of blockID most significant bits (blockMSBArray) for this ExtendedBlockStorage.
     */
    public void setBlockMSBArray(NibbleArray p_76673_1_)
    {
        this.blockMSBArray = p_76673_1_;
    }

    /**
     * Sets the NibbleArray of block metadata (blockMetadataArray) for this ExtendedBlockStorage.
     */
    public void setBlockMetadataArray(NibbleArray p_76668_1_)
    {
        this.blockMetadataArray = p_76668_1_;
    }

    /**
     * Called by a Chunk to initialize the MSB array if getBlockMSBArray returns null. Returns the newly-created
     * NibbleArray instance.
     */
    public NibbleArray createBlockMSBArray()
    {
        this.blockMSBArray = new NibbleArray(this.blockLSBArray.length, 4);
        return this.blockMSBArray;
    }
}
