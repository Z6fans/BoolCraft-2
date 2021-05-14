package net.minecraft.world.chunk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.SessionLockException;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;

public class AnvilChunkLoader
{
    /** Save directory for chunks using the Anvil format */
    private final File chunkDirectory;

    public AnvilChunkLoader(File file)
    {
        this.chunkDirectory = file;
    }

    /**
     * Loads the specified(XZ) chunk into the specified world.
     */
    public Chunk loadChunk(WorldServer world, int chunkX, int chunkZ) throws IOException
    {
    	File chunkFile = new File(this.chunkDirectory, "c." + chunkX + "." + chunkZ + ".mca");
    	
    	if (chunkFile.exists())
        {
        	DataInputStream stream = new DataInputStream(new FileInputStream(chunkFile));
        	NBTTagCompound tag = new NBTTagCompound();
        	tag.read(stream);
        	NBTTagCompound levelTag = tag.getCompoundTag("Level");
            Chunk chunk = new Chunk(chunkX, chunkZ);
            chunk.setStorageArrays(levelTag.getByteArray("Blocks"));

            if (levelTag.isTagIdEqual("TileTicks", 9))
            {
            	for (NBTTagCompound tick : levelTag.getTagList("TileTicks"))
                {
                    world.addBlockUpdateFromSave(tick.getInteger("x"), tick.getInteger("y"), tick.getInteger("z"), Block.getBlockById(tick.getInteger("i")), tick.getInteger("t"), tick.getInteger("p"));
                }
            }

            return chunk;
        }
        
        return null;
    }

    public void saveChunk(WorldServer world, Chunk chunk) throws SessionLockException, IOException
    {
    	try
        {
            NBTTagCompound masterTag = new NBTTagCompound();
            NBTTagCompound levelTag = new NBTTagCompound();
            masterTag.setTag("Level", levelTag);
            levelTag.setByteArray("Blocks", chunk.getBlockStorageArray());
            List<NextTickListEntry> pendingUpdates = world.getPendingBlockUpdates(chunk);

            if (!pendingUpdates.isEmpty())
            {
                long time = world.getTotalWorldTime();
                List<NBTTagCompound> tickTags = new ArrayList<NBTTagCompound>();

                for (NextTickListEntry entry : pendingUpdates)
                {
                    NBTTagCompound tickTag = new NBTTagCompound();
                    tickTag.setInteger("i", Block.getIdFromBlock(entry.getBlock()));
                    tickTag.setInteger("x", entry.xCoord);
                    tickTag.setInteger("y", entry.yCoord);
                    tickTag.setInteger("z", entry.zCoord);
                    tickTag.setInteger("t", (int)(entry.scheduledTime - time));
                    tickTag.setInteger("p", entry.priority);
                    tickTags.add(tickTag);
                }

                levelTag.setTag("TileTicks", new NBTTagList(tickTags));
            }
            
            try
            {
            	int chunkX = chunk.xPosition;
            	int chunkZ = chunk.zPosition;
            	DataOutputStream stream = new DataOutputStream(new FileOutputStream(new File(this.chunkDirectory, "c." + chunkX + "." + chunkZ + ".mca")));
                masterTag.write(stream);
                stream.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
