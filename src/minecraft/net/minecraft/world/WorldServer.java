package net.minecraft.world;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.player.EntityPlayerMP;
import net.minecraft.server.PlayerChunkLoadManager;
import net.minecraft.util.LongHashMap;
import net.minecraft.util.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.storage.SaveHandler;

public class WorldServer extends World<EntityPlayerMP>
{
    private final PlayerChunkLoadManager thePlayerManager;
    private Set<NextTickListEntry> pendingTickListEntriesHashSet;

    /** All work to do in future ticks. */
    private TreeSet<NextTickListEntry> pendingTickListEntriesTreeSet;

    /** Whether or not level saving is enabled */
    private boolean levelSaving;
    private int updateEntityTick;
    private List<NextTickListEntry> pendingTickListEntriesThisTick = new ArrayList<NextTickListEntry>();
    
    /** Positions to update */
    private Set<ChunkCoordIntPair> activeChunkSet = new HashSet<ChunkCoordIntPair>();
    
    /**
     * Contains the current Linear Congruential Generator seed for block updates. Used with an A value of 3 and a C
     * value of 0x3c6ef35f, producing a highly planar series of values ill-suited for choosing random blocks in a
     * 16x128x16 field.
     */
    private int updateLCG = (new Random()).nextInt();
    private final SaveHandler saveHandler;
    
    private static final Logger logger = LogManager.getLogger();

    /**
     * used by unload100OldestChunks to iterate the loadedChunkHashMap for unload (underlying assumption, first in,
     * first out)
     */
    private Set<Long> chunksToUnload = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
    private AnvilChunkLoader currentChunkLoader;
    private LongHashMap<Chunk<EntityPlayerMP>> loadedChunkHashMap = new LongHashMap<Chunk<EntityPlayerMP>>();
    private List<Chunk<EntityPlayerMP>> loadedChunks = new ArrayList<Chunk<EntityPlayerMP>>();

    public WorldServer(SaveHandler sh)
    {
        super(sh.loadWorldInfo());
        this.saveHandler = sh;
        this.currentChunkLoader =new AnvilChunkLoader(this.saveHandler.getWorldDirectory());
        this.thePlayerManager = new PlayerChunkLoadManager(this);

        if (this.pendingTickListEntriesHashSet == null)
        {
            this.pendingTickListEntriesHashSet = new HashSet<NextTickListEntry>();
        }

        if (this.pendingTickListEntriesTreeSet == null)
        {
            this.pendingTickListEntriesTreeSet = new TreeSet<NextTickListEntry>();
        }
    }

    /**
     * Runs a single tick for the world
     */
    public void tick()
    {
    	if (!this.levelSaving)
        {
            for (int var1 = 0; var1 < 100; ++var1)
            {
                if (!this.chunksToUnload.isEmpty())
                {
                    Long var2 = (Long)this.chunksToUnload.iterator().next();
                    Chunk<EntityPlayerMP> var3 = this.loadedChunkHashMap.getValueByKey(var2.longValue());

                    if (var3 != null)
                    {
                        this.safeSaveChunk(var3);
                        this.loadedChunks.remove(var3);
                    }

                    this.chunksToUnload.remove(var2);
                    this.loadedChunkHashMap.remove(var2.longValue());
                }
            }
        }
    	
        this.worldInfo.incrementTotalWorldTime(this.worldInfo.getWorldTotalTime() + 1L);
        
        int numEntries = this.pendingTickListEntriesTreeSet.size();

        if (numEntries != this.pendingTickListEntriesHashSet.size())
        {
            throw new IllegalStateException("TickNextTick list out of synch");
        }
        else
        {
            if (numEntries > 1000)
            {
                numEntries = 1000;
            }

            for (int i = 0; i < numEntries; ++i)
            {
            	NextTickListEntry entry = (NextTickListEntry)this.pendingTickListEntriesTreeSet.first();

                if (entry.scheduledTime > this.worldInfo.getWorldTotalTime())
                {
                    break;
                }

                this.pendingTickListEntriesTreeSet.remove(entry);
                this.pendingTickListEntriesHashSet.remove(entry);
                this.pendingTickListEntriesThisTick.add(entry);
            }
            
            Iterator<NextTickListEntry> entryListIterator = this.pendingTickListEntriesThisTick.iterator();

            while (entryListIterator.hasNext())
            {
            	NextTickListEntry entry = (NextTickListEntry)entryListIterator.next();
                entryListIterator.remove();
                byte var5 = 0;

                if (this.checkChunksExist(entry.xCoord - var5, entry.yCoord - var5, entry.zCoord - var5, entry.xCoord + var5, entry.yCoord + var5, entry.zCoord + var5))
                {
                    Block block = this.getBlock(entry.xCoord, entry.yCoord, entry.zCoord);

                    if (!block.isReplaceable() && Block.isEqualTo(block, entry.getBlock()))
                    {
                        try
                        {
                            block.updateTick(this, entry.xCoord, entry.yCoord, entry.zCoord);
                        }
                        catch (Throwable t)
                        {
                            throw new ReportedException(CrashReport.makeCrashReport(t, "Exception while ticking a block"));
                        }
                    }
                }
                else
                {
                    this.scheduleBlockUpdate(entry.xCoord, entry.yCoord, entry.zCoord, entry.getBlock(), 0);
                }
            }
            this.pendingTickListEntriesThisTick.clear();
        }
        
        this.activeChunkSet.clear();

        if (this.playerEntity != null)
        {
        	int chunkX = MathHelper.floor_double(this.playerEntity.posX / 16.0D);
            int chunkZ = MathHelper.floor_double(this.playerEntity.posZ / 16.0D);

            for (int xOff = -16; xOff <= 16; ++xOff)
            {
                for (int zOff = -16; zOff <= 16; ++zOff)
                {
                    this.activeChunkSet.add(new ChunkCoordIntPair(xOff + chunkX, zOff + chunkZ));
                }
            }
        }
        Iterator<ChunkCoordIntPair> var3 = this.activeChunkSet.iterator();

        while (var3.hasNext())
        {
            ChunkCoordIntPair var4 = (ChunkCoordIntPair)var3.next();
            int var5 = var4.chunkXPos * 16;
            int var6 = var4.chunkZPos * 16;
            Chunk<EntityPlayerMP> chunk = this.provideChunk(var4.chunkXPos, var4.chunkZPos);
            chunk.setLoaded();

            if (this.rand.nextInt(16) == 0)
            {
                this.updateLCG = this.updateLCG * 3 + 1013904223;
            }
            ExtendedBlockStorage[] storageArray = chunk.getBlockStorageArray();

            for (int i = 0; i < storageArray.length; ++i)
            {
                ExtendedBlockStorage storage = storageArray[i];

                if (storage != null && storage.getNeedsRandomTick())
                {
                    for (int j = 0; j < 3; ++j)
                    {
                        this.updateLCG = this.updateLCG * 3 + 1013904223;
                        int var13 = this.updateLCG >> 2;
                        int var14 = var13 & 15;
                        int var15 = var13 >> 8 & 15;
                        int var16 = var13 >> 16 & 15;
                        Block var17 = storage.getBlock(var14, var16, var15);

                        if (var17.getTickRandomly())
                        {
                            var17.updateTick(this, var14 + var5, var16 + storage.getYLocation(), var15 + var6);
                        }
                    }
                }
            }
        }
        
        this.thePlayerManager.updatePlayerInstances();
    }

    /**
     * Schedules a tick to a block with a delay (Most commonly the tick rate)
     */
    public void scheduleBlockUpdate(int x, int y, int z, Block block, int delay)
    {
    	if (this.checkChunksExist(x, y, z, x, y, z))
        {
        	NextTickListEntry entry = new NextTickListEntry(x, y, z, block);
            if (!block.isReplaceable())
            {
                entry.setScheduledTime((long)delay + this.worldInfo.getWorldTotalTime());
                entry.setPriority(0);
            }

            if (!this.pendingTickListEntriesHashSet.contains(entry))
            {
                this.pendingTickListEntriesHashSet.add(entry);
                this.pendingTickListEntriesTreeSet.add(entry);
            }
        }
    }

    public void addBlockUpdateFromSave(int x, int y, int z, Block block, int delay, int priority)
    {
        NextTickListEntry entry = new NextTickListEntry(x, y, z, block);
        entry.setPriority(priority);

        if (!block.isReplaceable())
        {
            entry.setScheduledTime((long)delay + this.worldInfo.getWorldTotalTime());
        }

        if (!this.pendingTickListEntriesHashSet.contains(entry))
        {
            this.pendingTickListEntriesHashSet.add(entry);
            this.pendingTickListEntriesTreeSet.add(entry);
        }
    }

    /**
     * Updates (and cleans up) entities and tile entities
     */
    public void updateEntities()
    {
        if (this.playerEntity == null)
        {
            if (this.updateEntityTick++ >= 1200)
            {
                return;
            }
        }
        else
        {
        	this.updateEntityTick = 0;
        }

        super.updateEntities();
    }

    public List<NextTickListEntry> getPendingBlockUpdates(Chunk<EntityPlayerMP> chunk)
    {
        ChunkCoordIntPair chunkCoords = chunk.getChunkCoordIntPair();
        int xmin = (chunkCoords.chunkXPos << 4) - 2;
        int xmax = xmin + 16 + 2;
        int zmin = (chunkCoords.chunkZPos << 4) - 2;
        int zmax = zmin + 16 + 2;
        
        ArrayList<NextTickListEntry> updateList = new ArrayList<NextTickListEntry>();

        for (int i = 0; i < 2; ++i)
        {
            Iterator<NextTickListEntry> iter;

            if (i == 0)
            {
                iter = this.pendingTickListEntriesTreeSet.iterator();
            }
            else
            {
                iter = this.pendingTickListEntriesThisTick.iterator();
            }

            while (iter.hasNext())
            {
                NextTickListEntry entry = (NextTickListEntry)iter.next();

                if (entry.xCoord >= xmin && entry.xCoord < xmax && entry.zCoord >= zmin && entry.zCoord < zmax)
                {
                    updateList.add(entry);
                }
            }
        }

        return updateList;
    }

    protected void initialize()
    {
        if (this.pendingTickListEntriesHashSet == null)
        {
            this.pendingTickListEntriesHashSet = new HashSet<NextTickListEntry>();
        }

        if (this.pendingTickListEntriesTreeSet == null)
        {
            this.pendingTickListEntriesTreeSet = new TreeSet<NextTickListEntry>();
        }

        super.initialize();
    }

    /**
     * Saves all chunks to disk while updating progress bar.
     */
    public void saveAllChunks() throws MinecraftException
    {
        if (!this.levelSaving)
        {
        	this.checkSessionLock();
            this.saveHandler.saveWorldInfo(this.worldInfo);

            ArrayList<Chunk<EntityPlayerMP>> chunkList = Lists.newArrayList(this.loadedChunks);

            for (int i = 0; i < chunkList.size(); ++i)
            {
                Chunk<EntityPlayerMP> chunk = chunkList.get(i);

                if (chunk.needsSaving())
                {
                    this.safeSaveChunk(chunk);
                    chunk.isModified = false;
                }
            }
            
            Iterator<Chunk<EntityPlayerMP>> var4 = Lists.newArrayList(this.getLoadedChunks()).iterator();

            while (var4.hasNext())
            {
                Chunk<EntityPlayerMP> chunk = var4.next();

                if (chunk != null && !this.thePlayerManager.doesPlayerInstanceExist(chunk.xPosition, chunk.zPosition))
                {
                    this.unloadChunksIfNotNearSpawn(chunk.xPosition, chunk.zPosition);
                }
            }
        }
    }

    /**
     * Syncs all changes to disk and wait for completion.
     */
    public void flush()
    {
        this.saveHandler.flush();
    }

    public PlayerChunkLoadManager getPlayerManager()
    {
        return this.thePlayerManager;
    }

	protected void markBlockForUpdate(int x, int y, int z)
	{
		this.thePlayerManager.markBlockForUpdate(x, y, z);
	}
	
	private void notifyBlockOfNeighborChange(int x, int y, int z, final Block neighborBlock)
    {
		try
        {
        	this.getBlock(x, y, z).onNeighborBlockChange(this, x, y, z, neighborBlock);
        }
        catch (Throwable t)
        {
            throw new ReportedException(CrashReport.makeCrashReport(t, "Exception while updating neighbors"));
        }
    }
	
	/**
     * Finds the highest block on the x, z coordinate that is solid and returns its y coord. Args x, z
     */
    public int getTopSolidOrLiquidBlock(int x, int z)
    {
        Chunk<EntityPlayerMP> chunk = this.provideChunk(x >> 4, z >> 4);
        int y = chunk.getTopFilledSegment() + 15;
        x &= 15;

        for (z &= 15; y > 0; --y)
        {
            if (chunk.getBlock(x, y, z).isSolid())
            {
                return y + 1;
            }
        }

        return -1;
    }
    
    /**
     * Checks whether the session lock file was modified by another process
     */
    public void checkSessionLock() throws MinecraftException
    {
        this.saveHandler.checkSessionLock();
    }
    
    public void spawnPlayerInWorld(Minecraft mc){
    	EntityPlayerMP player = new EntityPlayerMP(mc);
    	this.spawnEntityInWorld(player);
    	this.getPlayerManager().addPlayer(player);
        this.loadChunk((int)player.posX >> 4, (int)player.posZ >> 4);
    }
    
    public void notifyBlocksOfNeighborChange(int x, int y, int z, Block block)
    {
        this.notifyBlockOfNeighborChange(x - 1, y, z, block);
        this.notifyBlockOfNeighborChange(x + 1, y, z, block);
        this.notifyBlockOfNeighborChange(x, y - 1, z, block);
        this.notifyBlockOfNeighborChange(x, y + 1, z, block);
        this.notifyBlockOfNeighborChange(x, y, z - 1, block);
        this.notifyBlockOfNeighborChange(x, y, z + 1, block);
    }
    
    /**
     * Populates chunk with ores etc etc
     */
    public void populate(int x, int z)
    {
        Chunk<EntityPlayerMP> chunk = this.provideChunk(x, z);

        if (!chunk.isTerrainPopulated)
        {
            chunk.setChunkModified();
        }
    }
	
	/**
     * Checks to see if a chunk exists at x, y
     */
    public boolean chunkExists(int p_73149_1_, int p_73149_2_)
    {
        return this.loadedChunkHashMap.containsItem(ChunkCoordIntPair.chunkXZ2Int(p_73149_1_, p_73149_2_));
    }

    public List<Chunk<EntityPlayerMP>> getLoadedChunks()
    {
        return this.loadedChunks;
    }

    /**
     * marks chunk for unload by "unload100OldestChunks"  if there is no spawn point, or if the center of the chunk is
     * outside 200 blocks (x or z) of the spawn
     */
    public void unloadChunksIfNotNearSpawn(int chunkX, int chunkZ)
    {
        int x = chunkX * 16 + 8;
        int z = chunkZ * 16 + 8;

        if (x < -128 || x > 128 || z < -128 || z > 128)
        {
            this.chunksToUnload.add(Long.valueOf(ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ)));
        }
    }

    /**
     * loads or generates the chunk at the chunk location specified
     */
    public Chunk<EntityPlayerMP> loadChunk(int x, int z)
    {
        long posHash = ChunkCoordIntPair.chunkXZ2Int(x, z);
        this.chunksToUnload.remove(Long.valueOf(posHash));
        Chunk<EntityPlayerMP> newChunk = this.loadedChunkHashMap.getValueByKey(posHash);

        if (newChunk == null)
        {
        	if(this.currentChunkLoader != null)
        	{
        		try
        		{
        			newChunk = this.currentChunkLoader.loadChunk(this, x, z);
        		}
                catch (Exception e)
                {
                    logger.error("Couldn\'t load chunk", e);
                }
        	}

            if (newChunk == null)
            {
            	try
                {
                	newChunk = new Chunk<EntityPlayerMP>(this, x, z);

                    for (int y = 0; y < 5; ++y)
                    {
                        ExtendedBlockStorage storage = newChunk.getBlockStorageArray()[0];

                        if (storage == null)
                        {
                            storage = new ExtendedBlockStorage(y);
                            newChunk.getBlockStorageArray()[0] = storage;
                        }

                        for (int localX = 0; localX < 16; ++localX)
                        {
                            for (int localZ = 0; localZ < 16; ++localZ)
                            {
                                storage.setBlock(localX, y, localZ, Block.stone);
                                storage.setExtBlockMetadata(localX, y, localZ, 0);
                            }
                        }
                    }
                }
                catch (Throwable t)
                {
                    throw new ReportedException(CrashReport.makeCrashReport(t, "Exception generating new chunk at " + x + ", " + z));
                }
            }

            this.loadedChunkHashMap.add(posHash, newChunk);
            this.loadedChunks.add(newChunk);
            newChunk.populateChunk(this, x, z);
        }

        return newChunk;
    }

    /**
     * Will return back a chunk, if it doesn't exist and its not a MP client it will generates all the blocks for the
     * specified chunk from the map seed and chunk seed
     */
    public Chunk<EntityPlayerMP> provideChunk(int p_73154_1_, int p_73154_2_)
    {
        Chunk<EntityPlayerMP> var3 = this.loadedChunkHashMap.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(p_73154_1_, p_73154_2_));
        return var3 == null ? this.loadChunk(p_73154_1_, p_73154_2_) : var3;
    }

    /**
     * used by saveChunks, but catches any exceptions if the save fails.
     */
    private void safeSaveChunk(Chunk<EntityPlayerMP> chunk)
    {
        if (this.currentChunkLoader != null)
        {
            try
            {
                this.currentChunkLoader.saveChunk(this, chunk);
            }
            catch (IOException var3)
            {
                logger.error("Couldn\'t save chunk", var3);
            }
            catch (MinecraftException var4)
            {
                logger.error("Couldn\'t save chunk; already in use by another instance of Minecraft?", var4);
            }
        }
    }
}