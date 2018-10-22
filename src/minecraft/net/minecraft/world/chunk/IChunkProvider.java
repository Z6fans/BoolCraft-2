package net.minecraft.world.chunk;

public interface IChunkProvider
{
    /**
     * Checks to see if a chunk exists at x, y
     */
    boolean chunkExists(int p_73149_1_, int p_73149_2_);

    /**
     * Will return back a chunk, if it doesn't exist and its not a MP client it will generates all the blocks for the
     * specified chunk from the map seed and chunk seed
     */
    Chunk provideChunk(int x, int y);

    /**
     * loads or generates the chunk at the chunk location specified
     */
    Chunk loadChunk(int p_73158_1_, int p_73158_2_);

    /**
     * Unloads chunks that are marked to be unloaded. This is not guaranteed to unload every such chunk.
     */
    boolean unloadQueuedChunks();
}
