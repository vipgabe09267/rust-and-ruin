package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.expansion.RustArrowSmithingRecipe;
import com.tetanusmod.registry.ModItems;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.*;
import net.minecraft.recipe.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

/** Real networked menus: only ingredients are seeded; every output is crafted and taken in game. */
public final class GraphicalCraftingAudit {
    private enum Kind { TABLE, FURNACE, SMOKER, BLAST_FURNACE, CAMPFIRE, SMITHING }
    private enum Phase { INITIALIZE, NEXT, PREPARE, BEFORE, ACT, COOK, OUTPUT, COOK_VIEW, TAKE, WAIT_TAKE, VERIFY, AFTER, CLEANUP, DONE }
    private record Spec(Identifier id,Kind kind,ItemStack output) {}
    private record Prepared(int syncId,Map<Item,Integer> inventory) {}
    private static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger("RustRuinGraphicalCrafting");
    private static final BlockPos STATION=new BlockPos(125,4,24);
    private Phase phase;
    private List<Spec> recipes;
    private Spec active;
    private Prepared prepared;
    private CompletableFuture<List<Spec>> initialization;
    private CompletableFuture<Prepared> preparation;
    private CompletableFuture<Void> operation;
    private int ticks,at,index,campfireEntityId;
    private UUID campfireEntity;
    private String viewer;

    public void start(MinecraftClient client){
        if(phase!=null)throw new IllegalStateException("Graphical crafting audit already started");
        if(client.getServer()==null||client.player==null)throw new IllegalStateException("Graphical crafting requires a connected integrated world");
        viewer=FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
        initialization=server(client,()->{
            var player=player(client);var world=player.getServerWorld();player.closeHandledScreen();player.changeGameMode(GameMode.SURVIVAL);player.setNoGravity(false);player.clearStatusEffects();
            for(int x=STATION.getX()-4;x<=STATION.getX()+4;x++)for(int z=STATION.getZ()-5;z<=STATION.getZ()+4;z++){
                world.setBlockState(new BlockPos(x,3,z),Blocks.STONE.getDefaultState());
                for(int y=4;y<=8;y++)world.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());
            }
            var result=new ArrayList<Spec>();
            for(var recipe:world.getRecipeManager().values().stream().filter(r->r.getId().getNamespace().equals(TetanusMod.MOD_ID)).sorted(Comparator.comparing(r->r.getId().toString())).toList()){
                Kind kind=recipe instanceof CraftingRecipe?Kind.TABLE:recipe instanceof RustArrowSmithingRecipe?Kind.SMITHING:
                    recipe.getType()==RecipeType.SMELTING?Kind.FURNACE:recipe.getType()==RecipeType.SMOKING?Kind.SMOKER:
                    recipe.getType()==RecipeType.BLASTING?Kind.BLAST_FURNACE:recipe.getType()==RecipeType.CAMPFIRE_COOKING?Kind.CAMPFIRE:null;
                require(kind!=null,"No graphical acquisition path for "+recipe.getId());var output=recipe.getOutput(world.getRegistryManager()).copy();
                require(!output.isEmpty(),"Empty graphical recipe result: "+recipe.getId());result.add(new Spec(recipe.getId(),kind,output));
            }
            require(result.size()>=29,"Graphical recipe inventory fell below29: "+result.size());return List.copyOf(result);
        });phase=Phase.INITIALIZE;
    }

    /** Called from END_CLIENT_TICK. True only after all native GUI outputs and cleanup succeed. */
    public boolean tick(MinecraftClient client){
        if(phase==null)throw new IllegalStateException("Graphical crafting audit was not started");
        if(++ticks>14000)throw new IllegalStateException("Graphical crafting timed out at "+phase+" / "+(active==null?"initialization":active.id));
        switch(phase){
            case INITIALIZE->{if(initialization.isDone()){recipes=initialization.join();advance(Phase.NEXT);}}
            case NEXT->{
                if(index==recipes.size()){
                    operation=server(client,()->{var player=player(client);player.closeHandledScreen();player.getServerWorld().setBlockState(STATION,Blocks.AIR.getDefaultState());return null;});advance(Phase.CLEANUP);
                }else{active=recipes.get(index);preparation=server(client,()->prepare(client));advance(Phase.PREPARE);}
            }
            case PREPARE->{if(preparation.isDone()){prepared=preparation.join();advance(Phase.BEFORE);}}
            case BEFORE->{
                if(ticks-at<30)break;
                if(!ready(client,false)){require(ticks-at<120,"Native screen/input did not arrive: "+active.id);break;}
                screenshot(client,"before");advance(Phase.ACT);
            }
            case ACT->{
                if(ticks-at<2)break;
                if(active.kind==Kind.TABLE||active.kind==Kind.SMITHING){take(client);advance(Phase.WAIT_TAKE);}
                else{operation=server(client,()->{cook(client);return null;});advance(Phase.COOK);}
            }
            case COOK->{if(operation.isDone()){operation.join();advance(Phase.OUTPUT);}}
            case OUTPUT->{
                if(ticks-at<20)break;
                if(!ready(client,true)){require(ticks-at<120,"Native cooked result did not arrive: "+active.id);break;}
                if(active.kind==Kind.CAMPFIRE&&client.world.getEntityById(campfireEntityId) instanceof ItemEntity item){
                    double dx=item.getX()-client.player.getX(),dz=item.getZ()-client.player.getZ(),dy=item.getY()+0.15-client.player.getEyeY();
                    client.player.setYaw((float)(Math.toDegrees(Math.atan2(dz,dx))-90));client.player.setPitch((float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))));
                    advance(Phase.COOK_VIEW);
                }else{screenshot(client,"cooked");advance(Phase.TAKE);}
            }
            case COOK_VIEW->{if(ticks-at>=10){screenshot(client,"cooked");advance(Phase.TAKE);}}
            case TAKE->{if(ticks-at>=2){take(client);advance(Phase.WAIT_TAKE);}}
            case WAIT_TAKE->{
                if(ticks-at<10)break;
                if(operation!=null&&operation.isCompletedExceptionally())operation.join();
                if(client.player.getInventory().count(active.output.getItem())!=active.output.getCount()){
                    require(ticks-at<120,"Network output was not acquired: "+active.id);break;
                }
                operation=server(client,()->{verify(client);return null;});advance(Phase.VERIFY);
            }
            case VERIFY->{if(operation.isDone()){operation.join();screenshot(client,"after");LOGGER.info("GRAPHICAL_CRAFT_PASS {} type={} count={} native-network-output=true",active.id,active.kind,active.output.getCount());advance(Phase.AFTER);}}
            case AFTER->{if(ticks-at>=20){index++;advance(Phase.NEXT);}}
            case CLEANUP->{if(operation.isDone()){operation.join();LOGGER.info("GRAPHICAL_CRAFT_ALL_PASS recipes={}",recipes.size());advance(Phase.DONE);}}
            case DONE->{return true;}
        }
        return phase==Phase.DONE;
    }
    private Prepared prepare(MinecraftClient client){
        var player=player(client);var world=player.getServerWorld();player.closeHandledScreen();player.getInventory().clear();player.getHungerManager().setFoodLevel(20);
        player.teleport(world,STATION.getX()+0.5,STATION.getY(),STATION.getZ()-2.5,0,26);
        var recipe=world.getRecipeManager().get(active.id).orElseThrow();var wanted=new HashMap<Item,Integer>();add(wanted,active.output);
        if(active.kind==Kind.TABLE){
            world.setBlockState(STATION,Blocks.CRAFTING_TABLE.getDefaultState());open(player);
            require(player.currentScreenHandler instanceof CraftingScreenHandler,"Server did not open a crafting table");var menu=(CraftingScreenHandler)player.currentScreenHandler;
            var ingredients=recipe.getIngredients();for(int i=0;i<ingredients.size();i++)if(!ingredients.get(i).isEmpty()){
                var input=ingredient(ingredients.get(i));if(input.getItem().hasRecipeRemainder())add(wanted,new ItemStack(input.getItem().getRecipeRemainder()));
                int grid=recipe instanceof ShapedRecipe shaped?(i/shaped.getWidth())*3+i%shaped.getWidth():i;menu.getSlot(grid+1).setStack(input);
            }
            require(menu.matches((CraftingRecipe)recipe),"Server native grid does not match "+active.id);
            require(ItemStack.areEqual(menu.getSlot(0).getStack(),active.output),"Server crafting preview mismatch: "+active.id);
        }else if(active.kind==Kind.SMITHING){
            world.setBlockState(STATION,Blocks.SMITHING_TABLE.getDefaultState());open(player);
            require(player.currentScreenHandler instanceof SmithingScreenHandler,"Server did not open native smithing");
            player.currentScreenHandler.getSlot(1).setStack(new ItemStack(Items.ARROW,64));player.currentScreenHandler.getSlot(2).setStack(new ItemStack(ModItems.RUST,2));
        }else if(active.kind==Kind.CAMPFIRE){
            world.setBlockState(STATION,Blocks.CAMPFIRE.getDefaultState());var campfire=(CampfireBlockEntity)world.getBlockEntity(STATION);var input=ingredient(recipe.getIngredients().get(0));
            require(campfire.getRecipeFor(input).orElseThrow().getId().equals(active.id),"Campfire selected another recipe");
            require(campfire.addItem(player,input,((AbstractCookingRecipe)recipe).getCookTime())&&input.isEmpty(),"Campfire did not consume its fixture ingredient");
        }else{
            Block block=switch(active.kind){case SMOKER->Blocks.SMOKER;case BLAST_FURNACE->Blocks.BLAST_FURNACE;default->Blocks.FURNACE;};
            world.setBlockState(STATION,block.getDefaultState());var furnace=(AbstractFurnaceBlockEntity)world.getBlockEntity(STATION);
            furnace.setStack(0,ingredient(recipe.getIngredients().get(0)));furnace.setStack(1,new ItemStack(Items.COAL));open(player);
        }
        player.currentScreenHandler.sendContentUpdates();return new Prepared(player.currentScreenHandler.syncId,Map.copyOf(wanted));
    }
    private void open(ServerPlayerEntity player){
        var factory=player.getServerWorld().getBlockState(STATION).createScreenHandlerFactory(player.getServerWorld(),STATION);
        require(factory!=null,"Workstation has no native menu");require(player.openHandledScreen(factory).isPresent(),"Native menu could not open");
    }
    private boolean ready(MinecraftClient client,boolean cooked){
        if(client.player==null||client.world==null)return false;
        if(active.kind==Kind.CAMPFIRE){
            if(client.currentScreen!=null)return false;
            if(cooked){if(client.player.getInventory().count(active.output.getItem())==active.output.getCount())return true;var output=client.world.getEntityById(campfireEntityId);return output instanceof ItemEntity item&&item.getUuid().equals(campfireEntity)&&ItemStack.areEqual(item.getStack(),active.output);}
            return client.world.getBlockEntity(STATION) instanceof CampfireBlockEntity campfire&&campfire.getItemsBeingCooked().stream().anyMatch(stack->!stack.isEmpty());
        }
        if(client.player.currentScreenHandler.syncId!=prepared.syncId)return false;
        var screen=client.currentScreen;boolean correct=switch(active.kind){case TABLE->screen instanceof CraftingScreen;case SMITHING->screen instanceof SmithingScreen;case SMOKER->screen instanceof SmokerScreen;case BLAST_FURNACE->screen instanceof BlastFurnaceScreen;default->screen instanceof FurnaceScreen;};
        if(!correct)return false;
        if(active.kind==Kind.TABLE||active.kind==Kind.SMITHING||cooked){int slot=active.kind==Kind.TABLE?0:active.kind==Kind.SMITHING?3:2;return ItemStack.areEqual(client.player.currentScreenHandler.getSlot(slot).getStack(),active.output);}
        return !client.player.currentScreenHandler.getSlot(0).getStack().isEmpty();
    }
    private void cook(MinecraftClient client){
        var player=player(client);var world=player.getServerWorld();var recipe=(AbstractCookingRecipe)world.getRecipeManager().get(active.id).orElseThrow();
        if(active.kind==Kind.CAMPFIRE){
            var previous=new HashSet<UUID>();for(var item:world.getEntitiesByClass(ItemEntity.class,new Box(STATION).expand(2),entity->true))previous.add(item.getUuid());
            var campfire=(CampfireBlockEntity)world.getBlockEntity(STATION);for(int tick=0;tick<=recipe.getCookTime();tick++)CampfireBlockEntity.litServerTick(world,STATION,world.getBlockState(STATION),campfire);
            var outputs=world.getEntitiesByClass(ItemEntity.class,new Box(STATION).expand(2),entity->!previous.contains(entity.getUuid())&&entity.getStack().isOf(active.output.getItem()));
            require(outputs.size()==1&&ItemStack.areEqual(outputs.get(0).getStack(),active.output),"Native campfire did not produce exactly its declared new output");
            campfireEntity=outputs.get(0).getUuid();campfireEntityId=outputs.get(0).getId();
        }
        else{var furnace=(AbstractFurnaceBlockEntity)world.getBlockEntity(STATION);for(int tick=0;tick<=recipe.getCookTime();tick++)AbstractFurnaceBlockEntity.tick(world,STATION,world.getBlockState(STATION),furnace);player.currentScreenHandler.sendContentUpdates();}
    }
    private void take(MinecraftClient client){
        if(active.kind==Kind.CAMPFIRE){
            operation=server(client,()->{var player=player(client);var world=player.getServerWorld();world.setBlockState(STATION,world.getBlockState(STATION).with(CampfireBlock.LIT,false));
                if(player.getInventory().count(active.output.getItem())==active.output.getCount())return null;
                var tracked=world.getEntity(campfireEntity);require(tracked instanceof ItemEntity,"Finished campfire output disappeared before pickup");
                var target=(ItemEntity)tracked;require(ItemStack.areEqual(target.getStack(),active.output),"Tracked cooked output changed before pickup");target.setPickupDelay(0);
                player.teleport(world,target.getX(),Math.max(STATION.getY()+0.45,target.getY()),target.getZ(),0,26);return null;});
        }else{
            require(client.interactionManager!=null,"No client interaction manager");int slot=active.kind==Kind.TABLE?0:active.kind==Kind.SMITHING?3:2;
            client.interactionManager.clickSlot(prepared.syncId,slot,0,SlotActionType.QUICK_MOVE,client.player);
        }
    }
    private void verify(MinecraftClient client){
        var player=player(client);var menu=player.currentScreenHandler;var actual=new HashMap<Item,Integer>();for(int i=0;i<player.getInventory().size();i++)add(actual,player.getInventory().getStack(i));
        if(active.kind==Kind.TABLE){for(int i=1;i<=9;i++)add(actual,menu.getSlot(i).getStack());require(menu.getSlot(0).getStack().isEmpty(),"Duplicate native craft output remains");}
        if(active.kind==Kind.SMITHING){require(menu.getSlot(0).getStack().isEmpty()&&menu.getSlot(1).getStack().isEmpty()&&menu.getSlot(2).getStack().isOf(ModItems.RUST)&&menu.getSlot(2).getStack().getCount()==1&&menu.getSlot(3).getStack().isEmpty(),"Network smithing consumed the wrong arrow/Rust amounts");}
        if(active.kind==Kind.FURNACE||active.kind==Kind.SMOKER||active.kind==Kind.BLAST_FURNACE){var furnace=(AbstractFurnaceBlockEntity)player.getServerWorld().getBlockEntity(STATION);require(furnace.getStack(0).isEmpty()&&furnace.getStack(1).isEmpty()&&furnace.getStack(2).isEmpty(),"Native appliance inputs/fuel/output not consumed");}
        if(active.kind==Kind.CAMPFIRE){var campfire=(CampfireBlockEntity)player.getServerWorld().getBlockEntity(STATION);require(campfire.getItemsBeingCooked().stream().allMatch(ItemStack::isEmpty),"Campfire still contains raw food");var drop=player.getServerWorld().getEntity(campfireEntity);require(drop==null||!drop.isAlive()||((ItemEntity)drop).getStack().isEmpty(),"Cooked output duplicated between the ground and inventory");}
        require(actual.equals(prepared.inventory),"Graphical inventory/remainders mismatch for "+active.id+": "+actual+" expected "+prepared.inventory);
    }
    private static ItemStack ingredient(Ingredient ingredient){var choices=ingredient.getMatchingStacks();require(choices.length>0,"Unresolved graphical ingredient");return choices[0].copyWithCount(1);}
    private static void add(Map<Item,Integer> map,ItemStack stack){if(!stack.isEmpty())map.merge(stack.getItem(),stack.getCount(),Integer::sum);}
    private static ServerPlayerEntity player(MinecraftClient client){return client.getServer().getPlayerManager().getPlayerList().get(0);}
    private static <T> CompletableFuture<T> server(MinecraftClient client,Supplier<T> task){var future=new CompletableFuture<T>();client.getServer().execute(()->{try{future.complete(task.get());}catch(Throwable error){future.completeExceptionally(error);}});return future;}
    private void advance(Phase next){phase=next;at=ticks;}
    private void screenshot(MinecraftClient client,String stage){ScreenshotRecorder.saveScreenshot(client.runDirectory,"rust-and-ruin-"+viewer+"-craft-"+active.id.getPath()+"-"+stage+".png",client.getFramebuffer(),message->{});}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
