package com.tetanusmod.mixin;

import com.tetanusmod.entity.TetanusTntEntity;
import net.minecraft.entity.TntEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntEntity.class)
public abstract class TetanusTntExplosionMixin {
    @ModifyArg(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/world/World$ExplosionSourceType;)Lnet/minecraft/world/explosion/Explosion;"),
            index = 4)
    private float rustruin$doubleBlastPower(float power) {
        return (Object) this instanceof TetanusTntEntity ? power * 2.0F : power;
    }

    @Inject(method = "explode", at = @At("TAIL"))
    private void rustruin$rustBurst(CallbackInfo ci) {
        if ((Object) this instanceof TetanusTntEntity tnt) tnt.leaveRustCloud();
    }
}
