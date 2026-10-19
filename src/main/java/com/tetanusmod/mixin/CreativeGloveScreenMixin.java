package com.tetanusmod.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemGroup;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeGloveScreenMixin {
    @Shadow public abstract boolean isInventoryTabSelected();

    @Inject(method="setSelectedTab",at=@At("TAIL"))
    private void placeGloves(ItemGroup group,CallbackInfo ci){
        var player=MinecraftClient.getInstance().player;
        if(group.getType()!=ItemGroup.Type.INVENTORY||player==null)return;
        // Vanilla positions every extra slot as another hotbar slot. Reposition just our wrapper.
        var slots=((CreativeInventoryScreen)(Object)this).getScreenHandler().slots;if(slots.size()>46){Slot slot=slots.get(46);((SlotPositionAccessor)slot).rustruin$x(136);((SlotPositionAccessor)slot).rustruin$y(20);}
    }
    @Inject(method="drawBackground",at=@At("TAIL"))
    private void frame(DrawContext context,float delta,int mouseX,int mouseY,CallbackInfo ci){
        if(!isInventoryTabSelected())return;
        var self=(CreativeInventoryScreen)(Object)this;int left=(self.width-195)/2+135,top=(self.height-136)/2+19;
        context.fill(left,top,left+18,top+18,0xFF392920);context.fill(left+1,top+1,left+17,top+17,0xFF80624D);context.drawBorder(left,top,18,18,0xFFB47A45);
    }
}