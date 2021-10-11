package net.minecraft.client;

import java.io.File;
import java.util.Arrays;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiScreen
{
    /** Reference to the Minecraft object. */
    private final Minecraft mc;
    private String[] worldList;
    private String text;
    private int scrollPos;
    private final File saves;
    
    public GuiScreen(Minecraft minecraft, File saves)
    {
    	this.mc = minecraft;
    	this.saves = saves;
    	this.reset();
    }
    
    public void reset()
    {
        Keyboard.enableRepeatEvents(true);
    	this.text = "";
    	this.worldList = Arrays.stream(this.saves.listFiles()).filter(File::isDirectory).map(File::getName).sorted().toArray(String[]::new);
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
    				this.mc.launchIntegratedServer(new File(this.saves, this.worldList[off + this.scrollPos]));
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
                		this.mc.launchIntegratedServer(new File(this.saves,
                				this.text.trim()
                				.replaceAll("[/\n\r\t\u0000\f`?*\\<>|\":.]", "_")
                				.replaceAll("^$", "World")));
                	
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
