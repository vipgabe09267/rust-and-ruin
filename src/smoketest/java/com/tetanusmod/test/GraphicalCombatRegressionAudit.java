package com.tetanusmod.test;

import com.google.gson.GsonBuilder;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.InfectionState;
import com.tetanusmod.entity.*;
import com.tetanusmod.expansion.GloveInventory;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.passive.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Native client use-key shots and actual SoundManager observations. Creative setup never supplies hits. */
public final class GraphicalCombatRegressionAudit {
    private static final BlockPos CENTER=new BlockPos(80,4,80);
    private enum Kind { AIR, WALL, GOLEM, MOB_FIRST, MOB_REPEAT, PLAYER, VACCINATED }
    private record Scene(String id,Kind kind,Item item,boolean playerCreated,int voices){
        boolean returning(){return kind==Kind.PLAYER||kind==Kind.VACCINATED;}
    }
    private static final List<Scene> SCENES=List.of(
            new Scene("rusty-nail-air",Kind.AIR,ModItems.RUSTED_NAIL,false,0),
            new Scene("rusty-bent-nail-air",Kind.AIR,ModItems.RUSTED_BENT_NAIL,false,0),
            new Scene("tetanized-arrow-wall",Kind.WALL,Items.BOW,false,0),
            new Scene("rusty-nail-natural-golem",Kind.GOLEM,ModItems.RUSTED_NAIL,false,1),
            new Scene("rusty-bent-natural-golem",Kind.GOLEM,ModItems.RUSTED_BENT_NAIL,false,1),
            new Scene("rusty-nail-player-golem",Kind.GOLEM,ModItems.RUSTED_NAIL,true,1),
            new Scene("rusty-bent-player-golem",Kind.GOLEM,ModItems.RUSTED_BENT_NAIL,true,1),
            new Scene("fresh-mob-infection",Kind.MOB_FIRST,ModItems.RUSTED_NAIL,false,1),
            new Scene("later-repeat-mob-hit",Kind.MOB_REPEAT,ModItems.RUSTED_NAIL,false,0),
            new Scene("fresh-player-infection",Kind.PLAYER,ModItems.RUSTED_NAIL,false,1),
            new Scene("vaccinated-player-block",Kind.VACCINATED,ModItems.RUSTED_NAIL,false,0));
    private enum Phase { INITIALIZE, PREPARE, PREPARED, SYNC, BEFORE, INPUT, FLIGHT, DRAIN, AFTER, RESTORE, DONE }
    private record Prepared(ItemStack hand,Vec3d position,float pitch,int targetId,long time,boolean guardianBefore,boolean genericBefore){}
    private record Sample(boolean ready,UUID projectile,int age,int usedDelta,int handCount,int targetId,boolean infected,boolean permanent,float maxHealth,boolean guardian,boolean generic,Vec3d position,Map<String,Object> facts){}
    private final List<Map<String,Object>> evidence=new ArrayList<>();
    private final String viewer=FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
    private UUID playerId;
    private Phase phase;
    private CompletableFuture<?> pending;
    private volatile Fixture fixture;
    private Prepared prepared;
    private Sample sample;
    private int index,ticks,phaseTicks,audioBefore,inputTicks,settled;
    private boolean inputReleased,restoreQueued,oldProbeEnabled,clientGuardianBefore;
    private int oldProbeCount,oldFov;
    private Vec3d oldProbePosition;
    private Perspective oldPerspective;
    private Throwable failure;

    public void start(MinecraftClient client){
        require(phase==null,"Combat regression started twice");require(client.getServer()!=null&&client.player!=null,"Combat regression requires an integrated graphical player");
        playerId=client.player.getUuid();oldPerspective=client.options.getPerspective();oldFov=client.options.getFov().getValue();
        oldProbeEnabled=InfectionAudioClientProbe.enabled;oldProbeCount=InfectionAudioClientProbe.count;oldProbePosition=InfectionAudioClientProbe.lastPosition;
        client.setScreen(null);releaseKeys(client);client.options.setPerspective(Perspective.FIRST_PERSON);client.options.getFov().setValue(65);
        InfectionAudioClientProbe.enabled=true;
        pending=server(client,p->{fixture=new Fixture(p);fixture.build();return fixture;});phase=Phase.INITIALIZE;
    }
    public boolean tick(MinecraftClient client){
        try{return tickInternal(client);}catch(RuntimeException|Error problem){failure=problem;restore(client);throw problem;}
    }
    private boolean tickInternal(MinecraftClient client){
        require(phase!=null,"Combat regression not started");if(phase==Phase.DONE)return true;
        require(++ticks<4000,"Combat regression timeout at "+phase+" / scene "+index+" / sample "+sample);
        phaseTicks++;
        switch(phase){
            case INITIALIZE->{if(pending.isDone()){fixture=(Fixture)pending.join();next(Phase.PREPARE);}}
            case PREPARE->{releaseKeys(client);pending=server(client,p->fixture.prepare(p,SCENES.get(index)));next(Phase.PREPARED);}
            case PREPARED->{if(pending.isDone()){prepared=(Prepared)pending.join();pending=null;settled=0;sample=null;next(Phase.SYNC);}}
            case SYNC->{
                client.player.getInventory().selectedSlot=0;
                boolean ready=client.currentScreen==null&&ItemStack.areEqual(client.player.getMainHandStack(),prepared.hand())
                        &&client.player.squaredDistanceTo(prepared.position())<0.05&&client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)&&client.world.getBlockState(CENTER.down()).isOf(Blocks.LIGHT_GRAY_CONCRETE)
                        &&!client.player.getAbilities().creativeMode&&(prepared.targetId()<0||client.world.getEntityById(prepared.targetId())!=null);
                if(SCENES.get(index).kind()==Kind.GOLEM)ready&=!clientDone(client,"rusty_guardian")&&!clientDone(client,"tetanized_mob");
                if(SCENES.get(index).kind()==Kind.VACCINATED)ready&=client.player.hasStatusEffect(TetanusMod.VACCINATED)&&!client.player.hasStatusEffect(TetanusMod.TETANUS);
                if(ready&&++settled>=20){client.player.setYaw(0);client.player.setPitch(prepared.pitch());next(Phase.BEFORE);}else if(!ready)settled=0;
                require(phaseTicks<500,"Combat fixture did not synchronize: "+SCENES.get(index).id()+" hand="+client.player.getMainHandStack()+" position="+client.player.getPos());
            }
            case BEFORE->{if(phaseTicks>=30){screenshot(client,"before");clientGuardianBefore=clientDone(client,"rusty_guardian");audioBefore=InfectionAudioClientProbe.count;inputTicks=0;inputReleased=false;client.options.useKey.setPressed(true);next(Phase.INPUT);}}
            case INPUT->{
                var scene=SCENES.get(index);inputTicks++;
                if(scene.kind()==Kind.WALL){if(inputTicks>=26&&client.player.isUsingItem()){client.options.useKey.setPressed(false);inputReleased=true;next(Phase.FLIGHT);}}
                else if(client.player.getMainHandStack().isOf(scene.item())&&client.player.getMainHandStack().getCount()==1){client.options.useKey.setPressed(false);inputReleased=true;next(Phase.FLIGHT);}
                require(inputTicks<80,"Native use key did not fire exactly one item: "+scene.id()+" hand="+client.player.getMainHandStack()+" using="+client.player.isUsingItem());
            }
            case FLIGHT->{
                require(inputReleased,"Flight phase retained the use key");
                if(pending==null)pending=server(client,p->fixture.sample(p));
                if(pending.isDone()){sample=(Sample)pending.join();pending=null;require(sample.usedDelta()<=1,"Native input fired more than one projectile: "+SCENES.get(index).id());if(sample.ready())next(Phase.DRAIN);}
                require(phaseTicks<350,"Native projectile never completed its expected action: "+SCENES.get(index).id()+" / "+sample);
            }
            case DRAIN->{if(phaseTicks>=30&&clientAcknowledged(client)){validateAndRecord(client);screenshot(client,"after");next(Phase.AFTER);}require(phaseTicks<250,"Client did not acknowledge native combat result: "+sample);}
            case AFTER->{if(phaseTicks>=20){if(++index==SCENES.size()){restore(client);next(Phase.RESTORE);}else next(Phase.PREPARE);}}
            case RESTORE->{if(pending!=null&&pending.isDone()){pending.join();if(fixture.clientRestored(client)){writeReport(client);phase=Phase.DONE;return true;}}require(phaseTicks<250,"Combat restoration did not synchronize to the actual client");}
            default->{}
        }
        return false;
    }
    private boolean clientAcknowledged(MinecraftClient client){
        var scene=SCENES.get(index);
        if(scene.kind()==Kind.GOLEM){var entity=client.world.getEntityById(sample.targetId());return entity instanceof IronGolemEntity golem&&golem.getMaxHealth()==200&&MobVariants.permanent(golem)&&clientDone(client,"rusty_guardian")&&clientDone(client,"tetanized_mob");}
        if(scene.kind()==Kind.PLAYER)return client.player.hasStatusEffect(TetanusMod.TETANUS);
        if(scene.kind()==Kind.VACCINATED)return client.player.hasStatusEffect(TetanusMod.VACCINATED)&&!client.player.hasStatusEffect(TetanusMod.TETANUS);
        if(scene.kind()==Kind.WALL)return client.player.getMainHandStack().isOf(Items.BOW)&&client.player.getMainHandStack().getDamage()==1;
        return client.player.getMainHandStack().isOf(scene.item())&&client.player.getMainHandStack().getCount()==1;
    }
    private void validateAndRecord(MinecraftClient client){
        var scene=SCENES.get(index);int voices=InfectionAudioClientProbe.count-audioBefore;
        require(voices==scene.voices(),"Client SoundManager voice mismatch for "+scene.id()+": expected="+scene.voices()+" observed="+voices+" native="+sample);
        require(sample.usedDelta()==1&&sample.projectile()!=null&&sample.age()>0,"Missing exact native projectile/use proof: "+scene.id());
        if(voices==1)require(InfectionAudioClientProbe.lastPosition!=null&&InfectionAudioClientProbe.lastPosition.distanceTo(sample.position())<2,"Infection voice played away from its actual victim: "+scene.id());
        var row=new LinkedHashMap<String,Object>();row.put("Scene",scene.id());row.put("Action","Actual client use-key input; native projectile movement/collision");row.put("BeforeServerGuardianComplete",prepared.guardianBefore());row.put("BeforeClientGuardianComplete",clientGuardianBefore);row.put("ExpectedVoices",scene.voices());row.put("ClientSoundManagerVoices",voices);row.put("SoundPosition",voices==0?null:InfectionAudioClientProbe.lastPosition.toString());row.put("Native",sample.facts());row.put("ActualClientPlayer",client.player.getUuid().toString());row.put("ClientGuardianComplete",clientDone(client,"rusty_guardian"));row.put("ClientTetanus",client.player.hasStatusEffect(TetanusMod.TETANUS));row.put("BeforePNG",file("before"));row.put("AfterPNG",file("after"));evidence.add(row);
        org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_COMBAT_NATIVE_PASS {} nativeProjectile={} usedDelta={} clientVoices={} clientGuardian={}",scene.id(),sample.projectile(),sample.usedDelta(),voices,row.get("ClientGuardianComplete"));
    }
    @SuppressWarnings("unchecked")
    private boolean clientDone(MinecraftClient client,String id){
        var manager=client.player.networkHandler.getAdvancementHandler();var advancement=manager.getManager().get(TetanusMod.id(id));if(advancement==null)return false;
        try{var field=ClientAdvancementManager.class.getDeclaredField("advancementProgresses");field.setAccessible(true);var progress=((Map<Advancement,AdvancementProgress>)field.get(manager)).get(advancement);return progress!=null&&progress.isDone();}
        catch(ReflectiveOperationException problem){throw new IllegalStateException("Cannot read actual client advancement progress",problem);}
    }
    private <T> CompletableFuture<T> server(MinecraftClient client,Function<ServerPlayerEntity,T> action){
        var future=new CompletableFuture<T>();client.getServer().execute(()->{try{var player=client.getServer().getPlayerManager().getPlayer(playerId);require(player!=null,"Actual graphical combat player disappeared");future.complete(action.apply(player));}catch(Throwable problem){future.completeExceptionally(problem);}});return future;
    }
    private void next(Phase value){phase=value;phaseTicks=0;}
    private String file(String stage){return "rust-and-ruin-"+viewer+"-combat-"+SCENES.get(index).id()+"-"+stage+".png";}
    private void screenshot(MinecraftClient client,String stage){
        try(var image=ScreenshotRecorder.takeScreenshot(client.getFramebuffer())){var folder=client.runDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);image.writeTo(folder.resolve(file(stage)));}catch(Exception problem){throw new IllegalStateException("Cannot save native combat screenshot",problem);}
    }
    private void restore(MinecraftClient client){
        releaseKeys(client);client.options.setPerspective(oldPerspective);client.options.getFov().setValue(oldFov);
        InfectionAudioClientProbe.enabled=oldProbeEnabled;InfectionAudioClientProbe.count=oldProbeCount;InfectionAudioClientProbe.lastPosition=oldProbePosition;
        if(client.player!=null&&fixture!=null)client.player.getInventory().selectedSlot=fixture.oldSelected;
        if(restoreQueued)return;restoreQueued=true;
        if(fixture!=null&&client.getServer()!=null)pending=server(client,p->{fixture.restore(p);fixture.verifyRestored(p);return null;});
    }
    private void writeReport(MinecraftClient client){
        require(failure==null&&evidence.size()==SCENES.size(),"Incomplete graphical combat proof");
        var report=new LinkedHashMap<String,Object>();report.put("Result","PASS");report.put("ActualGraphicalClient",true);report.put("ActualClientUseKeyInput",true);report.put("ClientSoundManagerObservation",true);report.put("Cases",evidence.size());report.put("ExpectedTotalVoices",6);report.put("ObservedTotalVoices",evidence.stream().mapToInt(row->(int)row.get("ClientSoundManagerVoices")).sum());report.put("DirectExposureCalls",0);report.put("DirectProjectileCollisionCalls",0);report.put("DirectCriterionGrantsDuringActions",0);report.put("RestorationCriterionGrants",fixture.restorationGrants);report.put("ReturningNailFixture","Actual upward throw; only the grounded player's horizontal position is aligned under the naturally descending nail");report.put("LightingFixture","Native Night Vision status effect");report.put("Scenes",evidence);report.put("RestoredInventoryEffectsConfigMode",true);
        try{Files.writeString(Path.of("graphical-combat-regressions-"+viewer+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}catch(Exception problem){throw new IllegalStateException("Cannot save graphical combat proof",problem);}
    }
    private static void releaseKeys(MinecraftClient client){client.options.useKey.setPressed(false);client.options.forwardKey.setPressed(false);client.options.backKey.setPressed(false);client.options.leftKey.setPressed(false);client.options.rightKey.setPressed(false);client.options.jumpKey.setPressed(false);client.options.sneakKey.setPressed(false);}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}

    private static final class Fixture {
        private final ServerWorld world,oldWorld;
        private final Vec3d oldPosition,oldVelocity;
        private final float oldYaw,oldPitch,oldHealth,oldFall;
        private final int oldSelected,oldFire;
        private final GameMode oldMode;
        private final boolean oldGravity,oldFlying,oldInvulnerable,oldCamera,oldPvp;
        private final NbtList oldInventory;
        private final NbtCompound oldHunger=new NbtCompound();
        private final ItemStack oldGloves;
        private final List<StatusEffectInstance> oldEffects;
        private final int oldElapsed,oldTotal,oldFreeze,oldLockjaw,oldWindow;
        private final float oldMob,oldGolem,oldAnimal,oldVaccine,oldDamage;
        private final Map<BlockPos,BlockState> replaced=new LinkedHashMap<>();
        private final Map<String,Set<String>> oldAdvancements=new LinkedHashMap<>();
        private final List<Entity> entities=new ArrayList<>();
        private final Set<UUID> seenProjectiles=new HashSet<>();
        private Scene scene;
        private LivingEntity target;
        private CowEntity cow;
        private Entity projectile;
        private Vec3d launched;
        private long preparedAt,launchedAt;
        private int usedBefore,restorationGrants;
        private boolean restored;
        Fixture(ServerPlayerEntity player){
            world=player.getServerWorld();oldWorld=world;oldPosition=player.getPos();oldVelocity=player.getVelocity();oldYaw=player.getYaw();oldPitch=player.getPitch();oldHealth=player.getHealth();oldFall=player.fallDistance;oldFire=player.getFireTicks();oldSelected=player.getInventory().selectedSlot;
            oldPvp=world.getServer().isPvpEnabled();oldMode=player.interactionManager.getGameMode();oldGravity=player.hasNoGravity();oldFlying=player.getAbilities().flying;oldInvulnerable=player.getAbilities().invulnerable;oldInventory=player.getInventory().writeNbt(new NbtList());player.getHungerManager().writeNbt(oldHunger);oldGloves=GloveInventory.equipped(player).copy();oldEffects=player.getStatusEffects().stream().map(StatusEffectInstance::new).toList();
            var state=(InfectionState)player;oldElapsed=state.tetanus$getElapsed();oldTotal=state.tetanus$getTotal();oldFreeze=state.tetanus$getFreeze();oldLockjaw=state.tetanus$getLockjaw();oldWindow=state.tetanus$getLockjawWindow();
            oldMob=TetanusMod.CONFIG.mobConversionChance;oldGolem=TetanusMod.CONFIG.golemConversionChance;oldAnimal=TetanusMod.CONFIG.animalConversionChance;oldVaccine=TetanusMod.CONFIG.vaccineProtectionChance;oldDamage=TetanusMod.CONFIG.nailDamage;oldCamera=TetanusMod.CONFIG.cameraShaking;
            for(String id:List.of("rusty_guardian","tetanized_mob")){var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id(id));require(advancement!=null,"Missing advancement "+id);var obtained=new HashSet<String>();player.getAdvancementTracker().getProgress(advancement).getObtainedCriteria().forEach(obtained::add);oldAdvancements.put(id,obtained);}
            for(int x=-5;x<=5;x++)for(int z=-4;z<=12;z++)for(int y=-1;y<=7;y++){var pos=CENTER.add(x,y,z);require(world.getBlockEntity(pos)==null,"Combat fixture would replace a block entity at "+pos);replaced.put(pos,world.getBlockState(pos));}
        }
        void build(){
            for(var pos:replaced.keySet())world.setBlockState(pos,pos.getY()==CENTER.getY()-1?Blocks.LIGHT_GRAY_CONCRETE.getDefaultState():Blocks.AIR.getDefaultState());
        }
        Prepared prepare(ServerPlayerEntity player,Scene next){
            scene=next;projectile=null;launched=null;launchedAt=0;
            entities.removeIf(entity->{boolean keep=scene.kind()==Kind.MOB_REPEAT&&entity==cow;if(!keep)entity.discard();return !keep;});
            if(scene.kind()!=Kind.MOB_REPEAT){cow=null;target=null;}else target=cow;
            for(int x=-2;x<=2;x++)for(int y=0;y<4;y++)world.setBlockState(CENTER.add(x,y,6),scene.kind()==Kind.WALL?Blocks.STONE_BRICKS.getDefaultState():Blocks.AIR.getDefaultState());
            player.closeHandledScreen();player.clearStatusEffects();player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,2400,0,false,false,false));player.changeGameMode(GameMode.SURVIVAL);player.setNoGravity(true);player.getAbilities().flying=false;player.getAbilities().invulnerable=false;player.sendAbilitiesUpdate();player.setFireTicks(0);player.fallDistance=0;player.setHealth(player.getMaxHealth());player.getHungerManager().setFoodLevel(20);player.getInventory().clear();((GloveInventory)player).rustruin$gloves().clear();player.getInventory().selectedSlot=0;player.timeUntilRegen=0;
            TetanusMod.CONFIG.mobConversionChance=scene.kind()==Kind.GOLEM?1:0;TetanusMod.CONFIG.golemConversionChance=1;TetanusMod.CONFIG.animalConversionChance=0;TetanusMod.CONFIG.vaccineProtectionChance=1;TetanusMod.CONFIG.nailDamage=2;TetanusMod.CONFIG.cameraShaking=false;
            if(scene.kind()==Kind.VACCINATED)player.addStatusEffect(new StatusEffectInstance(TetanusMod.VACCINATED,2400));
            if(scene.kind()==Kind.GOLEM){
                for(String id:oldAdvancements.keySet()){var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id(id));for(String criterion:advancement.getCriteria().keySet())player.getAdvancementTracker().revokeCriterion(advancement,criterion);}
                var golem=Objects.requireNonNull(EntityType.IRON_GOLEM.create(world));golem.setPlayerCreated(scene.playerCreated());golem.setAiDisabled(true);golem.setNoGravity(true);target=golem;
            }
            if(scene.kind()==Kind.MOB_FIRST){cow=Objects.requireNonNull(EntityType.COW.create(world));cow.setAiDisabled(true);cow.setNoGravity(true);target=cow;}
            if(scene.kind()==Kind.MOB_REPEAT)require(cow!=null&&cow.hasStatusEffect(TetanusMod.TETANUS)&&!MobVariants.permanent(cow),"Repeat scene lost its existing temporary mob infection");
            if(scene.kind()==Kind.MOB_REPEAT){target.setPosition(Vec3d.ofBottomCenter(CENTER).add(0,0,3));target.setVelocity(Vec3d.ZERO);target.velocityModified=true;}
            if(target!=null&&scene.kind()!=Kind.MOB_REPEAT){target.setPosition(Vec3d.ofBottomCenter(CENTER).add(0,0,3));require(world.spawnEntity(target),"Native combat target could not spawn");entities.add(target);}
            if(scene.returning())world.getServer().setPvpEnabled(true);
            var position=Vec3d.ofBottomCenter(CENTER);float pitch=scene.returning()?-90:(scene.kind()==Kind.MOB_FIRST||scene.kind()==Kind.MOB_REPEAT?12:0);
            player.teleport(world,position.x,position.y,position.z,0,pitch);player.setVelocity(Vec3d.ZERO);
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(scene.item(),scene.kind()==Kind.WALL?1:2));if(scene.kind()==Kind.WALL)player.getInventory().setStack(9,new ItemStack(ModItems.TETANIZED_ARROW,3));
            for(var item:new Item[]{ModItems.NAIL,ModItems.RUSTED_NAIL,ModItems.BENT_NAIL,ModItems.RUSTED_BENT_NAIL})player.getItemCooldownManager().set(item,0);
            player.playerScreenHandler.syncState();usedBefore=player.getStatHandler().getStat(Stats.USED.getOrCreateStat(scene.item()));preparedAt=world.getTime();
            return new Prepared(player.getMainHandStack().copy(),position,pitch,target==null?-1:target.getId(),preparedAt,done(player,"rusty_guardian"),done(player,"tetanized_mob"));
        }
        Sample sample(ServerPlayerEntity player){
            int used=player.getStatHandler().getStat(Stats.USED.getOrCreateStat(scene.item()))-usedBefore;
            if(projectile==null&&target!=null){var damage=target.getRecentDamageSource();if(damage!=null&&damage.getSource() instanceof NailEntity nail&&nail.getOwner()==player&&!seenProjectiles.contains(nail.getUuid()))projectile=nail;}
            if(projectile==null&&scene.returning()){var damage=player.getRecentDamageSource();if(damage!=null&&damage.getSource() instanceof NailEntity nail&&nail.getOwner()==player&&!seenProjectiles.contains(nail.getUuid()))projectile=nail;}
            if(projectile==null)projectile=world.getOtherEntities(player,player.getBoundingBox().expand(90),entity->entity instanceof net.minecraft.entity.projectile.ProjectileEntity shot&&shot.getOwner()==player&&!seenProjectiles.contains(entity.getUuid())&&(scene.kind()==Kind.WALL?entity instanceof TetanizedArrowEntity:entity instanceof NailEntity)).stream().findFirst().orElse(null);
            if(projectile!=null&&launched==null){launched=projectile.getPos();launchedAt=world.getTime();entities.add(projectile);}
            if(scene.returning()&&projectile!=null&&projectile.isAlive()&&projectile.getVelocity().y<0&&projectile.getY()<CENTER.getY()+5){
                if(Math.abs(player.getX()-projectile.getX())>0.025||Math.abs(player.getZ()-projectile.getZ())>0.025)player.teleport(world,projectile.getX(),CENTER.getY(),projectile.getZ(),0,-75);
                player.setVelocity(Vec3d.ZERO);
            }
            LivingEntity victim=scene.returning()?player:target;boolean infected=victim!=null&&victim.hasStatusEffect(TetanusMod.TETANUS);boolean permanent=victim!=null&&MobVariants.permanent(victim);
            boolean collision=false;if(victim!=null){var damage=victim.getRecentDamageSource();collision=projectile!=null&&damage!=null&&damage.getSource()==projectile&&damage.getAttacker()==player;}
            boolean arrowGround=projectile instanceof TetanizedArrowEntity arrow&&arrow.writeNbt(new NbtCompound()).getBoolean("inGround");
            boolean guardian=done(player,"rusty_guardian"),generic=done(player,"tetanized_mob");
            boolean ready=used==1&&projectile!=null&&switch(scene.kind()){
                case AIR->world.getTime()-launchedAt>=25;
                case WALL->arrowGround&&player.getInventory().getStack(9).getCount()==2;
                case GOLEM->collision&&infected&&permanent&&victim.getMaxHealth()==200&&guardian&&generic;
                case MOB_FIRST,MOB_REPEAT->collision&&infected&&!permanent;
                case PLAYER->collision&&infected;
                case VACCINATED->collision&&!infected&&player.hasStatusEffect(TetanusMod.VACCINATED);
            };
            var facts=new LinkedHashMap<String,Object>();facts.put("Scene",scene.id());facts.put("NativeUsedDelta",used);facts.put("MainHandCount",player.getMainHandStack().getCount());facts.put("ProjectileUUID",projectile==null?null:projectile.getUuid().toString());facts.put("ProjectileAge",projectile==null?0:projectile.age);facts.put("ExactNativeDamageSource",collision);facts.put("ArrowNativeInGround",arrowGround);facts.put("VictimTetanus",infected);facts.put("PermanentVariant",permanent);facts.put("VictimMaxHealth",victim==null?null:victim.getMaxHealth());facts.put("VictimHealth",victim==null?null:victim.getHealth());facts.put("PlayerCreatedGolem",target instanceof IronGolemEntity golem?golem.isPlayerCreated():null);facts.put("ServerGuardianComplete",guardian);facts.put("ServerMobAdvancementComplete",generic);facts.put("NativeTimeSincePreparation",world.getTime()-preparedAt);facts.put("VictimPosition",(victim==null?player.getPos():victim.getPos()).toString());
            if(ready){seenProjectiles.add(projectile.getUuid());if(target!=null){target.setVelocity(Vec3d.ZERO);target.velocityModified=true;}if(scene.returning()){player.setVelocity(Vec3d.ZERO);player.velocityModified=true;}}
            return new Sample(ready,projectile==null?null:projectile.getUuid(),projectile==null?0:projectile.age,used,player.getMainHandStack().getCount(),target==null?-1:target.getId(),infected,permanent,victim==null?0:victim.getMaxHealth(),guardian,generic,victim==null?player.getPos():victim.getPos(),facts);
        }
        boolean clientRestored(MinecraftClient client){
            if(client.player==null||client.player.getInventory().selectedSlot!=oldSelected||client.player.getAbilities().creativeMode!=(oldMode==GameMode.CREATIVE))return false;
            if(!client.player.getInventory().writeNbt(new NbtList()).equals(oldInventory)||!ItemStack.areEqual(GloveInventory.equipped(client.player),oldGloves)||client.player.getStatusEffects().size()!=oldEffects.size())return false;
            for(var effect:oldEffects)if(!client.player.hasStatusEffect(effect.getEffectType()))return false;
            return client.player.squaredDistanceTo(oldPosition)<0.1;
        }
        void verifyRestored(ServerPlayerEntity player){
            require(player.getInventory().writeNbt(new NbtList()).equals(oldInventory)&&player.getInventory().selectedSlot==oldSelected&&ItemStack.areEqual(GloveInventory.equipped(player),oldGloves),"Combat fixture changed the saved player inventory/gloves");
            var hunger=new NbtCompound();player.getHungerManager().writeNbt(hunger);require(hunger.equals(oldHunger)&&player.getHealth()==Math.min(oldHealth,player.getMaxHealth()),"Combat fixture changed saved health/hunger");
            require(player.interactionManager.getGameMode()==oldMode&&player.hasNoGravity()==oldGravity&&player.getAbilities().flying==oldFlying&&player.getAbilities().invulnerable==oldInvulnerable,"Combat fixture changed the saved player mode/physics");
            require(TetanusMod.CONFIG.mobConversionChance==oldMob&&TetanusMod.CONFIG.golemConversionChance==oldGolem&&TetanusMod.CONFIG.animalConversionChance==oldAnimal&&TetanusMod.CONFIG.vaccineProtectionChance==oldVaccine&&TetanusMod.CONFIG.nailDamage==oldDamage&&TetanusMod.CONFIG.cameraShaking==oldCamera&&world.getServer().isPvpEnabled()==oldPvp,"Combat fixture did not restore original configuration/PvP");
            require(player.getStatusEffects().size()==oldEffects.size(),"Combat fixture changed original status effect count");
            for(var old:oldEffects){var effect=player.getStatusEffect(old.getEffectType());require(effect!=null&&effect.getDuration()==old.getDuration()&&effect.getAmplifier()==old.getAmplifier()&&effect.isAmbient()==old.isAmbient()&&effect.shouldShowParticles()==old.shouldShowParticles()&&effect.shouldShowIcon()==old.shouldShowIcon(),"Combat fixture changed original status effect "+old.getEffectType());}
        }
        private boolean done(ServerPlayerEntity player,String id){return player.getAdvancementTracker().getProgress(player.getServer().getAdvancementLoader().get(TetanusMod.id(id))).isDone();}
        void restore(ServerPlayerEntity player){
            if(restored)return;restored=true;
            TetanusMod.CONFIG.mobConversionChance=oldMob;TetanusMod.CONFIG.golemConversionChance=oldGolem;TetanusMod.CONFIG.animalConversionChance=oldAnimal;TetanusMod.CONFIG.vaccineProtectionChance=oldVaccine;TetanusMod.CONFIG.nailDamage=oldDamage;TetanusMod.CONFIG.cameraShaking=oldCamera;
            world.getServer().setPvpEnabled(oldPvp);entities.forEach(Entity::discard);for(var entry:replaced.entrySet())world.setBlockState(entry.getKey(),entry.getValue());
            player.closeHandledScreen();player.clearStatusEffects();com.tetanusmod.effect.Infection.silently(()->{for(var effect:oldEffects)com.tetanusmod.effect.InfectionProtection.checked(player,()->player.addStatusEffect(new StatusEffectInstance(effect)));});player.changeGameMode(oldMode);player.setNoGravity(oldGravity);player.getAbilities().flying=oldFlying;player.getAbilities().invulnerable=oldInvulnerable;player.sendAbilitiesUpdate();player.getInventory().readNbt(oldInventory);player.getInventory().selectedSlot=oldSelected;((GloveInventory)player).rustruin$gloves().setStack(0,oldGloves);player.getHungerManager().readNbt(oldHunger);player.setHealth(Math.min(oldHealth,player.getMaxHealth()));player.setFireTicks(oldFire);player.fallDistance=oldFall;player.teleport(oldWorld,oldPosition.x,oldPosition.y,oldPosition.z,oldYaw,oldPitch);player.setVelocity(oldVelocity);player.playerScreenHandler.syncState();
            var state=(InfectionState)player;state.tetanus$setInfection(oldElapsed,oldTotal);state.tetanus$setFreeze(oldFreeze);state.tetanus$setLockjaw(oldLockjaw);state.tetanus$setLockjawWindow(oldWindow);
            for(var entry:oldAdvancements.entrySet()){var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id(entry.getKey()));var progress=player.getAdvancementTracker().getProgress(advancement);for(String criterion:advancement.getCriteria().keySet()){boolean obtained=false;for(String current:progress.getObtainedCriteria())if(current.equals(criterion))obtained=true;if(entry.getValue().contains(criterion)&&!obtained){player.getAdvancementTracker().grantCriterion(advancement,criterion);restorationGrants++;}else if(!entry.getValue().contains(criterion)&&obtained)player.getAdvancementTracker().revokeCriterion(advancement,criterion);}}
        }
    }
}