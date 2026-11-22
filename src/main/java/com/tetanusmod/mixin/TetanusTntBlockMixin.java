package com.tetanusmod.mixin;

import com.tetanusmod.expansion.TetanusTnt;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntBlock.class)
public abstract class TetanusTntBlockMixin {
    @Inject(method = "primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)V",
            at = @At("HEAD"), cancellable = true)
    private static void rustruin$primeTetanusTnt(World world, BlockPos pos, @Nullable LivingEntity igniter, CallbackInfo ci) {
        if (world instanceof ServerWorld server && world.getBlockState(pos).isOf(TetanusTnt.BLOCK)) {
            TetanusTnt.prime(server, pos, igniter, false);
            ci.cancel();
        }
    }

    @Inject(method = "onDestroyedByExplosion", at = @At("HEAD"), cancellable = true)
    private void rustruin$chainTetanusTnt(World world, BlockPos pos, Explosion explosion, CallbackInfo ci) {
        if ((Object) this == TetanusTnt.BLOCK && world instanceof ServerWorld server) {
            TetanusTnt.prime(server, pos, explosion.getCausingEntity(), true);
            ci.cancel();
        }
    }
}
