package net.minecraft.block;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class BlockLever extends Block
{
    private static final String __OBFID = "CL_00000264";
    
    public boolean isSolid()
    {
        return false;
    }

    /**
     * Returns a bounding box from the pool of bounding boxes (this means this box can change after the pool has been
     * cleared to be reused)
     */
    public AxisAlignedBB getCollisionBoundingBoxFromPool(int p_149668_2_, int p_149668_3_, int p_149668_4_)
    {
        return null;
    }

    /**
     * The type of render function that is called for this block
     */
    public int getRenderType()
    {
        return 12;
    }

    /**
     * checks to see if you can place this block can be placed on that side of a block: BlockLever overrides
     */
    public boolean canPlaceBlockOnSide(World p_149707_1_, int p_149707_2_, int p_149707_3_, int p_149707_4_, int p_149707_5_)
    {
        return p_149707_5_ == 0 && p_149707_1_.getBlock(p_149707_2_, p_149707_3_ + 1, p_149707_4_).isSolid() ? true : (p_149707_5_ == 1 && World.doesBlockHaveSolidTopSurface(p_149707_1_, p_149707_2_, p_149707_3_ - 1, p_149707_4_) ? true : (p_149707_5_ == 2 && p_149707_1_.getBlock(p_149707_2_, p_149707_3_, p_149707_4_ + 1).isSolid() ? true : (p_149707_5_ == 3 && p_149707_1_.getBlock(p_149707_2_, p_149707_3_, p_149707_4_ - 1).isSolid() ? true : (p_149707_5_ == 4 && p_149707_1_.getBlock(p_149707_2_ + 1, p_149707_3_, p_149707_4_).isSolid() ? true : p_149707_5_ == 5 && p_149707_1_.getBlock(p_149707_2_ - 1, p_149707_3_, p_149707_4_).isSolid()))));
    }

    protected boolean canPlaceBlockAt(World p_149742_1_, int p_149742_2_, int p_149742_3_, int p_149742_4_)
    {
        return p_149742_1_.getBlock(p_149742_2_ - 1, p_149742_3_, p_149742_4_).isSolid() ? true : (p_149742_1_.getBlock(p_149742_2_ + 1, p_149742_3_, p_149742_4_).isSolid() ? true : (p_149742_1_.getBlock(p_149742_2_, p_149742_3_, p_149742_4_ - 1).isSolid() ? true : (p_149742_1_.getBlock(p_149742_2_, p_149742_3_, p_149742_4_ + 1).isSolid() ? true : (World.doesBlockHaveSolidTopSurface(p_149742_1_, p_149742_2_, p_149742_3_ - 1, p_149742_4_) ? true : p_149742_1_.getBlock(p_149742_2_, p_149742_3_ + 1, p_149742_4_).isSolid()))));
    }

    public int onBlockPlaced(World world, int x, int y, int z, int side)
    {
        int[] xOff = {0, 0, 0, 0, -1, 1};
    	int[] yOff = {-1, 1, 0, 0, 0, 0};
    	int[] zOff = {0, 0, -1, 1, 0, 0};
        
        if(world.getBlock(x - xOff[side], y - yOff[side], z - zOff[side]).isSolid())
        {
        	return (6 - side) % 6;
        }

        return -1;
    }

    public void onNeighborBlockChange(WorldServer p_149695_1_, int p_149695_2_, int p_149695_3_, int p_149695_4_, Block p_149695_5_)
    {
        if (this.func_149820_e(p_149695_1_, p_149695_2_, p_149695_3_, p_149695_4_))
        {
            int var6 = p_149695_1_.getBlockMetadata(p_149695_2_, p_149695_3_, p_149695_4_) & 7;
            boolean var7 = false;

            if (!p_149695_1_.getBlock(p_149695_2_ - 1, p_149695_3_, p_149695_4_).isSolid() && var6 == 1)
            {
                var7 = true;
            }

            if (!p_149695_1_.getBlock(p_149695_2_ + 1, p_149695_3_, p_149695_4_).isSolid() && var6 == 2)
            {
                var7 = true;
            }

            if (!p_149695_1_.getBlock(p_149695_2_, p_149695_3_, p_149695_4_ - 1).isSolid() && var6 == 3)
            {
                var7 = true;
            }

            if (!p_149695_1_.getBlock(p_149695_2_, p_149695_3_, p_149695_4_ + 1).isSolid() && var6 == 4)
            {
                var7 = true;
            }

            if (!World.doesBlockHaveSolidTopSurface(p_149695_1_, p_149695_2_, p_149695_3_ - 1, p_149695_4_) && var6 == 5)
            {
                var7 = true;
            }

            if (!World.doesBlockHaveSolidTopSurface(p_149695_1_, p_149695_2_, p_149695_3_ - 1, p_149695_4_) && var6 == 6)
            {
                var7 = true;
            }

            if (!p_149695_1_.getBlock(p_149695_2_, p_149695_3_ + 1, p_149695_4_).isSolid() && var6 == 0)
            {
                var7 = true;
            }

            if (!p_149695_1_.getBlock(p_149695_2_, p_149695_3_ + 1, p_149695_4_).isSolid() && var6 == 7)
            {
                var7 = true;
            }

            if (var7)
            {
                p_149695_1_.setBlockToAir(p_149695_2_, p_149695_3_, p_149695_4_);
            }
        }
    }

    private boolean func_149820_e(WorldServer p_149820_1_, int p_149820_2_, int p_149820_3_, int p_149820_4_)
    {
        if (!this.canPlaceBlockAt(p_149820_1_, p_149820_2_, p_149820_3_, p_149820_4_))
        {
            p_149820_1_.setBlockToAir(p_149820_2_, p_149820_3_, p_149820_4_);
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
     * Called upon block activation (right click on the block.)
     */
    public boolean onBlockActivatedClient(WorldClient world, int x, int y, int z)
    {
        return true;
    }
    
    /**
     * Called upon block activation (right click on the block.)
     */
    public boolean onBlockActivatedServer(WorldServer world, int x, int y, int z)
    {
    	int meta = world.getBlockMetadata(x, y, z);
        int orientation = meta & 7;
        int newState = 8 - (meta & 8);
        world.setBlockMetadataWithNotify(x, y, z, orientation + newState, true);
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
        else if (orientation != 5 && orientation != 6)
        {
            if (orientation == 0 || orientation == 7)
            {
                world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
            }
        }
        else
        {
            world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
        }
        
        return true;
    }

    public void breakBlock(WorldServer p_149749_1_, int p_149749_2_, int p_149749_3_, int p_149749_4_, Block p_149749_5_, int p_149749_6_)
    {
        if ((p_149749_6_ & 8) > 0)
        {
            p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_, p_149749_3_, p_149749_4_, this);
            int var7 = p_149749_6_ & 7;

            if (var7 == 1)
            {
                p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_ - 1, p_149749_3_, p_149749_4_, this);
            }
            else if (var7 == 2)
            {
                p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_ + 1, p_149749_3_, p_149749_4_, this);
            }
            else if (var7 == 3)
            {
                p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_, p_149749_3_, p_149749_4_ - 1, this);
            }
            else if (var7 == 4)
            {
                p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_, p_149749_3_, p_149749_4_ + 1, this);
            }
            else if (var7 != 5 && var7 != 6)
            {
                if (var7 == 0 || var7 == 7)
                {
                    p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_, p_149749_3_ + 1, p_149749_4_, this);
                }
            }
            else
            {
                p_149749_1_.notifyBlocksOfNeighborChange(p_149749_2_, p_149749_3_ - 1, p_149749_4_, this);
            }
        }
    }

    public int isProvidingWeakPower(World p_149709_1_, int p_149709_2_, int p_149709_3_, int p_149709_4_, int p_149709_5_)
    {
        return (p_149709_1_.getBlockMetadata(p_149709_2_, p_149709_3_, p_149709_4_) & 8) > 0 ? 15 : 0;
    }

    public int isProvidingStrongPower(World p_149748_1_, int p_149748_2_, int p_149748_3_, int p_149748_4_, int p_149748_5_)
    {
        int var6 = p_149748_1_.getBlockMetadata(p_149748_2_, p_149748_3_, p_149748_4_);

        if ((var6 & 8) == 0)
        {
            return 0;
        }
        else
        {
            int var7 = var6 & 7;
            return var7 == 0 && p_149748_5_ == 0 ? 15 : (var7 == 7 && p_149748_5_ == 0 ? 15 : (var7 == 6 && p_149748_5_ == 1 ? 15 : (var7 == 5 && p_149748_5_ == 1 ? 15 : (var7 == 4 && p_149748_5_ == 2 ? 15 : (var7 == 3 && p_149748_5_ == 3 ? 15 : (var7 == 2 && p_149748_5_ == 4 ? 15 : (var7 == 1 && p_149748_5_ == 5 ? 15 : 0)))))));
        }
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
    public int colorMultiplier(ChunkCache p_149720_1_, int p_149720_2_, int p_149720_3_, int p_149720_4_)
    {
    	int var5 = p_149720_1_.getBlockMetadata(p_149720_2_, p_149720_3_, p_149720_4_);
        return (var5 & 8) > 0 ? 0xEE39E4 : 0x701B6C;
    }

	public boolean isReplaceable()
	{
		return false;
	}

	public void updateTick(WorldServer p_149674_1_, int p_149674_2_, int p_149674_3_, int p_149674_4_){}

	public void onBlockAdded(WorldServer p_149726_1_, int p_149726_2_, int p_149726_3_, int p_149726_4_){}

	public boolean getTickRandomly()
	{
		return false;
	}
}
