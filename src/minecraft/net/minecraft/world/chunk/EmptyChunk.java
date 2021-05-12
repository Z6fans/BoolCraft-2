package net.minecraft.world.chunk;

import net.minecraft.block.Block;

public class EmptyChunk extends Chunk
{
    public EmptyChunk(){super(0, 0);}
    public Block getBlock(int p_150810_1_, int p_150810_2_, int p_150810_3_){return Block.air;}
    public int getBlockMetadata(int p_76628_1_, int p_76628_2_, int p_76628_3_){return 0;}
    public boolean setBlockMetadata(int p_76589_1_, int p_76589_2_, int p_76589_3_, int p_76589_4_){return false;}
    public void setChunkModified() {}
}
