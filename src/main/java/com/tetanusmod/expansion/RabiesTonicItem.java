package com.tetanusmod.expansion;

import com.tetanusmod.item.DescribedItem;
import com.tetanusmod.registry.ModAdvancements;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.*;
import net.minecraft.util.*;
import net.minecraft.world.World;

public final class RabiesTonicItem extends DescribedItem {
    private final boolean vaccine;
    public RabiesTonicItem(boolean vaccine,Settings settings){super(settings,vaccine?"rabies_vaccine":"rabies_cure");this.vaccine=vaccine;}
    @Override public int getMaxUseTime(ItemStack stack){return 32;}
    @Override public UseAction getUseAction(ItemStack stack){return UseAction.DRINK;}
    @Override public SoundEvent getDrinkSound(){return SoundEvents.ENTITY_GENERIC_DRINK;}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand){return user.isSpectator()?TypedActionResult.pass(user.getStackInHand(hand)):ItemUsage.consumeHeldItem(world,user,hand);}
    @Override public ItemStack finishUsing(ItemStack stack,World world,LivingEntity user){
        if(world.isClient||stack.isEmpty()||!stack.isOf(this)||!user.isAlive()||user.isSpectator())return stack;
        com.tetanusmod.item.MedicineConsumption.record(user,stack);
        if(vaccine)user.addStatusEffect(new StatusEffectInstance(ExpansionEffects.RABIES_RESISTANCE,24000));
        else {
            boolean infected=user.hasStatusEffect(ExpansionEffects.RABIES);RabiesEffect.cure(user);
            if(infected&&user instanceof ServerPlayerEntity player)ModAdvancements.award(player,"rabies_cured","cure_rabies");
        }
        if(user instanceof PlayerEntity player&&player.getAbilities().creativeMode)return stack;
        stack.decrement(1);var bottle=new ItemStack(Items.GLASS_BOTTLE);if(stack.isEmpty())return bottle;
        if(user instanceof PlayerEntity player){if(!player.getInventory().insertStack(bottle))player.dropItem(bottle,false);}else user.dropStack(bottle);
        return stack;
    }
}