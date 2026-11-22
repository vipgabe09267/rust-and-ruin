package com.tetanusmod.mixin;

import com.tetanusmod.entity.TetanusTntEntity;
import com.tetanusmod.expansion.TetanusTnt;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.TntEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.TntEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TntEntityRenderer.class)
public abstract class TetanusTntRendererMixin {
    @Redirect(method = "render(Lnet/minecraft/entity/TntEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getDefaultState()Lnet/minecraft/block/BlockState;"))
    private BlockState rustruin$renderTetanusTnt(Block vanillaBlock, TntEntity tnt, float yaw, float tickDelta,
                                                 MatrixStack matrices, VertexConsumerProvider vertices, int light) {
        return (tnt instanceof TetanusTntEntity ? TetanusTnt.BLOCK : vanillaBlock).getDefaultState();
    }
}
