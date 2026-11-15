package com.tetanusmod.test;

import com.google.gson.GsonBuilder;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.expansion.*;
import com.tetanusmod.medical.*;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.*;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.advancement.*;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.*;
import net.minecraft.util.*;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.village.*;
import net.minecraft.world.GameMode;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Real connected-client writing, prescription trade, timed cure and supply interaction. */
public final class GraphicalMedicalAudit {
    private static final BlockPos STATION=new BlockPos(320,4,96);
    private final String viewer=FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
    private final List<Map<String,Object>> shots=new ArrayList<>();
    private final Map<String,Object> evidence=new LinkedHashMap<>();
    private final Set<ChatHudLine> oldChat=Collections.newSetFromMap(new IdentityHashMap<>());
    private CompletableFuture<?> pending;
    private Scene scene;
    private UUID playerId;
    private int ticks,phase,phaseTicks,stable,medicinePrice;
    private boolean started,complete,restoring,clicked;
    private Perspective oldPerspective;
    private int oldFov;
    private boolean oldUse,oldSneak;
    private Vec3d cameraTarget=Vec3d.ofCenter(STATION);
    private AdvancementsScreen advancements;

    public void start(MinecraftClient client){
        require(!started&&client.player!=null&&client.getServer()!=null,"Medical proof needs a connected integrated player");started=true;playerId=client.player.getUuid();
        oldPerspective=client.options.getPerspective();oldFov=client.options.getFov().getValue();oldUse=client.options.useKey.isPressed();oldSneak=client.options.sneakKey.isPressed();release(client);client.setScreen(null);client.options.setPerspective(Perspective.FIRST_PERSON);client.options.getFov().setValue(70);
        pending=server(client,p->{scene=new Scene(p);scene.prepare();return null;});
    }
    public boolean tick(MinecraftClient client){try{return tickInternal(client);}catch(RuntimeException|Error problem){restore(client);throw problem;}}
    private boolean tickInternal(MinecraftClient client){
        require(started,"Medical proof not started");if(complete)return true;require(++ticks<3600,"Medical proof timed out at phase="+phase);phaseTicks++;
        if(pending!=null&&pending.isCompletedExceptionally())pending.join();
        switch(phase){
            case 0->{if(done()){pending.join();pending=null;next(1);}}
            case 1->{
                if(!at(client,scene.stationCamera)||!client.world.getBlockState(STATION).isOf(MedicalWorkstations.WRITER)){stable=0;return false;}aim(client,Vec3d.ofCenter(STATION));
                if(!(client.crosshairTarget instanceof BlockHitResult hit)||!hit.getBlockPos().equals(STATION)){stable=0;return false;}
                if(++stable<30)return false;shot(client,"writer-exterior");require(client.interactionManager.interactBlock(client.player,Hand.MAIN_HAND,hit).isAccepted(),"Native block interaction refused the writer");next(2);
            }
            case 2->{
                if(!(client.player.currentScreenHandler instanceof PrescriptionWriterScreenHandler menu)||client.currentScreen==null||phaseTicks<20)return false;
                click(client,menu,find(menu,Items.PAPER,3),0,SlotActionType.QUICK_MOVE);click(client,menu,find(menu,Items.INK_SAC,3),0,SlotActionType.QUICK_MOVE);next(3);
            }
            case 3->{
                if(!(client.player.currentScreenHandler instanceof PrescriptionWriterScreenHandler menu))return false;
                if(menu.getSlot(0).getStack().getCount()!=3||menu.getSlot(1).getStack().getCount()!=2||!menu.getSlot(2).getStack().isOf(MedicalWorkstations.PRESCRIPTION)||phaseTicks<30)return false;
                shot(client,"writer-inputs-output");click(client,menu,2,0,SlotActionType.QUICK_MOVE);next(4);
            }
            case 4->{
                if(!(client.player.currentScreenHandler instanceof PrescriptionWriterScreenHandler menu)||phaseTicks<30)return false;
                if(client.player.getInventory().count(MedicalWorkstations.PRESCRIPTION)!=2||menu.getSlot(0).getStack().getCount()!=1||menu.getSlot(1).hasStack()||menu.getSlot(2).hasStack())return false;
                require(menu.getSlot(1).getStack().isEmpty()&&menu.getSlot(2).getStack().isEmpty(),"Native writing batch left an unpaid result");shot(client,"writer-crafted");client.player.closeHandledScreen();
                pending=server(client,p->{scene.verifyWriting();scene.moveToMedic();return null;});next(5);
            }
            case 5->{if(done()){pending.join();pending=null;cameraTarget=scene.medic.getBoundingBox().getCenter();next(6);}}
            case 6->{
                if(!at(client,scene.medicCamera)||npc(client)==null||phaseTicks<20)return false;aim(client,npc(client).getBoundingBox().getCenter());client.options.sneakKey.setPressed(true);next(7);
            }
            case 7->{
                if(!client.player.isSneaking()||phaseTicks<10)return false;aim(client,npc(client).getBoundingBox().getCenter());oldChat.clear();oldChat.addAll(chat(client));
                require(client.interactionManager.interactEntity(client.player,npc(client),Hand.MAIN_HAND).isAccepted(),"Native medic query failed");next(8);
            }
            case 8->{
                var message=message(client,"message.tetanusmod.supply_request");if(message==null||phaseTicks<20)return false;
                evidence.put("ActualSupplyRequestMessage",message.getString());client.setScreen(new ChatScreen(""));shot(client,"supply-request");client.setScreen(null);
                pending=server(client,p->{scene.seedDelivery();return null;});next(9);
            }
            case 9->{
                if(pending!=null){if(!done())return false;pending.join();pending=null;}client.player.getInventory().selectedSlot=6;client.options.sneakKey.setPressed(true);
                if(!client.player.getMainHandStack().isOf(scene.request.getItem())||client.player.getMainHandStack().getCount()!=scene.request.getCount()||!client.player.isSneaking()||phaseTicks<20)return false;
                oldChat.clear();oldChat.addAll(chat(client));require(client.interactionManager.interactEntity(client.player,npc(client),Hand.MAIN_HAND).isAccepted(),"Native delivery interaction failed");next(10);
            }
            case 10->{
                var message=message(client,"message.tetanusmod.supplies_delivered");if(message==null||phaseTicks<20)return false;
                evidence.put("ActualSupplyCompletionMessage",message.getString());client.setScreen(new ChatScreen(""));shot(client,"supply-delivered");client.setScreen(null);client.options.sneakKey.setPressed(false);
                pending=server(client,p->{scene.verifyDelivery();return null;});next(11);
            }
            case 11->{
                if(pending!=null){if(!done())return false;pending.join();pending=null;}if(client.player.isSneaking()||phaseTicks<20)return false;
                aim(client,npc(client).getBoundingBox().getCenter());require(client.interactionManager.interactEntity(client.player,npc(client),Hand.MAIN_HAND).isAccepted(),"Native medic trade interaction failed");next(12);
            }
            case 12->{
                if(!(client.currentScreen instanceof MerchantScreen)||!(client.player.currentScreenHandler instanceof MerchantScreenHandler menu)||phaseTicks<30||menu.getRecipes().isEmpty())return false;
                var offer=menu.getRecipes().get(0);require(offer.getSellItem().isOf(ModItems.ANTITOXIN)&&offer.getSecondBuyItem().isOf(MedicalWorkstations.PRESCRIPTION),"Fixture nurse did not offer a real prescription treatment");medicinePrice=offer.getAdjustedFirstBuyItem().getCount();evidence.put("ActualEmeraldPrice",medicinePrice);
                click(client,menu,find(menu,Items.EMERALD,3),1,SlotActionType.PICKUP);click(client,menu,0,0,SlotActionType.PICKUP);
                click(client,menu,find(menu,MedicalWorkstations.PRESCRIPTION,3),1,SlotActionType.PICKUP);click(client,menu,1,0,SlotActionType.PICKUP);next(13);
            }
            case 13->{
                if(!(client.player.currentScreenHandler instanceof MerchantScreenHandler menu)||phaseTicks<30||!menu.getSlot(2).getStack().isOf(ModItems.ANTITOXIN)||menu.getSlot(1).getStack().getCount()!=1)return false;
                shot(client,"prescription-trade");click(client,menu,2,0,SlotActionType.QUICK_MOVE);next(14);
            }
            case 14->{
                if(client.player.getInventory().count(ModItems.ANTITOXIN)!=1||client.player.getInventory().count(MedicalWorkstations.PRESCRIPTION)!=1||phaseTicks<20)return false;
                client.player.closeHandledScreen();pending=server(client,p->{scene.verifyTrade(medicinePrice);scene.prepareCure();return null;});next(15);
            }
            case 15->{
                if(pending!=null){if(!done())return false;pending.join();pending=null;}client.player.getInventory().selectedSlot=scene.antitoxinSlot;
                if(!client.player.hasStatusEffect(TetanusMod.TETANUS)||!client.player.getMainHandStack().isOf(ModItems.ANTITOXIN)||phaseTicks<30)return false;
                shot(client,"tetanus-before-cure");client.options.useKey.setPressed(true);require(client.interactionManager.interactItem(client.player,Hand.MAIN_HAND).isAccepted(),"Native timed antitoxin use did not start");next(16);
            }
            case 16->{
                if(phaseTicks<25||client.player.hasStatusEffect(TetanusMod.TETANUS))return false;client.options.useKey.setPressed(false);
                pending=server(client,p->{scene.verifyCure();return null;});next(17);
            }
            case 17->{
                if(pending!=null){if(!done())return false;pending.join();pending=null;}
                var advancement=client.player.networkHandler.getAdvancementHandler().getManager().get(TetanusMod.id("antitoxin"));if(advancement==null||phaseTicks<20)return false;
                advancements=new AdvancementsScreen(client.player.networkHandler.getAdvancementHandler());client.setScreen(advancements);next(18);
            }
            case 18->{
                if(client.currentScreen!=advancements||phaseTicks<20)return false;selectModTab(advancements);
                var tab=(AdvancementTab)field(advancements,"selectedTab");if(!(Boolean)field(tab,"initialized"))return false;
                var advancement=client.player.networkHandler.getAdvancementHandler().getManager().get(TetanusMod.id("antitoxin"));var widget=advancements.getAdvancementWidget(advancement);if(widget==null)return false;
                var progress=(AdvancementProgress)field(widget,"progress");if(progress==null||!progress.isDone())return false;
                double dx=117-widget.getX()-13-((Number)field(tab,"originX")).doubleValue(),dy=56-widget.getY()-13-((Number)field(tab,"originY")).doubleValue();int cx=(advancements.width-252)/2+120,cy=(advancements.height-140)/2+70;
                advancements.mouseDragged(cx,cy,0,0,0);advancements.mouseDragged(cx,cy,0,dx,dy);advancements.mouseReleased(cx,cy,0);
                point(client,(advancements.width-252)/2+9+(int)Math.floor(((Number)field(tab,"originX")).doubleValue())+widget.getX()+13,(advancements.height-140)/2+18+(int)Math.floor(((Number)field(tab,"originY")).doubleValue())+widget.getY()+13);evidence.put("ClientCureAdvancementComplete",true);next(19);
            }
            case 19->{if(phaseTicks>=30){shot(client,"cure-advancement");restore(client);next(20);}}
            case 20->{if(pending!=null){if(!done())return false;pending.join();pending=null;}if(!scene.restored||phaseTicks<20)return false;write(client);complete=true;return true;}
            default->throw new IllegalStateException("Unknown medical phase "+phase);
        }
        return false;
    }
    private void restore(MinecraftClient client){
        if(restoring)return;restoring=true;release(client);client.setScreen(null);client.options.setPerspective(oldPerspective);client.options.getFov().setValue(oldFov);
        if(scene!=null&&client.getServer()!=null)pending=server(client,p->{scene.restore();return null;});else pending=CompletableFuture.completedFuture(null);
        client.options.useKey.setPressed(oldUse);client.options.sneakKey.setPressed(oldSneak);
    }
    private void write(MinecraftClient client){
        require(scene.restored,"Medical fixture was not restored");
        evidence.put("Result","PASS");evidence.put("ActualGraphicalClient",true);evidence.put("ManuallyPlacedFixtureStation",true);evidence.put("ManuallySpawnedFixtureMedic",true);evidence.put("FixtureMedicAiDisabled",true);evidence.put("DirectCriterionGrants",0);evidence.put("ProgressRevocations",0);evidence.put("NativeWriterOutputsCrafted",2);evidence.put("NativePrescriptionTradePurchases",1);evidence.put("NativeAntitoxinUses",1);evidence.put("NativeSupplyDeliveries",1);evidence.put("ExistingCriterionDatesPreserved",scene.beforeDates.size());evidence.put("OriginalPlayerAndWorldStateRestored",true);evidence.put("Screenshots",shots);
        try{Files.writeString(client.runDirectory.toPath().resolve("graphical-medical-"+viewer+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(evidence));}catch(Exception problem){throw new IllegalStateException("Cannot save medical graphical proof",problem);}
    }
    private boolean done(){return pending!=null&&pending.isDone();}
    private void next(int value){phase=value;phaseTicks=0;stable=0;clicked=false;}
    private <T> CompletableFuture<T> server(MinecraftClient client,Function<ServerPlayerEntity,T> action){var future=new CompletableFuture<T>();client.getServer().execute(()->{try{var player=client.getServer().getPlayerManager().getPlayer(playerId);require(player!=null,"Connected medical actor disappeared");future.complete(action.apply(player));}catch(Throwable failure){future.completeExceptionally(failure);}});return future;}
    private static void release(MinecraftClient client){client.options.useKey.setPressed(false);client.options.sneakKey.setPressed(false);}
    private static int find(ScreenHandler menu,Item item,int start){for(int i=start;i<menu.slots.size();i++)if(menu.getSlot(i).getStack().isOf(item))return i;throw new IllegalStateException("Native menu lacks ingredient "+item);}
    private static void click(MinecraftClient client,ScreenHandler menu,int slot,int button,SlotActionType action){client.interactionManager.clickSlot(menu.syncId,slot,button,action,client.player);}
    private boolean at(MinecraftClient client,Vec3d position){return client.player.squaredDistanceTo(position)<0.04;}
    private VillagerEntity npc(MinecraftClient client){if(scene==null||scene.medic==null)return null;for(var entity:client.world.getEntities())if(entity instanceof VillagerEntity medic&&entity.getUuid().equals(scene.medic.getUuid()))return medic;return null;}
    private void shot(MinecraftClient client,String view){String name="rust-and-ruin-"+viewer+"-medical-"+view+".png";ScreenshotRecorder.saveScreenshot(client.runDirectory,name,client.getFramebuffer(),message->{});shots.add(Map.of("View",view,"Screenshot",name,"ActualNativeScreen",client.currentScreen==null?"HUD":client.currentScreen.getClass().getSimpleName()));org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_MEDICAL_VIEW {}",view);}
    private static void aim(MinecraftClient client,Vec3d target){var delta=target.subtract(client.player.getEyePos());client.player.setYaw((float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90));client.player.setPitch((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));}
    @SuppressWarnings("unchecked") private static List<ChatHudLine> chat(MinecraftClient client){return List.copyOf((List<ChatHudLine>)field(client.inGameHud.getChatHud(),"messages"));}
    private Text message(MinecraftClient client,String key){return chat(client).stream().filter(line->!oldChat.contains(line)&&line.content().getContent() instanceof TranslatableTextContent c&&c.getKey().equals(key)).map(ChatHudLine::content).findFirst().orElse(null);}
    private static Object field(Object target,String name){try{var field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);}catch(ReflectiveOperationException failure){throw new IllegalStateException("Cannot observe native "+name,failure);}}
    @SuppressWarnings("unchecked") private static void selectModTab(AdvancementsScreen screen){
        var tabs=(Map<Advancement,AdvancementTab>)field(screen,"tabs");var entry=tabs.entrySet().stream().filter(e->e.getKey().getId().equals(TetanusMod.id("root"))).findFirst().orElseThrow();if(field(screen,"selectedTab")==entry.getValue())return;
        int left=(screen.width-252)/2,top=(screen.height-140)/2;for(int y=top-30;y<top+170;y+=2)for(int x=left-30;x<left+282;x+=2)if(entry.getValue().isClickOnTab(left,top,x,y)){screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);return;}
        throw new IllegalStateException("Native Rust & Ruin tab cannot be clicked");
    }
    private static void point(MinecraftClient client,int x,int y){double rawX=x*(double)client.getWindow().getWidth()/client.getWindow().getScaledWidth(),rawY=y*(double)client.getWindow().getHeight()/client.getWindow().getScaledHeight();org.lwjgl.glfw.GLFW.glfwSetCursorPos(client.getWindow().getHandle(),rawX,rawY);try{var method=net.minecraft.client.Mouse.class.getDeclaredMethod("onCursorPos",long.class,double.class,double.class);method.setAccessible(true);method.invoke(client.mouse,client.getWindow().getHandle(),rawX,rawY);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}

    private static final class Scene {
        final ServerPlayerEntity player;final ServerWorld world;final Vec3d oldPosition;final float oldYaw,oldPitch,oldHealth;final GameMode oldMode;final boolean oldGravity,oldFlying,oldAllowFlying,oldInvulnerable;
        final NbtList oldInventory;final NbtCompound oldHunger=new NbtCompound();final List<StatusEffectInstance> oldEffects;final int oldSelected;final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();final Map<String,String> beforeDates;
        final Vec3d stationCamera=Vec3d.ofBottomCenter(STATION).add(0,0,2.5),medicCamera=Vec3d.ofBottomCenter(STATION).add(3,0,3.5);
        VillagerEntity medic;ItemStack request;int antitoxinSlot,antitoxinUsedBefore;boolean restored;
        Scene(ServerPlayerEntity player){
            this.player=player;world=player.getServerWorld();require(world.getChunkManager().getChunkGenerator() instanceof net.minecraft.world.gen.chunk.FlatChunkGenerator,"Medical fixture requires a fresh flat test world");
            var cure=player.getServer().getAdvancementLoader().get(TetanusMod.id("antitoxin"));var supply=player.getServer().getAdvancementLoader().get(TetanusMod.id("supply_run"));require(cure!=null&&supply!=null&&!player.getAdvancementTracker().getProgress(cure).isDone()&&!player.getAdvancementTracker().getProgress(supply).isDone(),"Use a fresh fixture player; medical proof never revokes existing progress");
            require(!player.hasStatusEffect(TetanusMod.TETANUS)&&!player.hasStatusEffect(TetanusMod.LOCKJAW)&&!player.hasStatusEffect(ExpansionEffects.RABIES),"Medical proof needs an initially healthy actor");
            oldPosition=player.getPos();oldYaw=player.getYaw();oldPitch=player.getPitch();oldHealth=player.getHealth();oldMode=player.interactionManager.getGameMode();oldGravity=player.hasNoGravity();oldFlying=player.getAbilities().flying;oldAllowFlying=player.getAbilities().allowFlying;oldInvulnerable=player.getAbilities().invulnerable;oldInventory=player.getInventory().writeNbt(new NbtList());oldSelected=player.getInventory().selectedSlot;player.getHungerManager().writeNbt(oldHunger);oldEffects=player.getStatusEffects().stream().map(StatusEffectInstance::new).toList();beforeDates=dates(player);
        }
        void prepare(){
            player.closeHandledScreen();
            for(int x=-3;x<=6;x++)for(int z=-3;z<=6;z++)for(int y=-1;y<=4;y++){var pos=STATION.add(x,y,z);require(world.getBlockEntity(pos)==null,"Medical fixture refuses an existing block entity at "+pos);blocks.put(pos,world.getBlockState(pos));}
            for(var pos:blocks.keySet())world.setBlockState(pos,pos.getY()==3?Blocks.STONE.getDefaultState():Blocks.AIR.getDefaultState());world.setBlockState(STATION,MedicalWorkstations.WRITER.getDefaultState().with(HorizontalFacingBlock.FACING,Direction.SOUTH));
            player.changeGameMode(GameMode.SURVIVAL);player.clearStatusEffects();player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,2400,0,false,false,false));player.setNoGravity(true);player.getAbilities().invulnerable=true;player.sendAbilitiesUpdate();player.getInventory().clear();player.getInventory().selectedSlot=0;player.getInventory().setStack(9,new ItemStack(Items.PAPER,3));player.getInventory().setStack(10,new ItemStack(Items.INK_SAC,2));player.getInventory().setStack(11,new ItemStack(Items.EMERALD,64));player.teleport(world,stationCamera.x,stationCamera.y,stationCamera.z,180,25);player.setVelocity(Vec3d.ZERO);
            for(int seed=0;seed<64;seed++){var candidate=EntityType.VILLAGER.create(world);require(candidate!=null,"Native villager construction failed");candidate.setVillagerData(candidate.getVillagerData().withProfession(MedicVillage.MEDIC).withLevel(1));candidate.setExperience(1);candidate.getRandom().setSeed(seed*97L+3);var offers=candidate.getOffers();if(!offers.isEmpty()&&offers.get(0).getSellItem().isOf(ModItems.ANTITOXIN)){medic=candidate;break;}}
            require(medic!=null,"No native randomized Nurse fixture offers antitoxin");medic.setPosition(Vec3d.ofBottomCenter(STATION).add(3,0,1));medic.setAiDisabled(true);medic.setInvulnerable(true);medic.setNoGravity(true);require(world.spawnEntity(medic),"Native Nurse fixture could not spawn");request=MedicSupplies.request(medic);player.currentScreenHandler.sendContentUpdates();
        }
        void verifyWriting(){require(player.getInventory().count(MedicalWorkstations.PRESCRIPTION)==2&&player.getInventory().count(Items.PAPER)==1&&player.getInventory().count(Items.INK_SAC)==0,"Real writer outputs/ingredients failed accounting");}
        void moveToMedic(){player.teleport(world,medicCamera.x,medicCamera.y,medicCamera.z,180,0);player.setVelocity(Vec3d.ZERO);}
        void seedDelivery(){require(player.getInventory().getStack(6).isEmpty(),"Fixture delivery slot is unexpectedly occupied");player.getInventory().setStack(6,request.copy());player.getInventory().selectedSlot=6;player.currentScreenHandler.sendContentUpdates();}
        void verifyDelivery(){require(((MedicSupplies.State)medic).rustruin$lastSupplyDay()==MedicSupplies.day(medic)&&player.getMainHandStack().isEmpty(),"Native delivery did not charge its exact stack");var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id("supply_run"));require(player.getAdvancementTracker().getProgress(advancement).isDone(),"Native supply delivery did not award its advancement");}
        void verifyTrade(int price){require(player.getInventory().count(ModItems.ANTITOXIN)==1&&player.getInventory().count(MedicalWorkstations.PRESCRIPTION)==1&&player.getInventory().count(Items.EMERALD)==64-price&&medic.getOffers().get(0).getUses()==1,"Native prescription trade failed cost/stock accounting");}
        void prepareCure(){player.closeHandledScreen();require(CommandEffects.apply(player,new StatusEffectInstance(TetanusMod.TETANUS,10000),null),"Native Tetanus fixture application failed");require(!player.hasStatusEffect(TetanusMod.LOCKJAW),"Cure fixture entered late-stage Lockjaw");antitoxinUsedBefore=player.getStatHandler().getStat(Stats.USED.getOrCreateStat(ModItems.ANTITOXIN));antitoxinSlot=-1;for(int i=0;i<9;i++)if(player.getInventory().getStack(i).isOf(ModItems.ANTITOXIN)){antitoxinSlot=i;break;}require(antitoxinSlot>=0,"Traded antitoxin did not reach native hotbar");player.getInventory().selectedSlot=antitoxinSlot;player.currentScreenHandler.sendContentUpdates();}
        void verifyCure(){require(!player.hasStatusEffect(TetanusMod.TETANUS)&&!player.hasStatusEffect(TetanusMod.LOCKJAW)&&player.getStatHandler().getStat(Stats.USED.getOrCreateStat(ModItems.ANTITOXIN))-antitoxinUsedBefore==1,"Native timed antitoxin failed cure or consumption stat");var cure=player.getServer().getAdvancementLoader().get(TetanusMod.id("antitoxin"));require(player.getAdvancementTracker().getProgress(cure).isDone(),"Actual timed early cure did not award Common Sense");}
        void restore(){
            if(restored)return;player.closeHandledScreen();if(medic!=null&&!medic.isRemoved())medic.discard();for(var entry:blocks.entrySet())world.setBlockState(entry.getKey(),entry.getValue());
            var after=dates(player);require(after.entrySet().containsAll(beforeDates.entrySet()),"Medical proof changed an existing earned criterion date");
            player.teleport(world,oldPosition.x,oldPosition.y,oldPosition.z,oldYaw,oldPitch);player.changeGameMode(oldMode);player.setNoGravity(oldGravity);player.getAbilities().flying=oldFlying;player.getAbilities().allowFlying=oldAllowFlying;player.getAbilities().invulnerable=oldInvulnerable;player.sendAbilitiesUpdate();player.getInventory().readNbt(oldInventory.copy());player.getInventory().selectedSlot=oldSelected;player.getHungerManager().readNbt(oldHunger.copy());player.clearStatusEffects();for(var effect:oldEffects)player.addStatusEffect(new StatusEffectInstance(effect));player.setHealth(oldHealth);player.currentScreenHandler.sendContentUpdates();restored=true;
        }
        static Map<String,String> dates(ServerPlayerEntity player){var result=new TreeMap<String,String>();for(var advancement:player.getServer().getAdvancementLoader().getAdvancements()){var progress=player.getAdvancementTracker().getProgress(advancement);for(String criterion:progress.getObtainedCriteria())result.put(advancement.getId()+"/"+criterion,progress.getCriterionProgress(criterion).toJson().toString());}return result;}
    }
}
