package com.tetanusmod.beacon;

import com.tetanusmod.mixin.BeaconAccessor;
import com.tetanusmod.mixin.BlockEntityTypeAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.player.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class TetanizedBeaconEntity extends BeaconBlockEntity {
    private boolean levelIV;

    public TetanizedBeaconEntity(BlockPos pos,BlockState state) {
        super(pos,state);
        ((BlockEntityTypeAccessor)(Object)this).rustruin$setType(ModBeacons.ENTITY);
    }
    /** This is the current paid selection, not an unlock for future selections. */
    public boolean isLevelIV() { return levelIV; }
    public void setLevelIV(boolean enabled) {
        if(levelIV==enabled)return;
        levelIV=enabled;
        markDirty();
    }
    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        levelIV=nbt.getBoolean("RustRuinLevelIV");
    }
    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("RustRuinLevelIV",levelIV);
    }
    @Override public ScreenHandler createMenu(int syncId,PlayerInventory inventory,PlayerEntity player) {
        return new TetanizedBeaconScreenHandler(syncId,inventory,((BeaconAccessor)(Object)this).rustruin$properties(),ScreenHandlerContext.create(world,pos),this);
    }
    @Override public Text getDisplayName() { return Text.translatable("block.tetanusmod.tetanized_beacon"); }
}
