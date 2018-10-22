package net.minecraft.world.chunk.storage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AnvilSaveConverter
{
	/**
     * Reference to the File object representing the directory for the world saves
     */
    private final File savesDirectory;
    private static final Logger logger = LogManager.getLogger();
    private static final String __OBFID = "CL_00000582";

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
