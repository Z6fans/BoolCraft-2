package net.minecraft.world;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.MathHelper;

public class World {
	private final HashSet<NextTickListEntry> pendingTickListEntriesHashSet = new HashSet<NextTickListEntry>();

	/** All work to do in future ticks. */
	private final TreeSet<NextTickListEntry> pendingTickListEntriesTreeSet = new TreeSet<NextTickListEntry>();
	private final List<NextTickListEntry> pendingTickListEntriesThisTick = new ArrayList<NextTickListEntry>();

	/** The directory in which to save world data. */
	private final File worldDirectory;

	/**
	 * used by unload100OldestChunks to iterate the loadedChunkHashMap for unload
	 * (underlying assumption, first in, first out)
	 */
	private final Set<Long> chunksToUnload = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
	private final Map<Long, Chunk> loadedChunkHashMap = new HashMap<Long, Chunk>();

	/** Total time for this world. */
	private long totalTime = 0;

	private double playerPosX;
	private double playerPosZ;

	/** player X position as seen by PlayerManager */
	private double prevPosX;

	/** player Z position as seen by PlayerManager */
	private double prevPosZ;

	/**
	 * A map of chunk position (two ints concatenated into a long) to PlayerInstance
	 */
	private final Set<Long> playerInstances = new HashSet<Long>();

	/**
	 * Number of chunks the server sends to the client. Valid 3<=x<=15. In
	 * server.properties.
	 */
	private final int playerViewRadius = 10;

	/** time what is using to check if InhabitedTime should be calculated */
	private long previousTotalWorldTime;

	private final RenderGlobal render;

	private boolean isSpawned = false;

	private final byte[] blankChunkStorage;

	public World(RenderGlobal rg, File wd) {
		this.render = rg;
		this.worldDirectory = wd;
		this.worldDirectory.mkdirs();

		this.blankChunkStorage = new byte[0x10000];

		for (int y = 0; y < 5; ++y) {
			for (int x = 0; x < 16; ++x) {
				for (int z = 0; z < 16; ++z) {
					this.blankChunkStorage[y << 8 | z << 4 | x] = 1;
				}
			}
		}

		System.out.println("Preparing start region");

		for (int x = -12; x <= 12; x++) {
			for (int z = -12; z <= 12; z++) {
				this.loadChunk(x, z);
			}
		}
	}

	/**
	 * Runs a single tick for the world
	 */
	public void tick() {
		for (Long hash : this.chunksToUnload) {
			Chunk chunk = this.loadedChunkHashMap.get(hash);

			if (chunk != null && chunk.isModified) {
				this.safeSaveChunk(chunk);
				chunk.isModified = false;
			}

			this.loadedChunkHashMap.remove(hash);
		}

		this.chunksToUnload.clear();

		this.totalTime = this.totalTime + 1L;

		int numEntries = this.pendingTickListEntriesTreeSet.size();

		if (numEntries != this.pendingTickListEntriesHashSet.size()) {
			throw new IllegalStateException("TickNextTick list out of synch");
		} else {
			if (numEntries > 1000) {
				numEntries = 1000;
			}

			for (int i = 0; i < numEntries; ++i) {
				NextTickListEntry entry = this.pendingTickListEntriesTreeSet.first();

				if (entry.scheduledTime > this.totalTime) {
					break;
				}

				this.pendingTickListEntriesTreeSet.remove(entry);
				this.pendingTickListEntriesHashSet.remove(entry);
				this.pendingTickListEntriesThisTick.add(entry);
			}

			Iterator<NextTickListEntry> entryListIterator = this.pendingTickListEntriesThisTick.iterator();

			while (entryListIterator.hasNext()) {
				NextTickListEntry entry = entryListIterator.next();
				entryListIterator.remove();
				byte var5 = 0;

				if (this.checkChunksExist(entry.xCoord - var5, entry.yCoord - var5, entry.zCoord - var5,
						entry.xCoord + var5, entry.yCoord + var5, entry.zCoord + var5)) {
					try {
						this.getBlock(entry.xCoord, entry.yCoord, entry.zCoord).updateTick(this, entry.xCoord,
								entry.yCoord, entry.zCoord);
					} catch (Throwable t) {
						throw new RuntimeException("Exception while ticking a block", t);
					}
				} else {
					this.scheduleBlockUpdate(entry.xCoord, entry.yCoord, entry.zCoord, 0);
				}
			}
			this.pendingTickListEntriesThisTick.clear();
		}

		if (this.isSpawned) {
			int chunkX = MathHelper.floor_double(this.playerPosX / 16.0D);
			int chunkZ = MathHelper.floor_double(this.playerPosZ / 16.0D);

			for (int xOff = -16; xOff <= 16; ++xOff) {
				for (int zOff = -16; zOff <= 16; ++zOff) {
					this.loadChunk(xOff + chunkX, zOff + chunkZ);
				}
			}
		}

		if (this.totalTime - this.previousTotalWorldTime > 8000L) {
			this.previousTotalWorldTime = this.totalTime;
		}
	}

	/**
	 * Schedules a tick to a block with a delay (Most commonly the tick rate)
	 */
	public void scheduleBlockUpdate(int x, int y, int z, long delay) {
		NextTickListEntry entry = new NextTickListEntry(x, y, z, delay + this.totalTime);

		if (!this.pendingTickListEntriesHashSet.contains(entry)) {
			this.pendingTickListEntriesHashSet.add(entry);
			this.pendingTickListEntriesTreeSet.add(entry);
		}
	}

	/**
	 * Checks between a min and max all the chunks inbetween actually exist. Args:
	 * minX, minY, minZ, maxX, maxY, maxZ
	 */
	private final boolean checkChunksExist(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		if (maxY >= 0 && minY < 256) {
			minX >>= 4;
			minZ >>= 4;
			maxX >>= 4;
			maxZ >>= 4;

			for (int x = minX; x <= maxX; ++x) {
				for (int z = minZ; z <= maxZ; ++z) {
					if (!this.chunkExists(x, z)) {
						return false;
					}
				}
			}

			return true;
		} else {
			return false;
		}
	}

	private List<NextTickListEntry> getPendingBlockUpdates(Chunk chunk) {
		int xmin = (chunk.xPosition << 4) - 2;
		int xmax = xmin + 16 + 2;
		int zmin = (chunk.zPosition << 4) - 2;
		int zmax = zmin + 16 + 2;

		return Stream.concat(this.pendingTickListEntriesTreeSet.stream(), this.pendingTickListEntriesThisTick.stream())
				.filter(entry -> entry.xCoord >= xmin && entry.xCoord < xmax && entry.zCoord >= zmin
						&& entry.zCoord < zmax)
				.collect(Collectors.toList());
	}

	public void saveAllChunks() {
		for (Chunk chunk : this.loadedChunkHashMap.values()) {
			if (chunk.isModified) {
				this.safeSaveChunk(chunk);
				chunk.isModified = false;
			}

			if (!this.playerInstances
					.contains((long) chunk.xPosition + 2147483647L | (long) chunk.zPosition + 2147483647L << 32)) {
				if (chunk.xPosition < -8 || chunk.xPosition > 8 || chunk.zPosition < -8 || chunk.zPosition > 8) // keep
																												// spawn
																												// loaded
				{
					this.chunksToUnload
							.add(Long.valueOf(ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition)));
				}
			}
		}
	}

	private void notifyBlockOfNeighborChange(int x, int y, int z) {
		try {
			this.getBlock(x, y, z).onNeighborBlockChange(this, x, y, z);
		} catch (Throwable t) {
			throw new RuntimeException("Exception while updating neighbors", t);
		}
	}

	public void spawnPlayerInWorld(Minecraft mc) {
		this.isSpawned = true;
		this.prevPosX = 0;
		this.prevPosZ = 0;

		this.filterChunkLoadQueue();
		this.loadChunk(0, 0);
	}

	public void notifyBlocksOfNeighborChange(int x, int y, int z) {
		this.notifyBlockOfNeighborChange(x - 1, y, z);
		this.notifyBlockOfNeighborChange(x + 1, y, z);
		this.notifyBlockOfNeighborChange(x, y - 1, z);
		this.notifyBlockOfNeighborChange(x, y + 1, z);
		this.notifyBlockOfNeighborChange(x, y, z - 1);
		this.notifyBlockOfNeighborChange(x, y, z + 1);
	}

	/**
	 * Checks to see if a chunk exists at x, y
	 */
	private boolean chunkExists(int p_73149_1_, int p_73149_2_) {
		return this.loadedChunkHashMap.containsKey(ChunkCoordIntPair.chunkXZ2Int(p_73149_1_, p_73149_2_));
	}

	/**
	 * loads or generates the chunk at the chunk location specified
	 */
	private Chunk loadChunk(int x, int z) {
		long posHash = ChunkCoordIntPair.chunkXZ2Int(x, z);
		this.chunksToUnload.remove(Long.valueOf(posHash));
		Chunk chunk = this.loadedChunkHashMap.get(posHash);

		if (chunk == null) {
			chunk = new Chunk(x, z);

			File chunkFile = new File(this.worldDirectory, "c." + x + "." + z + ".mca");

			if (chunkFile.exists()) {
				try {
					DataInputStream stream = new DataInputStream(new FileInputStream(chunkFile));
					byte[] byteArray = new byte[stream.readInt()];
					stream.readFully(byteArray);
					chunk.setStorageArrays(byteArray);

					int length = stream.readInt();

					for (int i = 0; i < length; ++i) {
						int entryX = stream.readInt();
						int entryY = stream.readInt();
						int entryZ = stream.readInt();
						long entryT = stream.readLong();

						this.scheduleBlockUpdate(entryX, entryY, entryZ, entryT);
					}

					stream.close();
				} catch (Exception e) {
					System.out.println("Couldn\'t load chunk");
					e.printStackTrace(System.out);
				}
			} else {
				chunk.setStorageArrays(this.blankChunkStorage.clone());
			}

			this.loadedChunkHashMap.put(posHash, chunk);
		}

		return chunk;
	}

	/**
	 * used by saveChunks, but catches any exceptions if the save fails.
	 */
	private void safeSaveChunk(Chunk chunk) {
		try {
			DataOutputStream stream = new DataOutputStream(new FileOutputStream(
					new File(this.worldDirectory, "c." + chunk.xPosition + "." + chunk.zPosition + ".mca")));
			byte[] byteArray = chunk.getBlockStorageArray();
			stream.writeInt(byteArray.length);
			stream.write(byteArray);

			List<NextTickListEntry> tickTags = this.getPendingBlockUpdates(chunk);

			stream.writeInt(tickTags.size());

			for (NextTickListEntry entry : tickTags) {
				stream.writeInt(entry.xCoord);
				stream.writeInt(entry.yCoord);
				stream.writeInt(entry.zCoord);
				stream.writeLong(entry.scheduledTime - this.getTotalWorldTime());
			}

			stream.close();
		} catch (Exception e) {
			System.out.println("Couldn\'t save chunk");
			e.printStackTrace(System.out);
		}
	}

	/**
	 * Is this block powering in the specified direction Args: x, y, z, direction
	 */
	private int isBlockProvidingPowerTo(int x, int y, int z, int side) {
		return this.getBlock(x, y, z).isProvidingStrongPower(this, x, y, z, side);
	}

	/**
	 * Returns the highest redstone signal strength powering the given block. Args:
	 * X, Y, Z.
	 */
	private int getBlockPowerInput(int x, int y, int z) {
		int power = Math.max(0, this.isBlockProvidingPowerTo(x, y - 1, z, 0));
		power = Math.max(power, this.isBlockProvidingPowerTo(x, y + 1, z, 1));
		power = Math.max(power, this.isBlockProvidingPowerTo(x, y, z - 1, 2));
		power = Math.max(power, this.isBlockProvidingPowerTo(x, y, z + 1, 3));
		power = Math.max(power, this.isBlockProvidingPowerTo(x - 1, y, z, 4));
		power = Math.max(power, this.isBlockProvidingPowerTo(x + 1, y, z, 5));
		return power;
	}

	/**
	 * Returns the indirect signal strength being outputted by the given block in
	 * the *opposite* of the given direction. Args: X, Y, Z, direction
	 */
	public boolean getIndirectPowerOutput(int x, int y, int z, int side) {
		return this.getIndirectPowerLevelTo(x, y, z, side) > 0;
	}

	/**
	 * Gets the power level from a certain block face. Args: x, y, z, direction
	 */
	private int getIndirectPowerLevelTo(int x, int y, int z, int side) {
		return this.isSolid(x, y, z) ? this.getBlockPowerInput(x, y, z)
				: this.getBlock(x, y, z).isProvidingWeakPower(this, x, y, z, side);
	}

	public int getStrongestIndirectPower(int x, int y, int z) {
		int max = 0;

		int[] offsetsXForSide = new int[] { 0, 0, 0, 0, -1, 1 };
		int[] offsetsYForSide = new int[] { -1, 1, 0, 0, 0, 0 };
		int[] offsetsZForSide = new int[] { 0, 0, -1, 1, 0, 0 };

		for (int side = 0; side < 6; ++side) {
			int power = this.getIndirectPowerLevelTo(x + offsetsXForSide[side], y + offsetsYForSide[side],
					z + offsetsZForSide[side], side);

			if (power >= 15) {
				return 15;
			}

			if (power > max) {
				max = power;
			}
		}

		return max;
	}

	public long getTotalWorldTime() {
		return this.totalTime;
	}

	/**
	 * Sets the block ID and metadata at a given location. Args: X, Y, Z, new block
	 * ID, new metadata, flags. Flag 1 will cause a block update. Flag 2 will send
	 * the change to clients (you almost always want this). Flag 4 prevents the
	 * block from being re-rendered, if this is a client world. Flags can be added
	 * together.
	 */
	public boolean setBlockAndMeta(int x, int y, int z, int newBlockID, int newMeta) {
		int oldbm = this.getBlocMeta(x, y, z);
		int newbm = ((newMeta & 0xF) << 4) | (newBlockID & 0xF);

		if (oldbm != newbm) {
			this.loadChunk(x >> 4, z >> 4).setBlocMeta(x & 15, y & 255, z & 15, newbm);

			if ((oldbm & 0xF) != (newbm & 0xF)) {
				Block.getBlockById(oldbm & 0xF).onBlockBreak(this, x, y, z, oldbm >> 4);
				Block.getBlockById(newbm & 0xF).onBlockAdded(this, x, y, z);
			}

			this.markBlockForUpdate(x, y, z);

			this.notifyBlocksOfNeighborChange(x, y, z);
			return true;
		}

		return false;
	}

	public Block getBlock(int x, int y, int z) {
		return Block.getBlockById(this.getBlocMeta(x, y, z) & 0xF);
	}

	/**
	 * Returns the block metadata at coords x,y,z
	 */
	public int getBlockMetadata(int x, int y, int z) {
		return this.getBlocMeta(x, y, z) >> 4;
	}

	private void markBlockForUpdate(int x, int y, int z) {
		this.render.markChunksForUpdate((x - 1) >> 4, (y - 1) >> 4, (z - 1) >> 4, (x + 1) >> 4, (y + 1) >> 4,
				(z + 1) >> 4);
	}

	/**
	 * Removes all chunks from the given player's chunk load queue that are not in
	 * viewing range of the player.
	 */
	private void filterChunkLoadQueue() {
		int chunkX = (int) this.playerPosX >> 4;
		int chunkZ = (int) this.playerPosZ >> 4;

		for (int x = chunkX - this.playerViewRadius; x <= chunkX + this.playerViewRadius; x++) {
			for (int z = chunkZ - this.playerViewRadius; z <= chunkZ + this.playerViewRadius; z++) {
				long key = (long) x + 2147483647L | (long) z + 2147483647L << 32;

				if (!this.playerInstances.contains(key)) {
					this.loadChunk(x, z);
					this.playerInstances.add(key);
				}
			}
		}
	}

	/**
	 * update chunks around a player being moved by server logic (e.g. cart, boat)
	 */
	public void updateMountedMovingPlayer(double x, double z) {
		this.playerPosX = x;
		this.playerPosZ = z;

		int playerChunkX = (int) this.playerPosX >> 4;
		int playerChunkZ = (int) this.playerPosZ >> 4;
		double playerDeltaX = this.prevPosX - this.playerPosX;
		double playerDeltaZ = this.prevPosZ - this.playerPosZ;

		if (playerDeltaX * playerDeltaX + playerDeltaZ * playerDeltaZ >= 64.0D) {
			int managedChunkX = (int) this.prevPosX >> 4;
			int managedChunkZ = (int) this.prevPosZ >> 4;
			int chunkDeltaX = playerChunkX - managedChunkX;
			int chunkDeltaZ = playerChunkZ - managedChunkZ;

			if (chunkDeltaX != 0 || chunkDeltaZ != 0) {
				this.filterChunkLoadQueue();
				this.prevPosX = this.playerPosX;
				this.prevPosZ = this.playerPosZ;
			}
		}
	}

	public boolean isSolid(int x, int y, int z) {
		return (this.getBlocMeta(x, y, z) & 0xF) == 1;
	}

	public boolean isAir(int x, int y, int z) {
		return (this.getBlocMeta(x, y, z) & 0xF) == 0;
	}

	public boolean isWire(int x, int y, int z) {
		return (this.getBlocMeta(x, y, z) & 0xF) == 2;
	}

	public boolean canProvidePower(int x, int y, int z) {
		int id = this.getBlocMeta(x, y, z) & 0xF;
		return id == 2 || id == 3 || id == 4;
	}

	public int getBlocMeta(int x, int y, int z) {
		return this.loadChunk(x >> 4, z >> 4).getBlocMeta(x & 15, y & 255, z & 15);
	}
}