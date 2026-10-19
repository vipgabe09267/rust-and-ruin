package com.tetanusmod.client;

import com.tetanusmod.beacon.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.c2s.play.UpdateBeaconC2SPacket;
import net.minecraft.screen.BeaconScreenHandler;
import net.minecraft.text.Text;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Dedicated rust-and-copper menu; effect names are the actual levels, never mixed II/III labels. */
public final class TetanizedBeaconScreen extends HandledScreen<BeaconScreenHandler> {
    private StatusEffect primary,secondary;private boolean edited;
    private final List<RustButton> choices=new ArrayList<>();private RustButton confirm;
    public TetanizedBeaconScreen(TetanizedBeaconScreenHandler handler,PlayerInventory inventory,Text title){super(handler,inventory,title);backgroundWidth=230;backgroundHeight=219;}
    @Override protected void init(){
        super.init();choices.clear();
        StatusEffect[] effects={StatusEffects.SPEED,StatusEffects.HASTE,StatusEffects.RESISTANCE,StatusEffects.JUMP_BOOST,StatusEffects.STRENGTH};
        for(int i=0;i<effects.length;i++){
            var effect=effects[i];
            var button=new RustButton(x+10,y+31+i*18,98,16,Text.translatable(effect.getTranslationKey()).append(" II"),()->{
                edited=true;primary=effect;if(secondary!=StatusEffects.REGENERATION)secondary=null;
            },()->primary==effect,()->BeaconPower.primaryAllowed(handler.getProperties(),effect));
            choices.add(button);addDrawableChild(button);
        }
        var regeneration=new RustButton(x+118,y+31,102,18,Text.translatable(StatusEffects.REGENERATION.getTranslationKey()).append(" II"),()->{edited=true;secondary=StatusEffects.REGENERATION;},()->secondary==StatusEffects.REGENERATION,()->handler.getProperties()>=4);
        var upgrade=new RustButton(x+118,y+53,102,18,Text.translatable("gui.tetanusmod.upgrade_iii"),()->{edited=true;secondary=primary;},()->primary!=null&&secondary==primary,()->handler.getProperties()>=4&&primary!=null);
        choices.add(regeneration);choices.add(upgrade);addDrawableChild(regeneration);addDrawableChild(upgrade);
        addDrawableChild(new RustButton(x+205,y+5,15,16,Text.literal("X"),this::close,()->false,()->true));
        confirm=new RustButton(x+164,y+108,56,20,Text.translatable("gui.tetanusmod.activate"),()->{
            if(client!=null&&client.getNetworkHandler()!=null&&handler.hasPayment()&&BeaconPower.selectionAllowed(handler.getProperties(),primary,secondary)){
                client.getNetworkHandler().sendPacket(new UpdateBeaconC2SPacket(Optional.ofNullable(primary),Optional.ofNullable(secondary)));client.player.closeHandledScreen();
            }
        },()->false,()->handler.hasPayment()&&BeaconPower.selectionAllowed(handler.getProperties(),primary,secondary));
        addDrawableChild(confirm);
    }
    @Override protected void handledScreenTick(){super.handledScreenTick();if(!edited){primary=handler.getPrimaryEffect();secondary=handler.getSecondaryEffect();}for(var button:choices)button.refresh();if(confirm!=null)confirm.refresh();}
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta){renderBackground(context);super.render(context,mouseX,mouseY,delta);drawMouseoverTooltip(context,mouseX,mouseY);}
    @Override protected void drawBackground(DrawContext context,float delta,int mouseX,int mouseY){
        context.fill(x-2,y-2,x+232,y+221,0xFF1E1512);context.fill(x,y,x+230,y+219,0xFF62402B);
        context.drawBorder(x+2,y+2,226,215,0xFFBA7842);context.fill(x+6,y+24,x+224,y+124,0xFF33251E);
        for(int i=0;i<23;i++){int px=x+4+i*10;context.fill(px,y+3,px+3,y+5,0xFFDCA265);context.fill(px,y+213,px+2,y+216,0xFF311E16);}
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)slot(context,x+35+col*18,y+136+row*18);
        for(int col=0;col<9;col++)slot(context,x+35+col*18,y+194);
        slot(context,x+135,y+109);
        context.drawCenteredTextWithShadow(textRenderer,Text.translatable("gui.tetanusmod.primary"),x+59,y+17,0xFFF0C68C);
        context.drawCenteredTextWithShadow(textRenderer,Text.translatable("gui.tetanusmod.secondary"),x+169,y+17,0xFFF0C68C);
        context.drawCenteredTextWithShadow(textRenderer,Text.translatable("gui.tetanusmod.passive"),x+169,y+78,0xFFBFCF87);
        context.drawCenteredTextWithShadow(textRenderer,Text.translatable("gui.tetanusmod.passive_active"),x+169,y+88,0xFFBFCF87);
        context.drawText(textRenderer,Text.translatable("gui.tetanusmod.netherite_short"),x+117,y+99,0xFFD5B793,false);
    }
    private static void slot(DrawContext context,int left,int top){context.fill(left,top,left+18,top+18,0xFF211A16);context.fill(left+1,top+1,left+17,top+17,0xFF92775C);context.drawBorder(left,top,18,18,0xFFB38C5D);}
    @Override protected void drawForeground(DrawContext context,int mouseX,int mouseY){context.drawText(textRenderer,title,8,7,0xFFF7DCAB,false);}
    private final class RustButton extends PressableWidget {
        private final Runnable action;private final BooleanSupplier selected,enabled;
        RustButton(int left,int top,int width,int height,Text text,Runnable action,BooleanSupplier selected,BooleanSupplier enabled){super(left,top,width,height,text);this.action=action;this.selected=selected;this.enabled=enabled;refresh();}
        void refresh(){active=enabled.getAsBoolean();}
        @Override public void onPress(){if(active)action.run();}
        @Override protected void renderButton(DrawContext context,int mouseX,int mouseY,float delta){
            refresh();if(choices.indexOf(this)>=0&&choices.indexOf(this)<5){StatusEffect[] effects={StatusEffects.SPEED,StatusEffects.HASTE,StatusEffects.RESISTANCE,StatusEffects.JUMP_BOOST,StatusEffects.STRENGTH};var effect=effects[choices.indexOf(this)];setMessage(Text.translatable(effect.getTranslationKey()).append(primary==effect&&secondary==primary?" III":" II"));}int fill=!active?0xFF332D27:selected.getAsBoolean()?0xFF8B612E:isHovered()?0xFF935434:0xFF603D29;
            context.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),fill);context.drawBorder(getX(),getY(),getWidth(),getHeight(),selected.getAsBoolean()?0xFFE3BB6B:0xFFA27247);
            context.drawCenteredTextWithShadow(textRenderer,getMessage(),getX()+getWidth()/2,getY()+(getHeight()-8)/2,active?0xFFF3DBB5:0xFF82776A);
        }
        @Override protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder){appendDefaultNarrations(builder);}
    }
}