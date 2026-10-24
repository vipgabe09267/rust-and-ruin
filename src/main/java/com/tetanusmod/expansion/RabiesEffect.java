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
        // Reconcile changes to other health bonuses without dirtying an unchanged attribute.
        restorePenalty(entity);
        var state=(ExpansionState)entity;
        int ticks=state.rustruin$getRabiesTicks()+1;
        state.rustruin$setRabiesTicks(ticks%1200);
        if(ticks<1200)return;
        state.rustruin$setLostHealth(state.rustruin$getLostHealth()+2);
        restorePenalty(entity);
        if(entity.getMaxHealth()<=4.01F||entity.getHealth()<=4)Rabies.queueNausea(entity);
    }
    public static void restorePenalty(LivingEntity entity) {
        var attribute=entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if(attribute==null)return;
        // Match vanilla's operation order, excluding this effect (including older ADDITION saves).
        // A flat subtraction is multiplied by mutation bonuses and can breach the two-heart floor.
        double base=attribute.getBaseValue();
        for(var modifier:attribute.getModifiers(EntityAttributeModifier.Operation.ADDITION))
            if(!modifier.getId().equals(HEALTH_PENALTY))base+=modifier.getValue();
        double raw=base;
        for(var modifier:attribute.getModifiers(EntityAttributeModifier.Operation.MULTIPLY_BASE))
            if(!modifier.getId().equals(HEALTH_PENALTY))raw+=base*modifier.getValue();
        for(var modifier:attribute.getModifiers(EntityAttributeModifier.Operation.MULTIPLY_TOTAL))
            if(!modifier.getId().equals(HEALTH_PENALTY))raw*=1+modifier.getValue();
        double healthy=attribute.getAttribute().clamp(raw);
        var state=(ExpansionState)entity;
        int capped=Math.max(0,Math.min(state.rustruin$getLostHealth(),(int)Math.floor(healthy-4)));
        state.rustruin$setLostHealth(capped);
        double amount=capped>0&&raw>0?(healthy-capped)/raw-1:0;
        var existing=attribute.getModifier(HEALTH_PENALTY);
        if(capped==0){if(existing!=null)attribute.removeModifier(HEALTH_PENALTY);}
        else if(existing==null||existing.getOperation()!=EntityAttributeModifier.Operation.MULTIPLY_TOTAL
                ||Math.abs(existing.getValue()-amount)>1.0E-12){
            if(existing!=null)attribute.removeModifier(HEALTH_PENALTY);
            attribute.addTemporaryModifier(new EntityAttributeModifier(HEALTH_PENALTY,"Rabies heart loss",amount,EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if(entity.getHealth()>entity.getMaxHealth())entity.setHealth(entity.getMaxHealth());
    }
    public static void cure(LivingEntity entity) {
        entity.removeStatusEffect(ExpansionEffects.RABIES);
        var attribute=entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if(attribute!=null)attribute.removeModifier(HEALTH_PENALTY);
        var state=(ExpansionState)entity;state.rustruin$setRabiesTicks(0);state.rustruin$setLostHealth(0);
    }
}
