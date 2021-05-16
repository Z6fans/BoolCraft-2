package net.minecraft.world;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.LongHashMap;
import net.minecraft.util.MathHelper;

public class WorldServer
{
    private final Set<NextTickListEntry> pendingTickListEntriesHashSet = new HashSet<NextTickListEntry>();

    /** All work to do in future ticks. */
    private final TreeSet<NextTickListEntry> pendingTickListEntriesTreeSet = new TreeSet<NextTickListEntry>();
    private int updateEntityTick;
    private final List<NextTickListEntry> pendingTickListEntriesThisTick = new ArrayList<NextTickListEntry>();
    
    /** Positions to update */
    private final Set<ChunkCoordIntPair> activeChunkSet = new HashSet<ChunkCoordIntPair>();
    
    /** The directory in which to save world data. */
    private final File worldDirectory;
    
    private static final Logger logger = LogManager.getLogger();

    /**
     * used by unload100OldestChunks to iterate the loadedChunkHashMap for unload (underlying assumption, first in,
     * first out)
     */
    private final Set<Long> chunksToUnload = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
    private final LongHashMap<Chunk> loadedChunkHashMap = new LongHashMap<Chunk>();
    private final List<Chunk> loadedChunks = new ArrayList<Chunk>();
    
    /** Total time for this world. */
    private long totalTime = 0;
    
    private double playerPosX;
    private double playerPosZ;

    /** player X position as seen by PlayerManager */
    private double prevPosX;

    /** player Z position as seen by PlayerManager */
    private double prevPosZ;

    /**
     * A map of chunk position (two ints concatenated into a long) to PlayerInstance
     */
    private final Set<Long> playerInstances = new HashSet<Long>();

    /**
     * Number of chunks the server sends to the client. Valid 3<=x<=15. In server.properties.
     */
    private final int playerViewRadius = 10;

    /** time what is using to check if InhabitedTime should be calculated */
    private long previousTotalWorldTime;
    
	private final Minecraft minecraft;

    /** LinkedList that holds the loaded chunks. */
    private final List<ChunkCoordIntPair> playerLoadedChunks = new LinkedList<ChunkCoordIntPair>();

	private boolean isSpawned = false;
	
	private final byte[] blankChunkStorage;

    public WorldServer(Minecraft mc, File wd)
    {
    	this.minecraft = mc;
    	this.worldDirectory = wd;
        this.worldDirectory.mkdirs();
        
        this.blankChunkStorage = new byte[0x10000];

        for (int y = 0; y < 5; ++y)
        {
            for (int x = 0; x < 16; ++x)
            {
                for (int z = 0; z < 16; ++z)
                {
                	this.blankChunkStorage[y << 8 | z << 4 | x] = 1;
                }
            }
        }
    }

    /**
     * Runs a single tick for the world
     */
    public void tick()
    {
    	for (Long hash : this.chunksToUnload)
    	{
    		Chunk chunk = this.loadedChunkHashMap.getValueByKey(hash);

            if (chunk != null)
            {
            	if (chunk.isModified)
            	{
            		this.safeSaveChunk(chunk);
            		chunk.isModified = false;
            	}
                
                this.loadedChunks.remove(chunk);
            }

            this.loadedChunkHashMap.remove(hash);
    	}
    	
    	this.chunksToUnload.clear();
    	
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
                    try
                    {
                    	this.getBlock(entry.xCoord, entry.yCoord, entry.zCoord).updateTick(this, entry.xCoord, entry.yCoord, entry.zCoord);
                    }
                    catch (Throwable t)
                    {
                        throw new RuntimeException("Exception while ticking a block", t);
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

        for (ChunkCoordIntPair coords : this.activeChunkSet) this.provideChunk(coords.chunkXPos, coords.chunkZPos).setLoaded();

        if (this.totalTime - this.previousTotalWorldTime > 8000L)
        {
            this.previousTotalWorldTime = this.totalTime;
        }
    }

    /**
     * Schedules a tick to a block with a delay (Most commonly the tick rate)
     */
    public void scheduleBlockUpdate(int x, int y, int z, Block block, int delay)
    {
    	if (this.checkChunksExist(x, y, z, x, y, z))
        {
        	NextTickListEntry entry = new NextTickListEntry(x, y, z, block);
            if (!block.isReplaceobble())
            {
                entry.setScheduledTime((long)delay + this.totalTime);
            }

            if (!this.pendingTickListEntriesHashSet.contains(entry))
            {
                this.pendingTickListEntriesHashSet.add(entry);
                this.pendingTickListEntriesTreeSet.add(entry);
            }
        }
    }

    private void addBlockUpdateFromSave(int x, int y, int z, Block block, long delay)
    {
        NextTickListEntry entry = new NextTickListEntry(x, y, z, block);

        if (!block.isReplaceobble())
        {
            entry.setScheduledTime(delay + this.totalTime);
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
                        Iterator<ChunkCoordIntPair> chunkIterator = this.playerLoadedChunks.iterator();

                        while (chunkIterator.hasNext())
                        {
                            ChunkCoordIntPair chunkCoords = (ChunkCoordIntPair)chunkIterator.next();

                            if (chunkCoords != null)
                            {
                                if (this.chunkExists(chunkCoords.chunkXPos, chunkCoords.chunkZPos))
                                {
                                    Chunk chunk = this.provideChunk(chunkCoords.chunkXPos, chunkCoords.chunkZPos);
                                    
                                    if (chunk.getLoaded())
                                    {
                                        this.minecraft.renderGlobal.markChunksForUpdate(chunkCoords.chunkXPos - 1, 0, chunkCoords.chunkZPos - 1, chunkCoords.chunkXPos + 1, 16, chunkCoords.chunkZPos + 1);
                                        chunkIterator.remove();
                                    }
                                }
                            }
                            else
                            {
                                chunkIterator.remove();
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

    private List<NextTickListEntry> getPendingBlockUpdates(Chunk chunk)
    {
        int xmin = (chunk.xPosition << 4) - 2;
        int xmax = xmin + 16 + 2;
        int zmin = (chunk.zPosition << 4) - 2;
        int zmax = zmin + 16 + 2;

        return Stream.concat(this.pendingTickListEntriesTreeSet.stream(), this.pendingTickListEntriesThisTick.stream())
        		.filter(entry -> entry.xCoord >= xmin && entry.xCoord < xmax && entry.zCoord >= zmin && entry.zCoord < zmax)
        		.collect(Collectors.toList());
    }

    public void saveAllChunks()
    {
        for (Chunk chunk : this.loadedChunks)
        {
            if (chunk.isModified)
            {
                this.safeSaveChunk(chunk);
                chunk.isModified = false;
            }
            
            if (!this.playerInstances.contains((long)chunk.xPosition + 2147483647L | (long)chunk.zPosition + 2147483647L << 32))
            {
                if (chunk.xPosition < -8 || chunk.xPosition > 8 || chunk.zPosition < -8 || chunk.zPosition > 8) //keep spawn loaded
                {
                    this.chunksToUnload.add(Long.valueOf(ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition)));
                }
            }
        }
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
                this.playerLoadedChunks.add(new ChunkCoordIntPair(x, y));
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

    /**
     * loads or generates the chunk at the chunk location specified
     */
    public Chunk loadChunk(int x, int z)
    {
        long posHash = ChunkCoordIntPair.chunkXZ2Int(x, z);
        this.chunksToUnload.remove(Long.valueOf(posHash));
        Chunk chunk = this.loadedChunkHashMap.getValueByKey(posHash);

        if (chunk == null)
        {
            chunk = new Chunk(x, z);
            
        	File chunkFile = new File(this.worldDirectory, "c." + x + "." + z + ".mca");
        	
        	if (chunkFile.exists())
            {
        		try
        		{
            		DataInputStream stream = new DataInputStream(new FileInputStream(chunkFile));
                	byte[] byteArray = new byte[stream.readInt()];
                    stream.readFully(byteArray);
                    chunk.setStorageArrays(byteArray);
                    
                    int length = stream.readInt();

                    for (int i = 0; i < length; ++i)
                    {
                        int entryX = stream.readInt();
                        int entryY = stream.readInt();
                        int entryZ = stream.readInt();
                        byte entryI = stream.readByte();
                        long entryT = stream.readLong();
                        
                        this.addBlockUpdateFromSave(entryX, entryY, entryZ, Block.getBlockById(entryI), entryT);
                    }
                    
                    stream.close();
        		}
                catch (Exception e)
                {
                    logger.error("Couldn\'t load chunk", e);
                }
            }
        	else
        	{
            	chunk.setStorageArrays(this.blankChunkStorage.clone());
            }

            this.loadedChunkHashMap.add(posHash, chunk);
            this.loadedChunks.add(chunk);
        }

        return chunk;
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
    	try
        {
            DataOutputStream stream = new DataOutputStream(new FileOutputStream(new File(this.worldDirectory, "c." + chunk.xPosition + "." + chunk.zPosition + ".mca")));
            byte[] byteArray = chunk.getBlockStorageArray();
            stream.writeInt(byteArray.length);
            stream.write(byteArray);
            
            List<NextTickListEntry> tickTags = this.getPendingBlockUpdates(chunk);

            stream.writeInt(tickTags.size());
            
            for (NextTickListEntry entry : tickTags)
            {
                stream.writeInt(entry.xCoord);
                stream.writeInt(entry.yCoord);
                stream.writeInt(entry.zCoord);
                stream.writeByte(Block.getIdFromBlock(entry.getBlock()));
                stream.writeLong(entry.scheduledTime - this.getTotalWorldTime());
            }
            
            stream.close();
        }
        catch (Exception e)
        {
        	logger.error("Couldn\'t save chunk", e);
        }
    }

    /**
     * Is this block powering in the specified direction Args: x, y, z, direction
     */
    private int isBlockProvidingPowerTo(int x, int y, int z, int side)
    {
        return this.getBlock(x, y, z).isProvidingStrongPower(this, x, y, z, side);
    }

    /**
     * Returns the highest redstone signal strength powering the given block. Args: X, Y, Z.
     */
    private int getBlockPowerInput(int x, int y, int z)
    {
        int power = Math.max(0, this.isBlockProvidingPowerTo(x, y - 1, z, 0));
        power = Math.max(power, this.isBlockProvidingPowerTo(x, y + 1, z, 1));
        power = Math.max(power, this.isBlockProvidingPowerTo(x, y, z - 1, 2));
        power = Math.max(power, this.isBlockProvidingPowerTo(x, y, z + 1, 3));
        power = Math.max(power, this.isBlockProvidingPowerTo(x - 1, y, z, 4));
        power = Math.max(power, this.isBlockProvidingPowerTo(x + 1, y, z, 5));
        return power;
    }

    /**
     * Returns the indirect signal strength being outputted by the given block in the *opposite* of the given direction.
     * Args: X, Y, Z, direction
     */
    public boolean getIndirectPowerOutput(int x, int y, int z, int side)
    {
        return this.getIndirectPowerLevelTo(x, y, z, side) > 0;
    }

    /**
     * Gets the power level from a certain block face.  Args: x, y, z, direction
     */
    private int getIndirectPowerLevelTo(int x, int y, int z, int side)
    {
        return this.isSolid(x, y, z) ? this.getBlockPowerInput(x, y, z) : this.getBlock(x, y, z).isProvidingWeakPower(this, x, y, z, side);
    }

    public int getStrongestIndirectPower(int x, int y, int z)
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

    public boolean canPlaceEntity(Block block, int x, int y, int z)
    {
        return this.isReplaceable(x, y, z) && block.canPlaceBlockAt(this, x, y, z);
    }

    /**
     * Sets the blocks metadata and if set will then notify blocks that this block changed, depending on the flag. Args:
     * x, y, z, metadata, flag. See setBlock for flag description
     */
    public boolean setBlockMetadataWithNotify(int x, int y, int z, int metadata, boolean flag)
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

    public long getTotalWorldTime()
    {
        return this.totalTime;
    }

    /**
     * Sets the block ID and metadata at a given location. Args: X, Y, Z, new block ID, new metadata, flags. Flag 1 will
     * cause a block update. Flag 2 will send the change to clients (you almost always want this). Flag 4 prevents the
     * block from being re-rendered, if this is a client world. Flags can be added together.
     */
    public boolean setBlock(int x, int y, int z, Block block, int metadata)
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

    public Block getBlock(int x, int y, int z)
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
    public int getBlockMetadata(int x, int y, int z)
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

    private void markBlockForUpdate(int x, int y, int z)
    {
    	this.minecraft.renderGlobal.markChunksForUpdate((x - 1) >> 4, (y - 1) >> 4, (z - 1) >> 4, (x + 1) >> 4, (y + 1) >> 4, (z + 1) >> 4);
    }

    /**
     * Removes all chunks from the given player's chunk load queue that are not in viewing range of the player.
     */
    private void filterChunkLoadQueue()
    {
        ArrayList<ChunkCoordIntPair> oldPlayerLoadedChunks = new ArrayList<ChunkCoordIntPair>(this.playerLoadedChunks);
        int chunkX = (int)this.playerPosX >> 4;
        int chunkZ = (int)this.playerPosZ >> 4;
        this.playerLoadedChunks.clear();
        
        for (int x = chunkX - this.playerViewRadius; x <= chunkX + this.playerViewRadius; x++)
        {
        	for (int z = chunkZ - this.playerViewRadius; z <= chunkZ + this.playerViewRadius; z++)
            {
        		long key = (long)x + 2147483647L | (long)z + 2147483647L << 32;

                if (!this.playerInstances.contains(key))
                {
                    this.loadChunk(x, z);
                    this.playerInstances.add(key);
                }
                
        		ChunkCoordIntPair coord = new ChunkCoordIntPair(x, z);

                if (oldPlayerLoadedChunks.contains(coord))
                {
                	this.playerLoadedChunks.add(coord);
                }
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
            int r = this.playerViewRadius;
            int chunkDeltaX = playerChunkX - managedChunkX;
            int chunkDeltaZ = playerChunkZ - managedChunkZ;

            if (chunkDeltaX != 0 || chunkDeltaZ != 0)
            {
                for (int chunkX = playerChunkX - r; chunkX <= playerChunkX + r; ++chunkX)
                {
                    for (int chunkZ = playerChunkZ - r; chunkZ <= playerChunkZ + r; ++chunkZ)
                    {
                        if (!this.overlaps(chunkX, chunkZ, managedChunkX, managedChunkZ, r))
                        {
                            this.playerLoadedChunks.add(new ChunkCoordIntPair(chunkX, chunkZ));
                        }
                    }
                }

                this.filterChunkLoadQueue();
                this.prevPosX = this.playerPosX;
                this.prevPosZ = this.playerPosZ;
            }
        }
    }
    
    public boolean isSolid(int x, int y, int z)
    {
    	return this.getBlock(x, y, z).isSoled();
    }
    
    public boolean isReplaceable(int x, int y, int z)
    {
    	return this.getBlock(x, y, z).isReplaceobble();
    }
}