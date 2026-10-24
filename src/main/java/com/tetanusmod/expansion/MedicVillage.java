package com.tetanusmod.expansion;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.registry.ModItems;
import com.tetanusmod.mixin.StructurePoolAccessor;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.pool.*;
import net.minecraft.village.*;
import net.minecraft.world.poi.PointOfInterestType;

public final class MedicVillage {
    public static final RegistryKey<PointOfInterestType> JOB_KEY=RegistryKey.of(RegistryKeys.POINT_OF_INTEREST_TYPE,TetanusMod.id("medic"));
    public static final PointOfInterestType JOB=PointOfInterestHelper.register(TetanusMod.id("medic"),1,1,ExpansionBlocks.MEDIC_CABINET.getStateManager().getStates().stream().filter(state->state.get(net.minecraft.block.DoorBlock.HALF)==net.minecraft.block.enums.DoubleBlockHalf.LOWER).toList());
    public static final VillagerProfession MEDIC=Registry.register(Registries.VILLAGER_PROFESSION,TetanusMod.id("medic"),new VillagerProfession("medic",entry->entry.matchesKey(JOB_KEY),entry->entry.matchesKey(JOB_KEY),ImmutableSet.of(Items.HONEY_BOTTLE,Items.PAPER),ImmutableSet.of(),SoundEvents.BLOCK_BREWING_STAND_BREW));
    private static TradeOffers.Factory sell(Item item,int price,int count,int xp){return (entity,random)->new TradeOffer(new ItemStack(Items.EMERALD,price),new ItemStack(item,count),12,xp,0.05F);}
    public static void initialize(){
        TradeOfferHelper.registerVillagerOffers(MEDIC,1,trades->{
            trades.add(sell(ModItems.ANTITOXIN,6,1,2));trades.add(sell(ModItems.VACCINE,8,1,2));trades.add(sell(ModItems.PROTECTIVE_GLOVES,6,1,2));
            trades.add(sell(ExpansionItems.RABIES_CURE,10,1,2));trades.add(sell(ExpansionItems.RABIES_VACCINE,12,1,2));
        });
        TradeOfferHelper.registerVillagerOffers(MEDIC,2,trades->{trades.add(sell(ExpansionBlocks.MEDIC_CABINET.asItem(),5,1,10));trades.add((entity,random)->new TradeOffer(new ItemStack(Items.HONEY_BOTTLE,3),new ItemStack(Items.EMERALD),12,5,0.05F));});
        TradeOfferHelper.registerVillagerOffers(MEDIC,3,trades->trades.add((entity,random)->new TradeOffer(new ItemStack(Items.EMERALD,16),EnchantedBookItem.forEnchantment(new net.minecraft.enchantment.EnchantmentLevelEntry(ExpansionItems.TETANUS_PROTECTION,1)),5,15,0.2F)));
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.TOOLSMITH,2,trades->{trades.add(sell(ModItems.WIRE_BRUSH,4,1,5));trades.add(sell(ExpansionItems.TRAP_RESET_TOOL,4,1,5));});
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.WEAPONSMITH,2,trades->{trades.add(sell(ExpansionItems.SPEAR,7,1,5));trades.add(sell(ExpansionItems.RUSTED_SPEAR,9,1,5));trades.add(sell(ModItems.REBAR,3,1,5));trades.add(sell(ModItems.RUSTED_REBAR,4,1,5));});
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.ARMORER,2,trades->{trades.add(sell(ExpansionBlocks.BEAR_TRAP.asItem(),6,1,5));trades.add(sell(ExpansionBlocks.RUSTED_BEAR_TRAP.asItem(),7,1,5));});
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.FISHERMAN,2,trades->trades.add(sell(ModItems.RUSTY_FISHING_HOOK,3,1,5)));
        // Clinics now use a separately locatable structure with a 55% village-grid roll.
    }
    public static void ensureStarterTrades(net.minecraft.entity.passive.VillagerEntity medic){
        if(medic.getVillagerData().getProfession()!=MEDIC||medic.getVillagerData().getLevel()!=1)return;
        var offers=medic.getOffers();Item[] items={ModItems.ANTITOXIN,ModItems.VACCINE,ModItems.PROTECTIVE_GLOVES,ExpansionItems.RABIES_CURE,ExpansionItems.RABIES_VACCINE};int[] prices={6,8,6,10,12};
        for(int i=0;i<items.length;i++){Item item=items[i];if(offers.stream().noneMatch(offer->offer.getSellItem().isOf(item)))offers.add(new TradeOffer(new ItemStack(Items.EMERALD,prices[i]),new ItemStack(item),12,2,0.05F));}
    }}
