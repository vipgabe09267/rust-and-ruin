package com.tetanusmod.mixin;

import com.tetanusmod.rust.RustCoating;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class CoatingWearMixin {
    @Inject(method="damage(ILnet/minecraft/entity/LivingEntity;Ljava/util/function/Consumer;)V", at=@At("HEAD"))
    private <T extends LivingEntity> void wear(int amount, T entity, Consumer<T> broken, CallbackInfo ci) {
        if (!entity.getWorld().isClient && amount > 0 && !(entity instanceof net.minecraft.entity.player.PlayerEntity player && player.getAbilities().creativeMode))
            RustCoating.wear((ItemStack)(Object)this, (int)Math.min(72000L, (long)amount * 20));
    }
}