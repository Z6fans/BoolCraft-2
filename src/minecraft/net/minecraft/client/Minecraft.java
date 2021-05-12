package net.minecraft.client;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Timer;
import net.minecraft.world.SessionLockException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.ThreadedFileIOBase;

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
    public int displayWidth;
    public int displayHeight;
    private final Timer timer = new Timer();
    public WorldClient worldClient;
    private RenderGlobal renderGlobal;
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
    private EntityRenderer entityRenderer;

    /** Mouse left click counter */
    private int leftClickCounter;

    /** The ray trace hit that the mouse is over. */
    private MovingObjectPosition objectMouseOver;

    /** Mouse helper instance. */
    private final File mcDataDir;

    /**
     * When you place a block, it's set to 6, decremented once per tick, when it's 0, you can place another block.
     */
    private int rightClickDelayTimer;

    /**
     * Does the actual gameplay have focus. If so then mouse and keys will effect the player instead of menus.
     */
    private boolean inGameHasFocus;
    private long systemTime = getSystemTime();
    private long lastSystemTime = -1L;

    /**
     * Set to true to keep the game loop running. Set to false by shutdown() to allow the game loop to exit cleanly.
     */
    private volatile boolean running = true;
    
    /**
     * An array of 36 item stacks indicating the main player inventory (including the visible bar).
     */
    private final Block[] mainInventory = {Block.stone, Block.redstone_wire, Block.lever, Block.redstone_torch};
    
    //server section

    private boolean isServerPaused;
    
    /** The server world instance. */
    private WorldServer worldServer;

    /**
     * Indicates whether the server is running or not. Set to false to initiate a shutdown.
     */
    private boolean serverRunning = false;

    /** Incremented every tick. */
    private int tickCounter;

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
    public int currentItem;
    
    /**
     * Reference to the File object representing the directory for the world saves
     */
    private File savesDirectory;

    public Minecraft(int displayWidth, int displayHeight, File mcDataDir)
    {
        this.mcDataDir = mcDataDir;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        ImageIO.setUseCache(false);
    }

    /**
     * Wrapper around displayCrashReportInternal
     */
    private void displayCrashReport(Throwable cause, String description)
    {
    	System.out.println("---- Boolcraft Crash Report ----");
    	System.out.println("Description: " + description);
    	System.out.println();
        cause.printStackTrace(System.out);
        System.exit(-1);
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
        this.currentScreen.setWorldAndResolution(this, this.getScaledWidth(), this.getScaledHeight());
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
            p_147108_1_.setWorldAndResolution(this, this.getScaledWidth(), this.getScaledHeight());
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
            catch (LWJGLException e)
            {
                logger.error("Couldn\'t set pixel format", e);

                try
                {
                    Thread.sleep(1000L);
                }
                catch (InterruptedException ee){;}

                Display.create();
            }

            OpenGlHelper.initializeTextures();

            File saves = new File(this.mcDataDir, "saves");
            if (!saves.exists())
            {
        		saves.mkdirs();
            }
            this.savesDirectory = saves;
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, this.getScaledWidth(), this.getScaledHeight(), 0.0D, 1000.0D, 3000.0D);
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
            this.entityRenderer = new EntityRenderer(this, this.renderGlobal);
            GL11.glViewport(0, 0, this.displayWidth, this.displayHeight);
            this.checkGLError("Post startup");
            this.displayGuiScreen();
        }
        catch (Throwable e)
        {
            this.displayCrashReport(e, "Initializing game");
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

                        this.computeMouseOver(1.0F);

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
                                throw new RuntimeException("Updating screen events", t);
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
                                        
                                        this.currentItem -= mouseScroll;
                                        
                                        while (this.currentItem < 0)
                                        {
                                        	this.currentItem += 4;
                                        }

                                        while (this.currentItem >= 4)
                                        {
                                            this.currentItem -= 4;
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
                                        throw new RuntimeException("Manually triggered debug crash", new Throwable());
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
                                    else if (Keyboard.getEventKey() == 1)
                                    {
                                    	this.displayInGameMenu();
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
                                if (this.renderViewEntity == null)
                                {
                                    this.renderViewEntity = this.thePlayer;
                                }
                                
                                if (this.thePlayer != null)
                                {
                                	try
                                    {
                                		this.thePlayer.onUpdate();
                                    }
                                    catch (Throwable t)
                                    {
                                        throw new RuntimeException("Ticking entity", t);
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
                    this.entityRenderer.updateCameraAndRender(this.timer.renderPartialTicks);
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
                                throw new RuntimeException("Exception ticking world", t);
                            }

                            try
                            {
                            	this.worldServer.updateEntities();
                            }
                            catch (Throwable t)
                            {
                                throw new RuntimeException("Exception ticking world entities", t);
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
        catch (Throwable t)
        {
            this.freeMemory();
            logger.fatal("Exception thrown!", t);
            this.displayCrashReport(t, "Unexpected error");
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
                    this.currentScreen.setWorldAndResolution(this, this.getScaledWidth(), this.getScaledHeight());
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
        catch (Throwable t){;}

        try
        {
            System.gc();
        }
        catch (Throwable t){;}

        try
        {
            System.gc();
            this.loadWorldNull();
        }
        catch (Throwable t){;}

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
            Mouse.setGrabbed(true);
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
            Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
            Mouse.setGrabbed(false);
        }
    }
    
    public boolean getInGameHasFocus()
    {
    	return this.inGameHasFocus;
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
        	Block block = this.mainInventory[this.currentItem];
        	
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
        	
        	if (!this.worldServer.getBlock(x, y, z).onBlockActivatedServer(this.worldServer, x, y, z) && y < 256 && (y < 255 || side != 1) && this.worldServer.canPlaceEntity(block, xPrime, yPrime, zPrime))
            {
        		this.worldServer.setBlock(xPrime, yPrime, zPrime, block, block.onBlockPlaced(this.worldServer, xPrime, yPrime, zPrime, side));
            }
        }
    }

    /**
     * Arguments: World name
     */
    public void launchIntegratedServer(String name)
    {
        this.loadWorldNull();
        System.gc();

        try
        {
            this.serverRunning = true;
        	logger.info("Starting integrated minecraft server version 1.7.10");
            this.worldServer = new WorldServer(this, new File(this.savesDirectory, name));
            logger.info("Preparing start region ");

            for (int x = -192; x <= 192 && this.serverRunning; x += 16)
            {
                for (int z = -192; z <= 192 && this.serverRunning; z += 16)
                {
                    this.worldServer.loadChunk(x >> 4, z >> 4);
                }
            }
            
            prevTime = System.currentTimeMillis();
            tickTimer = 0L;
            
            this.worldClient = new WorldClient();
            this.renderViewEntity = null;

            if (this.renderGlobal != null)
            {
                this.renderGlobal.setWorldAndLoadRenderers(this.worldClient);
            }

            this.thePlayer = new EntityPlayer(this.worldClient, this, this.worldServer);
            this.renderViewEntity = this.thePlayer;

            System.gc();
            this.systemTime = 0L;
            
            this.worldServer.spawnPlayerInWorld(this);
        }
        catch (Throwable t)
        {
            throw new RuntimeException("Starting integrated server", t);
        }

        this.displayGuiScreenNull();
    }
    
    private void loadWorldNull()
    {
    	this.serverRunning = false;
        this.stopServer();
        this.renderViewEntity = null;
        this.worldClient = null;
        ThreadedFileIOBase.threadedIOInstance.clearRegionFileReferences();
        this.thePlayer = null;
        System.gc();
        this.systemTime = 0L;
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
    public void stopServer()
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
            catch (SessionLockException e)
            {
                logger.warn(e.getMessage());
            }
        }
    }

    /**
     * Finds what block or object the mouse is over at the specified partial tick time. Args: partialTickTime
     */
    public void computeMouseOver(float partialTickTime)
    {
        if (this.renderViewEntity != null)
        {
            if (this.worldClient != null)
            {
                this.objectMouseOver = this.renderViewEntity.rayTrace8(partialTickTime);
            }
        }
    }
    
    public MovingObjectPosition getMouseOver()
    {
    	return this.objectMouseOver;
    }
    
    public int getScaledWidth()
    {
    	int scaleFactor = 1;

        while (scaleFactor < 1000 && this.displayWidth / (scaleFactor + 1) >= 320 && this.displayHeight / (scaleFactor + 1) >= 240)
        {
            ++scaleFactor;
        }

        if (scaleFactor % 2 != 0 && scaleFactor != 1)
        {
            --scaleFactor;
        }
        
        return (int)Math.ceil((double)this.displayWidth / (double)scaleFactor);
    }

    public int getScaledHeight()
    {
    	int scaleFactor = 1;

        while (scaleFactor < 1000 && this.displayWidth / (scaleFactor + 1) >= 320 && this.displayHeight / (scaleFactor + 1) >= 240)
        {
            ++scaleFactor;
        }

        if (scaleFactor % 2 != 0 && scaleFactor != 1)
        {
            --scaleFactor;
        }
        
        return (int)Math.ceil((double)this.displayHeight / (double)scaleFactor);
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
     * Return whether the given world can be loaded.
     */
    public boolean canLoadWorld(String name)
    {
        return new File(this.savesDirectory, name).isDirectory();
    }
}