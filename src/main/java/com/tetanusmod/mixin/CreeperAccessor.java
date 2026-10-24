package com.tetanusmod.mixin;
import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(CreeperEntity.class)
public interface CreeperAccessor {
    @Accessor("explosionRadius") int rustruin$getExplosionRadius();
    @Accessor("explosionRadius") void rustruin$setExplosionRadius(int radius);
}
