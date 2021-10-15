package net.minecraft.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tesselator;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Project;

public class Minecraft
{
    public int w = 854;
    public int h = 480;
    private RenderGlobal render;
    private EntityPlayer player;

    /**
     * When you place a block, it's set to 6, decremented once per tick, when it's 0, you can place another block.
     */
    private int rightClickDelayTimer;
    private int leftClickDelayTimer;

    /**
     * Does the actual gameplay have focus. If so then mouse and keys will effect the player instead of menus.
     */
    private boolean inGameHasFocus;

    /**
     * Set to true to keep the game loop running. Set to false by shutdown() to allow the game loop to exit cleanly.
     */
    private volatile boolean running = true;
    
    /** The server world instance. */
    private World world;

    /** Incremented every tick. */
    private int tickCounter;
    private int currentItem;

    /**
     * How much time has elapsed since the last tick, in ticks (range: 0.0 - 1.0).
     */
    private double renderPartialTicks;
    
    /**
     * The time reported by the high-resolution clock at the last call of updateTimer(), in seconds
     */
    private double lastHRTime;
    
    private int playerChunkX;
	private int playerChunkZ;

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
    private void checkGLError(String s)
    {
        int e = GL11.glGetError();
        if (e != 0) System.out.println("GL ERROR @ " + s + "\n" + e + ": " + GLU.gluErrorString(e));
    }

    public void run(File savesDir)
    {
        this.running = true;
        savesDir.mkdirs();
        GuiScreen menu = new GuiScreen(this, savesDir);
        int fontTextureID = -1;

        try
        {
        	Display.setDisplayMode(new DisplayMode(this.w, this.h));
            Display.setResizable(true);
            Display.setTitle("Boolcraft");

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
            
            //GL11 calls begin
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glFlush();
            this.render = new RenderGlobal();
            this.checkGLError("Startup");
            
            try
            {
                byte[] input = new byte[0x10000];
            	InputStream texStream = GuiScreen.class.getResourceAsStream("/minecraft/font/asky.bin");
                texStream.read(input);
                texStream.close();
                fontTextureID = GL11.glGenTextures();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTextureID);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 128, 128, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
                		((ByteBuffer)ByteBuffer.allocateDirect(0x10000).put(input).flip()).asIntBuffer());
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            }
            catch (IOException e)
            {
            	throw new RuntimeException(e);
            }
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

                    this.renderPartialTicks += diffHRClockSecs * 20;
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

                        if (this.player == null)
                        {
                            menu.handleInput(Mouse.getX(), this.h - Mouse.getY() - 1);
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
                                    
                                    int mouseScroll = Mouse.getEventDWheel();

                                    this.currentItem = (this.currentItem + 4 - (mouseScroll > 0 ? 1 : mouseScroll < 0 ? -1 : 0)) % 4;
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
                                            menu.reset();
                                        }
                                    }
                                }
                            }
                            
                            MovingObjectPosition hit = null;
                            
                            if (this.player != null)
                            {
                            	hit = this.player.rayTrace8();
                            }

                            while (KeyBinding.keyBindAttack.isPressed())
                            {
                            	this.playerLeftClick(hit);
                            }

                            if (KeyBinding.keyBindAttack.getIsKeyPressed() && this.leftClickDelayTimer == 0)
                            {
                                this.playerLeftClick(hit);
                            }

                            while (KeyBinding.keyBindUseItem.isPressed())
                            {
                                this.playerRightClick(hit);
                            }
                            
                            if (KeyBinding.keyBindUseItem.getIsKeyPressed() && this.rightClickDelayTimer == 0)
                            {
                                this.playerRightClick(hit);
                            }
                        }
                        
                        if (this.player != null)
                        {
                            try
                            {
                        		this.player.onUpdate();
                        		this.playerChunkX = (int) this.player.getPosX() >> 4;
                        		this.playerChunkZ = (int) this.player.getPosZ() >> 4;
                            }
                            catch (Throwable t)
                            {
                                throw new RuntimeException("Ticking entity", t);
                            }
                            
                            ++this.tickCounter;

                            try
                            {
                            	this.world.tick(this.playerChunkX, this.playerChunkZ);
                            }
                            catch (Throwable t)
                            {
                                throw new RuntimeException("Exception ticking world", t);
                            }

                            if (this.tickCounter % 900 == 0)
                            {
                                this.saveAllWorlds();
                            }
                        }
                    }
                    
                    this.checkGLError("Pre render");
                    GL11.glPushMatrix();
                    GL11.glViewport(0, 0, this.w, this.h);
                    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                    
                    if (this.player == null)
                    {
                        GL11.glEnable(GL11.GL_TEXTURE_2D);
                        GL11.glMatrixMode(GL11.GL_PROJECTION);
                        GL11.glLoadIdentity();
                        GL11.glOrtho(0.0D, this.w, this.h, 0.0D, 0.0D, 1.0D);
                        GL11.glMatrixMode(GL11.GL_MODELVIEW);
                        GL11.glLoadIdentity();
                        GL11.glEnable(GL11.GL_ALPHA_TEST);
                        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
                        GL11.glColor4f(1, 1, 1, 1);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTextureID);
                    	menu.drawScreen();
                    }
                    else
                    {
                        if (this.inGameHasFocus && Display.isActive())
                        {
                        	this.player.setAngles();
                        }
                        
                        GL11.glDisable(GL11.GL_TEXTURE_2D);
                        GL11.glMatrixMode(GL11.GL_PROJECTION);
                        GL11.glLoadIdentity();
                        Project.gluPerspective(110, (float)this.w / (float)this.h, 0.05F, 512.0F); //FOV
                        GL11.glMatrixMode(GL11.GL_MODELVIEW);
                        GL11.glLoadIdentity();
                        GL11.glRotatef((float)this.player.getRotationPitch(), 1.0F, 0.0F, 0.0F);
                        GL11.glRotatef((float)this.player.getRotationYaw() + 180.0F, 0.0F, 1.0F, 0.0F);
                        Vec3 ppos = this.player.pttPos(this.renderPartialTicks);
                        this.render.updateRenderers(this.player, ppos);
                        GL11.glPushMatrix();
                        this.render.sortAndRender(this.player, ppos);
                        GL11.glPopMatrix();
                        
                        GL11.glEnable(GL11.GL_BLEND);
                        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                        
                        MovingObjectPosition hit = this.player.rayTrace8();
                        
                        Tesselator tess = Tesselator.instance;
                        
                        if (hit != null)
                        {
                            GL11.glLineWidth(2.0F);
                            int meta = this.world.getBlockMetadata(hit.x, hit.y, hit.z);
                            AxisAlignedBB aabb = this.world.getBlock(hit.x, hit.y, hit.z)
                            		.generateCubicBoundingBox(meta)
                            		.offset(hit.x-ppos.x, hit.y-ppos.y, hit.z-ppos.z)
                            		.expand(0.002F);
                            tess.setColor_I(0x44CCCCCC);
                            tess.startDrawing();
                            tess.addVertex(aabb.minX, aabb.minY, aabb.minZ);
                            tess.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
                            tess.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
                            tess.addVertex(aabb.maxX, aabb.minY, aabb.minZ);

                            tess.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
                            tess.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
                            tess.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
                            tess.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);

                            tess.addVertex(aabb.minX, aabb.minY, aabb.minZ);
                            tess.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
                            tess.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
                            tess.addVertex(aabb.minX, aabb.minY, aabb.maxZ);

                            tess.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
                            tess.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
                            tess.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
                            tess.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
                            
                            tess.addVertex(aabb.minX, aabb.minY, aabb.minZ);
                            tess.addVertex(aabb.minX, aabb.minY, aabb.maxZ);
                            tess.addVertex(aabb.minX, aabb.maxY, aabb.maxZ);
                            tess.addVertex(aabb.minX, aabb.maxY, aabb.minZ);
                            
                            tess.addVertex(aabb.maxX, aabb.minY, aabb.minZ);
                            tess.addVertex(aabb.maxX, aabb.minY, aabb.maxZ);
                            tess.addVertex(aabb.maxX, aabb.maxY, aabb.maxZ);
                            tess.addVertex(aabb.maxX, aabb.maxY, aabb.minZ);
                            tess.draw();
                        }
                        
                        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
                        GL11.glEnable(GL11.GL_DEPTH_TEST);
                        GL11.glDepthFunc(GL11.GL_LEQUAL);
                        GL11.glMatrixMode(GL11.GL_PROJECTION);
                        GL11.glLoadIdentity();
                        GL11.glOrtho(0.0D, this.w, this.h, 0.0D, 0.0D, 1.0D);
                        GL11.glMatrixMode(GL11.GL_MODELVIEW);
                        GL11.glLoadIdentity();
                        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        
                        tess.startDrawing();
                        tess.setColor_I(0x44CCCCCC);
                        tess.addVertex(this.w / 2 - 4, this.h / 2 + 6, 0);
                        tess.addVertex(this.w / 2 + 6, this.h / 2 + 6, 0);
                        tess.addVertex(this.w / 2 + 6, this.h / 2 - 4, 0);
                        tess.addVertex(this.w / 2 - 4, this.h / 2 - 4, 0);
                        
                        int[] colores = {0xFF505050, 0xFF39EEEE, 0xFFEE39E4, 0xFFE91A64};
                        
                        for (int i = 0; i < 4; ++i)
                        {
                        	int d = i == this.currentItem ? 2 : 0;
                            int x = this.w / 2 - 40 + i * 20 + 2;
                            tess.setColor_I(colores[i]);
                            tess.addVertex(x      - d, this.h -  3 + d, 0);
                            tess.addVertex(x + 16 + d, this.h -  3 + d, 0);
                            tess.addVertex(x + 16 + d, this.h - 19 - d, 0);
                            tess.addVertex(x      - d, this.h - 19 - d, 0);
                        }
                        
                        tess.draw();
                        GL11.glDisable(GL11.GL_BLEND);
                    }
                    
                    GL11.glFlush();
                    GL11.glPopMatrix();
                    Display.update();

                    if (Display.wasResized())
                    {
                        this.w = Display.getWidth();
                        this.h = Display.getHeight();

                        if (this.w <= 0)
                        {
                            this.w = 1;
                        }

                        if (this.h <= 0)
                        {
                            this.h = 1;
                        }
                    }
                    
                    this.checkGLError("Post render");
                }
                catch (OutOfMemoryError e)
                {
                    this.render.deleteAllDisplayLists();
                    System.gc();
                    this.loadWorldNull();
                    this.shutdown();
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

	private void playerRightClick(MovingObjectPosition hit)
    {
        this.rightClickDelayTimer = 4;

        if (hit != null)
        {
        	int id = this.currentItem + 1;
        	
        	int x = hit.x;
            int y = hit.y;
            int z = hit.z;
            int side = hit.side;
            
            int[] xOff = {0, 0, 0, 0, -1, 1};
        	int[] yOff = {-1, 1, 0, 0, 0, 0};
        	int[] zOff = {0, 0, -1, 1, 0, 0};
        	
        	int xPrime = x + xOff[side];
        	int yPrime = y + yOff[side];
        	int zPrime = z + zOff[side];
        	
        	if ((this.world.getBlocMeta(x, y, z) & 0xF) == 3)
        	{
        		int meta = this.world.getBlockMetadata(x, y, z);
                int orientation = meta & 7;
                this.world.setBlockAndMeta(x, y, z, 3, meta ^ 8);
                this.world.notifyBlocksOfNeighborChange(x, y, z);

                if (orientation == 1)
                {
                	this.world.notifyBlocksOfNeighborChange(x - 1, y, z);
                }
                else if (orientation == 2)
                {
                	this.world.notifyBlocksOfNeighborChange(x + 1, y, z);
                }
                else if (orientation == 3)
                {
                	this.world.notifyBlocksOfNeighborChange(x, y, z - 1);
                }
                else if (orientation == 4)
                {
                	this.world.notifyBlocksOfNeighborChange(x, y, z + 1);
                }
                else if (orientation == 5)
                {
                	this.world.notifyBlocksOfNeighborChange(x, y - 1, z);
                }
                else if (orientation == 0)
                {
                	this.world.notifyBlocksOfNeighborChange(x, y + 1, z);
                }
        	}
        	else if (y < 256 && (y < 255 || side != 1) && this.world.isAir(xPrime, yPrime, zPrime) && 
        			(this.world.isSolid(xPrime - 1, yPrime, zPrime)
        		  || this.world.isSolid(xPrime + 1, yPrime, zPrime)
        		  || this.world.isSolid(xPrime, yPrime, zPrime - 1)
        		  || this.world.isSolid(xPrime, yPrime, zPrime + 1)
        		  || this.world.isSolid(xPrime, yPrime - 1, zPrime)
        		  || this.world.isSolid(xPrime, yPrime + 1, zPrime)
        		  || id == 1))
            {
        		int meta = 0;
            	
            	if (id == 3 || id == 4)
            	{
            		if (this.world.isSolid(x, y, z))
                    {
                    	meta = (6 - side) % 6;
                    }
                    else if (this.world.isSolid(xPrime - 1, yPrime, zPrime))
                    {
                    	meta = 1;
                    }
                    else if (this.world.isSolid(xPrime + 1, yPrime, zPrime))
                    {
                    	meta = 2;
                    }
                    else if (this.world.isSolid(xPrime, yPrime, zPrime - 1))
                    {
                    	meta = 3;
                    }
                    else if (this.world.isSolid(xPrime, yPrime, zPrime + 1))
                    {
                    	meta = 4;
                    }
                    else if (this.world.isSolid(xPrime, yPrime - 1, zPrime) || id == 4)
                    {
                    	meta = 5;
                    }
            	}
            	
        		this.world.setBlockAndMeta(xPrime, yPrime, zPrime, id, meta | (id == 4 ? 8 : 0));
            }
        }
    }
	
	private void playerLeftClick(MovingObjectPosition hit)
	{
		this.leftClickDelayTimer = 6;
		
		if (hit != null)
        {
    		int x = hit.x;
            int y = hit.y;
            int z = hit.z;

            if (!this.world.isAir(x, y, z))
            {
                this.world.setBlockAndMeta(x, y, z, 0, 0);
            }
        }
	}

    /**
     * Arguments: World name
     */
    public void launchIntegratedServer(File dir)
    {
        this.loadWorldNull();

        try
        {
            System.out.println("Starting integrated minecraft server version 1.7.10");
            this.world = new World(this.render, dir);

            if (this.render != null)
            {
                this.render.setWorldAndLoadRenderers(this.world);
            }

            this.player = new EntityPlayer(this.world);

            System.gc();
        }
        catch (Throwable t)
        {
            throw new RuntimeException("Starting integrated server", t);
        }

        Keyboard.enableRepeatEvents(false);
        this.setIngameFocus();
    }
    
    private void loadWorldNull()
    {
        this.saveAllWorlds();
        this.player = null;
        System.gc();
    }
    
    /**
     * par1 indicates if a log message should be output.
     */
    public void saveAllWorlds()
    {
    	if (this.world != null)
        {
        	System.out.println("Saving worlds");
            this.world.saveAllChunks(this.playerChunkX, this.playerChunkZ);
        }
    }
}