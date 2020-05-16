package net.minecraft.world.chunk.storage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.storage.ThreadedFileIOBase;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AnvilChunkLoader
{
    private static final Logger logger = LogManager.getLogger();
    private final List<AnvilChunkLoader.PendingChunk> chunksToRemove = new ArrayList<AnvilChunkLoader.PendingChunk>();
    private final Set<ChunkCoordIntPair> pendingAnvilChunksCoordinates = new HashSet<ChunkCoordIntPair>();
    private final Object syncLockObject = new Object();

    /** Save directory for chunks using the Anvil format */
    private final File chunkSaveLocation;

    public AnvilChunkLoader(File file)
    {
        this.chunkSaveLocation = file;
    }

    /**
     * Loads the specified(XZ) chunk into the specified world.
     */
    public Chunk loadChunk(WorldServer world, int chunkX, int chunkZ) throws IOException
    {
        NBTTagCompound tag = null;
        ChunkCoordIntPair coords = new ChunkCoordIntPair(chunkX, chunkZ);
        @SuppressWarnings("unused")
		Object o = this.syncLockObject;

        synchronized (this.syncLockObject)
        {
            if (this.pendingAnvilChunksCoordinates.contains(coords))
            {
                for (int var7 = 0; var7 < this.chunksToRemove.size(); ++var7)
                {
                    if (((AnvilChunkLoader.PendingChunk)this.chunksToRemove.get(var7)).chunkCoordinate.equals(coords))
                    {
                        tag = ((AnvilChunkLoader.PendingChunk)this.chunksToRemove.get(var7)).nbtTags;
                        break;
                    }
                }
            }
        }

        if (tag == null)
        {
            DataInputStream stream = RegionFileCache.createOrLoadRegionFile(this.chunkSaveLocation, chunkX, chunkZ).getChunkDataInputStream(chunkX & 31, chunkZ & 31);

            if (stream == null)
            {
                return null;
            }

            tag = CompressedStreamTools.read(stream);
        }
        
        if (!tag.isTagIdEqual("Level", 10))
        {
            logger.error("Chunk file at " + chunkX + "," + chunkZ + " is missing level data, skipping");
            return null;
        }
        else if (!tag.getCompoundTag("Level").isTagIdEqual("Sections", 9))
        {
            logger.error("Chunk file at " + chunkX + "," + chunkZ + " is missing block data, skipping");
            return null;
        }
        else
        {
            Chunk chunk = this.readChunkFromNBT(world, tag.getCompoundTag("Level"));

            if (!chunk.isAtLocation(chunkX, chunkZ))
            {
                logger.error("Chunk file at " + chunkX + "," + chunkZ + " is in the wrong location; relocating. (Expected " + chunkX + ", " + chunkZ + ", got " + chunk.xPosition + ", " + chunk.zPosition + ")");
                tag.setInteger("xPos", chunkX);
                tag.setInteger("zPos", chunkZ);
                chunk = this.readChunkFromNBT(world, tag.getCompoundTag("Level"));
            }

            return chunk;
        }
    }

    public void saveChunk(WorldServer world, Chunk chunk) throws MinecraftException, IOException
    {
        world.checkSessionLock();

        try
        {
            NBTTagCompound masterTag = new NBTTagCompound();
            NBTTagCompound levelTag = new NBTTagCompound();
            masterTag.setTag("Level", levelTag);
            this.writeChunkToNBT(chunk, world, levelTag);
            this.addChunkToPending(chunk.getChunkCoordIntPair(), masterTag);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void addChunkToPending(ChunkCoordIntPair coords, NBTTagCompound tag)
    {
        @SuppressWarnings("unused")
		Object o = this.syncLockObject;

        synchronized (this.syncLockObject)
        {
            if (this.pendingAnvilChunksCoordinates.contains(coords))
            {
                for (int i = 0; i < this.chunksToRemove.size(); ++i)
                {
                    if (((AnvilChunkLoader.PendingChunk)this.chunksToRemove.get(i)).chunkCoordinate.equals(coords))
                    {
                        this.chunksToRemove.set(i, new AnvilChunkLoader.PendingChunk(coords, tag));
                        return;
                    }
                }
            }

            this.chunksToRemove.add(new AnvilChunkLoader.PendingChunk(coords, tag));
            this.pendingAnvilChunksCoordinates.add(coords);
            ThreadedFileIOBase.threadedIOInstance.queueIO(this);
        }
    }

    /**
     * Returns a boolean stating if the write was unsuccessful.
     */
    public boolean writeNextIO()
    {
        AnvilChunkLoader.PendingChunk pendingChunk = null;
        @SuppressWarnings("unused")
		Object o = this.syncLockObject;

        synchronized (this.syncLockObject)
        {
            if (this.chunksToRemove.isEmpty())
            {
                return false;
            }

            pendingChunk = (AnvilChunkLoader.PendingChunk)this.chunksToRemove.remove(0);
            this.pendingAnvilChunksCoordinates.remove(pendingChunk.chunkCoordinate);
        }

        if (pendingChunk != null)
        {
            try
            {
                this.writeChunkNBTTags(pendingChunk);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return true;
    }

    private void writeChunkNBTTags(AnvilChunkLoader.PendingChunk pendingChunk) throws IOException
    {
    	DataOutputStream stream = RegionFileCache.createOrLoadRegionFile(this.chunkSaveLocation, pendingChunk.chunkCoordinate.chunkXPos, pendingChunk.chunkCoordinate.chunkZPos).getChunkDataOutputStream(pendingChunk.chunkCoordinate.chunkXPos & 31, pendingChunk.chunkCoordinate.chunkZPos & 31);
        CompressedStreamTools.write(pendingChunk.nbtTags, stream);
        stream.close();
    }

    /**
     * Writes the Chunk passed as an argument to the NBTTagCompound also passed, using the World argument to retrieve
     * the Chunk's last update time.
     */
    private void writeChunkToNBT(Chunk chunk, WorldServer world, NBTTagCompound tag)
    {
        tag.setInteger("xPos", chunk.xPosition);
        tag.setInteger("zPos", chunk.zPosition);
        tag.setLong("LastUpdate", world.getTotalWorldTime());
        tag.setBoolean("TerrainPopulated", chunk.isTerrainPopulated);
        ExtendedBlockStorage[] storageArray = chunk.getBlockStorageArray();
        NBTTagList sectionsTagList = new NBTTagList();
        ExtendedBlockStorage[] storageArrayClone = storageArray;
        int storageArraySize = storageArray.length;

        for (int i = 0; i < storageArraySize; ++i)
        {
            ExtendedBlockStorage storage = storageArrayClone[i];

            if (storage != null)
            {
                NBTTagCompound sectionTag = new NBTTagCompound();
                sectionTag.setByte("Y", (byte)(storage.getYLocation() >> 4 & 255));
                sectionTag.setByteArray("Blocks", storage.getBlockLSBArray());

                if (storage.getBlockMSBArray() != null)
                {
                    sectionTag.setByteArray("Add", storage.getBlockMSBArray().data);
                }

                sectionTag.setByteArray("Data", storage.getMetadataArray().data);
                sectionsTagList.appendTag(sectionTag);
            }
        }

        tag.setTag("Sections", sectionsTagList);
        List<NextTickListEntry> pendingUpdates = world.getPendingBlockUpdates(chunk);

        if (!pendingUpdates.isEmpty())
        {
            long time = world.getTotalWorldTime();
            NBTTagList tickTagList = new NBTTagList();
            Iterator<NextTickListEntry> updates = pendingUpdates.iterator();

            while (updates.hasNext())
            {
                NextTickListEntry entry = (NextTickListEntry)updates.next();
                NBTTagCompound tickTag = new NBTTagCompound();
                tickTag.setInteger("i", Block.getIdFromBlock(entry.getBlock()));
                tickTag.setInteger("x", entry.xCoord);
                tickTag.setInteger("y", entry.yCoord);
                tickTag.setInteger("z", entry.zCoord);
                tickTag.setInteger("t", (int)(entry.scheduledTime - time));
                tickTag.setInteger("p", entry.priority);
                tickTagList.appendTag(tickTag);
            }

            tag.setTag("TileTicks", tickTagList);
        }
    }

    /**
     * Reads the data stored in the passed NBTTagCompound and creates a Chunk with that data in the passed World.
     * Returns the created Chunk.
     */
    private Chunk readChunkFromNBT(WorldServer world, NBTTagCompound tag)
    {
        int chunkX = tag.getInteger("xPos");
        int chunkZ = tag.getInteger("zPos");
        Chunk chunk = new Chunk(chunkX, chunkZ);
        chunk.isTerrainPopulated = tag.getBoolean("TerrainPopulated");
        NBTTagList sections = tag.getTagList("Sections", 10);
        ExtendedBlockStorage[] storageArray = new ExtendedBlockStorage[16];

        for (int i = 0; i < sections.tagCount(); ++i)
        {
            NBTTagCompound section = sections.getCompoundTagAt(i);
            byte y = section.getByte("Y");
            ExtendedBlockStorage storage = new ExtendedBlockStorage(y << 4);
            storage.setBlockLSBArray(section.getByteArray("Blocks"));

            if (section.isTagIdEqual("Add", 7))
            {
                storage.setBlockMSBArray(new NibbleArray(section.getByteArray("Add"), 4));
            }

            storage.setBlockMetadataArray(new NibbleArray(section.getByteArray("Data"), 4));
            storage.removeInvalidBlocks();
            storageArray[y] = storage;
        }

        chunk.setStorageArrays(storageArray);

        if (tag.isTagIdEqual("TileTicks", 9))
        {
            NBTTagList ticks = tag.getTagList("TileTicks", 10);

            if (ticks != null)
            {
                for (int i = 0; i < ticks.tagCount(); ++i)
                {
                    NBTTagCompound tick = ticks.getCompoundTagAt(i);
                    world.addBlockUpdateFromSave(tick.getInteger("x"), tick.getInteger("y"), tick.getInteger("z"), Block.getBlockById(tick.getInteger("i")), tick.getInteger("t"), tick.getInteger("p"));
                }
            }
        }

        return chunk;
    }

    private static class PendingChunk
    {
        private final ChunkCoordIntPair chunkCoordinate;
        private final NBTTagCompound nbtTags;

        private PendingChunk(ChunkCoordIntPair coord, NBTTagCompound tag)
        {
            this.chunkCoordinate = coord;
            this.nbtTags = tag;
        }
    }
}
