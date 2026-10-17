package com.tetanusmod.mixin;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {
    @Inject(method = "renderBossBar(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/entity/boss/BossBar;)V",
            at = @At("HEAD"),cancellable = true)
    private void rustruin$rustBossBar(DrawContext context,int x,int y,BossBar bar,CallbackInfo ci) {
        if (!(bar.getName().getContent() instanceof TranslatableTextContent text)
                || !text.getKey().equals("entity.tetanusmod.tetanized_wither")) return;
        context.fill(x-1,y-1,x+183,y+6,0xFF30221D);
        context.fill(x,y,x+182,y+5,0xFF493A31);
        int width = (int)(182*Math.max(0,Math.min(1,bar.getPercent())));
        context.fill(x,y,x+width,y+5,0xFF9F542C);
        context.fill(x,y,x+width,y+1,0xFFD8954F);
        for (int i=18;i<182;i+=18) context.fill(x+i,y,x+i+1,y+5,0xFF3A2C25);
        ci.cancel();
    }
}
