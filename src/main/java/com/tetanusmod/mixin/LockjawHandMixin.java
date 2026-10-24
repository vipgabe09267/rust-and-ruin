package com.tetanusmod.mixin;

import com.tetanusmod.client.LockjawAttempt;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class LockjawHandMixin {
    @Inject(method="renderFirstPersonItem",at=@At(value="INVOKE",target="Lnet/minecraft/client/util/math/MatrixStack;push()V",ordinal=0,shift=At.Shift.AFTER))
    private void failedBite(AbstractClientPlayerEntity player,float delta,float pitch,Hand hand,float swing,ItemStack stack,float equip,MatrixStack matrices,VertexConsumerProvider vertices,int light,CallbackInfo ci){if(stack.isFood())LockjawAttempt.pose(hand,hand==Hand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite(),delta,matrices);}
}
