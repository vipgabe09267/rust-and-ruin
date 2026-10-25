package com.tetanusmod.mixin;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.client.AdvancementNavigation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Only the mod tab gains wheel navigation; vanilla tabs keep native behavior. */
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementScreenNavigationMixin extends Screen {
    @Shadow private AdvancementTab selectedTab;
    protected AdvancementScreenNavigationMixin(){super(Text.empty());}

    @Unique private boolean rustruin$selected() {
        return selectedTab!=null && selectedTab.getRoot().getId().equals(TetanusMod.id("root"));
    }

    @Inject(method="render",at=@At("TAIL"))
    private void navigationHint(DrawContext context,int mouseX,int mouseY,float delta,CallbackInfo ci) {
        if(!rustruin$selected())return;
        int y=(height+140)/2+5;
        String hint=client.options.advancementsKey.matchesKey(GLFW.GLFW_KEY_HOME,GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_HOME))?"gui.tetanusmod.advancements.navigation_mouse":"gui.tetanusmod.advancements.navigation";
        if(y+9<=height)context.drawCenteredTextWithShadow(textRenderer,Text.translatable(hint),width/2,y,0xD7BA9A);
    }

    @Override public boolean mouseScrolled(double mouseX,double mouseY,double amount) {
        int left=(width-252)/2+9,top=(height-140)/2+18;
        if(rustruin$selected() && mouseX>=left && mouseX<left+234 && mouseY>=top && mouseY<top+113) {
            selectedTab.move(Screen.hasShiftDown()?amount*27.0:0.0,Screen.hasShiftDown()?0.0:amount*27.0);
            return true;
        }
        return super.mouseScrolled(mouseX,mouseY,amount);
    }

    @Inject(method="keyPressed",at=@At("HEAD"),cancellable=true)
    private void returnToStart(int key,int scanCode,int modifiers,CallbackInfoReturnable<Boolean> cir) {
        if(rustruin$selected() && key==GLFW.GLFW_KEY_HOME && !client.options.advancementsKey.matchesKey(key,scanCode)) {
            ((AdvancementNavigation)selectedTab).rustruin$focusRoot();
            cir.setReturnValue(true);
        }
    }
}
