package com.tetanusmod.client;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.expansion.GloveInventory;
import net.minecraft.client.model.*;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

public final class GlovesFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity,PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final ModelPart RIGHT=part(false,false),LEFT=part(true,false),SLIM_RIGHT=part(false,true),SLIM_LEFT=part(true,true);
    public GlovesFeatureRenderer(PlayerEntityRenderer renderer){super(renderer);}
    private static ModelPart part(boolean left,boolean slim){
        var data=new ModelData();
        data.getRoot().addChild("glove",ModelPartBuilder.create().uv(0,0).cuboid(left?-1:slim?-2:-3,4,-2,slim?3:4,6,4,new Dilation(0.18F)),ModelTransform.NONE);
        return TexturedModelData.of(data,32,32).createModel().getChild("glove");
    }
    @Override public void render(MatrixStack matrices,VertexConsumerProvider vertices,int light,AbstractClientPlayerEntity player,float limbAngle,float limbDistance,float delta,float progress,float headYaw,float headPitch){
        if(player.isInvisible()||GloveInventory.equipped(player).isEmpty())return;
        renderArm(matrices,vertices,light,player,getContextModel(),Arm.RIGHT);
        renderArm(matrices,vertices,light,player,getContextModel(),Arm.LEFT);
    }
    public static void renderArm(MatrixStack matrices,VertexConsumerProvider vertices,int light,AbstractClientPlayerEntity player,PlayerEntityModel<AbstractClientPlayerEntity> model,Arm arm){
        var gloves=GloveInventory.equipped(player);if(gloves.isEmpty()||player.isInvisible())return;
        boolean left=arm==Arm.LEFT,slim=player.getModel().equals("slim");
        var glove=left?(slim?SLIM_LEFT:LEFT):(slim?SLIM_RIGHT:RIGHT);
        var layer=RenderLayer.getArmorCutoutNoCull(TetanusMod.id("textures/entity/gloves.png"));
        var consumer=ItemRenderer.getArmorGlintConsumer(vertices,layer,false,gloves.hasGlint());
        matrices.push();try{(left?model.leftArm:model.rightArm).rotate(matrices);glove.render(matrices,consumer,light,OverlayTexture.DEFAULT_UV);}finally{matrices.pop();}
    }
}
