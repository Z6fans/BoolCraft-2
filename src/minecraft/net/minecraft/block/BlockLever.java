package net.minecraft.block;

import net.minecraft.world.WorldServer;

public class BlockLever extends BlockSmall
{
    public boolean canPlaceBlockAt(WorldServer world, int x, int y, int z)
    {
        return world.getBlock(x - 1, y, z).isSolid()
        	|| world.getBlock(x + 1, y, z).isSolid()
        	|| world.getBlock(x, y, z - 1).isSolid()
        	|| world.getBlock(x, y, z + 1).isSolid()
        	|| world.getBlock(x, y - 1, z).isSolid()
        	|| world.getBlock(x, y + 1, z).isSolid();
    }

    public int onBlockPlaced(WorldServer world, int x, int y, int z, int side)
    {
        int[] xOff = {0, 0, 0, 0, -1, 1};
    	int[] yOff = {-1, 1, 0, 0, 0, 0};
    	int[] zOff = {0, 0, -1, 1, 0, 0};
        
        if(world.getBlock(x - xOff[side], y - yOff[side], z - zOff[side]).isSolid())
        {
        	return (6 - side) % 6;
        }
        else if (world.getBlock(x - 1, y, z).isSolid())
        {
            return 1;
        }
        else if (world.getBlock(x + 1, y, z).isSolid())
        {
            return 2;
        }
        else if (world.getBlock(x, y, z - 1).isSolid())
        {
            return 3;
        }
        else if (world.getBlock(x, y, z + 1).isSolid())
        {
            return 4;
        }
        else if (world.getBlock(x, y + 1, z).isSolid())
        {
            return 0;
        }
        else
        {
        	return 5;
        }
    }

    public void onNeighborBlockChange(WorldServer world, int x, int y, int z, Block block)
    {
    	int meta = world.getBlockMetadata(x, y, z) & 7;

        if (!world.getBlock(x - 1, y, z).isSolid() && meta == 1
         || !world.getBlock(x + 1, y, z).isSolid() && meta == 2
         || !world.getBlock(x, y, z - 1).isSolid() && meta == 3
         || !world.getBlock(x, y, z + 1).isSolid() && meta == 4
         || !world.getBlock(x, y - 1, z).isSolid() && meta == 5
         || !world.getBlock(x, y + 1, z).isSolid() && meta == 0)
        {
            world.setBlock(x, y, z, Block.air, 0);
        }
    }
    
    /**
     * Called upon block activation (right click on the block.)
     */
    public boolean onBlockActivatedServer(WorldServer world, int x, int y, int z)
    {
    	int meta = world.getBlockMetadata(x, y, z);
        world.setBlockMetadataWithNotify(x, y, z, meta ^ 8, true);
        this.notifyAppropriateNeighbors(world, x, y, z, meta & 7);
        return true;
    }
    
    protected void notifyAppropriateNeighbors(WorldServer world, int x, int y, int z, int orientation)
    {
    	world.notifyBlocksOfNeighborChange(x, y, z, this);

        if (orientation == 1)
        {
            world.notifyBlocksOfNeighborChange(x - 1, y, z, this);
        }
        else if (orientation == 2)
        {
            world.notifyBlocksOfNeighborChange(x + 1, y, z, this);
        }
        else if (orientation == 3)
        {
            world.notifyBlocksOfNeighborChange(x, y, z - 1, this);
        }
        else if (orientation == 4)
        {
            world.notifyBlocksOfNeighborChange(x, y, z + 1, this);
        }
        else if (orientation == 5)
        {
            world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
        }
        else if (orientation == 0)
        {
            world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
        }
    }

    public int isProvidingWeakPower(WorldServer world, int x, int y, int z, int side)
    {
        return (world.getBlockMetadata(x, y, z) & 8) == 0 ? 0 : 15;
    }

    public int isProvidingStrongPower(WorldServer world, int x, int y, int z, int side)
    {
    	int meta = world.getBlockMetadata(x, y, z) & 7;
        return (meta == 0 && side == 0)
        	|| (meta == 5 && side == 1)
        	|| (meta == 4 && side == 2)
        	|| (meta == 3 && side == 3)
        	|| (meta == 2 && side == 4)
        	|| (meta == 1 && side == 5) ? this.isProvidingWeakPower(world, x, y, z, side) : 0;
    }
    
    /**
     * Returns a integer with hex for 0xrrggbb with this color multiplied against the blocks color. Note only called
     * when first determining what to render.
     */
    public int colorMultiplier(WorldServer world, int x, int y, int z, int said)
    {
    	return (world.getBlockMetadata(x, y, z) & 8) > 0 ? 0xFFEE39E4 : 0xFF701B6C;
    }

	public void updateTick(WorldServer p_149674_1_, int p_149674_2_, int p_149674_3_, int p_149674_4_){}

	public void onBlockAdded(WorldServer world, int x, int y, int z){}
}
