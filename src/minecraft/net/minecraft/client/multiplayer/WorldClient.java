package net.minecraft.client.multiplayer;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.player.EntityPlayerSP;
import net.minecraft.util.LongHashMap;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;

public class WorldClient
{
    /** Array list of players in the world. */
    private EntityPlayerSP playerEntity;
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

    public WorldClient()
    {
    	this.playerEntity = null;
        this.blankChunk = new EmptyChunk();
    }

    public void loadChunk(int x, int z)
    {
    	Chunk chunk = new Chunk(x, z);
        this.chunkMapping.add(ChunkCoordIntPair.chunkXZ2Int(x, z), chunk);
        chunk.setLoaded();
    }
    
    public void setRenderer(RenderGlobal r)
    {
    	this.renderer = r;
    }
    
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2)
    {
    	if(this.renderer != null)
    	{
    		this.renderer.markBlockRangeForRenderUpdate(x1, y1, z1, x2, y2, z2);
    	}
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

    /**
     * Updates (and cleans up) entities and tile entities
     */
    public void updateEntities()
    {
        if (this.playerEntity != null)
        {
        	try
            {
        		this.playerEntity.onUpdate();
                this.playerEntity.addedToChunk = true;
                this.playerEntity.chunkCoordX = MathHelper.floor_double(this.playerEntity.posX / 16.0D);
                this.playerEntity.chunkCoordY = MathHelper.floor_double(this.playerEntity.posY / 16.0D);
                this.playerEntity.chunkCoordZ = MathHelper.floor_double(this.playerEntity.posZ / 16.0D);
            }
            catch (Throwable t)
            {
                throw new ReportedException(CrashReport.makeCrashReport(t, "Ticking entity"));
            }
        }
    }

    /**
     * Called to place all entities as part of a world
     */
    public final void spawnEntityInWorld(EntityPlayerSP player)
    {
        int chunkX = MathHelper.floor_double(player.posX / 16.0D);
    	int chunkY = MathHelper.floor_double(player.posY / 16.0D);
        int chunkZ = MathHelper.floor_double(player.posZ / 16.0D);

        if (chunkY < 0)
        {
            chunkY = 0;
        }
        
        this.playerEntity = player;
        player.addedToChunk = true;
        player.chunkCoordX = chunkX;
        player.chunkCoordY = chunkY;
        player.chunkCoordZ = chunkZ;
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

            if (chunk.setBlockAndMetaClient(this, x & 15, y, z & 15, block, metadata))
            {
                if (chunk.getLoaded() && this.renderer != null)
                {
                    this.renderer.markBlockForUpdate(x, y, z);
                }

                return true;
            }
        }
    	
        return false;
    }
    
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
}