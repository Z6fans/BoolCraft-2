package net.minecraft.client;

import java.io.File;

import javax.imageio.ImageIO;

import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ScaledResolution;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.util.KeyBinding;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MouseHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Timer;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.AnvilSaveConverter;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.glu.GLU;

public class Minecraft
{
    private static final Logger logger = LogManager.getLogger();

    /**
     * Set to 'this' in Minecraft constructor; used by some settings get methods
     */
    private static Minecraft theMinecraft;
    public int displayWidth;
    public int displayHeight;
    private Timer timer = new Timer();
    public WorldClient worldClient;
    public RenderGlobal renderGlobal;
    public EntityPlayer thePlayer;

    /**
     * The Entity from which the renderer determines the render viewpoint. Currently is always the parent Minecraft
     * class's 'thePlayer' instance. Modification of its location, rotation, or other settings at render time will
     * modify the camera likewise, with the caveat of triggering chunk rebuilds as it moves, making it unsuitable for
     * changing the viewpoint mid-render.
     */
    public EntityPlayer renderViewEntity;
    private boolean isGamePaused;

    /** The GuiScreen that's being displayed at the moment. */
    public GuiScreen currentScreen;
    public EntityRenderer entityRenderer;

    /** Mouse left click counter */
    private int leftClickCounter;

    /** Skip render world */
    public boolean skipRenderWorld;

    /** The ray trace hit that the mouse is over. */
    public MovingObjectPosition objectMouseOver;

    /** Mouse helper instance. */
    public MouseHelper mouseHelper;
    private final File mcDataDir;
    private AnvilSaveConverter saveLoader;

    /**
     * When you place a block, it's set to 6, decremented once per tick, when it's 0, you can place another block.
     */
    private int rightClickDelayTimer;

    /**
     * Does the actual gameplay have focus. If so then mouse and keys will effect the player instead of menus.
     */
    public boolean inGameHasFocus;
    private long systemTime = getSystemTime();
    private long lastSystemTime = -1L;

    /**
     * Set to true to keep the game loop running. Set to false by shutdown() to allow the game loop to exit cleanly.
     */
    private volatile boolean running = true;
    
    /**
     * An array of 36 item stacks indicating the main player inventory (including the visible bar).
     */
    private Block[] mainInventory = {Block.stone, Block.redstone_wire, Block.lever, Block.redstone_torch};
    
    //server section

    private boolean isServerPaused;
    
    /** The server world instance. */
    public WorldServer worldServer;

    /**
     * Indicates whether the server is running or not. Set to false to initiate a shutdown.
     */
    private boolean serverRunning = false;

    /** Incremented every tick. */
    private int tickCounter;
    private String folderName;

    /**
     * Set when warned for "Can't keep up", which triggers again after 15 seconds.
     */
    private long timeOfLastWarning;
    private long prevTime;
    private long tickTimer;
    
    /**
     * Delays the first damage on the block after the first click on the block
     */
    private int blockHitDelay;

    public Minecraft(int displayWidth, int displayHeight, File mcDataDir)
    {
        theMinecraft = this;
        this.mcDataDir = mcDataDir;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        ImageIO.setUseCache(false);
    }

    /**
     * Wrapper around displayCrashReportInternal
     */
    private void displayCrashReport(CrashReport cr)
    {
        System.out.println(cr.getCompleteReport());
        System.exit(-1);
    }

    /**
     * Returns the save loader that is currently being used
     */
    public AnvilSaveConverter getSaveLoader()
    {
        return this.saveLoader;
    }

    /**
     * Sets the argument GuiScreen as the main (topmost visible) screen.
     */
    private void displayGuiScreen()
    {
    	if (this.currentScreen != null)
        {
            this.currentScreen.onGuiClosed();
        }

        this.currentScreen = new GuiScreen();
        this.setIngameNotInFocus();
        ScaledResolution var2 = new ScaledResolution(this.displayWidth, this.displayHeight);
        int var3 = var2.getScaledWidth();
        int var4 = var2.getScaledHeight();
        this.currentScreen.setWorldAndResolution(this, var3, var4);
        this.skipRenderWorld = false;
    }
    
    /**
     * Sets the argument GuiScreen as the main (topmost visible) screen.
     */
    public void displayGuiScreenNull()
    {
    	GuiScreen p_147108_1_ = null;
    	
        if (this.currentScreen != null)
        {
            this.currentScreen.onGuiClosed();
        }

        if (this.worldClient == null)
        {
            p_147108_1_ = new GuiScreen();
        }

        this.currentScreen = p_147108_1_;

        if (p_147108_1_ != null)
        {
            this.setIngameNotInFocus();
            ScaledResolution var2 = new ScaledResolution(this.displayWidth, this.displayHeight);
            int var3 = var2.getScaledWidth();
            int var4 = var2.getScaledHeight();
            p_147108_1_.setWorldAndResolution(this, var3, var4);
            this.skipRenderWorld = false;
        }
        else
        {
            this.setIngameFocus();
        }
    }

    /**
     * Checks for an OpenGL error. If there is one, prints the error ID and error string.
     */
    private void checkGLError(String p_71361_1_)
    {
        int var2 = GL11.glGetError();

        if (var2 != 0)
        {
            String var3 = GLU.gluErrorString(var2);
            logger.error("########## GL ERROR ##########");
            logger.error("@ " + p_71361_1_);
            logger.error(var2 + ": " + var3);
        }
    }

    public void run()
    {
        this.running = true;

        try
        {
        	Display.setDisplayMode(new DisplayMode(this.displayWidth, this.displayHeight));

            Display.setResizable(true);
            Display.setTitle("Boolcraft");
            logger.info("LWJGL Version: " + Sys.getVersion());

            try
            {
                Display.create((new PixelFormat()).withDepthBits(24));
            }
            catch (LWJGLException var7)
            {
                logger.error("Couldn\'t set pixel format", var7);

                try
                {
                    Thread.sleep(1000L);
                }
                catch (InterruptedException var6)
                {
                    ;
                }

                Display.create();
            }

            OpenGlHelper.initializeTextures();

            this.saveLoader = new AnvilSaveConverter(new File(this.mcDataDir, "saves"));
            ScaledResolution sr = new ScaledResolution(this.displayWidth, this.displayHeight);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, sr.getScaledWidth(), sr.getScaledHeight(), 0.0D, 1000.0D, 3000.0D);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glFlush();
            this.updateDisplaySize();
            this.entityRenderer = new EntityRenderer(this);
            this.mouseHelper = new MouseHelper();
            this.checkGLError("Pre startup");
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glShadeModel(GL11.GL_SMOOTH);
            GL11.glClearDepth(1.0D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glCullFace(GL11.GL_BACK);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            this.checkGLError("Startup");
            this.renderGlobal = new RenderGlobal(this);
            GL11.glViewport(0, 0, this.displayWidth, this.displayHeight);
            this.checkGLError("Post startup");
            this.displayGuiScreen();
        }
        catch (Throwable var11)
        {
            this.displayCrashReport(CrashReport.makeCrashReport(var11, "Initializing game"));
            return;
        }

        try
        {
            while (this.running)
            {
            	try
                {
                	if (Display.isCreated() && Display.isCloseRequested())
                    {
                        this.shutdown();
                    }

                    if (this.isGamePaused && this.worldClient != null)
                    {
                        float tmp = this.timer.renderPartialTicks;
                        this.timer.updateTimer();
                        this.timer.renderPartialTicks = tmp;
                    }
                    else
                    {
                        this.timer.updateTimer();
                    }

                    for (int tick = 0; tick < this.timer.elapsedTicks; ++tick)
                    {
                    	if (this.rightClickDelayTimer > 0)
                        {
                            --this.rightClickDelayTimer;
                        }

                        this.entityRenderer.getMouseOver(1.0F);

                        if (this.currentScreen != null)
                        {
                            this.leftClickCounter = 10000;
                        }

                        if (this.currentScreen != null)
                        {
                            try
                            {
                                this.currentScreen.handleInput();
                            }
                            catch (Throwable t)
                            {
                                throw new ReportedException(CrashReport.makeCrashReport(t, "Updating screen events"));
                            }
                        }

                        if (this.currentScreen == null)
                        {
                            while (Mouse.next())
                            {
                                int mouseButton = Mouse.getEventButton() - 100;
                                KeyBinding.setKeyBindState(mouseButton, Mouse.getEventButtonState());

                                if (Mouse.getEventButtonState())
                                {
                                    KeyBinding.onTick(mouseButton);
                                }

                                if (getSystemTime() - this.systemTime <= 200L)
                                {
                                    int mouseScroll = Mouse.getEventDWheel();

                                    if (mouseScroll != 0)
                                    {
                                        if (mouseScroll > 0)
                                        {
                                            mouseScroll = 1;
                                        }

                                        if (mouseScroll < 0)
                                        {
                                            mouseScroll = -1;
                                        }
                                        
                                        this.thePlayer.currentItem -= mouseScroll;
                                        
                                        while (this.thePlayer.currentItem < 0)
                                        {
                                        	this.thePlayer.currentItem += 4;
                                        }

                                        while (this.thePlayer.currentItem >= 4)
                                        {
                                            this.thePlayer.currentItem -= 4;
                                        }
                                    }

                                    if (this.currentScreen == null)
                                    {
                                        if (!this.inGameHasFocus && Mouse.getEventButtonState())
                                        {
                                            this.setIngameFocus();
                                        }
                                    }
                                }
                            }

                            if (this.leftClickCounter > 0)
                            {
                                --this.leftClickCounter;
                            }

                            while (Keyboard.next())
                            {
                                KeyBinding.setKeyBindState(Keyboard.getEventKey(), Keyboard.getEventKeyState());

                                if (Keyboard.getEventKeyState())
                                {
                                    KeyBinding.onTick(Keyboard.getEventKey());
                                }

                                if (this.lastSystemTime > 0L)
                                {
                                    if (getSystemTime() - this.lastSystemTime >= 6000L)
                                    {
                                        throw new ReportedException(new CrashReport("Manually triggered debug crash", new Throwable()));
                                    }

                                    if (!Keyboard.isKeyDown(46) || !Keyboard.isKeyDown(61))
                                    {
                                        this.lastSystemTime = -1L;
                                    }
                                }
                                else if (Keyboard.isKeyDown(46) && Keyboard.isKeyDown(61))
                                {
                                    this.lastSystemTime = getSystemTime();
                                }

                                if (Keyboard.getEventKeyState())
                                {
                                    if (this.currentScreen != null)
                                    {
                                        this.currentScreen.handleKeyboardInput();
                                    }
                                    else
                                    {
                                        if (Keyboard.getEventKey() == 1)
                                        {
                                            this.displayInGameMenu();
                                        }

                                        if (Keyboard.getEventKey() == 30 && Keyboard.isKeyDown(61))
                                        {
                                            this.renderGlobal.loadRenderers();
                                        }
                                    }
                                }
                            }

                            while (KeyBinding.keyBindAttack.isPressed())
                            {
                            	if (this.leftClickCounter <= 0 && this.objectMouseOver != null)
                                {
                            		int x = this.objectMouseOver.blockX;
                                    int y = this.objectMouseOver.blockY;
                                    int z = this.objectMouseOver.blockZ;

                                    if (!this.worldClient.getBlock(x, y, z).isReplaceable())
                                    {
                                        this.worldServer.setBlock(x, y, z, Block.air, 0);
                                        this.worldClient.setBlock(x, y, z, Block.air, 0);
                                        this.blockHitDelay = 5;
                                    }
                                }
                            }

                            while (KeyBinding.keyBindUseItem.isPressed())
                            {
                                this.playerRightClick();
                            }

                            if (KeyBinding.keyBindUseItem.getIsKeyPressed() && this.rightClickDelayTimer == 0)
                            {
                                this.playerRightClick();
                            }

                            boolean doAttack = this.currentScreen == null && KeyBinding.keyBindAttack.getIsKeyPressed() && this.inGameHasFocus;
                            
                            if (!doAttack)
                            {
                                this.leftClickCounter = 0;
                            }

                            if (this.leftClickCounter <= 0)
                            {
                                if (doAttack && this.objectMouseOver != null)
                                {
                                    int x = this.objectMouseOver.blockX;
                                    int y = this.objectMouseOver.blockY;
                                    int z = this.objectMouseOver.blockZ;

                                    if (!this.worldClient.getBlock(x, y, z).isReplaceable())
                                    {
                                        if (this.blockHitDelay > 0)
                                        {
                                            --this.blockHitDelay;
                                        }
                                        else
                                        {
                                        	this.worldServer.setBlock(x, y, z, Block.air, 0);
                                            this.worldClient.setBlock(x, y, z, Block.air, 0);
                                            this.blockHitDelay = 5;
                                        }
                                    }
                                }
                            }
                        }

                        if (this.worldClient != null)
                        {
                            if (!this.isGamePaused)
                            {
                                this.entityRenderer.updateRenderer();
                            }

                            if (!this.isGamePaused)
                            {
                                if (this.thePlayer != null)
                                {
                                	try
                                    {
                                		this.thePlayer.onUpdate();
                                    }
                                    catch (Throwable t)
                                    {
                                        throw new ReportedException(CrashReport.makeCrashReport(t, "Ticking entity"));
                                    }
                                }
                            }
                        }
                        
                        this.systemTime = getSystemTime();
                    }
                    
                    this.checkGLError("Pre render");
                    GL11.glPushMatrix();
                    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                    GL11.glDisable(GL11.GL_TEXTURE_2D);

                    if (!this.skipRenderWorld)
                    {
                        this.entityRenderer.updateCameraAndRender(this.timer.renderPartialTicks);
                    }

                    GL11.glFlush();
                    GL11.glPopMatrix();
                    GL11.glPushMatrix();
                    this.entityRenderer.setupOverlayRendering();
                    GL11.glPopMatrix();
                    this.updateDisplaySize();
                    Thread.yield();
                    this.checkGLError("Post render");
                    this.isGamePaused = this.serverRunning && this.currentScreen != null;
                }
                catch (OutOfMemoryError e)
                {
                    this.freeMemory();
                    System.gc();
                    this.shutdown();
                }
                
                if(this.serverRunning){
            		long currentTime = System.currentTimeMillis();
                    long deltaTime = currentTime - prevTime;

                    if (deltaTime > 2000L && prevTime - this.timeOfLastWarning >= 15000L)
                    {
                        logger.warn("Can\'t keep up! Did the system time change, or is the server overloaded? Running {}ms behind, skipping {} tick(s)", new Object[] {Long.valueOf(deltaTime), Long.valueOf(deltaTime / 50L)});
                        deltaTime = 2000L;
                        this.timeOfLastWarning = prevTime;
                    }

                    if (deltaTime < 0L)
                    {
                        logger.warn("Time ran backwards! Did the system time change?");
                        deltaTime = 0L;
                    }

                    tickTimer += deltaTime;
                    prevTime = currentTime;

                    while (tickTimer > 50L)
                    {
                        tickTimer -= 50L;
                        
                        boolean lastPaused = this.isServerPaused;
                        this.isServerPaused = this.isGamePaused;

                        if (!lastPaused && this.isServerPaused)
                        {
                            logger.info("Saving and pausing game...");
                            this.saveAllWorlds();
                        }

                        if (!this.isServerPaused)
                        {
                            ++this.tickCounter;

                            try
                            {
                            	this.worldServer.tick();
                            }
                            catch (Throwable t)
                            {
                                throw new ReportedException(CrashReport.makeCrashReport(t, "Exception ticking world"));
                            }

                            try
                            {
                            	this.worldServer.updateEntities();
                            }
                            catch (Throwable t)
                            {
                                throw new ReportedException(CrashReport.makeCrashReport(t, "Exception ticking world entities"));
                            }

                            if (this.tickCounter % 900 == 0)
                            {
                                this.saveAllWorlds();
                            }
                        }
                    }
            	}
            }
        }
        catch (ReportedException e)
        {
            this.freeMemory();
            logger.fatal("Reported exception thrown!", e);
            this.displayCrashReport(e.getCrashReport());
        }
        catch (Throwable t)
        {
            CrashReport crashReport = new CrashReport("Unexpected error", t);
            this.freeMemory();
            logger.fatal("Unreported exception thrown!", t);
            this.displayCrashReport(crashReport);
        }
        finally
        {
        	try
            {
                logger.info("Stopping!");

                try
                {
                    this.loadWorldNull();
                }
                catch (Throwable t){;}

                try
                {
                    GLAllocation.deleteTexturesAndDisplayLists();
                }
                catch (Throwable t){;}
            }
            finally
            {
                Display.destroy();
                System.exit(0);
            }

            System.gc();
        }
    }

    private void updateDisplaySize()
    {
        Display.update();

        if (Display.wasResized())
        {
            int var1 = this.displayWidth;
            int var2 = this.displayHeight;
            this.displayWidth = Display.getWidth();
            this.displayHeight = Display.getHeight();

            if (this.displayWidth != var1 || this.displayHeight != var2)
            {
                if (this.displayWidth <= 0)
                {
                    this.displayWidth = 1;
                }

                if (this.displayHeight <= 0)
                {
                    this.displayHeight = 1;
                }

                this.displayWidth = this.displayWidth <= 0 ? 1 : this.displayWidth;
                this.displayHeight = this.displayHeight <= 0 ? 1 : this.displayHeight;

                if (this.currentScreen != null)
                {
                    ScaledResolution var3 = new ScaledResolution(this.displayWidth, this.displayHeight);
                    int var4 = var3.getScaledWidth();
                    int var5 = var3.getScaledHeight();
                    this.currentScreen.setWorldAndResolution(this, var4, var5);
                }
            }
        }
    }

    private void freeMemory()
    {
        try
        {
            this.renderGlobal.deleteAllDisplayLists();
        }
        catch (Throwable var4)
        {
            ;
        }

        try
        {
            System.gc();
        }
        catch (Throwable var3)
        {
            ;
        }

        try
        {
            System.gc();
            this.loadWorldNull();
        }
        catch (Throwable var2)
        {
            ;
        }

        System.gc();
    }

    /**
     * Called when the window is closing. Sets 'running' to false which allows the game loop to exit cleanly.
     */
    public void shutdown()
    {
        this.running = false;
    }

    /**
     * Will set the focus to ingame if the Minecraft window is the active with focus. Also clears any GUI screen
     * currently displayed
     */
    private void setIngameFocus()
    {
    	if (Display.isActive() && !this.inGameHasFocus)
        {
            this.inGameHasFocus = true;
            this.mouseHelper.grabMouseCursor();
            this.displayGuiScreenNull();
            this.leftClickCounter = 10000;
        }
    }

    /**
     * Resets the player keystate, disables the ingame focus, and ungrabs the mouse cursor.
     */
    private void setIngameNotInFocus()
    {
        if (this.inGameHasFocus)
        {
            KeyBinding.unPressAllKeys();
            this.inGameHasFocus = false;
            this.mouseHelper.ungrabMouseCursor();
        }
    }

    /**
     * Displays the ingame menu
     */
    public void displayInGameMenu()
    {
        if (this.currentScreen == null)
        {
            this.loadWorldNull();
            this.displayGuiScreen();
        }
    }

	private void playerRightClick()
    {
        this.rightClickDelayTimer = 4;

        if (this.objectMouseOver != null)
        {
        	Block block = this.mainInventory[this.thePlayer.currentItem];
        	
        	int x = this.objectMouseOver.blockX;
            int y = this.objectMouseOver.blockY;
            int z = this.objectMouseOver.blockZ;
            int side = this.objectMouseOver.sideHit;
            
            int[] xOff = {0, 0, 0, 0, -1, 1};
        	int[] yOff = {-1, 1, 0, 0, 0, 0};
        	int[] zOff = {0, 0, -1, 1, 0, 0};
        	
        	int xPrime = x + xOff[side];
        	int yPrime = y + yOff[side];
        	int zPrime = z + zOff[side];
        	
        	if (!this.worldServer.getBlock(x, y, z).onBlockActivatedServer(this.worldServer, x, y, z) && y < 256 && (y < 255 || side != 1) && this.worldServer.canPlaceEntityOnSide(block, xPrime, yPrime, zPrime, side))
            {
        		this.worldServer.setBlock(xPrime, yPrime, zPrime, block, block.onBlockPlaced(this.worldServer, xPrime, yPrime, zPrime, side));
            }
        }
    }

    /**
     * Arguments: World foldername,  World ingame name, WorldSettings
     */
    public void launchIntegratedServer(String folder)
    {
        this.loadWorldNull();
        System.gc();
        SaveHandler sh = this.saveLoader.getSaveLoader(folder, false);
        WorldInfo wi = sh.loadWorldInfo();

        if (wi == null)
        {
            wi = new WorldInfo();
            sh.saveWorldInfo(wi);
        }

        try
        {
            this.folderName = folder;
            this.serverRunning = true;
        	logger.info("Starting integrated minecraft server version 1.7.10");
            this.worldServer = new WorldServer(this, (new AnvilSaveConverter(new File(this.mcDataDir, "saves"))).getSaveLoader(this.folderName, true));
            logger.info("Preparing start region ");

            for (int var11 = -192; var11 <= 192 && this.serverRunning; var11 += 16)
            {
                for (int var12 = -192; var12 <= 192 && this.serverRunning; var12 += 16)
                {
                    this.worldServer.loadChunk(var11 >> 4, var12 >> 4);
                }
            }
            
            prevTime = System.currentTimeMillis();
            tickTimer = 0L;
            
            this.worldClient = new WorldClient();

            if (this.renderGlobal != null)
            {
                this.renderGlobal.setWorldAndLoadRenderers(this.worldClient);
            }

            if (this.thePlayer == null)
            {
                this.thePlayer = new EntityPlayer(this.worldClient);
            }

            this.thePlayer.preparePlayerToSpawn();
            int chunkX = MathHelper.floor_double(this.thePlayer.posX / 16.0D);
        	int chunkY = MathHelper.floor_double(this.thePlayer.posY / 16.0D);
            int chunkZ = MathHelper.floor_double(this.thePlayer.posZ / 16.0D);

            if (chunkY < 0)
            {
                chunkY = 0;
            }
            
            this.thePlayer.addedToChunk = true;
            this.thePlayer.chunkCoordX = chunkX;
            this.thePlayer.chunkCoordY = chunkY;
            this.thePlayer.chunkCoordZ = chunkZ;
            this.renderViewEntity = this.thePlayer;

            System.gc();
            this.systemTime = 0L;
            
            this.worldServer.spawnPlayerInWorld(this);
        }
        catch (Throwable var10)
        {
            throw new ReportedException(CrashReport.makeCrashReport(var10, "Starting integrated server"));
        }

        this.displayGuiScreenNull();
    }
    
    private void loadWorldNull()
    {
    	this.serverRunning = false;
        this.stopServer();
        this.renderViewEntity = null;
        this.worldClient = null;
        this.saveLoader.flushCache();
        this.thePlayer = null;
        System.gc();
        this.systemTime = 0L;
    }

    /**
     * Return the singleton Minecraft instance for the game
     */
    public static Minecraft getMinecraft()
    {
        return theMinecraft;
    }

    public static void stopIntegratedServer()
    {
        if (theMinecraft != null)
        {
        	theMinecraft.stopServer();
        }
    }

    /**
     * Gets the system time in milliseconds.
     */
    public static long getSystemTime()
    {
        return Sys.getTime() * 1000L / Sys.getTimerResolution();
    }
    
    /**
     * Saves all necessary data as preparation for stopping the server.
     */
    private void stopServer()
    {
    	logger.info("Stopping server");

        if (this.worldServer != null)
        {
            logger.info("Saving worlds");
            this.saveAllWorlds();
            this.worldServer.flush();
        }
    }
    
    /**
     * par1 indicates if a log message should be output.
     */
    private void saveAllWorlds()
    {
    	if (this.worldServer != null)
        {
            try
            {
            	this.worldServer.saveAllChunks();
            }
            catch (MinecraftException e)
            {
                logger.warn(e.getMessage());
            }
        }
    }
}