package com.tetanusmod.mixin;

import com.tetanusmod.expansion.GloveInventory;
import com.tetanusmod.registry.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class GlovePlayerMixin implements GloveInventory {
    @Unique private SimpleInventory rustruin$gloveInventory;
    public SimpleInventory rustruin$gloves(){
        if(rustruin$gloveInventory==null)rustruin$gloveInventory=new SimpleInventory(1);
        return rustruin$gloveInventory;
    }
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))
    private void save(NbtCompound nbt,CallbackInfo ci){nbt.put("RustRuinGloves",rustruin$gloves().getStack(0).writeNbt(new NbtCompound()));}
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL"))
    private void load(NbtCompound nbt,CallbackInfo ci){
        var self=(PlayerEntity)(Object)this;
        var saved=ItemStack.fromNbt(nbt.getCompound("RustRuinGloves"));
        rustruin$gloves().setStack(0,saved.isOf(ModItems.PROTECTIVE_GLOVES)?saved.copyWithCount(1):ItemStack.EMPTY);
        if(rustruin$gloves().isEmpty()&&self.getOffHandStack().isOf(ModItems.PROTECTIVE_GLOVES)){
            rustruin$gloves().setStack(0,self.getOffHandStack());self.setStackInHand(Hand.OFF_HAND,ItemStack.EMPTY);
        }
    }
    @Inject(method="dropInventory",at=@At("TAIL"))
    private void drop(CallbackInfo ci){
        var self=(PlayerEntity)(Object)this;var stack=rustruin$gloves().removeStack(0);
        if(!stack.isEmpty()&&!net.minecraft.enchantment.EnchantmentHelper.hasVanishingCurse(stack))self.dropItem(stack,true,false);
    }
}