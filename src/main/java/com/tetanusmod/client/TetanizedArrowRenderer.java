package com.tetanusmod.client;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.entity.TetanizedArrowEntity;
import net.minecraft.client.render.entity.*;
import net.minecraft.util.Identifier;
public final class TetanizedArrowRenderer extends ProjectileEntityRenderer<TetanizedArrowEntity> {
    public TetanizedArrowRenderer(EntityRendererFactory.Context context) { super(context); }
    @Override public Identifier getTexture(TetanizedArrowEntity arrow) { return TetanusMod.id("textures/entity/tetanized_arrow.png"); }
}
