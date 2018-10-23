package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NBTTagList extends NBTBase
{
    /** The array list containing the tags encapsulated in this list. */
    private List<NBTBase> tagList = new ArrayList<NBTBase>();

    /**
     * The type byte for the tags in the list - they must all be of the same type.
     */
    private byte tagType = 0;

    /**
     * Write the actual data contents of the tag, implemented in NBT extension classes
     */
    void write(DataOutput p_74734_1_) throws IOException
    {
        if (!this.tagList.isEmpty())
        {
            this.tagType = this.tagList.get(0).getId();
        }
        else
        {
            this.tagType = 0;
        }

        p_74734_1_.writeByte(this.tagType);
        p_74734_1_.writeInt(this.tagList.size());

        for (int var2 = 0; var2 < this.tagList.size(); ++var2)
        {
            this.tagList.get(var2).write(p_74734_1_);
        }
    }

    void read(DataInput p_152446_1_) throws IOException
    {
    	this.tagType = p_152446_1_.readByte();
        int var4 = p_152446_1_.readInt();
        this.tagList = new ArrayList<NBTBase>();

        for (int var5 = 0; var5 < var4; ++var5)
        {
            NBTBase var6 = NBTBase.func_150284_a(this.tagType);
            var6.read(p_152446_1_);
            this.tagList.add(var6);
        }
    }

    /**
     * Gets the type byte for the tag.
     */
    public byte getId()
    {
        return (byte)9;
    }

    public String toString()
    {
        String var1 = "[";
        int var2 = 0;

        for (Iterator<NBTBase> var3 = this.tagList.iterator(); var3.hasNext(); ++var2)
        {
            NBTBase var4 = var3.next();
            var1 = var1 + "" + var2 + ':' + var4 + ',';
        }

        return var1 + "]";
    }

    /**
     * Adds the provided tag to the end of the list. There is no check to verify this tag is of the same type as any
     * previous tag.
     */
    public void appendTag(NBTBase p_74742_1_)
    {
        if (this.tagType == 0)
        {
            this.tagType = p_74742_1_.getId();
        }
        else if (this.tagType != p_74742_1_.getId())
        {
            System.err.println("WARNING: Adding mismatching tag types to tag list");
            return;
        }

        this.tagList.add(p_74742_1_);
    }

    /**
     * Retrieves the NBTTagCompound at the specified index in the list
     */
    public NBTTagCompound getCompoundTagAt(int p_150305_1_)
    {
        if (p_150305_1_ >= 0 && p_150305_1_ < this.tagList.size())
        {
            NBTBase var2 = (NBTBase)this.tagList.get(p_150305_1_);
            return var2.getId() == 10 ? (NBTTagCompound)var2 : new NBTTagCompound();
        }
        else
        {
            return new NBTTagCompound();
        }
    }

    /**
     * Returns the number of tags in the list.
     */
    public int tagCount()
    {
        return this.tagList.size();
    }

    /**
     * Creates a clone of the tag.
     */
    public NBTBase copy()
    {
        NBTTagList var1 = new NBTTagList();
        var1.tagType = this.tagType;
        Iterator<NBTBase> var2 = this.tagList.iterator();

        while (var2.hasNext())
        {
            NBTBase var3 = var2.next();
            NBTBase var4 = var3.copy();
            var1.tagList.add(var4);
        }

        return var1;
    }

    public boolean equals(Object p_equals_1_)
    {
        if (super.equals(p_equals_1_))
        {
            NBTTagList var2 = (NBTTagList)p_equals_1_;

            if (this.tagType == var2.tagType)
            {
                return this.tagList.equals(var2.tagList);
            }
        }

        return false;
    }

    public int hashCode()
    {
        return super.hashCode() ^ this.tagList.hashCode();
    }

    public int func_150303_d()
    {
        return this.tagType;
    }
}
