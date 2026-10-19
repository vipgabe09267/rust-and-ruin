package com.tetanusmod.mixin;

import com.tetanusmod.expansion.GloveInventory;
import com.tetanusmod.registry.ModItems;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.server.network.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extend vanilla creative synchronization to the one extra equipment slot. */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class CreativeGloveNetworkMixin {
    @Shadow public ServerPlayerEntity player;
    @Inject(method="onCreativeInventoryAction",at=@At("HEAD"),cancellable=true)
    private void gloves(CreativeInventoryActionC2SPacket packet,CallbackInfo ci){
        if(packet.getSlot()!=46)return;
        NetworkThreadUtils.forceMainThread(packet,(ServerPlayNetworkHandler)(Object)this,player.getServerWorld());
        ci.cancel();if(!player.interactionManager.isCreative()||player.isSpectator())return;
        var stack=packet.getItemStack();
        if(!stack.isEmpty()&&(!stack.isOf(ModItems.PROTECTIVE_GLOVES)||stack.getCount()!=1))return;
        ((GloveInventory)player).rustruin$gloves().setStack(0,stack.copy());player.playerScreenHandler.sendContentUpdates();
    }
}