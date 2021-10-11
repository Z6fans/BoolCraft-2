package net.minecraft.block;

import net.minecraft.world.World;

public class BlockLever extends Block
{
	private final float d = 0.1875F;

    public void onNeighborBlockChange(World world, int x, int y, int z)
    {
    	int meta = world.getBlockMetadata(x, y, z) & 7;

        if (!world.isSolid(x - 1, y, z) && meta == 1
         || !world.isSolid(x + 1, y, z) && meta == 2
         || !world.isSolid(x, y, z - 1) && meta == 3
         || !world.isSolid(x, y, z + 1) && meta == 4
         || !world.isSolid(x, y - 1, z) && meta == 5
         || !world.isSolid(x, y + 1, z) && meta == 0)
        {
            world.setBlockAndMeta(x, y, z, 0, 0);
        }
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

    public void onBlockBreak(World world, int x, int y, int z, int meta)
    {
        if ((meta & 8) > 0)
        {
            world.notifyBlocksOfNeighborChange(x, y, z);
            int side = meta & 7;

            if (side == 1)
            {
                world.notifyBlocksOfNeighborChange(x - 1, y, z);
            }
            else if (side == 2)
            {
                world.notifyBlocksOfNeighborChange(x + 1, y, z);
            }
            else if (side == 3)
            {
                world.notifyBlocksOfNeighborChange(x, y, z - 1);
            }
            else if (side == 4)
            {
                world.notifyBlocksOfNeighborChange(x, y, z + 1);
            }
            else if (side == 5)
            {
                world.notifyBlocksOfNeighborChange(x, y - 1, z);
            }
            else if (side == 0)
            {
                world.notifyBlocksOfNeighborChange(x, y + 1, z);
            }
        }
    }

    public int isProvidingWeakPower(World world, int x, int y, int z, int side)
    {
        return (world.getBlockMetadata(x, y, z) & 8) == 0 ? 0 : 15;
    }

    public int isProvidingStrongPower(World world, int x, int y, int z, int side)
    {
    	int meta = world.getBlockMetadata(x, y, z) & 7;
        return (meta == 0 && side == 0)
        	|| (meta == 5 && side == 1)
        	|| (meta == 4 && side == 2)
        	|| (meta == 3 && side == 3)
        	|| (meta == 2 && side == 4)
        	|| (meta == 1 && side == 5) ? this.isProvidingWeakPower(world, x, y, z, side) : 0;
    }

	public void updateTick(World p_149674_1_, int p_149674_2_, int p_149674_3_, int p_149674_4_){}

	public void onBlockAdded(World world, int x, int y, int z){}
}
