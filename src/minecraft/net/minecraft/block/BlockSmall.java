package net.minecraft.block;

import net.minecraft.world.WorldServer;

public abstract class BlockSmall extends Block
{
	private final float d = 0.1875F;
	
    public boolean isSolid()
    {
        return false;
    }
    
    protected double minX(int meta)
    {
    	int s = meta & 7;
    	return s == 1 ? 0.0F : s == 2 ? 1.0F - d : 0.5F - d;
    }
    
    protected double minY(int meta)
    {
    	int s = meta & 7;
    	return s == 5 ? 0.0F : s == 0 ? 1.0F - d : 0.5F - d;
    }
    
    protected double minZ(int meta)
    {
    	int s = meta & 7;
    	return s == 3 ? 0.0F : s == 4 ? 1.0F - d : 0.5F - d;
    }
    
    protected double maxX(int meta)
    {
    	int s = meta & 7;
    	return s == 1 ? d : s == 2 ? 1.0F : 0.5F + d;
    }
    
    protected double maxY(int meta)
    {
    	int s = meta & 7;
    	return s == 5 ? d : s == 0 ? 1.0F : 0.5F + d;
    }
    
    protected double maxZ(int meta)
    {
    	int s = meta & 7;
    	return s == 3 ? d : s == 4 ? 1.0F : 0.5F + d;
    }

	public boolean isReplaceable()
	{
		return false;
	}

    /**
     * Can this block provide power. Only wire currently seems to have this change based on its state.
     */
    public boolean canProvidePower()
    {
        return true;
    }
    /**
     * The type of render function that is called for this block
     */
    public int getRenderType()
    {
        return 0;
    }

    public void breakBlock(WorldServer world, int x, int y, int z, Block block, int meta)
    {
        if ((meta & 8) > 0) this.notifyAppropriateNeighbors(world, x, y, z, meta & 7);
    }
    
    protected abstract void notifyAppropriateNeighbors(WorldServer world, int x, int y, int z, int orientation);
}