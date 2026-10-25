package com.tetanusmod.test.mixin;

import com.tetanusmod.test.InfectionAudioClientProbe;
import net.minecraft.client.sound.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public abstract class InfectionAudioClientProbeMixin {
    @Inject(method="play(Lnet/minecraft/client/sound/SoundInstance;)V",at=@At("TAIL"))
    private void hear(SoundInstance sound,CallbackInfo ci){InfectionAudioClientProbe.observe(sound);}
}
