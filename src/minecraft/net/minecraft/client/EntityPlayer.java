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
	private final WorldClient worldObj;
	private final WorldServer worldServer;
	private final Minecraft minecraft;
    
    private double oldPosX;

    /** Old Minimum Y of the bounding box */
    private double oldMinY;
    private double oldPosZ;
    private float oldRotationYaw;
    private float oldRotationPitch;

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
    private final AxisAlignedBB boundingBox;

    /** How wide this entity is considered to be */
    private final double width = 0.6F;
    private double prevPosX;
    private double prevPosY;
    private double prevPosZ;

    /** Entity rotation Yaw */
    private float rotationYaw;

    /** Entity rotation Pitch */
    private float rotationPitch;
    private float prevRotationYaw;
    private float prevRotationPitch;

    /**
     * The entity's X coordinate at the previous tick, used to calculate position during rendering routines
     */
    private double lastTickPosX;

    /**
     * The entity's Y coordinate at the previous tick, used to calculate position during rendering routines
     */
    private double lastTickPosY;

    /**
     * The entity's Z coordinate at the previous tick, used to calculate position during rendering routines
     */
    private double lastTickPosZ;

    public EntityPlayer(WorldClient world, Minecraft mc, WorldServer worldServ)
    {
    	this.rotationYaw = (float)(Math.random() * Math.PI * 2.0D);
        this.worldObj = world;
        this.worldServer = worldServ;
        this.minecraft = mc;
        this.boundingBox = AxisAlignedBB.getBoundingBox(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        this.motionX = this.motionY = this.motionZ = 0;
        this.prevPosX = this.posX = this.prevPosZ = this.posZ = 0;
        this.prevPosY = this.posY = this.worldServer.getTopBlockAtSpawn() + 1.6200000047683716D;
        this.prevRotationYaw = this.rotationYaw = this.prevRotationPitch = this.rotationPitch = 0;
        this.minecraft.displayGuiScreenNull();
        
        this.boundingBox.setBounds(-this.width/2.0F, this.posY - (double)this.getYOffset(), -this.width/2.0F, this.width/2.0F, this.posY - (double)this.getYOffset() + 1.8F, this.width/2.0F);
        
        while (!this.getCollidingBoundingBoxes(this.boundingBox).isEmpty())
        {
            ++this.posY;
            this.boundingBox.setBounds(-this.width/2.0F, this.posY - (double)this.getYOffset(), -this.width/2.0F, this.width/2.0F, this.posY - (double)this.getYOffset() + 1.8F, this.width/2.0F);
        }
    }
    
    public float getYOffset()
    {
    	return 1.62F;
    }
    
    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
    	this.lastTickPosX = this.prevPosX = this.posX;
    	this.lastTickPosY = this.prevPosY = this.posY;
    	this.lastTickPosZ = this.prevPosZ = this.posZ;
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

        List<AxisAlignedBB> var36 = this.getCollidingBoundingBoxes(this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ));

        for (int var22 = 0; var22 < var36.size(); ++var22)
        {
        	this.motionY = var36.get(var22).calculateYOffset(this.boundingBox, this.motionY);
        }

        this.boundingBox.offset(0.0D, this.motionY, 0.0D);
        int var23;

        for (var23 = 0; var23 < var36.size(); ++var23)
        {
        	this.motionX = var36.get(var23).calculateXOffset(this.boundingBox, this.motionX);
        }

        this.boundingBox.offset(this.motionX, 0.0D, 0.0D);

        for (var23 = 0; var23 < var36.size(); ++var23)
        {
        	this.motionZ = var36.get(var23).calculateZOffset(this.boundingBox, this.motionZ);
        }

        this.boundingBox.offset(0.0D, 0.0D, this.motionZ);
        this.posX = (this.boundingBox.minX + this.boundingBox.maxX) / 2.0D;
        this.posY = this.boundingBox.minY + (double)this.getYOffset();
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
        	this.posX = this.lastTickPosX;
        }

        if (Double.isNaN(this.posY) || Double.isInfinite(this.posY))
        {
        	this.posY = this.lastTickPosY;
        }

        if (Double.isNaN(this.posZ) || Double.isInfinite(this.posZ))
        {
        	this.posZ = this.lastTickPosZ;
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
    
    public double getPartialPosX(double ptt)
    {
    	return this.lastTickPosX + (this.posX - this.lastTickPosX) * ptt;
    }
    
    public double getPartialPosY(double ptt)
    {
    	return this.lastTickPosY + (this.posY - this.lastTickPosY) * ptt;
    }
    
    public double getPartialPosZ(double ptt)
    {
    	return this.lastTickPosZ + (this.posZ - this.lastTickPosZ) * ptt;
    }
    
    public float getPartialRotationYaw(float ptt)
    {
    	return this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * ptt;
    }
    
    public float getPartialRotationPitch(float ptt)
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
        float oldPitch = this.rotationPitch;
        float oldYaw = this.rotationYaw;
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
        return this.worldObj.getBlock(x, y, z).isSolid();
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
    public MovingObjectPosition rayTrace8(float ptt)
    {
        double x = this.prevPosX + (this.posX - this.prevPosX) * (double)ptt;
        double y = this.prevPosY + (this.posY - this.prevPosY) * (double)ptt;
        double z = this.prevPosZ + (this.posZ - this.prevPosZ) * (double)ptt;
        Vec3 playerPos = Vec3.createVectorHelper(x, y, z);
        Vec3 lookVec = this.getLook(ptt);
        Vec3 viewVec = playerPos.addVector(lookVec.x * 8, lookVec.y * 8, lookVec.z * 8);
        if (!Double.isNaN(playerPos.x) && !Double.isNaN(playerPos.y) && !Double.isNaN(playerPos.z))
        {
            if (!Double.isNaN(viewVec.x) && !Double.isNaN(viewVec.y) && !Double.isNaN(viewVec.z))
            {
                int viewBlockX = MathHelper.floor_double(viewVec.x);
                int viewBlockY = MathHelper.floor_double(viewVec.y);
                int viewBlockZ = MathHelper.floor_double(viewVec.z);
                int playerBlockX = MathHelper.floor_double(playerPos.x);
                int playerBlockY = MathHelper.floor_double(playerPos.y);
                int playerBlockZ = MathHelper.floor_double(playerPos.z);
                Block playerBlock = this.worldObj.getBlock(playerBlockX, playerBlockY, playerBlockZ);

                if (!playerBlock.isReplaceable())
                {
                    MovingObjectPosition playerBlockPos = playerBlock.collisionRayTrace(this.worldObj, playerBlockX, playerBlockY, playerBlockZ, playerPos, viewVec);

                    if (playerBlockPos != null)
                    {
                        return playerBlockPos;
                    }
                }

                int var13 = 200;

                while (var13-- >= 0)
                {
                    if (Double.isNaN(playerPos.x) || Double.isNaN(playerPos.y) || Double.isNaN(playerPos.z))
                    {
                        return null;
                    }

                    if (playerBlockX == viewBlockX && playerBlockY == viewBlockY && playerBlockZ == viewBlockZ)
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

                    Vec3 var36 = Vec3.createVectorHelper(playerPos.x, playerPos.y, playerPos.z);
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

                    Block var37 = this.worldObj.getBlock(playerBlockX, playerBlockY, playerBlockZ);

                    if (!var37.isReplaceable())
                    {
                        MovingObjectPosition var39 = var37.collisionRayTrace(this.worldObj, playerBlockX, playerBlockY, playerBlockZ, playerPos, viewVec);

                        if (var39 != null)
                        {
                            return var39;
                        }
                    }
                }

                return null;
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    /**
     * Pure function. interpolated look vector
     */
    private Vec3 getLook(float ptt)
    {
        float pitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * ptt;
        float yaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * ptt;
        double cy = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        double sy = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        double cp = -MathHelper.cos(-pitch * 0.017453292F);
        double sp = MathHelper.sin(-pitch * 0.017453292F);
        return Vec3.createVectorHelper(sy * cp, sp, cy * cp);
    }
    
    /**
     * Pure function. Returns a list of bounding boxes that collide with aabb excluding the passed in entity's collision. Args: entity,
     * aabb
     */
    private List<AxisAlignedBB> getCollidingBoundingBoxes(AxisAlignedBB aabb)
    {
        ArrayList<AxisAlignedBB> collidingBoundingBoxes = new ArrayList<AxisAlignedBB>();
        int minx = MathHelper.floor_double(aabb.minX);
        int maxx = MathHelper.floor_double(aabb.maxX + 1.0D);
        int miny = MathHelper.floor_double(aabb.minY);
        int maxy = MathHelper.floor_double(aabb.maxY + 1.0D);
        int minz = MathHelper.floor_double(aabb.minZ);
        int maxz = MathHelper.floor_double(aabb.maxZ + 1.0D);

        for (int x = minx; x < maxx; ++x)
        {
            for (int z = minz; z < maxz; ++z)
            {
            	for (int y = miny - 1; y < maxy; ++y)
                {
                    if (this.worldObj.getBlock(x, y, z) == Block.stone)
                    {
                    	AxisAlignedBB thisAABB = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
                        if (aabb.intersectsWith(thisAABB)) collidingBoundingBoxes.add(thisAABB);
                    }
                }
            }
        }

        return collidingBoundingBoxes;
    }
}
