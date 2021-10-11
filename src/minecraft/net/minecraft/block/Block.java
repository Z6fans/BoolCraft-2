package net.minecraft.block;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
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

    public final MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 playerPos, Vec3 playerLook)
    {
    	AxisAlignedBB aabb = this.generateCubicBoundingBox(world.getBlockMetadata(x, y, z));
        playerPos = playerPos.addVector(-x, -y, -z);
        playerLook = playerLook.addVector(-x, -y, -z);
        Vec3 nx = playerPos.getIntermediateWithXValue(playerLook, aabb.minX);
        Vec3 xx = playerPos.getIntermediateWithXValue(playerLook, aabb.maxX);
        Vec3 ny = playerPos.getIntermediateWithYValue(playerLook, aabb.minY);
        Vec3 xy = playerPos.getIntermediateWithYValue(playerLook, aabb.maxY);
        Vec3 nz = playerPos.getIntermediateWithZValue(playerLook, aabb.minZ);
        Vec3 xz = playerPos.getIntermediateWithZValue(playerLook, aabb.maxZ);

        Vec3 closest = null;
        byte side = -1;

        if (nx != null && aabb.contains(nx) && (closest == null || playerPos.quadranceTo(nx) < playerPos.quadranceTo(closest)))
        {
            closest = nx;
            side = 4;
        }

        if (xx != null && aabb.contains(xx) && (closest == null || playerPos.quadranceTo(xx) < playerPos.quadranceTo(closest)))
        {
            closest = xx;
            side = 5;
        }

        if (ny != null && aabb.contains(ny) && (closest == null || playerPos.quadranceTo(ny) < playerPos.quadranceTo(closest)))
        {
            closest = ny;
            side = 0;
        }

        if (xy != null && aabb.contains(xy) && (closest == null || playerPos.quadranceTo(xy) < playerPos.quadranceTo(closest)))
        {
            closest = xy;
            side = 1;
        }

        if (nz != null && aabb.contains(nz) && (closest == null || playerPos.quadranceTo(nz) < playerPos.quadranceTo(closest)))
        {
            closest = nz;
            side = 2;
        }

        if (xz != null && aabb.contains(xz) && (closest == null || playerPos.quadranceTo(xz) < playerPos.quadranceTo(closest)))
        {
            closest = xz;
            side = 3;
        }

        return side == -1 ? null : new MovingObjectPosition(x, y, z, side);
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
