package com.tetanusmod.test.mixin;

import com.tetanusmod.test.GraphicalGloveAudit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class GloveProbeSkinMixin {
    private boolean rustruin$probe(){return GraphicalGloveAudit.active&&(Object)this==MinecraftClient.getInstance().player;}
    @Inject(method="getSkinTexture",at=@At("HEAD"),cancellable=true)
    private void skin(CallbackInfoReturnable<Identifier> cir){if(rustruin$probe())cir.setReturnValue(GraphicalGloveAudit.skin);}
    @Inject(method="getModel",at=@At("HEAD"),cancellable=true)
    private void model(CallbackInfoReturnable<String> cir){if(rustruin$probe())cir.setReturnValue(GraphicalGloveAudit.model);}
}
