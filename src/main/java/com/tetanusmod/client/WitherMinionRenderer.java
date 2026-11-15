package com.tetanusmod.client;

import net.minecraft.client.render.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.AbstractSkeletonEntity;

/** The normal skeleton model and UV map, scaled with its matching small server hitbox. */
public final class WitherMinionRenderer extends WitherSkeletonEntityRenderer {
    public WitherMinionRenderer(EntityRendererFactory.Context context){super(context);shadowRadius=0.25F;}
    @Override protected void scale(AbstractSkeletonEntity entity,MatrixStack matrices,float delta){super.scale(entity,matrices,delta);matrices.scale(0.5F,0.5F,0.5F);}
}
