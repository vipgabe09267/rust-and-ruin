package com.tetanusmod.mixin;

import com.tetanusmod.entity.MobVariants;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.WitherSkullEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WitherSkullEntity.class)
public abstract class WitherSkullEffectMixin {
    @ModifyArg(method = "onEntityHit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z"), index = 0)
    private StatusEffectInstance rustruin$strongerWither(StatusEffectInstance effect) {
        var owner = ((WitherSkullEntity) (Object) this).getOwner();
        if (effect.getEffectType() == StatusEffects.WITHER && owner instanceof WitherEntity wither && MobVariants.permanent(wither)) {
            // Wither III pulses twice as often as the skull's native Wither II at the same duration.
            return new StatusEffectInstance(StatusEffects.WITHER, effect.getDuration(), 2,
                    effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon());
        }
        return effect;
    }
}
