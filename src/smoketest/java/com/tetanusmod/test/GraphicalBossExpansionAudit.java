package com.tetanusmod.test;

import com.google.gson.GsonBuilder;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.beacon.*;
import com.tetanusmod.client.*;
import com.tetanusmod.dragon.DragonEncounter;
import com.tetanusmod.effect.Infection;
import com.tetanusmod.entity.*;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.texture.*;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Native beacon clicks/payments, live effect synchronization and disclosed paused visual specimens. */
public final class GraphicalBossExpansionAudit {
    private static final BlockPos BEACON=new BlockPos(380,8,96);
    private static final Vec3d BEACON_CAMERA=new Vec3d(380.5,9,93.5);
    private static final Vec3d ENDERMAN_CAMERA=new Vec3d(392.5,4,91.5);
    private static final Vec3d WITHER_CAMERA=new Vec3d(410.5,5,85.5);
    private static final Vec3d DRAGON_CAMERA=new Vec3d(445.5,9,77.5);
    private final String viewer=FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
    private final List<Map<String,Object>> shots=new ArrayList<>();
    private final Map<String,Object> evidence=new LinkedHashMap<>();
    private CompletableFuture<?> pending;
    private Scene scene;
    private UUID playerId;
    private int ticks,phase,phaseTicks,stable,oldScale,oldFov;
    private boolean started,complete,restoring,oldUse,oldSneak;
    private Perspective oldPerspective;

    public void start(MinecraftClient client) {
        require(!started&&client.player!=null&&client.getServer()!=null,"Boss expansion proof requires a connected integrated player");started=true;playerId=client.player.getUuid();
        oldPerspective=client.options.getPerspective();oldScale=client.options.getGuiScale().getValue();oldFov=client.options.getFov().getValue();oldUse=client.options.useKey.isPressed();oldSneak=client.options.sneakKey.isPressed();
        release(client);client.setScreen(null);client.options.setPerspective(Perspective.FIRST_PERSON);client.options.getGuiScale().setValue(2);client.options.getFov().setValue(70);client.onResolutionChanged();
        pending=server(client,p->{scene=new Scene(p);scene.prepare();return null;});
    }
    public boolean tick(MinecraftClient client) {try{return tickInternal(client);}catch(RuntimeException|Error problem){restore(client);throw problem;}}
    private boolean tickInternal(MinecraftClient client) {
        require(started,"Boss expansion proof was not started");if(complete)return true;require(++ticks<6000,"Boss expansion proof timeout at phase="+phase);phaseTicks++;
        if(pending!=null&&pending.isCompletedExceptionally())pending.join();
        switch(phase) {
            case 0->{if(done()){pending.join();pending=null;next(1);}}
            case 1->{if(!at(client,BEACON_CAMERA,Vec3d.ofCenter(BEACON))||!client.world.getBlockState(BEACON).isOf(ModBeacons.BLOCK)){stable=0;return false;}if(++stable<30)return false;shot(client,"beacon-pyramid");open(client);next(2);}
            case 2->{if(menu(client)==null||menu(client).getProperties()!=4||phaseTicks<25)return false;shift(client,find(menu(client),Items.NETHERITE_INGOT));button(client,Text.translatable(StatusEffects.HASTE.getTranslationKey()).getString(),true);next(23);}
            case 23->{if(menu(client)==null||phaseTicks<25)return false;button(client,tr("gui.tetanusmod.beacon.upgrade_iii"),true);next(3);}
            case 3->{var menu=menu(client);if(menu==null||!menu.getSlot(0).getStack().isOf(Items.NETHERITE_INGOT)||menu.getSlot(TetanizedBeaconScreenHandler.STAR_SLOT).hasStack()||phaseTicks<30)return false;require(activateButton(client).active,"Level III cannot activate with one ingot");shot(client,"iii-selection");button(client,tr("gui.tetanusmod.beacon.activate"),true);next(4);}
            case 4->{if(client.currentScreen!=null||!inventory(client,2,2)||!haste(client,2)||phaseTicks<100)return false;pending=server(client,p->{scene.verifySelection(false,2,2);return null;});next(5);}
            case 5->{if(!done())return false;pending.join();pending=null;shot(client,"haste-iii");open(client);next(6);}
            case 6->{var menu=menu(client);if(menu==null||menu.getProperties()!=4||phaseTicks<25)return false;shift(client,find(menu,Items.NETHERITE_INGOT));button(client,tr("gui.tetanusmod.beacon.upgrade_iv"),true);next(7);}
            case 7->{var menu=menu(client);if(menu==null||!menu.getSlot(0).hasStack()||phaseTicks<30)return false;require(!menu.hasStar()&&!activateButton(client).active,"Level IV was offered without a star payment");shot(client,"iv-star-required");shift(client,find(menu,ModItems.TETANIZED_WITHER_STAR));next(8);}
            case 8->{var menu=menu(client);if(menu==null||!menu.hasStar()||!menu.getSlot(0).hasStack()||phaseTicks<30)return false;require(activateButton(client).active,"Both payments did not enable level IV");shot(client,"iv-payments");button(client,tr("gui.tetanusmod.beacon.activate"),true);next(9);}
            case 9->{if(client.currentScreen!=null||!inventory(client,1,1)||!haste(client,3)||phaseTicks<100)return false;pending=server(client,p->{scene.verifySelection(true,1,1);return null;});next(10);}
            case 10->{if(!done())return false;pending.join();pending=null;shot(client,"haste-iv");open(client);next(11);}
            case 11->{var menu=menu(client);if(menu==null||!menu.isLevelIV()||phaseTicks<25)return false;shift(client,find(menu,Items.NETHERITE_INGOT));next(12);}
            case 12->{var menu=menu(client);if(menu==null||!menu.getSlot(0).hasStack()||phaseTicks<30)return false;require(menu.isLevelIV()&&!menu.hasStar()&&!activateButton(client).active,"Reconfirming IV omitted its additional star cost");shot(client,"iv-reconfirmation-needs-star");button(client,tr("gui.tetanusmod.beacon.upgrade_iii"),true);next(13);}
            case 13->{if(menu(client)==null||phaseTicks<30)return false;require(activateButton(client).active,"Downgrading to III still requires a star");shot(client,"iii-downgrade-selection");button(client,tr("gui.tetanusmod.beacon.activate"),true);next(14);}
            case 14->{
                // Vanilla lets the previous stronger effect expire before its weaker replacement applies.
                if(client.currentScreen!=null||!inventory(client,0,1)||!haste(client,2)||phaseTicks<100)return false;
                pending=server(client,p->{scene.verifySelection(false,0,1);return null;});next(15);
            }
            case 15->{if(!done())return false;pending.join();pending=null;shot(client,"haste-iii-after-downgrade");open(client);next(16);}
            case 16->{if(menu(client)==null||phaseTicks<25)return false;shift(client,find(menu(client),ModItems.TETANIZED_WITHER_STAR));next(17);}
            case 17->{if(menu(client)==null||!menu(client).hasStar()||phaseTicks<30)return false;shot(client,"unused-star");client.player.closeHandledScreen();next(18);}
            case 18->{if(client.currentScreen!=null||!inventory(client,0,1)||phaseTicks<30)return false;pending=server(client,p->{scene.verifyUnusedStar();scene.specimens();scene.move(ENDERMAN_CAMERA);return null;});next(19);}
            case 19->{
                if(pending!=null){if(!done())return false;pending.join();pending=null;}
                var enderman=client.world.getEntityById(scene.endermanId);if(!(enderman instanceof EndermanEntity)||!MobVariants.permanent((EndermanEntity)enderman)||!at(client,ENDERMAN_CAMERA,enderman.getBoundingBox().getCenter())){stable=0;return false;}
                if(!(client.crosshairTarget instanceof EntityHitResult hit)||hit.getEntity().getId()!=scene.endermanId){stable=0;return false;}if(++stable<30)return false;
                evidence.put("EndermanTexture",verifyEndermanTexture(client,(EndermanEntity)enderman));shot(client,"rusted-enderman");pending=server(client,p->{scene.move(WITHER_CAMERA);return null;});next(20);
            }
            case 20->{
                if(pending!=null){if(!done())return false;pending.join();pending=null;}
                var wither=client.world.getEntityById(scene.witherId);if(!(wither instanceof WitherEntity boss)||!MobVariants.permanent(boss)||Math.abs(boss.getMaxHealth()-650)>.01||!at(client,WITHER_CAMERA,boss.getBoundingBox().getCenter())){stable=0;return false;}
                var minions=new ArrayList<Map<String,Object>>();for(int id:scene.minionIds){var entity=client.world.getEntityById(id);if(!(entity instanceof WitherMinionEntity minion)||!minion.getMainHandStack().isOf(Items.IRON_SWORD)||Math.abs(minion.getMaxHealth()-20)>.01){stable=0;return false;}minions.add(Map.of("Id",id,"Health",minion.getHealth(),"MaxHealth",minion.getMaxHealth(),"BodyHeight",minion.getHeight(),"Weapon",minion.getMainHandStack().getItem().toString()));}
                if(++stable<30)return false;evidence.put("NativeWither",Map.of("MaxHealth",boss.getMaxHealth(),"CurrentHealth",boss.getHealth(),"Minions",minions));shot(client,"wither-and-minions");pending=server(client,p->{scene.removeWither();scene.move(DRAGON_CAMERA);return null;});next(21);
            }
            case 21->{
                if(pending!=null){if(!done())return false;pending.join();pending=null;}
                var entity=client.world.getEntityById(scene.dragonId);if(!(entity instanceof EnderDragonEntity dragon)||!MobVariants.permanent(dragon)||Math.abs(dragon.getMaxHealth()-DragonEncounter.HEALTH)>.01||!at(client,DRAGON_CAMERA,dragon.getPos().add(0,1.5,0))){stable=0;return false;}
                if(++stable<40)return false;evidence.put("NativeDragon",Map.of("MaxHealth",dragon.getMaxHealth(),"CurrentHealth",dragon.getHealth(),"Dimension",client.world.getRegistryKey().getValue().toString(),"Purpose","Paused model/skin fixture, not the End ritual or fight"));shot(client,"tetanized-dragon");restore(client);next(22);
            }
            case 22->{if(pending!=null){if(!done())return false;pending.join();pending=null;}if(!scene.restored||phaseTicks<20)return false;require(client.player.currentScreenHandler==client.player.playerScreenHandler&&client.player.squaredDistanceTo(scene.oldPosition)<.04,"Original client observer did not synchronize after restore");write(client);complete=true;return true;}
            default->throw new IllegalStateException("Unknown boss expansion phase "+phase);
        }
        return false;
    }
    private TetanizedBeaconScreenHandler menu(MinecraftClient client) {return client.player.currentScreenHandler instanceof TetanizedBeaconScreenHandler menu&&client.currentScreen instanceof TetanizedBeaconScreen?menu:null;}
    private static String tr(String key) {return Text.translatable(key).getString();}
    private PressableWidget activateButton(MinecraftClient client) {return widget(client,tr("gui.tetanusmod.beacon.activate"));}
    private PressableWidget widget(MinecraftClient client,String label) {require(client.currentScreen!=null,"Expected native beacon screen");for(var child:client.currentScreen.children())if(child instanceof PressableWidget button&&button.getMessage().getString().equals(label))return button;throw new IllegalStateException("Native beacon button missing: "+label);}
    private void button(MinecraftClient client,String label,boolean active) {var button=widget(client,label);var screen=client.currentScreen;require(button.active==active,"Unexpected native button enablement: "+label);double x=button.getX()+button.getWidth()/2.0,y=button.getY()+button.getHeight()/2.0;require(screen.mouseClicked(x,y,0),"Native screen did not consume button click: "+label);if(client.currentScreen==screen)screen.mouseReleased(x,y,0);}
    private void open(MinecraftClient client) {require(at(client,BEACON_CAMERA,Vec3d.ofCenter(BEACON)),"Observer left beacon interaction position");require(client.crosshairTarget instanceof BlockHitResult hit&&hit.getBlockPos().equals(BEACON)&&client.interactionManager.interactBlock(client.player,Hand.MAIN_HAND,hit).isAccepted(),"Actual block interaction did not open the beacon");}
    private static int find(ScreenHandler menu,Item item) {for(int i=1;i<37;i++)if(menu.getSlot(i).getStack().isOf(item))return i;throw new IllegalStateException("Native player slots lack "+item);}
    private static void shift(MinecraftClient client,int slot) {client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,slot,0,SlotActionType.QUICK_MOVE,client.player);}
    private static boolean inventory(MinecraftClient client,int ingots,int stars) {return client.player.getInventory().count(Items.NETHERITE_INGOT)==ingots&&client.player.getInventory().count(ModItems.TETANIZED_WITHER_STAR)==stars;}
    private static boolean haste(MinecraftClient client,int amplifier) {var effect=client.player.getStatusEffect(StatusEffects.HASTE);return effect!=null&&effect.getAmplifier()==amplifier&&client.player.hasStatusEffect(TetanusMod.VACCINATED);}
    private static boolean at(MinecraftClient client,Vec3d position,Vec3d target) {if(client.player==null||client.player.squaredDistanceTo(position)>.04)return false;var delta=target.subtract(client.player.getEyePos());client.player.setYaw((float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90));client.player.setPitch((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));return true;}
    @SuppressWarnings({"rawtypes","unchecked"})
    private Map<String,Object> verifyEndermanTexture(MinecraftClient client,EndermanEntity enderman) {
        net.minecraft.client.render.entity.EntityRenderer renderer=client.getEntityRenderDispatcher().getRenderer(enderman);Identifier original=renderer.getTexture(enderman),generated=TetanizedTextures.get(original);
        require(!generated.equals(original),"Native Enderman texture did not generate a variant");var texture=client.getTextureManager().getTexture(generated);require(texture instanceof NativeImageBackedTexture,"Generated Enderman texture is not backed by actual pixels");
        var image=((NativeImageBackedTexture)texture).getImage();require(image!=null,"Generated Enderman image disappeared");var resource=client.getResourceManager().getResource(original).orElseThrow();
        try(var stream=resource.getInputStream();var base=NativeImage.read(stream)) {
            require(base.getWidth()==image.getWidth()&&base.getHeight()==image.getHeight(),"Enderman UV dimensions changed");int changed=0,rust=0,oldRust=0;
            for(int x=0;x<image.getWidth();x++)for(int y=0;y<image.getHeight();y++){int a=base.getColor(x,y),b=image.getColor(x,y);require(a>>>24==b>>>24,"Generated Enderman texture changed alpha");if(a!=b)changed++;if(rust(b))rust++;if(rust(a))oldRust++;}
            require(changed>100&&rust>oldRust+75,"Enderman variant is still visually indistinguishable from its dark original");return Map.of("Original",original.toString(),"Generated",generated.toString(),"Width",image.getWidth(),"Height",image.getHeight(),"ChangedPixels",changed,"RustPixels",rust,"OriginalRustPixels",oldRust,"AlphaPreserved",true,"ClientCrosshairId",enderman.getId());
        }catch(java.io.IOException problem){throw new IllegalStateException("Cannot inspect native Enderman pixels",problem);}
    }
    private static boolean rust(int color) {int r=color&255,g=(color>>>8)&255,b=(color>>>16)&255;return color>>>24!=0&&r>60&&r>g*1.3&&r>b*1.5;}
    private void restore(MinecraftClient client) {if(restoring)return;restoring=true;release(client);client.setScreen(null);client.options.setPerspective(oldPerspective);client.options.getGuiScale().setValue(oldScale);client.options.getFov().setValue(oldFov);client.onResolutionChanged();if(scene!=null&&client.getServer()!=null)pending=server(client,p->{scene.restore();return null;});else pending=CompletableFuture.completedFuture(null);client.options.useKey.setPressed(oldUse);client.options.sneakKey.setPressed(oldSneak);}
    private void write(MinecraftClient client) {evidence.put("Result","PASS");evidence.put("ActualGraphicalClient",true);evidence.put("Viewer",viewer);evidence.put("BeaconUsesNativeMenuPackets",true);evidence.put("NetheriteConsumed",3);evidence.put("StarsConsumed",1);evidence.put("AdditionalStarRequiredForEachIVSelection",true);evidence.put("UnusedStarReturned",true);evidence.put("DowngradeWaitedForVanillaEffectExpiry",true);evidence.put("VisualSpecimensHaveAiPaused",true);evidence.put("BossRitualOrCombatTestedByThisController",false);evidence.put("OriginalPlayerAndWorldStateRestored",scene.restored);evidence.put("DirectCriterionGrants",0);evidence.put("ProgressRevocations",0);evidence.put("Screenshots",shots);try{Files.writeString(client.runDirectory.toPath().resolve("graphical-boss-expansion-"+viewer+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(evidence));}catch(Exception problem){throw new IllegalStateException("Cannot save boss expansion graphical proof",problem);}}
    private void shot(MinecraftClient client,String view) {String name="rust-and-ruin-"+viewer+"-boss-expansion-"+view+".png";ScreenshotRecorder.saveScreenshot(client.runDirectory,name,client.getFramebuffer(),text->{});shots.add(Map.of("View",view,"Screenshot",name,"NativeScreen",client.currentScreen==null?"HUD":client.currentScreen.getClass().getName(),"SettledTicks",phaseTicks));org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_BOSS_EXPANSION_VIEW {}",view);}
    private static void release(MinecraftClient client) {client.options.useKey.setPressed(false);client.options.sneakKey.setPressed(false);client.options.attackKey.setPressed(false);client.options.jumpKey.setPressed(false);client.options.forwardKey.setPressed(false);client.options.backKey.setPressed(false);client.options.leftKey.setPressed(false);client.options.rightKey.setPressed(false);}
    private boolean done() {return pending!=null&&pending.isDone();}
    private void next(int value) {phase=value;phaseTicks=0;stable=0;}
    private <T> CompletableFuture<T> server(MinecraftClient client,Function<ServerPlayerEntity,T> action) {var future=new CompletableFuture<T>();client.getServer().execute(()->{try{var player=client.getServer().getPlayerManager().getPlayer(playerId);require(player!=null,"Boss expansion actor disconnected");future.complete(action.apply(player));}catch(Throwable problem){future.completeExceptionally(problem);}});return future;}
    private static void require(boolean value,String message) {if(!value)throw new IllegalStateException(message);}
    private static final class Scene {
        final ServerPlayerEntity player;final ServerWorld world;final Vec3d oldPosition;final float yaw,pitch,health;final GameMode mode;final boolean gravity,flying,allowFlying,invulnerable,griefing;
        final NbtList inventory;final int selected;final NbtCompound hunger=new NbtCompound();final List<StatusEffectInstance> effects;final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();final List<Entity> specimens=new ArrayList<>();
        TetanizedBeaconEntity beacon;int endermanId,witherId,dragonId;List<Integer> minionIds=List.of();boolean restored;
        Scene(ServerPlayerEntity player) {this.player=player;world=player.getServerWorld();require(world.getChunkManager().getChunkGenerator() instanceof net.minecraft.world.gen.chunk.FlatChunkGenerator,"Boss expansion proof requires a fresh flat test world");require(world.getDifficulty()!=Difficulty.PEACEFUL,"Boss visual specimens need a non-Peaceful fixture world");require(!player.hasStatusEffect(TetanusMod.TETANUS)&&!player.hasStatusEffect(TetanusMod.LOCKJAW)&&!player.hasStatusEffect(com.tetanusmod.expansion.ExpansionEffects.RABIES),"Boss expansion actor must start without disease");oldPosition=player.getPos();yaw=player.getYaw();pitch=player.getPitch();health=player.getHealth();mode=player.interactionManager.getGameMode();gravity=player.hasNoGravity();flying=player.getAbilities().flying;allowFlying=player.getAbilities().allowFlying;invulnerable=player.getAbilities().invulnerable;inventory=player.getInventory().writeNbt(new NbtList());selected=player.getInventory().selectedSlot;player.getHungerManager().writeNbt(hunger);effects=player.getStatusEffects().stream().map(StatusEffectInstance::new).toList();griefing=world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);}
        void set(BlockPos pos,BlockState state) {if(!blocks.containsKey(pos)){require(world.getBlockEntity(pos)==null,"Boss fixture refuses an existing block entity at "+pos);blocks.put(pos,world.getBlockState(pos));}world.setBlockState(pos,state);}
        void prepare() {player.closeHandledScreen();world.getGameRules().get(GameRules.DO_MOB_GRIEFING).set(false,world.getServer());for(int layer=1;layer<=4;layer++)for(int x=-layer;x<=layer;x++)for(int z=-layer;z<=layer;z++)set(BEACON.add(x,-layer,z),com.tetanusmod.expansion.ExpansionBlocks.RUSTED_IRON_BLOCK.getDefaultState());set(BEACON,ModBeacons.BLOCK.getDefaultState());beacon=(TetanizedBeaconEntity)world.getBlockEntity(BEACON);player.changeGameMode(GameMode.CREATIVE);player.clearStatusEffects();player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,6000,0,false,false,false));player.setNoGravity(true);player.getAbilities().flying=true;player.getAbilities().invulnerable=true;player.sendAbilitiesUpdate();player.getInventory().clear();player.getInventory().selectedSlot=0;player.getInventory().setStack(9,new ItemStack(Items.NETHERITE_INGOT,3));player.getInventory().setStack(10,new ItemStack(ModItems.TETANIZED_WITHER_STAR,2));move(BEACON_CAMERA);player.currentScreenHandler.sendContentUpdates();}
        void move(Vec3d position) {player.teleport(world,position.x,position.y,position.z,180,0);player.setVelocity(Vec3d.ZERO);}
        void verifySelection(boolean iv,int ingots,int stars) {require(player.currentScreenHandler==player.playerScreenHandler&&player.getInventory().count(Items.NETHERITE_INGOT)==ingots&&player.getInventory().count(ModItems.TETANIZED_WITHER_STAR)==stars,"Native beacon payment differs from the client inventory");require(beacon.isLevelIV()==iv,"Native beacon retained the wrong paid upgrade");var menu=(TetanizedBeaconScreenHandler)beacon.createMenu(912,player.getInventory(),player);require(menu.getPrimaryEffect()==StatusEffects.HASTE&&menu.getSecondaryEffect()==StatusEffects.HASTE,"Native beacon power selection differs from its GUI");var effect=player.getStatusEffect(StatusEffects.HASTE);require(effect!=null&&effect.getAmplifier()==(iv?3:2),"Native beacon did not synchronize its selected strength");}
        void verifyUnusedStar() {require(player.currentScreenHandler==player.playerScreenHandler&&player.getInventory().count(Items.NETHERITE_INGOT)==0&&player.getInventory().count(ModItems.TETANIZED_WITHER_STAR)==1,"Actual client close lost its unused star or duplicated payment");}
        void specimens() {
            for(int x=389;x<=395;x++)for(int z=93;z<=99;z++){set(new BlockPos(x,3,z),Blocks.SMOOTH_STONE.getDefaultState());set(new BlockPos(x,9,z),Blocks.SMOOTH_STONE.getDefaultState());}
            for(int x=389;x<=395;x++)for(int y=4;y<=8;y++)set(new BlockPos(x,y,99),Blocks.QUARTZ_BLOCK.getDefaultState());
            var enderman=EntityType.ENDERMAN.create(world);require(enderman!=null,"Native Enderman creation failed");enderman.setPosition(392.5,4,96.5);enderman.setAiDisabled(true);enderman.setNoGravity(true);Infection.silently(()->MobVariants.makePermanent(enderman));spawn(enderman);endermanId=enderman.getId();
            var wither=EntityType.WITHER.create(world);require(wither!=null,"Native Wither creation failed");wither.setPosition(410.5,7,96.5);wither.setAiDisabled(true);wither.setNoGravity(true);wither.setInvulTimer(0);Infection.silently(()->MobVariants.makePermanent(wither));require(wither.getMaxHealth()==650,"Native Wither has the wrong health");spawn(wither);witherId=wither.getId();var ids=new ArrayList<Integer>();
            for(double x:new double[]{408.5,412.5}){var minion=WitherMinions.TYPE.create(world);require(minion!=null,"Native minion creation failed");minion.setPosition(x,4,95.5);minion.setAiDisabled(true);minion.setNoGravity(true);minion.assignBoss(wither.getUuid(),world.getTime()+WitherBossEncounter.MINION_LIFETIME);spawn(minion);ids.add(minion.getId());}minionIds=List.copyOf(ids);
            var dragon=EntityType.ENDER_DRAGON.create(world);require(dragon!=null,"Native dragon creation failed");dragon.setPosition(445.5,11,96.5);dragon.getPhaseManager().setPhase(PhaseType.HOVER);dragon.setAiDisabled(true);dragon.setNoGravity(true);Infection.silently(()->MobVariants.makePermanent(dragon));require(dragon.getMaxHealth()==DragonEncounter.HEALTH,"Native dragon has the wrong health");spawn(dragon);dragonId=dragon.getId();
        }
        void spawn(Entity entity) {require(world.spawnEntity(entity),"Native specimen spawn failed: "+entity.getType());specimens.add(entity);}
        void removeWither() {for(var entity:specimens)if(entity instanceof WitherEntity||entity instanceof WitherMinionEntity)entity.discard();}
        void restore() {if(restored)return;player.closeHandledScreen();for(var entity:specimens)if(!entity.isRemoved())entity.discard();for(var entry:blocks.entrySet())world.setBlockState(entry.getKey(),entry.getValue());world.getGameRules().get(GameRules.DO_MOB_GRIEFING).set(griefing,world.getServer());player.teleport(world,oldPosition.x,oldPosition.y,oldPosition.z,yaw,pitch);player.changeGameMode(mode);player.setNoGravity(gravity);player.getAbilities().flying=flying;player.getAbilities().allowFlying=allowFlying;player.getAbilities().invulnerable=invulnerable;player.sendAbilitiesUpdate();player.getInventory().readNbt(inventory.copy());player.getInventory().selectedSlot=selected;player.getHungerManager().readNbt(hunger.copy());player.clearStatusEffects();for(var effect:effects)player.addStatusEffect(new StatusEffectInstance(effect));player.setHealth(health);player.currentScreenHandler.sendContentUpdates();restored=true;}
    }
}
