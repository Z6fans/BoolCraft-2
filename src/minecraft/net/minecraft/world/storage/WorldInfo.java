package net.minecraft.world.storage;

import net.minecraft.nbt.NBTTagCompound;

public class WorldInfo
{
    /** Total time for this world. */
    private long totalTime;
    private boolean initialized;

    public WorldInfo(NBTTagCompound tag)
    {
        this.totalTime = tag.getLong("Time");

        if (tag.isTagIdEqual("initialized", 99))
        {
            this.initialized = tag.getBoolean("initialized");
        }
        else
        {
            this.initialized = true;
        }
    }

    public WorldInfo()
    {
        this.initialized = false;
    }

    /**
     * Gets the NBTTagCompound for the worldInfo
     */
    public NBTTagCompound getNBTTagCompound()
    {
    	NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("Time", this.totalTime);
        tag.setBoolean("initialized", this.initialized);
        return tag;
    }

    public long getWorldTotalTime()
    {
        return this.totalTime;
    }

    public void setTotalWorldTime(long time)
    {
        this.totalTime = time;
    }

    /**
     * Returns true if the World is initialized.
     */
    public boolean isInitialized()
    {
        return this.initialized;
    }

    /**
     * Sets the initialization status of the World.
     */
    public void setServerInitialized(boolean initialized)
    {
        this.initialized = initialized;
    }
}