package net.minecraft.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.renderer.GLAllocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.google.common.collect.Maps;

public class GuiScreen
{
    /** Reference to the Minecraft object. */
    private Minecraft mc;

    /** The width of the screen object. */
    private int width;

    /** The height of the screen object. */
    private int height;

    /** The FontRenderer used by GuiScreen */
    private FontRenderer fontRendererObj;
    private List<String> worldList;
    private String newWorldName;
    private boolean hasCreatedWorld;
    private String text = "";
    
    private boolean isLaunched = false;
    private float scrollPos;

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

                this.fontRendererObj.drawString(worldName, this.width / 2 - 110 + 4, renderY + 1);
            }
        }
        
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        this.fontRendererObj.drawString(this.text, this.width / 2 - 106, this.height - 52);
    }

    /**
     * Causes the screen to lay out its subcomponents again. This is the equivalent of the Java call
     * Container.validate()
     */
    public void setWorldAndResolution(Minecraft minecraft, int w, int h)
    {
        this.mc = minecraft;
        this.fontRendererObj = new FontRenderer();
        this.width = w;
        this.height = h;
        this.worldList = this.mc.getSaveList();
        Collections.sort(this.worldList);
        Keyboard.enableRepeatEvents(true);
        this.fixWorldName();
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
        	char ch = Keyboard.getEventCharacter();
        	int key = Keyboard.getEventKey();
            switch (key)
            {
                case 14:
                	if (this.text.length() > 0)
                    {
                		this.text = this.text.substring(0, this.text.length() - 1);
                    }
                    break;
                    
                case 203:
                	break;
                	
                case 205:
                	break;

                default:
                	if (ch != 167 && ch >= 32 && ch != 127 && this.text.length() < 32)
                    {
                    	this.text += ch;
                    }
            }
            
            if(key == 1)
            {
            	this.mc.shutdown();
            }

            if (key == 28 || key == 156)
            {
            	if (this.text.length() > 0)
            	{
            		if (this.hasCreatedWorld) return;
                    this.hasCreatedWorld = true;
                    this.mc.launchIntegratedServer(this.newWorldName);
            	}
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
    
    private class FontRenderer
    {
        private final Map<Integer, Texture> mapTextureObjects = Maps.newHashMap();

        /**
         * Array of the start/end column (in upper/lower nibble) for every glyph in the /font directory.
         */
        private byte[] glyphWidth = new byte[65536];

        /** Current X coordinate at which to draw the next character. */
        private float posX;

        /** Current Y coordinate at which to draw the next character. */
        private float posY;

        public FontRenderer()
        {
            try
            {
                FontRenderer.class.getResourceAsStream("/assets/minecraft/font/glyph_sizes.bin").read(this.glyphWidth);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        /**
         * Render a single Unicode character at current (posX,posY) location using one of the /font/glyph_XX.png files...
         */
        private float renderUnicodeChar(char ch)
        {
            if (this.glyphWidth[ch] == 0)
            {
                return 0.0F;
            }
            else
            {
                int page = ch / 256;
                
                Texture texture = this.mapTextureObjects.get(page);

                if (texture == null)
                {
                    try
                    {
                    	texture = new Texture(page);
                    }
                    catch (Throwable t)
                    {
                        throw new RuntimeException("Registering texture", t);
                    }

                    this.mapTextureObjects.put(page, texture);
                }

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlTextureId());
                
                int var4 = this.glyphWidth[ch] >>> 4;
                int var5 = this.glyphWidth[ch] & 15;
                float var6 = (float)var4;
                float var7 = (float)(var5 + 1);
                float var8 = (float)(ch % 16 * 16) + var6;
                float var9 = (float)((ch & 255) / 16 * 16);
                float var10 = var7 - var6 - 0.02F;
                GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
                GL11.glTexCoord2f(var8 / 256.0F, var9 / 256.0F);
                GL11.glVertex3f(this.posX, this.posY, 0.0F);
                GL11.glTexCoord2f(var8 / 256.0F, (var9 + 15.98F) / 256.0F);
                GL11.glVertex3f(this.posX, this.posY + 7.99F, 0.0F);
                GL11.glTexCoord2f((var8 + var10) / 256.0F, var9 / 256.0F);
                GL11.glVertex3f(this.posX + var10 / 2.0F, this.posY, 0.0F);
                GL11.glTexCoord2f((var8 + var10) / 256.0F, (var9 + 15.98F) / 256.0F);
                GL11.glVertex3f(this.posX + var10 / 2.0F, this.posY + 7.99F, 0.0F);
                GL11.glEnd();
                return (var7 - var6) / 2.0F + 1.0F;
            }
        }

        /**
         * Draws the specified string. Args: string, x, y, color, dropShadow
         */
        public void drawString(String text, int x, int y)
        {
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glColor4f(1, 1, 1, 1);
            this.posX = (float)x;
            this.posY = (float)y;
            
            for (int index = 0; index < text.length(); ++index)
            {
                char ch = text.charAt(index);

                if (!(ch == 167 && index + 1 < text.length()))
                {
                    this.posX += (float)((int)(ch == 32 ? 4.0F : this.renderUnicodeChar(ch)));
                }
            }
        }
        
        private class Texture
        {
            private int glTextureId;
            
            public Texture(int page) throws IOException
            {
                InputStream texStream = null;

                try
                {
                    texStream = Texture.class.getResourceAsStream(String.format("/assets/minecraft/textures/font/unicode_page_%02x.png", new Object[] {page}));
                    this.glTextureId = GL11.glGenTextures();
                    BufferedImage image = ImageIO.read(texStream);
                    GL11.glDeleteTextures(this.glTextureId);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.glTextureId);
                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, image.getWidth(), image.getHeight(), 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer)null);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.glTextureId);
                    
                    int w = image.getWidth();
                    int h = image.getHeight();
                    int var7 = 4194304 / w;
                    int[] source = new int[var7 * w];
                    
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

                    for (int i = 0; i < w * h; i += w * var7)
                    {
                        int var10 = i / w;
                        int var11 = Math.min(var7, h - var10);
                        int limit = w * var11;
                        image.getRGB(0, var10, w, var11, source, 0, w);
                        IntBuffer dataBuffer = GLAllocation.createDirectIntBuffer(4194304);
                        dataBuffer.put(source, 0, limit);
                        dataBuffer.position(0).limit(limit);
                        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, var10, w, var11, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, dataBuffer);
                    }
                }
                finally
                {
                    if (texStream != null)
                    {
                        texStream.close();
                    }
                }
            }

            public int getGlTextureId()
            {
                return this.glTextureId;
            }
        }
    }
}
