package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.expansion.RustArrowSmithingRecipe;
import com.tetanusmod.registry.ModItems;
import com.tetanusmod.rust.*;
import java.util.*;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.*;
import net.minecraft.recipe.*;
import net.minecraft.registry.Registries;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

/** Release coverage uses native output slots, survival consumption and real cooking ticks. */
public class ComprehensiveCraftingGameTests implements FabricGameTest {
    private static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger("RustRuinReleaseCrafting");
    private static List<Recipe<?>> recipes(TestContext ctx){
        var all=ctx.getWorld().getRecipeManager().values().stream()
            .filter(recipe->recipe.getId().getNamespace().equals(TetanusMod.MOD_ID))
            .sorted(Comparator.comparing(recipe->recipe.getId().toString())).toList();
        ctx.assertTrue(all.size()>=33,"Recipe resources unexpectedly disappeared: "+all.size());
        for(var recipe:all)ctx.assertTrue(recipe instanceof CraftingRecipe||recipe instanceof AbstractCookingRecipe||recipe instanceof RustArrowSmithingRecipe||recipe instanceof com.tetanusmod.medical.PrescriptionWritingRecipe||recipe instanceof com.tetanusmod.dragon.CrystalForgingRecipe,
            "Release acquisition lacks a native execution path for "+recipe.getId());
        return all;
    }
    // Vanilla's mock keeps isCreative() true. Game mode/abilities are Survival;
    // the assertions below independently require real input, fuel and durability debits.
    // The separate graphical journey uses an ordinary Survival client actor.
    private static ServerPlayerEntity survival(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();player.changeGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();return player;
    }
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
    private static ItemStack ingredient(TestContext ctx,Ingredient ingredient,String recipe){
        var choices=ingredient.getMatchingStacks();ctx.assertTrue(choices.length>0,"Unresolved ingredient in "+recipe);
        return choices[0].copyWithCount(1);
    }
    private static void add(Map<Item,Integer> counts,ItemStack stack){if(!stack.isEmpty())counts.merge(stack.getItem(),stack.getCount(),Integer::sum);}
    private static Map<Item,Integer> inventory(ServerPlayerEntity player){
        var counts=new HashMap<Item,Integer>();for(int i=0;i<player.getInventory().size();i++)add(counts,player.getInventory().getStack(i));return counts;
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="release_crafting")
    public void everyTableRecipeCraftsThroughNativeOutputAndConsumesIngredients(TestContext ctx){
        var player=survival(ctx);var world=ctx.getWorld();var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));world.setBlockState(pos,Blocks.CRAFTING_TABLE.getDefaultState());int covered=0;
        try{
            for(var entry:recipes(ctx))if(entry instanceof CraftingRecipe recipe){
                player.getInventory().clear();var menu=new CraftingScreenHandler(41,player.getInventory(),ScreenHandlerContext.create(world,pos));player.currentScreenHandler=menu;
                var expectedCounts=new HashMap<Item,Integer>();var ingredients=recipe.getIngredients();
                for(int i=0;i<ingredients.size();i++){
                    if(ingredients.get(i).isEmpty())continue;
                    var input=ingredient(ctx,ingredients.get(i),recipe.getId().toString());
                    if(input.getItem().hasRecipeRemainder())add(expectedCounts,new ItemStack(input.getItem().getRecipeRemainder()));
                    int grid=recipe instanceof ShapedRecipe shaped?(i/shaped.getWidth())*3+i%shaped.getWidth():i;
                    menu.getSlot(grid+1).setStack(input);
                }
                var expected=recipe.getOutput(world.getRegistryManager()).copy();ctx.assertTrue(!expected.isEmpty(),"Empty declared result: "+recipe.getId());
                ctx.assertTrue(menu.matches(recipe),"Native grid does not match "+recipe.getId());
                ctx.assertTrue(ItemStack.areEqual(menu.getSlot(0).getStack(),expected),"Native result mismatch: "+recipe.getId()+" / "+menu.getSlot(0).getStack());
                menu.quickMove(player,0);add(expectedCounts,expected);
                var actual=inventory(player);for(int i=1;i<=9;i++)add(actual,menu.getSlot(i).getStack());
                ctx.assertTrue(actual.equals(expectedCounts),"Consumed inputs/container remainders differ for "+recipe.getId()+": "+actual+" expected "+expectedCounts);
                ctx.assertTrue(menu.getSlot(0).getStack().isEmpty(),"One set of ingredients left a duplicate craft: "+recipe.getId());
                ctx.assertTrue(player.getRecipeBook().contains(recipe),"Taking native output did not unlock "+recipe.getId());
                menu.quickMove(player,0);var after=inventory(player);for(int i=1;i<=9;i++)add(after,menu.getSlot(i).getStack());
                ctx.assertTrue(after.equals(expectedCounts),"Claiming an empty craft duplicated items: "+recipe.getId());
                player.currentScreenHandler=player.playerScreenHandler;covered++;LOGGER.info("RELEASE_CRAFT_PASS {} count={}",recipe.getId(),expected.getCount());
            }
            ctx.assertTrue(covered>=22,"Crafting-table recipe coverage unexpectedly fell to "+covered);
        }finally{player.currentScreenHandler=player.playerScreenHandler;cleanupPlayer(player);}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="release_crafting")
    public void everyCookingRecipeProducesPickupThroughNativeBlockTicks(TestContext ctx){
        var player=survival(ctx);var world=ctx.getWorld();var pos=ctx.getAbsolutePos(new BlockPos(4,3,4));int covered=0;
        try{
            for(var entry:recipes(ctx))if(entry instanceof AbstractCookingRecipe recipe){
                player.getInventory().clear();var input=ingredient(ctx,recipe.getIngredients().get(0),recipe.getId().toString());var expected=recipe.getOutput(world.getRegistryManager()).copy();
                ctx.assertTrue(!expected.isEmpty(),"Empty cooking result: "+recipe.getId());
                if(recipe.getType()==RecipeType.CAMPFIRE_COOKING){
                    for(var item:world.getEntitiesByClass(ItemEntity.class,new Box(pos).expand(1),entity->true))item.discard();
                    world.setBlockState(pos,Blocks.CAMPFIRE.getDefaultState());var campfire=(CampfireBlockEntity)world.getBlockEntity(pos);
                    ctx.assertTrue(campfire.getRecipeFor(input).orElseThrow().getId().equals(recipe.getId()),"Campfire selected another recipe: "+recipe.getId());
                    ctx.assertTrue(campfire.addItem(player,input,recipe.getCookTime())&&input.isEmpty(),"Campfire did not consume its raw input: "+recipe.getId());
                    for(int tick=0;tick<=recipe.getCookTime();tick++)CampfireBlockEntity.litServerTick(world,pos,world.getBlockState(pos),campfire);
                    ctx.assertTrue(campfire.getItemsBeingCooked().stream().allMatch(ItemStack::isEmpty),"Campfire did not finish: "+recipe.getId());
                    var drops=world.getEntitiesByClass(ItemEntity.class,new Box(pos).expand(1),entity->entity.getStack().isOf(expected.getItem()));
                    ctx.assertTrue(drops.stream().mapToInt(item->item.getStack().getCount()).sum()==expected.getCount(),"Incorrect cooked campfire drop: "+recipe.getId());
                    for(var item:drops){item.setPickupDelay(0);item.onPlayerCollision(player);}
                }else{
                    Block block=recipe.getType()==RecipeType.SMELTING?Blocks.FURNACE:recipe.getType()==RecipeType.SMOKING?Blocks.SMOKER:recipe.getType()==RecipeType.BLASTING?Blocks.BLAST_FURNACE:null;
                    ctx.assertTrue(block!=null,"Unsupported cooking appliance: "+recipe.getId());world.setBlockState(pos,block.getDefaultState());
                    var furnace=(AbstractFurnaceBlockEntity)world.getBlockEntity(pos);furnace.setStack(0,input);furnace.setStack(1,new ItemStack(Items.COAL));
                    for(int tick=0;tick<=recipe.getCookTime();tick++)AbstractFurnaceBlockEntity.tick(world,pos,world.getBlockState(pos),furnace);
                    ctx.assertTrue(furnace.getStack(0).isEmpty()&&furnace.getStack(1).isEmpty(),"Appliance did not consume raw input and fuel: "+recipe.getId());
                    ctx.assertTrue(ItemStack.areEqual(furnace.getStack(2),expected),"Native cooked output mismatch: "+recipe.getId());
                    var properties=new ArrayPropertyDelegate(4);
                    ScreenHandler menu=recipe.getType()==RecipeType.SMOKING?new SmokerScreenHandler(42,player.getInventory(),furnace,properties):recipe.getType()==RecipeType.BLASTING?new BlastFurnaceScreenHandler(42,player.getInventory(),furnace,properties):new FurnaceScreenHandler(42,player.getInventory(),furnace,properties);
                    player.currentScreenHandler=menu;menu.quickMove(player,2);ctx.assertTrue(furnace.getStack(2).isEmpty(),"Output was not taken from appliance: "+recipe.getId());
                    ctx.assertTrue(player.getRecipeBook().contains(recipe),"Native cooked output did not unlock its recipe: "+recipe.getId());
                }
                ctx.assertTrue(player.getInventory().count(expected.getItem())==expected.getCount(),"Cooked item was not acquired in Survival: "+recipe.getId());
                player.currentScreenHandler=player.playerScreenHandler;world.setBlockState(pos,Blocks.AIR.getDefaultState());covered++;LOGGER.info("RELEASE_COOK_PASS {} count={}",recipe.getId(),expected.getCount());
            }
            ctx.assertTrue(covered>=6,"Cooking recipe coverage unexpectedly fell to "+covered);
        }finally{player.currentScreenHandler=player.playerScreenHandler;world.setBlockState(pos,Blocks.AIR.getDefaultState());cleanupPlayer(player);}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="release_crafting")
    public void everyArrowBatchUsesNativeSmithingConsumptionAndAdvancement(TestContext ctx){
        var player=survival(ctx);var world=ctx.getWorld();int covered=0;
        try{
            for(var recipe:recipes(ctx))if(recipe instanceof RustArrowSmithingRecipe){
                for(int batch:new int[]{1,16,64}){
                    player.getInventory().clear();var menu=new SmithingScreenHandler(43,player.getInventory());player.currentScreenHandler=menu;
                    menu.getSlot(1).setStack(new ItemStack(Items.ARROW,batch));menu.getSlot(2).setStack(new ItemStack(ModItems.RUST,2));
                    ctx.assertTrue(menu.getSlot(3).getStack().isOf(ModItems.TETANIZED_ARROW)&&menu.getSlot(3).getStack().getCount()==batch,"Smithing batch preview differs: "+batch);
                    menu.quickMove(player,3);
                    ctx.assertTrue(player.getInventory().count(ModItems.TETANIZED_ARROW)==batch&&menu.getSlot(1).getStack().isEmpty()&&menu.getSlot(2).getStack().getCount()==1,"Smithing consumption failed: "+batch);
                    menu.quickMove(player,3);ctx.assertTrue(player.getInventory().count(ModItems.TETANIZED_ARROW)==batch,"Native smithing duplicated an empty output");
                    var advancement=world.getServer().getAdvancementLoader().get(TetanusMod.id("rust_arrows"));ctx.assertTrue(player.getAdvancementTracker().getProgress(advancement).isDone(),"Smithing did not award its advancement");
                    player.currentScreenHandler=player.playerScreenHandler;
                }covered++;LOGGER.info("RELEASE_SMITH_PASS {} batches=1,16,64",recipe.getId());
            }ctx.assertTrue(covered==1,"Expected one custom arrow smithing recipe, found "+covered);
        }finally{player.currentScreenHandler=player.playerScreenHandler;cleanupPlayer(player);}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="release_crafting")
    public void allWaxableItemsAcquireCoatingThroughSurvivalHands(TestContext ctx){
        var player=survival(ctx);int covered=0;
        try{
            for(var item:Registries.ITEM)if(RustingSystem.rusted(item)!=null){
                var metal=new ItemStack(item,Math.min(3,item.getMaxCount()));metal.setCustomName(net.minecraft.text.Text.literal("Release coating fixture"));
                player.setStackInHand(Hand.OFF_HAND,metal);player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.HONEYCOMB,metal.getCount()+1));
                var result=UseItemCallback.EVENT.invoker().interact(player,ctx.getWorld(),Hand.MAIN_HAND);
                ctx.assertTrue(result.getResult().isAccepted()&&RustCoating.remaining(metal)==TetanusMod.CONFIG.coatingWeatherTicks,"Survival coating failed: "+Registries.ITEM.getId(item));
                ctx.assertTrue(player.getMainHandStack().getCount()==1&&metal.getCount()==Math.min(3,item.getMaxCount())&&metal.hasCustomName(),"Coating consumed the wrong payment or metal: "+Registries.ITEM.getId(item));
                var before=player.getMainHandStack().getCount();UseItemCallback.EVENT.invoker().interact(player,ctx.getWorld(),Hand.MAIN_HAND);ctx.assertTrue(player.getMainHandStack().getCount()==before,"Already-coated item charged twice");
                covered++;LOGGER.info("RELEASE_COAT_PASS {}",Registries.ITEM.getId(item));
            }ctx.assertTrue(covered==9,"Waxable item inventory changed: "+covered);
        }finally{cleanupPlayer(player);}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="release_crafting",tickLimit=20)
    public void pickingUpIronUnlocksEveryBookVisibleRuntimeModRecipe(TestContext ctx){
        var player=survival(ctx);var all=recipes(ctx);player.lockRecipes(all);
        var advancement=ctx.getWorld().getServer().getAdvancementLoader().get(TetanusMod.id("recipes/unlock"));ctx.assertTrue(advancement!=null,"Missing mod recipe unlock advancement");
        for(var criterion:advancement.getCriteria().keySet())player.getAdvancementTracker().revokeCriterion(advancement,criterion);
        player.getInventory().setStack(0,new ItemStack(Items.IRON_INGOT));player.playerScreenHandler.sendContentUpdates();
        ctx.runAtTick(2,()->{
            try{
                ctx.assertTrue(player.getAdvancementTracker().getProgress(advancement).isDone(),"Native inventory-change event did not complete the iron recipe unlock");
                for(var recipe:all){
                    if(recipe.isIgnoredInRecipeBook()){
                        ctx.assertTrue(recipe instanceof com.tetanusmod.medical.PrescriptionWritingRecipe||recipe instanceof com.tetanusmod.dragon.CrystalForgingRecipe,"Uncovered recipe excluded from the native book: "+recipe.getId());
                        continue; // Dedicated workstation menus/viewers are exercised in their own native/client proofs.
                    }
                    ctx.assertTrue(player.getRecipeBook().contains(recipe),"Recipe missing from iron unlock rewards: "+recipe.getId());
                }
                LOGGER.info("RELEASE_RECIPE_UNLOCK_PASS recipes={}",all.size());
            }finally{cleanupPlayer(player);}ctx.complete();
        });
    }
}
