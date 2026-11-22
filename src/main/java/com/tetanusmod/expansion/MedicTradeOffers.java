package com.tetanusmod.expansion;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.registry.ModItems;
import com.tetanusmod.medical.MedicalWorkstations;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.potion.PotionUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.*;
import java.util.List;

/** Two persistent, randomly selected offers per rank. Native gossip, demand and Hero prices apply. */
public final class MedicTradeOffers {
    public static final Identifier SCREEN_PACKET=TetanusMod.id("medic_trade_screen");
    public static final int LAST_RANK=4;
    public static final int SCHEMA=5;
    public static final long VILLAGE_RESTOCK_TICKS=24000L, FIELD_RESTOCK_TICKS=72000L;
    public interface State {
        boolean rustruin$isFieldMedic();
        void rustruin$isFieldMedic(boolean value);
        long rustruin$lastRestock();
        void rustruin$lastRestock(long tick);
    }
    private record Option(ItemStack first,ItemStack second,ItemStack result,int stock,int xp) {
        TradeOffer create(VillagerEntity medic,Random random) {
            boolean field=isFieldMedic(medic);
            int uses=isMedicine(result.getItem())?(field?2:2+random.nextInt(3)):stock;
            if(field)uses=Math.min(uses,2);
            return new TradeOffer(first.copy(),second.copy(),result.copy(),uses,xp,0.05F);
        }
        boolean matchesTreatment(TradeOffer offer) {
            return ItemStack.areEqual(result,offer.getSellItem())
                    &&ItemStack.areEqual(first,offer.getOriginalFirstBuyItem())
                    &&ItemStack.areEqual(second,offer.getSecondBuyItem());
        }
    }
    private static Option sale(Item item,int emeralds,int count,int stock,int xp){return barter(Items.EMERALD,emeralds,null,0,new ItemStack(item,count),stock,xp);}
    private static Option buy(Item item,int count,int emeralds,int stock,int xp){return barter(item,count,null,0,new ItemStack(Items.EMERALD,emeralds),stock,xp);}
    private static Option barter(Item first,int count,Item second,int secondCount,ItemStack result,int stock,int xp){return new Option(new ItemStack(first,count),second==null?ItemStack.EMPTY:new ItemStack(second,secondCount),result,stock,xp);}
    private static Option prescribed(Item item,int emeralds,int xp){return barter(Items.EMERALD,emeralds,MedicalWorkstations.PRESCRIPTION,1,new ItemStack(item),4,xp);}
    private static List<Option> medicine(int rank){return switch(rank){
        case 1->List.of(prescribed(ModItems.ANTITOXIN,8,5),prescribed(ModItems.VACCINE,10,5));
        case 2->List.of(prescribed(ModItems.ANTITOXIN,7,10),prescribed(ModItems.VACCINE,9,10),prescribed(ExpansionItems.RABIES_CURE,12,10));
        case 3->List.of(prescribed(ModItems.ANTITOXIN,6,20),prescribed(ModItems.VACCINE,8,20),prescribed(ExpansionItems.RABIES_CURE,11,20),prescribed(ExpansionItems.RABIES_VACCINE,14,20),prescribed(ExpansionItems.VETERINARY_KIT,12,20));
        default->List.of(prescribed(ModItems.ANTITOXIN,5,30),prescribed(ModItems.VACCINE,7,30),prescribed(ExpansionItems.RABIES_CURE,10,30),prescribed(ExpansionItems.RABIES_VACCINE,12,30),prescribed(ExpansionItems.VETERINARY_KIT,10,30));
    };}
    private static List<Option> villageSupplies(int rank){return switch(rank){
        case 1->List.of(
                buy(Items.GLASS_BOTTLE,12,1,3,3),buy(Items.HONEY_BOTTLE,3,2,2,3),
                sale(Items.HONEYCOMB,3,4,3,3),barter(Items.EMERALD,7,Items.LEATHER,2,new ItemStack(ModItems.PROTECTIVE_GLOVES),2,3),
                sale(MedicalWorkstations.WRITER_ITEM,10,1,1,3));
        case 2->List.of(
                sale(Items.BLAZE_POWDER,4,2,3,10),sale(Items.BREWING_STAND,5,1,2,10),
                sale(ExpansionBlocks.MEDIC_CABINET.asItem(),10,1,1,10),sale(MedicalWorkstations.WRITER_ITEM,8,1,1,10),
                barter(Items.HONEY_BOTTLE,2,Items.WHITE_WOOL,4,new ItemStack(Items.EMERALD,3),3,10));
        case 3->List.of(
                buy(Items.DIAMOND,1,6,1,20),sale(Items.GHAST_TEAR,5,1,2,20),
                sale(Items.GLISTERING_MELON_SLICE,3,2,2,20),sale(Items.GLOWSTONE_DUST,3,4,3,20),
                barter(Items.EMERALD,12,Items.APPLE,1,new ItemStack(Items.GOLDEN_APPLE),2,20));
        default->List.of(
                barter(Items.EMERALD,48,Items.DIAMOND,2,new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),1,30),
                barter(Items.EMERALD,18,Items.GOLD_INGOT,4,new ItemStack(Items.GOLDEN_APPLE,2),1,30),
                barter(Items.EMERALD,18,Items.BOOK,1,protectionBook(),1,30),
                barter(Items.EMERALD,9,Items.GLASS_BOTTLE,3,new ItemStack(Items.DRAGON_BREATH,3),1,30));
    };}
    private static List<Option> fieldSupplies(int rank){return switch(rank){
        case 1->List.of(
                buy(ModItems.RUST,8,2,3,3),buy(Items.IRON_INGOT,5,1,3,3),buy(Items.FLINT,8,1,3,3),
                sale(Items.LANTERN,2,1,3,3),sale(Items.BAKED_POTATO,2,6,3,3),
                sale(MedicalWorkstations.WRITER_ITEM,10,1,1,3));
        case 2->List.of(
                barter(Items.EMERALD,4,Items.IRON_INGOT,1,new ItemStack(ModItems.WIRE_BRUSH),2,10),
                sale(ExpansionItems.TRAP_RESET_TOOL,4,1,2,10),sale(ModItems.PROTECTIVE_GLOVES,8,1,2,10),
                sale(Items.SCAFFOLDING,3,12,2,10),buy(Items.GLOW_BERRIES,12,2,3,10));
        case 3->List.of(
                barter(Items.EMERALD,5,Items.IRON_INGOT,2,new ItemStack(Items.SHIELD),2,20),
                sale(Items.GOLDEN_CARROT,4,4,2,20),sale(Items.ENDER_PEARL,5,2,2,20),
                sale(Items.NAME_TAG,8,1,2,20),sale(Items.COMPASS,5,1,2,20));
        default->List.of(
                barter(Items.EMERALD,18,Items.BOOK,1,protectionBook(),1,30),
                barter(Items.EMERALD,10,Items.PHANTOM_MEMBRANE,1,PotionUtil.setPotion(new ItemStack(Items.POTION),net.minecraft.potion.Potions.SLOW_FALLING),1,30),
                barter(Items.EMERALD,15,ModItems.RUST,16,new ItemStack(Items.GOLDEN_APPLE),1,30),
                barter(Items.EMERALD,18,Items.LEATHER,4,new ItemStack(Items.SADDLE),1,30),
                barter(Items.DIAMOND,1,Items.REDSTONE,4,new ItemStack(Items.GLOWSTONE_DUST,12),2,30));
    };}
    private static ItemStack protectionBook(){return EnchantedBookItem.forEnchantment(new net.minecraft.enchantment.EnchantmentLevelEntry(ExpansionItems.TETANUS_PROTECTION,1));}
    public static boolean isMedicine(Item item){return item==ModItems.ANTITOXIN||item==ModItems.VACCINE||item==ExpansionItems.RABIES_CURE||item==ExpansionItems.RABIES_VACCINE||item==ExpansionItems.VETERINARY_KIT;}
    public static TradeOffer randomOffer(VillagerEntity medic,int rank,boolean medical,Random random){return randomOffer(medic,rank,medical,random,medic.getOffers());}
    private static TradeOffer randomOffer(VillagerEntity medic,int rank,boolean medical,Random random,TradeOfferList existing){
        var pool=medical?medicine(rank):isFieldMedic(medic)?fieldSupplies(rank):villageSupplies(rank);
        var unused=pool.stream().filter(option->existing.stream().noneMatch(offer->sameStock(option,offer))).toList();
        if(!unused.isEmpty())pool=unused;
        return pool.get(random.nextInt(pool.size())).create(medic,random);
    }
    private static boolean sameStock(Option candidate,TradeOffer existing){
        if(!existing.getSellItem().isOf(candidate.result().getItem()))return false;
        // Separate supply purchases can pay emeralds without duplicating the same trade.
        return !candidate.result().isOf(Items.EMERALD)
                ||existing.getOriginalFirstBuyItem().isOf(candidate.first().getItem())
                &&ItemStack.areItemsEqual(existing.getSecondBuyItem(),candidate.second());
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
    public static boolean isFieldMedic(VillagerEntity medic){return ((State)medic).rustruin$isFieldMedic();}
    public static long restockInterval(VillagerEntity medic){return isFieldMedic(medic)?FIELD_RESTOCK_TICKS:VILLAGE_RESTOCK_TICKS;}
    public static boolean canRestock(VillagerEntity medic){long last=((State)medic).rustruin$lastRestock();return last==Long.MIN_VALUE||medic.getWorld().getTime()-last>=restockInterval(medic);}
    public static void restocked(VillagerEntity medic){((State)medic).rustruin$lastRestock(medic.getWorld().getTime());}
    public static void load(VillagerEntity medic,NbtCompound nbt){
        var state=(State)medic;
        state.rustruin$isFieldMedic(nbt.getBoolean("RustRuinFieldMedic"));
        // Templates have no offers: their delivery clock starts when first stock is generated.
        // Saved offers already represent a delivery and keep their persisted cooldown.
        state.rustruin$lastRestock(!nbt.contains("Offers",NbtElement.COMPOUND_TYPE)?Long.MIN_VALUE:
                nbt.contains("RustRuinMedicRestock",NbtElement.LONG_TYPE)?Math.min(medic.getWorld().getTime(),nbt.getLong("RustRuinMedicRestock")):medic.getWorld().getTime());
        int previousVersion=nbt.getInt("RustRuinMedicTradesVersion");
        if(medic.getVillagerData().getProfession()==MedicVillage.MEDIC&&nbt.contains("Offers",NbtElement.COMPOUND_TYPE)&&previousVersion<SCHEMA){
            var previous=new TradeOfferList(nbt.getCompound("Offers"));
            var offers=new TradeOfferList();
            for(int rank=1;rank<=Math.min(LAST_RANK,medic.getVillagerData().getLevel());rank++){
                var treatments=medicine(rank);
                var treatment=previousVersion==4?previous.stream()
                        .filter(offer->treatments.stream().anyMatch(option->option.matchesTreatment(offer)))
                        .filter(offer->offers.stream().noneMatch(current->current.getSellItem().isOf(offer.getSellItem().getItem())))
                        .findFirst().orElse(null):null;
                if(treatment==null){
                    treatment=randomOffer(medic,rank,true,medic.getRandom(),offers);
                    preserveRemainingStock(treatment,previous);
                }
                offers.add(treatment);
                var supply=randomOffer(medic,rank,false,medic.getRandom(),offers);
                preserveRemainingStock(supply,previous);
                offers.add(supply);
            }
            medic.setOffers(offers);
        }
    }
    private static void preserveRemainingStock(TradeOffer updated,TradeOfferList previous){
        ItemStack result=updated.getSellItem();
        long limit=(long)updated.getMaxUses()*result.getCount(),units=0;
        for(var old:previous){
            if(!sameMigrationResource(updated,old))continue;
            long remaining=Math.max(0L,(long)old.getMaxUses()-Math.max(0,old.getUses()));
            units=Math.min(limit,units+remaining*old.getSellItem().getCount());
        }
        int retained=(int)Math.min(updated.getMaxUses(),units/Math.max(1,result.getCount()));
        // New catalogue entries wait for the existing delivery rather than gifting fresh stock on load.
        for(int use=retained;use<updated.getMaxUses();use++)updated.use();
    }
    private static boolean sameMigrationResource(TradeOffer updated,TradeOffer previous){
        ItemStack result=updated.getSellItem(),oldResult=previous.getSellItem();
        if(!oldResult.isOf(result.getItem()))return false;
        if(isMedicine(result.getItem()))return true;
        // Distinguish, for example, buying diamonds for emeralds from buying empty bottles for emeralds.
        return ItemStack.canCombine(result,oldResult)
                &&sameCostResource(updated.getOriginalFirstBuyItem(),previous.getOriginalFirstBuyItem())
                &&sameCostResource(updated.getSecondBuyItem(),previous.getSecondBuyItem());
    }
    private static boolean sameCostResource(ItemStack first,ItemStack second){
        return first.isEmpty()?second.isEmpty():!second.isEmpty()&&ItemStack.canCombine(first,second);
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
