package com.tetanusmod.client;

import com.tetanusmod.beacon.*;
import com.tetanusmod.mixin.SlotPositionAccessor;
import com.tetanusmod.registry.ModItems;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.UpdateBeaconC2SPacket;
import net.minecraft.screen.BeaconScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.*;
import java.util.function.*;

/** A compact steel beacon panel using native icons, slot IDs and payment packets. */
public final class TetanizedBeaconScreen extends HandledScreen<BeaconScreenHandler> {
    public static final int PANEL_WIDTH=276;
    public static final int PANEL_HEIGHT=224;
    private static final int INVENTORY_X=57,INVENTORY_Y=141,HOTBAR_Y=199;
    private static final int INGOT_X=140,STAR_X=169,PAYMENT_Y=111;
    private static final StatusEffect[] EFFECTS={
        StatusEffects.SPEED,StatusEffects.HASTE,StatusEffects.RESISTANCE,
        StatusEffects.JUMP_BOOST,StatusEffects.STRENGTH
    };
    private final List<PowerButton> choices=new ArrayList<>();
    private final ItemStack beaconIcon=new ItemStack(ModBeacons.ITEM);
    private final ItemStack ingotIcon=new ItemStack(Items.NETHERITE_INGOT);
    private final ItemStack starIcon=new ItemStack(ModItems.TETANIZED_WITHER_STAR);
    private StatusEffect primary,secondary;
    private boolean levelIV,edited;

    public TetanizedBeaconScreen(TetanizedBeaconScreenHandler handler,PlayerInventory inventory,Text title) {
        super(handler,inventory,title);
        backgroundWidth=PANEL_WIDTH;backgroundHeight=PANEL_HEIGHT;
    }
    private TetanizedBeaconScreenHandler beaconMenu() { return (TetanizedBeaconScreenHandler)handler; }

    @Override protected void init() {
        super.init();choices.clear();
        // Keep native slot IDs intact. Only the client decides their pixel positions.
        for(int i=1;i<37;i++) {
            int column=i<=27?(i-1)%9:i-28;
            int row=i<=27?(i-1)/9:0;
            positionSlot(i,INVENTORY_X+1+column*18,(i<=27?INVENTORY_Y+row*18:HOTBAR_Y)+1);
        }
        positionSlot(0,INGOT_X+1,PAYMENT_Y+1);
        positionSlot(TetanizedBeaconScreenHandler.STAR_SLOT,STAR_X+1,PAYMENT_Y+1);

        for(int i=0;i<EFFECTS.length;i++) {
            var effect=EFFECTS[i];
            add(new PowerButton(x+10,y+40+i*17,118,16,
                ()->Text.translatable(effect.getTranslationKey()),()->selectPrimary(effect),
                ()->primary==effect,()->BeaconPower.primaryAllowed(handler.getProperties(),effect),
                ()->effect,"gui.tetanusmod.beacon.primary_hint"));
        }
        add(new PowerButton(x+140,y+40,39,19,()->Text.translatable("gui.tetanusmod.beacon.level_ii"),
            ()->selectStrength(2),()->primary!=null&&selectedStrength()==2,()->primary!=null,
            ()->null,"gui.tetanusmod.beacon.level_ii_hint"));
        add(new PowerButton(x+183,y+40,39,19,()->Text.translatable("gui.tetanusmod.beacon.upgrade_iii"),
            ()->selectStrength(3),()->primary!=null&&selectedStrength()==3,
            ()->handler.getProperties()>=4&&primary!=null,()->null,"gui.tetanusmod.beacon.upgrade_iii_hint"));
        add(new PowerButton(x+226,y+40,40,19,()->Text.translatable("gui.tetanusmod.beacon.upgrade_iv"),
            ()->selectStrength(4),()->primary!=null&&selectedStrength()==4,
            ()->handler.getProperties()>=4&&primary!=null,()->null,"gui.tetanusmod.beacon.upgrade_iv_hint"));
        add(new PowerButton(x+140,y+80,126,18,
            ()->Text.translatable(StatusEffects.REGENERATION.getTranslationKey()).append(" II"),
            ()->{edited=true;secondary=secondary==StatusEffects.REGENERATION?null:StatusEffects.REGENERATION;levelIV=false;},
            ()->secondary==StatusEffects.REGENERATION,()->handler.getProperties()>=4&&primary!=null,
            ()->StatusEffects.REGENERATION,"gui.tetanusmod.beacon.regeneration_hint"));
        add(new PowerButton(x+192,y+111,74,18,()->Text.translatable("gui.tetanusmod.beacon.activate"),
            this::activate,()->false,this::canActivate,()->null,"gui.tetanusmod.beacon.confirm_hint"));
    }
    private void positionSlot(int index,int left,int top) {
        var slot=(SlotPositionAccessor)handler.getSlot(index);
        slot.rustruin$x(left);slot.rustruin$y(top);
    }
    private void add(PowerButton button) { choices.add(button);addDrawableChild(button); }

    private void selectPrimary(StatusEffect effect) {
        edited=true;
        boolean upgrade=primary!=null&&secondary==primary;
        primary=effect;
        if(upgrade)secondary=effect;
        // Selecting an effect is idempotent; changing strength has its own visible control.
    }
    private void selectStrength(int strength) {
        edited=true;levelIV=strength==4;
        secondary=strength>=3?primary:null;
    }
    private int selectedStrength() { return primary!=null&&secondary==primary?(levelIV?4:3):2; }
    private boolean canActivate() {
        return handler.getSlot(0).getStack().isOf(Items.NETHERITE_INGOT)
            &&BeaconPower.selectionAllowed(handler.getProperties(),primary,secondary)
            &&(!levelIV||(handler.getProperties()>=4&&secondary==primary&&beaconMenu().hasStar()));
    }
    private void activate() {
        if(!canActivate()||client==null||client.player==null||client.interactionManager==null||client.getNetworkHandler()==null)return;
        client.interactionManager.clickButton(handler.syncId,levelIV?TetanizedBeaconScreenHandler.SELECT_LEVEL_IV:TetanizedBeaconScreenHandler.SELECT_STANDARD);
        client.getNetworkHandler().sendPacket(new UpdateBeaconC2SPacket(Optional.ofNullable(primary),Optional.ofNullable(secondary)));
        client.player.closeHandledScreen();
    }
    @Override protected void handledScreenTick() {
        super.handledScreenTick();
        if(!edited) {
            primary=handler.getPrimaryEffect();secondary=handler.getSecondaryEffect();
            levelIV=beaconMenu().isLevelIV()&&primary!=null&&secondary==primary;
        }
        choices.forEach(PowerButton::refresh);
    }
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta) {
        renderBackground(context);
        super.render(context,mouseX,mouseY,delta);
        drawMouseoverTooltip(context,mouseX,mouseY);
        if(over(mouseX,mouseY,INGOT_X,PAYMENT_Y,18,18)&&!handler.getSlot(0).hasStack())
            context.drawTooltip(textRenderer,List.of(Text.translatable("item.minecraft.netherite_ingot").formatted(Formatting.WHITE),
                Text.translatable("gui.tetanusmod.beacon.ingot_hint").formatted(Formatting.GRAY)),mouseX,mouseY);
        if(over(mouseX,mouseY,STAR_X,PAYMENT_Y,18,18)&&!handler.getSlot(TetanizedBeaconScreenHandler.STAR_SLOT).hasStack())
            context.drawTooltip(textRenderer,List.of(Text.translatable("item.tetanusmod.tetanized_wither_star").formatted(Formatting.GOLD),
                Text.translatable("gui.tetanusmod.beacon.star_hint").formatted(Formatting.GRAY)),mouseX,mouseY);
        if(over(mouseX,mouseY,230,5,37,24))
            context.drawTooltip(textRenderer,Text.translatable("gui.tetanusmod.beacon.pyramid",handler.getProperties()),mouseX,mouseY);
    }
    private boolean over(int mx,int my,int left,int top,int width,int height) {
        return mx>=x+left&&mx<x+left+width&&my>=y+top&&my<y+top+height;
    }

    @Override protected void drawBackground(DrawContext context,float delta,int mouseX,int mouseY) {
        // Familiar Minecraft bevels, cool steel and small oxidized joints. No background text or fake slots.
        context.fill(x-2,y-2,x+PANEL_WIDTH+2,y+PANEL_HEIGHT+2,0xFF191B1D);
        context.fill(x,y,x+PANEL_WIDTH,y+PANEL_HEIGHT,0xFFC5C6C4);
        context.fill(x,y,x+PANEL_WIDTH-1,y+1,0xFFF0F0E9);
        context.fill(x,y,x+1,y+PANEL_HEIGHT-1,0xFFF0F0E9);
        context.fill(x+1,y+PANEL_HEIGHT-2,x+PANEL_WIDTH,y+PANEL_HEIGHT,0xFF5C6063);
        context.fill(x+PANEL_WIDTH-2,y+1,x+PANEL_WIDTH,y+PANEL_HEIGHT,0xFF5C6063);
        context.fill(x+5,y+5,x+PANEL_WIDTH-5,y+29,0xFF454C52);
        context.fill(x+6,y+28,x+PANEL_WIDTH-6,y+30,0xFF916342);
        context.drawItem(beaconIcon,x+10,y+9);

        for(int corner:new int[]{7,PANEL_WIDTH-10}) {
            context.fill(x+corner,y+PANEL_HEIGHT-7,x+corner+3,y+PANEL_HEIGHT-4,0xFF777B7C);
            context.fill(x+corner,y+PANEL_HEIGHT-7,x+corner+2,y+PANEL_HEIGHT-6,0xFFE5E5DC);
        }
        inset(context,x+138,y+62,130,15,0xFF737A7A);
        for(int row=0;row<3;row++)for(int column=0;column<9;column++)
            slot(context,x+INVENTORY_X+column*18,y+INVENTORY_Y+row*18);
        for(int column=0;column<9;column++)slot(context,x+INVENTORY_X+column*18,y+HOTBAR_Y);
        slot(context,x+INGOT_X,y+PAYMENT_Y);
        slot(context,x+STAR_X,y+PAYMENT_Y);
        if(!handler.getSlot(0).hasStack())ghost(context,ingotIcon,INGOT_X,PAYMENT_Y);
        if(!handler.getSlot(TetanizedBeaconScreenHandler.STAR_SLOT).hasStack())ghost(context,starIcon,STAR_X,PAYMENT_Y);
    }
    private void ghost(DrawContext context,ItemStack item,int left,int top) {
        context.drawItem(item,x+left+1,y+top+1);
        // Dim an empty native payment slot; inventory contents remain rendered by HandledScreen.
        context.getMatrices().push();
        context.getMatrices().translate(0,0,200);
        context.fill(x+left+1,y+top+1,x+left+17,y+top+17,0xA0787C7A);
        context.getMatrices().pop();
    }
    private static void inset(DrawContext context,int left,int top,int width,int height,int fill) {
        context.fill(left,top,left+width,top+height,0xFF64696B);
        context.fill(left+1,top+1,left+width-1,top+height-1,fill);
        context.fill(left+1,top+height-1,left+width,top+height,0xFFF0EFE8);
        context.fill(left+width-1,top+1,left+width,top+height,0xFFE0E0D8);
    }
    private static void slot(DrawContext context,int left,int top) { inset(context,left,top,18,18,0xFF888D8D); }

    @Override protected void drawForeground(DrawContext context,int mouseX,int mouseY) {
        context.drawTextWithShadow(textRenderer,title,31,8,0xFFF1F1E8);
        context.drawText(textRenderer,Text.translatable(handler.getProperties()>0
            ?"gui.tetanusmod.beacon.passive_active":"gui.tetanusmod.beacon.passive_inactive"),31,19,
            handler.getProperties()>0?0xFFCCE0C1:0xFFCDD0CC,false);
        context.drawCenteredTextWithShadow(textRenderer,Text.literal(handler.getProperties()+"/4"),249,8,0xFFF1DFC0);
        context.drawCenteredTextWithShadow(textRenderer,Text.translatable("gui.tetanusmod.beacon.layers"),249,19,0xFFD5D8D6);

        context.drawText(textRenderer,Text.translatable("gui.tetanusmod.beacon.primary"),11,31,0xFF404344,false);
        context.drawText(textRenderer,Text.translatable("gui.tetanusmod.beacon.strength"),141,31,0xFF404344,false);
        Text preview=primary==null?Text.translatable("gui.tetanusmod.beacon.choose_effect")
            :Text.translatable(primary.getTranslationKey()).append(" "+roman(selectedStrength()));
        context.drawCenteredTextWithShadow(textRenderer,preview,203,66,primary==null?0xFFDDDFD9:0xFFFFE3AC);
        context.drawText(textRenderer,Text.translatable(levelIV?"gui.tetanusmod.beacon.cost_iv":"gui.tetanusmod.beacon.cost_standard"),140,101,0xFF404344,false);
        context.drawText(textRenderer,Text.literal("+"),161,116,levelIV?0xFF705438:0xFF92938F,false);
        context.drawText(textRenderer,playerInventoryTitle,INVENTORY_X,130,0xFF404344,false);
    }
    private static String roman(int strength) { return strength==4?"IV":strength==3?"III":"II"; }

    private final class PowerButton extends PressableWidget {
        private final Supplier<Text> label;
        private final Runnable action;
        private final BooleanSupplier selected,enabled;
        private final Supplier<StatusEffect> effect;
        PowerButton(int left,int top,int width,int height,Supplier<Text> label,Runnable action,
                BooleanSupplier selected,BooleanSupplier enabled,Supplier<StatusEffect> effect,String hint) {
            super(left,top,width,height,label.get());
            this.label=label;this.action=action;this.selected=selected;this.enabled=enabled;this.effect=effect;
            setTooltip(Tooltip.of(Text.translatable(hint)));refresh();
        }
        void refresh() { active=enabled.getAsBoolean();setMessage(label.get()); }
        @Override public void onPress() {
            if(active){action.run();choices.forEach(PowerButton::refresh);}
        }
        @Override protected void renderButton(DrawContext context,int mouseX,int mouseY,float delta) {
            refresh();
            boolean chosen=selected.getAsBoolean(),hover=active&&(isHovered()||isFocused());
            int left=getX(),top=getY(),right=left+getWidth(),bottom=top+getHeight();
            context.fill(left,top,right,bottom,0xFF292D30);
            context.fill(left+1,top+1,right-1,bottom-1,!active?0xFF878A88:chosen?0xFF886447:hover?0xFFABB5B8:0xFF929A9D);
            context.fill(left+1,top+1,right-1,top+2,chosen?0xFFE5BE7F:!active?0xFFA3A5A0:0xFFE5E8E5);
            context.fill(left+1,top+2,left+2,bottom-1,chosen?0xFFB99260:0xFFC3C8C6);
            context.fill(left+2,bottom-2,right-1,bottom-1,chosen?0xFF62472F:0xFF626A6D);
            context.fill(right-2,top+2,right-1,bottom-1,chosen?0xFF62472F:0xFF626A6D);
            if(chosen)context.fill(left+3,top+3,left+5,bottom-3,0xFFFFDB8B);
            if(isFocused()) {
                context.fill(left-1,top-1,right+1,top,0xFFFFE6A6);
                context.fill(left-1,bottom,right+1,bottom+1,0xFFFFE6A6);
            }
            var icon=effect.get();int textColor=!active?0xFFD0D1CA:chosen?0xFFFFEFCD:0xFFFFFFFF;
            int textY=top+(getHeight()-8)/2;
            if(icon!=null&&client!=null) {
                context.drawSprite(left+7,top+(getHeight()-14)/2,0,14,14,client.getStatusEffectSpriteManager().getSprite(icon));
                String visible=textRenderer.trimToWidth(getMessage().getString(),getWidth()-29);
                context.drawTextWithShadow(textRenderer,Text.literal(visible),left+25,textY,textColor);
            }else context.drawCenteredTextWithShadow(textRenderer,getMessage(),left+getWidth()/2,textY,textColor);
        }
        @Override protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }
}
