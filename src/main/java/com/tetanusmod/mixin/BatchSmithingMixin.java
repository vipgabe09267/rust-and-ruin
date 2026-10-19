package com.tetanusmod.mixin;

import com.tetanusmod.expansion.RustArrowSmithingRecipe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.screen.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingScreenHandler.class)
public abstract class BatchSmithingMixin extends ForgingScreenHandler {
    protected BatchSmithingMixin(){super(null,0,null,ScreenHandlerContext.EMPTY);}
    @Shadow private SmithingRecipe currentRecipe;
    @Override public ItemStack quickMove(PlayerEntity player,int index){
        if(index!=3||!(currentRecipe instanceof RustArrowSmithingRecipe))return super.quickMove(player,index);
        var original=slots.get(3).getStack().copy();if(original.isEmpty())return ItemStack.EMPTY;
        var remaining=original.copy();if(!insertItem(remaining,4,slots.size(),true))return ItemStack.EMPTY;
        int moved=original.getCount()-remaining.getCount();if(moved<=0)return ItemStack.EMPTY;
        input.removeStack(1,moved);input.removeStack(2,1);input.markDirty();updateResult();
        if(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)com.tetanusmod.registry.ModAdvancements.award(serverPlayer,"rust_arrows","smith_arrows");
        return original;
    }
    @Inject(method="onTakeOutput",at=@At("HEAD"))
    private void consumeBatch(PlayerEntity player,ItemStack output,CallbackInfo ci){
        if(currentRecipe instanceof RustArrowSmithingRecipe){
            if(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)com.tetanusmod.registry.ModAdvancements.award(serverPlayer,"rust_arrows","smith_arrows");
            // Vanilla consumes one base after this callback. Consume only the additional arrows here.
            int additional=Math.max(0,Math.min(64,output.getCount())-1);
            if(additional>0)input.removeStack(1,additional);
        }
    }
}