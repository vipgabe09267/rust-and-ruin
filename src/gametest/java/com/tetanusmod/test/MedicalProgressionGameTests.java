package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.expansion.*;
import com.tetanusmod.medical.*;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Hand;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.BlockMirror;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.world.RaycastContext;
import java.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.GameMode;

/** Transactions use the real server menu, recipe manager and player advancement tracker. */
public final class MedicalProgressionGameTests implements FabricGameTest {
    private static ServerPlayerEntity player(TestContext ctx){var player=ctx.createMockCreativeServerPlayerInWorld();player.changeGameMode(GameMode.SURVIVAL);player.getInventory().clear();return player;}
    private static PrescriptionWriterScreenHandler writer(TestContext ctx,ServerPlayerEntity player){
        var pos=ctx.getAbsolutePos(new BlockPos(3,1,3));ctx.getWorld().setBlockState(pos,MedicalWorkstations.WRITER.getDefaultState());player.setPosition(Vec3d.ofBottomCenter(pos).add(0,1,2));
        var menu=new PrescriptionWriterScreenHandler(1,player.getInventory(),ScreenHandlerContext.create(ctx.getWorld(),pos));player.currentScreenHandler=menu;return menu;
    }
    private static void close(ServerPlayerEntity player,PrescriptionWriterScreenHandler menu){menu.setCursorStack(ItemStack.EMPTY);menu.onClosed(player);player.currentScreenHandler=player.playerScreenHandler;player.discard();}
    private static VillagerEntity medic(TestContext ctx,int rank){var medic=EntityType.VILLAGER.create(ctx.getWorld());medic.setVillagerData(medic.getVillagerData().withProfession(MedicVillage.MEDIC).withLevel(rank));return medic;}
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void prescriptionHasItsOwnStationRecipeAndRejectsSwappedInputs(TestContext ctx){
        var input=new SimpleInventory(2);input.setStack(0,new ItemStack(Items.PAPER));input.setStack(1,new ItemStack(Items.INK_SAC));
        var recipe=ctx.getWorld().getRecipeManager().getFirstMatch(PrescriptionWritingRecipe.TYPE,input,ctx.getWorld()).orElseThrow();
        ctx.assertTrue(recipe.craft(input,ctx.getWorld().getRegistryManager()).isOf(MedicalWorkstations.PRESCRIPTION),"Writing recipe did not produce the registered prescription");
        ctx.assertTrue(recipe.fits(1,2)&&recipe.fits(2,1)&&!recipe.fits(-2,-2)&&!recipe.fits(0,8)&&!recipe.fits(1,1),"Writing recipe accepted invalid dimensions or rejected its actual input layout");
        input.setStack(0,new ItemStack(Items.INK_SAC));input.setStack(1,new ItemStack(Items.PAPER));ctx.assertFalse(recipe.matches(input,ctx.getWorld()),"Swapped station inputs were accepted");
        var user=ctx.createMockSurvivalPlayer();var grid=new net.minecraft.inventory.CraftingInventory(new CraftingScreenHandler(1,user.getInventory()),3,3);grid.setStack(0,new ItemStack(Items.PAPER));grid.setStack(1,new ItemStack(Items.INK_SAC));
        var vanilla=ctx.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING,grid,ctx.getWorld());ctx.assertTrue(vanilla.isEmpty()||!vanilla.get().craft(grid,ctx.getWorld().getRegistryManager()).isOf(MedicalWorkstations.PRESCRIPTION),"Ordinary crafting bypasses the writer");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void nativeWriterPickupConsumesOnePaperAndInk(TestContext ctx){
        var player=player(ctx);var menu=writer(ctx,player);
        try{
            menu.setCursorStack(new ItemStack(Items.PAPER,3));menu.onSlotClick(0,0,SlotActionType.PICKUP,player);
            menu.setCursorStack(new ItemStack(Items.INK_SAC,2));menu.onSlotClick(1,0,SlotActionType.PICKUP,player);
            ctx.assertTrue(menu.getSlot(2).getStack().isOf(MedicalWorkstations.PRESCRIPTION),"Native input placement did not update output");
            menu.onSlotClick(2,0,SlotActionType.PICKUP,player);
            ctx.assertTrue(menu.getCursorStack().isOf(MedicalWorkstations.PRESCRIPTION)&&menu.getCursorStack().getCount()==1,"Native result pickup returned the wrong item/count");
            ctx.assertTrue(menu.getSlot(0).getStack().getCount()==2&&menu.getSlot(1).getStack().getCount()==1,"Prescription output failed exact ingredient accounting");
            ctx.assertFalse(menu.getSlot(2).canInsert(new ItemStack(Items.DIAMOND)),"Output slot accepts forged inputs");
        }finally{close(player,menu);}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void writerShiftClickFullInventoryConsumesNothingThenCraftsExactBatch(TestContext ctx){
        var player=player(ctx);var menu=writer(ctx,player);
        try{
            for(int i=0;i<player.getInventory().main.size();i++)player.getInventory().main.set(i,new ItemStack(Items.COBBLESTONE,64));
            menu.getSlot(0).setStack(new ItemStack(Items.PAPER,3));menu.getSlot(1).setStack(new ItemStack(Items.INK_SAC,2));
            menu.onSlotClick(2,0,SlotActionType.QUICK_MOVE,player);
            ctx.assertTrue(menu.getSlot(0).getStack().getCount()==3&&menu.getSlot(1).getStack().getCount()==2,"Full inventory destroyed ingredients");
            player.getInventory().setStack(8,ItemStack.EMPTY);menu.onSlotClick(2,0,SlotActionType.QUICK_MOVE,player);
            ctx.assertTrue(player.getInventory().count(MedicalWorkstations.PRESCRIPTION)==2&&menu.getSlot(0).getStack().getCount()==1&&menu.getSlot(1).getStack().isEmpty()&&menu.getSlot(2).getStack().isEmpty(),"Native shift-click batch duplicated output or mischarged inputs");
        }finally{close(player,menu);}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void closingOrBreakingWriterReturnsOnlyRealInputs(TestContext ctx){
        var player=player(ctx);var menu=writer(ctx,player);
        try{
            menu.getSlot(0).setStack(new ItemStack(Items.PAPER,5));menu.getSlot(1).setStack(new ItemStack(Items.INK_SAC,4));
            ctx.getWorld().setBlockState(ctx.getAbsolutePos(new BlockPos(3,1,3)),Blocks.AIR.getDefaultState());ctx.assertFalse(menu.canUse(player),"Broken writer remains usable");
            menu.onClosed(player);menu.onClosed(player);
            ctx.assertTrue(player.getInventory().count(Items.PAPER)==5&&player.getInventory().count(Items.INK_SAC)==4&&player.getInventory().count(MedicalWorkstations.PRESCRIPTION)==0,"Closing returned preview output or duplicated/lost inputs");
        }finally{close(player,menu);}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void treatmentTradesRequirePrescriptionAndNoDoctorTradesRawPaper(TestContext ctx){
        for(int seed=0;seed<48;seed++){
            var medic=medic(ctx,4);medic.getRandom().setSeed(seed*761L+9);
            for(var offer:medic.getOffers()){
                ctx.assertTrue(!offer.getOriginalFirstBuyItem().isOf(Items.PAPER)&&!offer.getSecondBuyItem().isOf(Items.PAPER)&&!offer.getSellItem().isOf(Items.PAPER),"A doctor still trades generic paper");
                if(MedicTradeOffers.isMedicine(offer.getSellItem().getItem())||offer.getSellItem().isOf(ExpansionItems.VETERINARY_KIT))ctx.assertTrue(offer.getOriginalFirstBuyItem().isOf(Items.EMERALD)&&offer.getSecondBuyItem().isOf(MedicalWorkstations.PRESCRIPTION)&&offer.getSecondBuyItem().getCount()==1,"Treatment does not require emeralds plus one prescription");
            }
        }ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void oldPaperOffersMigrateOnceWithoutRepeatedStockRefills(TestContext ctx){
        var medic=medic(ctx,2);var nbt=medic.writeNbt(new NbtCompound());nbt.putInt("RustRuinMedicTradesVersion",2);
        var old=new net.minecraft.village.TradeOfferList();old.add(new net.minecraft.village.TradeOffer(new ItemStack(Items.PAPER,4),new ItemStack(ModItems.VACCINE),20,1,0.05F));nbt.put("Offers",old.toNbt());
        var copy=medic(ctx,1);copy.readNbt(nbt);ctx.assertTrue(copy.getOffers().size()==4&&copy.getOffers().stream().noneMatch(offer->offer.getOriginalFirstBuyItem().isOf(Items.PAPER)||offer.getSecondBuyItem().isOf(Items.PAPER)),"Saved paper catalogue did not migrate");
        copy.getOffers().get(0).disable();var saved=copy.writeNbt(new NbtCompound());var again=medic(ctx,1);again.readNbt(saved);ctx.assertTrue(again.getOffers().toNbt().equals(copy.getOffers().toNbt()),"Prescription migration rerolls or refills every reload");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void earlyTetanusCureAwardsCommonSenseWithoutWaitingForLockjaw(TestContext ctx){
        var player=player(ctx);
        try{
            var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id("antitoxin"));ctx.assertTrue(advancement!=null&&advancement.getParent().getId().equals(TetanusMod.id("infected")),"Early cure is still hidden behind Lockjaw progression");
            CommandEffects.apply(player,new StatusEffectInstance(TetanusMod.TETANUS,10000),null);ctx.assertFalse(player.hasStatusEffect(TetanusMod.LOCKJAW),"Fixture entered Lockjaw before the early cure");
            ModItems.ANTITOXIN.finishUsing(new ItemStack(ModItems.ANTITOXIN),ctx.getWorld(),player);
            ctx.assertTrue(!player.hasStatusEffect(TetanusMod.TETANUS)&&player.getAdvancementTracker().getProgress(advancement).isDone(),"Successful early antitoxin cure did not award Common Sense");
        }finally{player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void standaloneLockjawCureAwardsButHealthyDoseDoesNot(TestContext ctx){
        var healthy=player(ctx);var ill=player(ctx);
        try{
            var advancement=ill.getServer().getAdvancementLoader().get(TetanusMod.id("antitoxin"));
            ModItems.ANTITOXIN.finishUsing(new ItemStack(ModItems.ANTITOXIN),ctx.getWorld(),healthy);ctx.assertFalse(healthy.getAdvancementTracker().getProgress(advancement).isDone(),"Healthy use falsely awarded a cure");
            ill.addStatusEffect(new StatusEffectInstance(TetanusMod.LOCKJAW,1200));ModItems.ANTITOXIN.finishUsing(new ItemStack(ModItems.ANTITOXIN),ctx.getWorld(),ill);
            ctx.assertTrue(!ill.hasStatusEffect(TetanusMod.LOCKJAW)&&ill.getAdvancementTracker().getProgress(advancement).isDone(),"Standalone Lockjaw cure failed its advancement");
            ctx.assertTrue(ill.getAdvancementTracker().getProgress(advancement).getCriterionProgress("cure_lockjaw").isObtained(),"Native consume-item Lockjaw criterion did not fire");
        }finally{healthy.discard();ill.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void supplyInstructionsContainActualCountAndRecognizeCompletedDelivery(TestContext ctx){
        var medic=medic(ctx,1);var wanted=MedicSupplies.request(medic);var message=MedicSupplies.requestInstructions(medic,new ItemStack(wanted.getItem(),1));
        ctx.assertTrue(message.getContent() instanceof TranslatableTextContent,"Supply message lost translation arguments");var content=(TranslatableTextContent)message.getContent();
        ctx.assertTrue(content.getKey().equals("message.tetanusmod.supply_request")&&content.getArgs().length==3&&content.getArgs()[0].equals(wanted.getCount())&&content.getArgs()[2].equals(1),"Request does not show the actual requested item/count and held amount");
        ((MedicSupplies.State)medic).rustruin$lastSupplyDay(MedicSupplies.day(medic));ctx.assertTrue(((TranslatableTextContent)MedicSupplies.requestInstructions(medic,ItemStack.EMPTY).getContent()).getKey().equals("message.tetanusmod.supplies_done"),"Completed daily request still invites a duplicate delivery");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void treatmentUnlocksAndBasePricesFollowMedicalRank(TestContext ctx){
        var previous=new HashMap<Item,Integer>();
        for(int rank=1;rank<=4;rank++){
            var seen=new HashMap<Item,Integer>();
            var medic=medic(ctx,rank);medic.setOffers(new net.minecraft.village.TradeOfferList());
            for(int seed=0;seed<128;seed++){
                var offer=MedicTradeOffers.randomOffer(medic,rank,true,net.minecraft.util.math.random.Random.create(seed*941L+17));
                Item result=offer.getSellItem().getItem();seen.put(result,offer.getOriginalFirstBuyItem().getCount());
                ctx.assertTrue(offer.getSecondBuyItem().isOf(MedicalWorkstations.PRESCRIPTION)&&offer.getSecondBuyItem().getCount()==1,"Ranked treatment waived its prescription");
                if(rank==1)ctx.assertTrue(result==ModItems.ANTITOXIN||result==ModItems.VACCINE,"A Nurse sells advanced treatment");
                if(rank<3)ctx.assertTrue(result!=ExpansionItems.RABIES_VACCINE&&result!=ExpansionItems.VETERINARY_KIT,"Doctor-only treatment appeared too early");
            }
            if(rank>=2)ctx.assertTrue(seen.containsKey(ExpansionItems.RABIES_CURE),"Practitioner or higher cannot offer Rabies Cure");
            if(rank>=3)ctx.assertTrue(seen.containsKey(ExpansionItems.RABIES_VACCINE)&&seen.containsKey(ExpansionItems.VETERINARY_KIT),"Doctor or higher lost advanced care");
            for(var entry:previous.entrySet())if(seen.containsKey(entry.getKey()))ctx.assertTrue(seen.get(entry.getKey())<=entry.getValue(),"Higher-rank treatment becomes more expensive: "+entry.getKey());
            previous=seen;
        }
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void writerAndEmergencyPrescriptionTradesStayScarceWithoutDuplicateStock(TestContext ctx){
        boolean writerSeen=false,prescriptionSeen=false;
        for(boolean field:new boolean[]{false,true})for(int seed=0;seed<128;seed++){
            var medic=medic(ctx,4);if(field)MedicTradeOffers.markField(medic);medic.getRandom().setSeed(seed*2053L+19);
            var distinct=new HashSet<Item>();int prescriptions=0,writers=0;
            for(var offer:medic.getOffers()){
                Item result=offer.getSellItem().getItem();
                if(MedicTradeOffers.isMedicine(result))ctx.assertTrue(distinct.add(result)&&offer.getMaxUses()>=2&&offer.getMaxUses()<=(field?2:4),"Treatment stock duplicates or exceeds delivery limits");
                if(result==MedicalWorkstations.PRESCRIPTION){prescriptionSeen=true;prescriptions++;ctx.assertTrue(offer.getSellItem().getCount()==1&&offer.getMaxUses()==(field?1:2),"Emergency prescription trade has unlimited/batched stock");}
                if(result==MedicalWorkstations.WRITER_ITEM){writerSeen=true;writers++;ctx.assertTrue(offer.getSellItem().getCount()==1&&offer.getMaxUses()==1,"Writer trade exceeds one per delivery");}
            }
            ctx.assertTrue(medic.getOffers().size()==8&&prescriptions<=1&&writers<=1,"Rank progression duplicated scarce support offers");
        }
        ctx.assertTrue(writerSeen&&prescriptionSeen,"Randomized catalogue never sells the new workstation or emergency prescriptions");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void heroDiscountsWriterAndPrescriptionWithoutWaivingScarceStock(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try{
            player.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.HERO_OF_THE_VILLAGE,1200));
            for(Item result:List.of(MedicalWorkstations.WRITER_ITEM,MedicalWorkstations.PRESCRIPTION)){
                VillagerEntity seller=null;net.minecraft.village.TradeOffer sale=null;
                for(int seed=0;seed<128&&seller==null;seed++){
                    var candidate=medic(ctx,1);candidate.getRandom().setSeed(seed*2053L+19);
                    for(var offer:candidate.getOffers())if(offer.getSellItem().isOf(result)){seller=candidate;sale=offer;break;}
                }
                ctx.assertTrue(seller!=null&&sale!=null,"No Nurse can trade "+result);
                int before=sale.getAdjustedFirstBuyItem().getCount(),stock=sale.getMaxUses();seller.setPosition(player.getPos());player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);player.setSneaking(false);
                seller.interactMob(player,Hand.MAIN_HAND);
                ctx.assertTrue(sale.getAdjustedFirstBuyItem().getCount()<before&&sale.getMaxUses()==stock,"Hero discount failed or replenished scarce support stock");
                player.closeHandledScreen();seller.setCustomer(null);ctx.assertTrue(sale.getAdjustedFirstBuyItem().getCount()==before,"Discount leaked into another customer's quote");
            }
        }finally{player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void schemaFourMigrationPreservesRemainingStockAndDeliveryClock(TestContext ctx){
        for(boolean field:new boolean[]{false,true}){
            var original=medic(ctx,4);if(field)MedicTradeOffers.markField(original);var old=original.getOffers();
            for(int i=0;i<old.size();i++){if(i%3==0)old.get(i).disable();else old.get(i).use();}
            var clock=(MedicTradeOffers.State)original;clock.rustruin$lastRestock(ctx.getWorld().getTime()-7);
            var nbt=original.writeNbt(new NbtCompound());nbt.putInt("RustRuinMedicTradesVersion",3);
            var loaded=medic(ctx,1);loaded.readNbt(nbt);var updated=loaded.getOffers();
            for(var offer:updated){
                Item result=offer.getSellItem().getItem();
                long previousUnits=old.stream().filter(prior->prior.getSellItem().isOf(result)).mapToLong(prior->(long)Math.max(0,prior.getMaxUses()-prior.getUses())*prior.getSellItem().getCount()).sum();
                long newUnits=(long)(offer.getMaxUses()-offer.getUses())*offer.getSellItem().getCount();
                ctx.assertTrue(newUnits<=previousUnits,"Catalogue migration refilled a sell-item resource: "+result);
            }
            ctx.assertTrue(((MedicTradeOffers.State)loaded).rustruin$lastRestock()==clock.rustruin$lastRestock()&&!MedicTradeOffers.canRestock(loaded),"Catalogue migration reset the delivery clock");
            var saved=loaded.writeNbt(new NbtCompound());var reread=medic(ctx,1);reread.readNbt(saved);ctx.assertTrue(reread.getOffers().toNbt().equals(updated.toNbt()),"Schema-four migration repeats after reload");
        }
        ctx.complete();
    }
    @GameTest(templateName="tetanusmod:test/camp_empty",batchId="medical_writer_access",tickLimit=400)
    public void everyClinicThemeAndCaveCampWriterIsReachableInSurvivalAfterNativePlacement(TestContext ctx){
        var world=ctx.getWorld();var user=player(ctx);
        try{
            for(String name:List.of("village/plains_clinic","village/desert_clinic","village/savanna_clinic","village/snowy_clinic","village/taiga_clinic","medic_tent")){
                var template=world.getStructureTemplateManager().getTemplate(TetanusMod.id(name)).orElseThrow();
                for(var rotation:BlockRotation.values()){
                    var placement=new StructurePlacementData().setRotation(rotation).setIgnoreEntities(true).setPlaceFluids(false).setUpdateNeighbors(true);
                    var origin=template.offsetByTransformedSize(ctx.getAbsolutePos(new BlockPos(2,2,2)),BlockMirror.NONE,rotation);
                    var bounds=template.calculateBoundingBox(placement,origin);var before=new LinkedHashMap<BlockPos,BlockState>();
                    for(var cell:BlockPos.iterate(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX(),bounds.getMaxY(),bounds.getMaxZ())){
                        ctx.assertTrue(world.getBlockEntity(cell)==null,"Writer placement fixture overlaps an existing block entity");before.put(cell.toImmutable(),world.getBlockState(cell));
                    }
                    try{
                        ctx.assertTrue(template.place(world,origin,origin,placement,world.random,Block.NOTIFY_LISTENERS|Block.FORCE_STATE),"Native medical template placement failed: "+name);
                        var writers=template.getInfosForBlock(origin,placement,MedicalWorkstations.WRITER);ctx.assertTrue(writers.size()==1,"Generated medical template lacks exactly one writer: "+name);
                        var at=writers.get(0).pos();var state=world.getBlockState(at);ctx.assertTrue(state.equals(writers.get(0).state()),"Placed writer rotation differs from template: "+name+" "+rotation);
                        var aisle=at.offset(state.get(HorizontalFacingBlock.FACING)).down();
                        ctx.assertTrue(world.getBlockState(at.down()).isSolidBlock(world,at.down())&&world.getBlockState(aisle.down()).isSolidBlock(world,aisle.down())&&world.isAir(aisle)&&world.isAir(aisle.up()),"Writer has no supported standing aisle: "+name+" "+rotation);
                        user.setPosition(Vec3d.ofBottomCenter(aisle));user.setSneaking(false);user.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);
                        ctx.assertTrue(world.isSpaceEmpty(user,user.getBoundingBox()),"Survival player collides with the writer desk or aisle");
                        Vec3d target=Vec3d.ofCenter(at);ctx.assertTrue(user.getEyePos().squaredDistanceTo(target)<4.5*4.5,"Shelf writer exceeds Survival block reach");
                        var hit=world.raycast(new RaycastContext(user.getEyePos(),target,RaycastContext.ShapeType.OUTLINE,RaycastContext.FluidHandling.NONE,user));
                        ctx.assertTrue(hit.getType()==net.minecraft.util.hit.HitResult.Type.BLOCK&&hit.getBlockPos().equals(at),"Desk or wall obstructs the Survival writer click");
                        var result=user.interactionManager.interactBlock(user,world,user.getMainHandStack(),Hand.MAIN_HAND,hit);
                        ctx.assertTrue(result.isAccepted()&&user.currentScreenHandler instanceof PrescriptionWriterScreenHandler,"Survival writer click did not open its real native menu");
                        user.closeHandledScreen();
                    }finally{user.closeHandledScreen();for(var entry:before.entrySet())world.setBlockState(entry.getKey(),entry.getValue(),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);}
                }
            }
        }finally{user.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void soldOutAntitoxinCannotInheritFullVaccineStockWhenCataloguePositionsSwap(TestContext ctx){
        var original=medic(ctx,2);var old=new net.minecraft.village.TradeOfferList();
        var anti=new net.minecraft.village.TradeOffer(new ItemStack(Items.EMERALD,8),new ItemStack(MedicalWorkstations.PRESCRIPTION),new ItemStack(ModItems.ANTITOXIN),4,5,.05F);anti.disable();old.add(anti);
        old.add(new net.minecraft.village.TradeOffer(new ItemStack(Items.GLASS_BOTTLE,12),new ItemStack(Items.EMERALD),3,3,.05F));
        old.add(new net.minecraft.village.TradeOffer(new ItemStack(Items.EMERALD,10),new ItemStack(MedicalWorkstations.PRESCRIPTION),new ItemStack(ModItems.VACCINE),4,10,.05F));
        var prescription=new net.minecraft.village.TradeOffer(new ItemStack(Items.EMERALD,3),new ItemStack(MedicalWorkstations.PRESCRIPTION),2,3,.05F);prescription.use();old.add(prescription);
        original.setOffers(old);((MedicTradeOffers.State)original).rustruin$lastRestock(ctx.getWorld().getTime()-5);
        var saved=original.writeNbt(new NbtCompound());saved.putInt("RustRuinMedicTradesVersion",3);boolean swapped=false;
        for(int seed=0;seed<64;seed++){
            var loaded=medic(ctx,1);loaded.getRandom().setSeed(seed*2053L+19);loaded.readNbt(saved);var offers=loaded.getOffers();int antiIndex=-1,vaccineIndex=-1;
            for(int i=0;i<offers.size();i++){
                var offer=offers.get(i);
                if(offer.getSellItem().isOf(ModItems.ANTITOXIN)){antiIndex=i;ctx.assertTrue(offer.isDisabled(),"Sold-out Antitoxin inherited another medicine's stock");}
                if(offer.getSellItem().isOf(ModItems.VACCINE)){vaccineIndex=i;ctx.assertTrue(offer.getUses()==0,"Full Vaccine stock was incorrectly exhausted by Antitoxin's old index");}
                if(offer.getSellItem().isOf(MedicalWorkstations.PRESCRIPTION))ctx.assertTrue(offer.getMaxUses()-offer.getUses()<=1,"Prescription supply was refilled by catalogue migration");
            }
            if(antiIndex>=0&&vaccineIndex>=0&&antiIndex>vaccineIndex)swapped=true;
            ctx.assertTrue(((MedicTradeOffers.State)loaded).rustruin$lastRestock()==((MedicTradeOffers.State)original).rustruin$lastRestock()&&!MedicTradeOffers.canRestock(loaded),"Swapped catalogue bypassed the saved delivery clock");
            loaded.restock();for(var offer:offers)if(offer.getSellItem().isOf(ModItems.ANTITOXIN))ctx.assertTrue(offer.isDisabled(),"Early native restock refilled migrated Antitoxin");
        }
        ctx.assertTrue(swapped,"Regression failed to exercise deliberately swapped medicine positions");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void newlyIntroducedSuppliesCannotBorrowUnrelatedEmeraldPurchaseStock(TestContext ctx){
        var original=medic(ctx,3);var prior=new net.minecraft.village.TradeOfferList();
        prior.add(new net.minecraft.village.TradeOffer(new ItemStack(Items.GLASS_BOTTLE,12),new ItemStack(Items.EMERALD),8,3,.05F));original.setOffers(prior);((MedicTradeOffers.State)original).rustruin$lastRestock(ctx.getWorld().getTime()-5);
        var saved=original.writeNbt(new NbtCompound());saved.putInt("RustRuinMedicTradesVersion",3);boolean diamondSeen=false;
        for(int seed=0;seed<64;seed++){
            var loaded=medic(ctx,1);loaded.getRandom().setSeed(seed*8713L+17);loaded.readNbt(saved);
            for(var offer:loaded.getOffers()){
                if(offer.getOriginalFirstBuyItem().isOf(Items.DIAMOND)&&offer.getSellItem().isOf(Items.EMERALD)){diamondSeen=true;ctx.assertTrue(offer.isDisabled(),"New diamond purchase borrowed old bottle-purchase stock");}
                if(MedicTradeOffers.isMedicine(offer.getSellItem().getItem()))ctx.assertTrue(offer.isDisabled(),"New medicine was gifted before the inherited delivery clock");
            }
        }
        ctx.assertTrue(diamondSeen,"Regression never selected the unrelated emerald purchase");ctx.complete();
    }
}
