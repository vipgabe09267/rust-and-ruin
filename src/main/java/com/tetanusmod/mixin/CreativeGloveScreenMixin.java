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
        com.tetanusmod.client.GloveSlotFrame.draw(context,left,top,self.getScreenHandler().getSlot(46).getStack().isEmpty());
    }
}
