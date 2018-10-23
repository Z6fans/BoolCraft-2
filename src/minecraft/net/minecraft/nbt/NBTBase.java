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

    abstract void read(DataInput p_152446_1_) throws IOException;

    public abstract String toString();

    /**
     * Gets the type byte for the tag.
     */
    public abstract byte getId();

    protected static NBTBase func_150284_a(byte p_150284_0_)
    {
        switch (p_150284_0_)
        {
            case 0:
                return new NBTTagEnd();

            case 1:
                return new NBTTagByte();

            case 3:
                return new NBTTagInt();

            case 4:
                return new NBTTagLong();

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

    /**
     * Creates a clone of the tag.
     */
    public abstract NBTBase copy();

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

    public abstract static class NBTPrimitive extends NBTBase
    {
        public abstract long func_150291_c();

        public abstract int func_150287_d();

        public abstract short func_150289_e();

        public abstract byte func_150290_f();

        public abstract double func_150286_g();

        public abstract float func_150288_h();
    }
}
