package com.tetanusmod.mixin;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.registry.ModAdvancements;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class SalmonellaAdvancementMixin {
    @Inject(method="onStatusEffectApplied",at=@At("TAIL"))
    private void award(StatusEffectInstance effect,Entity source,CallbackInfo ci) {
        if(effect.getEffectType()==TetanusMod.SALMONELLA && (Object)this instanceof ServerPlayerEntity player)
            ModAdvancements.award(player,"strange_meat","get_salmonella");
    }
}