package net.minecraft.world.chunk;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class RegionFile
{
    private final RandomAccessFile dataFile;

    public RegionFile(File file)
    {
    	RandomAccessFile newDataFile = null;
    	
        try
        {
        	newDataFile = new RandomAccessFile(file, "rw");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        this.dataFile = newDataFile;
    }

    /**
     * args: x, y - get uncompressed chunk stream from the region file
     */
    public DataInputStream getChunkDataInputStream()
    {
    	try
        {
    		this.dataFile.seek(0);
            int length = this.dataFile.readInt();

            if (length > 0)
            {
            	byte[] data = new byte[length - 1];
                this.dataFile.read(data);
                return new DataInputStream(new ByteArrayInputStream(data));
            }
        }
        catch (IOException e){}
    	
    	return null;
    }

    /**
     * args: x, z - get an output stream used to write chunk data, data is on disk when the returned stream is closed
     */
    public DataOutputStream getChunkDataOutputStream()
    {
        return new DataOutputStream(new RegionFile.ChunkBuffer(this.dataFile));
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

    private class ChunkBuffer extends OutputStream
    {
    	private final RandomAccessFile dataFile;
        /**
         * The buffer where data is stored.
         */
        protected byte buf[];

        /**
         * The number of valid bytes in the buffer.
         */
        protected int count;

        private void ensureCapacity(int minCapacity) {
            if (minCapacity > buf.length)
            {
                int newCapacity = buf.length << 1;
                if (newCapacity < minCapacity) newCapacity = minCapacity;
                if (newCapacity > Integer.MAX_VALUE - 8) newCapacity = Integer.MAX_VALUE - 8;
                buf = Arrays.copyOf(buf, newCapacity);
            }
        }

        /**
         * Writes the specified byte to this byte array output stream.
         *
         * @param   b   the byte to be written.
         */
        public synchronized void write(int b) {
            ensureCapacity(count + 1);
            buf[count] = (byte) b;
            count += 1;
        }

        /**
         * Writes <code>len</code> bytes from the specified byte array
         * starting at offset <code>off</code> to this byte array output stream.
         *
         * @param   b     the data.
         * @param   off   the start offset in the data.
         * @param   len   the number of bytes to write.
         */
        public synchronized void write(byte b[], int off, int len) {
            if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) - b.length > 0)) {
                throw new IndexOutOfBoundsException();
            }
            ensureCapacity(count + len);
            System.arraycopy(b, off, buf, count, len);
            count += len;
        }

        private ChunkBuffer(RandomAccessFile file)
        {
        	this.dataFile = file;
            buf = new byte[8096];
        }

        public void close() throws IOException
        {
            this.dataFile.seek(0);
            this.dataFile.writeInt(this.count + 1);
            this.dataFile.write(this.buf, 0, this.count);
        }
    }
}
