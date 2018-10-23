package net.minecraft.world;

import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.player.EntityPlayerSP;
import net.minecraft.world.chunk.Chunk;

public class ChunkCache
{
    private int chunkX;
    private int chunkZ;
    private Chunk<EntityPlayerSP>[][] chunkArray;

    /** True if the chunk cache is empty. */
    private boolean isEmpty;

    @SuppressWarnings("unchecked")
	public ChunkCache(WorldClient world, int p_i1964_2_, int p_i1964_3_, int p_i1964_4_, int p_i1964_5_, int p_i1964_6_, int p_i1964_7_, int p_i1964_8_)
    {
        this.chunkX = p_i1964_2_ - p_i1964_8_ >> 4;
        this.chunkZ = p_i1964_4_ - p_i1964_8_ >> 4;
        int var9 = p_i1964_5_ + p_i1964_8_ >> 4;
        int var10 = p_i1964_7_ + p_i1964_8_ >> 4;
        this.chunkArray = (Chunk<EntityPlayerSP>[][])(new Chunk[var9 - this.chunkX + 1][var10 - this.chunkZ + 1]);
        this.isEmpty = true;
        int var11;
        int var12;
        Chunk<EntityPlayerSP> var13;

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

        for (var11 = p_i1964_2_ >> 4; var11 <= p_i1964_5_ >> 4; ++var11)
        {
            for (var12 = p_i1964_4_ >> 4; var12 <= p_i1964_7_ >> 4; ++var12)
            {
                var13 = this.chunkArray[var11 - this.chunkX][var12 - this.chunkZ];

                if (var13 != null && !var13.getAreLevelsEmpty(p_i1964_3_, p_i1964_6_))
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
                Chunk<EntityPlayerSP> chunk = this.chunkArray[localChunkX][localChunkZ];

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