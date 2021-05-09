package net.minecraft.block;

import net.minecraft.client.WorldClient;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;

public class BlockRedstoneTorch extends Block
{
    public void onBlockAdded(WorldServer world, int x, int y, int z)
    {
    	int lit = world.getBlockMetadata(x, y, z) & 8;
        if ((world.getBlockMetadata(x, y, z) & 7) == 0)
        {
        	if (world.isBlockNormalCubeDefault(x - 1, y, z))
            {
                world.setBlockMetadataWithNotify(x, y, z, 1 | lit, false);
            }
            else if (world.isBlockNormalCubeDefault(x + 1, y, z))
            {
                world.setBlockMetadataWithNotify(x, y, z, 2 | lit, false);
            }
            else if (world.isBlockNormalCubeDefault(x, y, z - 1))
            {
                world.setBlockMetadataWithNotify(x, y, z, 3 | lit, false);
            }
            else if (world.isBlockNormalCubeDefault(x, y, z + 1))
            {
                world.setBlockMetadataWithNotify(x, y, z, 4 | lit, false);
            }
            else if (world.isBlockNormalCubeDefault(x, y - 1, z))
            {
                world.setBlockMetadataWithNotify(x, y, z, 5 | lit, false);
            }

            if (!this.canPlaceBlockAt(world, x, y, z) && world.getBlock(x, y, z) == this) world.setBlock(x, y, z, Block.air, 0);
        }

        if ((world.getBlockMetadata(x, y, z) & 8) > 0)
        {
            world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
            world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
            world.notifyBlocksOfNeighborChange(x - 1, y, z, this);
            world.notifyBlocksOfNeighborChange(x + 1, y, z, this);
            world.notifyBlocksOfNeighborChange(x, y, z - 1, this);
            world.notifyBlocksOfNeighborChange(x, y, z + 1, this);
        }
    }

    public void breakBlock(WorldServer world, int x, int y, int z, Block block, int meta)
    {
        if ((world.getBlockMetadata(x, y, z) & 8) > 0)
        {
            world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
            world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
            world.notifyBlocksOfNeighborChange(x - 1, y, z, this);
            world.notifyBlocksOfNeighborChange(x + 1, y, z, this);
            world.notifyBlocksOfNeighborChange(x, y, z - 1, this);
            world.notifyBlocksOfNeighborChange(x, y, z + 1, this);
        }
    }

    public int isProvidingWeakPower(WorldServer world, int x, int y, int z, int side)
    {
    	int meta = world.getBlockMetadata(x, y, z) & 7;
    	int lit = world.getBlockMetadata(x, y, z) & 8;
        return (meta == 5 && side == 1)
        	|| (meta == 3 && side == 3)
        	|| (meta == 4 && side == 2)
        	|| (meta == 1 && side == 5)
        	|| (meta == 2 && side == 4) 
        	|| lit == 0 ? 0 : 15;
    }

    private boolean isGettingPower(WorldServer world, int x, int y, int z)
    {
        int meta = world.getBlockMetadata(x, y, z) & 7;
        return meta == 5 && world.getIndirectPowerOutput(x, y - 1, z, 0) ? true : (meta == 3 && world.getIndirectPowerOutput(x, y, z - 1, 2) ? true : (meta == 4 && world.getIndirectPowerOutput(x, y, z + 1, 3) ? true : (meta == 1 && world.getIndirectPowerOutput(x - 1, y, z, 4) ? true : meta == 2 && world.getIndirectPowerOutput(x + 1, y, z, 5))));
    }

    /**
     * Ticks the block if it's been scheduled
     */
    public void updateTick(WorldServer world, int x, int y, int z)
    {
        boolean powered = this.isGettingPower(world, x, y, z);
        int meta = world.getBlockMetadata(x, y, z) & 7;

        if ((world.getBlockMetadata(x, y, z) & 8) > 0)
        {
            if (powered)
            {
                world.setBlockMetadataWithNotify(x, y, z, meta, true);
                world.notifyBlocksOfNeighborChange(x, y, z, this);
            }
        }
        else if (!powered)
        {
            world.setBlockMetadataWithNotify(x, y, z, meta | 8, true);
            world.notifyBlocksOfNeighborChange(x, y, z, this);
        }
    }

    public void onNeighborBlockChange(WorldServer world, int x, int y, int z, Block block)
    {
    	if (this.canPlaceBlockAt(world, x, y, z))
        {
            int meta = world.getBlockMetadata(x, y, z) & 7;

            if (  (!world.isBlockNormalCubeDefault(x - 1, y, z) && meta == 1)
               || (!world.isBlockNormalCubeDefault(x + 1, y, z) && meta == 2)
               || (!world.isBlockNormalCubeDefault(x, y, z - 1) && meta == 3)
               || (!world.isBlockNormalCubeDefault(x, y, z + 1) && meta == 4)
               || (!world.isBlockNormalCubeDefault(x, y - 1, z) && meta == 5))
            {
                world.setBlock(x, y, z, Block.air, 0);
            }
            else
            {
            	if (((world.getBlockMetadata(x, y, z) & 8) > 0) ^ !this.isGettingPower(world, x, y, z)) world.scheduleBlockUpdate(x, y, z, this, 2);
            }
        }
    	else if (world.getBlock(x, y, z) == this)
    	{
    		world.setBlock(x, y, z, Block.air, 0);
    	}
    }

    public int isProvidingStrongPower(WorldServer world, int x, int y, int z, int side)
    {
        return side == 0 ? this.isProvidingWeakPower(world, x, y, z, side) : 0;
    }

    /**
     * Can this block provide power. Only wire currently seems to have this change based on its state.
     */
    public boolean canProvidePower()
    {
        return true;
    }
    
    public AxisAlignedBB getCollisionBoundingBoxFromPool(int p_149668_2_, int p_149668_3_, int p_149668_4_)
    {
        return null;
    }
    
    public boolean isSolid()
    {
        return false;
    }
    
    public int getRenderType()
    {
        return 12;
    }

    protected boolean canPlaceBlockAt(WorldServer world, int x, int y, int z)
    {
        return world.isBlockNormalCubeDefault(x - 1, y, z) || world.isBlockNormalCubeDefault(x + 1, y, z) || world.isBlockNormalCubeDefault(x, y, z - 1) || world.isBlockNormalCubeDefault(x, y, z + 1) || world.getBlock(x, y - 1, z).isSolid();
    }

    public int onBlockPlaced(WorldServer world, int x, int y, int z, int side)
    {
        int var10 = 0;

        if (side == 1 && world.isBlockNormalCubeDefault(x, y - 1, z))
        {
            var10 = 5;
        }

        if (side == 2 && world.isBlockNormalCubeDefault(x, y, z + 1))
        {
            var10 = 4;
        }

        if (side == 3 && world.isBlockNormalCubeDefault(x, y, z - 1))
        {
            var10 = 3;
        }

        if (side == 4 && world.isBlockNormalCubeDefault(x + 1, y, z))
        {
            var10 = 2;
        }

        if (side == 5 && world.isBlockNormalCubeDefault(x - 1, y, z))
        {
            var10 = 1;
        }

        return var10 | 8;
    }

    public MovingObjectPosition collisionRayTrace(WorldClient world, int x, int y, int z, Vec3 playerPos, Vec3 playerLook)
    {
        int meta = world.getBlockMetadata(x, y, z) & 7;

        float d = 0.1875F;

        if (meta == 5)
        {
            this.setBlockBounds(0.5F - d, 0.0F, 0.5F - d, 0.5F + d, d, 0.5F + d);
        }
        else if (meta == 6)
        {
            this.setBlockBounds(0.5F - d, 0.0F, 0.5F - d, 0.5F + d, d, 0.5F + d);
        }
        else if (meta == 4)
        {
            this.setBlockBounds(0.5F - d, 0.5F - d, 1.0F - d, 0.5F + d, 0.5F + d, 1.0F);
        }
        else if (meta == 3)
        {
            this.setBlockBounds(0.5F - d, 0.5F - d, 0.0F, 0.5F + d, 0.5F + d, d);
        }
        else if (meta == 2)
        {
            this.setBlockBounds(1.0F - d, 0.5F - d, 0.5F - d, 1.0F, 0.5F + d, 0.5F + d);
        }
        else if (meta == 1)
        {
            this.setBlockBounds(0.0F, 0.5F - d, 0.5F - d, d, 0.5F + d, 0.5F + d);
        }
        else if (meta == 0)
        {
            this.setBlockBounds(0.5F - d, 1.0F - d, 0.5F - d, 0.5F + d, 1.0F, 0.5F + d);
        }
        else if (meta == 7)
        {
            this.setBlockBounds(0.5F - d, 1.0F - d, 0.5F - d, 0.5F + d, 1.0F, 0.5F + d);
        }

        return super.collisionRayTrace(world, x, y, z, playerPos, playerLook);
    }
    
    /**
     * Returns a integer with hex for 0xrrggbb with this color multiplied against the blocks color. Note only called
     * when first determining what to render.
     */
    public int colorMultiplier(WorldClient world, int x, int y, int z)
    {
        return (world.getBlockMetadata(x, y, z) & 8) > 0 ? 0xE91A64 : 0x5B0A27;
    }

	public boolean isReplaceable()
	{
		return false;
	}

	public boolean onBlockActivatedServer(WorldServer p_149727_1_, int p_149727_2_, int p_149727_3_, int p_149727_4_)
	{
		return false;
	}

	public boolean getTickRandomly()
	{
		return false;
	}
}
