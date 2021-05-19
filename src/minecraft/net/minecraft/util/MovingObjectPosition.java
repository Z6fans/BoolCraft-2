package net.minecraft.util;

public class MovingObjectPosition
{
    /** x coordinate of the block ray traced against */
    public final int x;

    /** y coordinate of the block ray traced against */
    public final int y;

    /** z coordinate of the block ray traced against */
    public final int z;

    /**
     * Which side was hit. If its -1 then it went the full length of the ray trace. Bottom = 0, Top = 1, East = 2, West
     * = 3, North = 4, South = 5.
     */
    public final int side;

    public MovingObjectPosition(int blockX, int blockY, int blockZ, int sideHit)
    {
        this.x = blockX;
        this.y = blockY;
        this.z = blockZ;
        this.side = sideHit;
    }
}
