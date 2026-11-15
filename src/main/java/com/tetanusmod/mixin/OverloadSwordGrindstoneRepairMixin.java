package com.tetanusmod.mixin;

import com.tetanusmod.dragon.DragonContent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GrindstoneScreenHandler;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrindstoneScreenHandler.class)
public abstract class OverloadSwordGrindstoneRepairMixin {
    @Shadow @Final private Inventory input;
    @Shadow @Final private Inventory result;
    @Inject(method="updateResult",at=@At("HEAD"),cancellable=true)
    private void rustruin$noCombinedRepair(CallbackInfo callback) {
        var first=input.getStack(0);var second=input.getStack(1);
        if(first.isEmpty()||second.isEmpty()||!first.isOf(DragonContent.SWORD)&&!second.isOf(DragonContent.SWORD))return;
        result.setStack(0,ItemStack.EMPTY);
        ((GrindstoneScreenHandler)(Object)this).sendContentUpdates();callback.cancel();
    }
}
