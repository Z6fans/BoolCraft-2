package net.minecraft.world;

public class NextTickListEntry implements Comparable<NextTickListEntry>
{
    /** The id number for the next tick entry */
    private static long nextID;

    /** X position this tick is occuring at */
    public final int x;

    /** Y position this tick is occuring at */
    public final int y;

    /** Z position this tick is occuring at */
    public final int z;

    /** Time this tick is scheduled to occur at */
    public final long t;

    /** The id of the tick entry */
    private final long id;

    public NextTickListEntry(int x, int y, int z, long time)
    {
        this.id = (long)(nextID++);
        this.x = x;
        this.y = y;
        this.z = z;
        this.t = time;
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
            return this.x == otherEntry.x && this.y == otherEntry.y && this.z == otherEntry.z;
        }
    }

    public int hashCode()
    {
        return (this.x * 1024 * 1024 + this.z * 1024 + this.y) * 256;
    }

    public int compareTo(NextTickListEntry other)
    {
        return this.t < other.t ? -1 : (this.t > other.t ? 1 : (this.id < other.id ? -1 : (this.id > other.id ? 1 : 0)));
    }
}