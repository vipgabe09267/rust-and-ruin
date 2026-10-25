package com.tetanusmod.expansion;

import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.*;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;

public final class BearTrapBlock extends BlockWithEntity {
    public static final BooleanProperty CLOSED = BooleanProperty.of("closed");
    private final boolean rusty;

    public BearTrapBlock(boolean rusty, Settings settings) {
        // A solid collision shape stops feet reaching the trigger in normal movement.
        super(settings.noCollision());
        this.rusty = rusty;
        setDefaultState(getStateManager().getDefaultState().with(CLOSED, false));
    }

    public boolean rusty() { return rusty; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(CLOSED); }
    @Override public BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return createCuboidShape(1, 0, 1, 15, state.get(CLOSED) ? 7 : 2, 15);
    }

    @Override public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;
        if (!entity.getBoundingBox().intersects(getOutlineShape(state, world, pos, ShapeContext.of(entity)).getBoundingBox().offset(pos).expand(0, 0.0001, 0))) return;
        if (!(world.getBlockEntity(pos) instanceof BearTrapBlockEntity trap)) return;
        if (!world.isClient && !state.get(CLOSED)) trap.trigger(living);
        // The synchronized capture UUID makes prediction agree with the server restraint.
        if (trap.holds(living)) {
            living.slowMovement(state, new Vec3d(0.01, 0.01, 0.01));
            living.setVelocity(Vec3d.ZERO);
        }
    }

    @Override public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState replacement, boolean moved) {
        if (state.getBlock() != replacement.getBlock() && !(replacement.getBlock() instanceof BearTrapBlock)
                && world.getBlockEntity(pos) instanceof BearTrapBlockEntity trap) trap.release();
        super.onStateReplaced(state, world, pos, replacement, moved);
    }

    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new BearTrapBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, ExpansionBlocks.BEAR_TRAP_ENTITY, BearTrapBlockEntity::tick);
    }
}
