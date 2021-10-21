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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderGlobal;

public class World {
	private final Set<NextTickListEntry> pendingEntries = new HashSet<NextTickListEntry>();
	private final List<NextTickListEntry> pendingEntriesThisTick = new ArrayList<NextTickListEntry>();

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
	private final int r = 16;

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
	}

	/**
	 * Runs a single tick for the world
	 */
	public void tick(int chunkX, int chunkZ) {
		this.chunksToUnload.stream().map(this.loadedChunkHashMap::remove).filter(c -> c != null && c.isModified).forEach(this::safeSaveChunk);

		this.chunksToUnload.clear();

		this.totalTime = this.totalTime + 1L;

		this.pendingEntriesThisTick.addAll(this.pendingEntries.stream().filter(e -> e.t <= this.totalTime).collect(Collectors.toList()));
		this.pendingEntries.removeAll(this.pendingEntriesThisTick);

		for (NextTickListEntry e : this.pendingEntriesThisTick) {
			if (e.y >= 0 && e.y < 256 && this.loadedChunkHashMap.containsKey(chunkXZ2Long(e.x >> 4, e.z >> 4))) {
				this.getBlock(e.x, e.y, e.z).updateTick(this, e.x, e.y, e.z);
			} else {
				this.scheduleBlockUpdate(e.x, e.y, e.z, 0);
			}
		}
		
		this.pendingEntriesThisTick.clear();
	}

	/**
	 * Schedules a tick to a block with a delay (Most commonly the tick rate)
	 */
	public void scheduleBlockUpdate(int x, int y, int z, long delay) {
		this.pendingEntries.add(new NextTickListEntry(x, y, z, delay + this.totalTime));
	}

	public void saveAllChunks(int cx, int cz) {
		for (Chunk c : this.loadedChunkHashMap.values()) {
			if (c.isModified) {
				this.safeSaveChunk(c);
			}

			if (c.x - cx < -this.r || c.x - cx > this.r
			 || c.z - cz < -this.r || c.z - cz > this.r) {
				if (c.x < -8 || c.x > 8 || c.z < -8 || c.z > 8) // keep spawn loaded
				{
					this.chunksToUnload.add(Long.valueOf(chunkXZ2Long(c.x, c.z)));
				}
			}
		}
	}

	public void notifyBlocksOfNeighborChange(int x, int y, int z) {
		this.getBlock(x - 1, y, z).onNeighborBlockChange(this, x - 1, y, z);
		this.getBlock(x + 1, y, z).onNeighborBlockChange(this, x + 1, y, z);
		this.getBlock(x, y - 1, z).onNeighborBlockChange(this, x, y - 1, z);
		this.getBlock(x, y + 1, z).onNeighborBlockChange(this, x, y + 1, z);
		this.getBlock(x, y, z - 1).onNeighborBlockChange(this, x, y, z - 1);
		this.getBlock(x, y, z + 1).onNeighborBlockChange(this, x, y, z + 1);
	}

	/**
	 * loads or generates the chunk at the chunk location specified
	 */
	private Chunk loadChunk(int x, int z) {
		long posHash = chunkXZ2Long(x, z);
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
			DataOutputStream stream = new DataOutputStream(new FileOutputStream(new File(this.worldDirectory, "c." + chunk.x + "." + chunk.z + ".mca")));
			byte[] byteArray = chunk.getBlockStorageArray();
			stream.writeInt(byteArray.length);
			stream.write(byteArray);

			int xmin = (chunk.x << 4) - 2;
			int xmax = xmin + 16 + 2;
			int zmin = (chunk.z << 4) - 2;
			int zmax = zmin + 16 + 2;

			List<NextTickListEntry> entries = Stream
					.concat(this.pendingEntries.stream(), this.pendingEntriesThisTick.stream())
					.filter(entry -> entry.x >= xmin && entry.x < xmax && entry.z >= zmin && entry.z < zmax)
					.collect(Collectors.toList());

			stream.writeInt(entries.size());

			for (NextTickListEntry entry : entries) {
				stream.writeInt(entry.x);
				stream.writeInt(entry.y);
				stream.writeInt(entry.z);
				stream.writeLong(entry.t - this.totalTime);
			}

			stream.close();
			
			chunk.isModified = false;
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
				power = Math.max(power, this.getBlock(x + xOff[i], y + yOff[i], z + zOff[i])
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

			this.render.markChunksForUpdate((x - 1) >> 4, (y - 1) >> 4, (z - 1) >> 4, (x + 1) >> 4, (y + 1) >> 4, (z + 1) >> 4);

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
		return y >= 0 && y <= 255 ? this.loadChunk(x >> 4, z >> 4).getBlocMeta(x & 15, y & 255, z & 15) : 0;
	}
	
	private static long chunkXZ2Long(int cx, int cz)
    {
        return (long)cx & 4294967295L | ((long)cz & 4294967295L) << 32;
    }
}