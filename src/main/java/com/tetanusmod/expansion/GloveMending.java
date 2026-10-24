package com.tetanusmod.expansion;

import net.minecraft.enchantment.*;
import net.minecraft.entity.*;
import net.minecraft.item.ItemStack;
import java.util.*;
import java.util.function.Predicate;

public final class GloveMending {
    public static Map.Entry<EquipmentSlot,ItemStack> choose(Enchantment enchantment,LivingEntity entity,Predicate<ItemStack> filter){
        var vanilla=EnchantmentHelper.chooseEquipmentWith(enchantment,entity,filter);
        var gloves=GloveInventory.equipped(entity);
        if(gloves.isEmpty()||EnchantmentHelper.getLevel(enchantment,gloves)==0||!filter.test(gloves))return vanilla;
        int count=0;
        for(EquipmentSlot slot:EquipmentSlot.values()){
            var stack=entity.getEquippedStack(slot);
            if(!stack.isEmpty()&&EnchantmentHelper.getLevel(enchantment,stack)>0&&filter.test(stack))count++;
        }
        return entity.getRandom().nextInt(count+1)==0?new AbstractMap.SimpleImmutableEntry<>(EquipmentSlot.OFFHAND,gloves):vanilla;
    }
}
