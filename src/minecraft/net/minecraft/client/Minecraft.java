package net.minecraft.client;

import java.io.File;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Project;

public class Minecraft
{
    public int displayWidth;
    public int displayHeight;
    public RenderGlobal renderGlobal;
    private EntityPlayer thePlayer;

    /** The GuiScreen that's being displayed at the moment. */
    public GuiScreen currentScreen;

    /** The ray trace hit that the mouse is over. */
    private MovingObjectPosition objectMouseOver;

    /** Mouse helper instance. */
    private final File mcDataDir;

    /**
     * When you place a block, it's set to 6, decremented once per tick, when it's 0, you can place another block.
     */
    private int rightClickDelayTimer;
    private int leftClickDelayTimer;

    /**
     * Does the actual gameplay have focus. If so then mouse and keys will effect the player instead of menus.
     */
    private boolean inGameHasFocus;
    private long systemTime = getSystemTime();

    /**
     * Set to true to keep the game loop running. Set to false by shutdown() to allow the game loop to exit cleanly.
     */
    private volatile boolean running = true;
    
    //server section
    
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
    private int currentItem;
    
    /**
     * Reference to the File object representing the directory for the world saves
     */
    private File savesDirectory;

    /**
     * How much time has elapsed since the last tick, in ticks (range: 0.0 - 1.0).
     */
    private double renderPartialTicks;
    
    /**
     * The time reported by the high-resolution clock at the last call of updateTimer(), in seconds
     */
    private double lastHRTime;

    public Minecraft(int displayWidth, int displayHeight, File mcDataDir)
    {
        this.mcDataDir = mcDataDir;
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
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
     * Checks for an OpenGL error. If there is one, prints the error ID and error string.
     */
    private void checkGLError(String description)
    {
        int err = GL11.glGetError();

        if (err != 0)
        {
            String errstring = GLU.gluErrorString(err);
            System.out.println("########## GL ERROR ##########");
            System.out.println("@ " + description);
            System.out.println(err + ": " + errstring);
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
            System.out.println("LWJGL Version: " + Sys.getVersion());

            try
            {
                Display.create((new PixelFormat()).withDepthBits(24));
            }
            catch (LWJGLException e)
            {
            	System.out.println("Couldn\'t set pixel format");
            	e.printStackTrace(System.out);
                try {Thread.sleep(1000L);} catch (InterruptedException ee) {}
                Display.create();
            }

            this.savesDirectory = new File(this.mcDataDir, "saves");
            this.savesDirectory.mkdirs();
            //GL11 calls begin
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glFlush();
            this.renderGlobal = new RenderGlobal();
            this.checkGLError("Startup");
            this.currentScreen = new GuiScreen(this, this.displayWidth, this.displayHeight);
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
                    
                    double currentHRClockSecs = (double)System.nanoTime() / 1000000000.0D;

                    double diffHRClockSecs = currentHRClockSecs - this.lastHRTime;
                    this.lastHRTime = currentHRClockSecs;

                    if (diffHRClockSecs < 0.0D)
                    {
                        diffHRClockSecs = 0.0D;
                    }

                    if (diffHRClockSecs > 1.0D)
                    {
                        diffHRClockSecs = 1.0D;
                    }

                    this.renderPartialTicks = this.renderPartialTicks + diffHRClockSecs * 20;
                    int elapsedTicks = (int)this.renderPartialTicks;
                    this.renderPartialTicks -= (double)elapsedTicks;

                    if (elapsedTicks > 10)
                    {
                        elapsedTicks = 10;
                    }

                    for (int tick = 0; tick < elapsedTicks; ++tick)
                    {
                    	if (this.rightClickDelayTimer > 0)
                        {
                            --this.rightClickDelayTimer;
                        }
                    	
                    	if (this.leftClickDelayTimer > 0)
                        {
                            --this.leftClickDelayTimer;
                        }

                        this.computeMouseOver(1.0F);

                        if (this.currentScreen != null)
                        {
                            this.currentScreen.handleInput();
                        }
                        else
                        {
                            while (Mouse.next())
                            {
                            	if (!this.inGameHasFocus && Mouse.getEventButtonState())
                        		{
                        			this.setIngameFocus();
                        			break;
                        		}
                            	
                            	if (this.inGameHasFocus)
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

                                        this.currentItem = (this.currentItem + 4 - (mouseScroll > 0 ? 1 : mouseScroll < 0 ? -1 : 0)) % 4;
                                    }
                            	}
                            }

                            while (Keyboard.next() && this.inGameHasFocus)
                            {
                                KeyBinding.setKeyBindState(Keyboard.getEventKey(), Keyboard.getEventKeyState());

                                if (Keyboard.getEventKeyState())
                                {
                                    KeyBinding.onTick(Keyboard.getEventKey());
                                    
                                    if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE)
                                    {
                                        if (this.inGameHasFocus)
                                        {
                                            KeyBinding.unPressAllKeys();
                                            this.inGameHasFocus = false;
                                            Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
                                            Mouse.setGrabbed(false);
                                        }
                                        
                                        if (Keyboard.isKeyDown(Keyboard.KEY_FUNCTION))
                                        {
                                        	this.loadWorldNull();
                                            this.currentScreen = new GuiScreen(this, this.displayWidth, this.displayHeight);
                                        }
                                    }
                                }
                            }

                            while (KeyBinding.keyBindAttack.isPressed())
                            {
                            	this.playerLeftClick();
                            }

                            if (KeyBinding.keyBindAttack.getIsKeyPressed() && this.leftClickDelayTimer == 0)
                            {
                                this.playerLeftClick();
                            }

                            while (KeyBinding.keyBindUseItem.isPressed())
                            {
                                this.playerRightClick();
                            }
                            
                            if (KeyBinding.keyBindUseItem.getIsKeyPressed() && this.rightClickDelayTimer == 0)
                            {
                                this.playerRightClick();
                            }
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
                        
                        this.systemTime = getSystemTime();
                    }
                    
                    this.checkGLError("Pre render");
                    GL11.glPushMatrix();
                    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                    
                    if (this.currentScreen != null)
                    {
                    	GL11.glViewport(0, 0, this.displayWidth, this.displayHeight);
                        GL11.glEnable(GL11.GL_TEXTURE_2D);
                        GL11.glMatrixMode(GL11.GL_PROJECTION);
                        GL11.glLoadIdentity();
                        GL11.glOrtho(0.0D, this.displayWidth, this.displayHeight, 0.0D, 0.0D, 1.0D);
                        GL11.glMatrixMode(GL11.GL_MODELVIEW);
                        GL11.glLoadIdentity();
                        GL11.glEnable(GL11.GL_ALPHA_TEST);
                        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
                        GL11.glColor4f(1, 1, 1, 1);
                    	this.currentScreen.drawScreen(Mouse.getX(), this.displayHeight - Mouse.getY() - 1);
                    }
                    else if (this.thePlayer != null)
                    {
                        if (this.getInGameHasFocus() && Display.isActive())
                        {
                        	this.thePlayer.setAngles();
                        }
                        
                        GL11.glViewport(0, 0, this.displayWidth, this.displayHeight);
                        GL11.glDisable(GL11.GL_TEXTURE_2D);
                        GL11.glEnable(GL11.GL_CULL_FACE);
                        GL11.glMatrixMode(GL11.GL_PROJECTION);
                        GL11.glLoadIdentity();
                        Project.gluPerspective(110, (float)this.displayWidth / (float)this.displayHeight, 0.05F, 512.0F); //FOV
                        GL11.glMatrixMode(GL11.GL_MODELVIEW);
                        GL11.glLoadIdentity();
                        GL11.glRotatef((float)this.thePlayer.getPartialRotationPitch(this.renderPartialTicks), 1.0F, 0.0F, 0.0F);
                        GL11.glRotatef((float)this.thePlayer.getPartialRotationYaw(this.renderPartialTicks) + 180.0F, 0.0F, 1.0F, 0.0F);

                        Vec3 ppos = this.thePlayer.pttPos(this.renderPartialTicks);
                        this.renderGlobal.updateRenderers(this.thePlayer, ppos.x, ppos.y, ppos.z);
                        GL11.glDisable(GL11.GL_LIGHTING);
                        GL11.glDisable(GL11.GL_LIGHT0);
                        GL11.glDisable(GL11.GL_LIGHT1);
                        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
                        GL11.glMatrixMode(GL11.GL_MODELVIEW);
                        GL11.glPushMatrix();
                        this.renderGlobal.sortAndRender(this.thePlayer, this.renderPartialTicks);
                        GL11.glShadeModel(GL11.GL_FLAT);
                        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
                        GL11.glMatrixMode(GL11.GL_MODELVIEW);
                        GL11.glPopMatrix();
                        GL11.glDisable(GL11.GL_TEXTURE_2D);
                        GL11.glDepthMask(false);

                        this.computeMouseOver(this.renderPartialTicks);
                        
                        if (this.getMouseOver() != null)
                        {
                            MovingObjectPosition hit = this.getMouseOver();
                            GL11.glLineWidth(2.0F);
                            int meta = this.worldServer.getBlockMetadata(hit.x, hit.y, hit.z);
                            AxisAlignedBB aabb = this.worldServer.getBlock(hit.x, hit.y, hit.z).generateCubicBoundingBox(hit.x, hit.y, hit.z, meta).expand(0.002F).offset(-ppos.x, -ppos.y, -ppos.z);
                            Tessellator tess = Tessellator.instance;
                            tess.startDrawing(3);
                            tess.setColor_I(0xFF000000);
                            tess.addVertex(aabb.minX, aabb.minY, aabb.minZ);
                            tess.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
                            tess.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
                            tess.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
                            tess.addVertex(aabb.minX, aabb.minY, aabb.minZ);
                            tess.draw();
                            tess.startDrawing(3);
                            tess.setColor_I(0xFF000000);
                            tess.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
                            tess.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
                            tess.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
                            tess.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
                            tess.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
                            tess.draw();
                            tess.startDrawing(1);
                            tess.setColor_I(0xFF000000);
                            tess.addVertex(aabb.minX, aabb.minY, aabb.minZ);
                            tess.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
                            tess.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
                            tess.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
                            tess.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
                            tess.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
                            tess.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
                            tess.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
                            tess.draw();
                        }
                    	GL11.glEnable(GL11.GL_COLOR_MATERIAL);
                        GL11.glColorMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT);
                        GL11.glDepthMask(true);
                        GL11.glEnable(GL11.GL_CULL_FACE);
                        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
                        GL11.glEnable(GL11.GL_DEPTH_TEST);
                        GL11.glDepthFunc(GL11.GL_LEQUAL);
                        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
                        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
                        GL11.glMatrixMode(GL11.GL_PROJECTION);
                        GL11.glLoadIdentity();
                        GL11.glOrtho(0.0D, this.displayWidth, this.displayHeight, 0.0D, 0.0D, 1.0D);
                        GL11.glMatrixMode(GL11.GL_MODELVIEW);
                        GL11.glLoadIdentity();
                        int sWidth = this.displayWidth;
                        int sHeight = this.displayHeight;
                        drawRect(sWidth / 2 - 41 - 1 + currentItem * 20, sHeight - 22 - 1, sWidth / 2 - 41 - 1 + currentItem * 20 + 24, sHeight, 0x44CCCCCC);
                        drawRect(sWidth / 2 - 4, sHeight / 2 - 4, sWidth / 2 + 6, sHeight / 2 + 6, 0x44CCCCCC);
                        int[] colores = {0xFF505050, 0xFF39EEEE, 0xFFEE39E4, 0xFFE91A64};
                        for (int i = 0; i < 4; ++i)
                        {
                            int x = sWidth / 2 - 40 + i * 20 + 2;
                            drawRect(x, sHeight - 19, x + 16, sHeight - 3, colores[i]);
                        }
                    }
                    
                    GL11.glFlush();
                    GL11.glPopMatrix();
                    this.updateDisplaySize();
                    this.checkGLError("Post render");
                }
                catch (OutOfMemoryError e)
                {
                    this.renderGlobal.deleteAllDisplayLists();
                    System.gc();
                    this.loadWorldNull();
                    this.shutdown();
                }
                
                if(this.serverRunning){
            		long currentTime = System.currentTimeMillis();
                    long deltaTime = currentTime - prevTime;

                    if (deltaTime > 2000L && prevTime - this.timeOfLastWarning >= 15000L)
                    {
                        System.out.println("Can\'t keep up! Did the system time change, or is the server overloaded? Running " + deltaTime + "ms behind, skipping " + deltaTime / 50L + " tick(s)");
                        deltaTime = 2000L;
                        this.timeOfLastWarning = prevTime;
                    }

                    if (deltaTime < 0L)
                    {
                    	System.out.println("Time ran backwards! Did the system time change?");
                        deltaTime = 0L;
                    }

                    tickTimer += deltaTime;
                    prevTime = currentTime;

                    while (tickTimer > 50L)
                    {
                        tickTimer -= 50L;

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
        catch (Throwable t)
        {
            this.displayCrashReport(t, "Unexpected error");
        }
        finally
        {
        	try
            {
        		System.out.println("Stopping!");

                try
                {
                    this.loadWorldNull();
                }
                catch (Throwable t){}

                try
                {
                    GLAllocation.deleteTexturesAndDisplayLists();
                }
                catch (Throwable t){}
            }
            finally
            {
                Display.destroy();
                System.exit(0);
            }

            System.gc();
        }
    }
    
    /**
     * Draws a solid color rectangle with the specified coordinates and color. Args: x1, y1, x2, y2, color
     */
    private static void drawRect(int x1, int y1, int x2, int y2, int color)
    {
        Tessellator tessellator = Tessellator.instance;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(770, 771);
        tessellator.startDrawing(7);
        tessellator.setColor_I(color);
        tessellator.addVertex(x1, y2, 0);
        tessellator.addVertex(x2, y2, 0);
        tessellator.addVertex(x2, y1, 0);
        tessellator.addVertex(x1, y1, 0);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void updateDisplaySize()
    {
        Display.update();

        if (Display.wasResized())
        {
            this.displayWidth = Display.getWidth();
            this.displayHeight = Display.getHeight();

            if (this.displayWidth <= 0)
            {
                this.displayWidth = 1;
            }

            if (this.displayHeight <= 0)
            {
                this.displayHeight = 1;
            }
        }
    }

    /**
     * Called when the window is closing. Sets 'running' to false which allows the game loop to exit cleanly.
     */
    public void shutdown()
    {
        this.running = false;
    }

    /**
     * Will set the focus to ingame if the Minecraft window is the active with focus.
     */
    private void setIngameFocus()
    {
    	if (Display.isActive() && !this.inGameHasFocus)
        {
            this.inGameHasFocus = true;
            Mouse.setGrabbed(true);
        }
    }
    
    public boolean getInGameHasFocus()
    {
    	return this.inGameHasFocus;
    }

	private void playerRightClick()
    {
        this.rightClickDelayTimer = 4;

        if (this.objectMouseOver != null)
        {
        	int blockID = this.currentItem + 1;
        	Block block = Block.getBlockById(blockID);
        	
        	int x = this.objectMouseOver.x;
            int y = this.objectMouseOver.y;
            int z = this.objectMouseOver.z;
            int side = this.objectMouseOver.side;
            
            int[] xOff = {0, 0, 0, 0, -1, 1};
        	int[] yOff = {-1, 1, 0, 0, 0, 0};
        	int[] zOff = {0, 0, -1, 1, 0, 0};
        	
        	int xPrime = x + xOff[side];
        	int yPrime = y + yOff[side];
        	int zPrime = z + zOff[side];
        	
        	if (!this.worldServer.getBlock(x, y, z).onBlockActivatedServer(this.worldServer, x, y, z) && y < 256 && (y < 255 || side != 1) && this.worldServer.canPlaceEntity(block, xPrime, yPrime, zPrime))
            {
        		this.worldServer.setBlockAndMeta(xPrime, yPrime, zPrime, blockID, block.onBlockPlaced(this.worldServer, xPrime, yPrime, zPrime, side));
            }
        }
    }
	
	private void playerLeftClick()
	{
		this.leftClickDelayTimer = 6;
		
		if (this.objectMouseOver != null)
        {
    		int x = this.objectMouseOver.x;
            int y = this.objectMouseOver.y;
            int z = this.objectMouseOver.z;

            if (!this.worldServer.isReplaceable(x, y, z))
            {
                this.worldServer.setBlockAndMeta(x, y, z, 0, 0);
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
            System.out.println("Starting integrated minecraft server version 1.7.10");
            this.worldServer = new WorldServer(this, new File(this.savesDirectory, name));
            
            prevTime = System.currentTimeMillis();
            tickTimer = 0L;

            if (this.renderGlobal != null)
            {
                this.renderGlobal.setWorldAndLoadRenderers(this.worldServer);
            }

            this.thePlayer = new EntityPlayer(this.worldServer);

            System.gc();
            this.systemTime = 0L;
            
            this.worldServer.spawnPlayerInWorld(this);
        }
        catch (Throwable t)
        {
            throw new RuntimeException("Starting integrated server", t);
        }

        this.currentScreen = null;
        Keyboard.enableRepeatEvents(false);
        this.setIngameFocus();
    }
    
    private void loadWorldNull()
    {
    	this.serverRunning = false;
        this.saveAllWorlds();
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
     * par1 indicates if a log message should be output.
     */
    public void saveAllWorlds()
    {
    	if (this.worldServer != null)
        {
        	System.out.println("Saving worlds");
            this.worldServer.saveAllChunks();
        }
    }

    /**
     * Finds what block or object the mouse is over at the specified partial tick time. Args: partialTickTime
     */
    public void computeMouseOver(double partialTickTime)
    {
        if (this.thePlayer != null)
        {
        	this.objectMouseOver = this.thePlayer.rayTrace8(partialTickTime);
        }
    }
    
    public MovingObjectPosition getMouseOver()
    {
    	return this.objectMouseOver;
    }
    
    public String[] getSaveList()
    {
        return Arrays.stream(this.savesDirectory.listFiles()).filter(File::isDirectory).map(File::getName).sorted().toArray(String[]::new);
    }
}