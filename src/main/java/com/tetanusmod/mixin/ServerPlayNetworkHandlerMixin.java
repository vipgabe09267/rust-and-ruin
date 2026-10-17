package com.tetanusmod.mixin;

import com.tetanusmod.effect.InfectionState;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;
    @Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
    private void tetanus$briefMovementSpasm(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        // Network handlers may first run on the Netty thread. Only touch game state on the server thread.
        if (!player.getServer().isOnThread() || player.hasVehicle() || player.isSpectator()) return;
        if (((InfectionState) player).tetanus$getFreeze() > 0 && packet.changesPosition()) {
            player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(),
                    packet.getYaw(player.getYaw()), packet.getPitch(player.getPitch()));
            ci.cancel();
        }
    }
}
