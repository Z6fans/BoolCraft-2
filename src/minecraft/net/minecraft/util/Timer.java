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
    private long diffSysAcc;

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
        long currentSysClock = Minecraft.getSystemTime();
        long diffSysClock = currentSysClock - this.lastSyncSysClock;
        
        long currentHRClock = System.nanoTime() / 1000000L;
        double currentHRClockSecs = (double)currentHRClock / 1000.0D;

        if (diffSysClock <= 1000L && diffSysClock >= 0L)
        {
            this.diffSysAcc += diffSysClock;

            if (this.diffSysAcc > 1000L)
            {
                long diffHRClock = currentHRClock - this.lastSyncHRClock;
                double newTimeSync = (double)this.diffSysAcc / (double)diffHRClock;
                this.timeSyncAdjustment += (newTimeSync - this.timeSyncAdjustment) * 0.20000000298023224D;
                this.lastSyncHRClock = currentHRClock;
                this.diffSysAcc = 0L;
            }

            if (this.diffSysAcc < 0L)
            {
                this.lastSyncHRClock = currentHRClock;
            }
        }
        else
        {
            this.lastHRTime = currentHRClockSecs;
        }

        this.lastSyncSysClock = currentSysClock;
        double diffHRClockSecs = (currentHRClockSecs - this.lastHRTime) * this.timeSyncAdjustment;
        this.lastHRTime = currentHRClockSecs;

        if (diffHRClockSecs < 0.0D)
        {
            diffHRClockSecs = 0.0D;
        }

        if (diffHRClockSecs > 1.0D)
        {
            diffHRClockSecs = 1.0D;
        }

        this.renderPartialTicks = (float)((double)this.renderPartialTicks + diffHRClockSecs * 20);
        this.elapsedTicks = (int)this.renderPartialTicks;
        this.renderPartialTicks -= (float)this.elapsedTicks;

        if (this.elapsedTicks > 10)
        {
            this.elapsedTicks = 10;
        }
    }
}
