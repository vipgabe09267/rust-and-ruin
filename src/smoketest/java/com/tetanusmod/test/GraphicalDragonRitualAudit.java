package com.tetanusmod.test;

import com.google.gson.GsonBuilder;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.dragon.*;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.*;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.enchantment.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.*;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Real portal rituals and native client attack/place packets in a disposable graphical test world. */
public final class GraphicalDragonRitualAudit {
    private static final List<Direction> SIDES=List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST);
    // Native fountain radius2 is portal; crystals stand on radius3 bedrock and overlap the fight's radius2 query boxes.
    private final String viewer=FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
    private final Map<String,Object> evidence=new LinkedHashMap<>();
    private final List<Map<String,Object>> screenshots=new ArrayList<>(),rituals=new ArrayList<>();
    private final Set<String> animationShots=new HashSet<>();
    private CompletableFuture<?> pending;
    private Scene scene;
    private Sample sample;
    private UUID playerId;
    private int phase,phaseTicks,ticks,index,stable,attackPackets;
    private long ritualStarted;
    private boolean special,started,complete,restoring,deathShot,grabShot;
    private GrabSample grabSample;
    private record GrabSample(boolean carried,boolean observed,boolean released,double lift,double fall,long cooldownAtGrab,int attempts){}
    private Perspective oldPerspective;
    private int oldFov;
    private boolean oldTooltips;
    private record Sample(String animation,int dragonId,float health,float maximum,boolean permanent,int deathTicks,boolean removed,float contribution,int xp,int swords,int eggs,boolean rewardsReady,Vec3d camera){}

    public void start(MinecraftClient client){
        require(!started&&client.player!=null&&client.getServer()!=null,"Dragon proof requires a connected integrated player");
        started=true;playerId=client.player.getUuid();oldPerspective=client.options.getPerspective();oldFov=client.options.getFov().getValue();oldTooltips=client.options.advancedItemTooltips;
        client.setScreen(null);client.options.useKey.setPressed(false);client.options.attackKey.setPressed(false);client.options.setPerspective(Perspective.FIRST_PERSON);client.options.getFov().setValue(70);client.options.advancedItemTooltips=true;
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{
            if(scene!=null&&scene.collectingGrab&&scene.player.getServer()==server)scene.recordActualGrabTick();
        });
        pending=server(client,p->{scene=new Scene(p);scene.prepare(false);return null;});
    }
    public boolean tick(MinecraftClient client){try{return tickInternal(client);}catch(RuntimeException|Error failure){restore(client);throw failure;}}
    private boolean tickInternal(MinecraftClient client){
        require(started,"Dragon proof not started");if(complete)return true;require(++ticks<11000,"Dragon graphical proof timed out phase="+phase+" special="+special+" sample="+sample);phaseTicks++;
        if(pending!=null&&pending.isCompletedExceptionally())pending.join();
        switch(phase){
            case 0->{if(done()){pending.join();pending=null;index=0;next(1);}}
            case 1->{if(pending==null)pending=server(client,p->{scene.positionForCrystal(index);return null;});if(done()){pending.join();pending=null;next(2);}}
            case 2->{
                require(phaseTicks<400,"Crystal fixture did not synchronize a reachable clear rim: camera="+scene.camera+" actual="+client.player.getPos()+" crosshair="+client.crosshairTarget+" portal="+scene.portal);
                client.player.getInventory().selectedSlot=!special&&index==3?1:0;
                var base=scene.crystalBase(index);
                if(!readyAt(client,scene.camera,base)||!client.player.getMainHandStack().isOf(!special&&index==3?Items.END_CRYSTAL:DragonContent.CRYSTAL)||phaseTicks<25)return false;
                if(++stable<10)return false;require(client.interactionManager.interactBlock(client.player,Hand.MAIN_HAND,(BlockHitResult)client.crosshairTarget).isAccepted(),"Actual crystal block click rejected at "+base);client.player.swingHand(Hand.MAIN_HAND);
                pending=null;next(3);
            }
            case 3->{if(pending==null&&phaseTicks>=15)pending=server(client,p->{scene.verifyCrystal(index,special);return null;});if(done()&&phaseTicks>=15){pending.join();pending=null;if(++index<4)next(1);else{ritualStarted=scene.end.getTime();animationShots.clear();shot(client,special?"special-four-crystals":"mixed-four-crystals");next(4);}}}
            case 4->{
                if(pending==null&&phaseTicks%10==0)pending=server(client,p->scene.sample());
                if(done()){
                    sample=(Sample)pending.join();pending=null;
                    if(!sample.animation().equals("NONE")&&phaseTicks>=40&&animationShots.add(sample.animation()))shot(client,(special?"special-":"mixed-")+sample.animation().toLowerCase(Locale.ROOT));
                    if(sample.dragonId()>=0){require(scene.end.getTime()-ritualStarted>=400,"Dragon skipped native respawn animation");pending=server(client,p->{scene.freezeSpawned(special);return scene.sample();});next(5);}
                }
                require(phaseTicks<3200,"Canonical End portal ritual did not finish: "+sample);
            }
            case 5->{
                if(pending!=null){if(!done())return false;sample=(Sample)pending.join();pending=null;}
                if(!readyDragon(client)||phaseTicks<35)return false;
                require(sample.maximum()==(special?1100:200)&&sample.permanent()==special,"Ritual variant or health mismatch: "+sample);
                if(special&&!clientDone(client,"dragon_summoned"))return false;
                require(special||!clientDone(client,"dragon_summoned"),"Mixed ritual falsely awarded special summon advancement");
                shot(client,special?"special-dragon-1100hp":"mixed-normal-dragon-200hp");
                rituals.add(Map.of("Special",special,"NativeHealth",sample.maximum(),"NativeRespawnTicks",scene.end.getTime()-ritualStarted,"ClientSummonAdvancement",clientDone(client,"dragon_summoned"),"PlacementPackets",4,"NativePortal",scene.portal.toShortString(),"ObservedAnimationStates",List.copyOf(animationShots)));
                if(!special){special=true;pending=server(client,p->{scene.prepare(true);return null;});next(0);}else{pending=server(client,p->{scene.prepareGrab();return null;});next(17);}
            }
            case 17->{if(done()){pending.join();pending=null;client.options.setPerspective(Perspective.THIRD_PERSON_BACK);next(18);}}
            case 18->{
                if(pending==null&&phaseTicks%4==0)pending=server(client,p->scene.advanceGrab());
                if(done()){
                    grabSample=(GrabSample)pending.join();pending=null;
                    if(grabSample.carried()&&client.player.hasVehicle()&&!grabShot){shot(client,"native-mouth-carry");grabShot=true;}
                    if(grabSample.released()&&!client.player.hasVehicle()&&grabSample.fall()>.15){
                        require(grabShot&&grabSample.lift()>=9.5&&grabSample.lift()<=10.75&&grabSample.cooldownAtGrab()>=2399,"Native grab/lift/drop/cooldown mismatch: "+grabSample);
                        shot(client,"native-mouth-drop");evidence.put("AutomaticNativeGrab",Map.of("LiftBlocks",grabSample.lift(),"ObservedFallBlocks",grabSample.fall(),"CooldownAtGrab",grabSample.cooldownAtGrab(),"SeededApproachAttempts",grabSample.attempts(),"DirectTryGrabCalls",0));
                        client.options.setPerspective(Perspective.FIRST_PERSON);pending=server(client,p->{scene.prepareAttack(false);return scene.sample();});next(6);
                    }
                }
                require(phaseTicks<1800,"Native automatic grab/drop did not complete: "+grabSample);
            }
            case 6->{
                if(pending!=null){if(!done())return false;sample=(Sample)pending.join();pending=null;}
                if(!readyAttack(client)||phaseTicks<25)return false;
                if(phaseTicks%24==0){attack(client);pending=server(client,p->scene.sample());next(7);}
            }
            case 7->{
                if(!done()||phaseTicks<12)return false;sample=(Sample)pending.join();pending=null;
                require(sample.health()>0,"Contribution fixture accidentally killed the dragon before proof");
                if(sample.contribution()<20){require(attackPackets<12,"Real graphical head attacks did not reach20accepted damage: "+sample);next(6);}
                else{shot(client,"native-contribution-at-least20");evidence.put("ActualAcceptedDamage",sample.contribution());pending=server(client,p->{scene.prepareAttack(true);return scene.sample();});next(8);}
            }
            case 8->{
                if(pending!=null){if(!done())return false;sample=(Sample)pending.join();pending=null;}
                if(!readyAttack(client)||phaseTicks<30)return false;
                attack(client);pending=null;next(9);
            }
            case 9->{
                if(pending==null&&phaseTicks>=12)pending=server(client,p->scene.sample());if(!done()||phaseTicks<12)return false;sample=(Sample)pending.join();pending=null;
                if(sample.health()>0){require(phaseTicks<100,"Finishing native client attack failed: "+sample);pending=server(client,p->{scene.prepareAttack(true);return scene.sample();});next(8);return false;}
                pending=server(client,p->{scene.deathCamera();return scene.sample();});next(10);
            }
            case 10->{
                if(pending==null&&phaseTicks%10==0)pending=server(client,p->scene.sample());
                if(done()){
                    sample=(Sample)pending.join();pending=null;
                    if(sample.deathTicks()>=180&&!deathShot){shot(client,"native-dragon-death-animation");deathShot=true;}
                    if(sample.removed()){
                        require(sample.deathTicks()>=200&&sample.xp()==500&&sample.swords()==1&&sample.eggs()==1,"Native repeat-dragon XP/loot mismatch: "+sample);
                        if(!clientDone(client,"dragon_defeated")||!sample.rewardsReady())return false;
                        require(DragonVictoryAudioProbe.count==1&&!DragonVictoryAudioProbe.relative&&"LINEAR".equals(DragonVictoryAudioProbe.attenuation),"Dragon victory cue missing, duplicated or non-positional: "+DragonVictoryAudioProbe.count);
                        evidence.put("ResolvedDragonVictorySound",Map.of("SoundId","tetanusmod:dragon_defeated","Count",DragonVictoryAudioProbe.count,"Relative",DragonVictoryAudioProbe.relative,"Attenuation",DragonVictoryAudioProbe.attenuation,"Position",DragonVictoryAudioProbe.position.toString()));
                        shot(client,"native-dragon-defeated");evidence.put("NativeXp",sample.xp());evidence.put("NativeDeathTicks",sample.deathTicks());evidence.put("CustomSwordDrops",sample.swords());evidence.put("CustomEggDrops",sample.eggs());pending=server(client,p->{scene.collectRewards();return null;});next(11);
                    }
                }
                require(phaseTicks<450,"Native200tick death animation failed to finish: "+sample);
            }
            case 11->{
                if(pending!=null){if(!done())return false;pending.join();pending=null;}
                if(client.player.getInventory().count(DragonContent.SWORD)!=1||client.player.getInventory().count(DragonContent.EGG_ITEM)!=1||phaseTicks<30)return false;
                var sword=client.player.getInventory().main.stream().filter(s->s.isOf(DragonContent.SWORD)).findFirst().orElseThrow();verifySword(sword);client.setScreen(new InventoryScreen(client.player));next(12);
            }
            case 12->{
                if(!(client.currentScreen instanceof InventoryScreen)||phaseTicks<20)return false;
                var slot=client.player.playerScreenHandler.slots.stream().filter(s->s.getStack().isOf(DragonContent.SWORD)).findFirst().orElseThrow();point(client,(client.getWindow().getScaledWidth()-176)/2+slot.x+8,(client.getWindow().getScaledHeight()-166)/2+slot.y+8);
                if(phaseTicks>=40){shot(client,"earned-overload-sword-tooltip");client.player.closeHandledScreen();pending=server(client,p->{scene.prepareEgg();return null;});next(13);}
            }
            case 13->{
                require(phaseTicks<300,"Egg placement camera did not synchronize: camera="+scene.camera+" actual="+client.player.getPos()+" base="+scene.eggBase);
                if(pending!=null){if(!done())return false;pending.join();pending=null;}
                client.player.getInventory().selectedSlot=scene.eggSlot;
                if(!readyAt(client,scene.camera,scene.eggBase)||!client.player.getMainHandStack().isOf(DragonContent.EGG_ITEM)||phaseTicks<25)return false;
                require(client.interactionManager.interactBlock(client.player,Hand.MAIN_HAND,(BlockHitResult)client.crosshairTarget).isAccepted(),"Actual earned egg placement rejected");client.player.swingHand(Hand.MAIN_HAND);next(14);
            }
            case 14->{
                if(!client.world.getBlockState(scene.eggBase.up()).isOf(DragonContent.EGG)||phaseTicks<30)return false;
                require(client.player.getInventory().count(DragonContent.EGG_ITEM)==0,"Placed reward egg was not consumed");shot(client,"earned-tetanized-egg-placed");pending=server(client,p->{require(scene.end.getBlockState(scene.eggBase.up()).isOf(DragonContent.EGG),"Server did not retain actual reward egg block");return null;});next(15);
            }
            case 15->{if(done()){pending.join();pending=null;restore(client);next(16);}}
            case 16->{if(pending!=null){if(!done())return false;pending.join();pending=null;}if(scene.restored&&phaseTicks>=25){write(client);complete=true;return true;}}
            default->throw new IllegalStateException("Unknown dragon graphical phase "+phase);
        }
        return false;
    }
    private boolean readyDragon(MinecraftClient client){if(!client.world.getRegistryKey().equals(World.END)||client.player.squaredDistanceTo(scene.camera)>.06)return false;var entity=client.world.getEntityById(sample.dragonId());if(!(entity instanceof EnderDragonEntity d)||d.getMaxHealth()!=sample.maximum())return false;look(client,d.getPos().add(0,2,0));return true;}
    private boolean readyAttack(MinecraftClient client){if(!client.world.getRegistryKey().equals(World.END)||client.player.squaredDistanceTo(scene.camera)>.06||!client.player.getMainHandStack().isOf(Items.NETHERITE_SWORD))return false;var entity=client.world.getEntityById(sample.dragonId());if(!(entity instanceof EnderDragonEntity d)||client.player.squaredDistanceTo(d.head)>24)return false;look(client,d.head.getBoundingBox().getCenter());return true;}
    private void attack(MinecraftClient client){var d=(EnderDragonEntity)client.world.getEntityById(sample.dragonId());require(d!=null,"Client lost the real ritual dragon");client.interactionManager.attackEntity(client.player,d.head);client.player.swingHand(Hand.MAIN_HAND);attackPackets++;}
    private static boolean readyAt(MinecraftClient client,Vec3d camera,BlockPos base){if(!client.world.getRegistryKey().equals(World.END)||client.player.squaredDistanceTo(camera)>.06)return false;look(client,Vec3d.ofCenter(base).add(0,.5,0));return client.crosshairTarget instanceof BlockHitResult hit&&hit.getBlockPos().equals(base);}
    private static void look(MinecraftClient client,Vec3d target){var delta=target.subtract(client.player.getEyePos());client.player.setYaw((float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90));client.player.setPitch((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));}
    @SuppressWarnings("unchecked") private static boolean clientDone(MinecraftClient client,String name){var manager=client.player.networkHandler.getAdvancementHandler();var advancement=manager.getManager().get(TetanusMod.id(name));if(advancement==null)return false;try{var field=ClientAdvancementManager.class.getDeclaredField("advancementProgresses");field.setAccessible(true);var progress=((Map<Advancement,AdvancementProgress>)field.get(manager)).get(advancement);return progress!=null&&progress.isDone();}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}}
    private static void verifySword(ItemStack sword){var modifiers=sword.getAttributeModifiers(EquipmentSlot.MAINHAND);double damage=1+modifiers.get(EntityAttributes.GENERIC_ATTACK_DAMAGE).stream().mapToDouble(m->m.getValue()).sum(),speed=4+modifiers.get(EntityAttributes.GENERIC_ATTACK_SPEED).stream().mapToDouble(m->m.getValue()).sum();require(Math.abs(damage-12)<.0001&&Math.abs(speed-2.3)<.0001&&sword.getMaxDamage()==4062&&EnchantmentHelper.getLevel(ModItems.TETANUS_ENCHANTMENT,sword)==2,"Earned sword stats/enchantment mismatch");}
    private void restore(MinecraftClient client){if(restoring)return;restoring=true;client.options.useKey.setPressed(false);client.options.attackKey.setPressed(false);client.setScreen(null);client.options.setPerspective(oldPerspective);client.options.getFov().setValue(oldFov);client.options.advancedItemTooltips=oldTooltips;if(scene!=null&&client.getServer()!=null)pending=server(client,p->{scene.restore();return null;});else pending=CompletableFuture.completedFuture(null);}
    private void write(MinecraftClient client){evidence.put("Result","PASS");evidence.put("ActualGraphicalClient",true);evidence.put("CanonicalEndFight",true);evidence.put("NativeFullHealthVerified",1100);evidence.put("NativeGrabChance",DragonEncounter.GRAB_CHANCE);evidence.put("IntendedMouthLiftBlocks",10);evidence.put("GrabMeasurement","Actual passenger position on every native server tick, measured from the successful-grab mouth height");evidence.put("CreativeStyleSurvivalInvulnerableFixture",true);evidence.put("CameraFlightFixture",true);evidence.put("CameraFlightDisabledForActualGrabAndFall",true);evidence.put("ContributionWeaponFixture","Vanilla netherite sword with SharpnessV; actual client head-attack packets");evidence.put("FinisherHealthReducedToOne",true);evidence.put("NotBalancePlaythrough",true);evidence.put("NativePortalRituals",rituals);evidence.put("ActualClientAttackPackets",attackPackets);evidence.put("SwordDamage",12);evidence.put("SwordAttackSpeed",2.3);evidence.put("SwordDurability",4062);evidence.put("SwordPreEnchant","TetanusII");evidence.put("EarnedEggActuallyPlaced",true);evidence.put("DirectCriterionGrants",0);evidence.put("ProgressRevocations",0);evidence.put("RespawnStateTimerWrites",0);evidence.put("Fixture","Prior normal dragon defeated; canonical generated exit portal in a fresh disposable test End. Native pillar regeneration intentionally changes test-world terrain.");evidence.put("OriginalPlayerStateRestored",scene.restored);evidence.put("Screenshots",screenshots);try{Files.writeString(client.runDirectory.toPath().resolve("graphical-dragon-ritual-"+viewer+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(evidence));}catch(Exception failure){throw new IllegalStateException("Cannot save graphical dragon evidence",failure);}}
    private void shot(MinecraftClient client,String view){String name="rust-and-ruin-"+viewer+"-dragon-"+view+".png";ScreenshotRecorder.saveScreenshot(client.runDirectory,name,client.getFramebuffer(),text->{});screenshots.add(Map.of("View",view,"Screenshot",name,"NativeScreen",client.currentScreen==null?"HUD":client.currentScreen.getClass().getName()));org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_DRAGON_VIEW {}",view);}
    private static void point(MinecraftClient client,int x,int y){double rx=x*(double)client.getWindow().getWidth()/client.getWindow().getScaledWidth(),ry=y*(double)client.getWindow().getHeight()/client.getWindow().getScaledHeight();org.lwjgl.glfw.GLFW.glfwSetCursorPos(client.getWindow().getHandle(),rx,ry);try{var method=net.minecraft.client.Mouse.class.getDeclaredMethod("onCursorPos",long.class,double.class,double.class);method.setAccessible(true);method.invoke(client.mouse,client.getWindow().getHandle(),rx,ry);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}}
    private boolean done(){return pending!=null&&pending.isDone();}
    private void next(int value){phase=value;phaseTicks=0;stable=0;}
    private <T> CompletableFuture<T> server(MinecraftClient client,Function<ServerPlayerEntity,T> action){var result=new CompletableFuture<T>();client.getServer().execute(()->{try{var p=client.getServer().getPlayerManager().getPlayer(playerId);require(p!=null,"Graphical dragon actor disconnected");result.complete(action.apply(p));}catch(Throwable failure){result.completeExceptionally(failure);}});return result;}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private static Object field(EnderDragonFight fight,String name){try{var field=EnderDragonFight.class.getDeclaredField(name);field.setAccessible(true);return field.get(fight);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}}
    private static void field(EnderDragonFight fight,String name,Object value){try{var field=EnderDragonFight.class.getDeclaredField(name);field.setAccessible(true);field.set(fight,value);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}}
    private static final class Scene {
        final ServerPlayerEntity player;final ServerWorld oldWorld,end;final EnderDragonFight fight;final Vec3d oldPosition;final float oldYaw,oldPitch,oldHealth;final GameMode oldMode;final boolean oldGravity,oldFlying,oldAllowFlying,oldInvulnerable;final NbtList inventory;final NbtCompound hunger=new NbtCompound();final List<StatusEffectInstance> effects;final int selected,oldLevel,oldTotal;final float oldProgress;
        Vec3d camera;BlockPos portal,eggBase;EnderDragonEntity dragon;int eggSlot,grabAttempts;boolean restored,grabObserved,grabReleased,collectingGrab;double grabStartY,grabMaximum,releaseHeight;long grabCooldown;
        Scene(ServerPlayerEntity p){player=p;oldWorld=p.getServerWorld();require(oldWorld.getChunkManager().getChunkGenerator() instanceof net.minecraft.world.gen.chunk.FlatChunkGenerator,"Dragon proof requires a fresh disposable flat test world");end=p.getServer().getWorld(World.END);require(end!=null,"End dimension unavailable");fight=end.getEnderDragonFight();require(fight!=null,"Canonical End fight unavailable");for(String id:List.of("dragon_summoned","dragon_defeated")){var a=p.getServer().getAdvancementLoader().get(TetanusMod.id(id));require(a!=null&&!p.getAdvancementTracker().getProgress(a).isDone(),"Use a fresh graphical dragon actor; existing progress is never revoked");}oldPosition=p.getPos();oldYaw=p.getYaw();oldPitch=p.getPitch();oldHealth=p.getHealth();oldMode=p.interactionManager.getGameMode();oldGravity=p.hasNoGravity();oldFlying=p.getAbilities().flying;oldAllowFlying=p.getAbilities().allowFlying;oldInvulnerable=p.getAbilities().invulnerable;inventory=p.getInventory().writeNbt(new NbtList());selected=p.getInventory().selectedSlot;oldLevel=p.experienceLevel;oldTotal=p.totalExperience;oldProgress=p.experienceProgress;p.getHungerManager().writeNbt(hunger);effects=p.getStatusEffects().stream().map(StatusEffectInstance::new).toList();}
        void prepare(boolean special){
            player.closeHandledScreen();if(dragon!=null){dragon.discard();dragon=null;}end.getChunk(0,0);
            for(var entity:end.getOtherEntities(null,new Box(-128,0,-128,128,256,128),e->e instanceof EnderDragonEntity||e instanceof EndCrystalEntity||e instanceof ExperienceOrbEntity||e instanceof ItemEntity))entity.discard();
            // Only the pre-existing vanilla kill/portal fixture is initialized reflectively; the ritual and timers run natively.
            field(fight,"dragonKilled",true);field(fight,"previouslyKilled",true);field(fight,"doLegacyCheck",false);field(fight,"dragonUuid",null);field(fight,"dragonSpawnState",null);field(fight,"crystals",null);
            // Inspect the complete footprint, ignoring the old artificial fountain column when finding exposed End terrain.
            var origin=(BlockPos)field(fight,"origin");int surface=end.getBottomY()+1;
            for(int dx=-7;dx<=7;dx++)for(int dz=-7;dz<=7;dz++){
                var column=new BlockPos.Mutable(origin.getX()+dx,end.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,origin.getX()+dx,origin.getZ()+dz)-1,origin.getZ()+dz);
                while(column.getY()>end.getBottomY()&&(end.getBlockState(column).isAir()||end.getBlockState(column).isOf(Blocks.BEDROCK)||end.getBlockState(column).isOf(Blocks.END_PORTAL)))column.move(Direction.DOWN);
                surface=Math.max(surface,column.getY()+1);
            }
            field(fight,"exitPortalLocation",new BlockPos(origin.getX(),surface-1,origin.getZ()));
            try{var method=EnderDragonFight.class.getDeclaredMethod("generateEndPortal",boolean.class);method.setAccessible(true);method.invoke(fight,true);}catch(ReflectiveOperationException failure){throw new IllegalStateException("Cannot prepare native prior-kill portal fixture",failure);}
            portal=((BlockPos)field(fight,"exitPortalLocation")).toImmutable();
            require(end.getBlockState(crystalBase(0)).isOf(Blocks.BEDROCK),"Native exit portal has no radius3 crystal base: "+end.getBlockState(crystalBase(0))+" at "+crystalBase(0)+"; radius2="+end.getBlockState(portal.offset(Direction.NORTH,2)));
            require(end.getGameRules().getBoolean(GameRules.DO_MOB_LOOT),"Graphical dragon loot proof requires native mob loot enabled");player.changeGameMode(GameMode.SURVIVAL);player.clearStatusEffects();player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,12000,0,false,false,false));cameraFlight();player.getAbilities().invulnerable=true;player.sendAbilitiesUpdate();player.getInventory().clear();player.getInventory().selectedSlot=0;player.getInventory().setStack(0,new ItemStack(DragonContent.CRYSTAL,special?4:3));if(!special)player.getInventory().setStack(1,new ItemStack(Items.END_CRYSTAL));positionForCrystal(0);player.currentScreenHandler.sendContentUpdates();
        }
        void cameraFlight(){player.setNoGravity(true);player.getAbilities().allowFlying=true;player.getAbilities().flying=true;player.sendAbilitiesUpdate();player.setVelocity(Vec3d.ZERO);}
        BlockPos crystalBase(int i){return portal.offset(SIDES.get(i),3);}
        void positionForCrystal(int i){
            cameraFlight();
            var side=SIDES.get(i);var base=crystalBase(i);require(end.isAir(base.up())&&end.isAir(base.up(2)),"Native fountain crystal clearance is blocked above "+base+": "+end.getBlockState(base.up())+" / "+end.getBlockState(base.up(2)));
            camera=cameraForBlock(base,List.of(side));
            player.teleport(end,camera.x,camera.y,camera.z,0,0);player.setVelocity(Vec3d.ZERO);
        }
        Vec3d cameraForBlock(BlockPos base,List<Direction> sides){
            for(var side:sides)for(double distance:new double[]{2.5,2,1.5}){
                var horizontal=Vec3d.ofBottomCenter(base.up()).add(side.getOffsetX()*distance,0,side.getOffsetZ()*distance);double y=Math.max(base.getY()+1,end.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,MathHelper.floor(horizontal.x),MathHelper.floor(horizontal.z)));var position=new Vec3d(horizontal.x,y,horizontal.z);
                if(position.add(0,player.getStandingEyeHeight(),0).squaredDistanceTo(Vec3d.ofCenter(base).add(0,.5,0))>20.25)continue;
                if(end.getBlockCollisions(player,player.getBoundingBox().offset(position.subtract(player.getPos()))).iterator().hasNext())continue;
                var hit=end.raycast(new RaycastContext(position.add(0,player.getStandingEyeHeight(),0),Vec3d.ofCenter(base).add(0,.49,0),RaycastContext.ShapeType.OUTLINE,RaycastContext.FluidHandling.NONE,player));
                if(hit.getType()==net.minecraft.util.hit.HitResult.Type.BLOCK&&hit.getBlockPos().equals(base))return position;
            }
            throw new IllegalStateException("No physically clear, reachable native block camera around "+base);
        }
        void verifyCrystal(int i,boolean special){var at=crystalBase(i).up();var found=end.getEntitiesByClass(EndCrystalEntity.class,new Box(at),e->true);require(found.size()==1,"Crystal packet did not place exactly one entity at "+at+": "+found.size());require((found.get(0) instanceof TetanizedEndCrystalEntity)==(special||i<3),"Placed crystal type differs from held item");require(player.getInventory().count(DragonContent.CRYSTAL)==(special?3-i:Math.max(0,2-i)),"Crystal placement failed exact survival consumption");if(i==3)require(field(fight,"dragonSpawnState")!=null,"Four actual portal crystals did not start canonical respawn");}
        Sample sample(){var id=fight.getDragonUuid();if(dragon==null&&id!=null&&end.getEntity(id) instanceof EnderDragonEntity d)dragon=d;String state=field(fight,"dragonSpawnState")==null?"NONE":field(fight,"dragonSpawnState").toString();int xp=0,swords=0,eggs=0;boolean lootReady=true;var area=new Box(-128,0,-128,128,256,128);for(var orb:end.getEntitiesByClass(ExperienceOrbEntity.class,area,e->true)){var nbt=orb.writeNbt(new NbtCompound());xp+=nbt.getShort("Value")*Math.max(1,nbt.getInt("Count"));}for(var item:end.getEntitiesByClass(ItemEntity.class,area,e->true)){if(item.getStack().isOf(DragonContent.SWORD)){swords+=item.getStack().getCount();lootReady&=!item.cannotPickup();}if(item.getStack().isOf(DragonContent.EGG_ITEM)){eggs+=item.getStack().getCount();lootReady&=!item.cannotPickup();}}return new Sample(state,dragon==null?-1:dragon.getId(),dragon==null?0:dragon.getHealth(),dragon==null?0:dragon.getMaxHealth(),dragon!=null&&MobVariants.permanent(dragon),dragon==null?0:dragon.ticksSinceDeath,dragon!=null&&dragon.isRemoved(),dragon==null?0:DragonEncounter.data(dragon).damage.getOrDefault(player.getUuid(),0F),xp,swords,eggs,lootReady&&swords==1&&eggs==1,camera);}
        void freezeSpawned(boolean special){
            int pillarCrystals=0,specialPillars=0;
            for(var spike:net.minecraft.world.gen.feature.EndSpikeFeature.getSpikes(end)){
                var crystals=end.getEntitiesByClass(EndCrystalEntity.class,new Box(spike.getCenterX()-1,spike.getHeight(),spike.getCenterZ()-1,spike.getCenterX()+2,spike.getHeight()+4,spike.getCenterZ()+2),e->!e.isRemoved());
                pillarCrystals+=crystals.size();specialPillars+=(int)crystals.stream().filter(c->c instanceof TetanizedEndCrystalEntity).count();
                for(var c:crystals)require(!c.isInvulnerable()&&c.getBeamTarget()==null,"Pillar crystal remained protected or stuck in the respawn beam");
            }
            require(pillarCrystals==10&&specialPillars==(special?10:0),"Native pillar variants mismatch: total="+pillarCrystals+" special="+specialPillars+" ritual="+special);
            org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_PILLAR_CRYSTALS specialRitual={} total={} special={}",special,pillarCrystals,specialPillars);
            cameraFlight();require(dragon!=null&&MobVariants.permanent(dragon)==special,"Native ritual produced wrong variant");dragon.getPhaseManager().setPhase(PhaseType.HOVER);camera=dragon.getPos().add(0,1,28);player.teleport(end,camera.x,camera.y,camera.z,180,0);player.setVelocity(Vec3d.ZERO);}
        void prepareGrab(){collectingGrab=true;dragon.getPhaseManager().setPhase(PhaseType.HOVER);dragon.setAiDisabled(false);grabStartY=DragonEncounter.mouth(dragon).y;grabMaximum=grabStartY;player.setNoGravity(false);player.getAbilities().flying=false;player.getAbilities().allowFlying=false;player.sendAbilitiesUpdate();player.setVelocity(Vec3d.ZERO);camera=dragon.head.getPos().add(0,0,12);player.teleport(end,camera.x,camera.y,camera.z,180,0);}
        void recordActualGrabTick(){
            var state=DragonEncounter.data(dragon);
            if(player.getVehicle()==dragon&&!grabObserved){
                double fixtureStart=grabStartY;
                grabObserved=true;grabStartY=state.startY;grabMaximum=grabStartY;
                grabCooldown=state.nextGrab-(end.getTime()-state.heldTicks);
                org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_GRAB_ACTUAL_START fixtureY={} actualSuccessfulGrabY={} heldTicks={}",fixtureStart,grabStartY,state.heldTicks);
            }
            // Read actual passenger position each native server tick, including
            // the release tick. Four-tick client polling can miss the peak.
            if(grabObserved&&!grabReleased)grabMaximum=Math.max(grabMaximum,player.getY());
        }
        GrabSample advanceGrab(){
            var state=DragonEncounter.data(dragon);boolean carried=player.getVehicle()==dragon;
            if(carried){recordActualGrabTick();}
            else if(grabObserved){if(!grabReleased){grabReleased=true;releaseHeight=player.getY();}return new GrabSample(false,true,true,grabMaximum-grabStartY,releaseHeight-player.getY(),grabCooldown,grabAttempts);}
            else{
                boolean approach=grabAttempts%2==1;camera=dragon.head.getPos().add(0,.2,approach?3:12);player.teleport(end,camera.x,camera.y,camera.z,180,0);player.setVelocity(Vec3d.ZERO);
                // Seed the ordinary entity RNG, never the infection helper or probability. Native tick consumption remains intact.
                long seed=grabAttempts*7919L;var probe=net.minecraft.util.math.random.Random.create(seed);while(probe.nextFloat()>=DragonEncounter.GRAB_CHANCE){seed++;probe.setSeed(seed);}dragon.getRandom().setSeed(seed);grabAttempts++;
                require(grabAttempts<=240,"Native15percent grab never occurred during seeded approaches");
            }
            return new GrabSample(carried,grabObserved,false,grabMaximum-grabStartY,0,grabCooldown,grabAttempts);
        }
        void prepareAttack(boolean finish){require(dragon!=null&&!dragon.isRemoved(),"Ritual dragon disappeared before native combat");cameraFlight();dragon.getPhaseManager().setPhase(PhaseType.SITTING_SCANNING);dragon.setAiDisabled(true);dragon.timeUntilRegen=0;if(finish)dragon.setHealth(1);camera=dragon.head.getPos().add(0,0,3);require(camera.y>80,"Native dragon head was never positioned before combat");player.teleport(end,camera.x,camera.y,camera.z,180,0);player.setVelocity(Vec3d.ZERO);player.getInventory().clear();var sword=new ItemStack(Items.NETHERITE_SWORD);sword.addEnchantment(Enchantments.SHARPNESS,5);player.getInventory().setStack(0,sword);player.getInventory().selectedSlot=0;player.currentScreenHandler.sendContentUpdates();}
        void deathCamera(){cameraFlight();DragonVictoryAudioProbe.reset();camera=DragonEncounter.rewardPosition(end,dragon.getFightOrigin()).add(0,3,18);player.teleport(end,camera.x,camera.y,camera.z,180,-60);player.setVelocity(Vec3d.ZERO);}
        void collectRewards(){require(sample().xp()==500,"XP was lost before reward collection");player.getInventory().clear();var loot=end.getEntitiesByClass(ItemEntity.class,new Box(-16,0,-16,16,256,16),e->e.getStack().isOf(DragonContent.SWORD)||e.getStack().isOf(DragonContent.EGG_ITEM));require(loot.size()==2,"Exact dragon reward item entities unavailable");for(var item:loot){var pos=item.getPos();player.teleport(end,pos.x,pos.y,pos.z,0,0);item.onPlayerCollision(player);}require(player.getInventory().count(DragonContent.SWORD)==1&&player.getInventory().count(DragonContent.EGG_ITEM)==1,"Native reward pickup did not enter actor inventory");player.currentScreenHandler.sendContentUpdates();}
        void prepareEgg(){cameraFlight();eggBase=end.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,portal.add(10,0,0)).down();end.setBlockState(eggBase,Blocks.END_STONE.getDefaultState());end.setBlockState(eggBase.up(),Blocks.AIR.getDefaultState());end.setBlockState(eggBase.up(2),Blocks.AIR.getDefaultState());eggSlot=-1;for(int i=0;i<9;i++)if(player.getInventory().getStack(i).isOf(DragonContent.EGG_ITEM))eggSlot=i;require(eggSlot>=0,"Earned egg not present in native hotbar");camera=cameraForBlock(eggBase,SIDES);player.teleport(end,camera.x,camera.y,camera.z,180,30);player.setVelocity(Vec3d.ZERO);player.getInventory().selectedSlot=eggSlot;player.currentScreenHandler.sendContentUpdates();}
        void restore(){if(restored)return;collectingGrab=false;DragonVictoryAudioProbe.enabled=false;player.closeHandledScreen();player.stopRiding();if(dragon!=null&&!dragon.isRemoved())dragon.discard();for(var entity:end.getOtherEntities(null,new Box(-128,0,-128,128,256,128),e->e instanceof EndCrystalEntity||e instanceof ExperienceOrbEntity||e instanceof ItemEntity))entity.discard();player.teleport(oldWorld,oldPosition.x,oldPosition.y,oldPosition.z,oldYaw,oldPitch);player.changeGameMode(oldMode);player.setNoGravity(oldGravity);player.getAbilities().flying=oldFlying;player.getAbilities().allowFlying=oldAllowFlying;player.getAbilities().invulnerable=oldInvulnerable;player.sendAbilitiesUpdate();player.getInventory().readNbt(inventory.copy());player.getInventory().selectedSlot=selected;player.getHungerManager().readNbt(hunger.copy());player.clearStatusEffects();for(var effect:effects)player.addStatusEffect(new StatusEffectInstance(effect));player.setHealth(oldHealth);player.setExperienceLevel(oldLevel);player.setExperiencePoints(Math.round(oldProgress*player.getNextLevelExperience()));player.totalExperience=oldTotal;player.currentScreenHandler.sendContentUpdates();restored=true;}
    }
}