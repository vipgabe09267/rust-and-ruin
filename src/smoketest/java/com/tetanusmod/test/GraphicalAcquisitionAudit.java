package com.tetanusmod.test;

import com.tetanusmod.registry.ModBrewing;
import com.tetanusmod.registry.ModItems;
import com.tetanusmod.registry.ModBlocks;
import com.tetanusmod.expansion.ExpansionBlocks;
import com.tetanusmod.rust.RustingSystem;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.Hand;
import com.tetanusmod.expansion.ExpansionItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.BrewingStandScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.*;
import net.minecraft.potion.*;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import java.util.ArrayList;
import java.util.List;

/** Actual rendered brewing menus and native inventory clicks. Only the 400-tick wait is accelerated. */
public final class GraphicalAcquisitionAudit {
    private record Step(int chain,String id,Potion target,Potion input,Item inputBottle,Item ingredient,Potion output,Item outputBottle,int bottles,boolean newChain){
        boolean finalForm(){return output==target;}
        int form(){return outputBottle==Items.POTION?0:outputBottle==Items.SPLASH_POTION?1:2;}
        String formName(){return form()==0?"drinkable":form()==1?"splash":"lingering";}
    }
    private final BlockPos station=new BlockPos(208,4,20);
    private final List<Step> steps=new ArrayList<>();
    private int phase,index,ticks,phaseTicks,collected;
    private boolean started,finished;
    private final Item[] weatherInputs={ModItems.NAIL,ModItems.BENT_NAIL,ModItems.REBAR,ModItems.SHARPENED_REBAR,ModItems.METAL_PIPE,ModBlocks.CALTROPS.asItem(),ModBlocks.BARBED_WIRE.asItem(),ExpansionItems.SPEAR,ExpansionBlocks.BEAR_TRAP.asItem()};
    private final List<ItemEntity> weatherDrops=new ArrayList<>();
    private final BlockPos tank=station.add(6,0,0);
    private volatile boolean weatherReady,weatherPicked,brushDone;
    private int brushIndex,brushSource;
    private InventorySwap inventorySwap;
    private static final class InventorySwap {
        final int from,to,button;
        final Item requiredFrom,requiredTo;
        final String label;
        ItemStack beforeFrom,beforeTo;
        volatile boolean ready,acknowledged;
        volatile String serverState="not sampled";
        boolean sent;
        int ticks;
        InventorySwap(int from,int button,Item requiredFrom,Item requiredTo,String label){
            this.from=from;this.button=button;this.to=button==40?45:36+button;this.requiredFrom=requiredFrom;this.requiredTo=requiredTo;this.label=label;
        }
    }
    private volatile boolean engineDone,acquired;
    private volatile Throwable failure;
    private volatile long completed;
    private long queued;
    private String viewer(){return FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";}
    private void addChain(int chain,String id,Potion target){
        steps.add(new Step(chain,id,target,Potions.WATER,Items.POTION,ModItems.COW_MANURE,ModBrewing.MANURE,Items.POTION,3,true));
        if(target==ModBrewing.TETANUS||target==ModBrewing.LOCKJAW)steps.add(new Step(chain,id,target,ModBrewing.MANURE,Items.POTION,ModItems.RUST,ModBrewing.TETANUS,Items.POTION,3,false));
        if(target==ModBrewing.SALMONELLA)steps.add(new Step(chain,id,target,ModBrewing.MANURE,Items.POTION,ModItems.INFECTED_MEAT,target,Items.POTION,3,false));
        if(target==ModBrewing.RABIES)steps.add(new Step(chain,id,target,ModBrewing.MANURE,Items.POTION,ExpansionItems.RABID_MEAT,target,Items.POTION,3,false));
        if(target==ModBrewing.LOCKJAW)steps.add(new Step(chain,id,target,ModBrewing.TETANUS,Items.POTION,Items.PUFFERFISH,target,Items.POTION,3,false));
        steps.add(new Step(chain,id,target,target,Items.POTION,Items.GUNPOWDER,target,Items.SPLASH_POTION,2,false));
        steps.add(new Step(chain,id,target,target,Items.SPLASH_POTION,Items.DRAGON_BREATH,target,Items.LINGERING_POTION,1,false));
    }
    public void start(MinecraftClient client){
        if(started)throw new IllegalStateException("Graphical acquisition audit already started");started=true;
        addChain(0,"cow-manure",ModBrewing.MANURE);addChain(1,"tetanus",ModBrewing.TETANUS);addChain(2,"salmonella",ModBrewing.SALMONELLA);addChain(3,"lockjaw-sonata",ModBrewing.LOCKJAW);addChain(4,"rabies",ModBrewing.RABIES);
        server(client,()->{
            var player=player(client);var world=player.getServerWorld();player.closeHandledScreen();player.clearStatusEffects();player.changeGameMode(GameMode.SURVIVAL);player.setHealth(player.getMaxHealth());player.getHungerManager().setFoodLevel(20);player.getInventory().clear();
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){world.setBlockState(station.add(x,-1,z),Blocks.STONE.getDefaultState());for(int y=0;y<4;y++)world.setBlockState(station.add(x,y,z),Blocks.AIR.getDefaultState());}
            world.setBlockState(station,Blocks.BREWING_STAND.getDefaultState());player.teleport(world,station.getX()+0.5,station.getY(),station.getZ()-2,0,12);player.openHandledScreen((BrewingStandBlockEntity)world.getBlockEntity(station));
        });
    }
    private ServerPlayerEntity player(MinecraftClient client){return client.getServer().getPlayerManager().getPlayerList().get(0);}
    private void server(MinecraftClient client,Runnable action){
        long ticket=++queued;client.getServer().execute(()->{try{action.run();}catch(Throwable problem){failure=problem;}finally{completed=ticket;}});
    }
    private void nextPhase(int value){phase=value;phaseTicks=0;}
    private boolean matches(ItemStack stack,Potion potion,Item bottle){return stack.isOf(bottle)&&PotionUtil.getPotion(stack)==potion&&stack.getCount()==1;}
    private int playerSlot(int inventory){return 5+inventory-9;}
    private void click(MinecraftClient client,int slot){client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,slot,0,SlotActionType.PICKUP,client.player);}
    private void move(MinecraftClient client,int from,int to){
        var handler=client.player.currentScreenHandler;
        if(!handler.getCursorStack().isEmpty())throw new IllegalStateException("Brewing transfer began with carried stack: "+menuState(handler,from,to));
        click(client,from);click(client,to);
        if(!handler.getCursorStack().isEmpty())throw new IllegalStateException("Brewing transfer left carried stack: "+menuState(handler,from,to));
    }
    private String menuState(net.minecraft.screen.ScreenHandler handler,int from,int to){
        return "handler="+handler.getClass().getSimpleName()+" sync="+handler.syncId+" revision="+handler.getRevision()+" cursor="+handler.getCursorStack()+" source["+from+"]="+handler.getSlot(from).getStack()+" destination["+to+"]="+handler.getSlot(to).getStack()+" phase="+phase+" brush="+brushIndex;
    }
    private boolean required(ItemStack stack,Item item){return item==null?stack.isEmpty():stack.isOf(item)&&stack.getCount()==1;}
    private void beginInventorySwap(MinecraftClient client,int from,int button,Item requiredFrom,Item requiredTo,String label){
        if(inventorySwap!=null)throw new IllegalStateException("Overlapping acknowledged inventory transactions");
        var transaction=new InventorySwap(from,button,requiredFrom,requiredTo,label);inventorySwap=transaction;
        org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_INVENTORY_BEGIN {} client {}",label,menuState(client.player.currentScreenHandler,transaction.from,transaction.to));
        server(client,()->{
            var player=player(client);var handler=player.currentScreenHandler;transaction.serverState=menuState(handler,transaction.from,transaction.to);
            if(handler!=player.playerScreenHandler||handler.syncId!=0)throw new IllegalStateException("Inventory transaction reached a different server menu: "+transaction.serverState);
            if(!handler.getCursorStack().isEmpty()||!required(handler.getSlot(transaction.from).getStack(),transaction.requiredFrom)||!required(handler.getSlot(transaction.to).getStack(),transaction.requiredTo))throw new IllegalStateException("Inventory transaction authoritative precondition failed: "+transaction.label+" "+transaction.serverState);
            transaction.beforeFrom=handler.getSlot(transaction.from).getStack().copy();transaction.beforeTo=handler.getSlot(transaction.to).getStack().copy();
            // Reconcile a cached player-menu cursor via the native full-state packet, never by changing inventory.
            handler.syncState();transaction.ready=true;
            org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_INVENTORY_SERVER_BEFORE {} {}",transaction.label,transaction.serverState);
        });
    }
    private boolean completeInventorySwap(MinecraftClient client){
        var transaction=inventorySwap;if(transaction==null)throw new IllegalStateException("No acknowledged inventory transaction");
        var handler=client.player.currentScreenHandler;
        if(++transaction.ticks>120)throw new IllegalStateException("Inventory transaction timed out: "+transaction.label+" client "+menuState(handler,transaction.from,transaction.to)+" server "+transaction.serverState);
        if(handler!=client.player.playerScreenHandler||handler.syncId!=0||!(client.currentScreen instanceof InventoryScreen))throw new IllegalStateException("Inventory transaction lost its actual player menu: "+transaction.label);
        if(!transaction.ready)return false;
        if(!transaction.sent){
            if(!handler.getCursorStack().isEmpty()||!ItemStack.areEqual(handler.getSlot(transaction.from).getStack(),transaction.beforeFrom)||!ItemStack.areEqual(handler.getSlot(transaction.to).getStack(),transaction.beforeTo))return false;
            // Native number-key/F-key swap packets do not use the carried stack.
            client.interactionManager.clickSlot(handler.syncId,transaction.from,transaction.button,SlotActionType.SWAP,client.player);transaction.sent=true;
            return false;
        }
        if(transaction.acknowledged){
            if(!handler.getCursorStack().isEmpty()||!ItemStack.areEqual(handler.getSlot(transaction.from).getStack(),transaction.beforeTo)||!ItemStack.areEqual(handler.getSlot(transaction.to).getStack(),transaction.beforeFrom))return false;
            org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_INVENTORY_ACK {} client {}",transaction.label,menuState(handler,transaction.from,transaction.to));inventorySwap=null;return true;
        }
        server(client,()->{
            var player=player(client);var current=player.currentScreenHandler;transaction.serverState=menuState(current,transaction.from,transaction.to);
            if(current!=player.playerScreenHandler||current.syncId!=0)throw new IllegalStateException("Native swap changed server menu: "+transaction.serverState);
            if(current.getCursorStack().isEmpty()&&ItemStack.areEqual(current.getSlot(transaction.from).getStack(),transaction.beforeTo)&&ItemStack.areEqual(current.getSlot(transaction.to).getStack(),transaction.beforeFrom)){
                current.syncState();transaction.acknowledged=true;
            }
        });return false;
    }
    private void screenshot(MinecraftClient client,String label){ScreenshotRecorder.saveScreenshot(client.runDirectory,"rust-and-ruin-"+viewer()+"-"+label+".png",client.getFramebuffer(),text->{});}
    public boolean tick(MinecraftClient client){
        if(!started)throw new IllegalStateException("Graphical acquisition audit was not started");if(finished)return true;
        if(failure!=null)throw new IllegalStateException("Graphical brewing server action failed",failure);
        if(++ticks>2600)throw new IllegalStateException("Graphical brewing audit timed out at step "+index+" phase "+phase);
        phaseTicks++;if(completed<queued)return false;
        if(phase>=10)return weatherTick(client);
        if(phase==9){
            if(phaseTicks==1){client.player.closeHandledScreen();client.setScreen(new InventoryScreen(client.player));}
            if(phaseTicks>=8){
                if(!(client.currentScreen instanceof InventoryScreen))throw new IllegalStateException("Earned potion inventory did not open");
                for(int chain=0;chain<5;chain++)for(int form=0;form<3;form++){
                    Potion target=new Potion[]{ModBrewing.MANURE,ModBrewing.TETANUS,ModBrewing.SALMONELLA,ModBrewing.LOCKJAW,ModBrewing.RABIES}[chain];Item bottle=new Item[]{Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION}[form];
                    if(!matches(client.player.getInventory().getStack(14+chain*3+form),target,bottle))throw new IllegalStateException("Graphical client inventory lacks earned potion "+chain+"/"+form);
                }
                if(collected!=15)throw new IllegalStateException("Expected 15 earned potion forms, acquired "+collected);screenshot(client,"brew-all-earned-potions");
                org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_ACQUIRE_PASS: fifteen native brewed potions in the actual client inventory");nextPhase(10);return false;
            }
            return false;
        }
        if(!(client.currentScreen instanceof BrewingStandScreen)||!(client.player.currentScreenHandler instanceof BrewingStandScreenHandler)){
            if(phaseTicks>100)throw new IllegalStateException("Actual brewing menu did not remain open");return false;
        }
        var step=steps.get(index);var handler=client.player.currentScreenHandler;
        if(phase==0){nextPhase(1);return false;}
        if(phase==1){
            engineDone=false;acquired=false;
            server(client,()->{
                var player=player(client);var stand=(BrewingStandBlockEntity)player.getServerWorld().getBlockEntity(station);
                if(step.newChain()){
                    for(int slot=0;slot<3;slot++){if(!stand.getStack(slot).isEmpty())throw new IllegalStateException("Previous brewing chain left a bottle");player.getInventory().setStack(9+slot,PotionUtil.setPotion(new ItemStack(Items.POTION),Potions.WATER));}
                }
                if(!stand.getStack(3).isEmpty())throw new IllegalStateException("Previous brewing step left an ingredient");
                player.getInventory().setStack(12,new ItemStack(step.ingredient()));
                if(index==0)player.getInventory().setStack(13,new ItemStack(Items.BLAZE_POWDER));
                player.getInventory().markDirty();player.currentScreenHandler.sendContentUpdates();
            });nextPhase(2);return false;
        }
        if(phase==2){
            if(!handler.getSlot(playerSlot(12)).getStack().isOf(step.ingredient()))return false;
            if(step.newChain())for(int slot=0;slot<3;slot++)if(!matches(handler.getSlot(playerSlot(9+slot)).getStack(),Potions.WATER,Items.POTION))return false;
            if(index==0&&!handler.getSlot(playerSlot(13)).getStack().isOf(Items.BLAZE_POWDER))return false;
            if(step.newChain())for(int slot=0;slot<3;slot++)move(client,playerSlot(9+slot),slot);
            move(client,playerSlot(12),3);if(index==0)move(client,playerSlot(13),4);nextPhase(3);return false;
        }
        if(phase==3){
            if(engineDone){nextPhase(4);return false;}
            if(phaseTicks>120)throw new IllegalStateException("Real brewing clicks did not reach the server");
            server(client,()->{
                var player=player(client);var world=player.getServerWorld();var stand=(BrewingStandBlockEntity)world.getBlockEntity(station);
                if(!stand.getStack(3).isOf(step.ingredient()))return;
                for(int slot=3-step.bottles();slot<3;slot++)if(!matches(stand.getStack(slot),step.input(),step.inputBottle()))return;
                // Execute the actual vanilla brewing-stand engine, retaining its ingredient/fuel consumption.
                for(int tick=0;tick<402;tick++)BrewingStandBlockEntity.tick(world,station,world.getBlockState(station),stand);
                for(int slot=3-step.bottles();slot<3;slot++)if(!matches(stand.getStack(slot),step.output(),step.outputBottle()))throw new IllegalStateException("Native brewing produced the wrong potion in step "+step.id());
                player.currentScreenHandler.sendContentUpdates();engineDone=true;
            });return false;
        }
        if(phase==4){
            for(int slot=3-step.bottles();slot<3;slot++)if(!matches(handler.getSlot(slot).getStack(),step.output(),step.outputBottle()))return false;
            if(phaseTicks<5)return false;
            if(step.finalForm()){
                screenshot(client,"brew-"+step.id()+"-"+step.formName());int destination=14+step.chain()*3+step.form();move(client,3-step.bottles(),playerSlot(destination));
                if(step.outputBottle()==Items.LINGERING_POTION){
                    // Vanilla 1.20.1 consumes the final ingredient before checking its remainder;
                    // one Dragon Breath therefore leaves AIR. Preserve a real bottle if another mod fixes it.
                    var remainder=handler.getSlot(3).getStack();
                    if(remainder.isOf(Items.GLASS_BOTTLE))move(client,3,playerSlot(31));
                    else if(!remainder.isEmpty())throw new IllegalStateException("Unexpected brewing remainder: "+remainder);
                }
                nextPhase(5);
            }else{index++;nextPhase(1);}
            return false;
        }
        if(phase==5){
            if(acquired){
                int destination=14+step.chain()*3+step.form();if(!matches(client.player.getInventory().getStack(destination),step.output(),step.outputBottle()))return false;
                collected++;org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_BREW_PASS: {} {} via native screen clicks and brewing",step.id(),step.formName());index++;nextPhase(index==steps.size()?9:1);return false;
            }
            if(phaseTicks>120)throw new IllegalStateException("Earned potion was not taken through native menu clicks");
            server(client,()->{
                var player=player(client);var stand=(BrewingStandBlockEntity)player.getServerWorld().getBlockEntity(station);int destination=14+step.chain()*3+step.form();
                if(matches(player.getInventory().getStack(destination),step.output(),step.outputBottle())&&stand.getStack(3-step.bottles()).isEmpty()&&(step.outputBottle()!=Items.LINGERING_POTION||stand.getStack(3).isEmpty()))acquired=true;
            });return false;
        }
        return false;
    }
    private int inventoryScreenSlot(int inventory){return inventory<9?36+inventory:inventory;}
    private int count(ServerPlayerEntity player,Item item){int total=0;for(int i=0;i<player.getInventory().size();i++)if(player.getInventory().getStack(i).isOf(item))total+=player.getInventory().getStack(i).getCount();return total;}
    private boolean weatherTick(MinecraftClient client){
        if(phase==10){
            client.setScreen(null);
            server(client,()->{
                var player=player(client);var world=player.getServerWorld();player.closeHandledScreen();
                for(int x=-7;x<=2;x++)for(int z=-2;z<=2;z++){world.setBlockState(tank.add(x,-1,z),Blocks.STONE.getDefaultState());for(int y=0;y<=6;y++)world.setBlockState(tank.add(x,y,z),Blocks.AIR.getDefaultState());}
                for(int height=0;height<5;height++){
                    for(var side:net.minecraft.util.math.Direction.Type.HORIZONTAL)world.setBlockState(tank.up(height).offset(side),Blocks.GLASS.getDefaultState());
                    world.setBlockState(tank.up(height),Blocks.WATER.getDefaultState());
                }
                world.setBlockState(tank.up(5),Blocks.GLASS.getDefaultState());
                player.teleport(world,tank.getX()-6,tank.getY(),tank.getZ()+0.5,-90,-10);player.getInventory().setStack(9,new ItemStack(ModItems.WIRE_BRUSH));player.currentScreenHandler.sendContentUpdates();
                for(var item:weatherInputs){
                    var entity=new ItemEntity(world,tank.getX()+0.5,tank.getY()+0.5,tank.getZ()+0.5,new ItemStack(item));entity.setNoGravity(true);entity.setVelocity(net.minecraft.util.math.Vec3d.ZERO);entity.setPickupDelay(32767);world.spawnEntity(entity);weatherDrops.add(entity);
                }
            });nextPhase(11);return false;
        }
        if(phase==11){
            if(weatherReady){screenshot(client,"acquire-weathering-water");server(client,()->{var player=player(client);weatherDrops.forEach(entity->entity.setPickupDelay(0));player.getServerWorld().setBlockState(tank.up(5),Blocks.AIR.getDefaultState());player.setNoGravity(true);player.teleport(player.getServerWorld(),tank.getX()+0.5,weatherDrops.get(0).getY()-0.5,tank.getZ()+0.5,0,0);});nextPhase(12);return false;}
            if(phaseTicks>350)throw new IllegalStateException("Natural wet-item weathering did not finish");
            server(client,()->{
                for(int i=0;i<weatherInputs.length;i++){
                    var entity=weatherDrops.get(i);if(!entity.isAlive())throw new IllegalStateException("Weathering specimen disappeared before pickup");
                    if(!entity.getStack().isOf(RustingSystem.rusted(weatherInputs[i]))){if(entity.age>200)throw new IllegalStateException("Actual wet specimen failed to rust: "+entity.getStack()+" at "+entity.getPos());return;}
                }
                weatherReady=true;
            });return false;
        }
        if(phase==12){
            if(weatherPicked){client.setScreen(new InventoryScreen(client.player));nextPhase(13);return false;}
            if(phaseTicks>180)throw new IllegalStateException("Native player collision did not collect weathered items");
            server(client,()->{
                var player=player(client);for(var input:weatherInputs)if(count(player,RustingSystem.rusted(input))!=1)return;
                if(weatherDrops.stream().anyMatch(ItemEntity::isAlive))throw new IllegalStateException("Picked-up rusty items were duplicated");
                player.setNoGravity(false);player.teleport(player.getServerWorld(),tank.getX()-6,tank.getY(),tank.getZ()+0.5,-90,0);weatherPicked=true;
            });return false;
        }
        if(phase==13){
            if(!(client.currentScreen instanceof InventoryScreen))throw new IllegalStateException("Rusty acquisition inventory did not open");
            if(inventorySwap!=null){if(completeInventorySwap(client)){client.player.getInventory().selectedSlot=0;nextPhase(14);}return false;}
            if(phaseTicks<8)return false;
            for(var input:weatherInputs)if(client.player.getInventory().count(RustingSystem.rusted(input))!=1)return false;
            screenshot(client,"acquire-all-nine-rusted");
            var hotbar=client.player.getInventory().getStack(0);
            if(hotbar.isEmpty()||!client.player.getInventory().getStack(9).isOf(ModItems.WIRE_BRUSH))throw new IllegalStateException("Rusty pickup/brush equipment fixture was not synchronized");
            beginInventorySwap(client,9,0,ModItems.WIRE_BRUSH,hotbar.getItem(),"equip-brush");return false;
        }
        if(phase==14){
            if(!(client.currentScreen instanceof InventoryScreen)){client.setScreen(new InventoryScreen(client.player));return false;}
            if(inventorySwap!=null){if(completeInventorySwap(client)){brushDone=false;nextPhase(15);}return false;}
            var expected=RustingSystem.rusted(weatherInputs[brushIndex]);brushSource=-1;
            for(int slot=0;slot<36;slot++)if(client.player.getInventory().getStack(slot).isOf(expected)){brushSource=slot;break;}
            if(brushSource<0)throw new IllegalStateException("Earned rusty item missing before brushing: "+expected);
            if(!client.player.getInventory().getStack(0).isOf(ModItems.WIRE_BRUSH))return false;
            beginInventorySwap(client,inventoryScreenSlot(brushSource),40,expected,null,"equip-rusty-"+brushIndex);return false;
        }
        if(phase==15){
            if(phaseTicks<4)return false;client.setScreen(null);client.player.getInventory().selectedSlot=0;
            if(!client.player.getOffHandStack().isOf(RustingSystem.rusted(weatherInputs[brushIndex])))throw new IllegalStateException("Native inventory click did not equip rusty off-hand item");
            client.interactionManager.interactItem(client.player,Hand.MAIN_HAND);nextPhase(16);return false;
        }
        if(phase==16){
            if(brushDone){
                if(!client.player.getOffHandStack().isOf(weatherInputs[brushIndex]))return false;
                org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_ACQUIRE_PASS: {} from100tick water exposure and native pickup; actual client wire-brush use restored clean item",net.minecraft.registry.Registries.ITEM.getId(RustingSystem.rusted(weatherInputs[brushIndex])));
                client.setScreen(new InventoryScreen(client.player));nextPhase(17);return false;
            }
            if(phaseTicks>120)throw new IllegalStateException("Native wire brush did not clean the acquired rusty item");
            server(client,()->{
                var player=player(client);if(player.getOffHandStack().isOf(weatherInputs[brushIndex])){
                    if(count(player,ModItems.RUST)!=brushIndex+1||player.getMainHandStack().getDamage()!=brushIndex+1)throw new IllegalStateException("Client brushing produced incorrect Rust or durability");brushDone=true;
                }
            });return false;
        }
        if(phase==17){
            if(phaseTicks<3)return false;if(!(client.currentScreen instanceof InventoryScreen))throw new IllegalStateException("Brushed inventory did not reopen");
            if(inventorySwap!=null){if(completeInventorySwap(client)){brushIndex++;nextPhase(brushIndex==weatherInputs.length?18:14);}return false;}
            int destination=brushSource;
            // Rust may occupy the emptied source slot. Keep that earned byproduct and choose another empty slot.
            if(!client.player.getInventory().getStack(destination).isEmpty()){
                destination=-1;for(int slot=1;slot<36;slot++)if(client.player.getInventory().getStack(slot).isEmpty()){destination=slot;break;}
            }
            if(destination<0)throw new IllegalStateException("No empty inventory slot for the genuinely cleaned item");
            beginInventorySwap(client,inventoryScreenSlot(destination),40,null,weatherInputs[brushIndex],"store-clean-"+brushIndex);return false;
        }
        if(phase==18){
            if(phaseTicks<8)return false;
            for(var input:weatherInputs)if(client.player.getInventory().count(input)!=1)throw new IllegalStateException("Restored clean item missing: "+input);
            if(client.player.getInventory().count(ModItems.RUST)!=9||client.player.getMainHandStack().getDamage()!=9)throw new IllegalStateException("Final Rust/byproduct/durability did not synchronize");
            screenshot(client,"acquire-all-nine-brushed");org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_ACQUIRE_PASS: all nine weathering/cleaning pairs, nine earned Rust and nine durability consumed");finished=true;return true;
        }
        throw new IllegalStateException("Unknown acquisition phase "+phase);
    }
}
