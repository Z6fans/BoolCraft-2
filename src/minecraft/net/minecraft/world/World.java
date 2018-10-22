package net.minecraft.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;

public abstract class World<Entity extends EntityPlayer> implements IBlockAccess
{
    /** Array list of players in the world. */
    protected Entity playerEntity;

    /** RNG for World. */
    protected Random rand = new Random();

    /** Handles chunk operations and caching */
    protected IChunkProvider chunkProvider;

    /**
     * holds information about a world (size on disk, time, spawn point, seed, ...)
     */
    protected WorldInfo worldInfo;
    private static final String __OBFID = "CL_00000140";

    protected World(WorldInfo wi)
    {
    	this.playerEntity = null;
        this.worldInfo = wi;

        if (this.worldInfo == null)
        {
            this.worldInfo = new WorldInfo();
        }

        if (!this.worldInfo.isInitialized())
        {
            try
            {
                this.initialize();
            }
            catch (Throwable t)
            {
                throw new ReportedException(CrashReport.makeCrashReport(t, "Exception initializing level"));
            }

            this.worldInfo.setServerInitialized(true);
        }
    }

    protected void initialize()
    {
        this.worldInfo.setServerInitialized(true);
    }

    public final Block getBlock(int x, int y, int z)
    {
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000 && y >= 0 && y < 256)
        {
            Chunk<Entity> chunk = null;

            try
            {
                chunk = this.provideChunk(x >> 4, z >> 4);
                return chunk.getBlock(x & 15, y, z & 15);
            }
            catch (Throwable t)
            {
                throw new ReportedException(CrashReport.makeCrashReport(t, "Exception getting block type in world, did " + (chunk == null ? "not " : "") + "find chunk"));
            }
        }
        else
        {
            return Block.air;
        }
    }

    /**
     * Returns whether a block exists at world coordinates x, y, z
     */
    public final boolean chunkExists(int x, int z)
    {
        return this.chunkProvider.chunkExists(x, z);
    }

    /**
     * Checks between a min and max all the chunks inbetween actually exist. Args: minX, minY, minZ, maxX, maxY, maxZ
     */
    public final boolean checkChunksExist(int p_72904_1_, int p_72904_2_, int p_72904_3_, int p_72904_4_, int p_72904_5_, int p_72904_6_)
    {
        if (p_72904_5_ >= 0 && p_72904_2_ < 256)
        {
            p_72904_1_ >>= 4;
            p_72904_3_ >>= 4;
            p_72904_4_ >>= 4;
            p_72904_6_ >>= 4;

            for (int var7 = p_72904_1_; var7 <= p_72904_4_; ++var7)
            {
                for (int var8 = p_72904_3_; var8 <= p_72904_6_; ++var8)
                {
                    if (!this.chunkExists(var7, var8))
                    {
                        return false;
                    }
                }
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Returns back a chunk looked up by chunk coordinates Args: x, y
     */
    public final Chunk<Entity> provideChunk(int x, int y)
    {
        return this.chunkProvider.provideChunk(x, y);
    }

    /**
     * Sets the block ID and metadata at a given location. Args: X, Y, Z, new block ID, new metadata, flags. Flag 1 will
     * cause a block update. Flag 2 will send the change to clients (you almost always want this). Flag 4 prevents the
     * block from being re-rendered, if this is a client world. Flags can be added together.
     */
    public final boolean setBlock(int x, int y, int z, Block block, int metadata)
    {
    	if (x >= -30000000 && y >= 0 && z >= -30000000 && x < 30000000 && y < 256 && z < 30000000)
        {
    		Chunk<Entity> chunk = this.provideChunk(x >> 4, z >> 4);
            Block oldBlock = chunk.getBlock(x & 15, y, z & 15);

            if (chunk.setBlockAndMeta(x & 15, y, z & 15, block, metadata))
            {
                if (chunk.getLoaded())
                {
                    this.markBlockForUpdate(x, y, z);
                }

                this.notifyBlocksOfNeighborChange(x, y, z, oldBlock);
                return true;
            }
        }
    	
        return false;
    }

    /**
     * Returns the block metadata at coords x,y,z
     */
    public final int getBlockMetadata(int p_72805_1_, int p_72805_2_, int p_72805_3_)
    {
        if (p_72805_1_ >= -30000000 && p_72805_3_ >= -30000000 && p_72805_1_ < 30000000 && p_72805_3_ < 30000000)
        {
            if (p_72805_2_ < 0)
            {
                return 0;
            }
            else if (p_72805_2_ >= 256)
            {
                return 0;
            }
            else
            {
                Chunk<Entity> var4 = this.provideChunk(p_72805_1_ >> 4, p_72805_3_ >> 4);
                p_72805_1_ &= 15;
                p_72805_3_ &= 15;
                return var4.getBlockMetadata(p_72805_1_, p_72805_2_, p_72805_3_);
            }
        }
        else
        {
            return 0;
        }
    }

    /**
     * Sets the blocks metadata and if set will then notify blocks that this block changed, depending on the flag. Args:
     * x, y, z, metadata, flag. See setBlock for flag description
     */
    public final boolean setBlockMetadataWithNotify(int x, int y, int z, int metadata, boolean flag)
    {
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000)
        {
            if (y < 0)
            {
                return false;
            }
            else if (y >= 256)
            {
                return false;
            }
            else
            {
                Chunk<Entity> chunk = this.provideChunk(x >> 4, z >> 4);
                int localX = x & 15;
                int localZ = z & 15;
                boolean didChange = chunk.setBlockMetadata(localX, y, localZ, metadata);

                if (didChange)
                {
                    Block block = chunk.getBlock(localX, y, localZ);

                    if (chunk.getLoaded())
                    {
                        this.markBlockForUpdate(x, y, z);
                    }

                    if (flag)
                    {
                        this.notifyBlocksOfNeighborChange(x, y, z, block);
                    }
                }

                return didChange;
            }
        }
        else
        {
            return false;
        }
    }

    public final boolean setBlockToAir(int x, int y, int z)
    {
        return this.setBlock(x, y, z, Block.air, 0);
    }

    protected abstract void markBlockForUpdate(int x, int y, int z);

    public abstract void notifyBlocksOfNeighborChange(int x, int y, int z, Block block);

    /**
     * Called to place all entities as part of a world
     */
    public final void spawnEntityInWorld(Entity player)
    {
        int chunkX = MathHelper.floor_double(player.posX / 16.0D);
        int chunkZ = MathHelper.floor_double(player.posZ / 16.0D);
        this.playerEntity = player;
        this.provideChunk(chunkX, chunkZ).addPlayer(player);
    }

    /**
     * Updates (and cleans up) entities and tile entities
     */
    public void updateEntities()
    {
        if (this.playerEntity != null)
        {
        	try
            {
        		int playerX = MathHelper.floor_double(this.playerEntity.posX);
                int playerZ = MathHelper.floor_double(this.playerEntity.posZ);
                int r = 32;

                if (this.checkChunksExist(playerX - r, 0, playerZ - r, playerX + r, 0, playerZ + r))
                {
                	this.playerEntity.onUpdate();

                    int chunkX = MathHelper.floor_double(this.playerEntity.posX / 16.0D);
                    int chunkY = MathHelper.floor_double(this.playerEntity.posY / 16.0D);
                    int chunkZ = MathHelper.floor_double(this.playerEntity.posZ / 16.0D);

                    if (!this.playerEntity.addedToChunk || this.playerEntity.chunkCoordX != chunkX || this.playerEntity.chunkCoordY != chunkY || this.playerEntity.chunkCoordZ != chunkZ)
                    {
                        if (this.chunkExists(chunkX, chunkZ))
                        {
                        	this.playerEntity.addedToChunk = true;
                            this.provideChunk(chunkX, chunkZ).addPlayer(this.playerEntity);
                        }
                        else
                        {
                        	this.playerEntity.addedToChunk = false;
                        }
                    }
                }
            }
            catch (Throwable t)
            {
                throw new ReportedException(CrashReport.makeCrashReport(t, "Ticking entity"));
            }
        }
    }

    /**
     * Returns true if the block at the given coordinate has a solid (buildable) top surface.
     */
    public static boolean doesBlockHaveSolidTopSurface(World world, int x, int y, int z)
    {
        return world.getBlock(x, y, z).isSolid();
    }

    /**
     * Checks if the block is a solid, normal cube. If the chunk does not exist, or is not loaded, it returns the
     * boolean parameter
     */
    public final boolean isBlockNormalCubeDefault(int x, int y, int z, boolean def)
    {
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000)
        {
            Chunk<Entity> chunk = this.chunkProvider.provideChunk(x >> 4, z >> 4);

            if (chunk != null && !chunk.isEmpty())
            {
                return this.getBlock(x, y, z).isSolid();
            }
            else
            {
                return def;
            }
        }
        else
        {
            return def;
        }
    }

    /**
     * Runs a single tick for the world
     */
    public abstract void tick();

    public final boolean canPlaceEntityOnSide(Block block, int x, int y, int z, int side)
    {
        return this.getBlock(x, y, z).isReplaceable() && block.canPlaceBlockOnSide(this, x, y, z, side);
    }

    /**
     * Is this block powering in the specified direction Args: x, y, z, direction
     */
    private int isBlockProvidingPowerTo(int p_72879_1_, int p_72879_2_, int p_72879_3_, int p_72879_4_)
    {
        return this.getBlock(p_72879_1_, p_72879_2_, p_72879_3_).isProvidingStrongPower(this, p_72879_1_, p_72879_2_, p_72879_3_, p_72879_4_);
    }

    /**
     * Returns the highest redstone signal strength powering the given block. Args: X, Y, Z.
     */
    private int getBlockPowerInput(int p_94577_1_, int p_94577_2_, int p_94577_3_)
    {
        byte var4 = 0;
        int var5 = Math.max(var4, this.isBlockProvidingPowerTo(p_94577_1_, p_94577_2_ - 1, p_94577_3_, 0));

        if (var5 >= 15)
        {
            return var5;
        }
        else
        {
            var5 = Math.max(var5, this.isBlockProvidingPowerTo(p_94577_1_, p_94577_2_ + 1, p_94577_3_, 1));

            if (var5 >= 15)
            {
                return var5;
            }
            else
            {
                var5 = Math.max(var5, this.isBlockProvidingPowerTo(p_94577_1_, p_94577_2_, p_94577_3_ - 1, 2));

                if (var5 >= 15)
                {
                    return var5;
                }
                else
                {
                    var5 = Math.max(var5, this.isBlockProvidingPowerTo(p_94577_1_, p_94577_2_, p_94577_3_ + 1, 3));

                    if (var5 >= 15)
                    {
                        return var5;
                    }
                    else
                    {
                        var5 = Math.max(var5, this.isBlockProvidingPowerTo(p_94577_1_ - 1, p_94577_2_, p_94577_3_, 4));

                        if (var5 >= 15)
                        {
                            return var5;
                        }
                        else
                        {
                            var5 = Math.max(var5, this.isBlockProvidingPowerTo(p_94577_1_ + 1, p_94577_2_, p_94577_3_, 5));
                            return var5 >= 15 ? var5 : var5;
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the indirect signal strength being outputted by the given block in the *opposite* of the given direction.
     * Args: X, Y, Z, direction
     */
    public final boolean getIndirectPowerOutput(int x, int y, int z, int side)
    {
        return this.getIndirectPowerLevelTo(x, y, z, side) > 0;
    }

    /**
     * Gets the power level from a certain block face.  Args: x, y, z, direction
     */
    private int getIndirectPowerLevelTo(int x, int y, int z, int side)
    {
        return this.getBlock(x, y, z).isSolid() ? this.getBlockPowerInput(x, y, z) : this.getBlock(x, y, z).isProvidingWeakPower(this, x, y, z, side);
    }

    public final int getStrongestIndirectPower(int x, int y, int z)
    {
        int max = 0;
        
        int[] offsetsXForSide = new int[] {0, 0, 0, 0, -1, 1};
        int[] offsetsYForSide = new int[] {-1, 1, 0, 0, 0, 0};
        int[] offsetsZForSide = new int[] {0, 0, -1, 1, 0, 0};

        for (int side = 0; side < 6; ++side)
        {
            int power = this.getIndirectPowerLevelTo(x + offsetsXForSide[side], y + offsetsYForSide[side], z + offsetsZForSide[side], side);

            if (power >= 15)
            {
                return 15;
            }

            if (power > max)
            {
                max = power;
            }
        }

        return max;
    }

    public final void incrementTotalWorldTime(long time)
    {
        this.worldInfo.incrementTotalWorldTime(time);
    }

    public final long getTotalWorldTime()
    {
        return this.worldInfo.getWorldTotalTime();
    }
}
