package net.minecraft.server;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.S21PacketChunkData;
import net.minecraft.player.EntityPlayerMP;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

public class PlayerChunkLoadManager
{
    private final WorldServer theWorldServer;

    /** players in the current instance */
    private EntityPlayerMP player;

    /** player X position as seen by PlayerManager */
    private double managedPosX;

    /** player Z position as seen by PlayerManager */
    private double managedPosZ;

    /**
     * A map of chunk position (two ints concatenated into a long) to PlayerInstance
     */
    private final LongHashMap<PlayerInstance> playerInstances = new LongHashMap<PlayerInstance>();

    /**
     * contains a PlayerInstance for every chunk they can see. the "player instance" cotains a list of all players who
     * can also that chunk
     */
    private final List<PlayerInstance> chunkWatcherWithPlayers = new ArrayList<PlayerInstance>();

    /** This field is using when chunk should be processed (every 8000 ticks) */
    private final List<PlayerInstance> playerInstanceList = new ArrayList<PlayerInstance>();

    /**
     * Number of chunks the server sends to the client. Valid 3<=x<=15. In server.properties.
     */
    private int playerViewRadius;

    /** time what is using to check if InhabitedTime should be calculated */
    private long previousTotalWorldTime;

    /** x, z direction vectors: east, south, west, north */
    private final int[][] xzDirectionsConst = new int[][] {{1, 0}, {0, 1}, { -1, 0}, {0, -1}};

    public PlayerChunkLoadManager(WorldServer p_i1176_1_)
    {
        this.theWorldServer = p_i1176_1_;
        this.playerViewRadius = 10;
    }

    public WorldServer getWorldServer()
    {
        return this.theWorldServer;
    }

    /**
     * updates all the player instances that need to be updated
     */
    public void updatePlayerInstances()
    {
        long var1 = this.theWorldServer.getTotalWorldTime();
        int var3;
        PlayerChunkLoadManager.PlayerInstance var4;

        if (var1 - this.previousTotalWorldTime > 8000L)
        {
            this.previousTotalWorldTime = var1;

            for (var3 = 0; var3 < this.playerInstanceList.size(); ++var3)
            {
                var4 = (PlayerChunkLoadManager.PlayerInstance)this.playerInstanceList.get(var3);
                var4.sendChunkUpdate();
            }
        }
        else
        {
            for (var3 = 0; var3 < this.chunkWatcherWithPlayers.size(); ++var3)
            {
                var4 = (PlayerChunkLoadManager.PlayerInstance)this.chunkWatcherWithPlayers.get(var3);
                var4.sendChunkUpdate();
            }
        }

        this.chunkWatcherWithPlayers.clear();
    }

    public boolean doesPlayerInstanceExist(int chunkX, int chunkZ)
    {
        long key = (long)chunkX + 2147483647L | (long)chunkZ + 2147483647L << 32;
        return this.playerInstances.getValueByKey(key) != null;
    }

    private PlayerChunkLoadManager.PlayerInstance getOrCreateChunkWatcher(int chunkX, int chunkZ, boolean doCreate)
    {
        long key = (long)chunkX + 2147483647L | (long)chunkZ + 2147483647L << 32;
        PlayerChunkLoadManager.PlayerInstance chunkWatcher = (PlayerChunkLoadManager.PlayerInstance)this.playerInstances.getValueByKey(key);

        if (chunkWatcher == null && doCreate)
        {
            chunkWatcher = new PlayerChunkLoadManager.PlayerInstance(chunkX, chunkZ);
            this.playerInstances.add(key, chunkWatcher);
            this.playerInstanceList.add(chunkWatcher);
        }

        return chunkWatcher;
    }

    public void markBlockForUpdate(int x, int y, int z)
    {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        PlayerChunkLoadManager.PlayerInstance playerInstance = this.getOrCreateChunkWatcher(chunkX, chunkZ, false);

        if (playerInstance != null)
        {
            playerInstance.markBlockForUpdate(x & 15, y, z & 15);
        }
    }

    /**
     * Adds an EntityPlayerMP to the PlayerManager.
     */
    public void addPlayer(EntityPlayerMP player)
    {
        int chunkX = (int)player.posX >> 4;
        int chunkZ = (int)player.posZ >> 4;
        this.managedPosX = player.posX;
        this.managedPosZ = player.posZ;

        for (int x = chunkX - this.playerViewRadius; x <= chunkX + this.playerViewRadius; ++x)
        {
            for (int y = chunkZ - this.playerViewRadius; y <= chunkZ + this.playerViewRadius; ++y)
            {
                this.getOrCreateChunkWatcher(x, y, true).addPlayer(player);
            }
        }

        this.player = player;
        this.filterChunkLoadQueue(player);
    }

    /**
     * Removes all chunks from the given player's chunk load queue that are not in viewing range of the player.
     */
    private void filterChunkLoadQueue(EntityPlayerMP p_72691_1_)
    {
        ArrayList<ChunkCoordIntPair> var2 = new ArrayList<ChunkCoordIntPair>(p_72691_1_.loadedChunks);
        int var3 = 0;
        int var4 = this.playerViewRadius;
        int var5 = (int)p_72691_1_.posX >> 4;
        int var6 = (int)p_72691_1_.posZ >> 4;
        int var7 = 0;
        int var8 = 0;
        ChunkCoordIntPair var9 = this.getOrCreateChunkWatcher(var5, var6, true).chunkLocation;
        p_72691_1_.loadedChunks.clear();

        if (var2.contains(var9))
        {
            p_72691_1_.loadedChunks.add(var9);
        }

        int var10;

        for (var10 = 1; var10 <= var4 * 2; ++var10)
        {
            for (int var11 = 0; var11 < 2; ++var11)
            {
                int[] var12 = this.xzDirectionsConst[var3++ % 4];

                for (int var13 = 0; var13 < var10; ++var13)
                {
                    var7 += var12[0];
                    var8 += var12[1];
                    var9 = this.getOrCreateChunkWatcher(var5 + var7, var6 + var8, true).chunkLocation;

                    if (var2.contains(var9))
                    {
                        p_72691_1_.loadedChunks.add(var9);
                    }
                }
            }
        }

        var3 %= 4;

        for (var10 = 0; var10 < var4 * 2; ++var10)
        {
            var7 += this.xzDirectionsConst[var3][0];
            var8 += this.xzDirectionsConst[var3][1];
            var9 = this.getOrCreateChunkWatcher(var5 + var7, var6 + var8, true).chunkLocation;

            if (var2.contains(var9))
            {
                p_72691_1_.loadedChunks.add(var9);
            }
        }
    }

    /**
     * Removes an EntityPlayerMP from the PlayerManager.
     */
    public void removePlayer(EntityPlayerMP p_72695_1_)
    {
        int var2 = (int)this.managedPosX >> 4;
        int var3 = (int)this.managedPosZ >> 4;

        for (int var4 = var2 - this.playerViewRadius; var4 <= var2 + this.playerViewRadius; ++var4)
        {
            for (int var5 = var3 - this.playerViewRadius; var5 <= var3 + this.playerViewRadius; ++var5)
            {
                PlayerChunkLoadManager.PlayerInstance var6 = this.getOrCreateChunkWatcher(var4, var5, false);

                if (var6 != null)
                {
                    var6.removePlayer(p_72695_1_);
                }
            }
        }

        this.player = null;
    }

    /**
     * Determine if two rectangles centered at the given points overlap for the provided radius. Arguments: x1, z1, x2,
     * z2, radius.
     */
    private boolean overlaps(int x1, int z1, int x2, int z2, int r)
    {
        int deltaX = x1 - x2;
        int deltaZ = z1 - z2;
        return (deltaX >= -r && deltaX <= r) && (deltaZ >= -r && deltaZ <= r);
    }

    /**
     * update chunks around a player being moved by server logic (e.g. cart, boat)
     */
    public void updateMountedMovingPlayer(double x, double y, double z)
    {
    	this.player.posX = x;
    	this.player.posY = y;
    	this.player.posZ = z;
    	
        int playerChunkX = (int)this.player.posX >> 4;
        int playerChunkZ = (int)this.player.posZ >> 4;
        double playerDeltaX = this.managedPosX - this.player.posX;
        double playerDeltaZ = this.managedPosZ - this.player.posZ;
        double playerDistance = playerDeltaX * playerDeltaX + playerDeltaZ * playerDeltaZ;

        if (playerDistance >= 64.0D)
        {
            int managedChunkX = (int)this.managedPosX >> 4;
            int managedChunkZ = (int)this.managedPosZ >> 4;
            int radius = this.playerViewRadius;
            int chunkDeltaX = playerChunkX - managedChunkX;
            int chunkDeltaZ = playerChunkZ - managedChunkZ;

            if (chunkDeltaX != 0 || chunkDeltaZ != 0)
            {
                for (int chunkX = playerChunkX - radius; chunkX <= playerChunkX + radius; ++chunkX)
                {
                    for (int chunkZ = playerChunkZ - radius; chunkZ <= playerChunkZ + radius; ++chunkZ)
                    {
                        if (!this.overlaps(chunkX, chunkZ, managedChunkX, managedChunkZ, radius))
                        {
                            this.getOrCreateChunkWatcher(chunkX, chunkZ, true).addPlayer(this.player);
                        }

                        if (!this.overlaps(chunkX - chunkDeltaX, chunkZ - chunkDeltaZ, playerChunkX, playerChunkZ, radius))
                        {
                            PlayerChunkLoadManager.PlayerInstance chunkWatcher = this.getOrCreateChunkWatcher(chunkX - chunkDeltaX, chunkZ - chunkDeltaZ, false);

                            if (chunkWatcher != null)
                            {
                                chunkWatcher.removePlayer(this.player);
                            }
                        }
                    }
                }

                this.filterChunkLoadQueue(this.player);
                this.managedPosX = this.player.posX;
                this.managedPosZ = this.player.posZ;
            }
        }
    }

    private class PlayerInstance
    {
        private EntityPlayerMP playerWatchingChunk = null;
        private final ChunkCoordIntPair chunkLocation;
        private short[] tilesToUpdate = new short[64];
        private int numberOfTilesToUpdate;
        private int flagsYAreasToUpdate;

        private PlayerInstance(int chunkX, int chunkZ)
        {
            this.chunkLocation = new ChunkCoordIntPair(chunkX, chunkZ);
            PlayerChunkLoadManager.this.getWorldServer().loadChunk(chunkX, chunkZ);
        }

        private void addPlayer(EntityPlayerMP player)
        {
            if (this.playerWatchingChunk == null)
            {
                this.playerWatchingChunk = player;
                player.loadedChunks.add(this.chunkLocation);
            }
        }

        private void removePlayer(EntityPlayerMP player)
        {
            if (this.playerWatchingChunk != null)
            {
                Chunk<EntityPlayerMP> chunk = PlayerChunkLoadManager.this.theWorldServer.provideChunk(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos);

                if (chunk.getLoaded())
                {
                    Minecraft.getMinecraft().clientHandler.handleChunkData(new S21PacketChunkData(chunk, true, 0));
                }

                this.playerWatchingChunk = null;
                player.loadedChunks.remove(this.chunkLocation);

                long key = (long)this.chunkLocation.chunkXPos + 2147483647L | (long)this.chunkLocation.chunkZPos + 2147483647L << 32;
                PlayerChunkLoadManager.this.playerInstances.remove(key);
                PlayerChunkLoadManager.this.playerInstanceList.remove(this);

                if (this.numberOfTilesToUpdate > 0)
                {
                    PlayerChunkLoadManager.this.chunkWatcherWithPlayers.remove(this);
                }

                PlayerChunkLoadManager.this.getWorldServer().unloadChunksIfNotNearSpawn(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos);
            }
        }

        private void markBlockForUpdate(int localX, int localY, int localZ)
        {
            if (this.numberOfTilesToUpdate == 0)
            {
                PlayerChunkLoadManager.this.chunkWatcherWithPlayers.add(this);
            }

            this.flagsYAreasToUpdate |= 1 << (localY >> 4);

            if (this.numberOfTilesToUpdate < 64)
            {
                short localKey = (short)(localX << 12 | localZ << 8 | localY);

                for (int i = 0; i < this.numberOfTilesToUpdate; ++i)
                {
                    if (this.tilesToUpdate[i] == localKey)
                    {
                        return;
                    }
                }

                this.tilesToUpdate[this.numberOfTilesToUpdate++] = localKey;
            }
        }

        private void sendChunkUpdate()
        {
            if (this.numberOfTilesToUpdate != 0)
            {
            	if(!this.playerWatchingChunk.loadedChunks.contains(this.chunkLocation)){
            		if (this.numberOfTilesToUpdate == 1)
            		{
            			int x = this.chunkLocation.chunkXPos * 16 + (this.tilesToUpdate[0] >> 12 & 15);
            			int y = this.tilesToUpdate[0] & 255;
            			int z = this.chunkLocation.chunkZPos * 16 + (this.tilesToUpdate[0] >> 8 & 15);
            			Minecraft.getMinecraft().clientHandler.handleBlockChange(x, y, z, PlayerChunkLoadManager.this.theWorldServer);
            		}
            		else if (this.numberOfTilesToUpdate == 64)
            		{
            			Minecraft.getMinecraft().clientHandler.handleChunkData(new S21PacketChunkData(PlayerChunkLoadManager.this.theWorldServer.provideChunk(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos), false, this.flagsYAreasToUpdate));
            		}
            		else
            		{
            			Minecraft.getMinecraft().clientHandler.handleMultiBlockChange(this.numberOfTilesToUpdate, this.tilesToUpdate, PlayerChunkLoadManager.this.theWorldServer.provideChunk(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos));
            		}
            	}
                

                this.numberOfTilesToUpdate = 0;
                this.flagsYAreasToUpdate = 0;
            }
        }
    }
}
