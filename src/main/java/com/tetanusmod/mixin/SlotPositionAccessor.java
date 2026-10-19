package com.tetanusmod.mixin;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Slot.class)
public interface SlotPositionAccessor {
    @Mutable @Accessor("x") void rustruin$x(int x);
    @Mutable @Accessor("y") void rustruin$y(int y);
}