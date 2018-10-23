package net.minecraft.world.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.world.chunk.storage.AnvilChunkLoader;

public class ThreadedFileIOBase implements Runnable
{
    /** Instance of ThreadedFileIOBase */
    public static final ThreadedFileIOBase threadedIOInstance = new ThreadedFileIOBase();
    private List<AnvilChunkLoader> threadedIOQueue = Collections.synchronizedList(new ArrayList<AnvilChunkLoader>());
    private volatile long writeQueuedCounter;
    private volatile long savedIOCounter;
    private volatile boolean isThreadWaiting;

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
}
