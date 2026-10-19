package com.tetanusmod.item;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
public final class TetanusEnchantment extends Enchantment {
    public TetanusEnchantment() { super(Rarity.VERY_RARE, EnchantmentTarget.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND}); }
    @Override public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem || stack.getItem() instanceof RebarItem || stack.getItem() instanceof TridentItem || stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
    }
    @Override public int getMaxLevel() { return 2; }
    @Override public net.minecraft.text.Text getName(int level) {
        return net.minecraft.text.Text.translatable(getTranslationKey()).append(" ")
                .append(net.minecraft.text.Text.translatable("enchantment.level."+level)).formatted(net.minecraft.util.Formatting.GRAY);
    }
    @Override public boolean isTreasure() { return true; }
    @Override public boolean isAvailableForRandomSelection() { return false; }
    @Override public boolean isAvailableForEnchantedBookOffer() { return false; }
}
