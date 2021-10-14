package net.minecraft.block;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class Block
{
	private static final Block air = new Block();
    private static final Block stone = new Block();
    private static final Block redstone_wire = new BlockRedstoneWire();
    private static final Block lever = new BlockLever();
    private static final Block redstone_torch = new BlockRedstoneTorch();
    
    private static final Map<Integer, Block> registry = new HashMap<Integer, Block>();
    
    static
    {
    	registry.put(0, air);
    	registry.put(1, stone);
    	registry.put(2, redstone_wire);
    	registry.put(3, lever);
    	registry.put(4, redstone_torch);
    }

    public static Block getBlockById(int id)
    {
    	return registry.get(id);
    }
    
    protected final float d = 1F/8F;
    
    protected double minX(int meta)
    {
    	return 0;
    }
    
    protected double minY(int meta)
    {
    	return 0;
    }
    
    protected double minZ(int meta)
    {
    	return 0;
    }
    
    protected double maxX(int meta)
    {
    	return 1;
    }
    
    protected double maxY(int meta)
    {
    	return 1;
    }
    
    protected double maxZ(int meta)
    {
    	return 1;
    }
    
    public final AxisAlignedBB generateCubicBoundingBox(int meta)
    {
        return new AxisAlignedBB(this.minX(meta), this.minY(meta), this.minZ(meta), this.maxX(meta), this.maxY(meta), this.maxZ(meta));
    }

    public int isProvidingWeakPower(World p_149709_1_, int p_149709_2_, int p_149709_3_, int p_149709_4_, int p_149709_5_)
	{
		return 0;
	}

	public int isProvidingStrongPower(World p_149748_1_, int p_149748_2_, int p_149748_3_, int p_149748_4_, int p_149748_5_)
	{
		return 0;
	}

	public void updateTick(World p_149674_1_, int p_149674_2_, int p_149674_3_, int p_149674_4_){}

	public void onNeighborBlockChange(World p_149695_1_, int p_149695_2_, int p_149695_3_, int p_149695_4_){}

	public void onBlockAdded(World p_149726_1_, int p_149726_2_, int p_149726_3_, int p_149726_4_){}

	public void onBlockBreak(World world, int x, int y, int z, int meta){}
}
