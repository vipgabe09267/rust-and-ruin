package com.tetanusmod.mixin;

import com.tetanusmod.rust.RustingSystem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"net.minecraft.screen.GrindstoneScreenHandler$2", "net.minecraft.screen.GrindstoneScreenHandler$3"})
public abstract class GrindstoneInputSlotMixin {
    @Inject(method = "canInsert", at = @At("HEAD"), cancellable = true)
    private void tetanus$acceptRustyItems(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (RustingSystem.clean(stack.getItem()) != null) cir.setReturnValue(true);
    }
}
