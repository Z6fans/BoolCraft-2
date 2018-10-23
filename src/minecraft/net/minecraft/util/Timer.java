package net.minecraft.util;

import net.minecraft.client.Minecraft;

public class Timer
{
    /**
     * How many full ticks have turned over since the last call to updateTimer(), capped at 10.
     */
    public int elapsedTicks;

    /**
     * How much time has elapsed since the last tick, in ticks (range: 0.0 - 1.0).
     */
    public float renderPartialTicks;

    /**
     * The time reported by the system clock at the last sync, in milliseconds
     */
    private long lastSyncSysClock;

    /**
     * The time reported by the high-resolution clock at the last sync, in milliseconds
     */
    private long lastSyncHRClock;
    
    /**
     * The time reported by the high-resolution clock at the last call of updateTimer(), in seconds
     */
    private double lastHRTime;
    private long field_74285_i;

    /**
     * A ratio used to sync the high-resolution clock to the system clock, updated once per second
     */
    private double timeSyncAdjustment = 1.0D;

    public Timer()
    {
        this.lastSyncSysClock = Minecraft.getSystemTime();
        this.lastSyncHRClock = System.nanoTime() / 1000000L;
    }

    /**
     * Updates all fields of the Timer using the current time
     */
    public void updateTimer()
    {
        long var1 = Minecraft.getSystemTime();
        long var3 = var1 - this.lastSyncSysClock;
        long var5 = System.nanoTime() / 1000000L;
        double var7 = (double)var5 / 1000.0D;

        if (var3 <= 1000L && var3 >= 0L)
        {
            this.field_74285_i += var3;

            if (this.field_74285_i > 1000L)
            {
                long var9 = var5 - this.lastSyncHRClock;
                double var11 = (double)this.field_74285_i / (double)var9;
                this.timeSyncAdjustment += (var11 - this.timeSyncAdjustment) * 0.20000000298023224D;
                this.lastSyncHRClock = var5;
                this.field_74285_i = 0L;
            }

            if (this.field_74285_i < 0L)
            {
                this.lastSyncHRClock = var5;
            }
        }
        else
        {
            this.lastHRTime = var7;
        }

        this.lastSyncSysClock = var1;
        double var13 = (var7 - this.lastHRTime) * this.timeSyncAdjustment;
        this.lastHRTime = var7;

        if (var13 < 0.0D)
        {
            var13 = 0.0D;
        }

        if (var13 > 1.0D)
        {
            var13 = 1.0D;
        }

        this.renderPartialTicks = (float)((double)this.renderPartialTicks + var13 * 20);
        this.elapsedTicks = (int)this.renderPartialTicks;
        this.renderPartialTicks -= (float)this.elapsedTicks;

        if (this.elapsedTicks > 10)
        {
            this.elapsedTicks = 10;
        }
    }
}
