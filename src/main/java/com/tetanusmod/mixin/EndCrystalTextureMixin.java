package com.tetanusmod.mixin;
import com.tetanusmod.client.TetanizedTextures;
import com.tetanusmod.dragon.TetanizedEndCrystalEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(EndCrystalEntityRenderer.class)
public abstract class EndCrystalTextureMixin {
    @Shadow @Final private static Identifier TEXTURE;
    @Shadow @Final private static RenderLayer END_CRYSTAL;
    @Redirect(method="render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;"))
    private VertexConsumer rustruin$crystalTexture(VertexConsumerProvider provider,RenderLayer layer,EndCrystalEntity crystal,float yaw,float delta,MatrixStack matrices,VertexConsumerProvider unused,int light){return provider.getBuffer(crystal instanceof TetanizedEndCrystalEntity&&layer==END_CRYSTAL?RenderLayer.getEntityCutoutNoCull(TetanizedTextures.get(TEXTURE)):layer);}
}
