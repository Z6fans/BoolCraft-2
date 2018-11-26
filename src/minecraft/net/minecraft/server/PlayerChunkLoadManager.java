package net.minecraft.server;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
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
    private final LongHashMap<ChunkUpdateTracker> playerInstances = new LongHashMap<ChunkUpdateTracker>();

    /**
     * contains a PlayerInstance for every chunk they can see. the "player instance" cotains a list of all players who
     * can also that chunk
     */
    private final List<ChunkUpdateTracker> chunkWatcherWithPlayers = new ArrayList<ChunkUpdateTracker>();

    /** This field is using when chunk should be processed (every 8000 ticks) */
    private final List<ChunkUpdateTracker> playerInstanceList = new ArrayList<ChunkUpdateTracker>();

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

    /**
     * updates all the player instances that need to be updated
     */
    public void updatePlayerInstances()
    {
        long worldTime = this.theWorldServer.getTotalWorldTime();

        if (worldTime - this.previousTotalWorldTime > 8000L)
        {
            this.previousTotalWorldTime = worldTime;

            for (int var3 = 0; var3 < this.playerInstanceList.size(); ++var3)
            {
            	PlayerChunkLoadManager.ChunkUpdateTracker var4 = this.playerInstanceList.get(var3);
                var4.sendChunkUpdate();
            }
        }
        else
        {
            for (int var3 = 0; var3 < this.chunkWatcherWithPlayers.size(); ++var3)
            {
            	PlayerChunkLoadManager.ChunkUpdateTracker var4 = this.chunkWatcherWithPlayers.get(var3);
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

    private PlayerChunkLoadManager.ChunkUpdateTracker getOrCreateChunkWatcher(int chunkX, int chunkZ, boolean doCreate)
    {
        long key = (long)chunkX + 2147483647L | (long)chunkZ + 2147483647L << 32;
        PlayerChunkLoadManager.ChunkUpdateTracker chunkWatcher = (PlayerChunkLoadManager.ChunkUpdateTracker)this.playerInstances.getValueByKey(key);

        if (chunkWatcher == null && doCreate)
        {
            chunkWatcher = new PlayerChunkLoadManager.ChunkUpdateTracker(chunkX, chunkZ);
            this.playerInstances.add(key, chunkWatcher);
            this.playerInstanceList.add(chunkWatcher);
        }

        return chunkWatcher;
    }

    public void markBlockForUpdate(int x, int y, int z)
    {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        PlayerChunkLoadManager.ChunkUpdateTracker playerInstance = this.getOrCreateChunkWatcher(chunkX, chunkZ, false);

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
        this.player = player;
        int chunkX = (int)this.player.posX >> 4;
        int chunkZ = (int)this.player.posZ >> 4;
        this.managedPosX = this.player.posX;
        this.managedPosZ = this.player.posZ;

        for (int x = chunkX - this.playerViewRadius; x <= chunkX + this.playerViewRadius; ++x)
        {
            for (int y = chunkZ - this.playerViewRadius; y <= chunkZ + this.playerViewRadius; ++y)
            {
                PlayerChunkLoadManager.this.player.loadedChunks.add(new ChunkCoordIntPair(x, y));
            }
        }
        
        this.filterChunkLoadQueue();
    }

    /**
     * Removes all chunks from the given player's chunk load queue that are not in viewing range of the player.
     */
    private void filterChunkLoadQueue()
    {
        ArrayList<ChunkCoordIntPair> var2 = new ArrayList<ChunkCoordIntPair>(this.player.loadedChunks);
        int var3 = 0;
        int r = this.playerViewRadius;
        int chunkX = (int)this.player.posX >> 4;
        int chunkZ = (int)this.player.posZ >> 4;
        int var7 = 0;
        int var8 = 0;
        ChunkCoordIntPair var9 = this.getOrCreateChunkWatcher(chunkX, chunkZ, true).chunkLocation;
        this.player.loadedChunks.clear();

        if (var2.contains(var9))
        {
        	this.player.loadedChunks.add(var9);
        }

        for (int var10 = 1; var10 <= r * 2; ++var10)
        {
            for (int var11 = 0; var11 < 2; ++var11)
            {
                int[] var12 = this.xzDirectionsConst[var3++ % 4];

                for (int var13 = 0; var13 < var10; ++var13)
                {
                    var7 += var12[0];
                    var8 += var12[1];
                    var9 = this.getOrCreateChunkWatcher(chunkX + var7, chunkZ + var8, true).chunkLocation;

                    if (var2.contains(var9))
                    {
                    	this.player.loadedChunks.add(var9);
                    }
                }
            }
        }

        var3 %= 4;

        for (int var10 = 0; var10 < r * 2; ++var10)
        {
            var7 += this.xzDirectionsConst[var3][0];
            var8 += this.xzDirectionsConst[var3][1];
            var9 = this.getOrCreateChunkWatcher(chunkX + var7, chunkZ + var8, true).chunkLocation;

            if (var2.contains(var9))
            {
            	this.player.loadedChunks.add(var9);
            }
        }
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
    public void updateMountedMovingPlayer(double x, double z)
    {
    	this.player.posX = x;
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
                            PlayerChunkLoadManager.this.player.loadedChunks.add(new ChunkCoordIntPair(chunkX, chunkZ));
                        }
                    }
                }

                this.filterChunkLoadQueue();
                this.managedPosX = this.player.posX;
                this.managedPosZ = this.player.posZ;
            }
        }
    }

    private class ChunkUpdateTracker
    {
        private final ChunkCoordIntPair chunkLocation;
        private short[] tilesToUpdate = new short[64];
        private int numberOfTilesToUpdate;

        private ChunkUpdateTracker(int chunkX, int chunkZ)
        {
            this.chunkLocation = new ChunkCoordIntPair(chunkX, chunkZ);
            PlayerChunkLoadManager.this.theWorldServer.loadChunk(chunkX, chunkZ);
        }

        private void markBlockForUpdate(int localX, int localY, int localZ)
        {
            if (this.numberOfTilesToUpdate == 0)
            {
                PlayerChunkLoadManager.this.chunkWatcherWithPlayers.add(this);
            }

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
        	if (!PlayerChunkLoadManager.this.player.loadedChunks.contains(this.chunkLocation))
        	{
        		Chunk chunk = PlayerChunkLoadManager.this.theWorldServer.provideChunk(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos);
        		int baseX = chunk.xPosition * 16;
                int baseZ = chunk.zPosition * 16;

                for (int i = 0; i < this.numberOfTilesToUpdate; ++i)
                {
                    short localKey = this.tilesToUpdate[i];
                    int localX = localKey >> 12 & 15;
                    int localZ = localKey >> 8 & 15;
                    int localY = localKey & 255;
                    Minecraft.getMinecraft().worldClient.setBlock(localX + baseX, localY, localZ + baseZ, chunk.getBlock(localX, localY, localZ), chunk.getBlockMetadata(localX, localY, localZ));
                }
        	}

            this.numberOfTilesToUpdate = 0;
        }
    }
}
