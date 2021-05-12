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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class ThreadedFileIOBase implements Runnable
{
    /** Instance of ThreadedFileIOBase */
    public static final ThreadedFileIOBase threadedIOInstance = new ThreadedFileIOBase();
    private final List<AnvilChunkLoader> threadedIOQueue = Collections.synchronizedList(new ArrayList<AnvilChunkLoader>());
    private volatile long writeQueuedCounter;
    private volatile long savedIOCounter;
    private volatile boolean isThreadWaiting;
    
    /** A map containing Files as keys and RegionFiles as values */
    private final Map<File, RegionFile> regionsByFilename = new HashMap<File, RegionFile>();

    private ThreadedFileIOBase()
    {
        Thread var1 = new Thread(this, "File IO Thread");
        var1.setPriority(1);
        var1.start();
    }

    public void run()
    {
        while (true)
        {
        	for (int var1 = 0; var1 < this.threadedIOQueue.size(); ++var1)
            {
                AnvilChunkLoader var2 = this.threadedIOQueue.get(var1);
                boolean var3 = var2.writeNextIO();

                if (!var3)
                {
                    this.threadedIOQueue.remove(var1--);
                    ++this.savedIOCounter;
                }

                try
                {
                    Thread.sleep(this.isThreadWaiting ? 0L : 10L);
                }
                catch (InterruptedException var6)
                {
                    var6.printStackTrace();
                }
            }

            if (this.threadedIOQueue.isEmpty())
            {
                try
                {
                    Thread.sleep(25L);
                }
                catch (InterruptedException var5)
                {
                    var5.printStackTrace();
                }
            }
        }
    }

    /**
     * threaded io
     */
    public void queueIO(AnvilChunkLoader p_75735_1_)
    {
        if (!this.threadedIOQueue.contains(p_75735_1_))
        {
            ++this.writeQueuedCounter;
            this.threadedIOQueue.add(p_75735_1_);
        }
    }

    public void waitForFinish() throws InterruptedException
    {
        this.isThreadWaiting = true;

        while (this.writeQueuedCounter != this.savedIOCounter)
        {
            Thread.sleep(10L);
        }

        this.isThreadWaiting = false;
    }
    
    private synchronized RegionFile createOrLoadRegionFile(File chunkSaveLocation, int chunkX, int chunkZ)
    {
        File regionDir = new File(chunkSaveLocation, "region");
        File regionFileName = new File(regionDir, "r." + (chunkX >> 5) + "." + (chunkZ >> 5) + ".mca");
        RegionFile regionFile = regionsByFilename.get(regionFileName);

        if (regionFile != null)
        {
            return regionFile;
        }
        else
        {
            if (!regionDir.exists())
            {
                regionDir.mkdirs();
            }

            if (regionsByFilename.size() >= 256)
            {
                clearRegionFileReferences();
            }

            RegionFile newRegionFile = new RegionFile(regionFileName);
            regionsByFilename.put(regionFileName, newRegionFile);
            return newRegionFile;
        }
    }
    
    public synchronized DataInputStream getInputStream(File chunkSaveLocation, int chunkX, int chunkZ)
    {
    	return this.createOrLoadRegionFile(chunkSaveLocation, chunkX, chunkZ).getChunkDataInputStream(chunkX & 31, chunkZ & 31);
    }
    
    public synchronized DataOutputStream getOutputStream(File chunkSaveLocation, int chunkX, int chunkZ)
    {
    	return this.createOrLoadRegionFile(chunkSaveLocation, chunkX, chunkZ).getChunkDataOutputStream(chunkX & 31, chunkZ & 31);
    }

    /**
     * Saves the current Chunk Map Cache
     */
    public synchronized void clearRegionFileReferences()
    {
        for (RegionFile regionFile : regionsByFilename.values())
        {
            try
            {
                if (regionFile != null)
                {
                    regionFile.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        regionsByFilename.clear();
    }
    
    private static class RegionFile
    {
        private static final byte[] emptySector = new byte[4096];
        private final RandomAccessFile dataFile;
        private final int[] offsets = new int[1024];
        private final int[] chunkTimestamps = new int[1024];
        private final ArrayList<Boolean> sectorFree;

        private RegionFile(File file)
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
                newSectorFree = new ArrayList<Boolean>(fileSize);

                for (int i = 0; i < fileSize; ++i)
                {
                	newSectorFree.add(Boolean.valueOf(true));
                }

                newSectorFree.set(0, Boolean.valueOf(false));
                newSectorFree.set(1, Boolean.valueOf(false));
                newDataFile.seek(0L);

                for (int i = 0; i < 1024; ++i)
                {
                    int offsets = newDataFile.readInt();
                    this.offsets[i] = offsets;

                    if (offsets != 0 && (offsets >> 8) + (offsets & 255) <= newSectorFree.size())
                    {
                        for (int var5 = 0; var5 < (offsets & 255); ++var5)
                        {
                        	newSectorFree.set((offsets >> 8) + var5, Boolean.valueOf(false));
                        }
                    }
                }

                for (int i = 0; i < 1024; ++i)
                {
                    int timestamp = newDataFile.readInt();
                    this.chunkTimestamps[i] = timestamp;
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
        public synchronized DataInputStream getChunkDataInputStream(int x, int z)
        {
            if (this.outOfBounds(x, z))
            {
                return null;
            }
            else
            {
                try
                {
                    int offset = this.offsets[x + z * 32];

                    if (offset == 0)
                    {
                        return null;
                    }
                    else
                    {
                        int sectorNumber = offset >> 8;
                        int numSectors = offset & 255;

                        if (sectorNumber + numSectors > this.sectorFree.size())
                        {
                            return null;
                        }
                        else
                        {
                            this.dataFile.seek((long)(sectorNumber * 4096));
                            int length = this.dataFile.readInt();

                            if (length > 4096 * numSectors)
                            {
                                return null;
                            }
                            else if (length <= 0)
                            {
                                return null;
                            }
                            else
                            {
                                byte type = this.dataFile.readByte();

                                if (type == 1)
                                {
                                	byte[] data = new byte[length - 1];
                                    this.dataFile.read(data);
                                    return new DataInputStream(new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(data))));
                                }
                                else if (type == 2)
                                {
                                	byte[] data = new byte[length - 1];
                                    this.dataFile.read(data);
                                    return new DataInputStream(new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(data))));
                                }
                                else
                                {
                                    return null;
                                }
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    return null;
                }
            }
        }

        /**
         * args: x, z - get an output stream used to write chunk data, data is on disk when the returned stream is closed
         */
        public DataOutputStream getChunkDataOutputStream(int x, int z)
        {
            return this.outOfBounds(x, z) ? null : new DataOutputStream(new DeflaterOutputStream(new RegionFile.ChunkBuffer(x, z)));
        }

        /**
         * args: x, z, data, length - write chunk data at (x, z) to disk
         */
        private synchronized void write(int x, int z, byte[] data, int length)
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

                this.setChunkTimestamp(x, z, (int)(System.currentTimeMillis() / 1000L));
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
         * args: x, z - check region bounds
         */
        private boolean outOfBounds(int x, int z)
        {
            return x < 0 || x >= 32 || z < 0 || z >= 32;
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
         * args: x, z, timestamp - sets the chunk's write timestamp
         */
        private void setChunkTimestamp(int x, int z, int timestamp) throws IOException
        {
            this.chunkTimestamps[x + z * 32] = timestamp;
            this.dataFile.seek((long)(4096 + (x + z * 32) * 4));
            this.dataFile.writeInt(timestamp);
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
}
