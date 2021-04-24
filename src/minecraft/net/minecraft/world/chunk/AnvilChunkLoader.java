package net.minecraft.world.chunk;

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
import net.minecraft.world.SessionLockException;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;

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
                for (int i = 0; i < this.chunksToRemove.size(); ++i)
                {
                    if (((AnvilChunkLoader.PendingChunk)this.chunksToRemove.get(i)).chunkCoordinate.equals(coords))
                    {
                        tag = ((AnvilChunkLoader.PendingChunk)this.chunksToRemove.get(i)).nbtTags;
                        break;
                    }
                }
            }
        }

        if (tag == null)
        {
            DataInputStream stream = ThreadedFileIOBase.threadedIOInstance.createOrLoadRegionFile(this.chunkSaveLocation, chunkX, chunkZ).getChunkDataInputStream(chunkX & 31, chunkZ & 31);

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
            NBTTagCompound levelTag = tag.getCompoundTag("Level");
            
            int readChunkX = levelTag.getInteger("xPos");
            int readChunkZ = levelTag.getInteger("zPos");

            if (!(chunkX == readChunkX && chunkZ == readChunkZ))
            {
                logger.error("Chunk file at " + chunkX + "," + chunkZ + " is in the wrong location; relocating. (Expected " + chunkX + ", " + chunkZ + ", got " + readChunkX + ", " + readChunkZ + ")");
                tag.setInteger("xPos", chunkX);
                tag.setInteger("zPos", chunkZ);
            }
            
            Chunk chunk = new Chunk(readChunkX, readChunkZ);
            chunk.isTerrainPopulated = levelTag.getBoolean("TerrainPopulated");
            NBTTagList sections = levelTag.getTagList("Sections", 10);
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

            if (levelTag.isTagIdEqual("TileTicks", 9))
            {
                NBTTagList ticks = levelTag.getTagList("TileTicks", 10);

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
    }

    public void saveChunk(WorldServer world, Chunk chunk) throws SessionLockException, IOException
    {
        world.checkSessionLock();

        try
        {
            NBTTagCompound masterTag = new NBTTagCompound();
            NBTTagCompound levelTag = new NBTTagCompound();
            masterTag.setTag("Level", levelTag);
            
            levelTag.setInteger("xPos", chunk.xPosition);
            levelTag.setInteger("zPos", chunk.zPosition);
            levelTag.setLong("LastUpdate", world.getTotalWorldTime());
            levelTag.setBoolean("TerrainPopulated", chunk.isTerrainPopulated);
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

            levelTag.setTag("Sections", sectionsTagList);
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

                levelTag.setTag("TileTicks", tickTagList);
            }
            
            ChunkCoordIntPair coords = chunk.getChunkCoordIntPair();
            
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
                            this.chunksToRemove.set(i, new AnvilChunkLoader.PendingChunk(coords, masterTag));
                            return;
                        }
                    }
                }

                this.chunksToRemove.add(new AnvilChunkLoader.PendingChunk(coords, masterTag));
                this.pendingAnvilChunksCoordinates.add(coords);
                ThreadedFileIOBase.threadedIOInstance.queueIO(this);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
            	DataOutputStream stream = ThreadedFileIOBase.threadedIOInstance.createOrLoadRegionFile(this.chunkSaveLocation, pendingChunk.chunkCoordinate.chunkXPos, pendingChunk.chunkCoordinate.chunkZPos).getChunkDataOutputStream(pendingChunk.chunkCoordinate.chunkXPos & 31, pendingChunk.chunkCoordinate.chunkZPos & 31);
                stream.writeByte(pendingChunk.nbtTags.getId());
                stream.writeUTF("");
            	pendingChunk.nbtTags.write(stream);
                stream.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return true;
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
