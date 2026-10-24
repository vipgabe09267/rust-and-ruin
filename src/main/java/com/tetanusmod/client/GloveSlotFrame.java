package com.tetanusmod.client;

import com.tetanusmod.TetanusMod;
import net.minecraft.client.gui.DrawContext;

public final class GloveSlotFrame {
    public static void draw(DrawContext context,int x,int y,boolean empty){
        context.fill(x,y,x+18,y+18,0xFF8B8B8B);
        context.fill(x,y,x+18,y+1,0xFF373737);context.fill(x,y,x+1,y+18,0xFF373737);
        context.fill(x+1,y+17,x+18,y+18,0xFFFFFFFF);context.fill(x+17,y+1,x+18,y+18,0xFFFFFFFF);
        if(empty)context.drawTexture(TetanusMod.id("textures/gui/empty_glove_slot.png"),x+1,y+1,0,0,16,16,16,16);
    }
}
