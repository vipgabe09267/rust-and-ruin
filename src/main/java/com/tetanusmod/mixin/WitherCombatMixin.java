package com.tetanusmod.mixin;

import com.tetanusmod.entity.*;
import com.tetanusmod.expansion.CombatInfection;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Native successful boss damage exposes the victim once, using ordinary vaccine protection. */
@Mixin(LivingEntity.class)
public abstract class WitherCombatMixin {
    @Inject(method="damage",at=@At("RETURN"))
    private void rustruin$bossInfection(DamageSource source,float amount,CallbackInfoReturnable<Boolean> result){
        var target=(LivingEntity)(Object)this;
        if(target.getWorld().isClient||!result.getReturnValue()||amount<=0||!target.isAlive())return;
        var attacker=source.getAttacker();
        if(attacker instanceof WitherMinionEntity||attacker instanceof WitherEntity wither&&MobVariants.permanent(wither))
            CombatInfection.expose(target,attacker,0,false);
    }
    @Inject(method="dropXp",at=@At("HEAD"),cancellable=true)
    private void rustruin$realBossExperience(CallbackInfo ci){
        var entity=(LivingEntity)(Object)this;
        if(!(entity instanceof WitherEntity)||!MobVariants.permanent(entity))return;
        ci.cancel();
        if(entity.getWorld() instanceof ServerWorld world&&!entity.isExperienceDroppingDisabled()&&world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT))
            ExperienceOrbEntity.spawn(world,entity.getPos(),50);
    }
    @Inject(method="onDeath",at=@At("TAIL"))
    private void rustruin$releaseSummons(DamageSource source,CallbackInfo ci){if((Object)this instanceof WitherEntity boss)WitherBossEncounter.bossDied(boss);}
}
