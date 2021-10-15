package net.minecraft.block;

import net.minecraft.world.World;

public class BlockRedstoneTorch extends Block
{
	public void onBlockAdded(World world, int x, int y, int z)
    {
    	if ((world.getBlockMetadata(x, y, z) & 8) > 0)
        {
            world.notifyBlocksOfNeighborChange(x, y - 1, z);
            world.notifyBlocksOfNeighborChange(x, y + 1, z);
            world.notifyBlocksOfNeighborChange(x - 1, y, z);
            world.notifyBlocksOfNeighborChange(x + 1, y, z);
            world.notifyBlocksOfNeighborChange(x, y, z - 1);
            world.notifyBlocksOfNeighborChange(x, y, z + 1);
        }
    }

    public void onBlockBreak(World world, int x, int y, int z, int meta)
    {
        if ((meta & 8) > 0)
        {
            world.notifyBlocksOfNeighborChange(x, y - 1, z);
            world.notifyBlocksOfNeighborChange(x, y + 1, z);
            world.notifyBlocksOfNeighborChange(x - 1, y, z);
            world.notifyBlocksOfNeighborChange(x + 1, y, z);
            world.notifyBlocksOfNeighborChange(x, y, z - 1);
            world.notifyBlocksOfNeighborChange(x, y, z + 1);
        }
    }

    public int isProvidingWeakPower(World world, int x, int y, int z, int side)
    {
    	int meta = world.getBlockMetadata(x, y, z) & 7;
        return (meta == 5 && side == 1)
        	|| (meta == 3 && side == 3)
        	|| (meta == 4 && side == 2)
        	|| (meta == 1 && side == 5)
        	|| (meta == 2 && side == 4)
        	|| (world.getBlockMetadata(x, y, z) & 8) == 0 ? 0 : 15;
    }

    private boolean isGettingPower(World world, int x, int y, int z)
    {
        int meta = world.getBlockMetadata(x, y, z) & 7;
        return meta == 5 && world.getIndirectPowerLevelTo(x, y - 1, z, 0) > 0
        	|| meta == 3 && world.getIndirectPowerLevelTo(x, y, z - 1, 2) > 0
        	|| meta == 4 && world.getIndirectPowerLevelTo(x, y, z + 1, 3) > 0
        	|| meta == 1 && world.getIndirectPowerLevelTo(x - 1, y, z, 4) > 0
        	|| meta == 2 && world.getIndirectPowerLevelTo(x + 1, y, z, 5) > 0;
    }

    /**
     * Ticks the block if it's been scheduled
     */
    public void updateTick(World world, int x, int y, int z)
    {
    	world.setBlockAndMeta(x, y, z, 4, (world.getBlockMetadata(x, y, z) & 7) | (this.isGettingPower(world, x, y, z) ? 0 : 8));
        world.notifyBlocksOfNeighborChange(x, y, z);
        if (world.isSolid(x, y + 1, z)) world.notifyBlocksOfNeighborChange(x, y + 1, z);
    }

    public void onNeighborBlockChange(World world, int x, int y, int z)
    {
    	int meta = world.getBlockMetadata(x, y, z) & 7;

        if (  (!world.isSolid(x - 1, y, z) && meta == 1)
           || (!world.isSolid(x + 1, y, z) && meta == 2)
           || (!world.isSolid(x, y, z - 1) && meta == 3)
           || (!world.isSolid(x, y, z + 1) && meta == 4)
           || (!world.isSolid(x, y - 1, z) && meta == 5))
        {
            world.setBlockAndMeta(x, y, z, 0, 0);
        }
        else
        {
        	if (((world.getBlockMetadata(x, y, z) & 8) == 0) ^ this.isGettingPower(world, x, y, z)) world.scheduleBlockUpdate(x, y, z, 2);
        }
    }

    public int isProvidingStrongPower(World world, int x, int y, int z, int side)
    {
        return side == 0 ? this.isProvidingWeakPower(world, x, y, z, side) : 0;
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
}
