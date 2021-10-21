package net.minecraft.client.renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.util.Vec3;

public class RenderGlobal
{
    private final Set<ChunkRenderer> crUpdate = new HashSet<ChunkRenderer>();
    private ChunkRenderer[][][] crs;
    private final int r = 16;
    private final int w = this.r * 2 + 1;
    private final int h = 16;

    /** OpenGL render lists base */
    private final int glRenderListBase;
    
    private final int numLists = this.w * this.w * this.h;

    public RenderGlobal()
    {
        this.glRenderListBase = GL11.glGenLists(this.numLists);
    }

    /**
     * set null to clear
     */
    public void loadRenderers()
    {
        this.crs = new ChunkRenderer[this.w][this.h][this.w];

        this.crUpdate.clear();
        
        int listOff = 0;

        for (int x = 0; x < this.w; x++)
        {
            for (int y = 0; y < this.h; y++)
            {
                for (int z = 0; z < this.w; z++)
                {
                    this.crs[x][y][z] = new ChunkRenderer(x, y, z, this.glRenderListBase + (listOff++));
                    this.crUpdate.add(this.crs[x][y][z]);
                }
            }
        }
    }

    /**
     * Sorts all renderers based on the passed in entity. Args: entityLiving, ppos
     */
    public void sortAndRender(EntityPlayer player, Vec3 ppos)
    {
    	IntBuffer glLists = ByteBuffer.allocateDirect(this.w * this.h * this.w * 4)
        		.order(ByteOrder.nativeOrder()).asIntBuffer();
    	
    	int px = player.getChunkCoordX() - this.r;
		int pz = player.getChunkCoordZ() - this.r;

        for (int x = 0; x < this.w; x++)
        {
            for (int z = 0; z < this.w; z++)
            {
                for (int y = 0; y < this.h; y++)
                {
                	ChunkRenderer cr = this.crs[x][y][z];

                    if (cr.setPosition(px + Math.floorMod(x - px, this.w), y, pz + Math.floorMod(z - pz, this.w)))
                    {
                        this.crUpdate.add(cr);
                    }
                    
                    glLists.put(cr.getGLCallList());
                }
            }
        }
        
        glLists.flip();
        
    	GL11.glPushMatrix();
        GL11.glTranslatef(-(float)ppos.x, -(float)ppos.y, -(float)ppos.z);
        GL11.glCallLists(glLists);
        GL11.glPopMatrix();
    }

    /**
     * Updates some of the renderers sorted by distance from the player
     */
    public void updateRenderers(World world, EntityPlayer player, Vec3 ppos)
    {
    	this.crUpdate.removeAll(this.crUpdate.stream()
    			.filter(cr->cr.quadranceToPlayer(player) <= 272D)
    			.map(cr->cr.updateRenderer(world))
    			.collect(Collectors.toSet()));
        
        this.crUpdate.remove(this.crUpdate.stream().min(new Comparator<ChunkRenderer>(){
        	public int compare(ChunkRenderer cr1, ChunkRenderer cr2){
        		double d = cr1.quadranceToPlayer(player) - cr2.quadranceToPlayer(player);
        		return d > 0 ? 1 : d < 0 ? -1 : 0;
        	}
        }).get().updateRenderer(world));
    }

    /**
     * Marks the blocks in the given range for update
     */
    public void markChunksForUpdate(int x1, int y1, int z1, int x2, int y2, int z2)
    {
        for (int x = x1; x <= x2; x++)
        {
            for (int y = y1; y <= y2; y++)
            {
                for (int z = z1; z <= z2; z++)
                {
                    this.crUpdate.add(this.crs[Math.floorMod(x, this.w)][Math.floorMod(y, this.h)][Math.floorMod(z, this.w)]);
                }
            }
        }
    }

    /**
     * Deletes all display lists
     */
    public void deleteAllDisplayLists()
    {
    	GL11.glDeleteLists(this.glRenderListBase, this.numLists);
    }
}
