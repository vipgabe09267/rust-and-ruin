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

/** Fitted hand panels and a short stitched cuff; the forearm remains visible. */
public final class GlovesFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity,PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final ModelPart RIGHT=part(false,false),LEFT=part(true,false),SLIM_RIGHT=part(false,true),SLIM_LEFT=part(true,true);
    public GlovesFeatureRenderer(PlayerEntityRenderer renderer){super(renderer);}
    private static ModelPart part(boolean left,boolean slim){
        var data=new ModelData();
        float minX=left?-1:slim?-2:-3,width=slim?3:4;
        var root=data.getRoot();
        // Clear the native 0.25-unit skin sleeve layer; keep the fitted dimensions and arm widths.
        root.addChild("hand",ModelPartBuilder.create().uv(0,0).mirrored(left)
            .cuboid(minX,6,-2,width,4,4,new Dilation(0.275F)),ModelTransform.NONE);
        root.addChild("cuff",ModelPartBuilder.create().uv(0,12).mirrored(left)
            .cuboid(minX,5.5F,-2,width,1,4,new Dilation(0.30F)),ModelTransform.NONE);
        float thumbX=left?-1.45F:minX+width-0.55F;
        root.addChild("thumb",ModelPartBuilder.create().uv(0,20).mirrored(left)
            .cuboid(thumbX,7.4F,-2.15F,1,2,1,new Dilation(0.15F)),ModelTransform.NONE);
        return TexturedModelData.of(data,32,32).createModel();
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
