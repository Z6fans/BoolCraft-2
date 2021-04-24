package net.minecraft.world;

import com.google.common.collect.Lists;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.LongHashMap;
import net.minecraft.util.MathHelper;
import net.minecraft.world.chunk.AnvilChunkLoader;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ExtendedBlockStorage;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.ThreadedFileIOBase;

public class WorldServer
{
    private final Set<NextTickListEntry> pendingTickListEntriesHashSet = new HashSet<NextTickListEntry>();

    /** All work to do in future ticks. */
    private final TreeSet<NextTickListEntry> pendingTickListEntriesTreeSet = new TreeSet<NextTickListEntry>();
    private int updateEntityTick;
    private final List<NextTickListEntry> pendingTickListEntriesThisTick = new ArrayList<NextTickListEntry>();
    
    /** Positions to update */
    private final Set<ChunkCoordIntPair> activeChunkSet = new HashSet<ChunkCoordIntPair>();
    
    /**
     * Contains the current Linear Congruential Generator seed for block updates. Used with an A value of 3 and a C
     * value of 0x3c6ef35f, producing a highly planar series of values ill-suited for choosing random blocks in a
     * 16x128x16 field.
     */
    private int updateLCG = (new Random()).nextInt();
    
    /** The directory in which to save world data. */
    private final File worldDirectory;

    /**
     * The time in milliseconds when this field was initialized. Stored in the session lock file.
     */
    private final long initializationTime = System.currentTimeMillis();
    
    private static final Logger logger = LogManager.getLogger();

    /**
     * used by unload100OldestChunks to iterate the loadedChunkHashMap for unload (underlying assumption, first in,
     * first out)
     */
    private final Set<Long> chunksToUnload = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
    private final AnvilChunkLoader currentChunkLoader;
    private final LongHashMap<Chunk> loadedChunkHashMap = new LongHashMap<Chunk>();
    private final List<Chunk> loadedChunks = new ArrayList<Chunk>();

    /** RNG for World. */
    private final Random rand = new Random();
    
    /** Total time for this world. */
    private long totalTime;
    
    private double playerPosX;
    private double playerPosZ;

    /** player X position as seen by PlayerManager */
    private double prevPosX;

    /** player Z position as seen by PlayerManager */
    private double prevPosZ;

    /**
     * A map of chunk position (two ints concatenated into a long) to PlayerInstance
     */
    private final LongHashMap<ChunkUpdateTracker> playerInstances = new LongHashMap<ChunkUpdateTracker>();

    /**
     * contains a PlayerInstance for every chunk they can see. the "player instance" cotains a list of all players who
     * can also that chunk
     */
    private final List<ChunkUpdateTracker> chunkWatcherWithPlayers = new ArrayList<ChunkUpdateTracker>();

    /** This field is using when chunk should be processed (every 8000 ticks) */
    private final List<ChunkUpdateTracker> playerInstanceList = new ArrayList<ChunkUpdateTracker>();

    /**
     * Number of chunks the server sends to the client. Valid 3<=x<=15. In server.properties.
     */
    private final int playerViewRadius = 10;

    /** time what is using to check if InhabitedTime should be calculated */
    private long previousTotalWorldTime;

    /** x, z direction vectors: east, south, west, north */
    private final int[][] xzDirectionsConst = new int[][] {{1, 0}, {0, 1}, { -1, 0}, {0, -1}};
    
	private final Minecraft minecraft;

    /** LinkedList that holds the loaded chunks. */
    private final List<ChunkCoordIntPair> playerLoadedChunks = new LinkedList<ChunkCoordIntPair>();

	private boolean isSpawned = false;

    public WorldServer(Minecraft mc, File wd)
    {
    	this.minecraft = mc;
    	this.worldDirectory = wd;
        this.worldDirectory.mkdirs();
        (new File(this.worldDirectory, "data")).mkdirs();

        try
        {
            final File sessionLock = new File(this.worldDirectory, "session.lock");
            final DataOutputStream stream = new DataOutputStream(new FileOutputStream(sessionLock));

            try
            {
                stream.writeLong(this.initializationTime);
            }
            finally
            {
                stream.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException("Failed to check session lock, aborting");
        }
        
        NBTTagCompound info = this.loadWorldInfo();
        
        if(info != null)
    	{
    		this.totalTime = info.getLong("Time");
    	}
        
        this.currentChunkLoader = new AnvilChunkLoader(this.worldDirectory);
    }
    
    /**
     * Loads and returns the world info
     */
    private NBTTagCompound loadWorldInfo()
    {
        File file = new File(this.worldDirectory, "level.dat");

        if (file.exists())
        {
            try
            {
            	DataInputStream stream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))));
                NBTTagCompound tag;

                try
                {
                    tag = CompressedStreamTools.read(stream);
                }
                finally
                {
                    stream.close();
                }

            	return tag.getCompoundTag("Data");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        file = new File(this.worldDirectory, "level.dat_old");

        if (file.exists())
        {
            try
            {
            	DataInputStream stream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))));
                NBTTagCompound tag;

                try
                {
                    tag = CompressedStreamTools.read(stream);
                }
                finally
                {
                    stream.close();
                }

            	return tag.getCompoundTag("Data");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Runs a single tick for the world
     */
    public void tick()
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
    	
    	this.totalTime = this.totalTime + 1L;
        
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

                if (entry.scheduledTime > this.totalTime)
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
                            throw new RuntimeException("Exception while ticking a block", t);
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

        if (this.isSpawned)
        {
        	int chunkX = MathHelper.floor_double(this.playerPosX / 16.0D);
            int chunkZ = MathHelper.floor_double(this.playerPosZ / 16.0D);

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
        
        long worldTime = this.getTotalWorldTime();

        if (worldTime - this.previousTotalWorldTime > 8000L)
        {
            this.previousTotalWorldTime = worldTime;

            for (int var18 = 0; var18 < this.playerInstanceList.size(); ++var18)
            {
            	ChunkUpdateTracker var19 = this.playerInstanceList.get(var18);
            	var19.sendChunkUpdate();
            }
        }
        else
        {
            for (int var18 = 0; var18 < this.chunkWatcherWithPlayers.size(); ++var18)
            {
            	ChunkUpdateTracker var19 = this.chunkWatcherWithPlayers.get(var18);
            	var19.sendChunkUpdate();
            }
        }

        this.chunkWatcherWithPlayers.clear();
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
                entry.setScheduledTime((long)delay + this.totalTime);
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
            entry.setScheduledTime((long)delay + this.totalTime);
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
        if (!this.isSpawned)
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

        if (this.isSpawned)
        {
        	try
            {
        		int playerX = MathHelper.floor_double(this.playerPosX);
                int playerZ = MathHelper.floor_double(this.playerPosZ);
                int r = 32;

                if (this.checkChunksExist(playerX - r, 0, playerZ - r, playerX + r, 0, playerZ + r))
                {
                	if (!this.playerLoadedChunks.isEmpty())
                    {
                		ArrayList<Chunk> chunksToSend = new ArrayList<Chunk>();
                        Iterator<ChunkCoordIntPair> chunkIterator = this.playerLoadedChunks.iterator();

                        while (chunkIterator.hasNext() && chunksToSend.size() < 5)
                        {
                            ChunkCoordIntPair chunkCoords = (ChunkCoordIntPair)chunkIterator.next();

                            if (chunkCoords != null)
                            {
                                if (this.chunkExists(chunkCoords.chunkXPos, chunkCoords.chunkZPos))
                                {
                                    Chunk chunk = this.provideChunk(chunkCoords.chunkXPos, chunkCoords.chunkZPos);
                                    
                                    if (chunk.getLoaded())
                                    {
                                        chunksToSend.add(chunk);
                                        chunkIterator.remove();
                                    }
                                }
                            }
                            else
                            {
                                chunkIterator.remove();
                            }
                        }

                        if (!chunksToSend.isEmpty())
                        {
                            for (int i = 0; i < chunksToSend.size(); ++i)
                            {
                            	Chunk serverChunk = chunksToSend.get(i);
                                int chunkX = serverChunk.xPosition;
                                int chunkZ = serverChunk.zPosition;
                                Chunk clientChunk = new Chunk(chunkX, chunkZ);
                                clientChunk.setLoaded();
                                clientChunk.setStorageArrays(this.copyStorage(serverChunk));
                                clientChunk.isTerrainPopulated = true;
                                clientChunk.setChunkModified();
                                this.minecraft.worldClient.addChunk(clientChunk);
                                this.minecraft.worldClient.markBlockRangeForRenderUpdate(chunkX << 4, 0, chunkZ << 4, (chunkX << 4) + 15, 256, (chunkZ << 4) + 15);
                            }
                        }
                    }
                }
            }
            catch (Throwable t)
            {
                throw new RuntimeException("Ticking entity", t);
            }
        }
    }
    
    private ExtendedBlockStorage[] copyStorage(Chunk chunk)
    {
    	ExtendedBlockStorage[] oldStorageArray = chunk.getBlockStorageArray();
    	ExtendedBlockStorage[] newStorageArray = new ExtendedBlockStorage[oldStorageArray.length];
    	
    	for(int i = 0; i < oldStorageArray.length; i++)
    	{
    		ExtendedBlockStorage oldStorage = oldStorageArray[i];
    		
    		if(oldStorage != null)
    		{
    			ExtendedBlockStorage newStorage = new ExtendedBlockStorage(oldStorage.getYLocation());
        		
        		if(oldStorage.getBlockLSBArray() != null)
        		{
        			byte[] oldLSBArray = oldStorage.getBlockLSBArray();
            		byte[] newLSBArray = newStorage.getBlockLSBArray();
            		System.arraycopy(oldLSBArray, 0, newLSBArray, 0, oldLSBArray.length);
        		}
        		
        		if(oldStorage.getBlockMSBArray() != null)
        		{
        			byte[] oldMSBArrayData = oldStorage.getBlockMSBArray().data;
            		NibbleArray newMSBArray = newStorage.getBlockMSBArray();
            		
            		if(newMSBArray == null)
            		{
            			newStorage.createBlockMSBArray();
            		}
            		
            		System.arraycopy(oldMSBArrayData, 0, newMSBArray.data, 0, oldMSBArrayData.length);
        		}
        		
        		if(oldStorage.getMetadataArray() != null)
        		{
        			byte[] oldMetadataArrayData = oldStorage.getMetadataArray().data;
        			NibbleArray newMetadataArray = newStorage.getMetadataArray();
            		System.arraycopy(oldMetadataArrayData, 0, newMetadataArray.data, 0, oldMetadataArrayData.length);
        		}
        		
        		newStorage.removeInvalidBlocks();
        		newStorageArray[i] = newStorage;
    		}
    	}
    	
    	return newStorageArray;
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

    /**
     * Saves all chunks to disk while updating progress bar.
     */
    public void saveAllChunks() throws SessionLockException
    {
    	this.checkSessionLock();
        NBTTagCompound dataTag = new NBTTagCompound();
        dataTag.setLong("Time", this.totalTime);
        NBTTagCompound masterTag = new NBTTagCompound();
        masterTag.setTag("Data", dataTag);

        try
        {
            File levelNew = new File(this.worldDirectory, "level.dat_new");
            File levelOld = new File(this.worldDirectory, "level.dat_old");
            File level = new File(this.worldDirectory, "level.dat");
            DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(levelNew))));

            try
            {
                stream.writeByte(masterTag.getId());
                stream.writeUTF("");
            	masterTag.write(stream);
            }
            finally
            {
                stream.close();
            }

            if (levelOld.exists())
            {
                levelOld.delete();
            }

            level.renameTo(levelOld);

            if (level.exists())
            {
                level.delete();
            }

            levelNew.renameTo(level);

            if (levelNew.exists())
            {
                levelNew.delete();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        ArrayList<Chunk> chunkList = Lists.newArrayList(this.loadedChunks);

        for (int i = 0; i < chunkList.size(); ++i)
        {
            Chunk chunk = chunkList.get(i);

            if (chunk.isModified)
            {
                this.safeSaveChunk(chunk);
                chunk.isModified = false;
            }
        }
        
        Iterator<Chunk> var4 = Lists.newArrayList(this.getLoadedChunks()).iterator();

        while (var4.hasNext())
        {
            Chunk chunk = var4.next();

            if (chunk != null && this.playerInstances.getValueByKey((long)chunk.xPosition + 2147483647L | (long)chunk.zPosition + 2147483647L << 32) == null)
            {
                int x = chunk.xPosition * 16 + 8;
                int z = chunk.zPosition * 16 + 8;

                if (x < -128 || x > 128 || z < -128 || z > 128)
                {
                    this.chunksToUnload.add(Long.valueOf(ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition)));
                }
            }
        }
    }

    /**
     * Syncs all changes to disk and wait for completion.
     */
    public void flush()
    {
    	try
        {
            ThreadedFileIOBase.threadedIOInstance.waitForFinish();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        ThreadedFileIOBase.threadedIOInstance.clearRegionFileReferences();
    }
	
	private void notifyBlockOfNeighborChange(int x, int y, int z, final Block neighborBlock)
    {
		try
        {
        	this.getBlock(x, y, z).onNeighborBlockChange(this, x, y, z, neighborBlock);
        }
        catch (Throwable t)
        {
            throw new RuntimeException("Exception while updating neighbors", t);
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
    public void checkSessionLock() throws SessionLockException
    {
    	try
        {
            DataInputStream stream = new DataInputStream(new FileInputStream(new File(this.worldDirectory, "session.lock")));

            try
            {
                if (stream.readLong() != this.initializationTime)
                {
                    throw new SessionLockException("The save is being accessed from another location, aborting");
                }
            }
            finally
            {
                stream.close();
            }
        }
        catch (IOException e)
        {
            throw new SessionLockException("Failed to check session lock, aborting");
        }
    }
    
    public void spawnPlayerInWorld(Minecraft mc)
    {
    	this.isSpawned  = true;
        int chunkX = (int)this.playerPosX >> 4;
        int chunkZ = (int)this.playerPosZ >> 4;
        this.prevPosX = this.playerPosX;
        this.prevPosZ = this.playerPosZ;

        for (int x = chunkX - this.playerViewRadius; x <= chunkX + this.playerViewRadius; ++x)
        {
            for (int y = chunkZ - this.playerViewRadius; y <= chunkZ + this.playerViewRadius; ++y)
            {
                WorldServer.this.playerLoadedChunks.add(new ChunkCoordIntPair(x, y));
            }
        }
        
        this.filterChunkLoadQueue();
        this.loadChunk((int)this.playerPosX >> 4, (int)this.playerPosZ >> 4);
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
     * Checks to see if a chunk exists at x, y
     */
    private boolean chunkExists(int p_73149_1_, int p_73149_2_)
    {
        return this.loadedChunkHashMap.containsItem(ChunkCoordIntPair.chunkXZ2Int(p_73149_1_, p_73149_2_));
    }

    public List<Chunk> getLoadedChunks()
    {
        return this.loadedChunks;
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
                    throw new RuntimeException("Exception generating new chunk at " + x + ", " + z, t);
                }
            }

            this.loadedChunkHashMap.add(posHash, newChunk);
            this.loadedChunks.add(newChunk);
        }

        return newChunk;
    }

    /**
     * Will return back a chunk, if it doesn't exist and its not a MP client it will generates all the blocks for the
     * specified chunk from the map seed and chunk seed
     */
    private Chunk provideChunk(int p_73154_1_, int p_73154_2_)
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
            catch (SessionLockException var4)
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
        return this.totalTime;
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
            Block oldBlock = chunk.getBlock(x & 15, y, z & 15);

            if (chunk.setBlockAndMetaServer(this, x & 15, y, z & 15, block, metadata))
            {
                if (chunk.getLoaded())
                {
                    this.markBlockForUpdate(x, y, z);
                }

                this.notifyBlocksOfNeighborChange(x, y, z, oldBlock);
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

    private ChunkUpdateTracker getOrCreateChunkWatcher(int chunkX, int chunkZ, boolean doCreate)
    {
        long key = (long)chunkX + 2147483647L | (long)chunkZ + 2147483647L << 32;
        ChunkUpdateTracker chunkWatcher = this.playerInstances.getValueByKey(key);

        if (chunkWatcher == null && doCreate)
        {
            chunkWatcher = new ChunkUpdateTracker(chunkX, chunkZ);
            this.playerInstances.add(key, chunkWatcher);
            this.playerInstanceList.add(chunkWatcher);
        }

        return chunkWatcher;
    }

    public void markBlockForUpdate(int x, int y, int z)
    {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        ChunkUpdateTracker playerInstance = this.getOrCreateChunkWatcher(chunkX, chunkZ, false);

        if (playerInstance != null)
        {
            playerInstance.markBlockForUpdate(x & 15, y, z & 15);
        }
    }

    /**
     * Removes all chunks from the given player's chunk load queue that are not in viewing range of the player.
     */
    private void filterChunkLoadQueue()
    {
        ArrayList<ChunkCoordIntPair> var2 = new ArrayList<ChunkCoordIntPair>(this.playerLoadedChunks);
        int var3 = 0;
        int r = this.playerViewRadius;
        int chunkX = (int)this.playerPosX >> 4;
        int chunkZ = (int)this.playerPosZ >> 4;
        int var7 = 0;
        int var8 = 0;
        ChunkCoordIntPair var9 = this.getOrCreateChunkWatcher(chunkX, chunkZ, true).chunkLocation;
        this.playerLoadedChunks.clear();

        if (var2.contains(var9))
        {
        	this.playerLoadedChunks.add(var9);
        }

        for (int var10 = 1; var10 <= r * 2; ++var10)
        {
            for (int var11 = 0; var11 < 2; ++var11)
            {
                int[] var12 = this.xzDirectionsConst[var3++ % 4];

                for (int var13 = 0; var13 < var10; ++var13)
                {
                    var7 += var12[0];
                    var8 += var12[1];
                    var9 = this.getOrCreateChunkWatcher(chunkX + var7, chunkZ + var8, true).chunkLocation;

                    if (var2.contains(var9))
                    {
                    	this.playerLoadedChunks.add(var9);
                    }
                }
            }
        }

        var3 %= 4;

        for (int var10 = 0; var10 < r * 2; ++var10)
        {
            var7 += this.xzDirectionsConst[var3][0];
            var8 += this.xzDirectionsConst[var3][1];
            var9 = this.getOrCreateChunkWatcher(chunkX + var7, chunkZ + var8, true).chunkLocation;

            if (var2.contains(var9))
            {
            	this.playerLoadedChunks.add(var9);
            }
        }
    }

    /**
     * Determine if two rectangles centered at the given points overlap for the provided radius. Arguments: x1, z1, x2,
     * z2, radius.
     */
    private boolean overlaps(int x1, int z1, int x2, int z2, int r)
    {
        int deltaX = x1 - x2;
        int deltaZ = z1 - z2;
        return (deltaX >= -r && deltaX <= r) && (deltaZ >= -r && deltaZ <= r);
    }

    /**
     * update chunks around a player being moved by server logic (e.g. cart, boat)
     */
    public void updateMountedMovingPlayer(double x, double z)
    {
    	this.playerPosX = x;
    	this.playerPosZ = z;
    	
        int playerChunkX = (int)this.playerPosX >> 4;
        int playerChunkZ = (int)this.playerPosZ >> 4;
        double playerDeltaX = this.prevPosX - this.playerPosX;
        double playerDeltaZ = this.prevPosZ - this.playerPosZ;

        if (playerDeltaX * playerDeltaX + playerDeltaZ * playerDeltaZ >= 64.0D)
        {
            int managedChunkX = (int)this.prevPosX >> 4;
            int managedChunkZ = (int)this.prevPosZ >> 4;
            int radius = this.playerViewRadius;
            int chunkDeltaX = playerChunkX - managedChunkX;
            int chunkDeltaZ = playerChunkZ - managedChunkZ;

            if (chunkDeltaX != 0 || chunkDeltaZ != 0)
            {
                for (int chunkX = playerChunkX - radius; chunkX <= playerChunkX + radius; ++chunkX)
                {
                    for (int chunkZ = playerChunkZ - radius; chunkZ <= playerChunkZ + radius; ++chunkZ)
                    {
                        if (!this.overlaps(chunkX, chunkZ, managedChunkX, managedChunkZ, radius))
                        {
                            WorldServer.this.playerLoadedChunks.add(new ChunkCoordIntPair(chunkX, chunkZ));
                        }
                    }
                }

                this.filterChunkLoadQueue();
                this.prevPosX = this.playerPosX;
                this.prevPosZ = this.playerPosZ;
            }
        }
    }

    private class ChunkUpdateTracker
    {
        private final ChunkCoordIntPair chunkLocation;
        private short[] tilesToUpdate = new short[64];
        private int numberOfTilesToUpdate;

        private ChunkUpdateTracker(int chunkX, int chunkZ)
        {
            this.chunkLocation = new ChunkCoordIntPair(chunkX, chunkZ);
            WorldServer.this.loadChunk(chunkX, chunkZ);
        }

        private void markBlockForUpdate(int localX, int localY, int localZ)
        {
            if (this.numberOfTilesToUpdate == 0)
            {
                WorldServer.this.chunkWatcherWithPlayers.add(this);
            }

            if (this.numberOfTilesToUpdate < 64)
            {
                short localKey = (short)(localX << 12 | localZ << 8 | localY);

                for (int i = 0; i < this.numberOfTilesToUpdate; ++i)
                {
                    if (this.tilesToUpdate[i] == localKey)
                    {
                        return;
                    }
                }

                this.tilesToUpdate[this.numberOfTilesToUpdate++] = localKey;
            }
        }

        private void sendChunkUpdate()
        {
        	if (!WorldServer.this.playerLoadedChunks.contains(this.chunkLocation))
        	{
        		Chunk chunk = WorldServer.this.provideChunk(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos);
        		int baseX = chunk.xPosition * 16;
                int baseZ = chunk.zPosition * 16;

                for (int i = 0; i < this.numberOfTilesToUpdate; ++i)
                {
                    short localKey = this.tilesToUpdate[i];
                    int localX = localKey >> 12 & 15;
                    int localZ = localKey >> 8 & 15;
                    int localY = localKey & 255;
                    WorldServer.this.minecraft.worldClient.setBlock(localX + baseX, localY, localZ + baseZ, chunk.getBlock(localX, localY, localZ), chunk.getBlockMetadata(localX, localY, localZ));
                }
        	}

            this.numberOfTilesToUpdate = 0;
        }
    }
}