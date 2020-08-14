package net.minecraft.client.renderer;

public class ScaledResolution
{
    private int scaledWidth;
    private int scaledHeight;

    public ScaledResolution(int w, int h)
    {
        this.scaledWidth = w;
        this.scaledHeight = h;
        int scaleFactor = 1;

        while (scaleFactor < 1000 && this.scaledWidth / (scaleFactor + 1) >= 320 && this.scaledHeight / (scaleFactor + 1) >= 240)
        {
            ++scaleFactor;
        }

        if (scaleFactor % 2 != 0 && scaleFactor != 1)
        {
            --scaleFactor;
        }
        this.scaledWidth = (int)Math.ceil((double)this.scaledWidth / (double)scaleFactor);
        this.scaledHeight = (int)Math.ceil((double)this.scaledHeight / (double)scaleFactor);
    }

    public int getScaledWidth()
    {
        return this.scaledWidth;
    }

    public int getScaledHeight()
    {
        return this.scaledHeight;
    }
}
