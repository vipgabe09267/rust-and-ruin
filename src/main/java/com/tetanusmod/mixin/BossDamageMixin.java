package com.tetanusmod.mixin;

import com.tetanusmod.entity.MobVariants;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class BossDamageMixin {
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float rustruin$scaleBossDamage(float amount, DamageSource source) {
        var target = (LivingEntity) (Object) this;
        var attacker = source.getAttacker();
        if (target.getWorld().isClient || attacker == target || !Float.isFinite(amount) || amount <= 0) return amount;
        if ((attacker instanceof WitherEntity || attacker instanceof EnderDragonEntity)
                && MobVariants.permanent((LivingEntity) attacker)) {
            return (float) Math.min(Float.MAX_VALUE, (double) amount * 2);
        }
        return amount;
    }
}
