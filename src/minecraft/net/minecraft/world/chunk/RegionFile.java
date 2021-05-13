package net.minecraft.world.chunk;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class RegionFile
{
    private static final byte[] emptySector = new byte[4096];
    private final RandomAccessFile dataFile;
    private final int[] offsets = new int[1024];
    private final ArrayList<Boolean> sectorFree;

    public RegionFile(File file)
    {
    	RandomAccessFile newDataFile = null;
    	ArrayList<Boolean> newSectorFree = null;
    	
        try
        {
        	newDataFile = new RandomAccessFile(file, "rw");

            if (newDataFile.length() < 4096L)
            {
                for (int i = 0; i < 1024; ++i)
                {
                	newDataFile.writeInt(0);
                }

                for (int i = 0; i < 1024; ++i)
                {
                	newDataFile.writeInt(0);
                }
            }

            if ((newDataFile.length() & 4095L) != 0L)
            {
                for (int i = 0; (long)i < (newDataFile.length() & 4095L); ++i)
                {
                	newDataFile.write(0);
                }
            }

            int fileSize = (int)newDataFile.length() / 4096;
            newSectorFree = new ArrayList<Boolean>(Collections.nCopies(fileSize, true));
            newSectorFree.set(0, false);
            newSectorFree.set(1, false);
            newDataFile.seek(0L);

            for (int i = 0; i < 1024; ++i)
            {
                int offset = newDataFile.readInt();
                this.offsets[i] = offset;

                if (offset != 0 && (offset >> 8) + (offset & 255) <= newSectorFree.size())
                {
                    for (int var5 = 0; var5 < (offset & 255); ++var5)
                    {
                    	newSectorFree.set((offset >> 8) + var5, false);
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        this.dataFile = newDataFile;
        this.sectorFree = newSectorFree;
    }

    /**
     * args: x, y - get uncompressed chunk stream from the region file
     */
    public DataInputStream getChunkDataInputStream(int x, int z)
    {
    	try
        {
            int offset = this.offsets[x + z * 32];

            if (offset != 0)
            {
            	int sectorNumber = offset >> 8;
                int numSectors = offset & 255;

                if (sectorNumber + numSectors <= this.sectorFree.size())
                {
                	this.dataFile.seek((long)(sectorNumber * 4096));
                    int length = this.dataFile.readInt();

                    if (length > 0 && length <= 4096 * numSectors)
                    {
                    	this.dataFile.readByte(); //for type, which is always 2
                    	byte[] data = new byte[length - 1];
                        this.dataFile.read(data);
                        return new DataInputStream(new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(data))));
                    }
                }
            }
        }
        catch (IOException e){}
    	
    	return null;
    }

    /**
     * args: x, z - get an output stream used to write chunk data, data is on disk when the returned stream is closed
     */
    public DataOutputStream getChunkDataOutputStream(int x, int z)
    {
        return new DataOutputStream(new DeflaterOutputStream(new RegionFile.ChunkBuffer(x, z)));
    }

    /**
     * args: x, z, data, length - write chunk data at (x, z) to disk
     */
    private void write(int x, int z, byte[] data, int length)
    {
        try
        {
            int offset = this.offsets[x + z * 32];
            int sectorNumber = offset >> 8;
            int numSectors = offset & 255;
            int requiredNumSectors = (length + 5) / 4096 + 1;

            if (requiredNumSectors >= 256)
            {
                return;
            }

            if (sectorNumber != 0 && numSectors == requiredNumSectors)
            {
                this.write(sectorNumber, data, length);
            }
            else
            {
                for (int i = 0; i < numSectors; ++i)
                {
                    this.sectorFree.set(sectorNumber + i, Boolean.valueOf(true));
                }

                int firstFree = this.sectorFree.indexOf(Boolean.valueOf(true));
                int numSectorsFree = 0;

                if (firstFree != -1)
                {
                    for (int i = firstFree; i < this.sectorFree.size(); ++i)
                    {
                        if (numSectorsFree != 0)
                        {
                            if (((Boolean)this.sectorFree.get(i)).booleanValue())
                            {
                                ++numSectorsFree;
                            }
                            else
                            {
                                numSectorsFree = 0;
                            }
                        }
                        else if (((Boolean)this.sectorFree.get(i)).booleanValue())
                        {
                            firstFree = i;
                            numSectorsFree = 1;
                        }

                        if (numSectorsFree >= requiredNumSectors)
                        {
                            break;
                        }
                    }
                }

                if (numSectorsFree >= requiredNumSectors)
                {
                    sectorNumber = firstFree;
                    this.setOffset(x, z, firstFree << 8 | requiredNumSectors);

                    for (int i = 0; i < requiredNumSectors; ++i)
                    {
                        this.sectorFree.set(sectorNumber + i, Boolean.valueOf(false));
                    }

                    this.write(sectorNumber, data, length);
                }
                else
                {
                    this.dataFile.seek(this.dataFile.length());
                    sectorNumber = this.sectorFree.size();

                    for (int i = 0; i < requiredNumSectors; ++i)
                    {
                        this.dataFile.write(emptySector);
                        this.sectorFree.add(Boolean.valueOf(false));
                    }

                    this.write(sectorNumber, data, length);
                    this.setOffset(x, z, sectorNumber << 8 | requiredNumSectors);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * args: sectorNumber, data, length - write the chunk data to this RegionFile
     */
    private void write(int sectorNumber, byte[] data, int length) throws IOException
    {
        this.dataFile.seek((long)(sectorNumber * 4096));
        this.dataFile.writeInt(length + 1);
        this.dataFile.writeByte(2);
        this.dataFile.write(data, 0, length);
    }

    /**
     * args: x, z, offset - sets the chunk's offset in the region file
     */
    private void setOffset(int x, int z, int offset) throws IOException
    {
        this.offsets[x + z * 32] = offset;
        this.dataFile.seek((long)((x + z * 32) * 4));
        this.dataFile.writeInt(offset);
    }

    /**
     * close this RegionFile and prevent further writes
     */
    public void close() throws IOException
    {
        if (this.dataFile != null)
        {
            this.dataFile.close();
        }
    }

    private class ChunkBuffer extends ByteArrayOutputStream
    {
        private int chunkX;
        private int chunkZ;

        private ChunkBuffer(int x, int z)
        {
            super(8096);
            this.chunkX = x;
            this.chunkZ = z;
        }

        public void close() throws IOException
        {
            RegionFile.this.write(this.chunkX, this.chunkZ, this.buf, this.count);
        }
    }
}
