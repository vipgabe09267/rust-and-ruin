package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.beacon.*;
import com.tetanusmod.effect.*;
import com.tetanusmod.expansion.*;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.registry.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.*;
import net.minecraft.registry.*;
import net.minecraft.screen.*;
import net.minecraft.test.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.util.*;

public class ProgressionExpansionGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void smithingConsumesOneRustAndEntireArrowBatch(TestContext ctx){
        for(int count:new int[]{1,16,64}){
            var player=ctx.createMockSurvivalPlayer();var menu=new SmithingScreenHandler(1,player.getInventory());
            menu.getSlot(1).setStack(new ItemStack(Items.ARROW,count));menu.getSlot(2).setStack(new ItemStack(ModItems.RUST,3));
            var output=menu.getSlot(3).getStack();ctx.assertTrue(output.isOf(ModItems.TETANIZED_ARROW)&&output.getCount()==count,"Wrong smithing batch output "+count);
            var taken=menu.getSlot(3).takeStack(count);menu.getSlot(3).onTakeItem(player,taken);
            ctx.assertTrue(menu.getSlot(1).getStack().isEmpty()&&menu.getSlot(2).getStack().getCount()==2&&menu.getSlot(3).getStack().isEmpty(),"Batch duplicated arrows or consumed extra Rust");
        }
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void smithingShiftClickCannotDuplicateBatch(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();var menu=new SmithingScreenHandler(1,player.getInventory());menu.getSlot(1).setStack(new ItemStack(Items.ARROW,64));menu.getSlot(2).setStack(new ItemStack(ModItems.RUST,2));
        menu.quickMove(player,3);ctx.assertTrue(player.getInventory().count(ModItems.TETANIZED_ARROW)==64&&menu.getSlot(1).getStack().isEmpty()&&menu.getSlot(2).getStack().getCount()==1,"Shift-click batch duplication or loss");
        menu.quickMove(player,3);ctx.assertTrue(player.getInventory().count(ModItems.TETANIZED_ARROW)==64,"Empty output could be claimed again");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void smithingRequiresCorrectInputsAndNoTemplate(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();var menu=new SmithingScreenHandler(1,player.getInventory());menu.getSlot(1).setStack(new ItemStack(Items.ARROW,64));ctx.assertTrue(menu.getSlot(3).getStack().isEmpty(),"Arrows smith without Rust");
        menu.getSlot(2).setStack(new ItemStack(ModItems.RUST));menu.getSlot(0).setStack(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));ctx.assertTrue(menu.getSlot(3).getStack().isEmpty(),"Nonempty template was accepted");
        menu.getSlot(0).setStack(ItemStack.EMPTY);menu.getSlot(1).setStack(new ItemStack(Items.SPECTRAL_ARROW));ctx.assertTrue(menu.getSlot(3).getStack().isEmpty(),"Wrong arrow type was accepted");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void gloveSlotEquipsShiftMovesAndPreservesNbt(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();var menu=player.playerScreenHandler;var inventory=((GloveInventory)player).rustruin$gloves();
        ctx.assertTrue(menu.slots.size()==47&&menu.getSlot(46).canInsert(new ItemStack(ModItems.PROTECTIVE_GLOVES))&&!menu.getSlot(46).canInsert(new ItemStack(Items.DIAMOND)),"Dedicated glove slot is missing or accepts wrong items");
        var gloves=new ItemStack(ModItems.PROTECTIVE_GLOVES);gloves.setDamage(27);player.setStackInHand(Hand.MAIN_HAND,gloves);ModItems.PROTECTIVE_GLOVES.use(ctx.getWorld(),player,Hand.MAIN_HAND);
        ctx.assertTrue(player.getMainHandStack().isEmpty()&&inventory.getStack(0).getDamage()==27,"Right-click failed to equip gloves");
        menu.quickMove(player,46);ctx.assertTrue(inventory.isEmpty()&&player.getInventory().count(ModItems.PROTECTIVE_GLOVES)==1,"Shift removal lost gloves");
        int slot=-1;for(int i=9;i<45;i++)if(menu.getSlot(i).getStack().isOf(ModItems.PROTECTIVE_GLOVES))slot=i;ctx.assertTrue(slot>=0,"Removed glove item missing");menu.quickMove(player,slot);
        ctx.assertTrue(inventory.getStack(0).getDamage()==27&&player.getInventory().count(ModItems.PROTECTIVE_GLOVES)==0,"Shift equip duplicated or reset gloves");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void legacyOffhandGlovesMigrateToDedicatedSlot(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();var gloves=new ItemStack(ModItems.PROTECTIVE_GLOVES);gloves.setDamage(19);player.setStackInHand(Hand.OFF_HAND,gloves);
        var nbt=player.writeNbt(new NbtCompound());nbt.remove("RustRuinGloves");var copy=ctx.createMockSurvivalPlayer();copy.readNbt(nbt);
        ctx.assertTrue(copy.getOffHandStack().isEmpty()&&GloveInventory.equipped(copy).isOf(ModItems.PROTECTIVE_GLOVES)&&GloveInventory.equipped(copy).getDamage()==19,"Old glove save did not migrate safely");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void gloveDeathDropsExactlyOnceAndKeepInventoryCopies(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();player.changeGameMode(GameMode.SURVIVAL);player.getAbilities().creativeMode=false;player.getAbilities().invulnerable=false;player.setPosition(ctx.getAbsolute(new Vec3d(3,4,3)));((GloveInventory)player).rustruin$gloves().setStack(0,new ItemStack(ModItems.PROTECTIVE_GLOVES));
        var replacement=ctx.createMockCreativeServerPlayerInWorld();replacement.copyFrom(player,true);ctx.assertTrue(GloveInventory.equipped(replacement).isOf(ModItems.PROTECTIVE_GLOVES),"Alive/keep-inventory respawn lost gloves");replacement.discard();
        var rule=ctx.getWorld().getGameRules().get(GameRules.KEEP_INVENTORY);boolean original=rule.get();try{rule.set(false,ctx.getWorld().getServer());player.damage(ctx.getWorld().getDamageSources().genericKill(),10000);var drops=ctx.getWorld().getEntitiesByClass(ItemEntity.class,player.getBoundingBox().expand(2),item->item.getStack().isOf(ModItems.PROTECTIVE_GLOVES));ctx.assertTrue(drops.stream().mapToInt(item->item.getStack().getCount()).sum()==1&&GloveInventory.equipped(player).isEmpty(),"Glove death drop duplicated or lost item");drops.forEach(Entity::discard);}finally{rule.set(original,ctx.getWorld().getServer());player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void bearTrapSnapsOnceAndResetRearmsIt(TestContext ctx){
        var pos=new BlockPos(3,3,3);ctx.setBlockState(pos,ExpansionBlocks.RUSTED_BEAR_TRAP);var trap=(BearTrapBlockEntity)ctx.getBlockEntity(pos);var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(3.5,3,3.5));float chance=TetanusMod.CONFIG.animalConversionChance;
        try{TetanusMod.CONFIG.animalConversionChance=0;ctx.assertTrue(trap.trigger(cow),"Open trap did not trigger");ctx.assertTrue(cow.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier()==9&&cow.hasStatusEffect(TetanusMod.TETANUS)&&cow.getHealth()==7,"Rusty bear trap did not immobilize/damage/infect");cow.timeUntilRegen=0;ctx.assertFalse(trap.trigger(cow),"Closed trap triggered again");ctx.assertTrue(cow.getHealth()==7,"Closed trap repeated damage");ctx.assertTrue(trap.reset()&&!cow.hasStatusEffect(StatusEffects.SLOWNESS),"Reset did not release victim");ctx.assertFalse(trap.trigger(cow),"Reset recaptured the victim before its escape grace");for(int tick=0;tick<BearTrapBlockEntity.RESET_GRACE_TICKS;tick++)BearTrapBlockEntity.tick(ctx.getWorld(),trap.getPos(),trap.getCachedState(),trap);ctx.assertTrue(trap.trigger(cow),"Reset failed to rearm after its grace");}finally{TetanusMod.CONFIG.animalConversionChance=chance;}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void resetToolCostsDurabilityAndAwardsAdvancement(TestContext ctx){
        var pos=new BlockPos(3,3,3);ctx.setBlockState(pos,ExpansionBlocks.BEAR_TRAP);var trap=(BearTrapBlockEntity)ctx.getBlockEntity(pos);trap.trigger(ctx.spawnEntity(EntityType.COW,new Vec3d(3.5,3,3.5)));
        var player=ctx.createMockCreativeServerPlayerInWorld();player.changeGameMode(GameMode.SURVIVAL);player.getAbilities().creativeMode=false;player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ExpansionItems.TRAP_RESET_TOOL));
        var hit=new net.minecraft.util.hit.BlockHitResult(Vec3d.ofCenter(ctx.getAbsolutePos(pos)),Direction.UP,ctx.getAbsolutePos(pos),false);ExpansionItems.TRAP_RESET_TOOL.useOnBlock(new ItemUsageContext(player,Hand.MAIN_HAND,hit));
        ctx.assertTrue(!ctx.getWorld().getBlockState(ctx.getAbsolutePos(pos)).get(BearTrapBlock.CLOSED)&&player.getMainHandStack().getDamage()==1,"Reset tool failed or charged incorrect durability");
        var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id("trap_reset"));ctx.assertTrue(advancement!=null&&player.getAdvancementTracker().getProgress(advancement).isDone(),"Reset advancement missing");player.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rustPileSupportsFloorsAndWalls(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUST,3));var floor=new BlockPos(3,2,3);ctx.setBlockState(floor,Blocks.STONE);
        var hit=new net.minecraft.util.hit.BlockHitResult(Vec3d.ofCenter(ctx.getAbsolutePos(floor)).add(0,0.5,0),Direction.UP,ctx.getAbsolutePos(floor),false);ModItems.RUST.useOnBlock(new ItemUsageContext(player,Hand.MAIN_HAND,hit));
        ctx.assertTrue(ctx.getWorld().getBlockState(ctx.getAbsolutePos(floor.up())).isOf(ExpansionBlocks.RUST_PILE),"Rust did not place on floor");
        var wall=new BlockPos(5,3,3);ctx.setBlockState(wall,Blocks.STONE);hit=new net.minecraft.util.hit.BlockHitResult(Vec3d.ofCenter(ctx.getAbsolutePos(wall)).add(0,0,-0.5),Direction.NORTH,ctx.getAbsolutePos(wall),false);ModItems.RUST.useOnBlock(new ItemUsageContext(player,Hand.MAIN_HAND,hit));
        var placed=ctx.getWorld().getBlockState(ctx.getAbsolutePos(wall.north()));ctx.assertTrue(placed.isOf(ExpansionBlocks.RUST_PILE)&&placed.get(RustPileBlock.FACING)==Direction.NORTH&&player.getMainHandStack().getCount()==1,"Rust wall placement or consumption failed");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rustyIronBlockAndSpearRecipesAreDistinct(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();var grid=new net.minecraft.inventory.CraftingInventory(new CraftingScreenHandler(1,player.getInventory()),3,3);
        grid.setStack(4,new ItemStack(Items.IRON_BLOCK));for(int slot:new int[]{1,3,5,7})grid.setStack(slot,new ItemStack(ModItems.RUST));var recipe=ctx.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING,grid,ctx.getWorld()).orElseThrow();ctx.assertTrue(recipe.craft(grid,ctx.getWorld().getRegistryManager()).isOf(ExpansionBlocks.RUSTED_IRON_BLOCK.asItem()),"Rusty iron block recipe failed");
        grid.clear();for(int slot:new int[]{1,2,5})grid.setStack(slot,new ItemStack(Items.IRON_INGOT));grid.setStack(4,new ItemStack(Items.CHAIN));grid.setStack(6,new ItemStack(Items.STICK));recipe=ctx.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING,grid,ctx.getWorld()).orElseThrow();ctx.assertTrue(recipe.craft(grid,ctx.getWorld().getRegistryManager()).isOf(ExpansionItems.SPEAR),"Spear recipe failed");ctx.assertTrue(RustingSystemHelper.spearPair(),"Spear cannot rust/clean");ctx.complete();
    }
    private static class RustingSystemHelper{static boolean spearPair(){return com.tetanusmod.rust.RustingSystem.rusted(ExpansionItems.SPEAR)==ExpansionItems.RUSTED_SPEAR&&com.tetanusmod.rust.RustingSystem.clean(ExpansionItems.RUSTED_SPEAR)==ExpansionItems.SPEAR;}}
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void spearThrustHasReachAndRespectsWalls(TestContext ctx){
        for(int x=2;x<=4;x++)for(int y=3;y<=5;y++)for(int z=1;z<=6;z++)ctx.setBlockState(x,y,z,Blocks.AIR);
        var player=ctx.createMockSurvivalPlayer();player.setPosition(ctx.getAbsolute(new Vec3d(3.5,3,1.5)));player.setYaw(0);player.setPitch(0);player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ExpansionItems.SPEAR));
        var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(3.5,3.5,5.5));cow.setAiDisabled(true);cow.setNoGravity(true);
        ctx.runAtTick(2,()->{ExpansionItems.SPEAR.use(ctx.getWorld(),player,Hand.MAIN_HAND);ctx.assertTrue(cow.getHealth()==3&&player.getMainHandStack().getDamage()==1,"Spear reach/damage failed");player.getItemCooldownManager().remove(ExpansionItems.SPEAR);cow.setHealth(10);cow.timeUntilRegen=0;ctx.setBlockState(3,4,3,Blocks.STONE);ExpansionItems.SPEAR.use(ctx.getWorld(),player,Hand.MAIN_HAND);ctx.assertTrue(cow.getHealth()==10&&player.getMainHandStack().getDamage()==1,"Spear thrust penetrated wall");ctx.complete();});
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void noviceMedicHasLimitedRandomizedOffers(TestContext ctx){
        var medic=EntityType.VILLAGER.create(ctx.getWorld());medic.setVillagerData(medic.getVillagerData().withProfession(MedicVillage.MEDIC).withLevel(1));var offers=medic.getOffers();
        ctx.assertTrue(offers.size()==2,"Nurse should offer two randomized trades, not every medicine");ctx.assertTrue(offers.stream().filter(offer->MedicTradeOffers.isMedicine(offer.getSellItem().getItem())).count()==1,"Nurse needs one medicine and one supply trade");
        ctx.assertTrue(MedicVillage.JOB.contains(ExpansionBlocks.MEDIC_CABINET.getDefaultState()),"Medicine Cabinet is not a medic job site");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=220)
    public void clinicStructureLoadsPlacesAndContainsMedic(TestContext ctx){
        var template=ctx.getWorld().getStructureTemplateManager().getTemplate(TetanusMod.id("village/clinic")).orElseThrow();ctx.assertTrue(template.getSize().equals(new Vec3i(9,11,13)),"Clinic template missing or wrong size");
        var pos=ctx.getAbsolutePos(new BlockPos(0,2,0));boolean placed=template.place(ctx.getWorld(),pos,pos,new net.minecraft.structure.StructurePlacementData().setIgnoreEntities(false).addProcessor(net.minecraft.structure.processor.JigsawReplacementStructureProcessor.INSTANCE),ctx.getWorld().random,2);ctx.assertTrue(placed,"Clinic structure failed to place");
        ctx.assertTrue(ctx.getWorld().getBlockState(pos.add(6,2,6)).isOf(ExpansionBlocks.MEDIC_CABINET),"Clinic lacks its workstation");
        ctx.assertTrue(ctx.getWorld().getEntitiesByClass(net.minecraft.entity.passive.VillagerEntity.class,new Box(pos,pos.add(9,11,13)),v->v.getVillagerData().getProfession()==MedicVillage.MEDIC).size()==1,"Clinic did not spawn its medic");
        var medic=ctx.getWorld().getEntitiesByClass(net.minecraft.entity.passive.VillagerEntity.class,new Box(pos,pos.add(9,11,13)),v->v.getVillagerData().getProfession()==MedicVillage.MEDIC).get(0);ctx.assertTrue(medic.getExperience()>=1,"Clinic medic can lose its profession before finding workstation");
        ctx.runAtTick(160,()->{ctx.assertTrue(medic.isAlive()&&medic.getVillagerData().getProfession()==MedicVillage.MEDIC,"Clinic medic lost its profession after AI ticks");ctx.complete();});
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void clinicsUseNativeLocatableStructures(TestContext ctx){
        var registry=ctx.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE);
        ctx.assertTrue(registry.get(TetanusMod.id("medic_clinic")) instanceof com.tetanusmod.world.ClinicStructure,"Clinic is not a locatable structure");
        for(String biome:new String[]{"plains","desert","savanna","snowy","taiga"})ctx.assertTrue(ctx.getWorld().getStructureTemplateManager().getTemplate(TetanusMod.id("village/"+biome+"_clinic")).isPresent(),"Missing themed clinic "+biome);ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void naturalAndSpawnerMutationRatesRespectDifficulty(TestContext ctx){
        ctx.assertTrue(NaturalVariants.chance(Difficulty.EASY,false)==0.005F&&NaturalVariants.chance(Difficulty.NORMAL,false)==0.01F&&NaturalVariants.chance(Difficulty.HARD,false)==0.02F,"Natural difficulty defaults wrong");
        ctx.assertTrue(NaturalVariants.chance(Difficulty.EASY,true)==0.03F&&NaturalVariants.chance(Difficulty.NORMAL,true)==0.05F&&NaturalVariants.chance(Difficulty.HARD,true)==0.08F,"Dungeon/spawner defaults wrong");
        var difficulty=ctx.getWorld().getDifficulty();int converted=0;var random=new java.util.Random(103);try{ctx.getWorld().getServer().setDifficulty(Difficulty.NORMAL,true);for(int i=0;i<300;i++){var mob=EntityType.ZOMBIE.create(ctx.getWorld());mob.getRandom().setSeed(random.nextLong());mob.initialize(ctx.getWorld(),ctx.getWorld().getLocalDifficulty(mob.getBlockPos()),SpawnReason.NATURAL,null,null);if(MobVariants.permanent(mob))converted++;}}finally{ctx.getWorld().getServer().setDifficulty(difficulty,true);}ctx.assertTrue(converted>0&&converted<=12,"Natural initialize hook did not apply the seeded rate: "+converted);ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void commandAndEggSpawnsDoNotRollNaturalMutation(TestContext ctx){
        for(var reason:List.of(SpawnReason.COMMAND,SpawnReason.SPAWN_EGG,SpawnReason.BREEDING)){var cow=EntityType.COW.create(ctx.getWorld());cow.initialize(ctx.getWorld(),ctx.getWorld().getLocalDifficulty(cow.getBlockPos()),reason,null,null);ctx.assertTrue(!MobVariants.permanent(cow)&&!Rabies.rabid(cow),"Non-natural spawn unexpectedly rolled mutation: "+reason);}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void variantEggsSpawnTheRequestedVariantAndConsumeOne(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();var item=(VariantSpawnEggItem)Registries.ITEM.get(TetanusMod.id("tetanized_zombie_spawn_egg"));var stack=new ItemStack(item,2);var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));ctx.assertTrue(item.spawn(ctx.getWorld(),player,stack,pos)&&stack.getCount()==1,"Variant egg failed or consumed wrong count");
        ctx.assertTrue(ctx.getWorld().getEntitiesByClass(net.minecraft.entity.mob.ZombieEntity.class,new Box(pos).expand(1),MobVariants::permanent).size()==1,"Tetanized egg spawned ordinary mob");
        var rabid=(VariantSpawnEggItem)Registries.ITEM.get(TetanusMod.id("rabid_cow_spawn_egg"));rabid.spawn(ctx.getWorld(),player,new ItemStack(rabid),pos.add(2,0,0));ctx.assertTrue(ctx.getWorld().getEntitiesByClass(net.minecraft.entity.passive.CowEntity.class,new Box(pos.add(2,0,0)).expand(1),Rabies::rabid).size()==1,"Rabid egg spawned ordinary animal");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void everyModItemHasAModelAndAllNewAdvancementsLoad(TestContext ctx){
        int eggs=0;for(Item item:ModItems.ALL){var id=Registries.ITEM.getId(item);ctx.assertTrue(getClass().getClassLoader().getResource("assets/tetanusmod/models/item/"+id.getPath()+".json")!=null,"Missing item model "+id);if(item instanceof VariantSpawnEggItem)eggs++;}
        ctx.assertTrue(eggs>90,"Too few variant eggs registered: "+eggs);
        for(String id:List.of("why_did_i_do_that","beacon_placed","beacon_full","shield_protection","glove_protection","rabies","rabies_cured","antitoxin","trap_reset","spear","variant_egg","rust_arrows","medic","clinic"))ctx.assertTrue(ctx.getWorld().getServer().getAdvancementLoader().get(TetanusMod.id(id))!=null,"New advancement did not load: "+id);ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void giveSuggestionsFindModItemsWithoutRememberingNamespace(TestContext ctx)throws Exception{
        var player=ctx.createMockCreativeServerPlayerInWorld();var dispatcher=ctx.getWorld().getServer().getCommandManager().getDispatcher();var source=player.getCommandSource().withLevel(2);
        var suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("give @s rusted_iron_spear",source)).get();ctx.assertTrue(suggestions.getList().stream().anyMatch(s->s.getText().equals("tetanusmod:rusted_iron_spear")),"/give did not suggest mod item by path");
        dispatcher.execute("give @s tetanusmod:iron_spear",source);ctx.assertTrue(player.getInventory().contains(new ItemStack(ExpansionItems.SPEAR)),"/give could not obtain registered mod item");player.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void beaconAlwaysGrantsResistanceWithoutSelectedPower(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();player.setPosition(ctx.getAbsolute(new Vec3d(3,4,3)));BeaconPower.apply(ctx.getWorld(),player.getBlockPos(),1,null,null);ctx.assertTrue(player.hasStatusEffect(TetanusMod.VACCINATED),"Active beacon with no selection lacked passive resistance");
        var full=ctx.getWorld().getServer().getAdvancementLoader().get(TetanusMod.id("beacon_full"));ctx.assertFalse(player.getAdvancementTracker().getProgress(full).isDone(),"Incomplete/unselected beacon awarded full activation");
        BeaconPower.apply(ctx.getWorld(),player.getBlockPos(),4,StatusEffects.HASTE,StatusEffects.HASTE);ctx.assertTrue(player.getStatusEffect(StatusEffects.HASTE).getAmplifier()==2&&player.getAdvancementTracker().getProgress(full).isDone(),"Full beacon failed Haste III or its custom advancement");player.discard();ctx.complete();
    }
}
