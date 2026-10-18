package com.tetanusmod.beacon;

import com.tetanusmod.mixin.BeaconAccessor;
import com.tetanusmod.mixin.BlockEntityTypeAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.player.*;
import net.minecraft.screen.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class TetanizedBeaconEntity extends BeaconBlockEntity {
    public TetanizedBeaconEntity(BlockPos pos,BlockState state) {
        super(pos,state);
        ((BlockEntityTypeAccessor)(Object)this).rustruin$setType(ModBeacons.ENTITY);
    }
    @Override public ScreenHandler createMenu(int syncId,PlayerInventory inventory,PlayerEntity player) {
        return new TetanizedBeaconScreenHandler(syncId,inventory,((BeaconAccessor)(Object)this).rustruin$properties(),ScreenHandlerContext.create(world,pos));
    }
    @Override public Text getDisplayName() { return Text.translatable("block.tetanusmod.tetanized_beacon"); }
}