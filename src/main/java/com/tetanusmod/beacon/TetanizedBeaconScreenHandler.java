package com.tetanusmod.beacon;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import java.util.Optional;

public final class TetanizedBeaconScreenHandler extends BeaconScreenHandler {
    private final ScreenHandlerContext location;
    public TetanizedBeaconScreenHandler(int syncId,PlayerInventory inventory) { this(syncId,inventory,new ArrayPropertyDelegate(3),ScreenHandlerContext.EMPTY); }
    public TetanizedBeaconScreenHandler(int syncId,PlayerInventory inventory,PropertyDelegate properties,ScreenHandlerContext context) {
        super(syncId,inventory,properties,context);location=context;
        Slot old=getSlot(0);
        Slot payment=new Slot(old.inventory,0,old.x,old.y) {
            @Override public boolean canInsert(ItemStack stack) { return stack.isOf(Items.NETHERITE_INGOT); }
            @Override public int getMaxItemCount() { return 1; }
        };
        payment.id=0;slots.set(0,payment);
    }
    @Override public ScreenHandlerType<?> getType() { return ModBeacons.SCREEN; }
    @Override public boolean canUse(PlayerEntity player) {
        return location.get((world,pos)->world.getBlockState(pos).isOf(ModBeacons.BLOCK)&&player.squaredDistanceTo(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5)<=64,true);
    }
    @Override public ItemStack quickMove(PlayerEntity player,int slot) {
        if(slot>0 && !getSlot(slot).getStack().isOf(Items.NETHERITE_INGOT))return ItemStack.EMPTY;
        return super.quickMove(player,slot);
    }
    @Override public void setEffects(Optional<StatusEffect> primary,Optional<StatusEffect> secondary) {
        if(getSlot(0).getStack().isOf(Items.NETHERITE_INGOT))super.setEffects(primary,secondary);
    }
}