package net.minecraft.block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.WorldServer;

public class BlockRedstoneWire extends Block
{
    private boolean isCheckingForPower = false;
    private Set<ChunkPosition> field_150179_b = new HashSet<ChunkPosition>();

    public BlockRedstoneWire()
    {
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F);
    }

    /**
     * Returns a bounding box from the pool of bounding boxes (this means this box can change after the pool has been
     * cleared to be reused)
     */
    public AxisAlignedBB getCollisionBoundingBoxFromPool(int p_149668_2_, int p_149668_3_, int p_149668_4_)
    {
        return null;
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
    public int colorMultiplier(ChunkCache p_149720_1_, int p_149720_2_, int p_149720_3_, int p_149720_4_)
    {
        int var6 = p_149720_1_.getBlockMetadata(p_149720_2_, p_149720_3_, p_149720_4_);
        float var11 = (float)var6 / 15.0F;
        float var12 = var11 * 0.6F + 0.4F;

        if (var6 == 0)
        {
            var12 = 0.3F;
        }
        return (int)(0x39 * var12) << 16 | (int)(0xEE * var12) << 8 | (int)(0xEE * var12);
    }

    protected boolean canPlaceBlockAt(WorldServer world, int x, int y, int z)
    {
        return world.getBlock(x, y - 1, z).isSolid();
    }

    private void func_150177_e(WorldServer world, int x, int y, int z)
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
            this.field_150179_b.add(new ChunkPosition(x, y, z));
            this.field_150179_b.add(new ChunkPosition(x - 1, y, z));
            this.field_150179_b.add(new ChunkPosition(x + 1, y, z));
            this.field_150179_b.add(new ChunkPosition(x, y - 1, z));
            this.field_150179_b.add(new ChunkPosition(x, y + 1, z));
            this.field_150179_b.add(new ChunkPosition(x, y, z - 1));
            this.field_150179_b.add(new ChunkPosition(x, y, z + 1));
        }
        ArrayList<ChunkPosition> var5 = new ArrayList<ChunkPosition>(this.field_150179_b);
        this.field_150179_b.clear();

        for (int var6 = 0; var6 < var5.size(); ++var6)
        {
            ChunkPosition var7 = (ChunkPosition)var5.get(var6);
            world.notifyBlocksOfNeighborChange(var7.xCoord, var7.yCoord, var7.zCoord, this);
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

    private void func_150172_m(WorldServer world, int x, int y, int z)
    {
        if (world.getBlock(x, y, z) == this)
        {
            world.notifyBlocksOfNeighborChange(x, y, z, this);
            world.notifyBlocksOfNeighborChange(x - 1, y, z, this);
            world.notifyBlocksOfNeighborChange(x + 1, y, z, this);
            world.notifyBlocksOfNeighborChange(x, y, z - 1, this);
            world.notifyBlocksOfNeighborChange(x, y, z + 1, this);
            world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
            world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
        }
    }

    public void onBlockAdded(WorldServer p_149726_1_, int p_149726_2_, int p_149726_3_, int p_149726_4_)
    {
    	this.func_150177_e(p_149726_1_, p_149726_2_, p_149726_3_, p_149726_4_);
        p_149726_1_.notifyBlocksOfNeighborChange(p_149726_2_, p_149726_3_ + 1, p_149726_4_, this);
        p_149726_1_.notifyBlocksOfNeighborChange(p_149726_2_, p_149726_3_ - 1, p_149726_4_, this);
        this.func_150172_m(p_149726_1_, p_149726_2_ - 1, p_149726_3_, p_149726_4_);
        this.func_150172_m(p_149726_1_, p_149726_2_ + 1, p_149726_3_, p_149726_4_);
        this.func_150172_m(p_149726_1_, p_149726_2_, p_149726_3_, p_149726_4_ - 1);
        this.func_150172_m(p_149726_1_, p_149726_2_, p_149726_3_, p_149726_4_ + 1);

        if (p_149726_1_.getBlock(p_149726_2_ - 1, p_149726_3_, p_149726_4_).isSolid())
        {
            this.func_150172_m(p_149726_1_, p_149726_2_ - 1, p_149726_3_ + 1, p_149726_4_);
        }
        else
        {
            this.func_150172_m(p_149726_1_, p_149726_2_ - 1, p_149726_3_ - 1, p_149726_4_);
        }

        if (p_149726_1_.getBlock(p_149726_2_ + 1, p_149726_3_, p_149726_4_).isSolid())
        {
            this.func_150172_m(p_149726_1_, p_149726_2_ + 1, p_149726_3_ + 1, p_149726_4_);
        }
        else
        {
            this.func_150172_m(p_149726_1_, p_149726_2_ + 1, p_149726_3_ - 1, p_149726_4_);
        }

        if (p_149726_1_.getBlock(p_149726_2_, p_149726_3_, p_149726_4_ - 1).isSolid())
        {
            this.func_150172_m(p_149726_1_, p_149726_2_, p_149726_3_ + 1, p_149726_4_ - 1);
        }
        else
        {
            this.func_150172_m(p_149726_1_, p_149726_2_, p_149726_3_ - 1, p_149726_4_ - 1);
        }

        if (p_149726_1_.getBlock(p_149726_2_, p_149726_3_, p_149726_4_ + 1).isSolid())
        {
            this.func_150172_m(p_149726_1_, p_149726_2_, p_149726_3_ + 1, p_149726_4_ + 1);
        }
        else
        {
            this.func_150172_m(p_149726_1_, p_149726_2_, p_149726_3_ - 1, p_149726_4_ + 1);
        }
    }

    public void breakBlock(WorldServer p_149749_1_, int p_149749_2_, int p_149749_3_, int p_149749_4_, Block p_149749_5_, int p_149749_6_)
    {
    	p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_, p_149749_3_ + 1, p_149749_4_, this);
        p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_, p_149749_3_ - 1, p_149749_4_, this);
        p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_ + 1, p_149749_3_, p_149749_4_, this);
        p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_ - 1, p_149749_3_, p_149749_4_, this);
        p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_, p_149749_3_, p_149749_4_ + 1, this);
        p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_, p_149749_3_, p_149749_4_ - 1, this);
        this.func_150177_e(p_149749_1_, p_149749_2_, p_149749_3_, p_149749_4_);
        this.func_150172_m(p_149749_1_, p_149749_2_ - 1, p_149749_3_, p_149749_4_);
        this.func_150172_m(p_149749_1_, p_149749_2_ + 1, p_149749_3_, p_149749_4_);
        this.func_150172_m(p_149749_1_, p_149749_2_, p_149749_3_, p_149749_4_ - 1);
        this.func_150172_m(p_149749_1_, p_149749_2_, p_149749_3_, p_149749_4_ + 1);

        if (p_149749_1_.getBlock(p_149749_2_ - 1, p_149749_3_, p_149749_4_).isSolid())
        {
            this.func_150172_m(p_149749_1_, p_149749_2_ - 1, p_149749_3_ + 1, p_149749_4_);
        }
        else
        {
            this.func_150172_m(p_149749_1_, p_149749_2_ - 1, p_149749_3_ - 1, p_149749_4_);
        }

        if (p_149749_1_.getBlock(p_149749_2_ + 1, p_149749_3_, p_149749_4_).isSolid())
        {
            this.func_150172_m(p_149749_1_, p_149749_2_ + 1, p_149749_3_ + 1, p_149749_4_);
        }
        else
        {
            this.func_150172_m(p_149749_1_, p_149749_2_ + 1, p_149749_3_ - 1, p_149749_4_);
        }

        if (p_149749_1_.getBlock(p_149749_2_, p_149749_3_, p_149749_4_ - 1).isSolid())
        {
            this.func_150172_m(p_149749_1_, p_149749_2_, p_149749_3_ + 1, p_149749_4_ - 1);
        }
        else
        {
            this.func_150172_m(p_149749_1_, p_149749_2_, p_149749_3_ - 1, p_149749_4_ - 1);
        }

        if (p_149749_1_.getBlock(p_149749_2_, p_149749_3_, p_149749_4_ + 1).isSolid())
        {
            this.func_150172_m(p_149749_1_, p_149749_2_, p_149749_3_ + 1, p_149749_4_ + 1);
        }
        else
        {
            this.func_150172_m(p_149749_1_, p_149749_2_, p_149749_3_ - 1, p_149749_4_ + 1);
        }
    }

    public void onNeighborBlockChange(WorldServer world, int x, int y, int z, Block block)
    {
    	if (this.canPlaceBlockAt(world, x, y, z))
        {
            this.func_150177_e(world, x, y, z);
        }
        else
        {
            world.setBlockToAir(x, y, z);
        }
    }

    public int isProvidingStrongPower(WorldServer p_149748_1_, int p_149748_2_, int p_149748_3_, int p_149748_4_, int p_149748_5_)
    {
        return this.isCheckingForPower ? 0 : this.isProvidingWeakPower(p_149748_1_, p_149748_2_, p_149748_3_, p_149748_4_, p_149748_5_);
    }

    public int isProvidingWeakPower(WorldServer world, int x, int y, int z, int side)
    {
        if (this.isCheckingForPower)
        {
            return 0;
        }
        else
        {
            int var6 = world.getBlockMetadata(x, y, z);

            if (var6 == 0)
            {
                return 0;
            }
            else if (side == 1)
            {
                return var6;
            }
            else
            {
                boolean var7 = shouldConnect(world, x - 1, y, z, true) || !world.getBlock(x - 1, y, z).isSolid() && shouldConnect(world, x - 1, y - 1, z, false);
                boolean var8 = shouldConnect(world, x + 1, y, z, true) || !world.getBlock(x + 1, y, z).isSolid() && shouldConnect(world, x + 1, y - 1, z, false);
                boolean var9 = shouldConnect(world, x, y, z - 1, true) || !world.getBlock(x, y, z - 1).isSolid() && shouldConnect(world, x, y - 1, z - 1, false);
                boolean var10 = shouldConnect(world, x, y, z + 1, true) || !world.getBlock(x, y, z + 1).isSolid() && shouldConnect(world, x, y - 1, z + 1, false);

                if (!world.getBlock(x, y + 1, z).isSolid())
                {
                    if (world.getBlock(x - 1, y, z).isSolid() && shouldConnect(world, x - 1, y + 1, z, false))
                    {
                        var7 = true;
                    }

                    if (world.getBlock(x + 1, y, z).isSolid() && shouldConnect(world, x + 1, y + 1, z, false))
                    {
                        var8 = true;
                    }

                    if (world.getBlock(x, y, z - 1).isSolid() && shouldConnect(world, x, y + 1, z - 1, false))
                    {
                        var9 = true;
                    }

                    if (world.getBlock(x, y, z + 1).isSolid() && shouldConnect(world, x, y + 1, z + 1, false))
                    {
                        var10 = true;
                    }
                }

                return !var9 && !var8 && !var7 && !var10 && side >= 2 && side <= 5 ? var6 : (side == 2 && var9 && !var7 && !var8 ? var6 : (side == 3 && var10 && !var7 && !var8 ? var6 : (side == 4 && var7 && !var9 && !var10 ? var6 : (side == 5 && var8 && !var9 && !var10 ? var6 : 0))));
            }
        }
    }

    private static boolean shouldConnect(WorldServer world, int x, int y, int z, boolean flag)
    {
        Block block = world.getBlock(x, y, z);
        return block == Block.redstone_wire || (block.canProvidePower() && flag);
    }
    
    public static boolean shouldConnect(ChunkCache cc, int x, int y, int z, boolean flag)
    {
        Block block = cc.getBlock(x, y, z);
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

	public boolean onBlockActivatedClient(WorldClient p_149727_1_, int p_149727_2_, int p_149727_3_, int p_149727_4_)
	{
		return false;
	}

	public boolean onBlockActivatedServer(WorldServer p_149727_1_, int p_149727_2_, int p_149727_3_, int p_149727_4_)
	{
		return false;
	}

	public void updateTick(WorldServer p_149674_1_, int p_149674_2_, int p_149674_3_, int p_149674_4_){}

	public boolean getTickRandomly()
	{
		return false;
	}
}
