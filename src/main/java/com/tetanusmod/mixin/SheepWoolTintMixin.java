package com.tetanusmod.mixin;
import com.tetanusmod.entity.MobVariants;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.feature.SheepWoolFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.util.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(SheepWoolFeatureRenderer.class)
public abstract class SheepWoolTintMixin {
    @Redirect(method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/passive/SheepEntity;FFFFFF)V",at=@At(value="INVOKE",target="Lnet/minecraft/entity/passive/SheepEntity;getRgbColor(Lnet/minecraft/util/DyeColor;)[F"))
    private float[] wool(DyeColor dye,MatrixStack matrices,VertexConsumerProvider vertices,int light,SheepEntity sheep,float angle,float distance,float delta,float progress,float yaw,float pitch) {
        float[] color=SheepEntity.getRgbColor(dye);
        return MobVariants.permanent(sheep)?new float[]{Math.max(0.35F,color[0]*0.8F),color[1]*0.47F,color[2]*0.3F}:color;
    }
}