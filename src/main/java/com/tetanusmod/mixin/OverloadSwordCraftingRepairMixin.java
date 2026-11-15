package com.tetanusmod.mixin;

import com.tetanusmod.dragon.DragonContent;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.recipe.RepairItemRecipe;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public abstract class OverloadSwordCraftingRepairMixin {
    @Inject(method="matches(Lnet/minecraft/inventory/RecipeInputInventory;Lnet/minecraft/world/World;)Z",at=@At("HEAD"),cancellable=true)
    private void rustruin$noCraftingRepair(RecipeInputInventory inventory,World world,CallbackInfoReturnable<Boolean> result) {
        for(int i=0;i<inventory.size();i++)if(inventory.getStack(i).isOf(DragonContent.SWORD)){result.setReturnValue(false);return;}
    }
}
