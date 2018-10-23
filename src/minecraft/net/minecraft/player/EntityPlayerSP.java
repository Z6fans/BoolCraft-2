package net.minecraft.player;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.KeyBinding;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class EntityPlayerSP extends EntityPlayer
{
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

    /** The index of the currently held item (0-8). */
    public int currentItem;

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
    public float yOffset;
    private float ySize;

    /** How wide this entity is considered to be */
    private float width;

    /** How high this entity is considered to be */
    private float height;
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

    public EntityPlayerSP(WorldClient p_i1238_2_)
    {
    	this.rotationYaw = (float)(Math.random() * Math.PI * 2.0D);
        this.worldObj = p_i1238_2_;
        this.yOffset = 1.62F;
        this.boundingBox = AxisAlignedBB.getBoundingBox(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        this.width = 0.6F;
        this.height = 1.8F;
        this.setPosition(0.0D, 0.0D, 0.0D);
        this.setPosition(this.posX, this.posY, this.posZ);
        this.ySize = 0.0F;
        this.motionX = this.motionY = this.motionZ = 0.0D;
        double ypos = Minecraft.getMinecraft().worldServer.getTopSolidOrLiquidBlock(0, 0);
        this.setPositionAndRotation(ypos + 1.6200000047683716D);
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        Minecraft.getMinecraft().displayGuiScreenNull();
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
        	if (this.worldObj.chunkExists(MathHelper.floor_double(this.posX) >> 4, MathHelper.floor_double(this.posZ) >> 4))
            {
            	this.onEntityUpdate();
                this.onLivingUpdate();

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
                    Minecraft.getMinecraft().worldServer.getPlayerManager().updateMountedMovingPlayer(this.posX, this.posY, this.posZ);
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

    /**
     * Gets called every tick from main Entity class
     */
    private void onEntityUpdate()
    {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.prevRotationPitch = this.rotationPitch;
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;
    }

    /**
     * Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
     * use this to react to sunlight and start to burn.
     */
    private void onLivingUpdate()
    {
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

        this.func_145771_j(this.posX - (double)this.width * 0.35D, this.boundingBox.minY + 0.5D, this.posZ + (double)this.width * 0.35D);
        this.func_145771_j(this.posX - (double)this.width * 0.35D, this.boundingBox.minY + 0.5D, this.posZ - (double)this.width * 0.35D);
        this.func_145771_j(this.posX + (double)this.width * 0.35D, this.boundingBox.minY + 0.5D, this.posZ - (double)this.width * 0.35D);
        this.func_145771_j(this.posX + (double)this.width * 0.35D, this.boundingBox.minY + 0.5D, this.posZ + (double)this.width * 0.35D);

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
        
        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        
        this.motionX = xMov * 0.6D;
        this.motionY = yMov * 0.6D;
        this.motionZ = zMov * 0.6D;
    }

    /**
     * Tries to moves the entity by the passed in displacement. Args: x, y, z
     */
    private void moveEntity(double x, double y, double z)
    {
    	this.ySize *= 0.4F;

        double var13 = x;
        double var15 = y;
        double var17 = z;

        List<AxisAlignedBB> var36 = this.getCollidingBoundingBoxes(this.boundingBox.addCoord(x, y, z));

        for (int var22 = 0; var22 < var36.size(); ++var22)
        {
            y = var36.get(var22).calculateYOffset(this.boundingBox, y);
        }

        this.boundingBox.offset(0.0D, y, 0.0D);
        int var23;

        for (var23 = 0; var23 < var36.size(); ++var23)
        {
            x = var36.get(var23).calculateXOffset(this.boundingBox, x);
        }

        this.boundingBox.offset(x, 0.0D, 0.0D);

        for (var23 = 0; var23 < var36.size(); ++var23)
        {
            z = var36.get(var23).calculateZOffset(this.boundingBox, z);
        }

        this.boundingBox.offset(0.0D, 0.0D, z);
        this.posX = (this.boundingBox.minX + this.boundingBox.maxX) / 2.0D;
        this.posY = this.boundingBox.minY + (double)this.yOffset - (double)this.ySize;
        this.posZ = (this.boundingBox.minZ + this.boundingBox.maxZ) / 2.0D;

        if (var13 != x)
        {
            this.motionX = 0.0D;
        }

        if (var15 != y)
        {
            this.motionY = 0.0D;
        }

        if (var17 != z)
        {
            this.motionZ = 0.0D;
        }
    }

    /**
     * Adds par1*0.15 to the entity's yaw, and *subtracts* par2*0.15 from the pitch. Clamps pitch from -90 to 90. Both
     * arguments in degrees.
     */
    public void setAngles(float dYaw, float dPitch)
    {
        float var3 = this.rotationPitch;
        float var4 = this.rotationYaw;
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

        this.prevRotationPitch += this.rotationPitch - var3;
        this.prevRotationYaw += this.rotationYaw - var4;
    }

    private boolean isBlockTranslucent(int p_71153_1_, int p_71153_2_, int p_71153_3_)
    {
        return this.worldObj.getBlock(p_71153_1_, p_71153_2_, p_71153_3_).isSolid();
    }

    private boolean func_145771_j(double p_145771_1_, double p_145771_3_, double p_145771_5_)
    {
        int var7 = MathHelper.floor_double(p_145771_1_);
        int var8 = MathHelper.floor_double(p_145771_3_);
        int var9 = MathHelper.floor_double(p_145771_5_);
        double var10 = p_145771_1_ - (double)var7;
        double var12 = p_145771_5_ - (double)var9;

        if (this.isBlockTranslucent(var7, var8, var9) || this.isBlockTranslucent(var7, var8 + 1, var9))
        {
            boolean var14 = !this.isBlockTranslucent(var7 - 1, var8, var9) && !this.isBlockTranslucent(var7 - 1, var8 + 1, var9);
            boolean var15 = !this.isBlockTranslucent(var7 + 1, var8, var9) && !this.isBlockTranslucent(var7 + 1, var8 + 1, var9);
            boolean var16 = !this.isBlockTranslucent(var7, var8, var9 - 1) && !this.isBlockTranslucent(var7, var8 + 1, var9 - 1);
            boolean var17 = !this.isBlockTranslucent(var7, var8, var9 + 1) && !this.isBlockTranslucent(var7, var8 + 1, var9 + 1);
            byte var18 = -1;
            double var19 = 9999.0D;

            if (var14 && var10 < var19)
            {
                var19 = var10;
                var18 = 0;
            }

            if (var15 && 1.0D - var10 < var19)
            {
                var19 = 1.0D - var10;
                var18 = 1;
            }

            if (var16 && var12 < var19)
            {
                var19 = var12;
                var18 = 4;
            }

            if (var17 && 1.0D - var12 < var19)
            {
                var19 = 1.0D - var12;
                var18 = 5;
            }

            float var21 = 0.1F;

            if (var18 == 0)
            {
                this.motionX = (double)(-var21);
            }

            if (var18 == 1)
            {
                this.motionX = (double)var21;
            }

            if (var18 == 4)
            {
                this.motionZ = (double)(-var21);
            }

            if (var18 == 5)
            {
                this.motionZ = (double)var21;
            }
        }

        return false;
    }
    
    /**
     * Keeps moving the entity up so it isn't colliding with blocks and other requirements for this entity to be spawned
     * (only actually used on players though its also on Entity)
     */
    public void preparePlayerToSpawn()
    {
    	this.yOffset = 1.62F;
    	
        if (0.6F != this.width || 1.8F != this.height)
        {
            this.width = 0.6F;
            this.height = 1.8F;
            this.boundingBox.maxX = this.boundingBox.minX + (double)this.width;
            this.boundingBox.maxZ = this.boundingBox.minZ + (double)this.width;
            this.boundingBox.maxY = this.boundingBox.minY + (double)this.height;
        }
        
        if (this.worldObj != null)
        {
            while (this.posY > 0.0D)
            {
                this.setPosition(this.posX, this.posY, this.posZ);

                if (this.getCollidingBoundingBoxes(this.boundingBox).isEmpty())
                {
                    break;
                }

                ++this.posY;
            }

            this.motionX = this.motionY = this.motionZ = 0.0D;
            this.rotationPitch = 0.0F;
        }
    }

    /**
     * Performs a ray trace for the distance specified and using the partial tick time. Args: distance, partialTickTime
     */
    public MovingObjectPosition rayTrace(double distance, float ptt)
    {
        Vec3 v1 = this.getPosition(ptt);
        Vec3 lookVec = this.getLook(ptt);
        Vec3 v2 = v1.addVector(lookVec.x * distance, lookVec.y * distance, lookVec.z * distance);
        if (!Double.isNaN(v1.x) && !Double.isNaN(v1.y) && !Double.isNaN(v1.z))
        {
            if (!Double.isNaN(v2.x) && !Double.isNaN(v2.y) && !Double.isNaN(v2.z))
            {
                int var6 = MathHelper.floor_double(v2.x);
                int var7 = MathHelper.floor_double(v2.y);
                int var8 = MathHelper.floor_double(v2.z);
                int var9 = MathHelper.floor_double(v1.x);
                int var10 = MathHelper.floor_double(v1.y);
                int var11 = MathHelper.floor_double(v1.z);
                Block var12 = this.worldObj.getBlock(var9, var10, var11);

                if (!var12.isReplaceable())
                {
                    MovingObjectPosition var14 = var12.collisionRayTrace(this.worldObj, var9, var10, var11, v1, v2);

                    if (var14 != null)
                    {
                        return var14;
                    }
                }

                int var13 = 200;

                while (var13-- >= 0)
                {
                    if (Double.isNaN(v1.x) || Double.isNaN(v1.y) || Double.isNaN(v1.z))
                    {
                        return null;
                    }

                    if (var9 == var6 && var10 == var7 && var11 == var8)
                    {
                        return null;
                    }

                    boolean var41 = true;
                    boolean var15 = true;
                    boolean var16 = true;
                    double var17 = 999.0D;
                    double var19 = 999.0D;
                    double var21 = 999.0D;

                    if (var6 > var9)
                    {
                        var17 = (double)var9 + 1.0D;
                    }
                    else if (var6 < var9)
                    {
                        var17 = (double)var9 + 0.0D;
                    }
                    else
                    {
                        var41 = false;
                    }

                    if (var7 > var10)
                    {
                        var19 = (double)var10 + 1.0D;
                    }
                    else if (var7 < var10)
                    {
                        var19 = (double)var10 + 0.0D;
                    }
                    else
                    {
                        var15 = false;
                    }

                    if (var8 > var11)
                    {
                        var21 = (double)var11 + 1.0D;
                    }
                    else if (var8 < var11)
                    {
                        var21 = (double)var11 + 0.0D;
                    }
                    else
                    {
                        var16 = false;
                    }

                    double var23 = 999.0D;
                    double var25 = 999.0D;
                    double var27 = 999.0D;
                    double var29 = v2.x - v1.x;
                    double var31 = v2.y - v1.y;
                    double var33 = v2.z - v1.z;

                    if (var41)
                    {
                        var23 = (var17 - v1.x) / var29;
                    }

                    if (var15)
                    {
                        var25 = (var19 - v1.y) / var31;
                    }

                    if (var16)
                    {
                        var27 = (var21 - v1.z) / var33;
                    }

                    byte var42;

                    if (var23 < var25 && var23 < var27)
                    {
                        if (var6 > var9)
                        {
                            var42 = 4;
                        }
                        else
                        {
                            var42 = 5;
                        }

                        v1.x = var17;
                        v1.y += var31 * var23;
                        v1.z += var33 * var23;
                    }
                    else if (var25 < var27)
                    {
                        if (var7 > var10)
                        {
                            var42 = 0;
                        }
                        else
                        {
                            var42 = 1;
                        }

                        v1.x += var29 * var25;
                        v1.y = var19;
                        v1.z += var33 * var25;
                    }
                    else
                    {
                        if (var8 > var11)
                        {
                            var42 = 2;
                        }
                        else
                        {
                            var42 = 3;
                        }

                        v1.x += var29 * var27;
                        v1.y += var31 * var27;
                        v1.z = var21;
                    }

                    Vec3 var36 = Vec3.createVectorHelper(v1.x, v1.y, v1.z);
                    var9 = (int)(var36.x = (double)MathHelper.floor_double(v1.x));

                    if (var42 == 5)
                    {
                        --var9;
                        ++var36.x;
                    }

                    var10 = (int)(var36.y = (double)MathHelper.floor_double(v1.y));

                    if (var42 == 1)
                    {
                        --var10;
                        ++var36.y;
                    }

                    var11 = (int)(var36.z = (double)MathHelper.floor_double(v1.z));

                    if (var42 == 3)
                    {
                        --var11;
                        ++var36.z;
                    }

                    Block var37 = this.worldObj.getBlock(var9, var10, var11);

                    if (!var37.isReplaceable())
                    {
                        MovingObjectPosition var39 = var37.collisionRayTrace(this.worldObj, var9, var10, var11, v1, v2);

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
     * interpolated position vector
     */
    private Vec3 getPosition(float p_70666_1_)
    {
        if (p_70666_1_ == 1.0F)
        {
            return Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
        }
        else
        {
            double var2 = this.prevPosX + (this.posX - this.prevPosX) * (double)p_70666_1_;
            double var4 = this.prevPosY + (this.posY - this.prevPosY) * (double)p_70666_1_;
            double var6 = this.prevPosZ + (this.posZ - this.prevPosZ) * (double)p_70666_1_;
            return Vec3.createVectorHelper(var2, var4, var6);
        }
    }

    /**
     * interpolated look vector
     */
    private Vec3 getLook(float p_70676_1_)
    {
        float var2;
        float var3;
        float var4;
        float var5;

        if (p_70676_1_ == 1.0F)
        {
            var2 = MathHelper.cos(-this.rotationYaw * 0.017453292F - (float)Math.PI);
            var3 = MathHelper.sin(-this.rotationYaw * 0.017453292F - (float)Math.PI);
            var4 = -MathHelper.cos(-this.rotationPitch * 0.017453292F);
            var5 = MathHelper.sin(-this.rotationPitch * 0.017453292F);
            return Vec3.createVectorHelper((double)(var3 * var4), (double)var5, (double)(var2 * var4));
        }
        else
        {
            var2 = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * p_70676_1_;
            var3 = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * p_70676_1_;
            var4 = MathHelper.cos(-var3 * 0.017453292F - (float)Math.PI);
            var5 = MathHelper.sin(-var3 * 0.017453292F - (float)Math.PI);
            float var6 = -MathHelper.cos(-var2 * 0.017453292F);
            float var7 = MathHelper.sin(-var2 * 0.017453292F);
            return Vec3.createVectorHelper((double)(var5 * var6), (double)var7, (double)(var4 * var6));
        }
    }

    /**
     * Sets the entity's position and rotation. Args: posX, posY, posZ, yaw, pitch
     */
    public void setPositionAndRotation(double p_70080_3_)
    {
        this.prevPosX = this.posX = 0;
        this.prevPosY = this.posY = p_70080_3_;
        this.prevPosZ = this.posZ = 0;
        this.prevRotationYaw = this.rotationYaw = 0;
        this.prevRotationPitch = this.rotationPitch = 0;
        this.ySize = 0.0F;
        this.setPosition(this.posX, this.posY, this.posZ);
    }

    /**
     * Sets the x,y,z of the entity from the given parameters. Also seems to set up a bounding box.
     */
    private void setPosition(double p_70107_1_, double p_70107_3_, double p_70107_5_)
    {
        this.posX = p_70107_1_;
        this.posY = p_70107_3_;
        this.posZ = p_70107_5_;
        float var7 = this.width / 2.0F;
        float var8 = this.height;
        this.boundingBox.setBounds(p_70107_1_ - (double)var7, p_70107_3_ - (double)this.yOffset + (double)this.ySize, p_70107_5_ - (double)var7, p_70107_1_ + (double)var7, p_70107_3_ - (double)this.yOffset + (double)this.ySize + (double)var8, p_70107_5_ + (double)var7);
    }
    
    /**
     * Returns a list of bounding boxes that collide with aabb excluding the passed in entity's collision. Args: entity,
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
                if (this.worldObj.chunkExists(x >> 4, z >> 4))
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
        }

        return collidingBoundingBoxes;
    }
}
