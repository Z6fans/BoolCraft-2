package net.minecraft.world.chunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

import net.minecraft.block.Block;
import net.minecraft.player.EntityPlayer;
import net.minecraft.player.EntityPlayerSP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Chunk<Entity extends EntityPlayer>
{
    /**
     * Used to store block IDs, block MSBs, Sky-light maps, Block-light maps, and metadata. Each entry corresponds to a
     * logical segment of 16x16x16 blocks, stacked vertically.
     */
    private ExtendedBlockStorage[] storageArrays;

    /** Reference to the World object. */
    private World<Entity> worldObj;

    /** The x coordinate of the chunk. */
    public final int xPosition;

    /** The z coordinate of the chunk. */
    public final int zPosition;

    /** Boolean value indicating if the terrain is populated. */
    public boolean isTerrainPopulated;
    public boolean isLightPopulated;
    private boolean isLoaded;

    /**
     * Set to true if the chunk has been modified and needs to be updated internally.
     */
    public boolean isModified;
    private static final String __OBFID = "CL_00000373";

    public Chunk(World<Entity> world, int x, int y)
    {
        this.storageArrays = new ExtendedBlockStorage[16];
        this.worldObj = world;
        this.xPosition = x;
        this.zPosition = y;
    }

    /**
     * Checks whether the chunk is at the X/Z location specified
     */
    public boolean isAtLocation(int p_76600_1_, int p_76600_2_)
    {
        return p_76600_1_ == this.xPosition && p_76600_2_ == this.zPosition;
    }

    /**
     * Returns the topmost ExtendedBlockStorage instance for this Chunk that actually contains a block.
     */
    public int getTopFilledSegment()
    {
        for (int var1 = this.storageArrays.length - 1; var1 >= 0; --var1)
        {
            if (this.storageArrays[var1] != null)
            {
                return this.storageArrays[var1].getYLocation();
            }
        }

        return 0;
    }

    /**
     * Returns the ExtendedBlockStorage array for this Chunk.
     */
    public ExtendedBlockStorage[] getBlockStorageArray()
    {
        return this.storageArrays;
    }

    /**
     * Generates the height map for a chunk from scratch
     */
    public void generateHeightMap()
    {
        this.isModified = true;
    }

    public Block getBlock(final int x, final int y, final int z)
    {
        Block block = Block.air;

        if (y >> 4 < this.storageArrays.length)
        {
            ExtendedBlockStorage storage = this.storageArrays[y >> 4];

            if (storage != null)
            {
                block = storage.getBlock(x, y & 15, z);
            }
        }

        return block;
    }

    /**
     * Return the metadata corresponding to the given coordinates inside a chunk.
     */
    public int getBlockMetadata(int p_76628_1_, int p_76628_2_, int p_76628_3_)
    {
        if (p_76628_2_ >> 4 >= this.storageArrays.length)
        {
            return 0;
        }
        else
        {
            ExtendedBlockStorage var4 = this.storageArrays[p_76628_2_ >> 4];
            return var4 != null ? var4.getExtBlockMetadata(p_76628_1_, p_76628_2_ & 15, p_76628_3_) : 0;
        }
    }

    public boolean setBlockAndMeta(int localX, int y, int localZ, Block block, int meta)
    {
        Block oldBlock = this.getBlock(localX, y, localZ);
        int oldMeta = this.getBlockMetadata(localX, y, localZ);

        if (oldBlock == block && oldMeta == meta)
        {
            return false;
        }
        else
        {
            ExtendedBlockStorage storageArray = this.storageArrays[y >> 4];

            if (storageArray == null)
            {
                if (block == Block.air)
                {
                    return false;
                }

                storageArray = this.storageArrays[y >> 4] = new ExtendedBlockStorage(y >> 4 << 4);
            }

            int trueX = this.xPosition * 16 + localX;
            int trueZ = this.zPosition * 16 + localZ;

            storageArray.setBlock(localX, y & 15, localZ, block);

            if (this.worldObj instanceof WorldServer)
            {
                oldBlock.breakBlock((WorldServer)this.worldObj, trueX, y, trueZ, oldBlock, oldMeta);
            }

            if (storageArray.getBlock(localX, y & 15, localZ) != block)
            {
                return false;
            }
            else
            {
                storageArray.setExtBlockMetadata(localX, y & 15, localZ, meta);

                if (this.worldObj instanceof WorldServer)
                {
                    block.onBlockAdded((WorldServer)this.worldObj, trueX, y, trueZ);
                }

                this.isModified = true;
                return true;
            }
        }
    }

    /**
     * Set the metadata of a block in the chunk
     */
    public boolean setBlockMetadata(int x, int y, int z, int newMeta)
    {
        ExtendedBlockStorage storage = this.storageArrays[y >> 4];

        if (storage == null)
        {
            return false;
        }
        else
        {
            int oldMeta = storage.getExtBlockMetadata(x, y & 15, z);

            if (oldMeta == newMeta)
            {
                return false;
            }
            else
            {
                this.isModified = true;
                storage.setExtBlockMetadata(x, y & 15, z, newMeta);

                return true;
            }
        }
    }

    /**
     * Adds an entity to the chunk. Args: entity
     */
    public void addPlayer(Entity player)
    {
        int chunkY = MathHelper.floor_double(player.posY / 16.0D);

        if (chunkY < 0)
        {
            chunkY = 0;
        }

        player.addedToChunk = true;
        player.chunkCoordX = this.xPosition;
        player.chunkCoordY = chunkY;
        player.chunkCoordZ = this.zPosition;
    }

    /**
     * Sets the isModified flag for this Chunk
     */
    public void setChunkModified()
    {
        this.isModified = true;
    }

    /**
     * Returns true if this Chunk needs to be saved
     */
    public boolean needsSaving()
    {
        return this.isModified;
    }

    public boolean isEmpty()
    {
        return false;
    }

    public void populateChunk(ChunkProviderServer provider, int chunkX, int chunkZ)
    {
        if (!this.isTerrainPopulated && provider.chunkExists(chunkX + 1, chunkZ + 1) && provider.chunkExists(chunkX, chunkZ + 1) && provider.chunkExists(chunkX + 1, chunkZ))
        {
            provider.populate(chunkX, chunkZ);
        }

        if (provider.chunkExists(chunkX - 1, chunkZ) && !provider.provideChunk(chunkX - 1, chunkZ).isTerrainPopulated && provider.chunkExists(chunkX - 1, chunkZ + 1) && provider.chunkExists(chunkX, chunkZ + 1) && provider.chunkExists(chunkX - 1, chunkZ + 1))
        {
            provider.populate(chunkX - 1, chunkZ);
        }

        if (provider.chunkExists(chunkX, chunkZ - 1) && !provider.provideChunk(chunkX, chunkZ - 1).isTerrainPopulated && provider.chunkExists(chunkX + 1, chunkZ - 1) && provider.chunkExists(chunkX + 1, chunkZ - 1) && provider.chunkExists(chunkX + 1, chunkZ))
        {
            provider.populate(chunkX, chunkZ - 1);
        }

        if (provider.chunkExists(chunkX - 1, chunkZ - 1) && !provider.provideChunk(chunkX - 1, chunkZ - 1).isTerrainPopulated && provider.chunkExists(chunkX, chunkZ - 1) && provider.chunkExists(chunkX - 1, chunkZ))
        {
            provider.populate(chunkX - 1, chunkZ - 1);
        }
    }

    public void setLoaded()
    {
        this.isLoaded = true;
    }

    public boolean getLoaded()
    {
        return this.isLoaded;
    }

    /**
     * Gets a ChunkCoordIntPair representing the Chunk's position.
     */
    public ChunkCoordIntPair getChunkCoordIntPair()
    {
        return new ChunkCoordIntPair(this.xPosition, this.zPosition);
    }

    /**
     * Returns whether the ExtendedBlockStorages containing levels (in blocks) from arg 1 to arg 2 are fully empty
     * (true) or not (false).
     */
    public boolean getAreLevelsEmpty(int yMin, int yMax)
    {
        if (yMin < 0)
        {
            yMin = 0;
        }

        if (yMax >= 256)
        {
            yMax = 255;
        }

        for (int y = yMin; y <= yMax; y += 16)
        {
            ExtendedBlockStorage storage = this.storageArrays[y >> 4];

            if (storage != null && !storage.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    public void setStorageArrays(ExtendedBlockStorage[] storage)
    {
        this.storageArrays = storage;
    }

    /**
     * Initialise this chunk with new binary data
     */
    public void fillChunk(byte[] p_76607_1_, int p_76607_2_, int p_76607_3_, boolean p_76607_4_)
    {
        int var5 = 0;

        for (int i = 0; i < this.storageArrays.length; ++i)
        {
            if ((p_76607_2_ & 1 << i) != 0)
            {
                if (this.storageArrays[i] == null)
                {
                    this.storageArrays[i] = new ExtendedBlockStorage(i << 4);
                }

                byte[] var8 = this.storageArrays[i].getBlockLSBArray();
                System.arraycopy(p_76607_1_, var5, var8, 0, var8.length);
                var5 += var8.length;
            }
            else if (p_76607_4_ && this.storageArrays[i] != null)
            {
                this.storageArrays[i] = null;
            }
        }

        NibbleArray var10;

        for (int i = 0; i < this.storageArrays.length; ++i)
        {
            if ((p_76607_2_ & 1 << i) != 0 && this.storageArrays[i] != null)
            {
                var10 = this.storageArrays[i].getMetadataArray();
                System.arraycopy(p_76607_1_, var5, var10.data, 0, var10.data.length);
                var5 += var10.data.length;
            }
        }

        for (int i = 0; i < this.storageArrays.length; ++i)
        {
            if ((p_76607_3_ & 1 << i) != 0)
            {
                if (this.storageArrays[i] == null)
                {
                    var5 += 2048;
                }
                else
                {
                    var10 = this.storageArrays[i].getBlockMSBArray();

                    if (var10 == null)
                    {
                        var10 = this.storageArrays[i].createBlockMSBArray();
                    }

                    System.arraycopy(p_76607_1_, var5, var10.data, 0, var10.data.length);
                    var5 += var10.data.length;
                }
            }
            else if (p_76607_4_ && this.storageArrays[i] != null && this.storageArrays[i].getBlockMSBArray() != null)
            {
                this.storageArrays[i].clearMSBArray();
            }
        }

        for (int i = 0; i < this.storageArrays.length; ++i)
        {
            if (this.storageArrays[i] != null && (p_76607_2_ & 1 << i) != 0)
            {
                this.storageArrays[i].removeInvalidBlocks();
            }
        }

        this.isLightPopulated = true;
        this.isTerrainPopulated = true;
        this.generateHeightMap();
    }
}
