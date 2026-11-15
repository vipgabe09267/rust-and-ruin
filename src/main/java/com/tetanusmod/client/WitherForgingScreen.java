package com.tetanusmod.client;
import com.tetanusmod.dragon.WitherForgingScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
public final class WitherForgingScreen extends HandledScreen<WitherForgingScreenHandler>{
    public WitherForgingScreen(WitherForgingScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title);backgroundWidth=176;backgroundHeight=166;titleY=6;playerInventoryTitleY=72;}
    @Override protected void drawBackground(DrawContext draw,float delta,int mouseX,int mouseY){
        draw.fill(x,y,x+176,y+166,0xFF202329);draw.drawBorder(x,y,176,166,0xFF8B959B);draw.fill(x+5,y+19,x+171,y+67,0xFF34313B);
        for(var slot:handler.slots){int sx=x+slot.x,sy=y+slot.y;draw.fill(sx-1,sy-1,sx+17,sy+17,0xFF12151A);draw.fill(sx,sy,sx+16,sy+16,0xFF555B61);draw.fill(sx,sy,sx+16,sy+1,0xFF8C949A);}
        draw.drawText(textRenderer,Text.literal("+"),x+44,y+41,0xFFB18460,false);draw.drawText(textRenderer,Text.literal("+"),x+82,y+41,0xFFB18460,false);draw.drawText(textRenderer,Text.literal("→"),x+120,y+41,0xFFDDB47B,false);
    }
    @Override protected void drawForeground(DrawContext draw,int mouseX,int mouseY){draw.drawText(textRenderer,title,8,6,0xFFE1DFD7,false);draw.drawText(textRenderer,playerInventoryTitle,8,72,0xFFE1DFD7,false);}
    @Override public void render(DrawContext draw,int mouseX,int mouseY,float delta){renderBackground(draw);super.render(draw,mouseX,mouseY,delta);drawMouseoverTooltip(draw,mouseX,mouseY);}
}
