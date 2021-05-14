package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NBTTagList extends NBTBase
{
    /** The array list containing the tags encapsulated in this list. */
    private List<NBTTagCompound> tagList = new ArrayList<NBTTagCompound>();
    
    NBTTagList(){}
    
    public NBTTagList(List<NBTTagCompound> list)
    {
    	this.tagList = list;
    }

    /**
     * Write the actual data contents of the tag, implemented in NBT extension classes
     */
    void write(DataOutput stream) throws IOException
    {
        stream.writeInt(this.tagList.size());

        for (int i = 0; i < this.tagList.size(); ++i)
        {
            this.tagList.get(i).write(stream);
        }
    }

    public void read(DataInput stream) throws IOException
    {
        int length = stream.readInt();
        this.tagList = new ArrayList<NBTTagCompound>();

        for (int i = 0; i < length; ++i)
        {
        	NBTTagCompound tag = new NBTTagCompound();
            tag.read(stream);
            this.tagList.add(tag);
        }
    }

    /**
     * Gets the type byte for the tag.
     */
    public byte getId()
    {
        return (byte)9;
    }
    
    List<NBTTagCompound> getTagList()
    {
    	return this.tagList;
    }

    public boolean equals(Object p_equals_1_)
    {
        return super.equals(p_equals_1_) && this.tagList.equals(((NBTTagList)p_equals_1_).tagList);
    }

    public int hashCode()
    {
        return super.hashCode() ^ this.tagList.hashCode();
    }
}
