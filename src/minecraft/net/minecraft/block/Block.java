package net.minecraft.block;

import java.util.List;

import com.google.common.collect.HashBiMap;

import net.minecraft.client.WorldClient;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;

public abstract class Block
{
	public static final Block air = new BlockAir();
    public static final Block stone = new BlockStone();
    public static final Block redstone_wire = new BlockRedstoneWire();
    public static final Block lever = new BlockLever();
    public static final Block redstone_torch = new BlockRedstoneTorch();
    
    private static final HashBiMap<Integer, Block> registry = HashBiMap.create();
    
    static
    {
    	registry.put(0, air);
    	registry.put(1, stone);
    	registry.put(55, redstone_wire);
    	registry.put(69, lever);
    	registry.put(76, redstone_torch);
    }
    
    /**
     * Flags whether or not this block is of a type that needs random ticking. Ref-counted by ExtendedBlockStorage in
     * order to broadly cull a chunk from the random chunk update list for efficiency's sake.
     */
    private double minX;
    private double minY;
    private double minZ;
    private double maxX;
    private double maxY;
    private double maxZ;
    
    public static int getIdFromBlock(Block block)
    {
    	Integer id = registry.inverse().get(block);
        return id == null ? -1 : id.intValue();
    }

    public static Block getBlockById(int id)
    {
    	Block block = registry.get(id);
    	return block == null ? air : block;
    }

    protected final void setBlockBounds(float minXCoord, float minYCoord, float minZCoord, float maxXCoord, float maxYCoord, float maxZCoord)
    {
        this.minX = (double)minXCoord;
        this.minY = (double)minYCoord;
        this.minZ = (double)minZCoord;
        this.maxX = (double)maxXCoord;
        this.maxY = (double)maxYCoord;
        this.maxZ = (double)maxZCoord;
    }

    public final void addCollisionBoxesToList(int x, int y, int z, AxisAlignedBB otherAABB, List<AxisAlignedBB> list)
    {
        AxisAlignedBB thisAABB = this.getCollisionBoundingBoxFromPool(x, y, z);

        if (thisAABB != null && otherAABB.intersectsWith(thisAABB))
        {
            list.add(thisAABB);
        }
    }
    
    public final AxisAlignedBB generateCubicBoundingBox(int x, int y, int z)
    {
        return AxisAlignedBB.getBoundingBox((double)x + this.minX, (double)y + this.minY, (double)z + this.minZ, (double)x + this.maxX, (double)y + this.maxY, (double)z + this.maxZ);
    }

    public MovingObjectPosition collisionRayTrace(WorldClient world, int x, int y, int z, Vec3 p_149731_5_, Vec3 p_149731_6_)
    {
        p_149731_5_ = p_149731_5_.addVector((double)(-x), (double)(-y), (double)(-z));
        p_149731_6_ = p_149731_6_.addVector((double)(-x), (double)(-y), (double)(-z));
        Vec3 var7 = p_149731_5_.getIntermediateWithXValue(p_149731_6_, this.minX);
        Vec3 var8 = p_149731_5_.getIntermediateWithXValue(p_149731_6_, this.maxX);
        Vec3 var9 = p_149731_5_.getIntermediateWithYValue(p_149731_6_, this.minY);
        Vec3 var10 = p_149731_5_.getIntermediateWithYValue(p_149731_6_, this.maxY);
        Vec3 var11 = p_149731_5_.getIntermediateWithZValue(p_149731_6_, this.minZ);
        Vec3 var12 = p_149731_5_.getIntermediateWithZValue(p_149731_6_, this.maxZ);

        if (!this.isVecInsideYZBounds(var7))
        {
            var7 = null;
        }

        if (!this.isVecInsideYZBounds(var8))
        {
            var8 = null;
        }

        if (!this.isVecInsideXZBounds(var9))
        {
            var9 = null;
        }

        if (!this.isVecInsideXZBounds(var10))
        {
            var10 = null;
        }

        if (!this.isVecInsideXYBounds(var11))
        {
            var11 = null;
        }

        if (!this.isVecInsideXYBounds(var12))
        {
            var12 = null;
        }

        Vec3 var13 = null;

        if (var7 != null && (var13 == null || p_149731_5_.quadranceTo(var7) < p_149731_5_.quadranceTo(var13)))
        {
            var13 = var7;
        }

        if (var8 != null && (var13 == null || p_149731_5_.quadranceTo(var8) < p_149731_5_.quadranceTo(var13)))
        {
            var13 = var8;
        }

        if (var9 != null && (var13 == null || p_149731_5_.quadranceTo(var9) < p_149731_5_.quadranceTo(var13)))
        {
            var13 = var9;
        }

        if (var10 != null && (var13 == null || p_149731_5_.quadranceTo(var10) < p_149731_5_.quadranceTo(var13)))
        {
            var13 = var10;
        }

        if (var11 != null && (var13 == null || p_149731_5_.quadranceTo(var11) < p_149731_5_.quadranceTo(var13)))
        {
            var13 = var11;
        }

        if (var12 != null && (var13 == null || p_149731_5_.quadranceTo(var12) < p_149731_5_.quadranceTo(var13)))
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
    private boolean isVecInsideYZBounds(Vec3 v)
    {
        return v == null ? false : v.y >= this.minY && v.y <= this.maxY && v.z >= this.minZ && v.z <= this.maxZ;
    }

    /**
     * Checks if a vector is within the X and Z bounds of the block.
     */
    private boolean isVecInsideXZBounds(Vec3 v)
    {
        return v == null ? false : v.x >= this.minX && v.x <= this.maxX && v.z >= this.minZ && v.z <= this.maxZ;
    }

    /**
     * Checks if a vector is within the X and Y bounds of the block.
     */
    private boolean isVecInsideXYBounds(Vec3 v)
    {
        return v == null ? false : v.x >= this.minX && v.x <= this.maxX && v.y >= this.minY && v.y <= this.maxY;
    }

    /**
     * checks to see if you can place this block can be placed on that side of a block: BlockLever overrides
     */
    public boolean canPlaceBlockAt(WorldServer p_149742_1_, int p_149742_2_, int p_149742_3_, int p_149742_4_)
    {
    	return p_149742_1_.getBlock(p_149742_2_, p_149742_3_, p_149742_4_).isReplaceable();
    }

    public static boolean isEqualTo(Block block1, Block block2)
    {
        return block1 != null && block2 != null && block1 == block2;
    }

    public abstract boolean isSolid();
    
    public abstract boolean isReplaceable();

    /**
     * The type of render function that is called for this block
     */
    public abstract int getRenderType();

    /**
     * Returns a bounding box from the pool of bounding boxes (this means this box can change after the pool has been
     * cleared to be reused)
     */
    public abstract AxisAlignedBB getCollisionBoundingBoxFromPool(int x, int y, int z);

    /**
     * Ticks the block if it's been scheduled
     */
    public abstract void updateTick(WorldServer p_149674_1_, int p_149674_2_, int p_149674_3_, int p_149674_4_);

    public abstract void onNeighborBlockChange(WorldServer p_149695_1_, int p_149695_2_, int p_149695_3_, int p_149695_4_, Block p_149695_5_);

    public abstract void onBlockAdded(WorldServer p_149726_1_, int p_149726_2_, int p_149726_3_, int p_149726_4_);

    public abstract void breakBlock(WorldServer world, int x, int y, int z, Block block, int meta);
    
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
    public abstract int colorMultiplier(WorldClient cc, int x, int y, int z);

    public abstract int isProvidingWeakPower(WorldServer p_149709_1_, int p_149709_2_, int p_149709_3_, int p_149709_4_, int p_149709_5_);

    /**
     * Can this block provide power. Only wire currently seems to have this change based on its state.
     */
    public abstract boolean canProvidePower();

    public abstract int isProvidingStrongPower(WorldServer world, int x, int y, int z, int side);
}
