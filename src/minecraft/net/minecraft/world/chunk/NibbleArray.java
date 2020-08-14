package net.minecraft.world.chunk;

public class NibbleArray
{
    /**
     * Byte array of data stored in this holder. Possibly a light map or some chunk data. Data is accessed in 4-bit
     * pieces.
     */
    public final byte[] data;

    /**
     * Log base 2 of the chunk height (128); applied as a shift on Z coordinate
     */
    private final int depthBits;

    /**
     * Log base 2 of the chunk height (128) * width (16); applied as a shift on Y coordinate
     */
    private final int depthBitsPlusFour;

    public NibbleArray(int length, int depth)
    {
        this.data = new byte[length >> 1];
        this.depthBits = depth;
        this.depthBitsPlusFour = depth + 4;
    }

    public NibbleArray(byte[] data, int depth)
    {
        this.data = data;
        this.depthBits = depth;
        this.depthBitsPlusFour = depth + 4;
    }

    /**
     * Returns the nibble of data corresponding to the passed in x, y, z. y is at most 6 bits, z is at most 4.
     */
    public int get(int x, int y, int z)
    {
        int nibbAddr = y << this.depthBitsPlusFour | z << this.depthBits | x;
        int byteAddr = nibbAddr >> 1;
        int subAddr = nibbAddr & 1;
        return subAddr == 0 ? this.data[byteAddr] & 15 : this.data[byteAddr] >> 4 & 15;
    }

    /**
     * Arguments are x, y, z, val. Sets the nibble of data at x << 11 | z << 7 | y to val.
     */
    public void set(int x, int y, int z, int val)
    {
        int nibbAddr = y << this.depthBitsPlusFour | z << this.depthBits | x;
        int byteAddr = nibbAddr >> 1;
        int subAddr = nibbAddr & 1;

        if (subAddr == 0)
        {
            this.data[byteAddr] = (byte)(this.data[byteAddr] & 240 | val & 15);
        }
        else
        {
            this.data[byteAddr] = (byte)(this.data[byteAddr] & 15 | (val & 15) << 4);
        }
    }
}
