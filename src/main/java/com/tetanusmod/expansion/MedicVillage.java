package com.tetanusmod.expansion;

import com.google.common.collect.ImmutableSet;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.sound.SoundEvents;
import net.minecraft.village.*;
import net.minecraft.world.poi.PointOfInterestType;

public final class MedicVillage {
    public static final RegistryKey<PointOfInterestType> JOB_KEY=RegistryKey.of(RegistryKeys.POINT_OF_INTEREST_TYPE,TetanusMod.id("medic"));
    public static final RegistryKey<PointOfInterestType> FIELD_JOB_KEY=RegistryKey.of(RegistryKeys.POINT_OF_INTEREST_TYPE,TetanusMod.id("medic_tent"));
    public static final PointOfInterestType JOB=PointOfInterestHelper.register(TetanusMod.id("medic"),1,1,ExpansionBlocks.MEDIC_CABINET.getStateManager().getStates().stream().filter(state->state.get(net.minecraft.block.DoorBlock.HALF)==net.minecraft.block.enums.DoubleBlockHalf.LOWER).toList());
    public static final PointOfInterestType FIELD_JOB=PointOfInterestHelper.register(TetanusMod.id("medic_tent"),1,1,ExpansionBlocks.FIELD_MEDIC_CABINET.getStateManager().getStates().stream().filter(state->state.get(net.minecraft.block.DoorBlock.HALF)==net.minecraft.block.enums.DoubleBlockHalf.LOWER).toList());
    public static final VillagerProfession MEDIC=Registry.register(Registries.VILLAGER_PROFESSION,TetanusMod.id("medic"),new VillagerProfession("medic",entry->entry.matchesKey(JOB_KEY)||entry.matchesKey(FIELD_JOB_KEY),entry->entry.matchesKey(JOB_KEY)||entry.matchesKey(FIELD_JOB_KEY),ImmutableSet.of(Items.HONEY_BOTTLE,Items.PAPER),ImmutableSet.of(),SoundEvents.BLOCK_BREWING_STAND_BREW));
    private static TradeOffers.Factory sell(Item item,int price,int count,int xp){return (entity,random)->new TradeOffer(new ItemStack(Items.EMERALD,price),new ItemStack(item,count),12,xp,0.05F);}
    public static void initialize(){
        MedicTradeOffers.initialize();
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.TOOLSMITH,2,trades->{trades.add(sell(ModItems.WIRE_BRUSH,4,1,5));trades.add(sell(ExpansionItems.TRAP_RESET_TOOL,4,1,5));});
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.WEAPONSMITH,2,trades->{trades.add(sell(ExpansionItems.SPEAR,7,1,5));trades.add(sell(ExpansionItems.RUSTED_SPEAR,9,1,5));trades.add(sell(ModItems.REBAR,3,1,5));trades.add(sell(ModItems.RUSTED_REBAR,4,1,5));});
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.ARMORER,2,trades->{trades.add(sell(ExpansionBlocks.BEAR_TRAP.asItem(),6,1,5));trades.add(sell(ExpansionBlocks.RUSTED_BEAR_TRAP.asItem(),7,1,5));});
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.FISHERMAN,2,trades->trades.add(sell(ModItems.RUSTY_FISHING_HOOK,3,1,5)));
    }
}
