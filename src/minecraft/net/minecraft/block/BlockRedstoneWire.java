package net.minecraft.block;

import net.minecraft.client.WorldClient;
import net.minecraft.world.WorldServer;

public class BlockRedstoneWire extends Block
{
    private boolean isCheckingForPower = false;

    public BlockRedstoneWire()
    {
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F);
    }
    
    public boolean isSolid()
    {
        return false;
    }

    /**
     * The type of render function that is called for this block
     */
    public int getRenderType()
    {
        return 5;
    }

    /**
     * Returns a integer with hex for 0xrrggbb with this color multiplied against the blocks color. Note only called
     * when first determining what to render.
     */
    public int colorMultiplier(WorldClient world, int x, int y, int z)
    {
    	int scale[] = {0, 136, 181, 204, 218, 227, 233, 238, 242, 245, 247, 249, 251, 253, 254, 255};
        return 0x101 * scale[world.getBlockMetadata(x, y, z)];
    }

    public boolean canPlaceBlockAt(WorldServer world, int x, int y, int z)
    {
        return world.getBlock(x, y - 1, z).isSolid();
    }

    private void computeMetadata(WorldServer world, int x, int y, int z)
    {
        int prevPower = world.getBlockMetadata(x, y, z);
        int currentPower = this.maxRedstonePowerAt(world, x, y, z, 0);
        this.isCheckingForPower = true;
        int indirectPower = world.getStrongestIndirectPower(x, y, z);
        this.isCheckingForPower = false;

        int directPower = 0;

        for (int side2D = 0; side2D < 4; ++side2D)
        {
            int xToCheck = x;
            int zToCheck = z;

            if (side2D == 0)
            {
                xToCheck = x - 1;
            }

            if (side2D == 1)
            {
                ++xToCheck;
            }

            if (side2D == 2)
            {
                zToCheck = z - 1;
            }

            if (side2D == 3)
            {
                ++zToCheck;
            }

            if (xToCheck != x || zToCheck != z)
            {
                directPower = this.maxRedstonePowerAt(world, xToCheck, y, zToCheck, directPower);
            }

            if (world.getBlock(xToCheck, y, zToCheck).isSolid() && !world.getBlock(x, y + 1, z).isSolid())
            {
                if (xToCheck != x || zToCheck != z)
                {
                    directPower = this.maxRedstonePowerAt(world, xToCheck, y + 1, zToCheck, directPower);
                }
            }
            else if (!world.getBlock(xToCheck, y, zToCheck).isSolid() && (xToCheck != x || zToCheck != z))
            {
                directPower = this.maxRedstonePowerAt(world, xToCheck, y - 1, zToCheck, directPower);
            }
        }

        if (directPower > currentPower)
        {
            currentPower = directPower - 1;
        }
        else if (currentPower > 0)
        {
            --currentPower;
        }
        else
        {
            currentPower = 0;
        }

        if (indirectPower > currentPower)
        {
            currentPower = indirectPower;
        }

        if (prevPower != currentPower)
        {
            world.setBlockMetadataWithNotify(x, y, z, currentPower, false);
            this.notifyNeighbors(world, x, y, z);
        }
    }

    private int maxRedstonePowerAt(WorldServer world, int x, int y, int z, int other)
    {
        if (world.getBlock(x, y, z) != this)
        {
            return other;
        }
        else
        {
            return Math.max(world.getBlockMetadata(x, y, z), other);
        }
    }

    private void propagateSignal(WorldServer world, int x, int y, int z)
    {
        if (world.getBlock(x, y, z) == this)
        {
        	this.notifyNeighbors(world, x - 1, y, z);
            this.notifyNeighbors(world, x + 1, y, z);
            this.notifyNeighbors(world, x, y, z - 1);
            this.notifyNeighbors(world, x, y, z + 1);

            if (world.getBlock(x - 1, y, z).isSolid())
            {
                this.notifyNeighbors(world, x - 1, y + 1, z);
            }
            else
            {
                this.notifyNeighbors(world, x - 1, y - 1, z);
            }

            if (world.getBlock(x + 1, y, z).isSolid())
            {
                this.notifyNeighbors(world, x + 1, y + 1, z);
            }
            else
            {
                this.notifyNeighbors(world, x + 1, y - 1, z);
            }

            if (world.getBlock(x, y, z - 1).isSolid())
            {
                this.notifyNeighbors(world, x, y + 1, z - 1);
            }
            else
            {
                this.notifyNeighbors(world, x, y - 1, z - 1);
            }

            if (world.getBlock(x, y, z + 1).isSolid())
            {
                this.notifyNeighbors(world, x, y + 1, z + 1);
            }
            else
            {
                this.notifyNeighbors(world, x, y - 1, z + 1);
            }
        }
    }
    
    private void notifyNeighbors(WorldServer world, int x, int y, int z)
    {
    	world.notifyBlocksOfNeighborChange(x, y, z, this);
        world.notifyBlocksOfNeighborChange(x - 1, y, z, this);
        world.notifyBlocksOfNeighborChange(x + 1, y, z, this);
        world.notifyBlocksOfNeighborChange(x, y, z - 1, this);
        world.notifyBlocksOfNeighborChange(x, y, z + 1, this);
        world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
        world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
    }

    public void onBlockAdded(WorldServer world, int x, int y, int z)
    {
    	this.computeMetadata(world, x, y, z);
        world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
        world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
        this.propagateSignal(world, x, y, z);
    }

    public void breakBlock(WorldServer world, int x, int y, int z, Block block, int meta)
    {
    	world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
        world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
        world.notifyBlocksOfNeighborChange(x + 1, y, z, this);
        world.notifyBlocksOfNeighborChange(x - 1, y, z, this);
        world.notifyBlocksOfNeighborChange(x, y, z + 1, this);
        world.notifyBlocksOfNeighborChange(x, y, z - 1, this);
        this.computeMetadata(world, x, y, z);
        this.propagateSignal(world, x, y, z);
    }

    public void onNeighborBlockChange(WorldServer world, int x, int y, int z, Block block)
    {
    	if (this.canPlaceBlockAt(world, x, y, z))
        {
            this.computeMetadata(world, x, y, z);
        }
        else
        {
            world.setBlock(x, y, z, Block.air, 0);
        }
    }

    public int isProvidingStrongPower(WorldServer world, int x, int y, int z, int side)
    {
        return this.isProvidingWeakPower(world, x, y, z, side);
    }

    public int isProvidingWeakPower(WorldServer world, int x, int y, int z, int side)
    {
        if (this.isCheckingForPower)
        {
            return 0;
        }
        else
        {
            int meta = world.getBlockMetadata(x, y, z);

            if (meta == 0)
            {
                return 0;
            }
            else if (side == 1)
            {
                return meta;
            }
            else
            {
                boolean xm = shouldConnect(world, x - 1, y, z, true) || !world.getBlock(x - 1, y, z).isSolid() && shouldConnect(world, x - 1, y - 1, z, false);
                boolean xp = shouldConnect(world, x + 1, y, z, true) || !world.getBlock(x + 1, y, z).isSolid() && shouldConnect(world, x + 1, y - 1, z, false);
                boolean zm = shouldConnect(world, x, y, z - 1, true) || !world.getBlock(x, y, z - 1).isSolid() && shouldConnect(world, x, y - 1, z - 1, false);
                boolean zp = shouldConnect(world, x, y, z + 1, true) || !world.getBlock(x, y, z + 1).isSolid() && shouldConnect(world, x, y - 1, z + 1, false);

                if (!world.getBlock(x, y + 1, z).isSolid())
                {
                    xm |= world.getBlock(x - 1, y, z).isSolid() && shouldConnect(world, x - 1, y + 1, z, false);
                    xp |= world.getBlock(x + 1, y, z).isSolid() && shouldConnect(world, x + 1, y + 1, z, false);
                    zm |= world.getBlock(x, y, z - 1).isSolid() && shouldConnect(world, x, y + 1, z - 1, false);
                    zp |= world.getBlock(x, y, z + 1).isSolid() && shouldConnect(world, x, y + 1, z + 1, false);
                }

                return !zm && !xp && !xm && !zp && side >= 2 && side <= 5 ? meta : (side == 2 && zm && !xm && !xp ? meta : (side == 3 && zp && !xm && !xp ? meta : (side == 4 && xm && !zm && !zp ? meta : (side == 5 && xp && !zm && !zp ? meta : 0))));
            }
        }
    }

    private static boolean shouldConnect(WorldServer world, int x, int y, int z, boolean flag)
    {
        Block block = world.getBlock(x, y, z);
        return block == Block.redstone_wire || (block.canProvidePower() && flag);
    }

    /**
     * Can this block provide power. Only wire currently seems to have this change based on its state.
     */
    public boolean canProvidePower()
    {
        return !this.isCheckingForPower;
    }

	public boolean isReplaceable()
	{
		return false;
	}

	public int onBlockPlaced(WorldServer world, int x, int y, int z, int side)
	{
		return 0;
	}

	public boolean onBlockActivatedServer(WorldServer p_149727_1_, int p_149727_2_, int p_149727_3_, int p_149727_4_)
	{
		return false;
	}

	public void updateTick(WorldServer p_149674_1_, int p_149674_2_, int p_149674_3_, int p_149674_4_){}
}
