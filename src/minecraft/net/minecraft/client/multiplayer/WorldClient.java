package net.minecraft.client.multiplayer;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.player.EntityPlayerSP;
import net.minecraft.world.World;

public class WorldClient extends World<EntityPlayerSP>
{
    /** The ChunkProviderClient instance */
    private ChunkProviderClient clientChunkProvider;
    private RenderGlobal renderer;

    public WorldClient()
    {
        super(null);
        this.clientChunkProvider = new ChunkProviderClient(this);
        this.chunkProvider = this.clientChunkProvider;
    }

    /**
     * Runs a single tick for the world
     */
    public void tick()
    {
        this.incrementTotalWorldTime(this.getTotalWorldTime() + 1L);
        this.clientChunkProvider.unloadQueuedChunks();
    }

    public void doPreChunk(int chunkX, int chunkZ, boolean doLoad)
    {
        if (doLoad)
        {
            this.clientChunkProvider.loadChunk(chunkX, chunkZ);
        }
        else
        {
            this.clientChunkProvider.unloadChunk(chunkX, chunkZ);
            this.markBlockRangeForRenderUpdate(chunkX * 16, 0, chunkZ * 16, chunkX * 16 + 15, 256, chunkZ * 16 + 15);
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
}