package com.tetanusmod.test;

import com.tetanusmod.expansion.*;
import com.tetanusmod.network.ModPacketDecoder;
import com.tetanusmod.registry.ModItems;
import com.tetanusmod.test.mixin.MedicTestAccessor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.village.*;
import java.util.HashSet;

public class MedicTradingGameTests implements FabricGameTest {
    private VillagerEntity medic(TestContext ctx,int rank,boolean field){
        var medic=EntityType.VILLAGER.create(ctx.getWorld());medic.getRandom().setSeed(40L+rank);
        medic.setVillagerData(medic.getVillagerData().withProfession(MedicVillage.MEDIC).withLevel(rank));
        if(field)MedicTradeOffers.markField(medic);return medic;
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void nursesOfferOneRandomMedicineAndOneSupply(TestContext ctx){
        var medic=medic(ctx,1,false);var offers=medic.getOffers();
        ctx.assertTrue(offers.size()==2,"Nurse still appends the full medicine catalogue");
        ctx.assertTrue(offers.stream().filter(offer->MedicTradeOffers.isMedicine(offer.getSellItem().getItem())).count()==1,"Nurse does not split medicine and supplies");
        ctx.assertTrue(offers.stream().allMatch(offer->offer.getMerchantExperience()>0),"Nurse cannot earn experience");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=120)
    public void twoMedicineTradesActuallyPromoteTheNurse(TestContext ctx){
        var medic=medic(ctx,1,false);medic.setPosition(ctx.getAbsolute(new net.minecraft.util.math.Vec3d(3.5,4,3.5)));medic.setNoGravity(true);medic.setInvulnerable(true);ctx.getWorld().spawnEntity(medic);
        var offer=medic.getOffers().stream().filter(trade->MedicTradeOffers.isMedicine(trade.getSellItem().getItem())).findFirst().orElseThrow();
        medic.trade(offer);medic.trade(offer);
        ctx.assertTrue(medic.getExperience()>=10,"Actual trades do not award the experience needed to leave Nurse");
        ctx.runAtTick(70,()->{try{ctx.assertTrue(medic.getVillagerData().getProfession()==MedicVillage.MEDIC&&medic.getVillagerData().getLevel()==2,"Native delayed promotion left the medic at Nurse");ctx.assertTrue(medic.getOffers().size()==4,"Automatic promotion failed to unlock trades");ctx.complete();}finally{medic.discard();}});
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void promotionsAppendOffersAndStopAtAttending(TestContext ctx){
        var medic=medic(ctx,1,false);var access=(MedicTestAccessor)medic;medic.getOffers();
        for(int rank=1;rank<4;rank++){
            medic.setExperience(VillagerData.getUpperLevelExperience(rank));
            ctx.assertTrue(access.rustruin$canLevelUp(),"Medic cannot qualify for rank "+(rank+1));
            access.rustruin$levelUp();ctx.assertTrue(medic.getVillagerData().getLevel()==rank+1&&medic.getOffers().size()==(rank+1)*2,"Promotion lost or repeated offers");
        }
        medic.setExperience(1000);ctx.assertFalse(access.rustruin$canLevelUp(),"Attending continued to an unnamed fifth rank");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void attendingHasEightOffersWithoutDuplicateMedicines(TestContext ctx){
        var medic=medic(ctx,4,false);var offers=medic.getOffers();var medicines=new HashSet<Item>();
        ctx.assertTrue(offers.size()==8,"Fresh attending lacks the earlier rank trades needed for scrolling");
        for(var offer:offers)if(MedicTradeOffers.isMedicine(offer.getSellItem().getItem())){
            ctx.assertTrue(medicines.add(offer.getSellItem().getItem()),"Medicine stocks multiply through duplicate rank offers");
            ctx.assertTrue(offer.getSellItem().getCount()==1&&offer.getMaxUses()>=2&&offer.getMaxUses()<=4,"Medicine stock exceeds two to four single doses");
        }
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void medicVarietyIncludesBarterIngredientsAndRareFood(TestContext ctx){
        var clinicResults=new HashSet<Item>();var fieldResults=new HashSet<Item>();var costs=new HashSet<Item>();var nurses=new HashSet<String>();
        for(boolean field:new boolean[]{false,true}){
        var results=field?fieldResults:clinicResults;
        for(int seed=0;seed<128;seed++){
            var medic=medic(ctx,4,field);medic.getRandom().setSeed(seed*8713L+17);
            for(var offer:medic.getOffers()){
                results.add(offer.getSellItem().getItem());costs.add(offer.getOriginalFirstBuyItem().getItem());
                if(!offer.getSecondBuyItem().isEmpty())costs.add(offer.getSecondBuyItem().getItem());
            }
            var nurse=medic(ctx,1,field);nurse.getRandom().setSeed(seed*8713L+17);nurses.add(nurse.getOffers().toNbt().toString());
        }
        }
        for(Item item:new Item[]{Items.HONEYCOMB,Items.GOLDEN_APPLE,Items.ENCHANTED_GOLDEN_APPLE,Items.BLAZE_POWDER,Items.GHAST_TEAR})ctx.assertTrue(clinicResults.contains(item)&&(!fieldResults.contains(item)||item==Items.GOLDEN_APPLE),"Clinic supplies missing or no longer distinct: "+item);
        for(Item item:new Item[]{Items.LANTERN,Items.SCAFFOLDING,ModItems.WIRE_BRUSH,ExpansionItems.TRAP_RESET_TOOL,Items.COMPASS})ctx.assertTrue(fieldResults.contains(item)&&!clinicResults.contains(item),"Field supplies missing or no longer distinct: "+item);
        for(Item item:new Item[]{Items.IRON_INGOT,Items.FLINT,Items.DIAMOND,Items.EMERALD,com.tetanusmod.medical.MedicalWorkstations.PRESCRIPTION,Items.HONEY_BOTTLE})ctx.assertTrue(costs.contains(item),"Trade payments never use "+item);
        ctx.assertTrue(nurses.size()>8,"Independent medics receive effectively identical trades");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void medicinesSellOutAndDoNotImmediatelyRestock(TestContext ctx){
        var medic=medic(ctx,1,false);var offer=medic.getOffers().stream().filter(trade->MedicTradeOffers.isMedicine(trade.getSellItem().getItem())).findFirst().orElseThrow();
        int stock=offer.getMaxUses();for(int i=0;i<stock;i++)offer.use();
        ctx.assertTrue(offer.isDisabled()&&stock>=2&&stock<=4,"Medicine fails to sell out within four purchases");
        medic.restock();ctx.assertTrue(offer.isDisabled(),"Direct restock bypasses the supply interval");
        ((MedicTestAccessor)medic).rustruin$dailyRestock();ctx.assertTrue(offer.isDisabled(),"Native daily reset bypasses scarcity");
        ctx.assertFalse(medic.shouldRestock(),"AI requests an early refill");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void villageRestockRefreshesOncePerDay(TestContext ctx){
        var medic=medic(ctx,1,false);var offer=medic.getOffers().get(0);offer.disable();var state=(MedicTradeOffers.State)medic;
        state.rustruin$lastRestock(ctx.getWorld().getTime()-MedicTradeOffers.VILLAGE_RESTOCK_TICKS);
        medic.restock();ctx.assertTrue(!offer.isDisabled()&&offer.getUses()==0,"Due village delivery did not replenish stock");
        ctx.assertTrue(state.rustruin$lastRestock()==ctx.getWorld().getTime(),"Refill time was not saved");
        offer.disable();medic.restock();ctx.assertTrue(offer.isDisabled(),"Village receives a second delivery immediately");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void caveStockIsScarcerAndRestocksEveryThreeDays(TestContext ctx){
        var medic=medic(ctx,4,true);var offers=medic.getOffers();var state=(MedicTradeOffers.State)medic;
        ctx.assertTrue(offers.stream().allMatch(offer->offer.getMaxUses()<=2),"Field medic has village-size supplies");
        offers.forEach(TradeOffer::disable);state.rustruin$lastRestock(ctx.getWorld().getTime()-24000);
        medic.restock();((MedicTestAccessor)medic).rustruin$dailyRestock();ctx.assertTrue(offers.stream().allMatch(TradeOffer::isDisabled),"Field medic refills on the village schedule");
        state.rustruin$lastRestock(ctx.getWorld().getTime()-MedicTradeOffers.FIELD_RESTOCK_TICKS);
        medic.restock();ctx.assertTrue(offers.stream().noneMatch(TradeOffer::isDisabled),"Field supply delivery never arrives");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void stockPricesAndCaveCooldownSurviveReload(TestContext ctx){
        var medic=medic(ctx,4,true);var offers=medic.getOffers();offers.get(0).disable();offers.get(1).use();
        String savedOffers=offers.toNbt().toString();var nbt=medic.writeNbt(new NbtCompound());
        var copy=medic(ctx,1,false);copy.readNbt(nbt);
        ctx.assertTrue(((MedicTradeOffers.State)copy).rustruin$isFieldMedic(),"Field provenance was lost on reload");
        ctx.assertTrue(copy.getOffers().toNbt().toString().equals(savedOffers),"Reload rerolls or replenishes offers");
        ctx.assertTrue(((MedicTradeOffers.State)copy).rustruin$lastRestock()==((MedicTradeOffers.State)medic).rustruin$lastRestock(),"Cooldown was reset on reload");
        copy.restock();ctx.assertTrue(copy.getOffers().get(0).isDisabled(),"Reload bypasses scarcity");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void firstStockStartsItsOwnCooldownInsteadOfUsingTemplateAge(TestContext ctx){
        for(boolean field:new boolean[]{false,true}){
            var template=medic(ctx,1,field).writeNbt(new NbtCompound());template.remove("Offers");
            template.putLong("RustRuinMedicRestock",ctx.getWorld().getTime()-MedicTradeOffers.FIELD_RESTOCK_TICKS);
            var loaded=medic(ctx,1,false);loaded.readNbt(template);var state=(MedicTradeOffers.State)loaded;
            ctx.assertTrue(state.rustruin$lastRestock()==Long.MIN_VALUE,"Unopened template already has a stock clock");
            var offer=loaded.getOffers().get(0);offer.disable();
            ctx.assertTrue(state.rustruin$lastRestock()==ctx.getWorld().getTime(),"First delivery inherited the structure's age");
            loaded.restock();((MedicTestAccessor)loaded).rustruin$dailyRestock();
            ctx.assertTrue(offer.isDisabled(),"Old unopened template immediately replenished newly sold stock");
        }
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void legacyUnlimitedNoviceListsMigrateOnce(TestContext ctx){
        var old=medic(ctx,1,false);var list=new TradeOfferList();
        for(Item item:new Item[]{ModItems.ANTITOXIN,ModItems.VACCINE,ModItems.PROTECTIVE_GLOVES,ExpansionItems.RABIES_CURE,ExpansionItems.RABIES_VACCINE})list.add(new TradeOffer(new ItemStack(Items.EMERALD,8),new ItemStack(item),12,2,0.05F));
        old.setOffers(list);var nbt=old.writeNbt(new NbtCompound());nbt.remove("RustRuinMedicTradesVersion");
        var copy=medic(ctx,1,false);copy.readNbt(nbt);ctx.assertTrue(copy.getOffers().size()==2,"Legacy novice kept all five oversized trades");
        copy.getOffers().get(0).disable();var migrated=copy.writeNbt(new NbtCompound());var again=medic(ctx,1,false);again.readNbt(migrated);
        ctx.assertTrue(again.getOffers().toNbt().equals(copy.getOffers().toNbt()),"Migration repeats and refills stock on each load");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void heroOfTheVillageDiscountsActualMedicineOffers(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try{
            var medic=medic(ctx,1,false);medic.setPosition(player.getPos());player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);player.setSneaking(false);
            var medicine=medic.getOffers().stream().filter(offer->MedicTradeOffers.isMedicine(offer.getSellItem().getItem())).findFirst().orElseThrow();int base=medicine.getAdjustedFirstBuyItem().getCount();
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE,1200));medic.interactMob(player,Hand.MAIN_HAND);
            ctx.assertTrue(medicine.getAdjustedFirstBuyItem().getCount()<base,"Hero does not discount actual native offers");
            player.closeHandledScreen();medic.setCustomer(null);ctx.assertTrue(medicine.getAdjustedFirstBuyItem().getCount()==base,"Hero discount leaked to the next customer");
        }finally{player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void nonMedicTradesKeepTheirNativeRestock(TestContext ctx){
        var farmer=EntityType.VILLAGER.create(ctx.getWorld());farmer.setVillagerData(farmer.getVillagerData().withProfession(VillagerProfession.FARMER));
        var offers=new TradeOfferList();var offer=new TradeOffer(new ItemStack(Items.WHEAT,20),new ItemStack(Items.EMERALD),16,2,0.05F);offers.add(offer);farmer.setOffers(offers);offer.disable();
        farmer.restock();ctx.assertFalse(offer.isDisabled(),"Medic supply restrictions changed another profession");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void medicScreenMetadataIsStrictlyBounded(TestContext ctx){
        var valid=PacketByteBufs.create();try{valid.writeVarInt(100);valid.writeBoolean(true);var parsed=ModPacketDecoder.medicScreen(valid);ctx.assertTrue(parsed.syncId()==100&&parsed.fieldMedic(),"Valid screen metadata rejected");}finally{valid.release();}
        for(byte[] data:new byte[][]{new byte[0],{1},{0,0},{101,0},{1,2},{1,0,0},{-1,0}}){var bytes=PacketByteBufs.create();try{bytes.writeBytes(data);boolean rejected=false;try{ModPacketDecoder.medicScreen(bytes);}catch(RuntimeException expected){rejected=true;}ctx.assertTrue(rejected,"Malformed screen metadata accepted");}finally{bytes.release();}}
        ctx.complete();
    }
}
