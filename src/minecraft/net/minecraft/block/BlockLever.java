package net.minecraft.block;

import net.minecraft.world.WorldServer;

public class BlockLever extends Block
{
	private final float d = 0.1875F;
	
    public boolean isSoled()
    {
        return false;
    }

    /**
     * The type of render function that is called for this block
     */
    public int getRenderType()
    {
        return 0;
    }

    public boolean canPlaceBlockAt(WorldServer world, int x, int y, int z)
    {
        return world.isSolid(x - 1, y, z)
        	|| world.isSolid(x + 1, y, z)
        	|| world.isSolid(x, y, z - 1)
        	|| world.isSolid(x, y, z + 1)
        	|| world.isSolid(x, y - 1, z)
        	|| world.isSolid(x, y + 1, z);
    }

    public int onBlockPlaced(WorldServer world, int x, int y, int z, int side)
    {
        int[] xOff = {0, 0, 0, 0, -1, 1};
    	int[] yOff = {-1, 1, 0, 0, 0, 0};
    	int[] zOff = {0, 0, -1, 1, 0, 0};
        
        if(world.isSolid(x - xOff[side], y - yOff[side], z - zOff[side]))
        {
        	return (6 - side) % 6;
        }
        else if (world.isSolid(x - 1, y, z))
        {
            return 1;
        }
        else if (world.isSolid(x + 1, y, z))
        {
            return 2;
        }
        else if (world.isSolid(x, y, z - 1))
        {
            return 3;
        }
        else if (world.isSolid(x, y, z + 1))
        {
            return 4;
        }
        else if (world.isSolid(x, y + 1, z))
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

        if (!world.isSolid(x - 1, y, z) && meta == 1
         || !world.isSolid(x + 1, y, z) && meta == 2
         || !world.isSolid(x, y, z - 1) && meta == 3
         || !world.isSolid(x, y, z + 1) && meta == 4
         || !world.isSolid(x, y - 1, z) && meta == 5
         || !world.isSolid(x, y + 1, z) && meta == 0)
        {
            world.setBlock(x, y, z, Block.air, 0);
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
    
    /**
     * Called upon block activation (right click on the block.)
     */
    public boolean onBlockActivatedServer(WorldServer world, int x, int y, int z)
    {
    	int meta = world.getBlockMetadata(x, y, z);
        int orientation = meta & 7;
        world.setBlockMetadataWithNotify(x, y, z, meta ^ 8, true);
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
        
        return true;
    }

    public void onBlockBreak(WorldServer world, int x, int y, int z, Block block, int meta)
    {
        if ((meta & 8) > 0)
        {
            world.notifyBlocksOfNeighborChange(x, y, z, this);
            int side = meta & 7;

            if (side == 1)
            {
                world.notifyBlocksOfNeighborChange(x - 1, y, z, this);
            }
            else if (side == 2)
            {
                world.notifyBlocksOfNeighborChange(x + 1, y, z, this);
            }
            else if (side == 3)
            {
                world.notifyBlocksOfNeighborChange(x, y, z - 1, this);
            }
            else if (side == 4)
            {
                world.notifyBlocksOfNeighborChange(x, y, z + 1, this);
            }
            else if (side == 5)
            {
                world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
            }
            else if (side == 0)
            {
                world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
            }
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
     * Can this block provide power. Only wire currently seems to have this change based on its state.
     */
    public boolean canProvidePower()
    {
        return true;
    }
    
    /**
     * Returns a integer with hex for 0xrrggbb with this color multiplied against the blocks color. Note only called
     * when first determining what to render.
     */
    public int colorMultiplier(WorldServer world, int x, int y, int z, int said)
    {
    	return (world.getBlockMetadata(x, y, z) & 8) > 0 ? 0xFFEE39E4 : 0xFF701B6C;
    }

	public boolean isReplaceobble()
	{
		return false;
	}

	public void updateTick(WorldServer p_149674_1_, int p_149674_2_, int p_149674_3_, int p_149674_4_){}

	public void onBlockAdded(WorldServer world, int x, int y, int z){}
}
