package com.tetanusmod.expansion;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;

public interface GloveInventory {
    SimpleInventory rustruin$gloves();
    static ItemStack equipped(LivingEntity entity){return entity instanceof PlayerEntity?((GloveInventory)entity).rustruin$gloves().getStack(0):entity.getOffHandStack();}
}