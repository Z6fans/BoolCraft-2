package net.minecraft.block;

import net.minecraft.client.WorldClient;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;

public class BlockAir extends Block
{
    
    public BlockAir()
    {
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }
    
    public boolean isReplaceable()
    {
    	return true;
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
        return -1;
    }

    /**
     * Returns a bounding box from the pool of bounding boxes (this means this box can change after the pool has been
     * cleared to be reused)
     */
    public AxisAlignedBB getCollisionBoundingBoxFromPool(int p_149668_2_, int p_149668_3_, int p_149668_4_)
    {
        return null;
    }

	public int colorMultiplier(WorldClient cc, int x, int y, int z)
	{
		return 0;
	}

	public int isProvidingWeakPower(WorldServer p_149709_1_, int p_149709_2_, int p_149709_3_, int p_149709_4_, int p_149709_5_)
	{
		return 0;
	}

	public int isProvidingStrongPower(WorldServer p_149748_1_, int p_149748_2_, int p_149748_3_, int p_149748_4_, int p_149748_5_)
	{
		return 0;
	}

	public boolean canProvidePower()
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

	public void onNeighborBlockChange(WorldServer p_149695_1_, int p_149695_2_, int p_149695_3_, int p_149695_4_, Block p_149695_5_){}

	public void onBlockAdded(WorldServer p_149726_1_, int p_149726_2_, int p_149726_3_, int p_149726_4_){}

	public void breakBlock(WorldServer p_149749_1_, int p_149749_2_, int p_149749_3_, int p_149749_4_, Block p_149749_5_, int p_149749_6_){}

	public boolean getTickRandomly()
	{
		return false;
	}
}