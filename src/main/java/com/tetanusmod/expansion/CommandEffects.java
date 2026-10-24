package com.tetanusmod.expansion;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;

/** Only the native permission-checked /effect path bypasses random vaccine prevention. */
public final class CommandEffects {
    private static final ThreadLocal<Boolean> FORCED=ThreadLocal.withInitial(()->false);
    public static boolean forced(){return FORCED.get();}
    public static boolean apply(LivingEntity target,StatusEffectInstance effect,Entity source){
        boolean previous=FORCED.get();FORCED.set(true);
        try{return target.addStatusEffect(effect,source);}
        finally{if(previous)FORCED.set(true);else FORCED.remove();}
    }
}
