package com.tetanusmod.mixin;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.world.MedicStructurePiece;
import net.minecraft.block.Block;
import net.minecraft.structure.SimpleStructurePiece;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Preserve authored cave fixtures until their native light and chunk data settle. */
@Mixin(SimpleStructurePiece.class)
public abstract class MedicCampKnownShapePlacementMixin {
    @Shadow protected abstract Identifier getId();

    @ModifyArg(method = "generate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/structure/StructureTemplate;place(Lnet/minecraft/world/ServerWorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/structure/StructurePlacementData;Lnet/minecraft/util/math/random/Random;I)Z"), index = 5)
    private int rustruin$preserveCampFixtures(int flags) {
        // The camp already authors fence connections, stair shapes and block halves.
        // Its placement data skips late shape processing; FORCE_STATE also skips
        // synchronous updates while neighboring template air replaces cave water.
        // Otherwise wheat can fail its light predicate before the light engine runs.
        return (Object) this instanceof MedicStructurePiece
                && getId().equals(TetanusMod.id("medic_tent"))
                ? flags | Block.FORCE_STATE : flags;
    }
}
