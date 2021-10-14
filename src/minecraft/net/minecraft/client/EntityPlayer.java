package net.minecraft.client;

import org.lwjgl.input.Mouse;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.KeyBinding;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class EntityPlayer
{
    /** Entity position X */
    private double posX;

    /** Entity position Y */
    private double posY;

    /** Entity position Z */
    private double posZ;
	private final World worldServer;
    private double prevPosX;
    private double prevPosY;
    private double prevPosZ;

    /** Entity rotation Yaw */
    private double rotationYaw;

    /** Entity rotation Pitch */
    private double rotationPitch;

    public EntityPlayer(World worldServ)
    {
    	this.worldServer = worldServ;
        this.prevPosX = this.posX = this.prevPosZ = this.posZ = 0;
        this.rotationYaw = this.rotationPitch = 0;
        
        this.posY = 0.5D;
        
        while (this.worldServer.isSolid(0, MathHelper.floor_double(this.posY), 0))
        {
            ++this.posY;
        }
        
        this.prevPosY = this.posY;
    }
    
    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
    	double motionX = 0.0D;
    	double motionY = 0.0D;
    	double motionZ = 0.0D;
        
        double s = 0.5D * MathHelper.sin(this.rotationYaw * Math.PI / 180.0D);
        double c = 0.5D * MathHelper.cos(this.rotationYaw * Math.PI / 180.0D);

        if (KeyBinding.keyBindForward.getIsKeyPressed())
        {
            motionX -= s;
            motionZ += c;
        }

        if (KeyBinding.keyBindBack.getIsKeyPressed())
        {
            motionX += s;
            motionZ -= c;
        }

        if (KeyBinding.keyBindLeft.getIsKeyPressed())
        {
            motionX += c;
            motionZ += s;
        }

        if (KeyBinding.keyBindRight.getIsKeyPressed())
        {
        	motionX -= c;
            motionZ -= s;
        }
    	
        if (KeyBinding.keyBindSneak.getIsKeyPressed())
        {
            motionY -= 0.5D;
        }

        if (KeyBinding.keyBindJump.getIsKeyPressed())
        {
            motionY += 0.5D;
        }
        
        this.prevPosX = this.posX;
    	this.prevPosY = this.posY;
    	this.prevPosZ = this.posZ;
        
        if (this.worldServer.isSolid(MathHelper.floor_double(this.posX),
        		                     MathHelper.floor_double(this.posY),
        		                     MathHelper.floor_double(this.posZ)) ||
           !this.worldServer.isSolid(MathHelper.floor_double(this.posX + motionX),
                		             MathHelper.floor_double(this.posY),
                		             MathHelper.floor_double(this.posZ)))
        {
        	this.posX += motionX;
        }
        
        if (this.worldServer.isSolid(MathHelper.floor_double(this.posX),
                                     MathHelper.floor_double(this.posY),
                                     MathHelper.floor_double(this.posZ)) ||
           !this.worldServer.isSolid(MathHelper.floor_double(this.posX),
	                                 MathHelper.floor_double(this.posY + motionY),
	                                 MathHelper.floor_double(this.posZ)))
        {
        	this.posY += motionY;
        }
        
        if (this.worldServer.isSolid(MathHelper.floor_double(this.posX),
                                     MathHelper.floor_double(this.posY),
                                     MathHelper.floor_double(this.posZ)) ||
           !this.worldServer.isSolid(MathHelper.floor_double(this.posX),
                                     MathHelper.floor_double(this.posY),
                                     MathHelper.floor_double(this.posZ + motionZ)))
        {
        	this.posZ += motionZ;
        }
    }
    
    public double getPosX()
    {
    	return this.posX;
    }
    
    public double getPosY()
    {
    	return this.posY;
    }
    
    public double getPosZ()
    {
    	return this.posZ;
    }
    
    public Vec3 pttPos(double ptt)
    {
    	return new Vec3(this.prevPosX + (this.posX - this.prevPosX) * ptt,
    			        this.prevPosY + (this.posY - this.prevPosY) * ptt,
    			        this.prevPosZ + (this.posZ - this.prevPosZ) * ptt);
    }
    
    public double getRotationYaw()
    {
    	return this.rotationYaw;
    }
    
    public double getRotationPitch()
    {
    	return this.rotationPitch;
    }
    
    public int getChunkCoordX()
    {
    	return MathHelper.floor_double(this.posX / 16.0D);
    }
    
    public int getChunkCoordY()
    {
    	return MathHelper.floor_double(this.posY / 16.0D);
    }
    
    public int getChunkCoordZ()
    {
    	return MathHelper.floor_double(this.posZ / 16.0D);
    }

    public void setAngles()
    {
        this.rotationYaw += Mouse.getDX() * 0.15D;
        this.rotationPitch -= Mouse.getDY() * 0.15D;

        if (this.rotationPitch < -90.0F)
        {
            this.rotationPitch = -90.0F;
        }

        if (this.rotationPitch > 90.0F)
        {
            this.rotationPitch = 90.0F;
        }
    }

    /**
     * Pure function. Performs a ray trace for the distance specified and using the partial tick time. Args: distance, partialTickTime
     */
    public MovingObjectPosition rayTrace8()
    {
        Vec3 playerPos = new Vec3(this.posX, this.posY, this.posZ);
        double cy = MathHelper.cos(-this.rotationYaw * 0.017453292D - Math.PI);
        double sy = MathHelper.sin(-this.rotationYaw * 0.017453292D - Math.PI);
        double cp = -MathHelper.cos(-this.rotationPitch * 0.017453292D);
        double sp = MathHelper.sin(-this.rotationPitch * 0.017453292D);
        Vec3 viewVec = playerPos.addVector(sy * cp * 8, sp * 8, cy * cp * 8);
        if (!Double.isNaN(playerPos.x) && !Double.isNaN(playerPos.y) && !Double.isNaN(playerPos.z)
         && !Double.isNaN(  viewVec.x) && !Double.isNaN(  viewVec.y) && !Double.isNaN(  viewVec.z))
        {
        	int viewBlockX = MathHelper.floor_double(viewVec.x);
            int viewBlockY = MathHelper.floor_double(viewVec.y);
            int viewBlockZ = MathHelper.floor_double(viewVec.z);
            int currentBlockX = MathHelper.floor_double(playerPos.x);
            int currentBlockY = MathHelper.floor_double(playerPos.y);
            int currentBlockZ = MathHelper.floor_double(playerPos.z);

            if (!this.worldServer.isAir(currentBlockX, currentBlockY, currentBlockZ))
            {
                MovingObjectPosition hit = this.collisionRayTrace(currentBlockX, currentBlockY, currentBlockZ, playerPos, viewVec);

                if (hit != null)
                {
                    return hit;
                }
            }

            for (int i = 0; i <= 200; i++)
            {
                if (Double.isNaN(playerPos.x) || Double.isNaN(playerPos.y) || Double.isNaN(playerPos.z) || (currentBlockX == viewBlockX && currentBlockY == viewBlockY && currentBlockZ == viewBlockZ))
                {
                    return null;
                }

                boolean var41 = true;
                boolean var15 = true;
                boolean var16 = true;
                double var17 = 999.0D;
                double var19 = 999.0D;
                double var21 = 999.0D;

                if (viewBlockX > currentBlockX)
                {
                    var17 = (double)currentBlockX + 1.0D;
                }
                else if (viewBlockX < currentBlockX)
                {
                    var17 = (double)currentBlockX + 0.0D;
                }
                else
                {
                    var41 = false;
                }

                if (viewBlockY > currentBlockY)
                {
                    var19 = (double)currentBlockY + 1.0D;
                }
                else if (viewBlockY < currentBlockY)
                {
                    var19 = (double)currentBlockY + 0.0D;
                }
                else
                {
                    var15 = false;
                }

                if (viewBlockZ > currentBlockZ)
                {
                    var21 = (double)currentBlockZ + 1.0D;
                }
                else if (viewBlockZ < currentBlockZ)
                {
                    var21 = (double)currentBlockZ + 0.0D;
                }
                else
                {
                    var16 = false;
                }

                double var23 = 999.0D;
                double var25 = 999.0D;
                double var27 = 999.0D;
                double var29 = viewVec.x - playerPos.x;
                double var31 = viewVec.y - playerPos.y;
                double var33 = viewVec.z - playerPos.z;

                if (var41)
                {
                    var23 = (var17 - playerPos.x) / var29;
                }

                if (var15)
                {
                    var25 = (var19 - playerPos.y) / var31;
                }

                if (var16)
                {
                    var27 = (var21 - playerPos.z) / var33;
                }

                byte var42;

                if (var23 < var25 && var23 < var27)
                {
                    if (viewBlockX > currentBlockX)
                    {
                        var42 = 4;
                    }
                    else
                    {
                        var42 = 5;
                    }

                    playerPos.x = var17;
                    playerPos.y += var31 * var23;
                    playerPos.z += var33 * var23;
                }
                else if (var25 < var27)
                {
                    if (viewBlockY > currentBlockY)
                    {
                        var42 = 0;
                    }
                    else
                    {
                        var42 = 1;
                    }

                    playerPos.x += var29 * var25;
                    playerPos.y = var19;
                    playerPos.z += var33 * var25;
                }
                else
                {
                    if (viewBlockZ > currentBlockZ)
                    {
                        var42 = 2;
                    }
                    else
                    {
                        var42 = 3;
                    }

                    playerPos.x += var29 * var27;
                    playerPos.y += var31 * var27;
                    playerPos.z = var21;
                }

                Vec3 var36 = new Vec3(playerPos.x, playerPos.y, playerPos.z);
                currentBlockX = (int)(var36.x = (double)MathHelper.floor_double(playerPos.x));

                if (var42 == 5)
                {
                    --currentBlockX;
                    ++var36.x;
                }

                currentBlockY = (int)(var36.y = (double)MathHelper.floor_double(playerPos.y));

                if (var42 == 1)
                {
                    --currentBlockY;
                    ++var36.y;
                }

                currentBlockZ = (int)(var36.z = (double)MathHelper.floor_double(playerPos.z));

                if (var42 == 3)
                {
                    --currentBlockZ;
                    ++var36.z;
                }

                if (!this.worldServer.isAir(currentBlockX, currentBlockY, currentBlockZ))
                {
                    MovingObjectPosition hit = this.collisionRayTrace(currentBlockX, currentBlockY, currentBlockZ, playerPos, viewVec);

                    if (hit != null)
                    {
                        return hit;
                    }
                }
            }
        }
        
        return null;
    }
    
    private MovingObjectPosition collisionRayTrace(int x, int y, int z, Vec3 playerPos, Vec3 playerLook)
    {
    	AxisAlignedBB aabb = this.worldServer.getBlock(x, y, z).generateCubicBoundingBox(this.worldServer.getBlockMetadata(x, y, z));
        playerPos = playerPos.addVector(-x, -y, -z);
        playerLook = playerLook.addVector(-x, -y, -z);
        Vec3 nx = playerPos.getIntermediateWithXValue(playerLook, aabb.minX);
        Vec3 xx = playerPos.getIntermediateWithXValue(playerLook, aabb.maxX);
        Vec3 ny = playerPos.getIntermediateWithYValue(playerLook, aabb.minY);
        Vec3 xy = playerPos.getIntermediateWithYValue(playerLook, aabb.maxY);
        Vec3 nz = playerPos.getIntermediateWithZValue(playerLook, aabb.minZ);
        Vec3 xz = playerPos.getIntermediateWithZValue(playerLook, aabb.maxZ);

        Vec3 closest = null;
        byte side = -1;

        if (nx != null && aabb.contains(nx) && (closest == null || playerPos.quadranceTo(nx) < playerPos.quadranceTo(closest)))
        {
            closest = nx;
            side = 4;
        }

        if (xx != null && aabb.contains(xx) && (closest == null || playerPos.quadranceTo(xx) < playerPos.quadranceTo(closest)))
        {
            closest = xx;
            side = 5;
        }

        if (ny != null && aabb.contains(ny) && (closest == null || playerPos.quadranceTo(ny) < playerPos.quadranceTo(closest)))
        {
            closest = ny;
            side = 0;
        }

        if (xy != null && aabb.contains(xy) && (closest == null || playerPos.quadranceTo(xy) < playerPos.quadranceTo(closest)))
        {
            closest = xy;
            side = 1;
        }

        if (nz != null && aabb.contains(nz) && (closest == null || playerPos.quadranceTo(nz) < playerPos.quadranceTo(closest)))
        {
            closest = nz;
            side = 2;
        }

        if (xz != null && aabb.contains(xz) && (closest == null || playerPos.quadranceTo(xz) < playerPos.quadranceTo(closest)))
        {
            closest = xz;
            side = 3;
        }

        return side == -1 ? null : new MovingObjectPosition(x, y, z, side);
    }
}
