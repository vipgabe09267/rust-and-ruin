package com.tetanusmod.test.mixin;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ServerPlayerEntity.class)
public interface ServerPlayerTestAccessor {
    @Accessor("joinInvulnerabilityTicks") void rustruin$joinInvulnerability(int ticks);
}