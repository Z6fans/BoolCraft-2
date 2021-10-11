package net.minecraft.block;

import net.minecraft.world.World;

public class BlockRedstoneWire extends Block
{
    private boolean isCheckingForPower = false;
    
    protected double maxY(int meta)
    {
    	return d/2f;
    }

    private void computeMetadata(World world, int x, int y, int z)
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

            if (world.isSolid(xToCheck, y, zToCheck) && !world.isSolid(x, y + 1, z))
            {
                if (xToCheck != x || zToCheck != z)
                {
                    directPower = this.maxRedstonePowerAt(world, xToCheck, y + 1, zToCheck, directPower);
                }
            }
            else if (!world.isSolid(xToCheck, y, zToCheck) && (xToCheck != x || zToCheck != z))
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
            world.setBlockAndMeta(x, y, z, 2, currentPower);
            this.notifyNeighbors(world, x, y, z);
        }
    }

    private int maxRedstonePowerAt(World world, int x, int y, int z, int other)
    {
        if (world.isWire(x, y, z))
        {
            return Math.max(world.getBlockMetadata(x, y, z), other);
        }
        else
        {
            return other;
        }
    }
    
    private void notifyNeighbors(World world, int x, int y, int z)
    {
    	world.notifyBlocksOfNeighborChange(x, y, z);
        world.notifyBlocksOfNeighborChange(x - 1, y, z);
        world.notifyBlocksOfNeighborChange(x + 1, y, z);
        world.notifyBlocksOfNeighborChange(x, y, z - 1);
        world.notifyBlocksOfNeighborChange(x, y, z + 1);
        world.notifyBlocksOfNeighborChange(x, y - 1, z);
        world.notifyBlocksOfNeighborChange(x, y + 1, z);
    }

    public void onBlockAdded(World world, int x, int y, int z)
    {
    	this.computeMetadata(world, x, y, z);
        world.notifyBlocksOfNeighborChange(x, y + 1, z);
        world.notifyBlocksOfNeighborChange(x, y - 1, z);
        this.notifyNeighbors(world, x - 1, y, z);
        this.notifyNeighbors(world, x + 1, y, z);
        this.notifyNeighbors(world, x, y, z - 1);
        this.notifyNeighbors(world, x, y, z + 1);

        if (world.isSolid(x - 1, y, z))
        {
            this.notifyNeighbors(world, x - 1, y + 1, z);
        }
        else
        {
            this.notifyNeighbors(world, x - 1, y - 1, z);
        }

        if (world.isSolid(x + 1, y, z))
        {
            this.notifyNeighbors(world, x + 1, y + 1, z);
        }
        else
        {
            this.notifyNeighbors(world, x + 1, y - 1, z);
        }

        if (world.isSolid(x, y, z - 1))
        {
            this.notifyNeighbors(world, x, y + 1, z - 1);
        }
        else
        {
            this.notifyNeighbors(world, x, y - 1, z - 1);
        }

        if (world.isSolid(x, y, z + 1))
        {
            this.notifyNeighbors(world, x, y + 1, z + 1);
        }
        else
        {
            this.notifyNeighbors(world, x, y - 1, z + 1);
        }
    }

    public void onBlockBreak(World world, int x, int y, int z, int meta)
    {
    	world.notifyBlocksOfNeighborChange(x, y + 1, z);
        world.notifyBlocksOfNeighborChange(x, y - 1, z);
        world.notifyBlocksOfNeighborChange(x + 1, y, z);
        world.notifyBlocksOfNeighborChange(x - 1, y, z);
        world.notifyBlocksOfNeighborChange(x, y, z + 1);
        world.notifyBlocksOfNeighborChange(x, y, z - 1);
    }

    public void onNeighborBlockChange(World world, int x, int y, int z)
    {
    	if (world.isSolid(x, y - 1, z))
        {
            this.computeMetadata(world, x, y, z);
        }
        else
        {
            world.setBlockAndMeta(x, y, z, 0, 0);
        }
    }

    public int isProvidingStrongPower(World world, int x, int y, int z, int side)
    {
        return this.isProvidingWeakPower(world, x, y, z, side);
    }

    public int isProvidingWeakPower(World world, int x, int y, int z, int side)
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
                boolean xm = world.canProvidePower(x - 1, y, z) || !world.isSolid(x - 1, y, z) && world.isWire(x - 1, y - 1, z);
                boolean xp = world.canProvidePower(x + 1, y, z) || !world.isSolid(x + 1, y, z) && world.isWire(x + 1, y - 1, z);
                boolean zm = world.canProvidePower(x, y, z - 1) || !world.isSolid(x, y, z - 1) && world.isWire(x, y - 1, z - 1);
                boolean zp = world.canProvidePower(x, y, z + 1) || !world.isSolid(x, y, z + 1) && world.isWire(x, y - 1, z + 1);

                if (!world.isSolid(x, y + 1, z))
                {
                    xm |= world.isSolid(x - 1, y, z) && world.isWire(x - 1, y + 1, z);
                    xp |= world.isSolid(x + 1, y, z) && world.isWire(x + 1, y + 1, z);
                    zm |= world.isSolid(x, y, z - 1) && world.isWire(x, y + 1, z - 1);
                    zp |= world.isSolid(x, y, z + 1) && world.isWire(x, y + 1, z + 1);
                }

                return !zm && !xp && !xm && !zp && side >= 2 && side <= 5 ? meta : (side == 2 && zm && !xm && !xp ? meta : (side == 3 && zp && !xm && !xp ? meta : (side == 4 && xm && !zm && !zp ? meta : (side == 5 && xp && !zm && !zp ? meta : 0))));
            }
        }
    }

	public void updateTick(World p_149674_1_, int p_149674_2_, int p_149674_3_, int p_149674_4_){}
}
