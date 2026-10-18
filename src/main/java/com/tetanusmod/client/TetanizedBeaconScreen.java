package com.tetanusmod.client;

import com.tetanusmod.beacon.TetanizedBeaconScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.BeaconScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.text.Text;

public final class TetanizedBeaconScreen extends BeaconScreen {
    public TetanizedBeaconScreen(TetanizedBeaconScreenHandler handler,PlayerInventory inventory,Text title) { super(handler,inventory,title); }
    @Override protected void drawBackground(DrawContext context,float delta,int mouseX,int mouseY) {
        super.drawBackground(context,delta,mouseX,mouseY);
        context.getMatrices().push();context.getMatrices().translate(0,0,400);
        context.fill(x+18,y+107,x+127,y+127,0xFFC6C6C6);
        context.drawItem(new ItemStack(Items.NETHERITE_INGOT),x+20,y+109);
        context.drawText(textRenderer,Text.translatable("gui.tetanusmod.netherite_only"),x+41,y+113,0xFF40352C,false);
        context.getMatrices().pop();
    }
    @Override protected void drawForeground(DrawContext context,int mouseX,int mouseY) {
        context.drawCenteredTextWithShadow(textRenderer,Text.translatable("gui.tetanusmod.primary"),62,10,0xFFF0C68C);
        context.drawCenteredTextWithShadow(textRenderer,Text.translatable("gui.tetanusmod.secondary"),169,10,0xFFF0C68C);
    }
}