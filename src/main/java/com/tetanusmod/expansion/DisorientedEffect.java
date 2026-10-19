package com.tetanusmod.expansion;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.*;

public final class DisorientedEffect extends StatusEffect {
    public DisorientedEffect(){super(StatusEffectCategory.HARMFUL,0xAF7746);}
    @Override public boolean canApplyUpdateEffect(int duration,int amplifier){return true;}
    @Override public void applyUpdateEffect(LivingEntity entity,int amplifier) {
        if(entity.getWorld().isClient)return;
        var state=(ExpansionState)entity;int tick=state.rustruin$getDisorientedTicks()+1;state.rustruin$setDisorientedTicks(tick%200);
        if(tick%100==0)Rabies.queueNausea(entity);
        if(tick%40==0)com.tetanusmod.effect.Infection.twitch(entity,2);
        if(entity instanceof net.minecraft.entity.mob.MobEntity mob && tick%40==0) {
            mob.setTarget(null);mob.getNavigation().stop();
            if(mob.getRandom().nextBoolean())mob.getNavigation().startMovingTo(mob.getX()+mob.getRandom().nextBetween(-4,4),mob.getY(),mob.getZ()+mob.getRandom().nextBetween(-4,4),0.6);
            mob.setYaw(mob.getYaw()+mob.getRandom().nextBetween(-100,100));
        }
        if(tick>=200) {
            float damage=Math.min(2,Math.max(0,entity.getHealth()-1));
            if(damage>0)entity.damage(ModDamage.source(entity,ModDamage.DISORIENTATION),damage);
        }
    }
}