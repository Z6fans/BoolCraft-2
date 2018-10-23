package net.minecraft.network;

import java.util.zip.Deflater;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class S21PacketChunkData
{
    private int chunkX;
    private int chunkZ;
    private int LSBFlags;
    private int MSBFlags;
    private byte[] field_149281_e;
    private byte[] data;
    private boolean isHardCopy;
    private static byte[] field_149286_i = new byte[196864];

    public S21PacketChunkData() {}

    public S21PacketChunkData(Chunk chunk, boolean hardCopy, int flagsYAreasToUpdate)
    {
        this.chunkX = chunk.xPosition;
        this.chunkZ = chunk.zPosition;
        this.isHardCopy = hardCopy;
        S21PacketChunkData.Extracted extracted = func_149269_a(chunk, hardCopy, flagsYAreasToUpdate);
        Deflater var5 = new Deflater(-1);
        this.MSBFlags = extracted.MSBFlags;
        this.LSBFlags = extracted.LSBFlags;

        try
        {
            this.data = extracted.data;
            var5.setInput(extracted.data, 0, extracted.data.length);
            var5.finish();
            this.field_149281_e = new byte[extracted.data.length];
            var5.deflate(this.field_149281_e);
        }
        finally
        {
            var5.end();
        }
    }

    /**
     * Returns a string formatted as comma separated [field]=[value] values. Used by Minecraft for logging purposes.
     */
    public byte[] getData()
    {
        return this.data;
    }

    public static S21PacketChunkData.Extracted func_149269_a(Chunk chunk, boolean hardCopy, int flagsYAreasToUpdate)
    {
        int var3 = 0;
        ExtendedBlockStorage[] storageArray = chunk.getBlockStorageArray();
        int var5 = 0;
        S21PacketChunkData.Extracted extracted = new S21PacketChunkData.Extracted();
        byte[] var7 = field_149286_i;

        int i;

        for (i = 0; i < storageArray.length; ++i)
        {
            if (storageArray[i] != null && (!hardCopy || !storageArray[i].isEmpty()) && (flagsYAreasToUpdate & 1 << i) != 0)
            {
                extracted.LSBFlags |= 1 << i;

                if (storageArray[i].getBlockMSBArray() != null)
                {
                    extracted.MSBFlags |= 1 << i;
                    ++var5;
                }
            }
        }

        for (i = 0; i < storageArray.length; ++i)
        {
            if (storageArray[i] != null && (!hardCopy || !storageArray[i].isEmpty()) && (flagsYAreasToUpdate & 1 << i) != 0)
            {
                byte[] var9 = storageArray[i].getBlockLSBArray();
                System.arraycopy(var9, 0, var7, var3, var9.length);
                var3 += var9.length;
            }
        }

        NibbleArray var11;

        for (i = 0; i < storageArray.length; ++i)
        {
            if (storageArray[i] != null && (!hardCopy || !storageArray[i].isEmpty()) && (flagsYAreasToUpdate & 1 << i) != 0)
            {
                var11 = storageArray[i].getMetadataArray();
                System.arraycopy(var11.data, 0, var7, var3, var11.data.length);
                var3 += var11.data.length;
            }
        }

        if (var5 > 0)
        {
            for (i = 0; i < storageArray.length; ++i)
            {
                if (storageArray[i] != null && (!hardCopy || !storageArray[i].isEmpty()) && storageArray[i].getBlockMSBArray() != null && (flagsYAreasToUpdate & 1 << i) != 0)
                {
                    var11 = storageArray[i].getBlockMSBArray();
                    System.arraycopy(var11.data, 0, var7, var3, var11.data.length);
                    var3 += var11.data.length;
                }
            }
        }
        extracted.data = new byte[var3];
        System.arraycopy(var7, 0, extracted.data, 0, var3);
        return extracted;
    }

    public int getChunkX()
    {
        return this.chunkX;
    }

    public int getChunkZ()
    {
        return this.chunkZ;
    }

    public int getLSBFlags()
    {
        return this.LSBFlags;
    }

    public int getMSBFlags()
    {
        return this.MSBFlags;
    }

    public boolean getIsHardCopy()
    {
        return this.isHardCopy;
    }

    public static class Extracted
    {
        public byte[] data;
        public int LSBFlags;
        public int MSBFlags;
    }
}
