package com.tetanusmod.registry;

import com.tetanusmod.TetanusMod;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.*;
import net.minecraft.potion.*;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.*;
import net.minecraft.util.ActionResult;
import java.util.WeakHashMap;

public final class ModBrewing {
    public static final Potion MANURE=Registry.register(Registries.POTION,TetanusMod.id("cow_manure"),new Potion());
    public static final Potion TETANUS=Registry.register(Registries.POTION,TetanusMod.id("tetanus"),new Potion(new StatusEffectInstance(TetanusMod.TETANUS,3600)));
    public static final Potion SALMONELLA=Registry.register(Registries.POTION,TetanusMod.id("salmonella"),new Potion(new StatusEffectInstance(TetanusMod.SALMONELLA,1200)));
    private static final WeakHashMap<CowEntity,Long> COLLECTED=new WeakHashMap<>();
    public static void initialize() {
        BrewingRecipeRegistry.registerPotionRecipe(Potions.WATER,ModItems.COW_MANURE,MANURE);
        BrewingRecipeRegistry.registerPotionRecipe(MANURE,ModItems.RUST,TETANUS);
        BrewingRecipeRegistry.registerPotionRecipe(MANURE,ModItems.INFECTED_MEAT,SALMONELLA);
        UseEntityCallback.EVENT.register((player,world,hand,entity,hit)->{
            if(player.isSpectator() || !(entity instanceof CowEntity cow)||!player.getStackInHand(hand).isOf(Items.GLASS_BOTTLE))return ActionResult.PASS;
            if(!world.isClient) {
                long now=world.getTime();
                if(now<COLLECTED.getOrDefault(cow,Long.MIN_VALUE)) { player.sendMessage(net.minecraft.text.Text.translatable("message.tetanusmod.manure_wait"),true);return ActionResult.FAIL; }
                COLLECTED.put(cow,now+1200);
                if(!player.getAbilities().creativeMode)player.getStackInHand(hand).decrement(1);
                var manure=new ItemStack(ModItems.COW_MANURE);
                if(!player.getInventory().insertStack(manure))player.dropItem(manure,false);
            }
            return ActionResult.success(world.isClient);
        });
        net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries->{
            for(var potion:new Potion[]{MANURE,TETANUS,SALMONELLA})entries.add(PotionUtil.setPotion(new ItemStack(Items.POTION),potion));
            for(var potion:new Potion[]{TETANUS,SALMONELLA})entries.add(PotionUtil.setPotion(new ItemStack(Items.SPLASH_POTION),potion));
        });
    }
}