package com.tetanusmod.expansion;

import net.minecraft.enchantment.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;

public final class TetanusProtectionEnchantment extends Enchantment {
    public TetanusProtectionEnchantment(){super(Rarity.RARE,EnchantmentTarget.BREAKABLE,new EquipmentSlot[]{EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND});}
    @Override public boolean isAcceptableItem(ItemStack stack){return stack.getItem() instanceof ShieldItem;}
    @Override public int getMaxLevel(){return 1;}
    @Override public boolean isTreasure(){return true;}
    @Override public boolean isAvailableForRandomSelection(){return false;}
}