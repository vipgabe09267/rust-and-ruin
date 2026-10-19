package com.tetanusmod.expansion;

import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;

public final class RustPileBlock extends Block {
    public static final DirectionProperty FACING=Properties.FACING;
    public RustPileBlock(Settings settings){super(settings);setDefaultState(getStateManager().getDefaultState().with(FACING,Direction.UP));}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getPlacementState(ItemPlacementContext context){return context.getSide()==Direction.DOWN?null:getDefaultState().with(FACING,context.getSide());}
    @Override public boolean canPlaceAt(BlockState state,WorldView world,BlockPos pos){var face=state.get(FACING);return world.getBlockState(pos.offset(face.getOpposite())).isSideSolidFullSquare(world,pos.offset(face.getOpposite()),face);}
    @Override public BlockState getStateForNeighborUpdate(BlockState state,Direction direction,BlockState neighbor,WorldAccess world,BlockPos pos,BlockPos neighborPos){return canPlaceAt(state,world,pos)?state:Blocks.AIR.getDefaultState();}
    @Override public VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return switch(state.get(FACING)){
        case NORTH->createCuboidShape(2,2,14,14,14,16);case SOUTH->createCuboidShape(2,2,0,14,14,2);
        case WEST->createCuboidShape(14,2,2,16,14,14);case EAST->createCuboidShape(0,2,2,2,14,14);
        default->createCuboidShape(2,0,2,14,2,14);
    };}
}