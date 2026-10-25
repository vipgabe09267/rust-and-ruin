package com.tetanusmod.expansion;

import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;

/** Native door lifecycle plus an explicit migration state for older, one-block cabinets. */
public final class MedicineCabinetBlock extends DoorBlock {
    public static final BooleanProperty COMPLETE=BooleanProperty.of("complete");
    private static final BlockSetType STEEL=new BlockSetType("tetanusmod:medical_steel",true,
        BlockSetType.IRON.soundType(),BlockSetType.IRON.doorClose(),BlockSetType.IRON.doorOpen(),
        BlockSetType.IRON.trapdoorClose(),BlockSetType.IRON.trapdoorOpen(),
        BlockSetType.IRON.pressurePlateClickOff(),BlockSetType.IRON.pressurePlateClickOn(),
        BlockSetType.IRON.buttonClickOff(),BlockSetType.IRON.buttonClickOn());
    private static final VoxelShape NORTH=Block.createCuboidShape(1.5,0,3,14.5,16,14.5);
    private static final VoxelShape SOUTH=Block.createCuboidShape(1.5,0,1.5,14.5,16,13);
    private static final VoxelShape EAST=Block.createCuboidShape(1.5,0,1.5,13,16,14.5);
    private static final VoxelShape WEST=Block.createCuboidShape(3,0,1.5,14.5,16,14.5);
    public MedicineCabinetBlock(Settings settings){super(settings.nonOpaque(),STEEL);setDefaultState(getDefaultState().with(COMPLETE,false));}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> builder){super.appendProperties(builder);builder.add(COMPLETE);}
    @Override public BlockState getPlacementState(net.minecraft.item.ItemPlacementContext context){
        // A cabinet presents its front to the placer. Let the native door also choose
        // its hinge against that outward facing, using the original click and world.
        var outward=new net.minecraft.item.ItemPlacementContext(context){
            @Override public Direction getHorizontalPlayerFacing(){return context.getHorizontalPlayerFacing().getOpposite();}
        };
        var state=super.getPlacementState(outward);
        return state==null?null:state.with(COMPLETE,true);
    }
    @Override public boolean canPathfindThrough(BlockState state,BlockView world,BlockPos pos,net.minecraft.entity.ai.pathing.NavigationType type){return false;}
    @Override public VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext context){return switch(state.get(FACING)){case SOUTH->SOUTH;case EAST->EAST;case WEST->WEST;default->NORTH;};}
    @Override public BlockState getStateForNeighborUpdate(BlockState state,Direction side,BlockState neighbor,WorldAccess world,BlockPos pos,BlockPos neighborPos){
        if(!state.get(COMPLETE)&&state.get(HALF)==DoubleBlockHalf.LOWER&&side==Direction.UP&&!neighbor.isOf(this))return state;
        return super.getStateForNeighborUpdate(state,side,neighbor,world,pos,neighborPos);
    }
    @Override public ActionResult onUse(BlockState state,World world,BlockPos pos,PlayerEntity player,Hand hand,BlockHitResult hit){
        if(player.isSpectator())return ActionResult.PASS;
        if(!world.isClient&&!state.get(COMPLETE)&&state.get(HALF)==DoubleBlockHalf.LOWER&&pos.getY()<world.getTopY()-1&&world.isAir(pos.up())){
            state=state.with(COMPLETE,true);world.setBlockState(pos.up(),state.with(HALF,DoubleBlockHalf.UPPER),18);world.setBlockState(pos,state,18);world.updateNeighborsAlways(pos,this);world.updateNeighborsAlways(pos.up(),this);
        }
        return super.onUse(state,world,pos,player,hand,hit);
    }
}
