package com.tetanusmod.test.mixin;

import com.tetanusmod.test.AdvancementTooltipClientProbe;
import net.minecraft.advancement.Advancement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes completed native drawing; it never changes input, progress or rendering. */
@Mixin(AdvancementWidget.class)
public abstract class AdvancementTooltipClientProbeMixin {
    @Shadow @Final private Advancement advancement;
    @Inject(method="drawTooltip",at=@At("HEAD"))
    private void tooltipBegins(DrawContext draw,int originX,int originY,float alpha,int x,int y,CallbackInfo ci){AdvancementTooltipClientProbe.begin(advancement.getId());}
    @Inject(method="drawTooltip",at=@At("TAIL"))
    private void tooltipDrawn(DrawContext draw,int originX,int originY,float alpha,int x,int y,CallbackInfo ci){
        AdvancementTooltipClientProbe.rendered(advancement.getId());
    }
}
