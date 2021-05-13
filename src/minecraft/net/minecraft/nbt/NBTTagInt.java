package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTTagInt extends NBTBase.NBTPrimitive
{
    /** The integer value for the tag. */
    private int data;

    NBTTagInt() {}

    public NBTTagInt(int p_i45133_1_)
    {
        this.data = p_i45133_1_;
    }

    /**
     * Write the actual data contents of the tag, implemented in NBT extension classes
     */
    void write(DataOutput p_74734_1_) throws IOException
    {
        p_74734_1_.writeInt(this.data);
    }

    public void read(DataInput p_152446_1_) throws IOException
    {
        this.data = p_152446_1_.readInt();
    }

    /**
     * Gets the type byte for the tag.
     */
    public byte getId()
    {
        return (byte)3;
    }

    public String toString()
    {
        return "" + this.data;
    }

    public boolean equals(Object p_equals_1_)
    {
        if (super.equals(p_equals_1_))
        {
            NBTTagInt var2 = (NBTTagInt)p_equals_1_;
            return this.data == var2.data;
        }
        else
        {
            return false;
        }
    }

    public int hashCode()
    {
        return super.hashCode() ^ this.data;
    }

    public long getAsLong()
    {
        return (long)this.data;
    }

    public int getAsInteger()
    {
        return this.data;
    }

    public byte getAsByte()
    {
        return (byte)(this.data & 255);
    }
}
