package net.minecraft.util;

public class MathHelper
{
    /**
     * A table of sin values computed from 0 (inclusive) to 2*pi (exclusive), with steps of 2*PI / 65536.
     */
    private static final double[] SIN_TABLE = new double[65536];

    /**
     * sin looked up in a table
     */
    public static final double sin(double f)
    {
        return SIN_TABLE[(int)(f * 10430.378D) & 65535];
    }

    /**
     * cos looked up in the sin table with the appropriate offset
     */
    public static final double cos(double f)
    {
        return SIN_TABLE[(int)(f * 10430.378D + 16384.0D) & 65535];
    }

    /**
     * Returns the greatest integer less than or equal to the double argument
     */
    public static final int floor_double(double d)
    {
        int i = (int)d;
        return d < (double)i ? i - 1 : i;
    }

    static
    {
        for (int i = 0; i < 65536; ++i)
        {
            SIN_TABLE[i] = Math.sin((double)i * Math.PI * 2.0D / 65536.0D);
        }
    }
}
