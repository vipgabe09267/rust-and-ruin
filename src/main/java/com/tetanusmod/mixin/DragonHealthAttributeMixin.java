package com.tetanusmod.mixin;

import com.tetanusmod.entity.MobVariants;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cache1100HP for our exact dragon mutation without changing any vanilla attribute's global range. */
@Mixin(EntityAttributeInstance.class)
public abstract class DragonHealthAttributeMixin {
    @Inject(method="computeValue",at=@At("RETURN"),cancellable=true)
    private void rustruin$dragonHealthCeiling(CallbackInfoReturnable<Double> result){
        double value=MobVariants.dragonHealthValue((EntityAttributeInstance)(Object)this,result.getReturnValueD());
        if(Double.compare(value,result.getReturnValueD())!=0)result.setReturnValue(value);
    }
}