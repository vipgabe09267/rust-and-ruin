package com.tetanusmod.test.mixin;

import com.tetanusmod.test.AdvancementTooltipClientProbe;
import com.tetanusmod.test.GraphicalAdvancementLayoutAudit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Passive per-frame boundary: observations reset before rendering, screenshots after rendering. */
@Mixin(GameRenderer.class)
public abstract class AdvancementFrameCaptureProbeMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void beginNativeFrame(float delta,long startTime,boolean tick,CallbackInfo ci){AdvancementTooltipClientProbe.frameStart();}
    @Inject(method="render",at=@At("TAIL"))
    private void completeNativeFrame(float delta,long startTime,boolean tick,CallbackInfo ci){GraphicalAdvancementLayoutAudit.afterNativeFrame(MinecraftClient.getInstance());}
}
