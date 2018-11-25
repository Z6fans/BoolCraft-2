package net.minecraft.world;

import net.minecraft.block.Block;
import net.minecraft.world.chunk.Chunk;

public abstract class World
{
    public final Block getBlock(int x, int y, int z)
    {
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000 && y >= 0 && y < 256)
        {
            Chunk chunk = this.provideChunk(x >> 4, z >> 4);
            return chunk.getBlock(x & 15, y, z & 15);
        }
        else
        {
            return Block.air;
        }
    }

    /**
     * Returns the block metadata at coords x,y,z
     */
    public final int getBlockMetadata(int x, int y, int z)
    {
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000 && y >= 0 && y < 256)
        {
        	Chunk chunk = this.provideChunk(x >> 4, z >> 4);
            return chunk.getBlockMetadata(x & 15, y, z & 15);
        }
        else
        {
            return 0;
        }
    }

    /**
     * Returns back a chunk looked up by chunk coordinates Args: x, z
     */
    public abstract Chunk provideChunk(int x, int z);
}
