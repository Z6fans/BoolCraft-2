package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTTagEnd extends NBTBase
{
    public void read(DataInput p_152446_1_) throws IOException {}

    /**
     * Write the actual data contents of the tag, implemented in NBT extension classes
     */
    void write(DataOutput p_74734_1_) throws IOException {}

    /**
     * Gets the type byte for the tag.
     */
    public byte getId()
    {
        return (byte)0;
    }

    public String toString()
    {
        return "END";
    }
}
