package com.tetanusmod.effect;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.registry.ModItems;
import net.minecraft.entity.*;
import java.util.function.Supplier;

/** Server-side prevention. Checked scopes avoid rolling twice for a single infection. */
public final class InfectionProtection {
    private static final ThreadLocal<LivingEntity> CHECKED=new ThreadLocal<>();
    public static boolean checked(LivingEntity target){return CHECKED.get()==target;}
    public static boolean checked(LivingEntity target,Supplier<Boolean> action) {
        LivingEntity previous=CHECKED.get();CHECKED.set(target);
        try{return action.get();}finally{if(previous!=null)CHECKED.set(previous);else CHECKED.remove();}
    }
    public static boolean allows(LivingEntity target,boolean contact) {
        return allows(target,contact,target.getRandom().nextFloat());
    }
    public static boolean allows(LivingEntity target,boolean contact,float roll) {
        if(target.getWorld().isClient)return true;
        if(MobVariants.permanent(target))return true;
        double vaccine=target.hasStatusEffect(TetanusMod.VACCINATED)?TetanusMod.CONFIG.vaccineProtectionChance:0;
        var gloves=target.getOffHandStack();
        double glove=contact&&gloves.isOf(ModItems.PROTECTIVE_GLOVES)?TetanusMod.CONFIG.gloveProtectionChance:0;
        if(contact&&gloves.isOf(ModItems.PROTECTIVE_GLOVES))gloves.damage(1,target,e->e.sendEquipmentBreakStatus(EquipmentSlot.OFFHAND));
        float chance=(float)(1-(1-vaccine)*(1-glove));
        return roll>=chance;
    }
}