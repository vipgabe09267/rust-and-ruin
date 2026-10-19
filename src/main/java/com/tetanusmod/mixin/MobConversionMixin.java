package com.tetanusmod.mixin;

import com.tetanusmod.entity.MobVariants;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class MobConversionMixin {
    @Inject(method="convertTo", at=@At("RETURN"))
    private <T extends MobEntity> void rustruin$preserveMutation(EntityType<T> type, boolean keepEquipment, CallbackInfoReturnable<T> result) {
        MobEntity original=(MobEntity)(Object)this;
        T converted=result.getReturnValue();
        if(converted!=null&&!original.getWorld().isClient){
            if(MobVariants.permanent(original))MobVariants.makePermanent(converted);
            else if(com.tetanusmod.expansion.Rabies.rabid(original))com.tetanusmod.expansion.Rabies.makeRabid(converted);
        }
    }
}