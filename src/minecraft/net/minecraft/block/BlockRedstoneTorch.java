package net.minecraft.block;

import net.minecraft.world.WorldServer;

public class BlockRedstoneTorch extends BlockSmall
{
	protected void notifyAppropriateNeighbors(WorldServer world, int x, int y, int z, int orientation)
	{
		world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
        world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
        world.notifyBlocksOfNeighborChange(x - 1, y, z, this);
        world.notifyBlocksOfNeighborChange(x + 1, y, z, this);
        world.notifyBlocksOfNeighborChange(x, y, z - 1, this);
        world.notifyBlocksOfNeighborChange(x, y, z + 1, this);
	}
	
	public void onBlockAdded(WorldServer world, int x, int y, int z)
    {
		int meta = world.getBlockMetadata(x, y, z);
    	if ((meta & 8) > 0) this.notifyAppropriateNeighbors(world, x, y, z, meta & 7);
    }

    public int isProvidingWeakPower(WorldServer world, int x, int y, int z, int side)
    {
    	int meta = world.getBlockMetadata(x, y, z) & 7;
        return (meta == 5 && side == 1)
        	|| (meta == 3 && side == 3)
        	|| (meta == 4 && side == 2)
        	|| (meta == 1 && side == 5)
        	|| (meta == 2 && side == 4) 
        	|| (world.getBlockMetadata(x, y, z) & 8) == 0 ? 0 : 15;
    }

    private boolean isGettingPower(WorldServer world, int x, int y, int z)
    {
        int meta = world.getBlockMetadata(x, y, z) & 7;
        return meta == 5 && world.getIndirectPowerOutput(x, y - 1, z, 0)
        	|| meta == 3 && world.getIndirectPowerOutput(x, y, z - 1, 2)
        	|| meta == 4 && world.getIndirectPowerOutput(x, y, z + 1, 3)
        	|| meta == 1 && world.getIndirectPowerOutput(x - 1, y, z, 4)
        	|| meta == 2 && world.getIndirectPowerOutput(x + 1, y, z, 5);
    }

    /**
     * Ticks the block if it's been scheduled
     */
    public void updateTick(WorldServer world, int x, int y, int z)
    {
    	world.setBlockMetadataWithNotify(x, y, z, (world.getBlockMetadata(x, y, z) & 7) | (this.isGettingPower(world, x, y, z) ? 0 : 8), true);
        world.notifyBlocksOfNeighborChange(x, y, z, this);
        if (world.getBlock(x, y + 1, z).isSolid()) world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
    }

    public void onNeighborBlockChange(WorldServer world, int x, int y, int z, Block block)
    {
    	int meta = world.getBlockMetadata(x, y, z) & 7;

        if (  (!world.getBlock(x - 1, y, z).isSolid() && meta == 1)
           || (!world.getBlock(x + 1, y, z).isSolid() && meta == 2)
           || (!world.getBlock(x, y, z - 1).isSolid() && meta == 3)
           || (!world.getBlock(x, y, z + 1).isSolid() && meta == 4)
           || (!world.getBlock(x, y - 1, z).isSolid() && meta == 5))
        {
            world.setBlock(x, y, z, Block.air, 0);
        }
        else
        {
        	if (((world.getBlockMetadata(x, y, z) & 8) == 0) ^ this.isGettingPower(world, x, y, z)) world.scheduleBlockUpdate(x, y, z, this, 2);
        }
    }

    public int isProvidingStrongPower(WorldServer world, int x, int y, int z, int side)
    {
        return side == 0 ? this.isProvidingWeakPower(world, x, y, z, side) : 0;
    }

    public boolean canPlaceBlockAt(WorldServer world, int x, int y, int z)
    {
        return world.getBlock(x - 1, y, z).isSolid()
        	|| world.getBlock(x + 1, y, z).isSolid()
        	|| world.getBlock(x, y, z - 1).isSolid()
        	|| world.getBlock(x, y, z + 1).isSolid()
        	|| world.getBlock(x, y - 1, z).isSolid();
    }

    public int onBlockPlaced(WorldServer world, int x, int y, int z, int side)
    {
        int[] xOff = {0, 0, 0, 0, -1, 1};
    	int[] yOff = {-1, 1, 0, 0, 0, 0};
    	int[] zOff = {0, 0, -1, 1, 0, 0};
        
        if(side != 0 && world.getBlock(x - xOff[side], y - yOff[side], z - zOff[side]).isSolid())
        {
        	return ((6 - side) % 6) | 8;
        }
        else if (world.getBlock(x - 1, y, z).isSolid())
        {
            return 9;
        }
        else if (world.getBlock(x + 1, y, z).isSolid())
        {
            return 10;
        }
        else if (world.getBlock(x, y, z - 1).isSolid())
        {
            return 11;
        }
        else if (world.getBlock(x, y, z + 1).isSolid())
        {
            return 12;
        }
        else
        {
            return 13;
        }
    }
    
    /**
     * Returns a integer with hex for 0xrrggbb with this color multiplied against the blocks color. Note only called
     * when first determining what to render.
     */
    public int colorMultiplier(WorldServer world, int x, int y, int z, int said)
    {
        return (world.getBlockMetadata(x, y, z) & 8) > 0 ? 0xFFE91A64 : 0xFF5B0A27;
    }

	public boolean onBlockActivatedServer(WorldServer p_149727_1_, int p_149727_2_, int p_149727_3_, int p_149727_4_)
	{
		return false;
	}
}
