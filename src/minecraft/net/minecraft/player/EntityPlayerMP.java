package net.minecraft.player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.S26PacketMapChunkBulk;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;

public class EntityPlayerMP extends EntityPlayer
{
	private Minecraft minecraft;

    /** LinkedList that holds the loaded chunks. */
    public final List<ChunkCoordIntPair> loadedChunks = new LinkedList<ChunkCoordIntPair>();

    public EntityPlayerMP(Minecraft mc)
    {
        this.minecraft = mc;
        this.posX = 0.5D;
        this.posY = mc.worldServer.getTopSolidOrLiquidBlock(0, 0);
        this.posZ = 0.5D;
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
    	if (this.addedToChunk && !this.loadedChunks.isEmpty())
        {
    		ArrayList<Chunk> chunksToSend = new ArrayList<Chunk>();
            Iterator<ChunkCoordIntPair> chunkIterator = this.loadedChunks.iterator();

            while (chunkIterator.hasNext() && chunksToSend.size() < S26PacketMapChunkBulk.maxChunks())
            {
                ChunkCoordIntPair chunkCoords = (ChunkCoordIntPair)chunkIterator.next();

                if (chunkCoords != null)
                {
                    if (this.minecraft.worldServer.chunkExists(chunkCoords.chunkXPos, chunkCoords.chunkZPos))
                    {
                        Chunk chunk = this.minecraft.worldServer.provideChunk(chunkCoords.chunkXPos, chunkCoords.chunkZPos);
                        
                        if (chunk.getLoaded())
                        {
                            chunksToSend.add(chunk);
                            chunkIterator.remove();
                        }
                    }
                }
                else
                {
                    chunkIterator.remove();
                }
            }

            if (!chunksToSend.isEmpty())
            {
                this.minecraft.clientHandler.handleMapChunkBulk(new S26PacketMapChunkBulk(chunksToSend));
            }
        }
    }
}