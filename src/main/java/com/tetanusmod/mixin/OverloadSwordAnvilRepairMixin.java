package com.tetanusmod.mixin;

import com.tetanusmod.dragon.DragonContent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public abstract class OverloadSwordAnvilRepairMixin extends ForgingScreenHandler {
    @Shadow @Final private Property levelCost;
    @Shadow private int repairItemUsage;
    protected OverloadSwordAnvilRepairMixin(ScreenHandlerType<?> type,int id,PlayerInventory inventory,ScreenHandlerContext context) { super(type,id,inventory,context); }
    @Inject(method="updateResult",at=@At("HEAD"),cancellable=true)
    private void rustruin$noSameSwordRepair(CallbackInfo callback) {
        if(!input.getStack(0).isOf(DragonContent.SWORD)||!input.getStack(1).isOf(DragonContent.SWORD))return;
        output.setStack(0,ItemStack.EMPTY);levelCost.set(0);repairItemUsage=0;
        sendContentUpdates();callback.cancel();
    }
}
