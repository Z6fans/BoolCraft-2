package net.minecraft.client;

import org.lwjgl.input.Mouse;

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
        Vec3 playerPos = this.pttPos(1.0F);
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
            int playerBlockX = MathHelper.floor_double(playerPos.x);
            int playerBlockY = MathHelper.floor_double(playerPos.y);
            int playerBlockZ = MathHelper.floor_double(playerPos.z);

            if (!this.worldServer.isReplaceable(playerBlockX, playerBlockY, playerBlockZ))
            {
                MovingObjectPosition playerBlockPos = this.worldServer.getBlock(playerBlockX, playerBlockY, playerBlockZ).collisionRayTrace(this.worldServer, playerBlockX, playerBlockY, playerBlockZ, playerPos, viewVec);

                if (playerBlockPos != null)
                {
                    return playerBlockPos;
                }
            }

            for (int i = 0; i <= 200; i++)
            {
                if (Double.isNaN(playerPos.x) || Double.isNaN(playerPos.y) || Double.isNaN(playerPos.z) || (playerBlockX == viewBlockX && playerBlockY == viewBlockY && playerBlockZ == viewBlockZ))
                {
                    return null;
                }

                boolean var41 = true;
                boolean var15 = true;
                boolean var16 = true;
                double var17 = 999.0D;
                double var19 = 999.0D;
                double var21 = 999.0D;

                if (viewBlockX > playerBlockX)
                {
                    var17 = (double)playerBlockX + 1.0D;
                }
                else if (viewBlockX < playerBlockX)
                {
                    var17 = (double)playerBlockX + 0.0D;
                }
                else
                {
                    var41 = false;
                }

                if (viewBlockY > playerBlockY)
                {
                    var19 = (double)playerBlockY + 1.0D;
                }
                else if (viewBlockY < playerBlockY)
                {
                    var19 = (double)playerBlockY + 0.0D;
                }
                else
                {
                    var15 = false;
                }

                if (viewBlockZ > playerBlockZ)
                {
                    var21 = (double)playerBlockZ + 1.0D;
                }
                else if (viewBlockZ < playerBlockZ)
                {
                    var21 = (double)playerBlockZ + 0.0D;
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
                    if (viewBlockX > playerBlockX)
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
                    if (viewBlockY > playerBlockY)
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
                    if (viewBlockZ > playerBlockZ)
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
                playerBlockX = (int)(var36.x = (double)MathHelper.floor_double(playerPos.x));

                if (var42 == 5)
                {
                    --playerBlockX;
                    ++var36.x;
                }

                playerBlockY = (int)(var36.y = (double)MathHelper.floor_double(playerPos.y));

                if (var42 == 1)
                {
                    --playerBlockY;
                    ++var36.y;
                }

                playerBlockZ = (int)(var36.z = (double)MathHelper.floor_double(playerPos.z));

                if (var42 == 3)
                {
                    --playerBlockZ;
                    ++var36.z;
                }

                if (!this.worldServer.isReplaceable(playerBlockX, playerBlockY, playerBlockZ))
                {
                    MovingObjectPosition var39 = this.worldServer.getBlock(playerBlockX, playerBlockY, playerBlockZ).collisionRayTrace(this.worldServer, playerBlockX, playerBlockY, playerBlockZ, playerPos, viewVec);

                    if (var39 != null)
                    {
                        return var39;
                    }
                }
            }
        }
        
        return null;
    }
}
