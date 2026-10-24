package com.tetanusmod.mixin;

import com.tetanusmod.client.GlovesFeatureRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class FirstPersonGlovesMixin {
    @Inject(method="renderRightArm",at=@At("TAIL"))
    private void right(MatrixStack matrices,VertexConsumerProvider vertices,int light,AbstractClientPlayerEntity player,CallbackInfo ci){GlovesFeatureRenderer.renderArm(matrices,vertices,light,player,((PlayerEntityRenderer)(Object)this).getModel(),Arm.RIGHT);}
    @Inject(method="renderLeftArm",at=@At("TAIL"))
    private void left(MatrixStack matrices,VertexConsumerProvider vertices,int light,AbstractClientPlayerEntity player,CallbackInfo ci){GlovesFeatureRenderer.renderArm(matrices,vertices,light,player,((PlayerEntityRenderer)(Object)this).getModel(),Arm.LEFT);}
}
