package com.tetanusmod.mixin;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.client.AdvancementNavigation;
import net.minecraft.advancement.Advancement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve vanilla layout and bounds, but start the Rust & Ruin tree at its root. */
@Mixin(AdvancementTab.class)
public abstract class AdvancementTabNavigationMixin implements AdvancementNavigation {
    @Shadow @Final private Advancement root;
    @Shadow @Final private AdvancementWidget rootWidget;
    @Shadow private double originX;
    @Shadow private double originY;
    @Shadow private int minPanX;
    @Shadow private int minPanY;
    @Shadow private int maxPanX;
    @Shadow private int maxPanY;
    @Shadow private boolean initialized;

    @Inject(method="render",at=@At("HEAD"))
    private void beginAtRoot(DrawContext context,int x,int y,CallbackInfo ci) {
        if(!initialized && root.getId().equals(TetanusMod.id("root"))) rustruin$focusRoot();
    }

    @Inject(method="addAdvancement",at=@At("TAIL"))
    private void keepGrowingTreeReachable(Advancement advancement,CallbackInfo ci) {
        if(!initialized || !root.getId().equals(TetanusMod.id("root")))return;
        // More nodes may arrive while this screen is open. Native dragging is
        // disabled on an axis whose whole tree fits, so update that centering
        // when its bounds grow. Keep the player's vertical exploration intact.
        originX=maxPanX-minPanX<=234
            ?117.0-(minPanX+maxPanX)/2.0
            :MathHelper.clamp(originX,234.0-maxPanX,0.0);
    }

    public void rustruin$focusRoot() {
        if(!root.getId().equals(TetanusMod.id("root"))) return;
        // Match the native 234x113 page and native drag limits. Trees that fit
        // retain normal centering; larger trees keep the root fully visible.
        originX=117.0-(minPanX+maxPanX)/2.0;
        originY=maxPanY-minPanY>113
            ?MathHelper.clamp(56.0-rootWidget.getY()-13.0,113.0-maxPanY,-minPanY)
            :56.0-(minPanY+maxPanY)/2.0;
        initialized=true;
    }
}
