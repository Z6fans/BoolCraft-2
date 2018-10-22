package net.minecraft.util;

public class MovingObjectPosition
{
    /** x coordinate of the block ray traced against */
    public int blockX;

    /** y coordinate of the block ray traced against */
    public int blockY;

    /** z coordinate of the block ray traced against */
    public int blockZ;

    /**
     * Which side was hit. If its -1 then it went the full length of the ray trace. Bottom = 0, Top = 1, East = 2, West
     * = 3, North = 4, South = 5.
     */
    public int sideHit;
    private static final String __OBFID = "CL_00000610";

    public MovingObjectPosition(int p_i45481_1_, int p_i45481_2_, int p_i45481_3_, int p_i45481_4_)
    {
        this.blockX = p_i45481_1_;
        this.blockY = p_i45481_2_;
        this.blockZ = p_i45481_3_;
        this.sideHit = p_i45481_4_;
    }
}
