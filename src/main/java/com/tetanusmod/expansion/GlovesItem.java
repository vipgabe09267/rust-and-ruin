package com.tetanusmod.expansion;

import com.tetanusmod.item.DescribedItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;

public final class GlovesItem extends DescribedItem {
    public GlovesItem(Settings settings){super(settings,"gloves");}
    @Override public int getEnchantability(){return 12;}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand) {
        var held=user.getStackInHand(hand);var inventory=((GloveInventory)user).rustruin$gloves();
        if(user.isSpectator()||!inventory.getStack(0).isEmpty())return TypedActionResult.pass(held);
        if(!world.isClient){inventory.setStack(0,held.copyWithCount(1));held.decrement(1);user.playerScreenHandler.sendContentUpdates();}
        return TypedActionResult.success(held,world.isClient);
    }
}