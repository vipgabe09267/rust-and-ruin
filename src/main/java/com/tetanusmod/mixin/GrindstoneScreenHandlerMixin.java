package com.tetanusmod.mixin;

import com.tetanusmod.rust.RustingSystem;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GrindstoneScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrindstoneScreenHandler.class)
public abstract class GrindstoneScreenHandlerMixin {
    @Shadow @Final private Inventory input;
    @Shadow @Final private Inventory result;
    @Shadow protected abstract ItemStack grind(ItemStack item, int damage, int count);

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void tetanus$removeRust(CallbackInfo ci) {
        ItemStack first = input.getStack(0);
        ItemStack second = input.getStack(1);
        if (first.isEmpty() == second.isEmpty()) return;
        ItemStack rusty = first.isEmpty() ? second : first;
        Item clean = RustingSystem.clean(rusty.getItem());
        if (clean == null) return;
        ItemStack converted = RustingSystem.convert(rusty, clean);
        // Vanilla grind strips enchantments and awards XP once, preventing an XP duplication loop.
        result.setStack(0, grind(converted, converted.getDamage(), converted.getCount()));
        ((GrindstoneScreenHandler) (Object) this).sendContentUpdates();
        ci.cancel();
    }
}
