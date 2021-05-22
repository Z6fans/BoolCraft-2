package net.minecraft.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class GuiScreen
{
    /** Reference to the Minecraft object. */
    private final Minecraft mc;
    private final int glTextureId;
    private final String[] worldList;
    private String text = "";
    private int scrollPos;
    
    public GuiScreen(Minecraft minecraft, int screenWidth, int screenHeight)
    {
    	this.mc = minecraft;
    	
        try
        {
            byte[] input = new byte[0x10000];
        	InputStream texStream = GuiScreen.class.getResourceAsStream("/minecraft/font/asky.bin");
            texStream.read(input);
            texStream.close();
            this.glTextureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.glTextureId);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 128, 128, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
            		((ByteBuffer)ByteBuffer.allocateDirect(0x10000).put(input).flip()).asIntBuffer());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }
        catch (IOException e)
        {
        	throw new RuntimeException(e);
        }
        
        this.worldList = this.mc.getSaveList();
        Keyboard.enableRepeatEvents(true);
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int mouseX, int mouseY)
    {
    	int cells = (this.mc.displayHeight >> 5) - 1;
    	
    	if (mouseX > 0 && mouseX < this.mc.displayWidth && mouseY > 0 && mouseY < this.mc.displayHeight)
        {
            if (Mouse.isButtonDown(0))
            {
            	int off = mouseY >> 5;

    			if (off >= 0 && off < cells && off < this.worldList.length - this.scrollPos)
    			{
    				this.mc.launchIntegratedServer(this.worldList[off + this.scrollPos]);
    			}
            }
            else
            {
                while (Mouse.next())
                {
                    if (Mouse.getEventDWheel() < 0 && this.scrollPos < this.worldList.length - cells) this.scrollPos++;
                    if (Mouse.getEventDWheel() > 0 && this.scrollPos > 0) this.scrollPos--;
                }
            }
        }
    	
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.glTextureId);
        
        for (int off = 0; off < this.worldList.length - this.scrollPos && off < cells; off++)
        	this.drawString(this.worldList[off + this.scrollPos], 32, (off << 5) + 8);
        
        this.drawString(this.text, 32, (cells << 5) + 8);
    }

    /**
     * Delegates mouse and keyboard input.
     */
    public void handleInput()
    {
        while (Keyboard.next())
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
                		this.mc.launchIntegratedServer(this.text.trim()
                				.replaceAll("[/\n\r\t\u0000\f`?*\\<>|\":.]", "_")
                				.replaceAll("^$", "World"));
                	
                case 203:
                case 205:
                	break;

                default:
                	char ch = Keyboard.getEventCharacter();
                	if (ch >= 32 && ch <= 126 && this.text.length() < 32) this.text += ch;
                }
            }
        }
    }
    
    /**
     * Draws the specified string. Args: string, x, y, color, dropShadow
     */
    private void drawString(String text, int xStart, int y)
    {
        for (int i = 0; i < text.length(); ++i)
        {
        	int x = xStart + (i * 14);
        	char ch = text.charAt(i);
        	int col = (ch & 15) * 8;
            int row = (ch >> 4) * 8;
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            GL11.glTexCoord2f(col / 128.0F, row / 128.0F);
            GL11.glVertex2f(x, y);
            GL11.glTexCoord2f(col / 128.0F, (row + 8) / 128.0F);
            GL11.glVertex2f(x, y + 16);
            GL11.glTexCoord2f((col + 7) / 128.0F, row / 128.0F);
            GL11.glVertex2f(x + 14, y);
            GL11.glTexCoord2f((col + 7) / 128.0F, (row + 8) / 128.0F);
            GL11.glVertex2f(x + 14, y + 16);
            GL11.glEnd();
        }
    }
}
