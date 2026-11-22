package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.command.MobInspector;
import com.tetanusmod.config.ModConfig;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.expansion.*;
import com.tetanusmod.registry.ModItems;
import com.tetanusmod.rust.RustCoating;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.Difficulty;

public class MaintenanceGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void spawnCategoriesAreIndependentAndBounded(TestContext ctx) {
        var before=TetanusMod.CONFIG;
        try {
            var config=ModConfig.fromJson("{\"naturalAnimalEasy\":0.9,\"naturalMobEasy\":0.123,\"spawnerEasy\":0.4,\"naturalAnimalHard\":4,\"naturalMobNormal\":-1,\"coatingWeatherTicks\":0}");
            TetanusMod.CONFIG=config;
            ctx.assertTrue(NaturalVariants.chance(Difficulty.EASY,false,true)==0.9F,"Animal rate ignored");
            ctx.assertTrue(NaturalVariants.chance(Difficulty.EASY,false,false)==0.123F,"Other mob rate changed");
            ctx.assertTrue(NaturalVariants.chance(Difficulty.EASY,true,true)==0.4F,"Spawner rate ignored");
            ctx.assertTrue(NaturalVariants.chance(Difficulty.PEACEFUL,true,true)==0,"Peaceful mutation enabled");
            ctx.assertTrue(config.naturalAnimalHard==1&&config.naturalMobNormal==0&&config.coatingWeatherTicks==20,"Config bounds bypassed");
        } finally {TetanusMod.CONFIG=before;}
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void oldConfigRetainsDefaultNaturalRates(TestContext ctx) {
        var config=ModConfig.fromJson("{\"balanceRevision\":4}");
        ctx.assertTrue(config.naturalAnimalEasy==0.005F&&config.naturalAnimalNormal==0.01F&&config.naturalAnimalHard==0.02F,"Animal defaults changed");
        ctx.assertTrue(config.spawnerEasy==0.03F&&config.spawnerNormal==0.05F&&config.spawnerHard==0.08F,"Spawner defaults changed");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void medicCasesRotateThroughDifferentVillageAndFieldCases(TestContext ctx) {
        var uuid=java.util.UUID.randomUUID();
        var clinic=new java.util.HashSet<MedicCases.CaseFile>();
        var field=new java.util.HashSet<MedicCases.CaseFile>();
        for(int day=0;day<3;day++){
            var assignment=MedicCases.offerFor(false,uuid,day);
            clinic.add(assignment);
            field.add(MedicCases.offerFor(true,uuid,day));
            ctx.assertTrue(assignment==MedicCases.offerFor(false,uuid,day+3),"Clinic case changed on the same point in the rotation");
        }
        ctx.assertTrue(clinic.size()==3&&field.size()==3&&java.util.Collections.disjoint(clinic,field),"Case pools do not rotate independently");
        ctx.complete();
    }
    private net.minecraft.entity.passive.VillagerEntity medic(TestContext ctx) {
        var medic=EntityType.VILLAGER.create(ctx.getWorld());
        medic.setVillagerData(medic.getVillagerData().withProfession(MedicVillage.MEDIC));
        medic.setPosition(ctx.getAbsolute(new Vec3d(1,3,1)));return medic;
    }
    private void visit(net.minecraft.entity.passive.VillagerEntity medic,ServerPlayerEntity player,Hand hand) {
        player.setSneaking(true);
        UseEntityCallback.EVENT.invoker().interact(player,player.getWorld(),hand,medic,null);
        player.setSneaking(false);
    }
    private NbtCompound caseData(String file,int step) {
        var nbt=new NbtCompound();
        var data=new NbtCompound();
        data.putString("File",file);
        data.putInt("Step",step);
        nbt.put("RustRuinMedicCase",data);
        return nbt;
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void clinicCaseAcceptsPartialDeliveriesPersistsAndRewardsOnce(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        var visitor=ctx.createMockCreativeServerPlayerInWorld();
        try {
            player.getAbilities().creativeMode=false;var medic=medic(ctx);player.setPosition(medic.getPos());
            var progress=MedicCases.progress(player);
            progress.read(caseData("clinic_restock",0));
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE,2));
            visit(medic,player,Hand.MAIN_HAND);
            ctx.assertTrue(player.getMainHandStack().isEmpty()&&progress.remaining()==4,"Partial delivery did not consume just the offered bottles");
            var saved=player.writeNbt(new NbtCompound());
            progress.read(new NbtCompound());
            player.readNbt(saved);
            ctx.assertTrue(progress.remaining()==4&&progress.activeCase()==MedicCases.CaseFile.CLINIC_RESTOCK,"Partial case progress was lost on reload");
            visitor.setPosition(medic.getPos());
            visit(medic,visitor,Hand.MAIN_HAND);
            var other=MedicCases.progress(visitor);
            ctx.assertTrue(other.activeCase()!=null&&other.remaining()==other.currentStep().amount()&&progress.remaining()==4,"Another player shared the first player's case progress");
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE,6));
            visit(medic,player,Hand.MAIN_HAND);
            ctx.assertTrue(player.getMainHandStack().getCount()==2&&progress.currentStep().delivery()==Items.HONEY_BOTTLE,"Delivery consumed excess stock or failed to advance");
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.HONEY_BOTTLE,2));
            visit(medic,player,Hand.MAIN_HAND);
            ctx.assertTrue(progress.activeCase()==null&&player.getInventory().count(Items.GLASS_BOTTLE)==2
                    &&player.getInventory().count(Items.EMERALD)==3&&player.getInventory().count(Items.HONEY_BOTTLE)==1,"Clinic case rewards or bottle returns were incorrect");
            var medicine=medic.getOffers().stream().filter(offer->MedicTradeOffers.isMedicine(offer.getSellItem().getItem())).findFirst().orElseThrow();int basePrice=medicine.getAdjustedFirstBuyItem().getCount();
            player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);
            medic.interactMob(player,Hand.MAIN_HAND);
            int discounted=medicine.getAdjustedFirstBuyItem().getCount();
            ctx.assertTrue(discounted<basePrice,"Delivery does not reduce actual medicine trade price");
            player.closeHandledScreen();medic.setCustomer(null);
            var gossip=medic.getGossip().getReputationFor(player.getUuid(),type->true);
            ctx.assertTrue(gossip>0,"Discount reputation missing");
            var completed=player.writeNbt(new NbtCompound());
            progress.read(new NbtCompound());
            player.readNbt(completed);
            visit(medic,player,Hand.MAIN_HAND);
            ctx.assertTrue(progress.activeCase()==null&&player.getInventory().count(Items.EMERALD)==3,"Reload bypassed the personal one-case-per-day limit");
        } finally {player.discard();visitor.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void invalidSupplyDeliveriesCannotEarnDiscount(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try {
            var medic=medic(ctx);player.setPosition(medic.getPos());
            visit(medic,player,Hand.MAIN_HAND);
            var progress=MedicCases.progress(player);
            int remaining=progress.remaining();
            var supplies=new ItemStack(progress.currentStep().delivery(),remaining);
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.DIRT,64));
            visit(medic,player,Hand.MAIN_HAND);
            ctx.assertTrue(progress.remaining()==remaining,"Wrong material advanced a case");
            player.setStackInHand(Hand.MAIN_HAND,supplies);
            visit(medic,player,Hand.OFF_HAND);
            ctx.assertTrue(progress.remaining()==remaining,"Offhand interaction advanced a case");
            var field=medic(ctx);
            MedicTradeOffers.markField(field);
            visit(field,player,Hand.MAIN_HAND);
            ctx.assertTrue(progress.remaining()==remaining,"A field medic accepted clinic supplies");
            player.setPosition(medic.getPos().add(100,0,0));
            visit(medic,player,Hand.MAIN_HAND);
            ctx.assertTrue(progress.remaining()==remaining&&supplies.getCount()==remaining,"Remote delivery was accepted");
        } finally {player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void realFieldworkAdvancesMatchingCaseAndRequiresReportingBack(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        var respawn=ctx.createMockCreativeServerPlayerInWorld();
        var cow=EntityType.COW.create(ctx.getWorld());
        try {
            var medic=medic(ctx);
            player.setPosition(medic.getPos());
            cow.setPosition(player.getPos());
            var progress=MedicCases.progress(player);
            progress.read(caseData("herd_checkup",1));
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.WIRE_BRUSH));
            player.setStackInHand(Hand.OFF_HAND,new ItemStack(ModItems.RUSTED_NAIL));
            ModItems.WIRE_BRUSH.use(ctx.getWorld(),player,Hand.MAIN_HAND);
            ctx.assertTrue(progress.remaining()==1,"Cleaning equipment counted as veterinary care");
            MobVariants.makePermanent(cow);
            var kit=new ItemStack(ExpansionItems.VETERINARY_KIT);
            player.setStackInHand(Hand.MAIN_HAND,kit);
            ctx.assertTrue(kit.useOnEntity(player,cow,Hand.MAIN_HAND).isAccepted(),"Real kit did not treat the mutated cow");
            ctx.assertTrue(progress.activeCase()!=null&&progress.currentStep()==null&&player.getInventory().count(Items.EMERALD)==0,"Case did not wait for a final medic report");
            respawn.copyFrom(player,false);
            respawn.setPosition(medic.getPos());
            ctx.assertTrue(MedicCases.progress(respawn).activeCase()==MedicCases.CaseFile.HERD_CHECKUP,"Respawning lost completed fieldwork");
            visit(medic,respawn,Hand.MAIN_HAND);
            ctx.assertTrue(respawn.getInventory().count(Items.EMERALD)==5,"Returning to the clinic did not award fieldwork");
        } finally {cow.discard();player.discard();respawn.discard();}
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void fieldCasesTrackActualCleaningAndCountOnlyQualifyingPatrolKills(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try {
            var medic=medic(ctx);
            MedicTradeOffers.markField(medic);
            player.setPosition(medic.getPos());
            var progress=MedicCases.progress(player);
            progress.read(caseData("salvage_inspection",1));
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.WIRE_BRUSH));
            player.setStackInHand(Hand.OFF_HAND,new ItemStack(ModItems.NAIL));
            ModItems.WIRE_BRUSH.use(ctx.getWorld(),player,Hand.MAIN_HAND);
            ctx.assertTrue(progress.remaining()==1,"Brushing something clean counted as salvage work");
            player.setStackInHand(Hand.OFF_HAND,new ItemStack(ModItems.RUSTED_NAIL));
            ModItems.WIRE_BRUSH.use(ctx.getWorld(),player,Hand.MAIN_HAND);
            ctx.assertTrue(progress.currentStep()==null,"Successful Wire Brush conversion did not count");

            progress.read(caseData("cave_patrol",1));
            var below=medic.getPos().add(0,27-medic.getY(),0);
            killForPatrol(ctx,player,EntityType.COW,below);
            killForPatrol(ctx,player,EntityType.ZOMBIE,below.add(0,8,0));
            ctx.assertTrue(progress.remaining()==2,"A passive animal or above-threshold kill counted toward the cave patrol");
            killForPatrol(ctx,player,EntityType.ZOMBIE,below);
            ctx.assertTrue(progress.remaining()==1,"A qualifying hostile kill did not count exactly once");
            killForPatrol(ctx,player,EntityType.SLIME,below);
            ctx.assertTrue(progress.activeCase()!=null&&progress.currentStep()==null,"A qualifying slime kill did not finish cave fieldwork");
            visit(medic,player,Hand.MAIN_HAND);
            ctx.assertTrue(progress.activeCase()==null&&player.getInventory().count(Items.EMERALD)==4,"The medic did not reward a completed patrol");
        } finally {player.discard();}
        ctx.complete();
    }
    private void killForPatrol(TestContext ctx,ServerPlayerEntity player,EntityType<? extends LivingEntity> type,Vec3d pos) {
        var target=type.create(ctx.getWorld());
        target.setPosition(pos);
        try {
            ctx.assertTrue(ctx.getWorld().spawnEntity(target),"Could not spawn cave patrol fixture");
            target.damage(target.getDamageSources().playerAttack(player),100);
            ctx.assertTrue(!target.isAlive(),"Cave patrol fixture survived the native hit");
        } finally {target.discard();}
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void coatingSurvivesSerializationAndExpires(TestContext ctx) {
        var stack=new ItemStack(ExpansionItems.SPEAR);stack.setDamage(20);stack.setCustomName(net.minecraft.text.Text.literal("Reliable spear"));
        ctx.assertTrue(RustCoating.coat(stack),"Clean spear not coat-able");
        ctx.assertFalse(RustCoating.coat(stack),"Coating renewed without wearing out");
        var copy=ItemStack.fromNbt(stack.writeNbt(new NbtCompound()));
        ctx.assertTrue(RustCoating.remaining(copy)==TetanusMod.CONFIG.coatingWeatherTicks&&copy.getDamage()==20&&copy.hasCustomName(),"Coating damaged item data");
        RustCoating.wear(copy,Integer.MAX_VALUE);ctx.assertTrue(RustCoating.remaining(copy)==0,"Coating cannot expire");
        ctx.assertTrue(RustCoating.coat(copy),"Cannot recoat exhausted protection");
        ctx.assertFalse(RustCoating.coat(new ItemStack(ModItems.RUSTED_NAIL)),"Wax cleans rusty objects");
        ctx.assertFalse(RustCoating.coat(new ItemStack(Items.DIRT)),"Coats non-equipment");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void honeycombCallbackConsumesPerItem(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try {
            player.getAbilities().creativeMode=false;
            player.setStackInHand(Hand.OFF_HAND,new ItemStack(ModItems.NAIL,4));player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.HONEYCOMB,3));
            var result=UseItemCallback.EVENT.invoker().interact(player,ctx.getWorld(),Hand.MAIN_HAND);
            ctx.assertTrue(result.getResult()==net.minecraft.util.ActionResult.FAIL&&RustCoating.remaining(player.getOffHandStack())==0,"Underpayment coats stack");
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.HONEYCOMB,5));
            UseItemCallback.EVENT.invoker().interact(player,ctx.getWorld(),Hand.MAIN_HAND);
            ctx.assertTrue(player.getMainHandStack().getCount()==1&&RustCoating.remaining(player.getOffHandStack())>0,"Wrong coating consumption");
        } finally {player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=160)
    public void coatingActuallyDelaysDroppedNailRust(TestContext ctx) {
        var pos=ctx.getAbsolutePos(new BlockPos(1,2,1));ctx.getWorld().setBlockState(pos,net.minecraft.block.Blocks.WATER.getDefaultState());
        for(int y=1;y<=4;y++)ctx.getWorld().setBlockState(pos.up(y),net.minecraft.block.Blocks.WATER.getDefaultState());
        var stack=new ItemStack(ModItems.NAIL);RustCoating.coat(stack);RustCoating.wear(stack,RustCoating.remaining(stack)-5);
        var item=new ItemEntity(ctx.getWorld(),pos.getX()+0.5,pos.getY()+0.3,pos.getZ()+0.5,stack);item.setNoGravity(true);item.setVelocity(Vec3d.ZERO);item.setPickupDelay(32767);ctx.getWorld().spawnEntity(item);
        ctx.runAtTick(4,()->ctx.assertTrue(item.getStack().isOf(ModItems.NAIL),"Coating failed immediately"));
        ctx.runAtTick(130,()->{ctx.assertTrue(item.getStack().isOf(ModItems.RUSTED_NAIL),"Nail never rusted after coating expired");item.discard();ctx.complete();});
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void equipmentUseWearsCoating(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try {player.getAbilities().creativeMode=false;var spear=new ItemStack(ExpansionItems.SPEAR);RustCoating.coat(spear);int before=RustCoating.remaining(spear);spear.damage(1,player,p->{});
            ctx.assertTrue(RustCoating.remaining(spear)==before-20,"Durability use does not wear coating");
        } finally {player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void veterinaryKitRestoresAnimalAndConsumesOnce(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try {player.getAbilities().creativeMode=false;var cow=EntityType.COW.create(ctx.getWorld());cow.setPosition(player.getPos());
            double speed=cow.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);
            MobVariants.makePermanent(cow);var kit=new ItemStack(ExpansionItems.VETERINARY_KIT,2);
            ctx.assertTrue(kit.useOnEntity(player,cow,Hand.MAIN_HAND).isAccepted(),"Animal treatment rejected");
            ctx.assertFalse(MobVariants.permanent(cow)||cow.hasStatusEffect(TetanusMod.TETANUS),"Animal remains infected");
            ctx.assertTrue(kit.getCount()==1&&Math.abs(cow.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED)-speed)<0.0001,"Treatment failed to restore movement");
            ctx.assertFalse(kit.useOnEntity(player,cow,Hand.MAIN_HAND).isAccepted(),"Healthy animal consumes kit");
            MobVariants.tick(cow);ctx.assertFalse(cow.hasStatusEffect(TetanusMod.TETANUS),"Cured animal reinfected by permanent tick");
        } finally {player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabidTreatmentRemovesAttributesAndHuntGoal(TestContext ctx) {
        var cow=EntityType.COW.create(ctx.getWorld());double health=cow.getMaxHealth(),armor=cow.getArmor();Rabies.makeRabid(cow);
        ctx.assertTrue(Rabies.cureAnimal(cow),"Rabid animal not curable");
        ctx.assertFalse(Rabies.rabid(cow),"Rabid flag remains");
        ctx.assertTrue(cow.getMaxHealth()==health&&cow.getArmor()==armor&&cow.getHealth()<=health,"Mutation attributes remain");
        ctx.assertTrue(((Rabies.MobGoals)cow).rustruin$goals().getGoals().stream().noneMatch(goal->goal.getGoal() instanceof Rabies.RabidHuntGoal),"Rabid hunt goal remains");
        var copy=EntityType.COW.create(ctx.getWorld());copy.readNbt(cow.writeNbt(new NbtCompound()));Rabies.loaded(copy);
        ctx.assertFalse(Rabies.rabid(copy),"Reload restores rabid state");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void veterinaryKitRefusesHostilesAndEmptyStacks(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try {var zombie=EntityType.ZOMBIE.create(ctx.getWorld());zombie.setPosition(player.getPos());MobVariants.makePermanent(zombie);var kit=new ItemStack(ExpansionItems.VETERINARY_KIT);
            ctx.assertFalse(kit.useOnEntity(player,zombie,Hand.MAIN_HAND).isAccepted(),"Kit cures hostile mob");ctx.assertTrue(kit.getCount()==1,"Refused treatment consumes kit");
            var cow=EntityType.COW.create(ctx.getWorld());MobVariants.makePermanent(cow);cow.setPosition(player.getPos());
            ExpansionItems.VETERINARY_KIT.useOnEntity(ItemStack.EMPTY,player,cow,Hand.MAIN_HAND);
            ctx.assertTrue(MobVariants.permanent(cow),"Empty kit gives free treatment");
        } finally {player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void inspectorCommandsAreBoundedAndPermissionChecked(TestContext ctx) throws Exception {
        var dispatcher=ctx.getWorld().getServer().getCommandManager().getDispatcher();
        var source=ctx.getWorld().getServer().getCommandSource().withWorld(ctx.getWorld()).withPosition(ctx.getAbsolute(new Vec3d(1,3,1))).withLevel(2);
        ctx.assertTrue(dispatcher.execute("rustandruin population 64",source)>=0,"Population command failed");
        for(String command:new String[]{"rustandruin population 129","rustandruin population 0","rustandruin population 64"}) {
            boolean denied=false;try{dispatcher.execute(command,command.endsWith("64")?source.withLevel(0):source);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
            ctx.assertTrue(denied,"Unsafe command accepted: "+command);
        }
        var zombie=EntityType.ZOMBIE.create(ctx.getWorld());ctx.assertTrue(MobInspector.reason(zombie).contains("eligible"),"Normal zombie despawn diagnosis wrong");
        zombie.setCustomName(net.minecraft.text.Text.literal("Pet"));ctx.assertTrue(MobInspector.reason(zombie).contains("named"),"Named mob diagnosis missing");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void workshopRegistryTemplateAndTrapsLoad(TestContext ctx) {
        var manager=ctx.getWorld().getRegistryManager();
        ctx.assertTrue(manager.get(RegistryKeys.STRUCTURE).containsId(TetanusMod.id("workshop")),"Natural workshop structure missing");
        ctx.assertTrue(manager.get(RegistryKeys.STRUCTURE_SET).containsId(TetanusMod.id("workshops")),"Workshop placement missing");
        var template=ctx.getWorld().getStructureTemplateManager().getTemplate(TetanusMod.id("workshop")).orElseThrow();
        ctx.assertTrue(template.getSize().equals(new Vec3i(11,8,13)),"Workshop size wrong");
        var pos=ctx.getAbsolutePos(new BlockPos(0,4,0));ctx.assertTrue(template.place(ctx.getWorld(),pos,pos,new net.minecraft.structure.StructurePlacementData(),ctx.getWorld().random,2),"Workshop placement failed");
        ctx.assertTrue(ctx.getWorld().getBlockState(pos.add(7,1,9)).isOf(ExpansionBlocks.RUSTED_BEAR_TRAP),"Workshop trap missing");
        ctx.assertTrue(ctx.getWorld().getBlockEntity(pos.add(2,1,10)) instanceof net.minecraft.block.entity.BarrelBlockEntity,"Supply barrel missing");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void workshopLootRecipeAndAdvancementsExist(TestContext ctx) {
        var server=ctx.getWorld().getServer();
        ctx.assertTrue(server.getRecipeManager().get(TetanusMod.id("veterinary_kit")).isPresent(),"Kit recipe missing");
        for(String id:new String[]{"clean_bill_of_health","supply_run"})ctx.assertTrue(server.getAdvancementLoader().get(TetanusMod.id(id))!=null,"Advancement missing: "+id);
        var table=server.getLootManager().getLootTable(TetanusMod.id("chests/workshop"));ctx.assertTrue(table!=net.minecraft.loot.LootTable.EMPTY,"Workshop loot table missing");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void configuredNaturalRatesReachRealSpawnHook(TestContext ctx) {
        var before=TetanusMod.CONFIG;
        try {var config=new ModConfig();config.naturalAnimalNormal=1;config.naturalMobNormal=0;TetanusMod.CONFIG=config;
            // Test world's Normal difficulty is explicit in the harness; use the matching field for all difficulties.
            config.naturalAnimalEasy=config.naturalAnimalHard=1;config.naturalMobEasy=config.naturalMobHard=0;
            var chicken=EntityType.CHICKEN.create(ctx.getWorld());chicken.initialize(ctx.getWorld(),ctx.getWorld().getLocalDifficulty(chicken.getBlockPos()),SpawnReason.NATURAL,null,null);
            var skeleton=EntityType.SKELETON.create(ctx.getWorld());skeleton.initialize(ctx.getWorld(),ctx.getWorld().getLocalDifficulty(skeleton.getBlockPos()),SpawnReason.NATURAL,null,null);
            ctx.assertTrue(MobVariants.permanent(chicken),"Natural animal config never reaches spawn hook");
            ctx.assertFalse(MobVariants.permanent(skeleton),"Animal rate leaks into hostile spawn hook");
        } finally {TetanusMod.CONFIG=before;}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void longAndInfiniteSalmonellaContinueVomiting(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try {
            player.getHungerManager().setFoodLevel(20);
            var longEffect=new net.minecraft.entity.effect.StatusEffectInstance(TetanusMod.SALMONELLA,6000);
            longEffect.update(player,()->{});
            ctx.assertTrue(player.getHungerManager().getFoodLevel()==18,"Long Salmonella silently skips vomiting");
            player.getHungerManager().setFoodLevel(20);player.age=2400;
            var infinite=new net.minecraft.entity.effect.StatusEffectInstance(TetanusMod.SALMONELLA,-1);
            infinite.update(player,()->{});
            ctx.assertTrue(player.getHungerManager().getFoodLevel()==18,"Infinite Salmonella stops vomiting after one minute");
            ctx.assertFalse(TetanusMod.SALMONELLA.canApplyUpdateEffect(5999,0),"Vomiting runs every tick");
        } finally {player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void workshopCreatesRealWorldgenStart(TestContext ctx) {
        var world=ctx.getWorld();var generator=world.getChunkManager().getChunkGenerator();
        var structure=world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(TetanusMod.id("workshop"));
        var allowed=net.minecraft.registry.tag.TagKey.of(RegistryKeys.BIOME,TetanusMod.id("has_workshop"));
        var start=structure.createStructureStart(world.getRegistryManager(),generator,generator.getBiomeSource(),
                world.getChunkManager().getNoiseConfig(),world.getStructureTemplateManager(),world.getSeed(),
                new net.minecraft.util.math.ChunkPos(0,0),0,world,entry->entry.isIn(allowed));
        ctx.assertTrue(start.hasChildren(),"Workshop cannot generate through native jigsaw world generation");
        ctx.assertTrue(start.getBoundingBox().getBlockCountX()>=11,"Workshop generation has an empty template");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void treatmentRestoresNormalLootAndAwardsItsAdvancement(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try {
            player.getAbilities().creativeMode=false;
            var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(3,4,3));player.setPosition(cow.getPos());
            MobVariants.makePermanent(cow);var kit=new ItemStack(ExpansionItems.VETERINARY_KIT);
            kit.useOnEntity(player,cow,Hand.MAIN_HAND);
            var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id("clean_bill_of_health"));
            ctx.assertTrue(kit.isEmpty()&&player.getAdvancementTracker().getProgress(advancement).isDone(),"Treatment did not consume kit/award Clean Bill of Health");
            cow.damage(ctx.getWorld().getDamageSources().genericKill(),10000);
            var drops=ctx.getWorld().getEntitiesByClass(ItemEntity.class,cow.getBoundingBox().expand(2),item->true);
            ctx.assertTrue(drops.stream().anyMatch(item->item.getStack().isOf(Items.BEEF)),"Cured cow did not restore ordinary meat drops");
            ctx.assertTrue(drops.stream().noneMatch(item->item.getStack().isOf(ModItems.INFECTED_MEAT)||item.getStack().isOf(ExpansionItems.RABID_MEAT)),"Cured cow still drops infected meat");
        } finally {player.discard();}ctx.complete();
    }
}
