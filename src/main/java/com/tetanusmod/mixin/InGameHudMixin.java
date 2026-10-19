package com.tetanusmod.mixin;
import com.tetanusmod.TetanusMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "drawHeart",at = @At("HEAD"),cancellable = true)
    private void rustruin$rustyHearts(DrawContext context,@Coerce Object type,int x,int y,int v,
            boolean blinking,boolean half,CallbackInfo ci) {
        var player = MinecraftClient.getInstance().player;
        if (player == null || (!player.hasStatusEffect(TetanusMod.TETANUS)&&!player.hasStatusEffect(com.tetanusmod.expansion.ExpansionEffects.RABIES))) return;
        String name = ((Enum<?>)type).name();
        // Preserve empty containers and absorption information. Only health becomes rusty.
        if (name.equals("CONTAINER") || name.equals("ABSORBING")) return;
        boolean rabies=player.hasStatusEffect(com.tetanusmod.expansion.ExpansionEffects.RABIES);
        context.drawTexture(TetanusMod.id(rabies?(half?"textures/gui/rabies_heart_half.png":"textures/gui/rabies_heart.png"):(half ? "textures/gui/rust_heart_half.png" : "textures/gui/rust_heart.png")),x,y,0,0,9,9,9,9);
        ci.cancel();
    }
}
