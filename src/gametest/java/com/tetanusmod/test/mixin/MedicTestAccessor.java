package com.tetanusmod.test.mixin;

import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VillagerEntity.class)
public interface MedicTestAccessor {
    @Invoker("clearDailyRestockCount") void rustruin$dailyRestock();
    @Invoker("canLevelUp") boolean rustruin$canLevelUp();
    @Invoker("levelUp") void rustruin$levelUp();
}
