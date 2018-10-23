package net.minecraft.world.chunk.storage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RegionFileCache
{
    /** A map containing Files as keys and RegionFiles as values */
    private static final Map<File, RegionFile> regionsByFilename = new HashMap<File, RegionFile>();

    public static synchronized RegionFile createOrLoadRegionFile(File chunkSaveLocation, int chunkX, int chunkZ)
    {
        File regionDir = new File(chunkSaveLocation, "region");
        File regionFileName = new File(regionDir, "r." + (chunkX >> 5) + "." + (chunkZ >> 5) + ".mca");
        RegionFile regionFile = regionsByFilename.get(regionFileName);

        if (regionFile != null)
        {
            return regionFile;
        }
        else
        {
            if (!regionDir.exists())
            {
                regionDir.mkdirs();
            }

            if (regionsByFilename.size() >= 256)
            {
                clearRegionFileReferences();
            }

            RegionFile newRegionFile = new RegionFile(regionFileName);
            regionsByFilename.put(regionFileName, newRegionFile);
            return newRegionFile;
        }
    }

    /**
     * Saves the current Chunk Map Cache
     */
    public static synchronized void clearRegionFileReferences()
    {
        Iterator<RegionFile> regionFileIterator = regionsByFilename.values().iterator();

        while (regionFileIterator.hasNext())
        {
            RegionFile regionFile = regionFileIterator.next();

            try
            {
                if (regionFile != null)
                {
                    regionFile.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        regionsByFilename.clear();
    }
}
