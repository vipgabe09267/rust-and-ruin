package com.tetanusmod.mixin;

import com.tetanusmod.expansion.MedicineCabinetBlock;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cabinet doors open visually, but their shelves remain a solid obstacle. */
@Mixin(LandPathNodeMaker.class)
public abstract class CabinetPathfindingMixin {
    @Inject(method="getCommonNodeType",at=@At("HEAD"),cancellable=true)
    private static void cabinet(BlockView world,BlockPos pos,CallbackInfoReturnable<PathNodeType> result){
        if(world.getBlockState(pos).getBlock() instanceof MedicineCabinetBlock)result.setReturnValue(PathNodeType.BLOCKED);
    }
}