package com.tetanusmod.item;

import com.tetanusmod.TetanusMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.sound.*;
import net.minecraft.util.*;
import net.minecraft.world.World;

public final class VaccineItem extends DescribedItem {
    public VaccineItem(Settings settings){super(settings,"vaccine");}
    @Override public int getMaxUseTime(ItemStack stack){return 32;}
    @Override public UseAction getUseAction(ItemStack stack){return UseAction.DRINK;}
    @Override public SoundEvent getDrinkSound(){return SoundEvents.ENTITY_GENERIC_DRINK;}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand) {
        if(user.isSpectator())return TypedActionResult.pass(user.getStackInHand(hand));
        return ItemUsage.consumeHeldItem(world,user,hand);
    }
    @Override public ItemStack finishUsing(ItemStack stack,World world,LivingEntity user) {
        if(world.isClient)return stack;
        if(stack.isEmpty()||!stack.isOf(this)||!user.isAlive()||user.isSpectator())return stack;
        MedicineConsumption.record(user,stack);
        user.addStatusEffect(new StatusEffectInstance(TetanusMod.VACCINATED,TetanusMod.CONFIG.vaccineDurationSeconds*20));
        if(user instanceof PlayerEntity player&&player.getAbilities().creativeMode)return stack;
        stack.decrement(1);var bottle=new ItemStack(Items.GLASS_BOTTLE);
        if(stack.isEmpty())return bottle;
        if(user instanceof PlayerEntity player){if(!player.getInventory().insertStack(bottle))player.dropItem(bottle,false);}
        else user.dropStack(bottle);
        return stack;
    }
}