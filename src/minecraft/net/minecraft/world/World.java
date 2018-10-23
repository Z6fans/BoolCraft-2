package net.minecraft.world;

import net.minecraft.block.Block;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.WorldInfo;

public abstract class World<Entity extends EntityPlayer>
{
    /** Array list of players in the world. */
    protected Entity playerEntity;

    /**
     * holds information about a world (size on disk, time, spawn point, seed, ...)
     */
    protected WorldInfo worldInfo;

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
            Chunk chunk = null;

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
    public abstract boolean chunkExists(int x, int z);

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
     * Returns back a chunk looked up by chunk coordinates Args: x, z
     */
    public abstract Chunk provideChunk(int x, int z);

    /**
     * Sets the block ID and metadata at a given location. Args: X, Y, Z, new block ID, new metadata, flags. Flag 1 will
     * cause a block update. Flag 2 will send the change to clients (you almost always want this). Flag 4 prevents the
     * block from being re-rendered, if this is a client world. Flags can be added together.
     */
    public final boolean setBlock(int x, int y, int z, Block block, int metadata)
    {
    	if (x >= -30000000 && y >= 0 && z >= -30000000 && x < 30000000 && y < 256 && z < 30000000)
        {
    		Chunk chunk = this.provideChunk(x >> 4, z >> 4);
            Block oldBlock = chunk.getBlock(x & 15, y, z & 15);

            if (chunk.setBlockAndMeta(this, x & 15, y, z & 15, block, metadata))
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
                Chunk var4 = this.provideChunk(p_72805_1_ >> 4, p_72805_3_ >> 4);
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
        this.addPlayerToChunk(provideChunk(chunkX, chunkZ), player);
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
                            this.addPlayerToChunk(provideChunk(chunkX, chunkZ), this.playerEntity);
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
    
    private void addPlayerToChunk(Chunk chunk, Entity player){
    	int chunkY = MathHelper.floor_double(player.posY / 16.0D);

        if (chunkY < 0)
        {
            chunkY = 0;
        }

        player.addedToChunk = true;
        player.chunkCoordX = chunk.xPosition;
        player.chunkCoordY = chunkY;
        player.chunkCoordZ = chunk.zPosition;
    }

    /**
     * Runs a single tick for the world
     */
    public abstract void tick();

    public final void incrementTotalWorldTime(long time)
    {
        this.worldInfo.incrementTotalWorldTime(time);
    }

    public final long getTotalWorldTime()
    {
        return this.worldInfo.getWorldTotalTime();
    }
}
