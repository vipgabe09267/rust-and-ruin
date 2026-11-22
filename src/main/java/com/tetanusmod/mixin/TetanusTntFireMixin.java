package com.tetanusmod.mixin;

import com.tetanusmod.expansion.TetanusTnt;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FireBlock.class)
public abstract class TetanusTntFireMixin {
    @Inject(method = "trySpreadingFire", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/TntBlock;primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"),
            cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private void rustruin$primeBurnedTetanusTnt(World world, BlockPos pos, int spreadFactor, Random random, int age,
                                               CallbackInfo ci, int spreadChance, BlockState burnedState, Block burnedBlock) {
        // Fire has already replaced the original block by the time vanilla primes it.
        if (burnedBlock == TetanusTnt.BLOCK && world instanceof ServerWorld server) {
            TetanusTnt.prime(server, pos, null, false);
            ci.cancel();
        }
    }
}
