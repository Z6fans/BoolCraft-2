package net.minecraft.client.main;

import java.io.File;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import net.minecraft.client.Minecraft;

public class Main
{
    public static void main(String[] args)
    {
        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        ArgumentAcceptingOptionSpec<File> argSaveDir = parser.accepts("saveDir").withRequiredArg().ofType(File.class).defaultsTo(new File("./saves"), new File[0]);
        Minecraft mc = new Minecraft();

        Runtime.getRuntime().addShutdownHook(new Thread("Client Shutdown Thread")
        {
            public void run()
            {
                mc.saveAllWorlds();
            }
        });

        Thread.currentThread().setName("Client thread");
        mc.run(parser.parse(args).valueOf(argSaveDir));
    }
}
