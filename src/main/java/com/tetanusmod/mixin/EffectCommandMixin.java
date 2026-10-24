package com.tetanusmod.mixin;

import com.tetanusmod.expansion.CommandEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.command.EffectCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EffectCommand.class)
public abstract class EffectCommandMixin {
    @Redirect(method="executeGive",at=@At(value="INVOKE",target="Lnet/minecraft/entity/LivingEntity;addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z"))
    private static boolean force(LivingEntity target,StatusEffectInstance effect,Entity source){return CommandEffects.apply(target,effect,source);}
}
