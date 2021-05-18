package net.minecraft.world;

public class NextTickListEntry implements Comparable<NextTickListEntry>
{
    /** The id number for the next tick entry */
    private static long nextTickEntryID;

    /** X position this tick is occuring at */
    public final int xCoord;

    /** Y position this tick is occuring at */
    public final int yCoord;

    /** Z position this tick is occuring at */
    public final int zCoord;

    /** Time this tick is scheduled to occur at */
    public final long scheduledTime;

    /** The id of the tick entry */
    private final long tickEntryID;

    public NextTickListEntry(int x, int y, int z, long time)
    {
        this.tickEntryID = (long)(nextTickEntryID++);
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
        this.scheduledTime = time;
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
            return this.xCoord == otherEntry.xCoord && this.yCoord == otherEntry.yCoord && this.zCoord == otherEntry.zCoord;
        }
    }

    public int hashCode()
    {
        return (this.xCoord * 1024 * 1024 + this.zCoord * 1024 + this.yCoord) * 256;
    }

    public int compareTo(NextTickListEntry other)
    {
        return this.scheduledTime < other.scheduledTime ? -1 : (this.scheduledTime > other.scheduledTime ? 1 : (this.tickEntryID < other.tickEntryID ? -1 : (this.tickEntryID > other.tickEntryID ? 1 : 0)));
    }
}