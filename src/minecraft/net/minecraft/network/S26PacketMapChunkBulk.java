package net.minecraft.network;

import java.util.List;
import java.util.zip.Deflater;

import net.minecraft.player.EntityPlayerMP;
import net.minecraft.world.chunk.Chunk;

public class S26PacketMapChunkBulk
{
    private int[] chunkXList;
    private int[] chunkZList;
    private int[] field_149265_c;
    private int[] field_149262_d;
    private byte[] field_149263_e;
    private byte[][] field_149260_f;
    private static byte[] field_149268_i = new byte[0];

    public S26PacketMapChunkBulk() {}

    public S26PacketMapChunkBulk(List<Chunk<EntityPlayerMP>> chunks)
    {
        int numChunks = chunks.size();
        this.chunkXList = new int[numChunks];
        this.chunkZList = new int[numChunks];
        this.field_149265_c = new int[numChunks];
        this.field_149262_d = new int[numChunks];
        this.field_149260_f = new byte[numChunks][];
        int var3 = 0;

        for (int i = 0; i < numChunks; ++i)
        {
            Chunk<EntityPlayerMP> chunk = chunks.get(i);
            S21PacketChunkData.Extracted extracted = S21PacketChunkData.func_149269_a(chunk, true, 65535);

            if (field_149268_i.length < var3 + extracted.field_150282_a.length)
            {
                byte[] var7 = new byte[var3 + extracted.field_150282_a.length];
                System.arraycopy(field_149268_i, 0, var7, 0, field_149268_i.length);
                field_149268_i = var7;
            }

            System.arraycopy(extracted.field_150282_a, 0, field_149268_i, var3, extracted.field_150282_a.length);
            var3 += extracted.field_150282_a.length;
            this.chunkXList[i] = chunk.xPosition;
            this.chunkZList[i] = chunk.zPosition;
            this.field_149265_c[i] = extracted.field_150280_b;
            this.field_149262_d[i] = extracted.field_150281_c;
            this.field_149260_f[i] = extracted.field_150282_a;
        }

        Deflater deflater = new Deflater(-1);

        try
        {
            deflater.setInput(field_149268_i, 0, var3);
            deflater.finish();
            this.field_149263_e = new byte[var3];
            deflater.deflate(this.field_149263_e);
        }
        finally
        {
            deflater.end();
        }
    }

    public static int maxChunks()
    {
        return 5;
    }

    public int getChunkX(int i)
    {
        return this.chunkXList[i];
    }

    public int getChunkZ(int i)
    {
        return this.chunkZList[i];
    }

    public int getNumChunks()
    {
        return this.chunkXList.length;
    }

    public byte[] func_149256_c(int p_149256_1_)
    {
        return this.field_149260_f[p_149256_1_];
    }

    public int[] func_149252_e()
    {
        return this.field_149265_c;
    }

    public int[] func_149257_f()
    {
        return this.field_149262_d;
    }
}
