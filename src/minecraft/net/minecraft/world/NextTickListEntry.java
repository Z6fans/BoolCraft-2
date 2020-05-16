package net.minecraft.world;

import net.minecraft.block.Block;

public class NextTickListEntry implements Comparable<NextTickListEntry>
{
    /** The id number for the next tick entry */
    private static long nextTickEntryID;
    private final Block block;

    /** X position this tick is occuring at */
    public final int xCoord;

    /** Y position this tick is occuring at */
    public final int yCoord;

    /** Z position this tick is occuring at */
    public final int zCoord;

    /** Time this tick is scheduled to occur at */
    public long scheduledTime;
    public int priority;

    /** The id of the tick entry */
    private final long tickEntryID;

    public NextTickListEntry(int x, int y, int z, Block b)
    {
        this.tickEntryID = (long)(nextTickEntryID++);
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
        this.block = b;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof NextTickListEntry))
        {
            return false;
        }
        else
        {
            NextTickListEntry otherEntry = (NextTickListEntry)other;
            return this.xCoord == otherEntry.xCoord && this.yCoord == otherEntry.yCoord && this.zCoord == otherEntry.zCoord && Block.isEqualTo(this.block, otherEntry.block);
        }
    }

    public int hashCode()
    {
        return (this.xCoord * 1024 * 1024 + this.zCoord * 1024 + this.yCoord) * 256;
    }

    /**
     * Sets the scheduled time for this tick entry
     */
    public NextTickListEntry setScheduledTime(long time)
    {
        this.scheduledTime = time;
        return this;
    }

    public void setPriority(int p)
    {
        this.priority = p;
    }

    public int compareTo(NextTickListEntry other)
    {
        return this.scheduledTime < other.scheduledTime ? -1 : (this.scheduledTime > other.scheduledTime ? 1 : (this.priority != other.priority ? this.priority - other.priority : (this.tickEntryID < other.tickEntryID ? -1 : (this.tickEntryID > other.tickEntryID ? 1 : 0))));
    }

    public String toString()
    {
        return Block.getIdFromBlock(this.block) + ": (" + this.xCoord + ", " + this.yCoord + ", " + this.zCoord + "), " + this.scheduledTime + ", " + this.priority + ", " + this.tickEntryID;
    }

    public Block getBlock()
    {
        return this.block;
    }
}