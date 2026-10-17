package com.tetanusmod.mixin;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {
    @Accessor("thrower") java.util.UUID rustruin$getThrower();
}
