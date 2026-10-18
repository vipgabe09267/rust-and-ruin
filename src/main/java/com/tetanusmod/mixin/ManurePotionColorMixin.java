package com.tetanusmod.mixin;
import com.tetanusmod.registry.ModBrewing;
import net.minecraft.potion.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PotionUtil.class)
public abstract class ManurePotionColorMixin {
    @Inject(method="getColor(Lnet/minecraft/item/ItemStack;)I",at=@At("HEAD"),cancellable=true)
    private static void bottleColor(net.minecraft.item.ItemStack stack,CallbackInfoReturnable<Integer> cir) {
        if(PotionUtil.getPotion(stack)==ModBrewing.MANURE && (!stack.hasNbt() || !stack.getNbt().contains("CustomPotionColor")))cir.setReturnValue(0x725131);
    }
    @Inject(method="getColor(Lnet/minecraft/potion/Potion;)I",at=@At("HEAD"),cancellable=true)
    private static void color(Potion potion,CallbackInfoReturnable<Integer> cir) {
        if(potion==ModBrewing.MANURE)cir.setReturnValue(0x725131);
    }
}