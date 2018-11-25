package net.minecraft.player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class EntityPlayerMP
{
    /** Entity position X */
    public double posX;

    /** Entity position Z */
    public double posZ;
	private Minecraft minecraft;

    /** LinkedList that holds the loaded chunks. */
    public final List<ChunkCoordIntPair> loadedChunks = new LinkedList<ChunkCoordIntPair>();

    public EntityPlayerMP(Minecraft mc)
    {
        this.minecraft = mc;
        this.posX = 0.5D;
        this.posZ = 0.5D;
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
    	if (!this.loadedChunks.isEmpty())
        {
    		ArrayList<Chunk> chunksToSend = new ArrayList<Chunk>();
            Iterator<ChunkCoordIntPair> chunkIterator = this.loadedChunks.iterator();

            while (chunkIterator.hasNext() && chunksToSend.size() < 5)
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
                for (int i = 0; i < chunksToSend.size(); ++i)
                {
                	Chunk serverChunk = chunksToSend.get(i);
                    int chunkX = serverChunk.xPosition;
                    int chunkZ = serverChunk.zPosition;
                    this.minecraft.worldClient.loadChunk(chunkX, chunkZ);
                    Chunk clientChunk = this.minecraft.worldClient.provideChunk(chunkX, chunkZ);
                    clientChunk.setStorageArrays(this.copyStorage(serverChunk));
                    clientChunk.isTerrainPopulated = true;
                    clientChunk.setChunkModified();
                    this.minecraft.worldClient.markBlockRangeForRenderUpdate(chunkX << 4, 0, chunkZ << 4, (chunkX << 4) + 15, 256, (chunkZ << 4) + 15);
                }
            }
        }
    }
    
    private ExtendedBlockStorage[] copyStorage(Chunk chunk)
    {
    	ExtendedBlockStorage[] oldStorageArray = chunk.getBlockStorageArray();
    	ExtendedBlockStorage[] newStorageArray = new ExtendedBlockStorage[oldStorageArray.length];
    	
    	for(int i = 0; i < oldStorageArray.length; i++)
    	{
    		ExtendedBlockStorage oldStorage = oldStorageArray[i];
    		
    		if(oldStorage != null)
    		{
    			ExtendedBlockStorage newStorage = new ExtendedBlockStorage(oldStorage.getYLocation());
        		
        		if(oldStorage.getBlockLSBArray() != null)
        		{
        			byte[] oldLSBArray = oldStorage.getBlockLSBArray();
            		byte[] newLSBArray = newStorage.getBlockLSBArray();
            		System.arraycopy(oldLSBArray, 0, newLSBArray, 0, oldLSBArray.length);
        		}
        		
        		if(oldStorage.getBlockMSBArray() != null)
        		{
        			byte[] oldMSBArrayData = oldStorage.getBlockMSBArray().data;
            		NibbleArray newMSBArray = newStorage.getBlockMSBArray();
            		
            		if(newMSBArray == null)
            		{
            			newStorage.createBlockMSBArray();
            		}
            		
            		System.arraycopy(oldMSBArrayData, 0, newMSBArray.data, 0, oldMSBArrayData.length);
        		}
        		
        		if(oldStorage.getMetadataArray() != null)
        		{
        			byte[] oldMetadataArrayData = oldStorage.getMetadataArray().data;
        			NibbleArray newMetadataArray = newStorage.getMetadataArray();
            		System.arraycopy(oldMetadataArrayData, 0, newMetadataArray.data, 0, oldMetadataArrayData.length);
        		}
        		
        		newStorage.removeInvalidBlocks();
        		newStorageArray[i] = newStorage;
    		}
    	}
    	
    	return newStorageArray;
    }
}