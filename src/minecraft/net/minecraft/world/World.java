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
import net.minecraft.client.renderer.RenderGlobal;

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

	/**
	 * Number of chunks the server sends to the client. Valid 3<=x<=15. In
	 * server.properties.
	 */
	private final int viewRadius = 16;

	private final RenderGlobal render;

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
	public void tick(int chunkX, int chunkZ) {
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

		for (int xOff = -this.viewRadius; xOff <= this.viewRadius; ++xOff) {
			for (int zOff = -this.viewRadius; zOff <= this.viewRadius; ++zOff) {
				this.loadChunk(xOff + chunkX, zOff + chunkZ);
			}
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

	public void saveAllChunks(int chunkX, int chunkZ) {
		for (Chunk chunk : this.loadedChunkHashMap.values()) {
			if (chunk.isModified) {
				this.safeSaveChunk(chunk);
				chunk.isModified = false;
			}

			if (chunk.xPosition - chunkX < -this.viewRadius || chunk.xPosition - chunkX > this.viewRadius
			 || chunk.zPosition - chunkZ < -this.viewRadius || chunk.zPosition - chunkZ > this.viewRadius) {
				if (chunk.xPosition < -8 || chunk.xPosition > 8 || chunk.zPosition < -8 || chunk.zPosition > 8) // keep spawn loaded
				{
					this.chunksToUnload.add(Long.valueOf(chunkXZ2Int(chunk.xPosition, chunk.zPosition)));
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
		return this.loadedChunkHashMap.containsKey(chunkXZ2Int(p_73149_1_, p_73149_2_));
	}

	/**
	 * loads or generates the chunk at the chunk location specified
	 */
	private Chunk loadChunk(int x, int z) {
		long posHash = chunkXZ2Int(x, z);
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

			int xmin = (chunk.xPosition << 4) - 2;
			int xmax = xmin + 16 + 2;
			int zmin = (chunk.zPosition << 4) - 2;
			int zmax = zmin + 16 + 2;

			List<NextTickListEntry> tickTags = Stream
					.concat(this.pendingTickListEntriesTreeSet.stream(), this.pendingTickListEntriesThisTick.stream())
					.filter(entry -> entry.xCoord >= xmin && entry.xCoord < xmax && entry.zCoord >= zmin
							&& entry.zCoord < zmax)
					.collect(Collectors.toList());

			stream.writeInt(tickTags.size());

			for (NextTickListEntry entry : tickTags) {
				stream.writeInt(entry.xCoord);
				stream.writeInt(entry.yCoord);
				stream.writeInt(entry.zCoord);
				stream.writeLong(entry.scheduledTime - this.totalTime);
			}

			stream.close();
		} catch (Exception e) {
			System.out.println("Couldn\'t save chunk");
			e.printStackTrace(System.out);
		}
	}

	/**
	 * Gets the power level from a certain block face. Args: x, y, z, direction
	 */
	public int getIndirectPowerLevelTo(int x, int y, int z, int side) {
		if (this.isSolid(x, y, z)) {
			int power = 0;
			int[] xOff = {0, 0, 0, 0, -1, 1};
			int[] yOff = {-1, 1, 0, 0, 0, 0};
			int[] zOff = {0, 0, -1, 1, 0, 0};
			for (int i = 0; i < 6; i++) {
				power = Math.max(power,
						this.getBlock(x + xOff[i], y + yOff[i], z + zOff[i])
						    .isProvidingStrongPower(this, x + xOff[i], y + yOff[i], z + zOff[i], i));
			}
			return power;
		} else {
			return this.getBlock(x, y, z).isProvidingWeakPower(this, x, y, z, side);
		}
	}

	/**
	 * Sets the block ID and metadata at a given location. Args: X, Y, Z, new block
	 * ID, new metadata.
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
	
	private static long chunkXZ2Int(int p_77272_0_, int p_77272_1_)
    {
        return (long)p_77272_0_ & 4294967295L | ((long)p_77272_1_ & 4294967295L) << 32;
    }
}