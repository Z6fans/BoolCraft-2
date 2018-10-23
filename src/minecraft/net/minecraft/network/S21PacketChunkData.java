package net.minecraft.network;

import java.util.zip.Deflater;
import net.minecraft.player.EntityPlayerMP;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class S21PacketChunkData
{
    private int chunkX;
    private int chunkZ;
    private int field_149283_c;
    private int field_149280_d;
    private byte[] field_149281_e;
    private byte[] field_149278_f;
    private boolean field_149279_g;
    private static byte[] field_149286_i = new byte[196864];

    public S21PacketChunkData() {}

    public S21PacketChunkData(Chunk<EntityPlayerMP> chunk, boolean p_i45196_2_, int flags)
    {
        this.chunkX = chunk.xPosition;
        this.chunkZ = chunk.zPosition;
        this.field_149279_g = p_i45196_2_;
        S21PacketChunkData.Extracted extracted = func_149269_a(chunk, p_i45196_2_, flags);
        Deflater var5 = new Deflater(-1);
        this.field_149280_d = extracted.field_150281_c;
        this.field_149283_c = extracted.field_150280_b;

        try
        {
            this.field_149278_f = extracted.field_150282_a;
            var5.setInput(extracted.field_150282_a, 0, extracted.field_150282_a.length);
            var5.finish();
            this.field_149281_e = new byte[extracted.field_150282_a.length];
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
    public byte[] func_149272_d()
    {
        return this.field_149278_f;
    }

    public static S21PacketChunkData.Extracted func_149269_a(Chunk<EntityPlayerMP> chunk, boolean p_149269_1_, int flags)
    {
        int var3 = 0;
        ExtendedBlockStorage[] storageArray = chunk.getBlockStorageArray();
        int var5 = 0;
        S21PacketChunkData.Extracted extracted = new S21PacketChunkData.Extracted();
        byte[] var7 = field_149286_i;

        int i;

        for (i = 0; i < storageArray.length; ++i)
        {
            if (storageArray[i] != null && (!p_149269_1_ || !storageArray[i].isEmpty()) && (flags & 1 << i) != 0)
            {
                extracted.field_150280_b |= 1 << i;

                if (storageArray[i].getBlockMSBArray() != null)
                {
                    extracted.field_150281_c |= 1 << i;
                    ++var5;
                }
            }
        }

        for (i = 0; i < storageArray.length; ++i)
        {
            if (storageArray[i] != null && (!p_149269_1_ || !storageArray[i].isEmpty()) && (flags & 1 << i) != 0)
            {
                byte[] var9 = storageArray[i].getBlockLSBArray();
                System.arraycopy(var9, 0, var7, var3, var9.length);
                var3 += var9.length;
            }
        }

        NibbleArray var11;

        for (i = 0; i < storageArray.length; ++i)
        {
            if (storageArray[i] != null && (!p_149269_1_ || !storageArray[i].isEmpty()) && (flags & 1 << i) != 0)
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
                if (storageArray[i] != null && (!p_149269_1_ || !storageArray[i].isEmpty()) && storageArray[i].getBlockMSBArray() != null && (flags & 1 << i) != 0)
                {
                    var11 = storageArray[i].getBlockMSBArray();
                    System.arraycopy(var11.data, 0, var7, var3, var11.data.length);
                    var3 += var11.data.length;
                }
            }
        }
        extracted.field_150282_a = new byte[var3];
        System.arraycopy(var7, 0, extracted.field_150282_a, 0, var3);
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

    public int func_149276_g()
    {
        return this.field_149283_c;
    }

    public int func_149270_h()
    {
        return this.field_149280_d;
    }

    public boolean func_149274_i()
    {
        return this.field_149279_g;
    }

    public static class Extracted
    {
        public byte[] field_150282_a;
        public int field_150280_b;
        public int field_150281_c;
    }
}
