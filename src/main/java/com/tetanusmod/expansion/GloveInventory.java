package com.tetanusmod.expansion;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;

public interface GloveInventory {
    SimpleInventory rustruin$gloves();
    ItemStack rustruin$visualGloves();
    static ItemStack equipped(LivingEntity entity){
        if(!(entity instanceof PlayerEntity))return entity.getOffHandStack();
        var inventory=(GloveInventory)entity;
        var visual=inventory.rustruin$visualGloves();
        return entity.getWorld().isClient?visual:inventory.rustruin$gloves().getStack(0);
    }
}