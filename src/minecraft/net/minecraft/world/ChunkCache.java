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
    private boolean isEmpty;

	public ChunkCache(WorldClient world, int xmin, int ymin, int zmin, int xmax, int ymax, int zmax)
    {
        this.chunkX = xmin - 1 >> 4;
        this.chunkZ = zmin - 1 >> 4;
        int var9 = xmax + 1 >> 4;
        int var10 = zmax + 1 >> 4;
        this.chunkArray = new Chunk[var9 - this.chunkX + 1][var10 - this.chunkZ + 1];
        this.isEmpty = true;
        int var11;
        int var12;
        Chunk var13;

        for (var11 = this.chunkX; var11 <= var9; ++var11)
        {
            for (var12 = this.chunkZ; var12 <= var10; ++var12)
            {
                var13 = world.provideChunk(var11, var12);

                if (var13 != null)
                {
                    this.chunkArray[var11 - this.chunkX][var12 - this.chunkZ] = var13;
                }
            }
        }

        for (var11 = xmin >> 4; var11 <= xmax >> 4; ++var11)
        {
            for (var12 = zmin >> 4; var12 <= zmax >> 4; ++var12)
            {
                var13 = this.chunkArray[var11 - this.chunkX][var12 - this.chunkZ];

                if (var13 != null && !var13.getAreLevelsEmpty(ymin, ymax))
                {
                    this.isEmpty = false;
                }
            }
        }
    }

    /**
     * set by !chunk.getAreLevelsEmpty
     */
    public boolean extendedLevelsInChunkCache()
    {
        return this.isEmpty;
    }

    public Block getBlock(int x, int y, int z)
    {
        Block block = Block.air;

        if (y >= 0 && y < 256)
        {
            int localChunkX = (x >> 4) - this.chunkX;
            int localChunkZ = (z >> 4) - this.chunkZ;

            if (localChunkX >= 0 && localChunkX < this.chunkArray.length && localChunkZ >= 0 && localChunkZ < this.chunkArray[localChunkX].length)
            {
                Chunk chunk = this.chunkArray[localChunkX][localChunkZ];

                if (chunk != null)
                {
                    block = chunk.getBlock(x & 15, y, z & 15);
                }
            }
        }

        return block;
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