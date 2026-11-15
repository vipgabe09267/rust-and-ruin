package com.tetanusmod.test;

import com.google.gson.GsonBuilder;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.compat.*;
import com.tetanusmod.dragon.*;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Actual forging GUI: blocked full inventory, batch extraction, preview close and both recipe screens. */
public final class GraphicalForgeAudit {
    private static final BlockPos STATION=new BlockPos(344,4,96);
    private final String viewer=FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
    private final List<Map<String,Object>> screenshots=new ArrayList<>();
    private final Map<String,Object> evidence=new LinkedHashMap<>();
    private CompletableFuture<?> pending;
    private Scene scene;
    private UUID playerId;
    private int phase,phaseTicks,ticks,stable;
    private boolean started,complete,restoring;
    private Perspective oldPerspective;
    private int oldFov;
    private boolean oldUse;
    public void start(MinecraftClient client){
        require(!started&&client.player!=null&&client.getServer()!=null,"Forging proof needs a connected integrated player");started=true;playerId=client.player.getUuid();oldPerspective=client.options.getPerspective();oldFov=client.options.getFov().getValue();oldUse=client.options.useKey.isPressed();client.options.useKey.setPressed(false);client.setScreen(null);client.options.setPerspective(Perspective.FIRST_PERSON);client.options.getFov().setValue(70);
        pending=server(client,p->{scene=new Scene(p);scene.prepare();return null;});
    }
    public boolean tick(MinecraftClient client){try{return tickInternal(client);}catch(RuntimeException|Error failure){restore(client);throw failure;}}
    private boolean tickInternal(MinecraftClient client){
        require(started,"Forging proof not started");if(complete)return true;require(++ticks<2800,"Forging proof timeout phase="+phase);phaseTicks++;
        if(pending!=null&&pending.isCompletedExceptionally())pending.join();
        switch(phase){
            case 0->{if(done()){pending.join();pending=null;next(1);}}
            case 1->{if(readyBlock(client)&&++stable>=30){shot(client,"station");open(client);next(2);}}
            case 2->{
                if(!(client.player.currentScreenHandler instanceof WitherForgingScreenHandler menu)||phaseTicks<20)return false;
                fill(client,menu);next(3);
            }
            case 3->{
                if(!(client.player.currentScreenHandler instanceof WitherForgingScreenHandler menu)||!inputs(menu,3,2,2)||!menu.getSlot(3).getStack().isOf(DragonContent.CRYSTAL)||phaseTicks<30)return false;
                pending=server(client,p->{scene.fillRemainingInventory();return null;});next(4);
            }
            case 4->{
                if(pending!=null){if(!done())return false;pending.join();pending=null;}if(client.player.getInventory().main.stream().anyMatch(ItemStack::isEmpty)||phaseTicks<30)return false;
                shot(client,"full-inventory-preview");click(client,client.player.currentScreenHandler,3,SlotActionType.QUICK_MOVE);next(5);
            }
            case 5->{
                if(phaseTicks<30)return false;require(client.player.getInventory().count(DragonContent.CRYSTAL)==0,"Full inventory received an uncharged crystal");
                pending=server(client,p->{scene.verifyFullInventoryRefusal();p.getInventory().setStack(8,ItemStack.EMPTY);p.currentScreenHandler.sendContentUpdates();return null;});next(6);
            }
            case 6->{
                if(pending!=null){if(!done())return false;pending.join();pending=null;}if(!client.player.getInventory().getStack(8).isEmpty()||phaseTicks<20)return false;
                click(client,client.player.currentScreenHandler,3,SlotActionType.QUICK_MOVE);next(7);
            }
            case 7->{
                if(!(client.player.currentScreenHandler instanceof WitherForgingScreenHandler menu)||client.player.getInventory().count(DragonContent.CRYSTAL)!=2||!inputs(menu,1,0,0)||menu.getSlot(3).hasStack()||phaseTicks<30)return false;
                shot(client,"batch-crafted");pending=server(client,p->{scene.verifyBatch();p.getInventory().setStack(7,ItemStack.EMPTY);p.currentScreenHandler.sendContentUpdates();return null;});next(8);
            }
            case 8->{if(pending!=null){if(!done())return false;pending.join();pending=null;}if(!client.player.getInventory().getStack(7).isEmpty()||phaseTicks<20)return false;client.player.closeHandledScreen();next(9);}
            case 9->{
                if(phaseTicks<20)return false;if(pending==null){pending=server(client,p->{scene.preparePreviewClose();return null;});return false;}if(!done())return false;pending.join();pending=null;next(10);
            }
            case 10->{if(readyBlock(client)&&phaseTicks>=20&&client.player.getInventory().count(ModItems.RUST)==1&&client.player.getInventory().count(Items.NETHERITE_INGOT)==1){open(client);next(11);}}
            case 11->{if(client.player.currentScreenHandler instanceof WitherForgingScreenHandler menu&&phaseTicks>=20){fill(client,menu);next(12);}}
            case 12->{
                if(!(client.player.currentScreenHandler instanceof WitherForgingScreenHandler menu)||!inputs(menu,1,1,1)||!menu.getSlot(3).getStack().isOf(DragonContent.CRYSTAL)||phaseTicks<30)return false;
                shot(client,"close-preview");client.player.closeHandledScreen();next(13);
            }
            case 13->{
                if(phaseTicks<20)return false;if(pending==null){pending=server(client,p->{scene.verifyPreviewClose();return null;});return false;}if(!done())return false;pending.join();pending=null;
                if(viewer.equals("plain")){restore(client);next(18);}else{showRecipe(client,false);next(14);}
            }
            case 14->{if(viewerReady(client)&&phaseTicks>=30){shot(client,"prescription-recipe-viewer");client.setScreen(null);showRecipe(client,true);next(15);}}
            case 15->{if(viewerReady(client)&&phaseTicks>=30){shot(client,"forging-recipe-viewer");verifyViewerCounts();evidence.put("NativeRecipeViewerScreens",2);restore(client);next(18);}}
            case 18->{if(pending!=null){if(!done())return false;pending.join();pending=null;}if(!scene.restored||phaseTicks<20)return false;write(client);complete=true;return true;}
            default->throw new IllegalStateException("Unknown forging phase "+phase);
        }
        return false;
    }
    private void fill(MinecraftClient client,WitherForgingScreenHandler menu){for(Item item:List.of(Items.END_CRYSTAL,ModItems.RUST,Items.NETHERITE_INGOT))click(client,menu,find(menu,item),SlotActionType.QUICK_MOVE);}
    private boolean readyBlock(MinecraftClient client){if(scene==null||client.player.squaredDistanceTo(scene.camera)>.04||!client.world.getBlockState(STATION).isOf(DragonContent.FORGE))return false;var delta=Vec3d.ofCenter(STATION).subtract(client.player.getEyePos());client.player.setYaw((float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90));client.player.setPitch((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));return client.crosshairTarget instanceof BlockHitResult hit&&hit.getBlockPos().equals(STATION);}
    private void open(MinecraftClient client){require(client.crosshairTarget instanceof BlockHitResult hit&&client.interactionManager.interactBlock(client.player,Hand.MAIN_HAND,hit).isAccepted(),"Actual block click did not open the forging table");}
    private static boolean inputs(ScreenHandler menu,int crystals,int rust,int ingots){return count(menu,0)==crystals&&count(menu,1)==rust&&count(menu,2)==ingots;}
    private static int count(ScreenHandler menu,int slot){return menu.getSlot(slot).getStack().getCount();}
    private static int find(ScreenHandler menu,Item item){for(int i=4;i<menu.slots.size();i++)if(menu.getSlot(i).getStack().isOf(item))return i;throw new IllegalStateException("Native forging inventory lacks "+item);}
    private static void click(MinecraftClient client,ScreenHandler menu,int slot,SlotActionType action){client.interactionManager.clickSlot(menu.syncId,slot,0,action,client.player);}
    private void showRecipe(MinecraftClient client,boolean forging){
        if(viewer.equals("jei")){require(forging?DragonJeiPlugin.showForging():MedicalJeiPlugin.showWriting(),"JEI runtime is not ready for the dedicated recipe screen");}
        else{boolean opened=me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder().addCategory(forging?DragonReiPlugin.FORGING:MedicalReiPlugin.WRITING).open();require(opened,"REI could not open the dedicated recipe category");}
    }
    private boolean viewerReady(MinecraftClient client){return client.currentScreen!=null&&(viewer.equals("jei")?client.currentScreen.getClass().getName().startsWith("mezz.jei."):client.currentScreen.getClass().getName().startsWith("me.shedaniel.rei."));}
    private void verifyViewerCounts(){if(viewer.equals("jei"))require(MedicalJeiPlugin.registeredRecipes==1&&DragonJeiPlugin.registeredRecipes==1,"JEI omitted a dedicated workstation recipe");else require(MedicalReiPlugin.registeredRecipes==1&&DragonReiPlugin.registeredRecipes==1,"REI omitted a dedicated workstation recipe");}
    private void restore(MinecraftClient client){if(restoring)return;restoring=true;client.options.useKey.setPressed(false);client.setScreen(null);client.options.setPerspective(oldPerspective);client.options.getFov().setValue(oldFov);if(scene!=null&&client.getServer()!=null)pending=server(client,p->{scene.restore();return null;});else pending=CompletableFuture.completedFuture(null);client.options.useKey.setPressed(oldUse);}
    private void write(MinecraftClient client){evidence.put("Result","PASS");evidence.put("ActualGraphicalClient",true);evidence.put("ManuallyPlacedFixtureStation",true);evidence.put("NativeCraftedCrystals",2);evidence.put("FullInventoryRefusedWithoutConsumption",true);evidence.put("ClosingPreviewReturnedOnlyInputs",true);evidence.put("ForgeAdvancementEarnedByNativeOutput",true);evidence.put("OriginalPlayerAndWorldStateRestored",scene.restored);evidence.put("DirectCriterionGrants",0);evidence.put("ProgressRevocations",0);evidence.put("Viewer",viewer);evidence.put("Screenshots",screenshots);try{Files.writeString(client.runDirectory.toPath().resolve("graphical-forge-"+viewer+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(evidence));}catch(Exception failure){throw new IllegalStateException("Cannot save forging graphical proof",failure);}}
    private void shot(MinecraftClient client,String view){String name="rust-and-ruin-"+viewer+"-forge-"+view+".png";ScreenshotRecorder.saveScreenshot(client.runDirectory,name,client.getFramebuffer(),text->{});screenshots.add(Map.of("View",view,"Screenshot",name,"NativeScreen",client.currentScreen==null?"HUD":client.currentScreen.getClass().getName()));org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_FORGE_VIEW {}",view);}
    private boolean done(){return pending!=null&&pending.isDone();}
    private void next(int value){phase=value;phaseTicks=0;stable=0;}
    private <T> CompletableFuture<T> server(MinecraftClient client,Function<ServerPlayerEntity,T> action){var result=new CompletableFuture<T>();client.getServer().execute(()->{try{var player=client.getServer().getPlayerManager().getPlayer(playerId);require(player!=null,"Forging actor disconnected");result.complete(action.apply(player));}catch(Throwable failure){result.completeExceptionally(failure);}});return result;}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static final class Scene {
        final ServerPlayerEntity player;final ServerWorld world;final Vec3d oldPosition,camera=Vec3d.ofBottomCenter(STATION).add(0,0,2.5);final float oldYaw,oldPitch,oldHealth;final GameMode oldMode;final boolean oldGravity,oldFlying,oldAllowFlying,oldInvulnerable;final NbtList inventory;final NbtCompound hunger=new NbtCompound();final List<StatusEffectInstance> effects;final int selected;final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();boolean restored;
        Scene(ServerPlayerEntity player){this.player=player;world=player.getServerWorld();require(world.getChunkManager().getChunkGenerator() instanceof net.minecraft.world.gen.chunk.FlatChunkGenerator,"Forging proof requires a fresh flat test world");require(!player.hasStatusEffect(TetanusMod.TETANUS)&&!player.hasStatusEffect(TetanusMod.LOCKJAW)&&!player.hasStatusEffect(com.tetanusmod.expansion.ExpansionEffects.RABIES),"Forging proof needs an initially healthy fixture actor");var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id("forge_crystal"));require(advancement!=null&&!player.getAdvancementTracker().getProgress(advancement).isDone(),"Use a fresh forging actor; existing progress is never revoked");oldPosition=player.getPos();oldYaw=player.getYaw();oldPitch=player.getPitch();oldHealth=player.getHealth();oldMode=player.interactionManager.getGameMode();oldGravity=player.hasNoGravity();oldFlying=player.getAbilities().flying;oldAllowFlying=player.getAbilities().allowFlying;oldInvulnerable=player.getAbilities().invulnerable;inventory=player.getInventory().writeNbt(new NbtList());selected=player.getInventory().selectedSlot;player.getHungerManager().writeNbt(hunger);effects=player.getStatusEffects().stream().map(StatusEffectInstance::new).toList();}
        void prepare(){player.closeHandledScreen();for(int x=-3;x<=3;x++)for(int z=-3;z<=4;z++)for(int y=-1;y<=4;y++){var pos=STATION.add(x,y,z);require(world.getBlockEntity(pos)==null,"Forging fixture refuses an existing block entity at "+pos);blocks.put(pos,world.getBlockState(pos));}for(var pos:blocks.keySet())world.setBlockState(pos,pos.getY()==3?Blocks.STONE.getDefaultState():Blocks.AIR.getDefaultState());world.setBlockState(STATION,DragonContent.FORGE.getDefaultState());player.changeGameMode(GameMode.SURVIVAL);player.clearStatusEffects();player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,2400,0,false,false,false));player.setNoGravity(true);player.getAbilities().invulnerable=true;player.sendAbilitiesUpdate();player.getInventory().clear();player.getInventory().selectedSlot=0;player.getInventory().setStack(9,new ItemStack(Items.END_CRYSTAL,3));player.getInventory().setStack(10,new ItemStack(ModItems.RUST,2));player.getInventory().setStack(11,new ItemStack(Items.NETHERITE_INGOT,2));player.teleport(world,camera.x,camera.y,camera.z,180,25);player.setVelocity(Vec3d.ZERO);player.currentScreenHandler.sendContentUpdates();}
        void fillRemainingInventory(){for(int i=0;i<player.getInventory().main.size();i++)if(player.getInventory().getStack(i).isEmpty())player.getInventory().setStack(i,new ItemStack(Items.COBBLESTONE,64));player.currentScreenHandler.sendContentUpdates();}
        void verifyFullInventoryRefusal(){require(player.currentScreenHandler instanceof WitherForgingScreenHandler&&inputs(player.currentScreenHandler,3,2,2)&&player.getInventory().count(DragonContent.CRYSTAL)==0,"Full inventory consumed forging inputs or created output");var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id("forge_crystal"));require(!player.getAdvancementTracker().getProgress(advancement).isDone(),"Preview/full inventory falsely earned the forging advancement");}
        void verifyBatch(){require(player.currentScreenHandler instanceof WitherForgingScreenHandler&&inputs(player.currentScreenHandler,1,0,0)&&player.getInventory().count(DragonContent.CRYSTAL)==2&&!player.currentScreenHandler.getSlot(3).hasStack(),"Native forging batch failed exact3ingredient accounting");}
        void preparePreviewClose(){require(player.currentScreenHandler==player.playerScreenHandler&&player.getInventory().count(Items.END_CRYSTAL)==1&&player.getInventory().count(DragonContent.CRYSTAL)==2,"Closing forge lost its remaining real crystal or gave another output");player.getInventory().setStack(9,new ItemStack(ModItems.RUST));player.getInventory().setStack(10,new ItemStack(Items.NETHERITE_INGOT));player.currentScreenHandler.sendContentUpdates();}
        void verifyPreviewClose(){require(player.currentScreenHandler==player.playerScreenHandler&&player.getInventory().count(Items.END_CRYSTAL)==1&&player.getInventory().count(ModItems.RUST)==1&&player.getInventory().count(Items.NETHERITE_INGOT)==1&&player.getInventory().count(DragonContent.CRYSTAL)==2,"Closing a preview duplicated output or lost an ingredient");var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id("forge_crystal"));require(player.getAdvancementTracker().getProgress(advancement).isDone(),"Real extraction did not earn the forging advancement");}
        void restore(){if(restored)return;player.closeHandledScreen();for(var entry:blocks.entrySet())world.setBlockState(entry.getKey(),entry.getValue());player.teleport(world,oldPosition.x,oldPosition.y,oldPosition.z,oldYaw,oldPitch);player.changeGameMode(oldMode);player.setNoGravity(oldGravity);player.getAbilities().flying=oldFlying;player.getAbilities().allowFlying=oldAllowFlying;player.getAbilities().invulnerable=oldInvulnerable;player.sendAbilitiesUpdate();player.getInventory().readNbt(inventory.copy());player.getInventory().selectedSlot=selected;player.getHungerManager().readNbt(hunger.copy());player.clearStatusEffects();for(var effect:effects)player.addStatusEffect(new StatusEffectInstance(effect));player.setHealth(oldHealth);player.currentScreenHandler.sendContentUpdates();restored=true;}
    }
}
