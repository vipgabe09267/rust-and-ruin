package com.tetanusmod.client;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.entity.MobVariants;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.feature.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.Identifier;
public final class RustedSkinFeature<T extends LivingEntity,M extends EntityModel<T>> extends FeatureRenderer<T,M> {
    private static final Identifier SKIN = TetanusMod.id("textures/entity/tetanized_skin.png");
    private static final Identifier CREEPER = TetanusMod.id("textures/entity/tetanized_creeper.png");
    public RustedSkinFeature(FeatureRendererContext<T,M> context) { super(context); }
    @Override public void render(MatrixStack matrices,VertexConsumerProvider vertices,int light,T entity,
            float limbAngle,float limbDistance,float tickDelta,float animationProgress,float headYaw,float headPitch) {
        if (!MobVariants.permanent(entity) || entity.isInvisible()) return;
        var layer = RenderLayer.getEntityCutoutNoCull(entity instanceof CreeperEntity ? CREEPER : SKIN);
        matrices.push();
        matrices.scale(1.002F,1.002F,1.002F);
        getContextModel().render(matrices,vertices.getBuffer(layer),light,OverlayTexture.DEFAULT_UV,1,1,1,1);
        matrices.pop();
    }
}
