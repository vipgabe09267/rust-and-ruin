package com.tetanusmod.mixin;
import com.tetanusmod.effect.Infection;
import net.minecraft.item.PotionItem;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(PotionItem.class)
public abstract class DiseasePotionItemMixin {
    @ModifyArg(method="finishUsing",at=@At(value="INVOKE",target="Lnet/minecraft/entity/LivingEntity;addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;)Z"),index=0)
    private StatusEffectInstance rustruin$randomDuration(StatusEffectInstance effect){return Infection.potionEffect(effect);}
}