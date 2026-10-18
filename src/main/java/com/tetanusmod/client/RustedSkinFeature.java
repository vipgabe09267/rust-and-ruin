package com.tetanusmod.client;

import com.tetanusmod.entity.MobVariants;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

public final class RustedSkinFeature<T extends LivingEntity,M extends EntityModel<T>> extends FeatureRenderer<T,M> {
    private final FeatureRendererContext<T,M> context;
    public RustedSkinFeature(FeatureRendererContext<T,M> context) { super(context);this.context=context; }
    @Override public void render(MatrixStack matrices,VertexConsumerProvider vertices,int light,T entity,
            float limbAngle,float limbDistance,float tickDelta,float animationProgress,float headYaw,float headPitch) {
        if(!MobVariants.permanent(entity)||entity.isInvisible()||!(context instanceof LivingEntityRenderer<?,?>))return;
        @SuppressWarnings("unchecked") var renderer=(LivingEntityRenderer<T,M>)context;
        var original=renderer.getTexture(entity);
        var texture=TetanizedTextures.get(original);
        boolean fallback=texture.equals(original);
        var layer=RenderLayer.getEntityCutoutNoCull(texture);
        matrices.push();matrices.scale(1.002F,1.002F,1.002F);
        getContextModel().render(matrices,vertices.getBuffer(layer),light,OverlayTexture.DEFAULT_UV,
                fallback?0.8F:1,fallback?0.65F:1,fallback?0.45F:1,1);
        matrices.pop();
    }
}