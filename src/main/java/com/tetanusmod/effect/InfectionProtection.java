package com.tetanusmod.effect;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.expansion.*;
import com.tetanusmod.registry.*;
import net.minecraft.entity.*;
import net.minecraft.item.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.function.Supplier;

public final class InfectionProtection {
    private static final ThreadLocal<LivingEntity> CHECKED=new ThreadLocal<>();
    public static boolean checked(LivingEntity target){return CHECKED.get()==target;}
    public static boolean checked(LivingEntity target,Supplier<Boolean> action){
        LivingEntity previous=CHECKED.get();CHECKED.set(target);
        try{return action.get();}finally{if(previous!=null)CHECKED.set(previous);else CHECKED.remove();}
    }
    public static boolean allows(LivingEntity target,boolean contact){return allows(target,contact,target.getRandom().nextFloat());}
    public static boolean allows(LivingEntity target,boolean contact,float roll){return allows(target,contact,roll,0);}
    public static boolean protectedShield(ItemStack stack){return stack.getItem() instanceof ShieldItem&&EnchantmentHelper.getLevel(ExpansionItems.TETANUS_PROTECTION,stack)>0;}
    public static boolean allows(LivingEntity target,boolean contact,float roll,int weaponLevel){
        if(target.getWorld().isClient||MobVariants.permanent(target))return true;
        float vaccine=target.hasStatusEffect(TetanusMod.VACCINATED)?TetanusMod.CONFIG.vaccineProtectionChance:0;
        boolean shield=contact&&weaponLevel>0&&(protectedShield(target.getMainHandStack())||protectedShield(target.getOffHandStack()));
        float shieldChance=shield?(weaponLevel>=2?0.40F:0.65F):0;
        var gloves=GloveInventory.equipped(target);
        float gloveChance=contact&&gloves.isOf(ModItems.PROTECTIVE_GLOVES)?TetanusMod.CONFIG.gloveProtectionChance:0;
        if(contact&&gloves.isOf(ModItems.PROTECTIVE_GLOVES))gloves.damage(1,target,e->e.sendEquipmentBreakStatus(EquipmentSlot.OFFHAND));
        float shieldThreshold=(float)(vaccine+(1.0-vaccine)*shieldChance);
        float combined=(float)(shieldThreshold+(1.0-shieldThreshold)*gloveChance);
        if(roll<vaccine)return false;
        if(roll<shieldThreshold){if(target instanceof ServerPlayerEntity player)ModAdvancements.award(player,"shield_protection","block_tetanus");return false;}
        if(roll<combined){if(target instanceof ServerPlayerEntity player)ModAdvancements.award(player,"glove_protection","prevent_tetanus");return false;}
        return true;
    }
}