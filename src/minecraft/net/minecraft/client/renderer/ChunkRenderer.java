package net.minecraft.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.util.AxisAlignedBB;

import org.lwjgl.opengl.GL11;

public class ChunkRenderer
{
    private final int glRenderList;
    private final static Tesselator tessellator = Tesselator.instance;
    private int chunkX;
    private int chunkY;
    private int chunkZ;

    public ChunkRenderer(int cx, int cy, int cz, int renderList)
    {
        this.glRenderList = renderList;
        this.chunkX = cx;
        this.chunkY = cy;
        this.chunkZ = cz;
    }

    public boolean setPosition(int cx, int cy, int cz)
    {
    	if (cx != this.chunkX || cy != this.chunkY || cz != this.chunkZ)
        {
            this.chunkX = cx;
            this.chunkY = cy;
            this.chunkZ = cz;
            return true;
        }
    	
    	return false;
    }

    public ChunkRenderer updateRenderer(World world)
    {
    	GL11.glNewList(this.glRenderList, GL11.GL_COMPILE);
        GL11.glPushMatrix();
        GL11.glTranslatef(this.chunkX * 16, this.chunkY * 16, this.chunkZ * 16);
        tessellator.startDrawing();
        tessellator.setTranslation(-this.chunkX * 16, -this.chunkY * 16, -this.chunkZ * 16);

        for (int y = this.chunkY * 16; y < (this.chunkY + 1) * 16; ++y) {
            for (int z = this.chunkZ * 16; z < (this.chunkZ + 1) * 16; ++z) {
                for (int x = this.chunkX * 16; x < (this.chunkX + 1) * 16; ++x) {
                	int bm = world.getBlocMeta(x, y, z);
                	int block = bm & 0xF;
                	int meta = bm >> 4;
                	
                    AxisAlignedBB aabb = Block.getBlockById(block).generateCubicBoundingBox(meta);
                    
                	Tesselator tess = Tesselator.instance;
                    
                    if (block == 2) {
                    	tess.setColor_I(getColor(block, meta, x, y, z, 0));
                        
                        tess.addVertex(x + 1, y + 0.01, z + 1);
                        tess.addVertex(x + 1, y + 0.01, z + 0);
                        tess.addVertex(x + 0, y + 0.01, z + 0);
                        tess.addVertex(x + 0, y + 0.01, z + 1);

                        if (!world.isSolid(x, y + 1, z))
                        {
                            if (world.isWire(x - 1, y + 1, z))
                            {
                                tess.addVertex(x + 0.01, y + 1, z + 1);
                                tess.addVertex(x + 0.01, y + 0, z + 1);
                                tess.addVertex(x + 0.01, y + 0, z + 0);
                                tess.addVertex(x + 0.01, y + 1, z + 0);
                            }

                            if (world.isWire(x + 1, y + 1, z))
                            {
                                tess.addVertex(x + 0.99, y + 0, z + 1);
                                tess.addVertex(x + 0.99, y + 1, z + 1);
                                tess.addVertex(x + 0.99, y + 1, z + 0);
                                tess.addVertex(x + 0.99, y + 0, z + 0);
                            }

                            if (world.isWire(x, y + 1, z - 1))
                            {
                                tess.addVertex(x + 1, y + 0, z + 0.01);
                                tess.addVertex(x + 1, y + 1, z + 0.01);
                                tess.addVertex(x + 0, y + 1, z + 0.01);
                                tess.addVertex(x + 0, y + 0, z + 0.01);
                            }

                            if (world.isWire(x, y + 1, z + 1))
                            {
                                tess.addVertex(x + 1, y + 1, z + 0.99);
                                tess.addVertex(x + 1, y + 0, z + 0.99);
                                tess.addVertex(x + 0, y + 0, z + 0.99);
                                tess.addVertex(x + 0, y + 1, z + 0.99);
                            }
                        }
                    } else if (block == 1 || block == 3 || block == 4) {
                    	double xmin = x + aabb.minX;
                        double xmax = x + aabb.maxX;
                        double ymin = y + aabb.minY;
                        double ymax = y + aabb.maxY;
                        double zmin = z + aabb.minZ;
                        double zmax = z + aabb.maxZ;

                        tess.setColor_I(getColor(block, meta, x, y, z, 2));
                        if (!world.isSolid(x, y - 1, z) || aabb.minY > 0.0D)
                        {
                        	tess.addVertex(xmin, ymin, zmax);
                            tess.addVertex(xmin, ymin, zmin);
                            tess.addVertex(xmax, ymin, zmin);
                            tess.addVertex(xmax, ymin, zmax);
                        }

                        tess.setColor_I(getColor(block, meta, x, y, z, 0));
                        if (!world.isSolid(x, y + 1, z) || aabb.maxY < 1.0D)
                        {
                        	tess.addVertex(xmax, ymax, zmax);
                            tess.addVertex(xmax, ymax, zmin);
                            tess.addVertex(xmin, ymax, zmin);
                            tess.addVertex(xmin, ymax, zmax);
                        }

                        tess.setColor_I(getColor(block, meta, x, y, z, 1));
                        if (!world.isSolid(x, y, z - 1) || aabb.minZ > 0.0D)
                        {
                        	tess.addVertex(xmin, ymax, zmin);
                            tess.addVertex(xmax, ymax, zmin);
                            tess.addVertex(xmax, ymin, zmin);
                            tess.addVertex(xmin, ymin, zmin);
                        }

                        if (!world.isSolid(x, y, z + 1) || aabb.maxZ < 1.0D)
                        {
                        	tess.addVertex(xmin, ymax, zmax);
                            tess.addVertex(xmin, ymin, zmax);
                            tess.addVertex(xmax, ymin, zmax);
                            tess.addVertex(xmax, ymax, zmax);
                        }

                        if (!world.isSolid(x - 1, y, z) || aabb.minX > 0.0D)
                        {
                        	tess.addVertex(xmin, ymax, zmax);
                            tess.addVertex(xmin, ymax, zmin);
                            tess.addVertex(xmin, ymin, zmin);
                            tess.addVertex(xmin, ymin, zmax);
                        }

                        if (!world.isSolid(x + 1, y, z) || aabb.maxX < 1.0D)
                        {
                        	tess.addVertex(xmax, ymin, zmax);
                            tess.addVertex(xmax, ymin, zmin);
                            tess.addVertex(xmax, ymax, zmin);
                            tess.addVertex(xmax, ymax, zmax);
                        }
                    }
                }
            }
        }

        tessellator.draw();
        GL11.glPopMatrix();
        GL11.glEndList();
        tessellator.setTranslation(0.0D, 0.0D, 0.0D);
        
        return this;
    }
    
    private static int getColor(int block, int meta, int x, int y, int z, int said)
    {
    	int scale[] = {0, 136, 181, 204, 218, 227, 233, 238, 242, 245, 247, 249, 251, 253, 254, 255};
    	
    	switch (block)
    	{
    	case 1: return (said == 0 ? 0xFF606060 : said == 1 ? 0xFF505050 : 0xFF404040) | ((x + y + z) % 2 == 0 ? 0x060000 : 0x000006);
    	case 2: return 0xFF000000 | (0x101 * scale[meta]);
    	case 3: return (meta & 8) > 0 ? 0xFFEE39E4 : 0xFF701B6C;
    	case 4: return (meta & 8) > 0 ? 0xFFE91A64 : 0xFF5B0A27;
    	default: return 0xFF000000;
    	}
    }

    public double quadranceToPlayer(EntityPlayer player)
    {
        double dx = player.getPosX() - this.chunkX * 16D - 8D;
        double dy = player.getPosY() - this.chunkY * 16D - 8D;
        double dz = player.getPosZ() - this.chunkZ * 16D - 8D;
        return dx * dx + dy * dy + dz * dz;
    }
    
    public int getGLCallList()
    {
    	return this.glRenderList;
    }
}
