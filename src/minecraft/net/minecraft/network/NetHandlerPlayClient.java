package net.minecraft.network;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.player.EntityPlayerSP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

public class NetHandlerPlayClient
{
	/**
     * Reference to the current ClientWorld instance, which many handler methods operate on
     */
    private WorldClient worldClient;
    private static final String __OBFID = "CL_00000878";

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
    public void handleChunkData(S21PacketChunkData packetChunkData)
    {
        if (packetChunkData.func_149274_i())
        {
            if (packetChunkData.func_149276_g() == 0)
            {
                this.worldClient.doPreChunk(packetChunkData.getChunkX(), packetChunkData.getChunkZ(), false);
                return;
            }
            
            this.worldClient.doPreChunk(packetChunkData.getChunkX(), packetChunkData.getChunkZ(), true);
        }

        Chunk<EntityPlayerSP> chunk = this.worldClient.provideChunk(packetChunkData.getChunkX(), packetChunkData.getChunkZ());
        chunk.fillChunk(packetChunkData.func_149272_d(), packetChunkData.func_149276_g(), packetChunkData.func_149270_h(), packetChunkData.func_149274_i());
        this.worldClient.markBlockRangeForRenderUpdate(packetChunkData.getChunkX() << 4, 0, packetChunkData.getChunkZ() << 4, (packetChunkData.getChunkX() << 4) + 15, 256, (packetChunkData.getChunkZ() << 4) + 15);
    }

    public void handleBlockChange(int x, int y, int z, WorldServer worldServer)
    {
        this.worldClient.setBlock(x, y, z, worldServer.getBlock(x, y, z), worldServer.getBlockMetadata(x, y, z));
    }

    public void handleMapChunkBulk(S26PacketMapChunkBulk packetMapChunkBulk)
    {
        for (int i = 0; i < packetMapChunkBulk.getNumChunks(); ++i)
        {
            int chunkX = packetMapChunkBulk.getChunkX(i);
            int chunkZ = packetMapChunkBulk.getChunkZ(i);
            this.worldClient.doPreChunk(chunkX, chunkZ, true);
            Chunk<EntityPlayerSP> chunk = this.worldClient.provideChunk(chunkX, chunkZ);
            chunk.fillChunk(packetMapChunkBulk.func_149256_c(i), packetMapChunkBulk.func_149252_e()[i], packetMapChunkBulk.func_149257_f()[i], true);
            this.worldClient.markBlockRangeForRenderUpdate(chunkX << 4, 0, chunkZ << 4, (chunkX << 4) + 15, 256, (chunkZ << 4) + 15);
        }
    }
}
