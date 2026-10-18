package com.tetanusmod.mixin;

import com.tetanusmod.client.TetanizedBeaconScreen;
import net.minecraft.client.gui.screen.ingame.BeaconScreen;
import net.minecraft.entity.effect.*;
import net.minecraft.text.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="net.minecraft.client.gui.screen.ingame.BeaconScreen$EffectButtonWidget")
public abstract class BeaconButtonMixin {
    @Shadow @Final private boolean primary;
    @Shadow @Final BeaconScreen field_2811;
    @Inject(method="getEffectName",at=@At("RETURN"),cancellable=true)
    private void strongerNames(StatusEffect effect,CallbackInfoReturnable<MutableText> cir) {
        if(field_2811 instanceof TetanizedBeaconScreen)cir.setReturnValue(Text.translatable(effect.getTranslationKey()).append(primary||effect==StatusEffects.REGENERATION?" II":" III"));
    }
}