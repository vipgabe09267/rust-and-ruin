package com.tetanusmod.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class GloveInventoryScreenMixin extends net.minecraft.client.gui.screen.ingame.HandledScreen<net.minecraft.screen.PlayerScreenHandler> {
    protected GloveInventoryScreenMixin(){super(null,null,net.minecraft.text.Text.empty());}
    @Inject(method="drawBackground",at=@At("TAIL"))
    private void gloveSlot(DrawContext context,float delta,int mouseX,int mouseY,CallbackInfo ci){
        int left=x+76,top=y+43;
        com.tetanusmod.client.GloveSlotFrame.draw(context,left,top,handler.getSlot(46).getStack().isEmpty());
    }
}
