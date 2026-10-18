package com.tetanusmod.mixin;
import com.tetanusmod.client.TetanizedTextures;
import com.tetanusmod.entity.MobVariants;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EnderDragonEntityRenderer.class)
public abstract class DragonTextureMixin {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private static Identifier TEXTURE;
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private static net.minecraft.client.render.RenderLayer DRAGON_CUTOUT;
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private static net.minecraft.client.render.RenderLayer DRAGON_DECAL;
    @Redirect(method="render(Lnet/minecraft/entity/boss/dragon/EnderDragonEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;"))
    private net.minecraft.client.render.VertexConsumer dragonLayer(net.minecraft.client.render.VertexConsumerProvider provider,net.minecraft.client.render.RenderLayer layer,EnderDragonEntity entity,float yaw,float delta,net.minecraft.client.util.math.MatrixStack matrices,net.minecraft.client.render.VertexConsumerProvider ignored,int light) {
        if(MobVariants.permanent(entity)&&(layer==DRAGON_CUTOUT||layer==DRAGON_DECAL))layer=net.minecraft.client.render.RenderLayer.getEntityCutoutNoCull(TetanizedTextures.get(TEXTURE));
        return provider.getBuffer(layer);
    }
    @Inject(method="getTexture(Lnet/minecraft/entity/boss/dragon/EnderDragonEntity;)Lnet/minecraft/util/Identifier;",at=@At("RETURN"),cancellable=true)
    private void texture(EnderDragonEntity entity,CallbackInfoReturnable<Identifier> cir) {
        if(MobVariants.permanent(entity))cir.setReturnValue(TetanizedTextures.get(cir.getReturnValue()));
    }
}