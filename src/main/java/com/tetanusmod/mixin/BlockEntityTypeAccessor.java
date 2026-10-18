package com.tetanusmod.mixin;
import net.minecraft.block.entity.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(BlockEntity.class)
public interface BlockEntityTypeAccessor {
    @Mutable @Accessor("type") void rustruin$setType(BlockEntityType<?> type);
}