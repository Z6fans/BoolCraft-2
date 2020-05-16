package net.minecraft.client.renderer;

import java.util.Comparator;

import net.minecraft.client.EntityPlayer;

public class RenderSorter implements Comparator<WorldRenderer>
{
    /** The entity (usually the player) that the camera is inside. */
    private final EntityPlayer baseEntity;

    public RenderSorter(EntityPlayer entity)
    {
        this.baseEntity = entity;
    }

    public int compare(WorldRenderer wr1, WorldRenderer wr2)
    {
        if (wr1.isInFrustum && !wr2.isInFrustum)
        {
            return 1;
        }
        else if (wr2.isInFrustum && !wr1.isInFrustum)
        {
            return -1;
        }
        else
        {
            double quad1 = (double)wr1.quadranceToPlayer(this.baseEntity);
            double quad2 = (double)wr2.quadranceToPlayer(this.baseEntity);
            return quad1 < quad2 ? 1 : (quad1 > quad2 ? -1 : (wr1.chunkIndex < wr2.chunkIndex ? 1 : -1));
        }
    }
}
