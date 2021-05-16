package net.minecraft.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.KeyBinding;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;

public class EntityPlayer
{
    /** Entity position X */
    private double posX;

    /** Entity position Y */
    private double posY;

    /** Entity position Z */
    private double posZ;
	private final WorldServer worldServer;
    
    private double oldPosX;

    /** Old Minimum Y of the bounding box */
    private double oldMinY;
    private double oldPosZ;
    private double oldRotationYaw;
    private double oldRotationPitch;

    /** Entity motion X */
    private double motionX;

    /** Entity motion Y */
    private double motionY;

    /** Entity motion Z */
    private double motionZ;

    /**
     * Counter used to ensure that the server sends a move packet (Packet11, 12 or 13) to the client at least once a
     * second.
     */
    private int ticksSinceMovePacket;

    /** Axis aligned bounding box. */
    private AxisAlignedBB boundingBox;

    /** How wide this entity is considered to be */
    private final double width = 0.6F;
    private double prevPosX;
    private double prevPosY;
    private double prevPosZ;

    /** Entity rotation Yaw */
    private double rotationYaw;

    /** Entity rotation Pitch */
    private double rotationPitch;
    private double prevRotationYaw;
    private double prevRotationPitch;

    public EntityPlayer(WorldServer worldServ)
    {
    	this.rotationYaw = (float)(Math.random() * Math.PI * 2.0D);
        this.worldServer = worldServ;
        this.boundingBox = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        this.motionX = this.motionY = this.motionZ = 0;
        this.prevPosX = this.posX = this.prevPosZ = this.posZ = 0;
        this.prevPosY = this.posY = this.worldServer.getTopBlockAtSpawn() + 1.6200000047683716D;
        this.prevRotationYaw = this.rotationYaw = this.prevRotationPitch = this.rotationPitch = 0;
        
        this.boundingBox = new AxisAlignedBB(-this.width/2.0D, this.posY - 1.62D, -this.width/2.0D, this.width/2.0D, this.posY + 0.18D, this.width/2.0D);
        
        while (!this.getCollidingBoundingBoxes(this.boundingBox).isEmpty())
        {
            ++this.posY;
            this.boundingBox = new AxisAlignedBB(-this.width/2.0D, this.posY - 1.62D, -this.width/2.0D, this.width/2.0D, this.posY + 0.18D, this.width/2.0D);
        }
    }
    
    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
    	this.prevPosX = this.posX;
    	this.prevPosY = this.posY;
    	this.prevPosZ = this.posZ;
    	this.prevRotationYaw = this.rotationYaw;
    	this.prevRotationPitch = this.rotationPitch;
        
        float strafe = 0.0F;
    	float forward = 0.0F;

        if (KeyBinding.keyBindForward.getIsKeyPressed())
        {
            ++forward;
        }

        if (KeyBinding.keyBindBack.getIsKeyPressed())
        {
            --forward;
        }

        if (KeyBinding.keyBindLeft.getIsKeyPressed())
        {
            ++strafe;
        }

        if (KeyBinding.keyBindRight.getIsKeyPressed())
        {
            --strafe;
        }
        
        strafe *= 0.98F;
        forward *= 0.98F;

        this.pushPlayerOutOfBlock(this.posX - this.width * 0.35D, this.boundingBox.minY + 0.5D, this.posZ + this.width * 0.35D);
        this.pushPlayerOutOfBlock(this.posX - this.width * 0.35D, this.boundingBox.minY + 0.5D, this.posZ - this.width * 0.35D);
        this.pushPlayerOutOfBlock(this.posX + this.width * 0.35D, this.boundingBox.minY + 0.5D, this.posZ - this.width * 0.35D);
        this.pushPlayerOutOfBlock(this.posX + this.width * 0.35D, this.boundingBox.minY + 0.5D, this.posZ + this.width * 0.35D);

        if (KeyBinding.keyBindSneak.getIsKeyPressed())
        {
            this.motionY -= 0.15D;
        }

        if (KeyBinding.keyBindJump.getIsKeyPressed())
        {
            this.motionY += 0.15D;
        }

        if (Math.abs(this.motionX) < 0.005D)
        {
            this.motionX = 0.0D;
        }

        if (Math.abs(this.motionY) < 0.005D)
        {
            this.motionY = 0.0D;
        }

        if (Math.abs(this.motionZ) < 0.005D)
        {
            this.motionZ = 0.0D;
        }
    	double xMov = this.motionX;
    	double yMov = this.motionY;
    	double zMov = this.motionZ;
    	
    	float magnitude = strafe * strafe + forward * forward;

        if (magnitude >= 1.0E-4F)
        {
            magnitude = (float)Math.sqrt(magnitude);

            if (magnitude < 1.0F)
            {
                magnitude = 1.0F;
            }

            magnitude = 0.41999998688697815F / magnitude;
            strafe *= magnitude;
            forward *= magnitude;
            double var5 = MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F);
            double var6 = MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F);
            this.motionX += strafe * var6 - forward * var5;
            this.motionZ += forward * var6 + strafe * var5;
        }

        List<Becktor> becs = this.getCollidingBoundingBoxes(this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ));

        for (Becktor bec : becs)
        {
        	this.motionY = bec.calculateYOffset(this.boundingBox, this.motionY);
        }

        this.boundingBox = this.boundingBox.offset(0.0D, this.motionY, 0.0D);

        for (Becktor bec : becs)
        {
        	this.motionX = bec.calculateXOffset(this.boundingBox, this.motionX);
        }

        this.boundingBox = this.boundingBox.offset(this.motionX, 0.0D, 0.0D);

        for (Becktor bec : becs)
        {
        	this.motionZ = bec.calculateZOffset(this.boundingBox, this.motionZ);
        }

        this.boundingBox = this.boundingBox.offset(0.0D, 0.0D, this.motionZ);
        
        this.posX = (this.boundingBox.minX + this.boundingBox.maxX) / 2.0D;
        this.posY = this.boundingBox.minY + 1.62D;
        this.posZ = (this.boundingBox.minZ + this.boundingBox.maxZ) / 2.0D;
        
        this.motionX = xMov * 0.6D;
        this.motionY = yMov * 0.6D;
        this.motionZ = zMov * 0.6D;

        while (this.rotationYaw - this.prevRotationYaw < -180.0F)
        {
            this.prevRotationYaw -= 360.0F;
        }

        while (this.rotationYaw - this.prevRotationYaw >= 180.0F)
        {
            this.prevRotationYaw += 360.0F;
        }

        while (this.rotationPitch - this.prevRotationPitch < -180.0F)
        {
            this.prevRotationPitch -= 360.0F;
        }

        while (this.rotationPitch - this.prevRotationPitch >= 180.0F)
        {
            this.prevRotationPitch += 360.0F;
        }
        
        double dx = this.posX - this.oldPosX;
        double dy = this.boundingBox.minY - this.oldMinY;
        double dz = this.posZ - this.oldPosZ;
        double dyaw = (double)(this.rotationYaw - this.oldRotationYaw);
        double dpitch = (double)(this.rotationPitch - this.oldRotationPitch);
        boolean hasMoved = dx * dx + dy * dy + dz * dz > 9.0E-4D || this.ticksSinceMovePacket >= 20;
        boolean hasTurned = dyaw != 0.0D || dpitch != 0.0D;

        if (hasMoved)
        {
        	this.worldServer.updateMountedMovingPlayer(this.posX, this.posZ);
        }

        ++this.ticksSinceMovePacket;

        if (hasMoved)
        {
            this.oldPosX = this.posX;
            this.oldMinY = this.boundingBox.minY;
            this.oldPosZ = this.posZ;
            this.ticksSinceMovePacket = 0;
        }

        if (hasTurned)
        {
            this.oldRotationYaw = this.rotationYaw;
            this.oldRotationPitch = this.rotationPitch;
        }

        if (Double.isNaN(this.posX) || Double.isInfinite(this.posX))
        {
        	this.posX = this.prevPosX;
        }

        if (Double.isNaN(this.posY) || Double.isInfinite(this.posY))
        {
        	this.posY = this.prevPosY;
        }

        if (Double.isNaN(this.posZ) || Double.isInfinite(this.posZ))
        {
        	this.posZ = this.prevPosZ;
        }

        if (Double.isNaN((double)this.rotationPitch) || Double.isInfinite((double)this.rotationPitch))
        {
        	this.rotationPitch = this.prevRotationPitch;
        }

        if (Double.isNaN((double)this.rotationYaw) || Double.isInfinite((double)this.rotationYaw))
        {
        	this.rotationYaw = this.prevRotationYaw;
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
    
    public double getPartialRotationYaw(double ptt)
    {
    	return this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * ptt;
    }
    
    public double getPartialRotationPitch(double ptt)
    {
    	return this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * ptt;
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

    /**
     * Adds par1*0.15 to the entity's yaw, and *subtracts* par2*0.15 from the pitch. Clamps pitch from -90 to 90. Both
     * arguments in degrees.
     */
    public void setAngles(double dYaw, double dPitch)
    {
    	double oldPitch = this.rotationPitch;
    	double oldYaw = this.rotationYaw;
        this.rotationYaw = (float)((double)this.rotationYaw + dYaw * 0.15D);
        this.rotationPitch = (float)((double)this.rotationPitch - dPitch * 0.15D);

        if (this.rotationPitch < -90.0F)
        {
            this.rotationPitch = -90.0F;
        }

        if (this.rotationPitch > 90.0F)
        {
            this.rotationPitch = 90.0F;
        }

        this.prevRotationPitch += this.rotationPitch - oldPitch;
        this.prevRotationYaw += this.rotationYaw - oldYaw;
    }

    private boolean isBlockSolid(int x, int y, int z)
    {
        return this.worldServer.getBlock(x, y, z).isSolid();
    }

    private void pushPlayerOutOfBlock(double xpos, double ypos, double zpos)
    {
        int x = MathHelper.floor_double(xpos);
        int y = MathHelper.floor_double(ypos);
        int z = MathHelper.floor_double(zpos);
        double fracx = xpos - (double)x;
        double fracz = zpos - (double)z;

        if (this.isBlockSolid(x, y, z) || this.isBlockSolid(x, y + 1, z))
        {
            boolean minusXClear = !this.isBlockSolid(x - 1, y, z) && !this.isBlockSolid(x - 1, y + 1, z);
            boolean plusXClear = !this.isBlockSolid(x + 1, y, z) && !this.isBlockSolid(x + 1, y + 1, z);
            boolean minusZClear = !this.isBlockSolid(x, y, z - 1) && !this.isBlockSolid(x, y + 1, z - 1);
            boolean plusZClear = !this.isBlockSolid(x, y, z + 1) && !this.isBlockSolid(x, y + 1, z + 1);
            byte direction = -1;
            double smallestDistance = 9999.0D;

            if (minusXClear && fracx < smallestDistance)
            {
                smallestDistance = fracx;
                direction = 0;
            }

            if (plusXClear && 1.0D - fracx < smallestDistance)
            {
                smallestDistance = 1.0D - fracx;
                direction = 1;
            }

            if (minusZClear && fracz < smallestDistance)
            {
                smallestDistance = fracz;
                direction = 4;
            }

            if (plusZClear && 1.0D - fracz < smallestDistance)
            {
                smallestDistance = 1.0D - fracz;
                direction = 5;
            }

            if (direction == 0)
            {
                this.motionX = -0.1D;
            }

            if (direction == 1)
            {
                this.motionX = 0.1D;
            }

            if (direction == 4)
            {
                this.motionZ = -0.1D;
            }

            if (direction == 5)
            {
                this.motionZ = 0.1D;
            }
        }
    }

    /**
     * Pure function. Performs a ray trace for the distance specified and using the partial tick time. Args: distance, partialTickTime
     */
    public MovingObjectPosition rayTrace8(double ptt)
    {
        Vec3 playerPos = this.pttPos(ptt);
        double pitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * ptt;
        double yaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * ptt;
        double cy = MathHelper.cos(-yaw * 0.017453292D - Math.PI);
        double sy = MathHelper.sin(-yaw * 0.017453292D - Math.PI);
        double cp = -MathHelper.cos(-pitch * 0.017453292D);
        double sp = MathHelper.sin(-pitch * 0.017453292D);
        Vec3 viewVec = playerPos.addVector(sy * cp * 8, sp * 8, cy * cp * 8);
        if (!Double.isNaN(playerPos.x) && !Double.isNaN(playerPos.y) && !Double.isNaN(playerPos.z)
         && !Double.isNaN(  viewVec.x) && !Double.isNaN(  viewVec.y) && !Double.isNaN(viewVec.z))
        {
        	int viewBlockX = MathHelper.floor_double(viewVec.x);
            int viewBlockY = MathHelper.floor_double(viewVec.y);
            int viewBlockZ = MathHelper.floor_double(viewVec.z);
            int playerBlockX = MathHelper.floor_double(playerPos.x);
            int playerBlockY = MathHelper.floor_double(playerPos.y);
            int playerBlockZ = MathHelper.floor_double(playerPos.z);
            Block playerBlock = this.worldServer.getBlock(playerBlockX, playerBlockY, playerBlockZ);

            if (!playerBlock.isReplaceable())
            {
                MovingObjectPosition playerBlockPos = playerBlock.collisionRayTrace(this.worldServer, playerBlockX, playerBlockY, playerBlockZ, playerPos, viewVec);

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

                Block var37 = this.worldServer.getBlock(playerBlockX, playerBlockY, playerBlockZ);

                if (!var37.isReplaceable())
                {
                    MovingObjectPosition var39 = var37.collisionRayTrace(this.worldServer, playerBlockX, playerBlockY, playerBlockZ, playerPos, viewVec);

                    if (var39 != null)
                    {
                        return var39;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Pure function. Returns a list of bounding boxes that collide with aabb excluding the passed in entity's collision. Args: entity,
     * aabb
     */
    private List<Becktor> getCollidingBoundingBoxes(AxisAlignedBB aabb)
    {
        List<Becktor> collidingBoundingBoxes = new ArrayList<Becktor>();
        int minx = MathHelper.floor_double(aabb.minX);
        int maxx = MathHelper.floor_double(aabb.maxX);
        int miny = MathHelper.floor_double(aabb.minY);
        int maxy = MathHelper.floor_double(aabb.maxY);
        int minz = MathHelper.floor_double(aabb.minZ);
        int maxz = MathHelper.floor_double(aabb.maxZ);

        for (int x = minx; x <= maxx; ++x)
        {
            for (int z = minz; z <= maxz; ++z)
            {
            	for (int y = miny - 1; y <= maxy; ++y)
                {
                    if (this.worldServer.getBlock(x, y, z) == Block.stone)
                    {
                        if (aabb.intersectsWith(x, y, z)) collidingBoundingBoxes.add(new Becktor(x, y, z));
                    }
                }
            }
        }

        return collidingBoundingBoxes;
    }
    
    private class Becktor
    {
    	private final int x;
    	private final int y;
    	private final int z;
    	
    	public Becktor(int xval, int yval, int zval)
    	{
    		this.x = xval;
    		this.y = yval;
    		this.z = zval;
    	}
    	


        /**
         * if instance and the argument bounding boxes overlap in the Y and Z dimensions, calculate the offset between them
         * in the X dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
         * calculated offset.  Otherwise return the calculated offset.
         */
        public double calculateXOffset(AxisAlignedBB other, double ret)
        {
            if (other.maxY > y && other.minY < y + 1 && other.maxZ > z && other.minZ < z + 1)
            {
            	if (ret > 0.0D && other.maxX <= x)
                {
                	double var4 = x - other.maxX;

                    if (var4 < ret)
                    {
                        ret = var4;
                    }
                }

                if (ret < 0.0D && other.minX >= x + 1)
                {
                	double var4 = x + 1 - other.minX;

                    if (var4 > ret)
                    {
                        ret = var4;
                    }
                }
            }
            
            return ret;
        }

        /**
         * if instance and the argument bounding boxes overlap in the X and Z dimensions, calculate the offset between them
         * in the Y dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
         * calculated offset.  Otherwise return the calculated offset.
         */
        public double calculateYOffset(AxisAlignedBB other, double ret)
        {
            if (other.maxX > x && other.minX < x + 1)
            {
                if (other.maxZ > z && other.minZ < z + 1)
                {
                    double var4;

                    if (ret > 0.0D && other.maxY <= y)
                    {
                        var4 = y - other.maxY;

                        if (var4 < ret)
                        {
                            ret = var4;
                        }
                    }

                    if (ret < 0.0D && other.minY >= y + 1)
                    {
                        var4 = y + 1 - other.minY;

                        if (var4 > ret)
                        {
                            ret = var4;
                        }
                    }

                    return ret;
                }
                else
                {
                    return ret;
                }
            }
            else
            {
                return ret;
            }
        }

        /**
         * if instance and the argument bounding boxes overlap in the Y and X dimensions, calculate the offset between them
         * in the Z dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
         * calculated offset.  Otherwise return the calculated offset.
         */
        public double calculateZOffset(AxisAlignedBB other, double ret)
        {
            if (other.maxX > x && other.minX < x + 1)
            {
                if (other.maxY > y && other.minY < y + 1)
                {
                    double var4;

                    if (ret > 0.0D && other.maxZ <= z)
                    {
                        var4 = z - other.maxZ;

                        if (var4 < ret)
                        {
                            ret = var4;
                        }
                    }

                    if (ret < 0.0D && other.minZ >= z + 1)
                    {
                        var4 = z + 1 - other.minZ;

                        if (var4 > ret)
                        {
                            ret = var4;
                        }
                    }

                    return ret;
                }
                else
                {
                    return ret;
                }
            }
            else
            {
                return ret;
            }
        }
    }
}
