package com.tetanusmod.client;

import com.tetanusmod.TetanusMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.*;
import net.minecraft.util.math.MathHelper;
import java.util.List;

/** Local, read-only reference: no NBT scripts, links, commands or gameplay packets. */
public final class FieldGuideScreen extends Screen {
    private static final String[] CHAPTERS={"overview","diseases","prevention","weapons","crafting","brewing","creatures","salvage","bosses","commands"};
    private int chapter,scroll,left,top,panelWidth,panelHeight;
    public FieldGuideScreen(){super(Text.translatable("guide.tetanusmod.title"));}
    @Override protected void init() {
        panelWidth=Math.min(440,width-12);panelHeight=Math.min(250,height-12);left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        for(int i=0;i<CHAPTERS.length;i++) {
            final int selected=i;
            addDrawableChild(ButtonWidget.builder(Text.translatable("guide.tetanusmod."+CHAPTERS[i]+".title"),button->selectChapter(selected)).dimensions(left+8,top+32+i*19,96,18).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("X"),button->close()).dimensions(left+panelWidth-26,top+6,20,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("^"),button->scrollBy(-3)).dimensions(left+panelWidth-52,top+panelHeight-24,20,18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("v"),button->scrollBy(3)).dimensions(left+panelWidth-28,top+panelHeight-24,20,18).build());
    }
    public void selectChapter(int index){chapter=MathHelper.clamp(index,0,CHAPTERS.length-1);scroll=0;}
    public int chapterCount(){return CHAPTERS.length;}
    public Text body() {
        var cfg=TetanusMod.CONFIG;
        return switch(chapter) {
            case 1->Text.translatable("guide.tetanusmod.diseases.body",cfg.minDurationSeconds,cfg.maxDurationSeconds);
            case 2->Text.translatable("guide.tetanusmod.prevention.body",Math.round(cfg.vaccineProtectionChance*100),(cfg.vaccineDurationSeconds%60==0?cfg.vaccineDurationSeconds/60+" minutes":cfg.vaccineDurationSeconds+" seconds"),Math.round(cfg.gloveProtectionChance*100),Math.round((1-(1-cfg.vaccineProtectionChance)*(1-cfg.gloveProtectionChance))*100));
            case 6->Text.translatable("guide.tetanusmod.creatures.body",Math.round(cfg.animalConversionChance*100),Math.round(cfg.mobConversionChance*100));
            default->Text.translatable("guide.tetanusmod."+CHAPTERS[chapter]+".body");
        };
    }
    private List<OrderedText> lines(){return textRenderer.wrapLines(body(),panelWidth-132);}
    private int visibleLines(){return Math.max(1,(panelHeight-90)/11);}
    private void scrollBy(int amount){scroll=MathHelper.clamp(scroll+amount,0,Math.max(0,lines().size()-visibleLines()));}
    @Override public boolean mouseScrolled(double x,double y,double amount){scrollBy(-(int)Math.signum(amount)*3);return true;}
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta) {
        renderBackground(context);
        context.fill(left-2,top-2,left+panelWidth+2,top+panelHeight+2,0xFF34251E);
        context.fill(left,top,left+panelWidth,top+panelHeight,0xFFE3CF9E);
        context.fill(left+110,top+28,left+112,top+panelHeight-8,0xFF9A5A35);
        context.drawText(textRenderer,title,left+10,top+10,0xFF4E2A1D,false);
        context.drawText(textRenderer,Text.translatable("guide.tetanusmod."+CHAPTERS[chapter]+".title"),left+120,top+34,0xFF79411F,false);
        var content=lines();scroll=MathHelper.clamp(scroll,0,Math.max(0,content.size()-visibleLines()));
        for(int row=0;row<visibleLines()&&scroll+row<content.size();row++)context.drawText(textRenderer,content.get(scroll+row),left+120,top+52+row*11,0xFF352B24,false);
        context.drawText(textRenderer,Text.translatable("guide.tetanusmod.scroll",scroll+1,content.size()),left+120,top+panelHeight-20,0xFF6A5742,false);
        super.render(context,mouseX,mouseY,delta);
    }
    @Override public boolean shouldPause(){return false;}
}