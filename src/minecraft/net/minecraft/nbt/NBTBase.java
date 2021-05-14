package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class NBTBase
{
	/**
     * Write the actual data contents of the tag, implemented in NBT extension classes
     */
    abstract void write(DataOutput p_74734_1_) throws IOException;

    public abstract void read(DataInput p_152446_1_) throws IOException;

    /**
     * Gets the type byte for the tag.
     */
    public abstract byte getId();

    protected static NBTBase func_150284_a(byte type)
    {
        switch (type)
        {
            case 3:
                return new NBTTagInt();

            case 7:
                return new NBTTagByteArray();

            case 9:
                return new NBTTagList();

            case 10:
                return new NBTTagCompound();

            default:
                return null;
        }
    }

    public boolean equals(Object p_equals_1_)
    {
        if (!(p_equals_1_ instanceof NBTBase))
        {
            return false;
        }
        else
        {
            NBTBase var2 = (NBTBase)p_equals_1_;
            return this.getId() == var2.getId();
        }
    }

    public int hashCode()
    {
        return this.getId();
    }
}
