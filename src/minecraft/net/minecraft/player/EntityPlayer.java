package net.minecraft.player;

public abstract class EntityPlayer
{
    /** Entity position X */
    public double posX;

    /** Entity position Y */
    public double posY;

    /** Entity position Z */
    public double posZ;

    /** Has this entity been added to the chunk its within */
    public boolean addedToChunk;
    public int chunkCoordX;
    public int chunkCoordY;
    public int chunkCoordZ;

    /**
     * Called to update the entity's position/logic.
     */
    public abstract void onUpdate();
}
