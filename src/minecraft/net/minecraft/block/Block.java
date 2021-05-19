package net.minecraft.block;

import com.google.common.collect.HashBiMap;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;

public abstract class Block
{
	public static final Block air = new BlockAir();
    private static final Block stone = new BlockStone();
    private static final Block redstone_wire = new BlockRedstoneWire();
    private static final Block lever = new BlockLever();
    private static final Block redstone_torch = new BlockRedstoneTorch();
    
    private static final HashBiMap<Integer, Block> registry = HashBiMap.create();
    
    static
    {
    	registry.put(0, air);
    	registry.put(1, stone);
    	registry.put(2, redstone_wire);
    	registry.put(3, lever);
    	registry.put(4, redstone_torch);
    }
    
    public static int getIdFromBlock(Block block)
    {
    	return registry.inverse().get(block);
    }

    public static Block getBlockById(int id)
    {
    	return registry.get(id);
    }
    
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
    
    public final AxisAlignedBB generateCubicBoundingBox(int x, int y, int z, int meta)
    {
        return new AxisAlignedBB(x + this.minX(meta), y + this.minY(meta), z + this.minZ(meta), x + this.maxX(meta), y + this.maxY(meta), z + this.maxZ(meta));
    }

    public MovingObjectPosition collisionRayTrace(WorldServer world, int x, int y, int z, Vec3 playerPos, Vec3 playerLook)
    {
    	int meta = world.getBlockMetadata(x, y, z);
        playerPos = playerPos.addVector((double)(-x), (double)(-y), (double)(-z));
        playerLook = playerLook.addVector((double)(-x), (double)(-y), (double)(-z));
        Vec3 var7 = playerPos.getIntermediateWithXValue(playerLook, this.minX(meta));
        Vec3 var8 = playerPos.getIntermediateWithXValue(playerLook, this.maxX(meta));
        Vec3 var9 = playerPos.getIntermediateWithYValue(playerLook, this.minY(meta));
        Vec3 var10 = playerPos.getIntermediateWithYValue(playerLook, this.maxY(meta));
        Vec3 var11 = playerPos.getIntermediateWithZValue(playerLook, this.minZ(meta));
        Vec3 var12 = playerPos.getIntermediateWithZValue(playerLook, this.maxZ(meta));

        if (!this.isVecInsideYZBounds(var7, meta))
        {
            var7 = null;
        }

        if (!this.isVecInsideYZBounds(var8, meta))
        {
            var8 = null;
        }

        if (!this.isVecInsideXZBounds(var9, meta))
        {
            var9 = null;
        }

        if (!this.isVecInsideXZBounds(var10, meta))
        {
            var10 = null;
        }

        if (!this.isVecInsideXYBounds(var11, meta))
        {
            var11 = null;
        }

        if (!this.isVecInsideXYBounds(var12, meta))
        {
            var12 = null;
        }

        Vec3 var13 = null;

        if (var7 != null && (var13 == null || playerPos.quadranceTo(var7) < playerPos.quadranceTo(var13)))
        {
            var13 = var7;
        }

        if (var8 != null && (var13 == null || playerPos.quadranceTo(var8) < playerPos.quadranceTo(var13)))
        {
            var13 = var8;
        }

        if (var9 != null && (var13 == null || playerPos.quadranceTo(var9) < playerPos.quadranceTo(var13)))
        {
            var13 = var9;
        }

        if (var10 != null && (var13 == null || playerPos.quadranceTo(var10) < playerPos.quadranceTo(var13)))
        {
            var13 = var10;
        }

        if (var11 != null && (var13 == null || playerPos.quadranceTo(var11) < playerPos.quadranceTo(var13)))
        {
            var13 = var11;
        }

        if (var12 != null && (var13 == null || playerPos.quadranceTo(var12) < playerPos.quadranceTo(var13)))
        {
            var13 = var12;
        }

        if (var13 == null)
        {
            return null;
        }
        else
        {
            byte side = -1;

            if (var13 == var7)
            {
                side = 4;
            }

            if (var13 == var8)
            {
                side = 5;
            }

            if (var13 == var9)
            {
                side = 0;
            }

            if (var13 == var10)
            {
                side = 1;
            }

            if (var13 == var11)
            {
                side = 2;
            }

            if (var13 == var12)
            {
                side = 3;
            }

            return new MovingObjectPosition(x, y, z, side);
        }
    }

    /**
     * Checks if a vector is within the Y and Z bounds of the block.
     */
    private boolean isVecInsideYZBounds(Vec3 v, int meta)
    {
        return v == null ? false : v.y >= this.minY(meta) && v.y <= this.maxY(meta) && v.z >= this.minZ(meta) && v.z <= this.maxZ(meta);
    }

    /**
     * Checks if a vector is within the X and Z bounds of the block.
     */
    private boolean isVecInsideXZBounds(Vec3 v, int meta)
    {
        return v == null ? false : v.x >= this.minX(meta) && v.x <= this.maxX(meta) && v.z >= this.minZ(meta) && v.z <= this.maxZ(meta);
    }

    /**
     * Checks if a vector is within the X and Y bounds of the block.
     */
    private boolean isVecInsideXYBounds(Vec3 v, int meta)
    {
        return v == null ? false : v.x >= this.minX(meta) && v.x <= this.maxX(meta) && v.y >= this.minY(meta) && v.y <= this.maxY(meta);
    }

    /**
     * checks to see if you can place this block can be placed on that side of a block: BlockLever overrides
     */
    public boolean canPlaceBlockAt(WorldServer p_149742_1_, int p_149742_2_, int p_149742_3_, int p_149742_4_)
    {
    	return p_149742_1_.isReplaceable(p_149742_2_, p_149742_3_, p_149742_4_);
    }

    public abstract boolean isSoled();
    
    public abstract boolean isReplaceobble();

    /**
     * The type of render function that is called for this block
     */
    public abstract int getRenderType();

    /**
     * Ticks the block if it's been scheduled
     */
    public abstract void updateTick(WorldServer p_149674_1_, int p_149674_2_, int p_149674_3_, int p_149674_4_);

    public abstract void onNeighborBlockChange(WorldServer p_149695_1_, int p_149695_2_, int p_149695_3_, int p_149695_4_);

    public abstract void onBlockAdded(WorldServer p_149726_1_, int p_149726_2_, int p_149726_3_, int p_149726_4_);

    public abstract void onBlockBreak(WorldServer world, int x, int y, int z, Block block, int meta);
    
    /**
     * Called upon block activation (right click on the block.)
     */
    public abstract boolean onBlockActivatedServer(WorldServer p_149727_1_, int p_149727_2_, int p_149727_3_, int p_149727_4_);

    /**
     * called when the block is placed, returns meta for new block
     */
    public abstract int onBlockPlaced(WorldServer world, int x, int y, int z, int side);

    /**
     * Returns a integer with hex for 0xrrggbb with this color multiplied against the blocks color. Note only called
     * when first determining what to render.
     */
    public abstract int colorMultiplier(WorldServer world, int x, int y, int z, int said);

    public abstract int isProvidingWeakPower(WorldServer p_149709_1_, int p_149709_2_, int p_149709_3_, int p_149709_4_, int p_149709_5_);

    /**
     * Can this block provide power. Only wire currently seems to have this change based on its state.
     */
    public abstract boolean canProvidePower();

    public abstract int isProvidingStrongPower(WorldServer world, int x, int y, int z, int side);
}
