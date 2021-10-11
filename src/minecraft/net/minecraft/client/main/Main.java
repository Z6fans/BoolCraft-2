package net.minecraft.client.main;

import java.io.File;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.client.Minecraft;

public class Main
{
    public static void main(String[] args)
    {
        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        ArgumentAcceptingOptionSpec<File> argGameDir = parser.accepts("gameDir").withRequiredArg().ofType(File.class).defaultsTo(new File("."), new File[0]);
        ArgumentAcceptingOptionSpec<Integer> argWidth = parser.accepts("width").withRequiredArg().ofType(Integer.class).defaultsTo(Integer.valueOf(854), new Integer[0]);
        ArgumentAcceptingOptionSpec<Integer> argHeight = parser.accepts("height").withRequiredArg().ofType(Integer.class).defaultsTo(Integer.valueOf(480), new Integer[0]);
        OptionSet optionSet = parser.parse(args);
        int width = optionSet.valueOf(argWidth).intValue();
        int height = optionSet.valueOf(argHeight).intValue();
        File savesDir = new File(optionSet.valueOf(argGameDir), "saves");
        savesDir.mkdirs();
        Minecraft mc = new Minecraft(width, height, savesDir);

        Runtime.getRuntime().addShutdownHook(new Thread("Client Shutdown Thread")
        {
            public void run()
            {
                mc.saveAllWorlds();
            }
        });

        Thread.currentThread().setName("Client thread");
        mc.run();
    }
}
