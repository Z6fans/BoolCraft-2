package net.minecraft.client.main;

import java.io.File;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.client.Minecraft;

public class Main
{
    private static final String __OBFID = "CL_00001461";

    public static void main(String[] args)
    {
        System.setProperty("java.net.preferIPv4Stack", "true");
        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        ArgumentAcceptingOptionSpec argGameDir = parser.accepts("gameDir").withRequiredArg().ofType(File.class).defaultsTo(new File("."), new File[0]);
        ArgumentAcceptingOptionSpec argWidth = parser.accepts("width").withRequiredArg().ofType(Integer.class).defaultsTo(Integer.valueOf(854), new Integer[0]);
        ArgumentAcceptingOptionSpec argHeight = parser.accepts("height").withRequiredArg().ofType(Integer.class).defaultsTo(Integer.valueOf(480), new Integer[0]);
        OptionSet optionSet = parser.parse(args);
        int width = ((Integer)optionSet.valueOf(argWidth)).intValue();
        int height = ((Integer)optionSet.valueOf(argHeight)).intValue();
        File gameDir = (File)optionSet.valueOf(argGameDir);
        Minecraft mc = new Minecraft(width, height, gameDir);

        Runtime.getRuntime().addShutdownHook(new Thread("Client Shutdown Thread")
        {
            private static final String __OBFID = "CL_00001835";
            public void run()
            {
                Minecraft.stopIntegratedServer();
            }
        });

        Thread.currentThread().setName("Client thread");
        mc.run();
    }
}
