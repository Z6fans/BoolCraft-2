package net.minecraft.world.chunk;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.player.EntityPlayerMP;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkProviderServer implements IChunkProvider
{
    private static final Logger logger = LogManager.getLogger();

    /**
     * used by unload100OldestChunks to iterate the loadedChunkHashMap for unload (underlying assumption, first in,
     * first out)
     */
    private Set chunksToUnload = Collections.newSetFromMap(new ConcurrentHashMap());
    private AnvilChunkLoader currentChunkLoader;
    private LongHashMap loadedChunkHashMap = new LongHashMap();
    private List loadedChunks = new ArrayList();
    private WorldServer worldObj;
    private static final String __OBFID = "CL_00001436";

    public ChunkProviderServer(WorldServer p_i1520_1_, AnvilChunkLoader p_i1520_2_)
    {
        this.worldObj = p_i1520_1_;
        this.currentChunkLoader = p_i1520_2_;
    }

    /**
     * Checks to see if a chunk exists at x, y
     */
    public boolean chunkExists(int p_73149_1_, int p_73149_2_)
    {
        return this.loadedChunkHashMap.containsItem(ChunkCoordIntPair.chunkXZ2Int(p_73149_1_, p_73149_2_));
    }

    public List func_152380_a()
    {
        return this.loadedChunks;
    }

    /**
     * marks chunk for unload by "unload100OldestChunks"  if there is no spawn point, or if the center of the chunk is
     * outside 200 blocks (x or z) of the spawn
     */
    public void unloadChunksIfNotNearSpawn(int p_73241_1_, int p_73241_2_)
    {
        int var4 = p_73241_1_ * 16 + 8;
        int var5 = p_73241_2_ * 16 + 8;
        short var6 = 128;

        if (var4 < -var6 || var4 > var6 || var5 < -var6 || var5 > var6)
        {
            this.chunksToUnload.add(Long.valueOf(ChunkCoordIntPair.chunkXZ2Int(p_73241_1_, p_73241_2_)));
        }
    }

    /**
     * loads or generates the chunk at the chunk location specified
     */
    public Chunk loadChunk(int x, int z)
    {
        long posHash = ChunkCoordIntPair.chunkXZ2Int(x, z);
        this.chunksToUnload.remove(Long.valueOf(posHash));
        Chunk newChunk = (Chunk)this.loadedChunkHashMap.getValueByKey(posHash);

        if (newChunk == null)
        {
            newChunk = this.safeLoadChunk(x, z);

            if (newChunk == null)
            {
            	try
                {
                	newChunk = new Chunk(this.worldObj, x, z);
                	System.out.println("creating new chunk in ChunkProviderServer.loadChunk");

                    for (int y = 0; y < 5; ++y)
                    {
                        ExtendedBlockStorage storage = newChunk.getBlockStorageArray()[0];

                        if (storage == null)
                        {
                            storage = new ExtendedBlockStorage(y);
                            newChunk.getBlockStorageArray()[0] = storage;
                        }

                        for (int localX = 0; localX < 16; ++localX)
                        {
                            for (int localZ = 0; localZ < 16; ++localZ)
                            {
                                storage.setBlock(localX, y, localZ, Block.stone);
                                storage.setExtBlockMetadata(localX, y, localZ, 0);
                            }
                        }
                    }
                }
                catch (Throwable t)
                {
                    throw new ReportedException(CrashReport.makeCrashReport(t, "Exception generating new chunk at " + x + ", " + z));
                }
            }

            this.loadedChunkHashMap.add(posHash, newChunk);
            this.loadedChunks.add(newChunk);
            newChunk.populateChunk(this, x, z);
        }

        return newChunk;
    }

    /**
     * Will return back a chunk, if it doesn't exist and its not a MP client it will generates all the blocks for the
     * specified chunk from the map seed and chunk seed
     */
    public Chunk provideChunk(int p_73154_1_, int p_73154_2_)
    {
        Chunk var3 = (Chunk)this.loadedChunkHashMap.getValueByKey(ChunkCoordIntPair.chunkXZ2Int(p_73154_1_, p_73154_2_));
        return var3 == null ? this.loadChunk(p_73154_1_, p_73154_2_) : var3;
    }

    /**
     * used by loadChunk, but catches any exceptions if the load fails.
     */
    private Chunk safeLoadChunk(int p_73239_1_, int p_73239_2_)
    {
        if (this.currentChunkLoader == null)
        {
            return null;
        }
        else
        {
            try
            {
                return this.currentChunkLoader.loadChunk(this.worldObj, p_73239_1_, p_73239_2_);
            }
            catch (Exception var4)
            {
                logger.error("Couldn\'t load chunk", var4);
                return null;
            }
        }
    }

    /**
     * used by saveChunks, but catches any exceptions if the save fails.
     */
    private void safeSaveChunk(Chunk p_73242_1_)
    {
        if (this.currentChunkLoader != null)
        {
            try
            {
                this.currentChunkLoader.saveChunk(this.worldObj, p_73242_1_);
            }
            catch (IOException var3)
            {
                logger.error("Couldn\'t save chunk", var3);
            }
            catch (MinecraftException var4)
            {
                logger.error("Couldn\'t save chunk; already in use by another instance of Minecraft?", var4);
            }
        }
    }

    /**
     * Populates chunk with ores etc etc
     */
    public void populate(int p_73153_2_, int p_73153_3_)
    {
        Chunk chunk = this.provideChunk(p_73153_2_, p_73153_3_);

        if (!chunk.isTerrainPopulated)
        {
            chunk.setChunkModified();
        }
    }

    /**
     * Two modes of operation: if passed true, save all Chunks in one go.  If passed false, save up to two chunks.
     * Return true if all chunks have been saved.
     */
    public boolean saveChunks(boolean p_73151_1_)
    {
        int var3 = 0;
        ArrayList var4 = Lists.newArrayList(this.loadedChunks);

        for (int var5 = 0; var5 < var4.size(); ++var5)
        {
            Chunk var6 = (Chunk)var4.get(var5);

            if (var6.needsSaving())
            {
                this.safeSaveChunk(var6);
                var6.isModified = false;
                ++var3;

                if (var3 == 24 && !p_73151_1_)
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Unloads chunks that are marked to be unloaded. This is not guaranteed to unload every such chunk.
     */
    public boolean unloadQueuedChunks()
    {
        if (!this.worldObj.levelSaving)
        {
            for (int var1 = 0; var1 < 100; ++var1)
            {
                if (!this.chunksToUnload.isEmpty())
                {
                    Long var2 = (Long)this.chunksToUnload.iterator().next();
                    Chunk var3 = (Chunk)this.loadedChunkHashMap.getValueByKey(var2.longValue());

                    if (var3 != null)
                    {
                        this.safeSaveChunk(var3);
                        this.loadedChunks.remove(var3);
                    }

                    this.chunksToUnload.remove(var2);
                    this.loadedChunkHashMap.remove(var2.longValue());
                }
            }
        }

        return false;
    }

    /**
     * Returns if the IChunkProvider supports saving.
     */
    public boolean canSave()
    {
        return !this.worldObj.levelSaving;
    }

    public int getLoadedChunkCount()
    {
        return this.loadedChunkHashMap.getNumHashElements();
    }
}
