package com.tetanusmod.expansion;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.effect.*;
import java.util.UUID;

public final class RabiesEffect extends StatusEffect {
    public static final UUID HEALTH_PENALTY=UUID.fromString("e1a61f7b-5baf-47c1-b4fd-90b5dfc446ed");
    public RabiesEffect(){super(StatusEffectCategory.HARMFUL,0x9B3F79);}
    @Override public boolean canApplyUpdateEffect(int duration,int amplifier){return true;}
    @Override public void applyUpdateEffect(LivingEntity entity,int amplifier) {
        if(entity.getWorld().isClient)return;
        var state=(ExpansionState)entity;
        int ticks=state.rustruin$getRabiesTicks()+1;
        state.rustruin$setRabiesTicks(ticks%1200);
        if(ticks<1200)return;
        var health=entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if(health==null)return;
        health.removeModifier(HEALTH_PENALTY);
        float healthyMax=(float)health.getValue();
        int lost=Math.min(state.rustruin$getLostHealth()+2,Math.max(0,(int)Math.floor(healthyMax-4)));
        state.rustruin$setLostHealth(lost);
        if(lost>0)health.addTemporaryModifier(new EntityAttributeModifier(HEALTH_PENALTY,"Rabies heart loss",-lost,EntityAttributeModifier.Operation.ADDITION));
        entity.setHealth(Math.min(entity.getHealth(),entity.getMaxHealth()));
        if(entity.getMaxHealth()<=4.01F||entity.getHealth()<=4)Rabies.queueNausea(entity);
    }
    public static void restorePenalty(LivingEntity entity) {
        var attribute=entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if(attribute==null)return;
        attribute.removeModifier(HEALTH_PENALTY);
        int lost=((ExpansionState)entity).rustruin$getLostHealth();
        int capped=Math.min(lost,Math.max(0,(int)Math.floor(attribute.getValue()-4)));
        if(capped>0)attribute.addTemporaryModifier(new EntityAttributeModifier(HEALTH_PENALTY,"Rabies heart loss",-capped,EntityAttributeModifier.Operation.ADDITION));
        entity.setHealth(Math.min(entity.getHealth(),entity.getMaxHealth()));
    }
    public static void cure(LivingEntity entity) {
        entity.removeStatusEffect(ExpansionEffects.RABIES);
        var attribute=entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if(attribute!=null)attribute.removeModifier(HEALTH_PENALTY);
        var state=(ExpansionState)entity;state.rustruin$setRabiesTicks(0);state.rustruin$setLostHealth(0);
    }
}