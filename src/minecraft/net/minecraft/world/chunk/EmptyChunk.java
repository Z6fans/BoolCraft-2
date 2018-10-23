package net.minecraft.world.chunk;

import net.minecraft.block.Block;
import net.minecraft.player.EntityPlayerSP;

public class EmptyChunk extends Chunk
{
    public EmptyChunk()
    {
        super(0, 0);
    }

    /**
     * Checks whether the chunk is at the X/Z location specified
     */
    public boolean isAtLocation(int p_76600_1_, int p_76600_2_)
    {
        return p_76600_1_ == this.xPosition && p_76600_2_ == this.zPosition;
    }

    /**
     * Generates the height map for a chunk from scratch
     */
    public void generateHeightMap() {}

    public Block getBlock(int p_150810_1_, int p_150810_2_, int p_150810_3_)
    {
        return Block.air;
    }

    public boolean setBlockAndMeta(int p_150807_1_, int p_150807_2_, int p_150807_3_, Block p_150807_4_, int p_150807_5_)
    {
        return true;
    }

    /**
     * Return the metadata corresponding to the given coordinates inside a chunk.
     */
    public int getBlockMetadata(int p_76628_1_, int p_76628_2_, int p_76628_3_)
    {
        return 0;
    }

    /**
     * Set the metadata of a block in the chunk
     */
    public boolean setBlockMetadata(int p_76589_1_, int p_76589_2_, int p_76589_3_, int p_76589_4_)
    {
        return false;
    }

    /**
     * Adds an entity to the chunk. Args: entity
     */
    public void addPlayer(EntityPlayerSP p_76612_1_) {}

    /**
     * Sets the isModified flag for this Chunk
     */
    public void setChunkModified() {}

    /**
     * Returns true if this Chunk needs to be saved
     */
    public boolean needsSaving()
    {
        return false;
    }

    public boolean isEmpty()
    {
        return true;
    }

    /**
     * Returns whether the ExtendedBlockStorages containing levels (in blocks) from arg 1 to arg 2 are fully empty
     * (true) or not (false).
     */
    public boolean getAreLevelsEmpty(int p_76606_1_, int p_76606_2_)
    {
        return true;
    }
}
