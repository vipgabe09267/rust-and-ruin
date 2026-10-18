package com.tetanusmod.mixin;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.client.InfectedPlayerList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.Identifier;
import java.util.UUID;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHeartsMixin {
    @Unique private boolean rustruin$infected;
    @Inject(method="renderHearts",at=@At("HEAD"))
    private void player(int y,int start,int end,UUID uuid,DrawContext context,int score,CallbackInfo ci) {
        rustruin$infected=InfectedPlayerList.contains(uuid);
    }
    @Redirect(method="renderHearts",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"))
    private void hearts(DrawContext context,Identifier texture,int x,int y,int u,int v,int width,int height) {
        if(rustruin$infected&&u>=52) {
            boolean half=u==61||u==79||u==169;
            context.drawTexture(TetanusMod.id(half?"textures/gui/rust_heart_half.png":"textures/gui/rust_heart.png"),x,y,0,0,width,height,9,9);
        }else context.drawTexture(texture,x,y,u,v,width,height);
    }
    @Redirect(method="renderHearts",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I"))
    private int numeric(DrawContext context,TextRenderer font,String value,int x,int y,int color) {
        return context.drawTextWithShadow(font,value,x,y,rustruin$infected?0xFFB86E3C:color);
    }
}