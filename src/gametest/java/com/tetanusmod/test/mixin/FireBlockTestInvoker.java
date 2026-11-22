package com.tetanusmod.test.mixin;

import net.minecraft.block.FireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FireBlock.class)
public interface FireBlockTestInvoker {
    @Invoker("trySpreadingFire")
    void rustruin$checkFireSpread(World world, BlockPos pos, int spreadFactor, Random random, int age);
}
