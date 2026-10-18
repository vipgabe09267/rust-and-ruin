package com.tetanusmod.mixin;

import com.tetanusmod.TetanusMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.MilkBucketItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(MilkBucketItem.class)
public abstract class MilkBucketItemMixin {
    @Redirect(method="finishUsing",at=@At(value="INVOKE",target="Lnet/minecraft/entity/LivingEntity;clearStatusEffects()Z"))
    private boolean rustruin$keepTetanus(LivingEntity user) {
        boolean removed=false;
        for(var effect:java.util.List.copyOf(user.getStatusEffects())) {
            var type=effect.getEffectType();
            if(type!=TetanusMod.TETANUS&&type!=TetanusMod.LOCKJAW)removed|=user.removeStatusEffect(type);
        }
        return removed;
    }
}