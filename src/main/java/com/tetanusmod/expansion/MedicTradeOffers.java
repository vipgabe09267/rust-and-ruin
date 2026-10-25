package com.tetanusmod.expansion;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.*;
import java.util.List;

/** Two persistent, randomly selected offers per rank. Native gossip, demand and Hero prices apply. */
public final class MedicTradeOffers {
    public static final Identifier SCREEN_PACKET=TetanusMod.id("medic_trade_screen");
    public static final int LAST_RANK=4;
    public static final int SCHEMA=2;
    public static final long VILLAGE_RESTOCK_TICKS=24000L, FIELD_RESTOCK_TICKS=72000L;
    public interface State {
        boolean rustruin$isFieldMedic();
        void rustruin$isFieldMedic(boolean value);
        long rustruin$lastRestock();
        void rustruin$lastRestock(long tick);
    }
    private record Option(ItemStack first,ItemStack second,ItemStack result,int stock,int xp) {
        TradeOffer create(VillagerEntity medic,Random random) {
            boolean medicine=isMedicine(result.getItem());
            int uses=medicine?(((State)medic).rustruin$isFieldMedic()?2:2+random.nextInt(3)):stock;
            if(((State)medic).rustruin$isFieldMedic())uses=Math.min(uses,2);
            return new TradeOffer(first.copy(),second.copy(),result.copy(),uses,xp,0.05F);
        }
    }
    private static Option sale(Item item,int emeralds,int count,int stock,int xp){return barter(Items.EMERALD,emeralds,null,0,new ItemStack(item,count),stock,xp);}
    private static Option buy(Item item,int count,int emeralds,int stock,int xp){return barter(item,count,null,0,new ItemStack(Items.EMERALD,emeralds),stock,xp);}
    private static Option barter(Item first,int count,Item second,int secondCount,ItemStack result,int stock,int xp){return new Option(new ItemStack(first,count),second==null?ItemStack.EMPTY:new ItemStack(second,secondCount),result,stock,xp);}
    private static List<Option> medicine(int rank){return switch(rank){
        case 1->List.of(sale(ModItems.ANTITOXIN,8,1,4,5),barter(Items.IRON_INGOT,8,Items.FLINT,3,new ItemStack(ModItems.ANTITOXIN),4,5),barter(Items.EMERALD,10,Items.PAPER,4,new ItemStack(ModItems.VACCINE),4,5));
        case 2->List.of(barter(Items.EMERALD,7,Items.REDSTONE,2,new ItemStack(ModItems.ANTITOXIN),4,10),barter(Items.EMERALD,12,Items.HONEY_BOTTLE,1,new ItemStack(ExpansionItems.RABIES_CURE),4,10),barter(Items.EMERALD,14,Items.FERMENTED_SPIDER_EYE,1,new ItemStack(ExpansionItems.RABIES_VACCINE),4,10),barter(Items.EMERALD,10,Items.IRON_INGOT,3,new ItemStack(ModItems.VACCINE),4,10));
        case 3->List.of(barter(Items.EMERALD,10,Items.GLISTERING_MELON_SLICE,1,new ItemStack(ModItems.ANTITOXIN),4,20),barter(Items.EMERALD,12,Items.GLOWSTONE_DUST,2,new ItemStack(ModItems.VACCINE),4,20),barter(Items.EMERALD,12,Items.LEATHER,3,new ItemStack(ExpansionItems.VETERINARY_KIT),4,20),barter(Items.DIAMOND,1,Items.HONEY_BOTTLE,2,new ItemStack(ExpansionItems.RABIES_CURE),4,20));
        default->List.of(barter(Items.EMERALD,9,Items.GHAST_TEAR,1,new ItemStack(ModItems.VACCINE),4,30),barter(Items.EMERALD,12,Items.GOLDEN_CARROT,2,new ItemStack(ExpansionItems.RABIES_VACCINE),4,30),sale(ModItems.ANTITOXIN,7,1,4,30),sale(ExpansionItems.RABIES_CURE,11,1,4,30));
    };}
    private static List<Option> supplies(int rank){return switch(rank){
        case 1->List.of(buy(Items.PAPER,16,1,8,3),buy(Items.IRON_INGOT,5,1,6,3),buy(Items.HONEY_BOTTLE,3,2,6,3),sale(Items.BREAD,2,4,6,3),barter(Items.EMERALD,7,Items.LEATHER,2,new ItemStack(ModItems.PROTECTIVE_GLOVES),4,3),sale(Items.APPLE,2,4,6,3));
        case 2->List.of(sale(Items.REDSTONE,2,4,6,10),sale(Items.BLAZE_POWDER,4,2,4,10),sale(Items.BREWING_STAND,5,1,4,10),sale(ExpansionBlocks.MEDIC_CABINET.asItem(),8,1,4,10),barter(Items.HONEY_BOTTLE,2,Items.PAPER,8,new ItemStack(Items.EMERALD,3),6,10));
        case 3->List.of(barter(Items.EMERALD,24,Items.BOOK,1,protectionBook(),2,20),barter(Items.EMERALD,14,Items.APPLE,1,new ItemStack(Items.GOLDEN_APPLE),3,20),buy(Items.DIAMOND,1,6,2,20),sale(Items.GHAST_TEAR,5,1,4,20),sale(Items.GLISTERING_MELON_SLICE,3,2,4,20));
        default->List.of(barter(Items.EMERALD,48,Items.DIAMOND,2,new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),1,30),barter(Items.EMERALD,18,Items.GOLD_INGOT,4,new ItemStack(Items.GOLDEN_APPLE,2),3,30),barter(Items.EMERALD,20,Items.BOOK,1,protectionBook(),2,30),barter(Items.DIAMOND,1,Items.REDSTONE,4,new ItemStack(Items.GLOWSTONE_DUST,12),4,30));
    };}
    private static ItemStack protectionBook(){return EnchantedBookItem.forEnchantment(new net.minecraft.enchantment.EnchantmentLevelEntry(ExpansionItems.TETANUS_PROTECTION,1));}
    public static boolean isMedicine(Item item){return item==ModItems.ANTITOXIN||item==ModItems.VACCINE||item==ExpansionItems.RABIES_CURE||item==ExpansionItems.RABIES_VACCINE;}
    public static TradeOffer randomOffer(VillagerEntity medic,int rank,boolean medical,Random random){return randomOffer(medic,rank,medical,random,medic.getOffers());}
    private static TradeOffer randomOffer(VillagerEntity medic,int rank,boolean medical,Random random,TradeOfferList existing){
        var pool=medical?medicine(rank):supplies(rank);
        if(medical){var unused=pool.stream().filter(option->existing.stream().noneMatch(offer->offer.getSellItem().isOf(option.result().getItem()))).toList();if(!unused.isEmpty())pool=unused;}
        return pool.get(random.nextInt(pool.size())).create(medic,random);
    }
    public static void initialize(){
        for(int level=1;level<=LAST_RANK;level++){
            final int rank=level;
            TradeOfferHelper.registerVillagerOffers(MedicVillage.MEDIC,rank,trades->{
                trades.add((entity,random)->randomOffer((VillagerEntity)entity,rank,true,random));
                trades.add((entity,random)->randomOffer((VillagerEntity)entity,rank,false,random));
            });
        }
        // Existing level-five villagers keep their identity and their final-rank offers.
        TradeOfferHelper.registerVillagerOffers(MedicVillage.MEDIC,5,trades->{trades.add((entity,random)->randomOffer((VillagerEntity)entity,LAST_RANK,true,random));trades.add((entity,random)->randomOffer((VillagerEntity)entity,LAST_RANK,false,random));});
    }
    public static void addEarlierRanks(VillagerEntity medic){
        if(medic.getVillagerData().getProfession()!=MedicVillage.MEDIC||!medic.getOffers().isEmpty())return;
        for(int rank=1;rank<Math.min(LAST_RANK,medic.getVillagerData().getLevel());rank++)addRank(medic,rank,medic.getOffers());
    }
    private static void addRank(VillagerEntity medic,int rank,TradeOfferList list){list.add(randomOffer(medic,rank,true,medic.getRandom(),list));list.add(randomOffer(medic,rank,false,medic.getRandom(),list));}
    public static void generated(VillagerEntity medic){if(medic.getVillagerData().getProfession()==MedicVillage.MEDIC&&((State)medic).rustruin$lastRestock()==Long.MIN_VALUE)((State)medic).rustruin$lastRestock(medic.getWorld().getTime());}
    public static void markField(VillagerEntity medic){((State)medic).rustruin$isFieldMedic(true);}
    public static long restockInterval(VillagerEntity medic){return ((State)medic).rustruin$isFieldMedic()?FIELD_RESTOCK_TICKS:VILLAGE_RESTOCK_TICKS;}
    public static boolean canRestock(VillagerEntity medic){long last=((State)medic).rustruin$lastRestock();return last==Long.MIN_VALUE||medic.getWorld().getTime()-last>=restockInterval(medic);}
    public static void restocked(VillagerEntity medic){((State)medic).rustruin$lastRestock(medic.getWorld().getTime());}
    public static void load(VillagerEntity medic,NbtCompound nbt){
        var state=(State)medic;
        state.rustruin$isFieldMedic(nbt.getBoolean("RustRuinFieldMedic"));
        // Templates have no offers: their delivery clock starts when first stock is generated.
        // Saved offers already represent a delivery and keep their persisted cooldown.
        state.rustruin$lastRestock(!nbt.contains("Offers",NbtElement.COMPOUND_TYPE)?Long.MIN_VALUE:
                nbt.contains("RustRuinMedicRestock",NbtElement.LONG_TYPE)?Math.min(medic.getWorld().getTime(),nbt.getLong("RustRuinMedicRestock")):medic.getWorld().getTime());
        if(medic.getVillagerData().getProfession()==MedicVillage.MEDIC&&nbt.contains("Offers",NbtElement.COMPOUND_TYPE)&&nbt.getInt("RustRuinMedicTradesVersion")<SCHEMA){
            var offers=new TradeOfferList();
            for(int rank=1;rank<=Math.min(LAST_RANK,medic.getVillagerData().getLevel());rank++)addRank(medic,rank,offers);
            medic.setOffers(offers);
        }
    }
    public static void save(VillagerEntity medic,NbtCompound nbt){
        var state=(State)medic;
        nbt.putBoolean("RustRuinFieldMedic",state.rustruin$isFieldMedic());nbt.putLong("RustRuinMedicRestock",state.rustruin$lastRestock());
        if(medic.getVillagerData().getProfession()==MedicVillage.MEDIC)nbt.putInt("RustRuinMedicTradesVersion",SCHEMA);
    }
    public static void sendScreenMetadata(VillagerEntity medic,net.minecraft.entity.player.PlayerEntity player){
        if(medic.getVillagerData().getProfession()!=MedicVillage.MEDIC||!(player instanceof ServerPlayerEntity server)||!(player.currentScreenHandler instanceof net.minecraft.screen.MerchantScreenHandler)||player.currentScreenHandler.syncId<1||player.currentScreenHandler.syncId>100||!ServerPlayNetworking.canSend(server,SCREEN_PACKET))return;
        var packet=PacketByteBufs.create();packet.writeVarInt(player.currentScreenHandler.syncId);packet.writeBoolean(((State)medic).rustruin$isFieldMedic());ServerPlayNetworking.send(server,SCREEN_PACKET,packet);
    }
}
