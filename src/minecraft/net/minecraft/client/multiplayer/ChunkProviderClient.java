package net.minecraft.client.multiplayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.player.EntityPlayerSP;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunkProvider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkProviderClient implements IChunkProvider
{
    /**
     * The completely empty chunk used by ChunkProviderClient when chunkMapping doesn't contain the requested
     * coordinates.
     */
    private Chunk<EntityPlayerSP> blankChunk;

    /**
     * The mapping between ChunkCoordinates and Chunks that ChunkProviderClient maintains.
     */
    private LongHashMap chunkMapping = new LongHashMap();

    /**
     * This may have been intended to be an iterable version of all currently loaded chunks (MultiplayerChunkCache),
     * with identical contents to chunkMapping's values. However it is never actually added to.
     */
    private List chunkListing = new ArrayList();

    /** Reference to the World object. */
    private WorldClient worldObj;
    private static final String __OBFID = "CL_00000880";

    public ChunkProviderClient(WorldClient world)
    {
        this.blankChunk = new EmptyChunk(world);
        this.worldObj = world;
    }

    /**
     * Checks to see if a chunk exists at x, z
     */
    public boolean chunkExists(int p_73149_1_, int p_73149_2_)
    {
        return true;
    }

    /**
     * Unload chunk from ChunkProviderClient's hashmap. Called in response to a Packet50PreChunk with its mode field set
     * to false
     */
    public void unloadChunk(int x, int z)
    {
        Chunk<EntityPlayerSP> chunk = this.provideChunk(x, z);
        this.chunkMapping.remove(ChunkCoordIntPair.chunkXZ2Int(x, z));
        this.chunkListing.remove(chunk);
    }

    /**
     * loads or generates the chunk at the chunk location specified
     */
    public Chunk<EntityPlayerSP> loadChunk(int x, int z)
    {
        Chunk<EntityPlayerSP> chunk = new Chunk<EntityPlayerSP>(this.worldObj, x, z);
        this.chunkMapping.add(ChunkCoordIntPair.chunkXZ2Int(x, z), chunk);
        this.chunkListing.add(chunk);
        return chunk;
    }

    /**
     * Will return back a chunk, if it doesn't exist and its not a MP client it will generates all the blocks for the
     * specified chunk from the map seed and chunk seed
     */
    public Chunk<EntityPlayerSP> provideChunk(int x, int z)
    {
        Chunk<EntityPlayerSP> chunk = (Chunk<EntityPlayerSP>)this.chunkMapping.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(x, z));
        return chunk == null ? this.blankChunk : chunk;
    }

    /**
     * Unloads chunks that are marked to be unloaded. This is not guaranteed to unload every such chunk.
     */
    public boolean unloadQueuedChunks()
    {
        Iterator<Chunk<EntityPlayerSP>> chunks = this.chunkListing.iterator();

        while (chunks.hasNext())
        {
            chunks.next().setLoaded();
        }

        return false;
    }
}
