package com.tetanusmod.client;

import com.tetanusmod.medical.PrescriptionWriterScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Native foreground text stays crisp, unshadowed and separate from the station artwork. */
public final class PrescriptionWriterScreen extends HandledScreen<PrescriptionWriterScreenHandler> {
    private static final Identifier FONT=new Identifier("minecraft","default");
    private static final int INK=0xFF334A45;

    public PrescriptionWriterScreen(PrescriptionWriterScreenHandler handler,PlayerInventory inventory,Text title){
        super(handler,inventory,title);backgroundWidth=176;backgroundHeight=172;titleY=7;playerInventoryTitleY=77;
    }
    @Override protected void drawBackground(DrawContext draw,float delta,int mouseX,int mouseY){
        draw.fill(x-2,y-2,x+178,y+174,0xFF263D3A);
        draw.fill(x,y,x+176,y+172,0xFFD6DDDA);
        draw.drawBorder(x,y,176,172,0xFF819792);
        draw.fill(x+7,y+21,x+169,y+73,0xFFF2F0E8);
        draw.drawBorder(x+7,y+21,162,52,0xFFB9C8C1);
        draw.fill(x+8,y+21,x+10,y+73,0xFF2D7169);
        slot(draw,x+35,y+38);slot(draw,x+71,y+38);slot(draw,x+131,y+38);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)slot(draw,x+8+col*18,y+88+row*18);
        for(int col=0;col<9;col++)slot(draw,x+8+col*18,y+146);
    }
    @Override protected void drawForeground(DrawContext draw,int mouseX,int mouseY){
        draw.drawText(textRenderer,nativeText(title),titleX,titleY,INK,false);
        draw.drawText(textRenderer,nativeText(playerInventoryTitle),playerInventoryTitleX,playerInventoryTitleY,INK,false);
        centered(draw,Text.translatable("gui.tetanusmod.writer.paper"),43,26);
        centered(draw,Text.translatable("gui.tetanusmod.writer.ink"),79,26);
        centered(draw,Text.translatable("gui.tetanusmod.writer.result"),139,26);
        centered(draw,Text.translatable("gui.tetanusmod.writer.hint"),88,62);
        draw.drawText(textRenderer,nativeText(Text.literal("+")),58,42,INK,false);
        draw.drawText(textRenderer,nativeText(Text.literal(">")),108,42,INK,false);
    }
    private static Text nativeText(Text text){return text.copy().styled(style->style.withFont(FONT));}
    private void centered(DrawContext draw,Text text,int center,int top){
        Text label=nativeText(text);
        draw.drawText(textRenderer,label,center-textRenderer.getWidth(label)/2,top,INK,false);
    }
    private static void slot(DrawContext draw,int x,int y){
        draw.fill(x-1,y-1,x+17,y+17,0xFF788F86);draw.fill(x,y,x+17,y+17,0xFFFFFFFF);draw.fill(x,y,x+16,y+16,0xFFB7C4BB);
    }
    @Override public void render(DrawContext draw,int mouseX,int mouseY,float delta){
        renderBackground(draw);super.render(draw,mouseX,mouseY,delta);drawMouseoverTooltip(draw,mouseX,mouseY);
    }
}
