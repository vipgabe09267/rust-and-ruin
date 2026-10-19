package com.tetanusmod.mixin;

import com.tetanusmod.expansion.GloveInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class GloveRespawnMixin {
    @Inject(method="copyFrom",at=@At("TAIL"))
    private void copy(ServerPlayerEntity old,boolean alive,CallbackInfo ci){
        var self=(ServerPlayerEntity)(Object)this;
        if(alive||self.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)||old.isSpectator())
            ((GloveInventory)self).rustruin$gloves().setStack(0,((GloveInventory)old).rustruin$gloves().getStack(0).copy());
    }
}