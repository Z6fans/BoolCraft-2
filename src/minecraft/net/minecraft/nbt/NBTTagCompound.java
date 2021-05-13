package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NBTTagCompound extends NBTBase
{
    /**
     * The key-value pairs for the tag. Each key is a UTF string, each value is a tag.
     */
    private final Map<String, NBTBase> tagMap = new HashMap<String, NBTBase>();

    /**
     * Write the actual data contents of the tag, implemented in NBT extension classes
     */
    public void write(DataOutput stream) throws IOException
    {
        for (String key : this.tagMap.keySet())
        {
            NBTBase tag = this.tagMap.get(key);
            stream.writeByte(tag.getId());

            if (tag.getId() != 0)
            {
                stream.writeUTF(key);
                tag.write(stream);
            }
        }

        stream.writeByte(0);
    }

    public void read(DataInput in) throws IOException
    {
    	this.tagMap.clear();
        byte id;

        while ((id = in.readByte()) != 0)
        {
            String name = in.readUTF();
            NBTBase tag = NBTBase.func_150284_a(id);

            try
            {
                tag.read(in);
            }
            catch (IOException var9)
            {
                throw new RuntimeException("Loading NBT data", var9);
            }
            
            this.tagMap.put(name, tag);
        }
    }

    /**
     * Gets the type byte for the tag.
     */
    public byte getId()
    {
        return (byte)10;
    }

    /**
     * Stores the given tag into the map with the given string key. This is mostly used to store tag lists.
     */
    public void setTag(String p_74782_1_, NBTBase p_74782_2_)
    {
        this.tagMap.put(p_74782_1_, p_74782_2_);
    }

    /**
     * Stores a new NBTTagByte with the given byte value into the map with the given string key.
     */
    public void setByte(String p_74774_1_, byte p_74774_2_)
    {
        this.tagMap.put(p_74774_1_, new NBTTagByte(p_74774_2_));
    }

    /**
     * Stores a new NBTTagInt with the given integer value into the map with the given string key.
     */
    public void setInteger(String p_74768_1_, int p_74768_2_)
    {
        this.tagMap.put(p_74768_1_, new NBTTagInt(p_74768_2_));
    }

    /**
     * Stores a new NBTTagLong with the given long value into the map with the given string key.
     */
    public void setLong(String p_74772_1_, long p_74772_2_)
    {
        this.tagMap.put(p_74772_1_, new NBTTagLong(p_74772_2_));
    }

    /**
     * Stores a new NBTTagByteArray with the given array as data into the map with the given string key.
     */
    public void setByteArray(String p_74773_1_, byte[] p_74773_2_)
    {
        this.tagMap.put(p_74773_1_, new NBTTagByteArray(p_74773_2_));
    }

    private byte getTagId(String p_150299_1_)
    {
        NBTBase var2 = (NBTBase)this.tagMap.get(p_150299_1_);
        return var2 != null ? var2.getId() : 0;
    }

    public boolean isTagIdEqual(String p_150297_1_, int p_150297_2_)
    {
        byte var3 = this.getTagId(p_150297_1_);
        return var3 == p_150297_2_ ? true : (p_150297_2_ != 99 ? false : var3 == 1 || var3 == 2 || var3 == 3 || var3 == 4 || var3 == 5 || var3 == 6);
    }

    /**
     * Retrieves a byte value using the specified key, or 0 if no such key was stored.
     */
    public byte getByte(String p_74771_1_)
    {
        try
        {
            return !this.tagMap.containsKey(p_74771_1_) ? 0 : ((NBTBase.NBTPrimitive)this.tagMap.get(p_74771_1_)).getAsByte();
        }
        catch (ClassCastException var3)
        {
            return (byte)0;
        }
    }

    /**
     * Retrieves an integer value using the specified key, or 0 if no such key was stored.
     */
    public int getInteger(String p_74762_1_)
    {
        try
        {
            return !this.tagMap.containsKey(p_74762_1_) ? 0 : ((NBTBase.NBTPrimitive)this.tagMap.get(p_74762_1_)).getAsInteger();
        }
        catch (ClassCastException var3)
        {
            return 0;
        }
    }

    /**
     * Retrieves a long value using the specified key, or 0 if no such key was stored.
     */
    public long getLong(String p_74763_1_)
    {
        try
        {
            return !this.tagMap.containsKey(p_74763_1_) ? 0L : ((NBTBase.NBTPrimitive)this.tagMap.get(p_74763_1_)).getAsLong();
        }
        catch (ClassCastException var3)
        {
            return 0L;
        }
    }

    /**
     * Retrieves a byte array using the specified key, or a zero-length array if no such key was stored.
     */
    public byte[] getByteArray(String p_74770_1_)
    {
        try
        {
            return !this.tagMap.containsKey(p_74770_1_) ? new byte[0] : ((NBTTagByteArray)this.tagMap.get(p_74770_1_)).func_150292_c();
        }
        catch (ClassCastException var3)
        {
            throw this.createCrashReport(p_74770_1_, 7, var3);
        }
    }

    /**
     * Retrieves a NBTTagCompound subtag matching the specified key, or a new empty NBTTagCompound if no such key was
     * stored.
     */
    public NBTTagCompound getCompoundTag(String p_74775_1_)
    {
        try
        {
            return !this.tagMap.containsKey(p_74775_1_) ? new NBTTagCompound() : (NBTTagCompound)this.tagMap.get(p_74775_1_);
        }
        catch (ClassCastException var3)
        {
            throw this.createCrashReport(p_74775_1_, 10, var3);
        }
    }

    /**
     * Gets the NBTTagList object with the given name. Args: name, NBTBase type
     */
    public List<NBTTagCompound> getTagList(String p_150295_1_)
    {
        try
        {
            return ((NBTTagList)this.tagMap.get(p_150295_1_)).getTagList();
        }
        catch (ClassCastException var4)
        {
            throw this.createCrashReport(p_150295_1_, 9, var4);
        }
    }

    public String toString()
    {
        String var1 = "{";
        String var3;

        for (Iterator<String> var2 = this.tagMap.keySet().iterator(); var2.hasNext(); var1 = var1 + var3 + ':' + this.tagMap.get(var3) + ',')
        {
            var3 = var2.next();
        }

        return var1 + "}";
    }

    /**
     * Create a crash report which indicates a NBT read error.
     */
    private RuntimeException createCrashReport(final String p_82581_1_, final int p_82581_2_, ClassCastException p_82581_3_)
    {
        return new RuntimeException("Reading NBT data", p_82581_3_);
    }

    public boolean equals(Object p_equals_1_)
    {
        if (super.equals(p_equals_1_))
        {
            NBTTagCompound var2 = (NBTTagCompound)p_equals_1_;
            return this.tagMap.entrySet().equals(var2.tagMap.entrySet());
        }
        else
        {
            return false;
        }
    }

    public int hashCode()
    {
        return super.hashCode() ^ this.tagMap.hashCode();
    }
}
