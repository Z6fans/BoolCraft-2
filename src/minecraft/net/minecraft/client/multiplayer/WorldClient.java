package net.minecraft.client.multiplayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.player.EntityPlayerSP;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;

public class WorldClient extends World<EntityPlayerSP>
{
    private RenderGlobal renderer;
    
    /**
     * The completely empty chunk used by ChunkProviderClient when chunkMapping doesn't contain the requested
     * coordinates.
     */
    private Chunk blankChunk;

    /**
     * The mapping between ChunkCoordinates and Chunks that ChunkProviderClient maintains.
     */
    private LongHashMap<Chunk> chunkMapping = new LongHashMap<Chunk>();

    /**
     * This may have been intended to be an iterable version of all currently loaded chunks (MultiplayerChunkCache),
     * with identical contents to chunkMapping's values. However it is never actually added to.
     */
    private List<Chunk> chunkListing = new ArrayList<Chunk>();

    public WorldClient()
    {
        super(null);
        this.blankChunk = new EmptyChunk();
    }

    /**
     * Runs a single tick for the world
     */
    public void tick()
    {
        this.incrementTotalWorldTime(this.getTotalWorldTime() + 1L);
        Iterator<Chunk> chunks = this.chunkListing.iterator();

        while (chunks.hasNext())
        {
            chunks.next().setLoaded();
        }
    }

    public void doPreChunk(int x, int z, boolean doLoad)
    {
        if (doLoad)
        {
        	Chunk chunk = new Chunk(x, z);
            this.chunkMapping.add(ChunkCoordIntPair.chunkXZ2Int(x, z), chunk);
            this.chunkListing.add(chunk);
        }
        else
        {
        	Chunk chunk = this.provideChunk(x, z);
            this.chunkMapping.remove(ChunkCoordIntPair.chunkXZ2Int(x, z));
            this.chunkListing.remove(chunk);
            this.markBlockRangeForRenderUpdate(x * 16, 0, z * 16, x * 16 + 15, 256, z * 16 + 15);
        }
    }
    
    public void setRenderer(RenderGlobal r)
    {
    	this.renderer = r;
    }
    
    protected void markBlockForUpdate(int x, int y, int z)
    {
    	if(this.renderer != null)
    	{
    		this.renderer.markBlockForUpdate(x, y, z);
    	}
    }
    
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2)
    {
    	if(this.renderer != null)
    	{
    		this.renderer.markBlockRangeForRenderUpdate(x1, y1, z1, x2, y2, z2);
    	}
    }

	public void notifyBlocksOfNeighborChange(int x, int y, int z, Block block){}

	public boolean chunkExists(int x, int z)
	{
		return true;
	}

    /**
     * Will return back a chunk, if it doesn't exist and its not a MP client it will generates all the blocks for the
     * specified chunk from the map seed and chunk seed
     */
    public Chunk provideChunk(int x, int z)
    {
        Chunk chunk = this.chunkMapping.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(x, z));
        return chunk == null ? this.blankChunk : chunk;
    }
}