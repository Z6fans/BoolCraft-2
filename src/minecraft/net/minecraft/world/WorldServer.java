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
import net.minecraft.world.storage.WorldInfo;

public class WorldServer extends World
{
    /** Array list of players in the world. */
    private EntityPlayerMP playerEntity;
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
    private LongHashMap<Chunk> loadedChunkHashMap = new LongHashMap<Chunk>();
    private List<Chunk> loadedChunks = new ArrayList<Chunk>();

    /** RNG for World. */
    private Random rand = new Random();

    /**
     * holds information about a world (size on disk, time, spawn point, seed, ...)
     */
    private WorldInfo worldInfo;

    public WorldServer(SaveHandler sh)
    {
    	this.playerEntity = null;
        this.worldInfo = sh.loadWorldInfo();

        if (this.worldInfo == null)
        {
            this.worldInfo = new WorldInfo();
        }

        if (!this.worldInfo.isInitialized())
        {
            try
            {
                this.initialize();
            }
            catch (Throwable t)
            {
                throw new ReportedException(CrashReport.makeCrashReport(t, "Exception initializing level"));
            }

            this.worldInfo.setServerInitialized(true);
        }
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
                    Chunk var3 = this.loadedChunkHashMap.getValueByKey(var2.longValue());

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
            Chunk chunk = this.provideChunk(var4.chunkXPos, var4.chunkZPos);
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

        if (this.playerEntity != null)
        {
        	try
            {
        		int playerX = MathHelper.floor_double(this.playerEntity.posX);
                int playerZ = MathHelper.floor_double(this.playerEntity.posZ);
                int r = 32;

                if (this.checkChunksExist(playerX - r, 0, playerZ - r, playerX + r, 0, playerZ + r))
                {
                	this.playerEntity.onUpdate();
                }
            }
            catch (Throwable t)
            {
                throw new ReportedException(CrashReport.makeCrashReport(t, "Ticking entity"));
            }
        }
    }

    /**
     * Checks between a min and max all the chunks inbetween actually exist. Args: minX, minY, minZ, maxX, maxY, maxZ
     */
    private final boolean checkChunksExist(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
    {
        if (maxY >= 0 && minY < 256)
        {
            minX >>= 4;
            minZ >>= 4;
            maxX >>= 4;
            maxZ >>= 4;

            for (int x = minX; x <= maxX; ++x)
            {
                for (int z = minZ; z <= maxZ; ++z)
                {
                    if (!this.chunkExists(x, z))
                    {
                        return false;
                    }
                }
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    public List<NextTickListEntry> getPendingBlockUpdates(Chunk chunk)
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

    private void initialize()
    {
        if (this.pendingTickListEntriesHashSet == null)
        {
            this.pendingTickListEntriesHashSet = new HashSet<NextTickListEntry>();
        }

        if (this.pendingTickListEntriesTreeSet == null)
        {
            this.pendingTickListEntriesTreeSet = new TreeSet<NextTickListEntry>();
        }

        this.worldInfo.setServerInitialized(true);
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

            ArrayList<Chunk> chunkList = Lists.newArrayList(this.loadedChunks);

            for (int i = 0; i < chunkList.size(); ++i)
            {
                Chunk chunk = chunkList.get(i);

                if (chunk.needsSaving())
                {
                    this.safeSaveChunk(chunk);
                    chunk.isModified = false;
                }
            }
            
            Iterator<Chunk> var4 = Lists.newArrayList(this.getLoadedChunks()).iterator();

            while (var4.hasNext())
            {
                Chunk chunk = var4.next();

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
    public int getTopBlockAtSpawn()
    {
        Chunk chunk = this.provideChunk(0, 0);

        for (int y = chunk.getTopFilledSegment() + 15; y > 0; --y)
        {
            if (chunk.getBlock(0, y, 0).isSolid())
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
    
    public void spawnPlayerInWorld(Minecraft mc)
    {
    	EntityPlayerMP player = new EntityPlayerMP(mc);
    	this.playerEntity = player;
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
        Chunk chunk = this.provideChunk(x, z);

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

    public List<Chunk> getLoadedChunks()
    {
        return this.loadedChunks;
    }

    /**
     * marks chunk for unload by "unload100OldestChunks"  if there is no spawn point, or if the center of the chunk is
     * outside 200 blocks (x or z) of the spawn
     */
    private void unloadChunksIfNotNearSpawn(int chunkX, int chunkZ)
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
    public Chunk loadChunk(int x, int z)
    {
        long posHash = ChunkCoordIntPair.chunkXZ2Int(x, z);
        this.chunksToUnload.remove(Long.valueOf(posHash));
        Chunk newChunk = this.loadedChunkHashMap.getValueByKey(posHash);

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
                	newChunk = new Chunk(x, z);

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
    public Chunk provideChunk(int p_73154_1_, int p_73154_2_)
    {
        Chunk var3 = this.loadedChunkHashMap.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(p_73154_1_, p_73154_2_));
        return var3 == null ? this.loadChunk(p_73154_1_, p_73154_2_) : var3;
    }

    /**
     * used by saveChunks, but catches any exceptions if the save fails.
     */
    private void safeSaveChunk(Chunk chunk)
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

    /**
     * Is this block powering in the specified direction Args: x, y, z, direction
     */
    private int isBlockProvidingPowerTo(int p_72879_1_, int p_72879_2_, int p_72879_3_, int p_72879_4_)
    {
        return this.getBlock(p_72879_1_, p_72879_2_, p_72879_3_).isProvidingStrongPower(this, p_72879_1_, p_72879_2_, p_72879_3_, p_72879_4_);
    }

    /**
     * Returns the highest redstone signal strength powering the given block. Args: X, Y, Z.
     */
    private int getBlockPowerInput(int p_94577_1_, int p_94577_2_, int p_94577_3_)
    {
        byte var4 = 0;
        int var5 = Math.max(var4, this.isBlockProvidingPowerTo(p_94577_1_, p_94577_2_ - 1, p_94577_3_, 0));

        if (var5 >= 15)
        {
            return var5;
        }
        else
        {
            var5 = Math.max(var5, this.isBlockProvidingPowerTo(p_94577_1_, p_94577_2_ + 1, p_94577_3_, 1));

            if (var5 >= 15)
            {
                return var5;
            }
            else
            {
                var5 = Math.max(var5, this.isBlockProvidingPowerTo(p_94577_1_, p_94577_2_, p_94577_3_ - 1, 2));

                if (var5 >= 15)
                {
                    return var5;
                }
                else
                {
                    var5 = Math.max(var5, this.isBlockProvidingPowerTo(p_94577_1_, p_94577_2_, p_94577_3_ + 1, 3));

                    if (var5 >= 15)
                    {
                        return var5;
                    }
                    else
                    {
                        var5 = Math.max(var5, this.isBlockProvidingPowerTo(p_94577_1_ - 1, p_94577_2_, p_94577_3_, 4));

                        if (var5 >= 15)
                        {
                            return var5;
                        }
                        else
                        {
                            var5 = Math.max(var5, this.isBlockProvidingPowerTo(p_94577_1_ + 1, p_94577_2_, p_94577_3_, 5));
                            return var5 >= 15 ? var5 : var5;
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the indirect signal strength being outputted by the given block in the *opposite* of the given direction.
     * Args: X, Y, Z, direction
     */
    public final boolean getIndirectPowerOutput(int x, int y, int z, int side)
    {
        return this.getIndirectPowerLevelTo(x, y, z, side) > 0;
    }

    /**
     * Gets the power level from a certain block face.  Args: x, y, z, direction
     */
    private int getIndirectPowerLevelTo(int x, int y, int z, int side)
    {
        return this.getBlock(x, y, z).isSolid() ? this.getBlockPowerInput(x, y, z) : this.getBlock(x, y, z).isProvidingWeakPower(this, x, y, z, side);
    }

    public final int getStrongestIndirectPower(int x, int y, int z)
    {
        int max = 0;
        
        int[] offsetsXForSide = new int[] {0, 0, 0, 0, -1, 1};
        int[] offsetsYForSide = new int[] {-1, 1, 0, 0, 0, 0};
        int[] offsetsZForSide = new int[] {0, 0, -1, 1, 0, 0};

        for (int side = 0; side < 6; ++side)
        {
            int power = this.getIndirectPowerLevelTo(x + offsetsXForSide[side], y + offsetsYForSide[side], z + offsetsZForSide[side], side);

            if (power >= 15)
            {
                return 15;
            }

            if (power > max)
            {
                max = power;
            }
        }

        return max;
    }

    public final boolean canPlaceEntityOnSide(Block block, int x, int y, int z, int side)
    {
        return this.getBlock(x, y, z).isReplaceable() && block.canPlaceBlockOnSide(this, x, y, z, side);
    }

    /**
     * Sets the blocks metadata and if set will then notify blocks that this block changed, depending on the flag. Args:
     * x, y, z, metadata, flag. See setBlock for flag description
     */
    public final boolean setBlockMetadataWithNotify(int x, int y, int z, int metadata, boolean flag)
    {
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000)
        {
            if (y < 0)
            {
                return false;
            }
            else if (y >= 256)
            {
                return false;
            }
            else
            {
                Chunk chunk = this.provideChunk(x >> 4, z >> 4);
                int localX = x & 15;
                int localZ = z & 15;
                boolean didChange = chunk.setBlockMetadata(localX, y, localZ, metadata);

                if (didChange)
                {
                    Block block = chunk.getBlock(localX, y, localZ);

                    if (chunk.getLoaded())
                    {
                        this.markBlockForUpdate(x, y, z);
                    }

                    if (flag)
                    {
                        this.notifyBlocksOfNeighborChange(x, y, z, block);
                    }
                }

                return didChange;
            }
        }
        else
        {
            return false;
        }
    }

    /**
     * Checks if the block is a solid, normal cube. If the chunk does not exist, or is not loaded, it returns the
     * boolean parameter
     */
    public final boolean isBlockNormalCubeDefault(int x, int y, int z)
    {
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000)
        {
            Chunk chunk = this.provideChunk(x >> 4, z >> 4);

            if (chunk != null && !chunk.isEmpty())
            {
                return this.getBlock(x, y, z).isSolid();
            }
        }
        
        return true;
    }

    public final long getTotalWorldTime()
    {
        return this.worldInfo.getWorldTotalTime();
    }
}