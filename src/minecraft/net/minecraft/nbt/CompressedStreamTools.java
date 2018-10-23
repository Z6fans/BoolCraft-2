package net.minecraft.nbt;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;

public class CompressedStreamTools
{
    /**
     * Reads from a CompressedStream.
     */
    public static NBTTagCompound read(DataInputStream in) throws IOException
    {
        if (in.readByte() != 10) throw new IOException("Root tag must be a named compound tag");

        in.readUTF();
        NBTTagCompound tag = new NBTTagCompound();

        try
        {
            tag.read(in);
        }
        catch (IOException e)
        {
            throw new ReportedException(CrashReport.makeCrashReport(e, "Loading NBT data"));
        }

        return tag;
    }

    public static void write(NBTTagCompound tag, DataOutput out) throws IOException
    {
    	out.writeByte(tag.getId());
    	out.writeUTF("");
        tag.write(out);
    }
}
