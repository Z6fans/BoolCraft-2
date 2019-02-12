package net.minecraft.world;

import net.minecraft.block.Block;
import net.minecraft.client.WorldClient;
import net.minecraft.world.chunk.Chunk;

public class ChunkCache
{
    private int chunkX;
    private int chunkZ;
    private Chunk[][] chunkArray;

    /** True if the chunk cache is empty. */
    private boolean isNotEmpty;

	public ChunkCache(WorldClient world, int x, int y, int z)
    {
        this.chunkX = x - 2 >> 4;
        this.chunkZ = z - 2 >> 4;
        int xmax = x + 18 >> 4;
        int zmax = z + 18 >> 4;
        this.chunkArray = new Chunk[xmax - this.chunkX + 1][zmax - this.chunkZ + 1];
        this.isNotEmpty = false;

        for (int xx = this.chunkX; xx <= xmax; ++xx)
        {
            for (int zz = this.chunkZ; zz <= zmax; ++zz)
            {
                Chunk chunk = world.provideChunk(xx, zz);

                if (chunk != null)
                {
                    this.chunkArray[xx - this.chunkX][zz - this.chunkZ] = chunk;
                }
            }
        }

        for (int xx = x - 1 >> 4; xx <= x + 17 >> 4; ++xx)
        {
            for (int zz = z - 1 >> 4; zz <= z + 18 >> 4; ++zz)
            {
                Chunk chunk = this.chunkArray[xx - this.chunkX][zz - this.chunkZ];

                if (chunk != null && !chunk.getAreLevelsEmpty(y - 1, y + 17))
                {
                    this.isNotEmpty = true;
                }
            }
        }
    }

    /**
     * set by !chunk.getAreLevelsEmpty
     */
    public boolean extendedLevelsInChunkCache()
    {
        return this.isNotEmpty;
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