package com.tetanusmod.test.mixin;

import com.tetanusmod.test.GraphicalGloveAudit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class GloveProbeSleevesMixin {
    @Inject(method="isPartVisible",at=@At("HEAD"),cancellable=true)
    private void sleeves(PlayerModelPart part,CallbackInfoReturnable<Boolean> cir){
        if(GraphicalGloveAudit.active&&(Object)this==MinecraftClient.getInstance().player&&(part==PlayerModelPart.LEFT_SLEEVE||part==PlayerModelPart.RIGHT_SLEEVE))cir.setReturnValue(true);
    }
}
