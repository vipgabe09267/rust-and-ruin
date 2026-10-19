package com.tetanusmod.mixin;

import com.tetanusmod.expansion.GloveInventory;
import com.tetanusmod.registry.ModItems;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(PlayerScreenHandler.class)
public abstract class GloveScreenHandlerMixin extends ScreenHandler {
    protected GloveScreenHandlerMixin(){super(null,0);}
    @Inject(method="<init>",at=@At("TAIL"))
    private void slot(PlayerInventory inventory,boolean onServer,PlayerEntity player,CallbackInfo ci){
        addSlot(new Slot(((GloveInventory)player).rustruin$gloves(),0,77,44){
            @Override public boolean canInsert(ItemStack stack){return stack.isOf(ModItems.PROTECTIVE_GLOVES);}
            @Override public int getMaxItemCount(){return 1;}
        });
    }
    @Inject(method="quickMove",at=@At("HEAD"),cancellable=true)
    private void move(PlayerEntity player,int index,CallbackInfoReturnable<ItemStack> result){
        if(index<0||index>=slots.size())return;
        var slot=slots.get(index);var stack=slot.getStack();
        if(stack.isEmpty())return;
        boolean gloveSlot=index==46;
        if(!gloveSlot&&(!(index>=9&&index<45)||!stack.isOf(ModItems.PROTECTIVE_GLOVES)||getSlot(46).hasStack()))return;
        var original=stack.copy();boolean moved=gloveSlot?insertItem(stack,9,45,false):insertItem(stack,46,47,false);
        if(moved){if(stack.isEmpty())slot.setStack(ItemStack.EMPTY);else slot.markDirty();slot.onTakeItem(player,stack);result.setReturnValue(original);}
        else result.setReturnValue(ItemStack.EMPTY);
    }
}