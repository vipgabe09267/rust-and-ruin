package com.tetanusmod.mixin;
import com.tetanusmod.effect.Infection;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(PotionEntity.class)
public abstract class DiseaseSplashPotionMixin {
    @ModifyArg(method="applySplashPotion",at=@At(value="INVOKE",target="Lnet/minecraft/entity/LivingEntity;addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z"),index=0)
    private StatusEffectInstance rustruin$randomDuration(StatusEffectInstance effect){return Infection.potionEffect(effect);}
}