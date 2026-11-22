package com.tetanusmod.medical;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public final class ClinicNoticeBoardBlock extends HorizontalFacingBlock {
    public ClinicNoticeBoardBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case EAST -> createCuboidShape(0, 2, 1, 3, 15, 15);
            case SOUTH -> createCuboidShape(1, 2, 0, 15, 15, 3);
            case WEST -> createCuboidShape(13, 2, 1, 16, 15, 15);
            default -> createCuboidShape(1, 2, 13, 15, 15, 16);
        };
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.isSpectator()) return ActionResult.PASS;
        if (!world.isClient) {
            var location = ScreenHandlerContext.create(world, pos);
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, user) -> new ClinicNoticeBoardScreenHandler(syncId, inventory, location), getName()));
        }
        return ActionResult.success(world.isClient);
    }
}
