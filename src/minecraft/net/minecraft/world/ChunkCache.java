package net.minecraft.world;

import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.chunk.Chunk;

public class ChunkCache
{
    private int chunkX;
    private int chunkZ;
    private Chunk[][] chunkArray;

	public ChunkCache(WorldClient world, int x, int z)
    {
        this.chunkX = (x - 1) - 1 >> 4;
        this.chunkZ = (z - 1) - 1 >> 4;
        int chunkXMAx = (x + 17) + 1 >> 4;
        int chunkZMax = (z + 17) + 1 >> 4;
        this.chunkArray = new Chunk[chunkXMAx - this.chunkX + 1][chunkZMax - this.chunkZ + 1];

        for (int xx = this.chunkX; x <= chunkXMAx; ++x)
        {
            for (int zz = this.chunkZ; z <= chunkZMax; ++z)
            {
                this.chunkArray[xx - this.chunkX][zz - this.chunkZ] = world.provideChunk(xx, zz);
            }
        }
    }

    public Block getBlock(int x, int y, int z)
    {
        if (y >= 0 && y < 256)
        {
            int localChunkX = (x >> 4) - this.chunkX;
            int localChunkZ = (z >> 4) - this.chunkZ;

            if (localChunkX >= 0 && localChunkX < this.chunkArray.length && localChunkZ >= 0 && localChunkZ < this.chunkArray[localChunkX].length)
            {
                Chunk chunk = this.chunkArray[localChunkX][localChunkZ];

                if (chunk != null)
                {
                    return chunk.getBlock(x & 15, y, z & 15);
                }
            }
        }

        return Block.air;
    }

    /**
     * Returns the block metadata at coords x,y,z
     */
    public int getBlockMetadata(int x, int y, int z)
    {
        if (y >= 0 && y < 256)
        {
            int localChunkX = (x >> 4) - this.chunkX;
            int localChunkZ = (z >> 4) - this.chunkZ;
            return this.chunkArray[localChunkX][localChunkZ].getBlockMetadata(x & 15, y, z & 15);
        }

        return 0;
    }
}