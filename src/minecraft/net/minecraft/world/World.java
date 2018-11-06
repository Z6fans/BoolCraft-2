package net.minecraft.world;

import net.minecraft.block.Block;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.world.chunk.Chunk;

public abstract class World
{
    public final Block getBlock(int x, int y, int z)
    {
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000 && y >= 0 && y < 256)
        {
            Chunk chunk = null;

            try
            {
                chunk = this.provideChunk(x >> 4, z >> 4);
                return chunk.getBlock(x & 15, y, z & 15);
            }
            catch (Throwable t)
            {
                throw new ReportedException(CrashReport.makeCrashReport(t, "Exception getting block type in world, did " + (chunk == null ? "not " : "") + "find chunk"));
            }
        }
        else
        {
            return Block.air;
        }
    }

    /**
     * Sets the block ID and metadata at a given location. Args: X, Y, Z, new block ID, new metadata, flags. Flag 1 will
     * cause a block update. Flag 2 will send the change to clients (you almost always want this). Flag 4 prevents the
     * block from being re-rendered, if this is a client world. Flags can be added together.
     */
    public final boolean setBlock(int x, int y, int z, Block block, int metadata)
    {
    	if (x >= -30000000 && y >= 0 && z >= -30000000 && x < 30000000 && y < 256 && z < 30000000)
        {
    		Chunk chunk = this.provideChunk(x >> 4, z >> 4);
            Block oldBlock = chunk.getBlock(x & 15, y, z & 15);

            if (chunk.setBlockAndMeta(this, x & 15, y, z & 15, block, metadata))
            {
                if (chunk.getLoaded())
                {
                    this.markBlockForUpdate(x, y, z);
                }

                this.notifyBlocksOfNeighborChange(x, y, z, oldBlock);
                return true;
            }
        }
    	
        return false;
    }

    /**
     * Returns the block metadata at coords x,y,z
     */
    public final int getBlockMetadata(int p_72805_1_, int p_72805_2_, int p_72805_3_)
    {
        if (p_72805_1_ >= -30000000 && p_72805_3_ >= -30000000 && p_72805_1_ < 30000000 && p_72805_3_ < 30000000)
        {
            if (p_72805_2_ < 0)
            {
                return 0;
            }
            else if (p_72805_2_ >= 256)
            {
                return 0;
            }
            else
            {
                Chunk var4 = this.provideChunk(p_72805_1_ >> 4, p_72805_3_ >> 4);
                p_72805_1_ &= 15;
                p_72805_3_ &= 15;
                return var4.getBlockMetadata(p_72805_1_, p_72805_2_, p_72805_3_);
            }
        }
        else
        {
            return 0;
        }
    }

    /**
     * Returns whether a block exists at world coordinates x, y, z
     */
    public abstract boolean chunkExists(int x, int z);

    /**
     * Returns back a chunk looked up by chunk coordinates Args: x, z
     */
    public abstract Chunk provideChunk(int x, int z);

    protected abstract void markBlockForUpdate(int x, int y, int z);

    public abstract void notifyBlocksOfNeighborChange(int x, int y, int z, Block block);
}
