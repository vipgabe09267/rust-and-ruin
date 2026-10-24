package com.tetanusmod.block;

import com.tetanusmod.registry.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public final class CaltropsBlock extends BlockWithEntity implements Waterloggable {
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 3, 14);
    private final boolean rusty;
    public CaltropsBlock(boolean rusty, Settings settings) {
        super(settings);
        this.rusty = rusty;
        setDefaultState(getStateManager().getDefaultState().with(WATERLOGGED, false));
    }
    public boolean isRusty() { return rusty; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(WATERLOGGED); }
    @Override public BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) { return this==ModBlocks.BARBED_WIRE||this==ModBlocks.RUSTED_BARBED_WIRE?Block.createCuboidShape(1,0,1,15,8,15):SHAPE; }
    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(WATERLOGGED, context.getWorld().getFluidState(context.getBlockPos()).getFluid() == Fluids.WATER);
    }
    @Override public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }
    @Override public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
            WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
    @Override public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if(!entity.getBoundingBox().intersects(getOutlineShape(state,world,pos,ShapeContext.of(entity)).getBoundingBox().offset(pos).expand(0,0.0001,0)))return;
        if (!world.isClient && entity instanceof LivingEntity living && world.getBlockEntity(pos) instanceof CaltropsBlockEntity trap) {
            trap.stepOn(living, rusty);
        }
    }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new CaltropsBlockEntity(pos, state); }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, ModBlocks.CALTROPS_ENTITY, CaltropsBlockEntity::tick);
    }
}
