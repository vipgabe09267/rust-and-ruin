package com.tetanusmod.expansion;

import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.*;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;

public final class BearTrapBlock extends BlockWithEntity {
    public static final BooleanProperty CLOSED=BooleanProperty.of("closed");
    private final boolean rusty;
    public BearTrapBlock(boolean rusty,Settings settings){super(settings);this.rusty=rusty;setDefaultState(getStateManager().getDefaultState().with(CLOSED,false));}
    public boolean rusty(){return rusty;}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(CLOSED);}
    @Override public BlockRenderType getRenderType(BlockState state){return BlockRenderType.MODEL;}
    @Override public VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return createCuboidShape(1,0,1,15,state.get(CLOSED)?6:2,15);}
    @Override public void onEntityCollision(BlockState state,World world,BlockPos pos,Entity entity){if(!world.isClient&&!state.get(CLOSED)&&entity instanceof LivingEntity living&&world.getBlockEntity(pos) instanceof BearTrapBlockEntity trap)trap.trigger(living);}
    @Override public BlockEntity createBlockEntity(BlockPos pos,BlockState state){return new BearTrapBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world,BlockState state,BlockEntityType<T> type){return world.isClient?null:checkType(type,ExpansionBlocks.BEAR_TRAP_ENTITY,BearTrapBlockEntity::tick);}
}