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
        if ((meta & 8) > 0)
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
}
