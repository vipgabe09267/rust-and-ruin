package com.tetanusmod.test.mixin;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.test.SoundProbe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe the native player-only S2C sound path, which does not call ServerWorld.playSound. */
@Mixin(ServerPlayerEntity.class)
public abstract class PlayerInfectionSoundProbeMixin {
    @Inject(method="playSound(Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",at=@At("HEAD"))
    private void observe(SoundEvent event,SoundCategory category,float volume,float pitch,CallbackInfo ci){
        var player=(ServerPlayerEntity)(Object)this;
        if(event==TetanusMod.INFECTION_SOUND&&SoundProbe.accepts(player.getWorld(),player.getX(),player.getY(),player.getZ())){SoundProbe.count++;SoundProbe.position=player.getPos();}
    }
}
