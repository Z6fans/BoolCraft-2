package net.minecraft.block;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class BlockStone extends Block {
	
	public BlockStone()
    {
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

	public boolean isSolid()
	{
		return true;
	}

	public boolean isReplaceable()
	{
		return false;
	}
	
	public AxisAlignedBB getCollisionBoundingBoxFromPool(int x, int y, int z)
    {
        return this.generateCubicBoundingBox(x, y, z);
    }

	public int getRenderType()
	{
		return 0;
	}
	
	public int colorMultiplier(ChunkCache p_149720_1_, int x, int y, int z)
    {
    	int a = (x%2 + 2)%2;
    	int b = (y%2 + 2)%2;
    	int c = (z%2 + 2)%2;
    	return 0x333333 + 0x060000 * a + 0x000300 * b + 0x000006 * c;
    }

	public int isProvidingWeakPower(World p_149709_1_, int p_149709_2_, int p_149709_3_, int p_149709_4_, int p_149709_5_)
	{
		return 0;
	}

	public int isProvidingStrongPower(World p_149748_1_, int p_149748_2_, int p_149748_3_, int p_149748_4_, int p_149748_5_)
	{
		return 0;
	}

	public boolean canProvidePower()
	{
		return false;
	}

	public int onBlockPlaced(World world, int x, int y, int z, int side)
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

	public void onNeighborBlockChange(WorldServer p_149695_1_, int p_149695_2_, int p_149695_3_, int p_149695_4_, Block p_149695_5_){}

	public void onBlockAdded(WorldServer p_149726_1_, int p_149726_2_, int p_149726_3_, int p_149726_4_){}

	public void breakBlock(WorldServer p_149749_1_, int p_149749_2_, int p_149749_3_, int p_149749_4_, Block p_149749_5_, int p_149749_6_){}

	public boolean getTickRandomly()
	{
		return false;
	}
}