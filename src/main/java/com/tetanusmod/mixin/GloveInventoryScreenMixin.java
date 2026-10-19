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
        context.fill(left,top,left+18,top+18,0xFF392920);context.fill(left+1,top+1,left+17,top+17,0xFF80624D);
        context.drawBorder(left,top,18,18,0xFFB47A45);
    }
}