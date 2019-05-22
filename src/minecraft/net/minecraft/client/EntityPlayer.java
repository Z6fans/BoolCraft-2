package net.minecraft.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.KeyBinding;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class EntityPlayer
{
    /** Entity position X */
    public double posX;

    /** Entity position Y */
    public double posY;

    /** Entity position Z */
    public double posZ;

    /** Has this entity been added to the chunk its within */
    public boolean addedToChunk;
    public int chunkCoordX;
    public int chunkCoordY;
    public int chunkCoordZ;
	private WorldClient worldObj;
    
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

    /** used to check whether entity is jumping. */
    private boolean isJumping;
    private float moveStrafing;
    private float moveForward;

    /**
     * Counter used to ensure that the server sends a move packet (Packet11, 12 or 13) to the client at least once a
     * second.
     */
    private int ticksSinceMovePacket;

    /** Axis aligned bounding box. */
    private final AxisAlignedBB boundingBox;
    private float ySize;

    /** How wide this entity is considered to be */
    private float width;
    private double prevPosX;
    private double prevPosY;
    private double prevPosZ;

    /** Entity rotation Yaw */
    public float rotationYaw;

    /** Entity rotation Pitch */
    public float rotationPitch;
    public float prevRotationYaw;
    public float prevRotationPitch;

    /**
     * The entity's X coordinate at the previous tick, used to calculate position during rendering routines
     */
    public double lastTickPosX;

    /**
     * The entity's Y coordinate at the previous tick, used to calculate position during rendering routines
     */
    public double lastTickPosY;

    /**
     * The entity's Z coordinate at the previous tick, used to calculate position during rendering routines
     */
    public double lastTickPosZ;

    public EntityPlayer(WorldClient world)
    {
    	this.rotationYaw = (float)(Math.random() * Math.PI * 2.0D);
        this.worldObj = world;
        this.boundingBox = AxisAlignedBB.getBoundingBox(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        this.width = 0.6F;
        this.ySize = 0;
        this.motionX = this.motionY = this.motionZ = 0;
        this.prevPosX = this.posX = this.prevPosZ = this.posZ = 0;
        this.prevPosY = this.posY = Minecraft.getMinecraft().worldServer.getTopBlockAtSpawn() + 1.6200000047683716D;
        this.prevRotationYaw = this.rotationYaw = this.prevRotationPitch = this.rotationPitch = 0;
        Minecraft.getMinecraft().displayGuiScreenNull();
        
        while (this.posY > 0.0D)
        {
            this.boundingBox.setBounds(-this.width/2.0F, this.posY - (double)this.getYOffset() + (double)this.ySize, -this.width/2.0F, this.width/2.0F, this.posY - (double)this.getYOffset() + (double)this.ySize + 1.8F, this.width/2.0F);

            if (this.getCollidingBoundingBoxes(this.boundingBox).isEmpty())
            {
                break;
            }

            ++this.posY;
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
    	this.lastTickPosX = this.posX;
    	this.lastTickPosY = this.posY;
    	this.lastTickPosZ = this.posZ;
    	this.prevRotationYaw = this.rotationYaw;
    	this.prevRotationPitch = this.rotationPitch;

        if (this.addedToChunk)
        {
        	this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            this.prevRotationPitch = this.rotationPitch;
            this.prevRotationYaw = this.rotationYaw;
            this.prevRotationYaw = this.rotationYaw;
            this.prevRotationPitch = this.rotationPitch;
            
            this.moveStrafing = 0.0F;
            this.moveForward = 0.0F;

            if (KeyBinding.keyBindForward.getIsKeyPressed())
            {
                ++this.moveForward;
            }

            if (KeyBinding.keyBindBack.getIsKeyPressed())
            {
                --this.moveForward;
            }

            if (KeyBinding.keyBindLeft.getIsKeyPressed())
            {
                ++this.moveStrafing;
            }

            if (KeyBinding.keyBindRight.getIsKeyPressed())
            {
                --this.moveStrafing;
            }

            this.isJumping = KeyBinding.keyBindJump.getIsKeyPressed();

            this.pushPlayerOutOfBlock(this.posX - (double)this.width * 0.35D, this.boundingBox.minY + 0.5D, this.posZ + (double)this.width * 0.35D);
            this.pushPlayerOutOfBlock(this.posX - (double)this.width * 0.35D, this.boundingBox.minY + 0.5D, this.posZ - (double)this.width * 0.35D);
            this.pushPlayerOutOfBlock(this.posX + (double)this.width * 0.35D, this.boundingBox.minY + 0.5D, this.posZ - (double)this.width * 0.35D);
            this.pushPlayerOutOfBlock(this.posX + (double)this.width * 0.35D, this.boundingBox.minY + 0.5D, this.posZ + (double)this.width * 0.35D);

            if (KeyBinding.keyBindSneak.getIsKeyPressed())
            {
                this.motionY -= 0.15D;
            }

            if (this.isJumping)
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
            this.moveStrafing *= 0.98F;
            this.moveForward *= 0.98F;
            
            float strafe = this.moveStrafing;
        	float forward = this.moveForward;
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
                float var5 = MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F);
                float var6 = MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F);
                this.motionX += (double)(strafe * var6 - forward * var5);
                this.motionZ += (double)(forward * var6 + strafe * var5);
            }
            
            this.ySize *= 0.4F;

            double var13 = this.motionX;
            double var15 = this.motionY;
            double var17 = this.motionZ;

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
            this.posY = this.boundingBox.minY + (double)this.getYOffset() - (double)this.ySize;
            this.posZ = (this.boundingBox.minZ + this.boundingBox.maxZ) / 2.0D;

            if (var13 != this.motionX)
            {
                this.motionX = 0.0D;
            }

            if (var15 != this.motionY)
            {
                this.motionY = 0.0D;
            }

            if (var17 != this.motionZ)
            {
                this.motionZ = 0.0D;
            }
            
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
                Minecraft.getMinecraft().worldServer.updateMountedMovingPlayer(this.posX, this.posZ);
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
        
        this.addedToChunk = true;
        this.chunkCoordX = MathHelper.floor_double(this.posX / 16.0D);
        this.chunkCoordY = MathHelper.floor_double(this.posY / 16.0D);
        this.chunkCoordZ = MathHelper.floor_double(this.posZ / 16.0D);
    }

    /**
     * Adds par1*0.15 to the entity's yaw, and *subtracts* par2*0.15 from the pitch. Clamps pitch from -90 to 90. Both
     * arguments in degrees.
     */
    public void setAngles(float dYaw, float dPitch)
    {
        float oldPitch = this.rotationPitch;
        float oldYaw = this.rotationYaw;
        this.rotationYaw = (float)((double)this.rotationYaw + (double)dYaw * 0.15D);
        this.rotationPitch = (float)((double)this.rotationPitch - (double)dPitch * 0.15D);

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
        float var2 = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * ptt;
        float var3 = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * ptt;
        float var4 = MathHelper.cos(-var3 * 0.017453292F - (float)Math.PI);
        float var5 = MathHelper.sin(-var3 * 0.017453292F - (float)Math.PI);
        float var6 = -MathHelper.cos(-var2 * 0.017453292F);
        float var7 = MathHelper.sin(-var2 * 0.017453292F);
        return Vec3.createVectorHelper((double)(var5 * var6), (double)var7, (double)(var4 * var6));
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
                    Block block;

                    if (x >= -30000000 && x < 30000000 && z >= -30000000 && z < 30000000)
                    {
                        block = this.worldObj.getBlock(x, y, z);
                    }
                    else
                    {
                        block = Block.stone;
                    }

                    block.addCollisionBoxesToList(x, y, z, aabb, collidingBoundingBoxes);
                }
            }
        }

        return collidingBoundingBoxes;
    }
}
