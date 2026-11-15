package com.tetanusmod.mixin;
import com.tetanusmod.dragon.DragonEncounter;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.effect.Infection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class DragonInfectionMixin {
    @Inject(method="damage",at=@At("HEAD"),cancellable=true)
    private void rustruin$noSelfBite(DamageSource source,float amount,CallbackInfoReturnable<Boolean> result){if(DragonEncounter.isCarriedBy((LivingEntity)(Object)this,source.getAttacker()))result.setReturnValue(false);}
    @Inject(method="damage",at=@At("RETURN"))
    private void rustruin$dragonExposure(DamageSource source,float amount,CallbackInfoReturnable<Boolean> result){var target=(LivingEntity)(Object)this;if(result.getReturnValueZ()&&target.isAlive()&&!target.getWorld().isClient&&source.getAttacker() instanceof EnderDragonEntity dragon&&MobVariants.permanent(dragon))Infection.apply(target,dragon);
        // Only victims of actual, accepted crystal explosion damage are exposed;
        // cover, distance, Creative immunity and native explosion falloff still apply.
        if(result.getReturnValueZ()&&target.isAlive()&&!target.getWorld().isClient
                &&source.getSource() instanceof com.tetanusmod.dragon.TetanizedEndCrystalEntity crystal
                &&source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_EXPLOSION))Infection.apply(target,crystal);
    }
}
