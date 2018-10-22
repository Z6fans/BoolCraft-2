package net.minecraft.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class BlockRedstoneTorch extends Block
{
    private boolean isLit;
    private static Map field_150112_b = new HashMap();
    private static final String __OBFID = "CL_00000298";

    private boolean func_150111_a(World p_150111_1_, int p_150111_2_, int p_150111_3_, int p_150111_4_, boolean p_150111_5_)
    {
        if (!field_150112_b.containsKey(p_150111_1_))
        {
            field_150112_b.put(p_150111_1_, new ArrayList());
        }

        List var6 = (List)field_150112_b.get(p_150111_1_);

        if (p_150111_5_)
        {
            var6.add(new BlockRedstoneTorch.Toggle(p_150111_2_, p_150111_3_, p_150111_4_, p_150111_1_.getTotalWorldTime()));
        }

        int var7 = 0;

        for (int var8 = 0; var8 < var6.size(); ++var8)
        {
            BlockRedstoneTorch.Toggle var9 = (BlockRedstoneTorch.Toggle)var6.get(var8);

            if (var9.field_150847_a == p_150111_2_ && var9.field_150845_b == p_150111_3_ && var9.field_150846_c == p_150111_4_)
            {
                ++var7;

                if (var7 >= 8)
                {
                    return true;
                }
            }
        }

        return false;
    }

    public BlockRedstoneTorch(boolean lit)
    {
        this.isLit = lit;
    }

    public void onBlockAdded(WorldServer world, int x, int y, int z)
    {
        if (world.getBlockMetadata(x, y, z) == 0)
        {
        	if (world.getBlockMetadata(x, y, z) == 0)
            {
                if (world.isBlockNormalCubeDefault(x - 1, y, z, true))
                {
                    world.setBlockMetadataWithNotify(x, y, z, 1, false);
                }
                else if (world.isBlockNormalCubeDefault(x + 1, y, z, true))
                {
                    world.setBlockMetadataWithNotify(x, y, z, 2, false);
                }
                else if (world.isBlockNormalCubeDefault(x, y, z - 1, true))
                {
                    world.setBlockMetadataWithNotify(x, y, z, 3, false);
                }
                else if (world.isBlockNormalCubeDefault(x, y, z + 1, true))
                {
                    world.setBlockMetadataWithNotify(x, y, z, 4, false);
                }
                else if (World.doesBlockHaveSolidTopSurface(world, x, y - 1, z))
                {
                    world.setBlockMetadataWithNotify(x, y, z, 5, false);
                }
            }

            this.func_150109_e(world, x, y, z);
        }

        if (this.isLit)
        {
            world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
            world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
            world.notifyBlocksOfNeighborChange(x - 1, y, z, this);
            world.notifyBlocksOfNeighborChange(x + 1, y, z, this);
            world.notifyBlocksOfNeighborChange(x, y, z - 1, this);
            world.notifyBlocksOfNeighborChange(x, y, z + 1, this);
        }
    }

    public void breakBlock(WorldServer p_149749_1_, int p_149749_2_, int p_149749_3_, int p_149749_4_, Block p_149749_5_, int p_149749_6_)
    {
        if (this.isLit)
        {
            p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_, p_149749_3_ - 1, p_149749_4_, this);
            p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_, p_149749_3_ + 1, p_149749_4_, this);
            p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_ - 1, p_149749_3_, p_149749_4_, this);
            p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_ + 1, p_149749_3_, p_149749_4_, this);
            p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_, p_149749_3_, p_149749_4_ - 1, this);
            p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_, p_149749_3_, p_149749_4_ + 1, this);
        }
    }

    public int isProvidingWeakPower(World p_149709_1_, int p_149709_2_, int p_149709_3_, int p_149709_4_, int p_149709_5_)
    {
        if (!this.isLit)
        {
            return 0;
        }
        else
        {
            int var6 = p_149709_1_.getBlockMetadata(p_149709_2_, p_149709_3_, p_149709_4_);
            return var6 == 5 && p_149709_5_ == 1 ? 0 : (var6 == 3 && p_149709_5_ == 3 ? 0 : (var6 == 4 && p_149709_5_ == 2 ? 0 : (var6 == 1 && p_149709_5_ == 5 ? 0 : (var6 == 2 && p_149709_5_ == 4 ? 0 : 15))));
        }
    }

    private boolean func_150110_m(WorldServer p_150110_1_, int p_150110_2_, int p_150110_3_, int p_150110_4_)
    {
        int var5 = p_150110_1_.getBlockMetadata(p_150110_2_, p_150110_3_, p_150110_4_);
        return var5 == 5 && p_150110_1_.getIndirectPowerOutput(p_150110_2_, p_150110_3_ - 1, p_150110_4_, 0) ? true : (var5 == 3 && p_150110_1_.getIndirectPowerOutput(p_150110_2_, p_150110_3_, p_150110_4_ - 1, 2) ? true : (var5 == 4 && p_150110_1_.getIndirectPowerOutput(p_150110_2_, p_150110_3_, p_150110_4_ + 1, 3) ? true : (var5 == 1 && p_150110_1_.getIndirectPowerOutput(p_150110_2_ - 1, p_150110_3_, p_150110_4_, 4) ? true : var5 == 2 && p_150110_1_.getIndirectPowerOutput(p_150110_2_ + 1, p_150110_3_, p_150110_4_, 5))));
    }

    /**
     * Ticks the block if it's been scheduled
     */
    public void updateTick(WorldServer world, int x, int y, int z)
    {
        boolean var6 = this.func_150110_m(world, x, y, z);
        List var7 = (List)field_150112_b.get(world);

        while (var7 != null && !var7.isEmpty() && world.getTotalWorldTime() - ((BlockRedstoneTorch.Toggle)var7.get(0)).field_150844_d > 60L)
        {
            var7.remove(0);
        }

        if (this.isLit)
        {
            if (var6)
            {
                world.setBlock(x, y, z, Block.unlit_redstone_torch, world.getBlockMetadata(x, y, z));
            }
        }
        else if (!var6 && !this.func_150111_a(world, x, y, z, false))
        {
            world.setBlock(x, y, z, Block.redstone_torch, world.getBlockMetadata(x, y, z));
        }
    }

    public void onNeighborBlockChange(WorldServer p_149695_1_, int p_149695_2_, int p_149695_3_, int p_149695_4_, Block p_149695_5_)
    {
        if (!this.func_150108_b(p_149695_1_, p_149695_2_, p_149695_3_, p_149695_4_, p_149695_5_))
        {
            boolean var6 = this.func_150110_m(p_149695_1_, p_149695_2_, p_149695_3_, p_149695_4_);

            if (this.isLit && var6 || !this.isLit && !var6)
            {
                p_149695_1_.scheduleBlockUpdate(p_149695_2_, p_149695_3_, p_149695_4_, this, 2);
            }
        }
    }

    public int isProvidingStrongPower(World p_149748_1_, int p_149748_2_, int p_149748_3_, int p_149748_4_, int p_149748_5_)
    {
        return p_149748_5_ == 0 ? this.isProvidingWeakPower(p_149748_1_, p_149748_2_, p_149748_3_, p_149748_4_, p_149748_5_) : 0;
    }

    /**
     * Can this block provide power. Only wire currently seems to have this change based on its state.
     */
    public boolean canProvidePower()
    {
        return true;
    }

    public boolean blockEquals(Block p_149667_1_)
    {
        return p_149667_1_ == Block.unlit_redstone_torch || p_149667_1_ == Block.redstone_torch;
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

    protected boolean canPlaceBlockAt(World p_149742_1_, int p_149742_2_, int p_149742_3_, int p_149742_4_)
    {
        return p_149742_1_.isBlockNormalCubeDefault(p_149742_2_ - 1, p_149742_3_, p_149742_4_, true) ? true : (p_149742_1_.isBlockNormalCubeDefault(p_149742_2_ + 1, p_149742_3_, p_149742_4_, true) ? true : (p_149742_1_.isBlockNormalCubeDefault(p_149742_2_, p_149742_3_, p_149742_4_ - 1, true) ? true : (p_149742_1_.isBlockNormalCubeDefault(p_149742_2_, p_149742_3_, p_149742_4_ + 1, true) ? true : World.doesBlockHaveSolidTopSurface(p_149742_1_, p_149742_2_, p_149742_3_ - 1, p_149742_4_))));
    }

    public int onBlockPlaced(World p_149660_1_, int p_149660_2_, int p_149660_3_, int p_149660_4_, int p_149660_5_)
    {
        int var10 = 0;

        if (p_149660_5_ == 1 && World.doesBlockHaveSolidTopSurface(p_149660_1_, p_149660_2_, p_149660_3_ - 1, p_149660_4_))
        {
            var10 = 5;
        }

        if (p_149660_5_ == 2 && p_149660_1_.isBlockNormalCubeDefault(p_149660_2_, p_149660_3_, p_149660_4_ + 1, true))
        {
            var10 = 4;
        }

        if (p_149660_5_ == 3 && p_149660_1_.isBlockNormalCubeDefault(p_149660_2_, p_149660_3_, p_149660_4_ - 1, true))
        {
            var10 = 3;
        }

        if (p_149660_5_ == 4 && p_149660_1_.isBlockNormalCubeDefault(p_149660_2_ + 1, p_149660_3_, p_149660_4_, true))
        {
            var10 = 2;
        }

        if (p_149660_5_ == 5 && p_149660_1_.isBlockNormalCubeDefault(p_149660_2_ - 1, p_149660_3_, p_149660_4_, true))
        {
            var10 = 1;
        }

        return var10;
    }

    private boolean func_150108_b(WorldServer p_150108_1_, int p_150108_2_, int p_150108_3_, int p_150108_4_, Block p_150108_5_)
    {
        if (this.func_150109_e(p_150108_1_, p_150108_2_, p_150108_3_, p_150108_4_))
        {
            int var6 = p_150108_1_.getBlockMetadata(p_150108_2_, p_150108_3_, p_150108_4_);
            boolean var7 = false;

            if (!p_150108_1_.isBlockNormalCubeDefault(p_150108_2_ - 1, p_150108_3_, p_150108_4_, true) && var6 == 1)
            {
                var7 = true;
            }

            if (!p_150108_1_.isBlockNormalCubeDefault(p_150108_2_ + 1, p_150108_3_, p_150108_4_, true) && var6 == 2)
            {
                var7 = true;
            }

            if (!p_150108_1_.isBlockNormalCubeDefault(p_150108_2_, p_150108_3_, p_150108_4_ - 1, true) && var6 == 3)
            {
                var7 = true;
            }

            if (!p_150108_1_.isBlockNormalCubeDefault(p_150108_2_, p_150108_3_, p_150108_4_ + 1, true) && var6 == 4)
            {
                var7 = true;
            }

            if (!World.doesBlockHaveSolidTopSurface(p_150108_1_, p_150108_2_, p_150108_3_ - 1, p_150108_4_) && var6 == 5)
            {
                var7 = true;
            }

            if (var7)
            {
                p_150108_1_.setBlockToAir(p_150108_2_, p_150108_3_, p_150108_4_);
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return true;
        }
    }

    private boolean func_150109_e(World p_150109_1_, int p_150109_2_, int p_150109_3_, int p_150109_4_)
    {
        if (!this.canPlaceBlockAt(p_150109_1_, p_150109_2_, p_150109_3_, p_150109_4_))
        {
            if (p_150109_1_.getBlock(p_150109_2_, p_150109_3_, p_150109_4_) == this)
            {
                p_150109_1_.setBlockToAir(p_150109_2_, p_150109_3_, p_150109_4_);
            }

            return false;
        }
        else
        {
            return true;
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
    public int colorMultiplier(ChunkCache p_149720_1_, int p_149720_2_, int p_149720_3_, int p_149720_4_)
    {
        return isLit ? 0xE91A64 : 0x5B0A27;
    }

    private static class Toggle
    {
        private int field_150847_a;
        private int field_150845_b;
        private int field_150846_c;
        private long field_150844_d;
        private static final String __OBFID = "CL_00000299";

        private Toggle(int p_i45422_1_, int p_i45422_2_, int p_i45422_3_, long p_i45422_4_)
        {
            this.field_150847_a = p_i45422_1_;
            this.field_150845_b = p_i45422_2_;
            this.field_150846_c = p_i45422_3_;
            this.field_150844_d = p_i45422_4_;
        }
    }

	public boolean isReplaceable()
	{
		return false;
	}

	public boolean onBlockActivatedClient(WorldClient p_149727_1_, int p_149727_2_, int p_149727_3_, int p_149727_4_)
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
