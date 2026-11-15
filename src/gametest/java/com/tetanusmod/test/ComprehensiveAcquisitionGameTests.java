package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.expansion.*;
import com.tetanusmod.registry.*;
import com.tetanusmod.rust.*;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.*;
import net.minecraft.potion.*;
import net.minecraft.registry.Registries;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import java.util.*;

/** Exercise acquisition paths that are deliberately not crafting recipes. Creative setup supplies inputs only. */
public class ComprehensiveAcquisitionGameTests implements FabricGameTest {
    /** Remove only this fixture actor; other concurrently running tests may own registered players. */
    private static void cleanupPlayer(net.minecraft.server.network.ServerPlayerEntity player){
        var manager=player.getServer().getPlayerManager();
        try{player.closeHandledScreen();}
        finally{
            try{if(manager.getPlayerList().stream().anyMatch(registered->registered==player))manager.remove(player);}
            finally{player.discard();}
        }
        if(manager.getPlayerList().stream().anyMatch(registered->registered==player))throw new IllegalStateException("Completed fixture actor remains registered in PlayerManager");
    }
    private int count(net.minecraft.entity.player.PlayerEntity player,Item item){int count=0;for(int i=0;i<player.getInventory().size();i++)if(player.getInventory().getStack(i).isOf(item))count+=player.getInventory().getStack(i).getCount();return count;}
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="release_acquisition")
    public void everyRegisteredItemHasAnExplicitAcquisitionRoute(TestContext ctx)throws Exception{
        var recipes=new HashMap<Item,List<String>>();
        for(var recipe:ctx.getWorld().getRecipeManager().values())if(recipe.getId().getNamespace().equals(TetanusMod.MOD_ID)){
            var output=recipe.getOutput(ctx.getWorld().getRegistryManager());
            if(!output.isEmpty())recipes.computeIfAbsent(output.getItem(),key->new ArrayList<>()).add(recipe.getId().toString());
        }
        var special=Map.of(ModItems.RUST,"wire-brush byproduct; permanent hostile salvage",ModItems.RUSTY_FISHING_HOOK,"15% fishing-rod water salvage; fisherman trade",ModItems.COW_MANURE,"glass-bottle interaction with cow",ModItems.TETANIZED_WITHER_STAR,"guaranteed Tetanized Wither drop",ModItems.INFECTED_MEAT,"permanent Tetanized animal drop",ExpansionItems.RABID_MEAT,"Rabid animal drop",ExpansionItems.RABIES_CURE,"randomized medic treatment trade",com.tetanusmod.dragon.DragonContent.EGG_ITEM,"guaranteed Tetanized Dragon drop",com.tetanusmod.dragon.DragonContent.SWORD,"guaranteed Tetanized Dragon drop");
        var rows=new TreeMap<String,String>();int eggs=0,legacy=0;
        for(var item:ModItems.ALL){
            String route=null;
            if(item instanceof VariantSpawnEggItem){route="Creative spawn-egg tab; intentionally no Survival recipe";eggs++;}
            else if(ModIronTools.isRusted(item)){route="retired compatibility item; existing worlds and operator give only";legacy++;}
            else if(recipes.containsKey(item)){route="recipe: "+String.join(", ",recipes.get(item));}
            else if(special.containsKey(item)){route=special.get(item);}
            else{var clean=RustingSystem.clean(item);if(clean!=null&&RustingSystem.rusted(clean)==item)route="weathering: "+Registries.ITEM.getId(clean)+" in water/rain";}
            ctx.assertTrue(route!=null,"Unclassified or inaccessible registered item: "+Registries.ITEM.getId(item));
            rows.put(Registries.ITEM.getId(item).toString(),route);
        }
        long registered=Registries.ITEM.stream().filter(item->Registries.ITEM.getId(item).getNamespace().equals(TetanusMod.MOD_ID)).count();
        ctx.assertTrue(rows.size()==registered,"ModItems.ALL omits a registered mod item");ctx.assertTrue(legacy==5&&eggs>90,"Compatibility/Creative inventory changed unexpectedly");
        var report=new StringBuilder("id\tacquisition\n");rows.forEach((id,route)->report.append(id).append('\t').append(route).append('\n'));
        java.nio.file.Files.writeString(java.nio.file.Path.of("..","acquisition-items-2026-10-24.tsv"),report,java.nio.charset.StandardCharsets.UTF_8);ctx.complete();
    }
    @GameTest(templateName="tetanusmod:test/camp_empty",batchId="release_acquisition")
    public void everyCreativeVariantEggActuallySpawnsItsSpeciesAndMutation(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();var pos=ctx.getAbsolutePos(new BlockPos(16,10,16));int total=0;
        var area=new Box(pos).expand(8);
        try{
            for(var item:ModItems.ALL)if(item instanceof VariantSpawnEggItem egg){
                var existing=ctx.getWorld().getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class,area,mob->true);
                var before=new HashSet<UUID>();existing.forEach(mob->before.add(mob.getUuid()));
                String existingRows=eggFixtureEntities(existing);
                try{
                    var stack=new ItemStack(item);ctx.assertTrue(egg.spawn(ctx.getWorld(),player,stack,pos),"Variant egg failed to spawn: "+Registries.ITEM.getId(item));
                    var nearby=ctx.getWorld().getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class,area,mob->true);
                    var spawned=nearby.stream().filter(mob->mob.getType()==egg.entityType()&&!before.contains(mob.getUuid())).toList();
                    ctx.assertTrue(spawned.size()==1,"Egg did not produce exactly one NEW matching species: "+Registries.ITEM.getId(item)
                            +" at="+pos+" newMatching="+spawned.size()+" chunkTicking="+ctx.getWorld().getChunkManager().isTickingFutureReady(new ChunkPos(pos).toLong())
                            +" preexisting="+existingRows+" newlyVisible="+eggFixtureEntities(nearby.stream().filter(mob->!before.contains(mob.getUuid())).toList()));
                    var mob=spawned.get(0);
                    ctx.assertTrue(egg.rabid()?Rabies.rabid(mob):MobVariants.permanent(mob),"Egg produced an ordinary creature: "+Registries.ITEM.getId(item));total++;
                }finally{
                    // A previous failure or another fixture may own nearby mobs. Remove only this egg's new UUIDs,
                    // including newly initialized jockey vehicles, even when the assertion fails.
                    ctx.getWorld().getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class,area,mob->!before.contains(mob.getUuid())).forEach(Entity::discard);
                }
            }
            ctx.assertTrue(total>90,"Creative egg coverage was unexpectedly small");
        }finally{cleanupPlayer(player);}ctx.complete();
    }
    private static String eggFixtureEntities(List<? extends Entity> entities){
        return "count="+entities.size()+" "+entities.stream().limit(24).map(entity->Registries.ENTITY_TYPE.getId(entity.getType())+"/"+entity.getUuid()+"@"+entity.getPos()).toList();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="release_acquisition",tickLimit=180)
    public void allNineWeatheringOutputsAreActuallyProducedAndBrushed(TestContext ctx){
        Item[] inputs={ModItems.NAIL,ModItems.BENT_NAIL,ModItems.REBAR,ModItems.SHARPENED_REBAR,ModItems.METAL_PIPE,ModBlocks.CALTROPS.asItem(),ModBlocks.BARBED_WIRE.asItem(),ExpansionItems.SPEAR,ExpansionBlocks.BEAR_TRAP.asItem()};
        var drops=new ArrayList<ItemEntity>();
        for(int i=0;i<inputs.length;i++){
            var relative=new BlockPos(1+(i%3)*3,3,1+(i/3)*3);
            ctx.setBlockState(relative.down(),Blocks.STONE);
            for(int height=0;height<5;height++){
                for(var side:net.minecraft.util.math.Direction.Type.HORIZONTAL)ctx.setBlockState(relative.up(height).offset(side),Blocks.GLASS);
                ctx.setBlockState(relative.up(height),Blocks.WATER);
            }
            ctx.setBlockState(relative.up(5),Blocks.GLASS);
            var pos=ctx.getAbsolutePos(relative);var stack=new ItemStack(inputs[i]);stack.setCustomName(net.minecraft.text.Text.literal("weathered specimen "+i));
            var entity=new ItemEntity(ctx.getWorld(),pos.getX()+0.5,pos.getY()+0.3,pos.getZ()+0.5,stack);entity.setNoGravity(true);entity.setVelocity(Vec3d.ZERO);entity.setPickupDelay(32767);ctx.getWorld().spawnEntity(entity);drops.add(entity);
        }
        ctx.runAtTick(120,()->{
            var player=ctx.createMockCreativeServerPlayerInWorld();player.getAbilities().creativeMode=false;player.setPosition(ctx.getAbsolute(new Vec3d(13,6,13)));
            try{
                player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.WIRE_BRUSH));
                for(int i=0;i<inputs.length;i++){
                    var entity=drops.get(i);var expected=RustingSystem.rusted(inputs[i]);ctx.assertTrue(entity.isAlive()&&entity.getStack().isOf(expected),"Water did not produce "+Registries.ITEM.getId(expected)+"; actual="+entity.getStack()+" at "+entity.getPos()+" age="+entity.age);
                    player.setStackInHand(Hand.OFF_HAND,entity.getStack().copy());entity.discard();
                    ModItems.WIRE_BRUSH.use(ctx.getWorld(),player,Hand.MAIN_HAND);
                    ctx.assertTrue(player.getOffHandStack().isOf(inputs[i])&&player.getOffHandStack().getName().getString().equals("weathered specimen "+i),"Brush failed to restore the acquired item and its name");
                }
                ctx.assertTrue(count(player,ModItems.RUST)==9&&player.getMainHandStack().getDamage()==9,"Nine conversions did not yield nine Rust at nine brush durability");
            }finally{cleanupPlayer(player);drops.forEach(Entity::discard);}
            ctx.complete();
        });
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="release_acquisition")
    public void cowBottleInteractionActuallyCollectsManure(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();player.getAbilities().creativeMode=false;
        try{
            var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(3,4,3));player.setPosition(cow.getPos());player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE,2));
            var result=UseEntityCallback.EVENT.invoker().interact(player,ctx.getWorld(),Hand.MAIN_HAND,cow,null);
            ctx.assertTrue(result.isAccepted()&&count(player,ModItems.COW_MANURE)==1&&player.getMainHandStack().getCount()==1,"Cow interaction did not consume one bottle and collect manure");
            UseEntityCallback.EVENT.invoker().interact(player,ctx.getWorld(),Hand.MAIN_HAND,cow,null);
            ctx.assertTrue(count(player,ModItems.COW_MANURE)==1&&player.getMainHandStack().getCount()==1,"Immediate collection bypasses the cow cooldown");cow.discard();
        }finally{cleanupPlayer(player);}ctx.complete();
    }
    private ItemStack brew(TestContext ctx,BrewingStandBlockEntity stand,BlockPos pos,ItemStack input,Item ingredient,Potion expected,Item bottle){
        stand.clear();stand.setStack(0,input);stand.setStack(3,new ItemStack(ingredient));stand.setStack(4,new ItemStack(Items.BLAZE_POWDER));
        for(int tick=0;tick<402;tick++)BrewingStandBlockEntity.tick(ctx.getWorld(),pos,ctx.getWorld().getBlockState(pos),stand);
        var output=stand.removeStack(0);ctx.assertTrue(output.isOf(bottle)&&PotionUtil.getPotion(output)==expected,"Native brewing did not produce "+Registries.POTION.getId(expected)+" in "+Registries.ITEM.getId(bottle));
        ctx.assertTrue(stand.getStack(3).isEmpty()||stand.getStack(3).isOf(Items.GLASS_BOTTLE),"Brewing did not consume its ingredient");return output;
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="release_acquisition")
    public void allFivePotionChainsAndTheirSplashLingeringFormsActuallyBrew(TestContext ctx){
        var relative=new BlockPos(3,3,3);ctx.setBlockState(relative,Blocks.BREWING_STAND);var pos=ctx.getAbsolutePos(relative);var stand=(BrewingStandBlockEntity)ctx.getBlockEntity(relative);
        var outputs=new ArrayList<ItemStack>();
        for(var potion:new Potion[]{ModBrewing.MANURE,ModBrewing.TETANUS,ModBrewing.SALMONELLA,ModBrewing.LOCKJAW,ModBrewing.RABIES}){
            ItemStack drink=brew(ctx,stand,pos,PotionUtil.setPotion(new ItemStack(Items.POTION),Potions.WATER),ModItems.COW_MANURE,ModBrewing.MANURE,Items.POTION);
            if(potion==ModBrewing.TETANUS||potion==ModBrewing.LOCKJAW)drink=brew(ctx,stand,pos,drink,ModItems.RUST,ModBrewing.TETANUS,Items.POTION);
            else if(potion==ModBrewing.SALMONELLA)drink=brew(ctx,stand,pos,drink,ModItems.INFECTED_MEAT,potion,Items.POTION);
            else if(potion==ModBrewing.RABIES)drink=brew(ctx,stand,pos,drink,ExpansionItems.RABID_MEAT,potion,Items.POTION);
            if(potion==ModBrewing.LOCKJAW)drink=brew(ctx,stand,pos,drink,Items.PUFFERFISH,potion,Items.POTION);
            outputs.add(drink.copy());var splash=brew(ctx,stand,pos,drink,Items.GUNPOWDER,potion,Items.SPLASH_POTION);outputs.add(splash.copy());outputs.add(brew(ctx,stand,pos,splash,Items.DRAGON_BREATH,potion,Items.LINGERING_POTION));
        }
        ctx.assertTrue(outputs.size()==15,"Not every potion form was brewed");ctx.complete();
    }
    private List<ItemEntity> drops(TestContext ctx,Vec3d pos){return ctx.getWorld().getEntitiesByClass(ItemEntity.class,new Box(pos,pos).expand(4),entity->true);}
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="release_acquisition")
    public void actualMutantDeathsProduceBothRawMeatsAndHostileSalvage(TestContext ctx){
        var pos=ctx.getAbsolute(new Vec3d(4,5,4));
        for(boolean rabid:new boolean[]{false,true}){
            var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(4,5,4));if(rabid)Rabies.makeRabid(cow);else MobVariants.makePermanent(cow);cow.getRandom().setSeed(541);cow.damage(ctx.getWorld().getDamageSources().genericKill(),10000);
            var meat=rabid?ExpansionItems.RABID_MEAT:ModItems.INFECTED_MEAT;var loot=drops(ctx,pos);int amount=loot.stream().filter(entity->entity.getStack().isOf(meat)).mapToInt(entity->entity.getStack().getCount()).sum();
            ctx.assertTrue(amount>=1&&amount<=3&&loot.stream().allMatch(entity->entity.getStack().isOf(meat)),"Actual animal death did not produce only its infected meat");loot.forEach(Entity::discard);cow.discard();
        }
        var salvage=new HashSet<Item>();
        for(int seed=0;seed<32;seed++){
            var zombie=ctx.spawnEntity(EntityType.ZOMBIE,new Vec3d(4,5,4));MobVariants.makePermanent(zombie);zombie.getRandom().setSeed(seed*359L+19);zombie.damage(ctx.getWorld().getDamageSources().genericKill(),10000);
            var loot=drops(ctx,pos);ctx.assertTrue(loot.stream().anyMatch(entity->entity.getStack().isOf(ModItems.RUST)),"Actual hostile death omitted Rust");loot.forEach(entity->salvage.add(entity.getStack().getItem()));loot.forEach(Entity::discard);zombie.discard();
        }
        for(var item:new Item[]{Items.IRON_NUGGET,Items.COPPER_INGOT,Items.FLINT,Items.STRING,Items.COAL,Items.BONE})ctx.assertTrue(salvage.contains(item),"Fixed death sample never acquired salvage "+item);ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="release_acquisition")
    public void actualWitherDeathsYieldOneReplacementBookAndStar(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();var pos=ctx.getAbsolute(new Vec3d(4,6,4));int levelTwo=0;var bonus=new HashSet<Item>();
        try{
            player.setPosition(ctx.getAbsolute(new Vec3d(13,6,13)));
            for(int seed=0;seed<32;seed++){
                var wither=EntityType.WITHER.create(ctx.getWorld());wither.setPosition(pos);wither.setAiDisabled(true);ctx.getWorld().spawnEntity(wither);MobVariants.makePermanent(wither);wither.getRandom().setSeed(seed*359L+19);
                wither.damage(ctx.getWorld().getDamageSources().playerAttack(player),10000);var loot=drops(ctx,pos);
                ctx.assertTrue(loot.stream().filter(entity->entity.getStack().isOf(ModItems.TETANIZED_WITHER_STAR)).mapToInt(entity->entity.getStack().getCount()).sum()==1,"Tetanized Wither did not drop one corrupted star");
                ctx.assertTrue(loot.stream().noneMatch(entity->entity.getStack().isOf(Items.NETHER_STAR)),"Tetanized Wither also dropped an ordinary star");
                ctx.assertTrue(loot.stream().filter(entity->EnchantmentHelper.get(entity.getStack()).getOrDefault(ModItems.TETANUS_ENCHANTMENT,0)>0).mapToInt(entity->entity.getStack().getCount()).sum()==1,"Wither must drop exactly one level-I-or-II book");
                if(loot.stream().anyMatch(entity->EnchantmentHelper.get(entity.getStack()).getOrDefault(ModItems.TETANUS_ENCHANTMENT,0)==2))levelTwo++;
                loot.forEach(entity->bonus.add(entity.getStack().getItem()));loot.forEach(Entity::discard);wither.discard();
            }
            ctx.assertTrue(levelTwo>0&&levelTwo<32,"Conditional Tetanus II loot never varied across fixed seeds");
            for(var item:new Item[]{Items.DIAMOND,Items.NETHERITE_SCRAP,Items.GOLDEN_APPLE,Items.EMERALD,Items.EXPERIENCE_BOTTLE})ctx.assertTrue(bonus.contains(item),"Fixed Wither death sample omitted bonus "+item);
        }finally{cleanupPlayer(player);}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="release_acquisition")
    public void successfulFishingSalvageReallyDropsTheHook(TestContext ctx){
        var pos=ctx.getAbsolute(new Vec3d(4,4,4));var rod=new ItemEntity(ctx.getWorld(),pos.x,pos.y,pos.z,new ItemStack(Items.FISHING_ROD));ctx.getWorld().spawnEntity(rod);
        ctx.assertTrue(FishingSalvage.breakRod(rod,0.149F)&&!rod.isAlive(),"Successful 15 percent branch failed to consume the actual rod");var loot=drops(ctx,pos);
        ctx.assertTrue(loot.stream().filter(entity->entity.getStack().isOf(ModItems.RUSTY_FISHING_HOOK)).mapToInt(entity->entity.getStack().getCount()).sum()==1,"Salvage did not actually spawn a hook");
        ctx.assertTrue(loot.stream().filter(entity->entity.getStack().isOf(Items.STICK)).mapToInt(entity->entity.getStack().getCount()).sum()==2&&loot.stream().filter(entity->entity.getStack().isOf(Items.STRING)).mapToInt(entity->entity.getStack().getCount()).sum()==2,"Rod salvage omitted sticks/string");loot.forEach(Entity::discard);ctx.complete();
    }
    private void acquireTrade(TestContext ctx,java.util.function.Predicate<ItemStack> wanted){
        var player=ctx.createMockCreativeServerPlayerInWorld();VillagerEntity selected=null;int index=-1;
        try{
            player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);player.setSneaking(false);
            for(int seed=0;seed<64&&selected==null;seed++){
                var medic=EntityType.VILLAGER.create(ctx.getWorld());medic.setVillagerData(medic.getVillagerData().withProfession(MedicVillage.MEDIC).withLevel(4));medic.getRandom().setSeed(seed*359L+19);
                for(int i=0;i<medic.getOffers().size();i++)if(wanted.test(medic.getOffers().get(i).getSellItem())){selected=medic;index=i;break;}
            }
            ctx.assertTrue(selected!=null,"Trade item never appears in randomized medic stock");selected.setPosition(player.getPos());selected.interactMob(player,Hand.MAIN_HAND);
            ctx.assertTrue(player.currentScreenHandler instanceof MerchantScreenHandler,"Native merchant menu did not open");var handler=(MerchantScreenHandler)player.currentScreenHandler;handler.setRecipeIndex(index);var offer=selected.getOffers().get(index);
            handler.getSlot(0).setStack(offer.getAdjustedFirstBuyItem().copy());handler.getSlot(1).setStack(offer.getSecondBuyItem().copy());
            ctx.assertTrue(wanted.test(handler.getSlot(2).getStack()),"Paid native merchant output does not contain the desired item");handler.quickMove(player,2);
            boolean acquired=false;for(int i=0;i<player.getInventory().size();i++)if(wanted.test(player.getInventory().getStack(i)))acquired=true;
            ctx.assertTrue(acquired&&offer.getUses()==1,"Native merchant result was not acquired or did not consume one use");
            ctx.assertTrue(handler.getSlot(0).getStack().isEmpty()&&handler.getSlot(1).getStack().isEmpty(),"Trade failed to consume exact supplied payment");
        }finally{player.closeHandledScreen();if(selected!=null)selected.setCustomer(null);cleanupPlayer(player);}
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="release_acquisition")
    public void rabiesCureCanActuallyBeBoughtThroughTheNativeMerchant(TestContext ctx){acquireTrade(ctx,stack->stack.isOf(ExpansionItems.RABIES_CURE));ctx.complete();}
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="release_acquisition")
    public void shieldProtectionBookCanActuallyBeBoughtThroughTheNativeMerchant(TestContext ctx){acquireTrade(ctx,stack->stack.isOf(Items.ENCHANTED_BOOK)&&EnchantmentHelper.get(stack).getOrDefault(ExpansionItems.TETANUS_PROTECTION,0)==1);ctx.complete();}
}
