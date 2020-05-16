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

    public AnvilSaveConverter(File file)
    {
    	if (!file.exists())
        {
    		file.mkdirs();
        }

        this.savesDirectory = file;
    }

    public List<String> getSaveList()
    {
        final ArrayList<String> saveList = new ArrayList<String>();
        if (this.savesDirectory != null && this.savesDirectory.isDirectory())
        {
            final File[] fileList = this.savesDirectory.listFiles();
            final int length = fileList.length;

            for (int i = 0; i < length; ++i)
            {
                File file = fileList[i];

                if (file.isDirectory())
                {
                    saveList.add(file.getName());
                }
            }
        }

        return saveList;
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
    public boolean canLoadWorld(String name)
    {
        return new File(this.savesDirectory, name).isDirectory();
    }
}
