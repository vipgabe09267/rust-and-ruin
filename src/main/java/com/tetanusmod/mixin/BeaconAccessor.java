package com.tetanusmod.mixin;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.screen.PropertyDelegate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(BeaconBlockEntity.class)
public interface BeaconAccessor {
    @Accessor("propertyDelegate") PropertyDelegate rustruin$properties();
}