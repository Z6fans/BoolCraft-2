package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTTagByte extends NBTBase.NBTPrimitive
{
    /** The byte value for the tag. */
    private byte data;

    NBTTagByte() {}

    public NBTTagByte(byte p_i45129_1_)
    {
        this.data = p_i45129_1_;
    }

    /**
     * Write the actual data contents of the tag, implemented in NBT extension classes
     */
    void write(DataOutput p_74734_1_) throws IOException
    {
        p_74734_1_.writeByte(this.data);
    }

    public void read(DataInput p_152446_1_) throws IOException
    {
        this.data = p_152446_1_.readByte();
    }

    /**
     * Gets the type byte for the tag.
     */
    public byte getId()
    {
        return (byte)1;
    }

    public String toString()
    {
        return "" + this.data + "b";
    }

    public boolean equals(Object p_equals_1_)
    {
        if (super.equals(p_equals_1_))
        {
            NBTTagByte var2 = (NBTTagByte)p_equals_1_;
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
        return this.data;
    }
}
