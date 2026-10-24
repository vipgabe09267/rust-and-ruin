package com.tetanusmod.item;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;

/** Match native drink items' statistics and consume_item advancement trigger. */
public final class MedicineConsumption {
    private MedicineConsumption(){}
    public static void record(LivingEntity user,ItemStack stack){
        if(user instanceof ServerPlayerEntity player){
            Criteria.CONSUME_ITEM.trigger(player,stack);
            player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
        }
    }
}