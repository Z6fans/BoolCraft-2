package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NBTTagCompound extends NBTBase
{
    /**
     * The key-value pairs for the tag. Each key is a UTF string, each value is a tag.
     */
    private Map tagMap = new HashMap();
    private static final String __OBFID = "CL_00001215";

    /**
     * Write the actual data contents of the tag, implemented in NBT extension classes
     */
    void write(DataOutput p_74734_1_) throws IOException
    {
        Iterator var2 = this.tagMap.keySet().iterator();

        while (var2.hasNext())
        {
            String var3 = (String)var2.next();
            NBTBase var4 = (NBTBase)this.tagMap.get(var3);
            func_150298_a(var3, var4, p_74734_1_);
        }

        p_74734_1_.writeByte(0);
    }

    void read(DataInput in) throws IOException
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
                throw new ReportedException(CrashReport.makeCrashReport(var9, "Loading NBT data"));
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

    /**
     * Stores the given boolean value as a NBTTagByte, storing 1 for true and 0 for false, using the given string key.
     */
    public void setBoolean(String p_74757_1_, boolean p_74757_2_)
    {
        this.setByte(p_74757_1_, (byte)(p_74757_2_ ? 1 : 0));
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
            return !this.tagMap.containsKey(p_74771_1_) ? 0 : ((NBTBase.NBTPrimitive)this.tagMap.get(p_74771_1_)).func_150290_f();
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
            return !this.tagMap.containsKey(p_74762_1_) ? 0 : ((NBTBase.NBTPrimitive)this.tagMap.get(p_74762_1_)).func_150287_d();
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
            return !this.tagMap.containsKey(p_74763_1_) ? 0L : ((NBTBase.NBTPrimitive)this.tagMap.get(p_74763_1_)).func_150291_c();
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
            throw new ReportedException(this.createCrashReport(p_74770_1_, 7, var3));
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
            throw new ReportedException(this.createCrashReport(p_74775_1_, 10, var3));
        }
    }

    /**
     * Gets the NBTTagList object with the given name. Args: name, NBTBase type
     */
    public NBTTagList getTagList(String p_150295_1_, int p_150295_2_)
    {
        try
        {
            if (this.getTagId(p_150295_1_) != 9)
            {
                return new NBTTagList();
            }
            else
            {
                NBTTagList var3 = (NBTTagList)this.tagMap.get(p_150295_1_);
                return var3.tagCount() > 0 && var3.func_150303_d() != p_150295_2_ ? new NBTTagList() : var3;
            }
        }
        catch (ClassCastException var4)
        {
            throw new ReportedException(this.createCrashReport(p_150295_1_, 9, var4));
        }
    }

    /**
     * Retrieves a boolean value using the specified key, or false if no such key was stored. This uses the getByte
     * method.
     */
    public boolean getBoolean(String p_74767_1_)
    {
        return this.getByte(p_74767_1_) != 0;
    }

    public String toString()
    {
        String var1 = "{";
        String var3;

        for (Iterator var2 = this.tagMap.keySet().iterator(); var2.hasNext(); var1 = var1 + var3 + ':' + this.tagMap.get(var3) + ',')
        {
            var3 = (String)var2.next();
        }

        return var1 + "}";
    }

    /**
     * Create a crash report which indicates a NBT read error.
     */
    private CrashReport createCrashReport(final String p_82581_1_, final int p_82581_2_, ClassCastException p_82581_3_)
    {
        return CrashReport.makeCrashReport(p_82581_3_, "Reading NBT data");
    }

    /**
     * Creates a clone of the tag.
     */
    public NBTBase copy()
    {
        NBTTagCompound var1 = new NBTTagCompound();
        Iterator var2 = this.tagMap.keySet().iterator();

        while (var2.hasNext())
        {
            String var3 = (String)var2.next();
            var1.setTag(var3, ((NBTBase)this.tagMap.get(var3)).copy());
        }

        return var1;
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

    private static void func_150298_a(String p_150298_0_, NBTBase p_150298_1_, DataOutput p_150298_2_) throws IOException
    {
        p_150298_2_.writeByte(p_150298_1_.getId());

        if (p_150298_1_.getId() != 0)
        {
            p_150298_2_.writeUTF(p_150298_0_);
            p_150298_1_.write(p_150298_2_);
        }
    }
}
