package com.tetanusmod.test.mixin;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.test.SoundProbe;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class SoundProbeMixin {
    @Inject(method="playSound", at=@At("HEAD"))
    private void record(PlayerEntity except, double x, double y, double z,
            RegistryEntry<SoundEvent> event, SoundCategory category, float volume,
            float pitch, long seed, CallbackInfo ci) {
        if (SoundProbe.enabled && event.value() == TetanusMod.INFECTION_SOUND) {
            SoundProbe.count++;
            SoundProbe.position = new Vec3d(x, y, z);
        }
    }
}
