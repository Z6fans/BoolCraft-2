package net.minecraft.world.chunk.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.storage.SaveHandler;

public class AnvilSaveConverter
{
	/**
     * Reference to the File object representing the directory for the world saves
     */
    private final File savesDirectory;

    public AnvilSaveConverter(File p_i2144_1_)
    {
    	if (!p_i2144_1_.exists())
        {
    		p_i2144_1_.mkdirs();
        }

        this.savesDirectory = p_i2144_1_;
    }

    public List<String> getSaveList()
    {
        ArrayList<String> var1 = new ArrayList<String>();
        if (this.savesDirectory != null && this.savesDirectory.isDirectory())
        {
            File[] var2 = this.savesDirectory.listFiles();
            File[] var3 = var2;
            int var4 = var2.length;

            for (int var5 = 0; var5 < var4; ++var5)
            {
                File var6 = var3[var5];

                if (var6.isDirectory())
                {
                    var1.add(var6.getName());
                }
            }
        }

        return var1;
    }

    public void flushCache()
    {
        RegionFileCache.clearRegionFileReferences();
    }

    /**
     * Returns back a loader for the specified save directory
     */
    public SaveHandler getSaveLoader(String name, boolean hasPlayerData)
    {
        return new SaveHandler(this.savesDirectory, name, hasPlayerData);
    }
    
    /**
     * Return whether the given world can be loaded.
     */
    public boolean canLoadWorld(String p_90033_1_)
    {
        File var2 = new File(this.savesDirectory, p_90033_1_);
        return var2.isDirectory();
    }
}
