package net.minecraft.network;

import java.util.List;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class NetHandlerPlayClient
{
	/**
     * Reference to the current ClientWorld instance, which many handler methods operate on
     */
    private WorldClient worldClient;

    public void setWorld(WorldClient wc)
    {
        this.worldClient = wc;
    }
    
    public void handleMultiBlockChange(int numTiles, short[] localKeyArray, Chunk chunk)
    {
        int baseX = chunk.xPosition * 16;
        int baseZ = chunk.zPosition * 16;

        for (int i = 0; i < numTiles; ++i)
        {
            short localKey = localKeyArray[i];
            int localX = localKey >> 12 & 15;
            int localZ = localKey >> 8 & 15;
            int localY = localKey & 255;
            this.worldClient.setBlock(localX + baseX, localY, localZ + baseZ, chunk.getBlock(localX, localY, localZ), chunk.getBlockMetadata(localX, localY, localZ));
        }
    }

    /**
     * Updates the specified chunk with the supplied data, marks it for re-rendering and lighting recalculation
     */
    public void handleUnloadChunk(Chunk serverChunk)
    {
    	this.worldClient.unloadChunk(serverChunk.xPosition, serverChunk.zPosition);
    }

    public void handleBlockChange(int x, int y, int z, WorldServer worldServer)
    {
        this.worldClient.setBlock(x, y, z, worldServer.getBlock(x, y, z), worldServer.getBlockMetadata(x, y, z));
    }

    public void handleMapChunkBulk(List<Chunk> chunks)
    {
        for (int i = 0; i < chunks.size(); ++i)
        {
        	Chunk serverChunk = chunks.get(i);
            int chunkX = serverChunk.xPosition;
            int chunkZ = serverChunk.zPosition;
            this.worldClient.loadChunk(chunkX, chunkZ);
            Chunk clientChunk = this.worldClient.provideChunk(chunkX, chunkZ);
            clientChunk.setStorageArrays(this.copyStorage(serverChunk));
            clientChunk.isLightPopulated = true;
            clientChunk.isTerrainPopulated = true;
            clientChunk.generateHeightMap();
            this.worldClient.markBlockRangeForRenderUpdate(chunkX << 4, 0, chunkZ << 4, (chunkX << 4) + 15, 256, (chunkZ << 4) + 15);
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

    public static int maxChunks()
    {
        return 5;
    }
}
