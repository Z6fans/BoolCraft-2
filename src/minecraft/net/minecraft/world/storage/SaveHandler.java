package net.minecraft.world.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.chunk.storage.RegionFileCache;

public class SaveHandler
{
    /** The directory in which to save world data. */
    private final File worldDirectory;

    /**
     * The time in milliseconds when this field was initialized. Stored in the session lock file.
     */
    private final long initializationTime = System.currentTimeMillis();
    private static final String __OBFID = "CL_00000585";

    public SaveHandler(File file, String name, boolean hasPlayerData)
    {
        this.worldDirectory = new File(file, name);
        this.worldDirectory.mkdirs();
        (new File(this.worldDirectory, "data")).mkdirs();

        if (hasPlayerData)
        {
            (new File(this.worldDirectory, "playerdata")).mkdirs();
        }

        try
        {
            File sessionLock = new File(this.worldDirectory, "session.lock");
            DataOutputStream stream = new DataOutputStream(new FileOutputStream(sessionLock));

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
    }

    /**
     * Gets the File object corresponding to the base directory of this world.
     */
    public File getWorldDirectory()
    {
        return this.worldDirectory;
    }

    /**
     * Checks the session lock to prevent save collisions
     */
    public void checkSessionLock() throws MinecraftException
    {
        try
        {
            DataInputStream stream = new DataInputStream(new FileInputStream(new File(this.worldDirectory, "session.lock")));

            try
            {
                if (stream.readLong() != this.initializationTime)
                {
                    throw new MinecraftException("The save is being accessed from another location, aborting");
                }
            }
            finally
            {
                stream.close();
            }
        }
        catch (IOException e)
        {
            throw new MinecraftException("Failed to check session lock, aborting");
        }
    }

    /**
     * Loads and returns the world info
     */
    public WorldInfo loadWorldInfo()
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

            	return new WorldInfo(tag.getCompoundTag("Data"));
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

            	return new WorldInfo(tag.getCompoundTag("Data"));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Saves the passed in world info.
     */
    public void saveWorldInfo(WorldInfo worldInfo)
    {
        NBTTagCompound dataTag = worldInfo.getNBTTagCompound();
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
                CompressedStreamTools.write(masterTag, stream);
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
    }

    /**
     * Called to flush all changes to disk, waiting for them to complete.
     */
    public void flush() {
    	try
        {
            ThreadedFileIOBase.threadedIOInstance.waitForFinish();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        RegionFileCache.clearRegionFileReferences();
    }
}
