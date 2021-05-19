package net.minecraft.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class GuiScreen
{
    /** Reference to the Minecraft object. */
    private final Minecraft mc;

    /** The width of the screen object. */
    private int width;

    /** The height of the screen object. */
    private int height;

    private final int glTextureId;
    private final List<String> worldList;
    private String newWorldName;
    private boolean hasCreatedWorld;
    private String text = "";
    
    private boolean isLaunched = false;
    private float scrollPos;
    
    public GuiScreen(Minecraft minecraft, int screenWidth, int screenHeight)
    {
    	this.mc = minecraft;
        try
        {
        	InputStream texStream = null;

            try
            {
                texStream = GuiScreen.class.getResourceAsStream("/minecraft/font/asky.bin");
                
                byte[] input = new byte[0x10000];
                texStream.read(input);
                ByteBuffer bbuf = ByteBuffer.allocateDirect(0x10000).put(input);
                bbuf.flip();
                
                this.glTextureId = GL11.glGenTextures();
                
                GL11.glDeleteTextures(this.glTextureId);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.glTextureId);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 128, 128, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, bbuf.asIntBuffer());
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            }
            finally
            {
                if (texStream != null)
                {
                    texStream.close();
                }
            }
        }
        catch (IOException e)
        {
        	throw new RuntimeException(e);
        }
        
        this.width = screenWidth;
        this.height = screenHeight;
        this.worldList = this.mc.getSaveList();
        Collections.sort(this.worldList);
        Keyboard.enableRepeatEvents(true);
        this.fixWorldName();
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int mouseX, int mouseY)
    {
    	int listLength = this.worldList.size();

        if (mouseX > 0 && mouseX < this.width && mouseY > 32 && mouseY < this.height - 64)
        {
            if (Mouse.isButtonDown(0))
            {
                if (!this.isLaunched)
                {
                    if (mouseY >= 32 && mouseY <= this.height - 64)
                    {
                        int minX = this.width / 2 - 110;
                        int maxX = this.width / 2 + 110;
                        int relMouseY = mouseY - 36 + (int)this.scrollPos;
                        int id = relMouseY / 36;

                        if (mouseX >= minX && mouseX <= maxX && id >= 0 && relMouseY >= 0 && id < listLength)
                        {
                            String worldFolderName = this.worldList.get(id);

                            if (worldFolderName == null)
                            {
                                worldFolderName = "World" + id;
                            }

                            if (this.mc.canLoadWorld(worldFolderName))
                            {
                                this.mc.launchIntegratedServer(worldFolderName);
                            }
                            
                            this.isLaunched = true;
                        }
                    }
                }
            }
            else
            {
                for (;Mouse.next();)
                {
                    int scroll = Mouse.getEventDWheel();

                    if (scroll != 0)
                    {
                        if (scroll > 0)
                        {
                            scroll = -1;
                        }
                        else if (scroll < 0)
                        {
                            scroll = 1;
                        }

                        this.scrollPos += scroll * 18;
                    }
                }

                this.isLaunched = false;
            }
        }

        int maxScrollPos = this.worldList.size() * 36 - this.height + 100;

        if (maxScrollPos < 0)
        {
            maxScrollPos /= 2;
        }

        if (this.scrollPos < 0.0F)
        {
            this.scrollPos = 0.0F;
        }

        if (this.scrollPos > (float)maxScrollPos)
        {
            this.scrollPos = (float)maxScrollPos;
        }
        
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        
        for (int id = 0; id < this.worldList.size(); ++id)
        {
            int renderY = 36 - (int)this.scrollPos + id * 36;

            if (renderY <= this.height - 64 && renderY >= 0)
            {
                String worldName = this.worldList.get(id);

                if (worldName == null || worldName.length() == 0)
                {
                    worldName = "World " + (id + 1);
                }

                this.drawString(worldName, this.width / 2 - 110 + 4, renderY + 1);
            }
        }
        
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        this.drawString(this.text, this.width / 2 - 106, this.height - 52);
    }

    /**
     * Causes the screen to lay out its subcomponents again. This is the equivalent of the Java call
     * Container.validate()
     */
    public void setWorldAndResolution(int w, int h)
    {
        this.width = w;
        this.height = h;
    }
    
    private void fixWorldName()
    {
        this.newWorldName = this.text.trim();
        char[] forbiddenCharacters = new char[] {'/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

        for (int i = 0; i < forbiddenCharacters.length; ++i)
        {
            char c = forbiddenCharacters[i];
            this.newWorldName = this.newWorldName.replace(c, '_');
        }

        if (this.newWorldName == null || this.newWorldName.length() == 0)
        {
            this.newWorldName = "World";
        }

        this.newWorldName = this.newWorldName.replaceAll("[\\./\"]", "_");

        while (this.mc.canLoadWorld(this.newWorldName))
        {
        	this.newWorldName = this.newWorldName + "-";
        }
    }

    /**
     * Delegates mouse and keyboard input.
     */
    public void handleInput()
    {
        if (Keyboard.isCreated())
        {
            while (Keyboard.next())
            {
                this.handleKeyboardInput();
            }
        }
    }

    /**
     * Handles keyboard input.
     */
    public void handleKeyboardInput()
    {
        if (Keyboard.getEventKeyState())
        {
        	switch (Keyboard.getEventKey())
            {
            case 1:
            	this.mc.shutdown();
            	break;
            	
            case 14:
            	if (this.text.length() > 0) this.text = this.text.substring(0, this.text.length() - 1);
            	break;
            	
            case 28:
            case 156:
            	if (this.text.length() > 0)
            	{
            		if (this.hasCreatedWorld) return;
                    this.hasCreatedWorld = true;
                    this.mc.launchIntegratedServer(this.newWorldName);
            	}
            	
            case 203:
            case 205:
            	break;

            default:
            	char ch = Keyboard.getEventCharacter();
            	if (ch >= 32 && ch <= 126 && this.text.length() < 32) this.text += ch;
            }
            
            this.fixWorldName();
        }
    }

    /**
     * "Called when the screen is unloaded. Used to disable keyboard repeat events."
     */
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
    }
    
    /**
     * Draws the specified string. Args: string, x, y, color, dropShadow
     */
    private void drawString(String text, int xStart, int y)
    {
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glColor4f(1, 1, 1, 1);
        
        for (int i = 0; i < text.length(); ++i)
        {
        	int x = xStart + (i * 7);
        	char ch = text.charAt(i);
        	int col = (ch & 15) * 8;
            int row = (ch >> 4) * 8;
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.glTextureId);
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            GL11.glTexCoord2f(col / 128.0F, row / 128.0F);
            GL11.glVertex3f(x, y, 0.0F);
            GL11.glTexCoord2f(col / 128.0F, (row + 7.99F) / 128.0F);
            GL11.glVertex3f(x, y + 7.99F, 0.0F);
            GL11.glTexCoord2f((col + 6.99F) / 128.0F, row / 128.0F);
            GL11.glVertex3f(x + 6.99F, y, 0.0F);
            GL11.glTexCoord2f((col + 6.99F) / 128.0F, (row + 7.99F) / 128.0F);
            GL11.glVertex3f(x + 6.99F, y + 7.99F, 0.0F);
            GL11.glEnd();
        }
    }
}
