package net.minecraft.world;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.player.EntityPlayerMP;
import net.minecraft.server.PlayerChunkLoadManager;
import net.minecraft.util.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkProviderServer;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.storage.SaveHandler;

public class WorldServer extends World<EntityPlayerMP>
{
    private final PlayerChunkLoadManager thePlayerManager;
    private Set<NextTickListEntry> pendingTickListEntriesHashSet;

    /** All work to do in future ticks. */
    private TreeSet<NextTickListEntry> pendingTickListEntriesTreeSet;
    public ChunkProviderServer theChunkProviderServer;

    /** Whether or not level saving is enabled */
    public boolean levelSaving;
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

    public WorldServer(SaveHandler sh)
    {
        super(sh.loadWorldInfo());
        this.saveHandler = sh;
        this.theChunkProviderServer = new ChunkProviderServer(this, new AnvilChunkLoader(this.saveHandler.getWorldDirectory()));
        this.chunkProvider = this.theChunkProviderServer;
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
        this.theChunkProviderServer.unloadQueuedChunks();
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
        if (this.theChunkProviderServer.canSave())
        {
        	this.checkSessionLock();
            this.saveHandler.saveWorldInfo(this.worldInfo);

            this.theChunkProviderServer.saveChunks(true);
            Iterator<Chunk<EntityPlayerMP>> var4 = Lists.newArrayList(this.theChunkProviderServer.func_152380_a()).iterator();

            while (var4.hasNext())
            {
                Chunk<EntityPlayerMP> chunk = var4.next();

                if (chunk != null && !this.thePlayerManager.doesPlayerInstanceExist(chunk.xPosition, chunk.zPosition))
                {
                    this.theChunkProviderServer.unloadChunksIfNotNearSpawn(chunk.xPosition, chunk.zPosition);
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
		Block block = this.getBlock(x, y, z);

        try
        {
            block.onNeighborBlockChange(this, x, y, z, neighborBlock);
        }
        catch (Throwable t)
        {
            throw new ReportedException(CrashReport.makeCrashReport(t, "Exception while updating neighbours"));
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
        this.theChunkProviderServer.loadChunk((int)player.posX >> 4, (int)player.posZ >> 4);
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
}