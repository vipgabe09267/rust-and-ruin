package com.tetanusmod.test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.config.ModConfig;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.expansion.MedicVillage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.passive.*;
import net.minecraft.nbt.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Actual client network commands and passive SoundManager observations at known distances. */
public final class GraphicalProximityAudioAudit {
    private static final Vec3d LISTENER=new Vec3d(256.5,4,128.5),TARGET=LISTENER.add(32,0,0);
    private final List<Map<String,Object>> receipts=new ArrayList<>();
    private final List<Entity> fixtures=new ArrayList<>();
    private final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();
    private final Set<ChatHudLine> previousChat=Collections.newSetFromMap(new IdentityHashMap<>());
    private final String viewer=FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
    private UUID playerId,cowId,naturalId,medicId;
    private volatile boolean ready=true,restored;
    private volatile Throwable failure;
    private boolean started,done,restoreQueued,oldOperator,oldSubtitles,freshShot;
    private int phase,ticks,phaseTicks,networkCommands,observedVoices,observedWork,lastReceiptVoices;
    private String pending;
    private ModConfig oldConfig;
    private Difficulty oldDifficulty;
    private ServerWorld oldWorld;
    private Vec3d oldPosition;
    private float oldYaw,oldPitch,oldHealth;
    private GameMode oldMode;
    private boolean oldGravity,oldFlying,oldInvulnerable;
    private NbtList oldInventory;
    private NbtCompound oldHunger;
    private int oldSlot,oldFov;
    private StatusEffectInstance oldVision;
    private Perspective oldPerspective;
    private InfectionAudioClientProbe.State oldProbe;

    public void start(MinecraftClient client){
        require(!started,"Proximity audio audit started twice");started=true;playerId=client.player.getUuid();oldProbe=InfectionAudioClientProbe.snapshot();InfectionAudioClientProbe.enabled=false;
        oldPerspective=client.options.getPerspective();oldFov=client.options.getFov().getValue();oldSubtitles=client.options.getShowSubtitles().getValue();client.options.getShowSubtitles().setValue(true);client.options.setPerspective(Perspective.FIRST_PERSON);client.options.getFov().setValue(55);client.setScreen(null);keys(client);
        server(client,player->{
            oldConfig=TetanusMod.CONFIG;var config=new Gson().fromJson(new Gson().toJson(oldConfig),ModConfig.class);config.infectionSoundVolume=4;config.animalConversionChance=config.mobConversionChance=0;config.naturalAnimalEasy=config.naturalAnimalNormal=config.naturalAnimalHard=1;TetanusMod.CONFIG=config;
            oldWorld=player.getServerWorld();oldDifficulty=oldWorld.getDifficulty();player.getServer().setDifficulty(Difficulty.NORMAL,true);oldPosition=player.getPos();oldYaw=player.getYaw();oldPitch=player.getPitch();oldHealth=player.getHealth();oldMode=player.interactionManager.getGameMode();oldGravity=player.hasNoGravity();oldFlying=player.getAbilities().flying;oldInvulnerable=player.getAbilities().invulnerable;
            oldInventory=player.getInventory().writeNbt(new NbtList());oldHunger=new NbtCompound();player.getHungerManager().writeNbt(oldHunger);oldSlot=player.getInventory().selectedSlot;var vision=player.getStatusEffect(StatusEffects.NIGHT_VISION);oldVision=vision==null?null:new StatusEffectInstance(vision);
            oldOperator=player.getServer().getPlayerManager().isOperator(player.getGameProfile());if(!oldOperator)player.getServer().getPlayerManager().addToOperators(player.getGameProfile());
            for(int x=-4;x<=36;x++)for(int z=-5;z<=5;z++)for(int y=-1;y<=5;y++){var pos=BlockPos.ofFloored(LISTENER).add(x,y,z);require(oldWorld.getBlockEntity(pos)==null,"Audio fixture would overwrite block entity at "+pos);blocks.put(pos,oldWorld.getBlockState(pos));}
            for(var pos:blocks.keySet())oldWorld.setBlockState(pos,pos.getY()==3?Blocks.STONE.getDefaultState():Blocks.AIR.getDefaultState());
            player.changeGameMode(GameMode.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;player.getAbilities().invulnerable=true;player.sendAbilitiesUpdate();player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,2400,0,false,false,false));player.teleport(oldWorld,LISTENER.x,LISTENER.y,LISTENER.z,-90,10);player.setVelocity(Vec3d.ZERO);
            var cow=cow(player,TARGET,SpawnReason.COMMAND);require(!MobVariants.permanent(cow)&&!cow.hasStatusEffect(TetanusMod.TETANUS),"Far subject was not initially healthy");cowId=cow.getUuid();
        });
    }
    public boolean tick(MinecraftClient client){try{return advance(client);}catch(RuntimeException|Error problem){restore(client);throw problem;}}
    private boolean advance(MinecraftClient client){
        require(started,"Audio audit not started");if(done)return true;if(failure!=null){restore(client);throw new IllegalStateException("Native proximity audio action failed",failure);}require(++ticks<2400,"Audio audit timed out at phase "+phase+" pending "+pending);if(!ready)return false;phaseTicks++;
        switch(phase){
            case 0->{if(phaseTicks<40||!client.player.hasPermissionLevel(2)||client.player.squaredDistanceTo(LISTENER)>0.04||entity(client,cowId)==null)return false;InfectionAudioClientProbe.reset();InfectionAudioClientProbe.enabled=true;aim(client,TARGET.add(0,0.8,0));command(client,"effect give "+cowId+" tetanusmod:tetanus 300");next();}
            case 1->{if(!commandReplied(client,false)||phaseTicks<35)return false;require(InfectionAudioClientProbe.count==0,"Cow32blocks away produced a client voice");receipt(client,"far32-new-command-infection",0);shot(client,"far32-command-silent");server(client,player->{require(cow(player,cowId).hasStatusEffect(TetanusMod.TETANUS),"Far native command did not really infect");var near=TARGET.add(-6,0,0);player.teleport(oldWorld,near.x,near.y,near.z,-90,10);});client.setScreen(null);next();}
            case 2->{if(client.player.squaredDistanceTo(TARGET.add(-6,0,0))>0.04||phaseTicks<35)return false;aim(client,TARGET.add(0,0.8,0));require(InfectionAudioClientProbe.count==0,"Approaching an already infected mob replayed the voice");receipt(client,"approach-already-infected-to6",0);shot(client,"approach6-still-silent");command(client,"effect clear "+cowId+" tetanusmod:tetanus");next();}
            case 3->{if(!commandReplied(client,true)||phaseTicks<20)return false;require(InfectionAudioClientProbe.count==0,"Clearing an infection played voice");server(client,player->require(!cow(player,cowId).hasStatusEffect(TetanusMod.TETANUS),"Native clear did not really remove Tetanus"));command(client,"effect give "+cowId+" tetanusmod:tetanus 300");next();}
            case 4->{if(!commandReplied(client,false))return false;if(phaseTicks>=25&&!freshShot){shot(client,"fresh-near-voice");freshShot=true;}if(phaseTicks<110)return false;require(InfectionAudioClientProbe.count==1,"Fresh nearby command did not produce exactly one client voice");require(InfectionAudioClientProbe.lastPosition!=null&&InfectionAudioClientProbe.lastPosition.squaredDistanceTo(TARGET)<0.02,"Voice source is not the infected cow");require(InfectionAudioClientProbe.lastVolume<=1&&!InfectionAudioClientProbe.lastRelative&&"LINEAR".equals(InfectionAudioClientProbe.lastAttenuation),"Native voice is not gain-bounded positional/attenuated audio");receipt(client,"fresh-near6-command-single-positional-voice",1);command(client,"effect give "+cowId+" tetanusmod:tetanus 600 1");next();}
            case 5->{if(!commandReplied(client,false)||phaseTicks<35)return false;require(InfectionAudioClientProbe.count==1,"Later refresh replayed nearby voice");receipt(client,"later-command-refresh-no-extra-voice",0);shot(client,"refresh-silent");server(client,player->{var effect=cow(player,cowId).getStatusEffect(TetanusMod.TETANUS);require(effect!=null&&effect.getAmplifier()==1&&effect.getDuration()>11000,"Refresh command was not actually accepted/upgraded");var natural=cow(player,TARGET.add(0,0,4),SpawnReason.CHUNK_GENERATION);require(MobVariants.permanent(natural)&&natural.hasStatusEffect(TetanusMod.TETANUS),"Procedural native spawn was not really tetanized");naturalId=natural.getUuid();});client.setScreen(null);next();}
            case 6->{var natural=entity(client,naturalId);if(natural==null||!(natural instanceof CowEntity)||!MobVariants.permanent((CowEntity)natural))return false;aim(client,natural.getPos().add(0,0.8,0));if(phaseTicks<35)return false;require(InfectionAudioClientProbe.count==1,"Nearby procedural tetanized spawn played a voice");receipt(client,"near-procedural-spawn-client-rusted-flag-no-voice",0);shot(client,"procedural-rust-skin-silent");server(client,player->{var medic=EntityType.VILLAGER.create(oldWorld);require(medic!=null,"Cannot create native medic");medic.setPosition(TARGET.add(0,0,-4));medic.setAiDisabled(true);medic.setNoGravity(true);medic.initialize(oldWorld,oldWorld.getLocalDifficulty(medic.getBlockPos()),SpawnReason.STRUCTURE,null,null);medic.setVillagerData(medic.getVillagerData().withProfession(MedicVillage.MEDIC));require(oldWorld.spawnEntity(medic),"Medic was not inserted into native tracking");fixtures.add(medic);medicId=medic.getUuid();});next();}
            case 7->{var medic=entity(client,medicId);if(!(medic instanceof VillagerEntity villager)||villager.getVillagerData().getProfession()!=MedicVillage.MEDIC)return false;aim(client,medic.getPos().add(0,1.1,0));if(phaseTicks<30)return false;require(client.player.squaredDistanceTo(medic.getPos())<144,"Medic work sample is outside its12block radius");require(InfectionAudioClientProbe.medicWorkCount==0,"Unexpected medic work before the actual native action");server(client,player->((VillagerEntity)oldWorld.getEntity(medicId)).playWorkSound());next();}
            case 8->{if(phaseTicks<30||InfectionAudioClientProbe.medicWorkCount==0)return false;require(InfectionAudioClientProbe.medicWorkCount==1&&InfectionAudioClientProbe.count==1,"Native medic work event duplicated or replayed infection voice");require(InfectionAudioClientProbe.lastMedicWorkPosition!=null&&InfectionAudioClientProbe.lastMedicWorkPosition.squaredDistanceTo(TARGET.add(0,0,-4))<0.02,"Custom work sound did not originate at the actual medic");observedVoices=InfectionAudioClientProbe.count;observedWork=InfectionAudioClientProbe.medicWorkCount;receipt(client,"native-medic-work-single-custom-client-event",0);shot(client,"medic-work-custom-sound");restore(client);next();}
            case 9->{if(!restored)return false;var report=new LinkedHashMap<String,Object>();report.put("Result","PASS");report.put("ActualConnectedPlayer",playerId.toString());report.put("NativeClientNetworkEffectCommands",networkCommands);report.put("ExpectedInfectionPlayEvents",1);report.put("ObservedInfectionPlayEvents",observedVoices);report.put("ObservedMedicWorkEvents",observedWork);report.put("Receipts",receipts);report.put("PlayerConfigProbeStateRestored",true);try{Files.writeString(client.runDirectory.toPath().resolve("graphical-proximity-audio-"+viewer+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}catch(Exception problem){throw new IllegalStateException(problem);}done=true;return true;}
            default->throw new IllegalStateException("Unknown proximity audio phase "+phase);
        }return false;
    }
    private CowEntity cow(ServerPlayerEntity player,Vec3d position,SpawnReason reason){var cow=EntityType.COW.create(oldWorld);require(cow!=null,"Cannot create native cow");cow.setPosition(position);cow.setAiDisabled(true);cow.setNoGravity(true);cow.initialize(oldWorld,oldWorld.getLocalDifficulty(cow.getBlockPos()),reason,null,null);require(oldWorld.spawnEntity(cow),"Cow did not enter native tracking");fixtures.add(cow);return cow;}
    private static CowEntity cow(ServerPlayerEntity player,UUID id){var entity=player.getServerWorld().getEntity(id);require(entity instanceof CowEntity,"Actual cow subject is missing");return (CowEntity)entity;}
    private void command(MinecraftClient client,String value){pending=value;networkCommands++;previousChat.clear();previousChat.addAll(chat(client));client.setScreen(new ChatScreen(""));client.player.networkHandler.sendChatCommand(value);org.slf4j.LoggerFactory.getLogger("RustRuinProximityAudio").info("PROXIMITY_AUDIO_NATIVE_COMMAND /{}",value);}
    private boolean commandReplied(MinecraftClient client,boolean clearing){for(var line:chat(client))if(!previousChat.contains(line)&&line.content().getContent() instanceof TranslatableTextContent text){var key=text.getKey();if(clearing&&key.startsWith("commands.effect.clear.")&&!key.contains("failed"))return true;if(!clearing&&key.equals("commands.effect.give.success.single"))return true;}return false;}
    @SuppressWarnings("unchecked") private static List<ChatHudLine> chat(MinecraftClient client){try{var field=client.inGameHud.getChatHud().getClass().getDeclaredField("messages");field.setAccessible(true);return List.copyOf((List<ChatHudLine>)field.get(client.inGameHud.getChatHud()));}catch(Exception problem){throw new IllegalStateException(problem);}}
    private void receipt(MinecraftClient client,String action,int expectedDelta){var row=new LinkedHashMap<String,Object>();row.put("Action",action);row.put("ExpectedNewInfectionEvents",expectedDelta);int delta=InfectionAudioClientProbe.count-lastReceiptVoices;require(delta==expectedDelta,"Actual per-scene infection audio delta disagrees for "+action+": "+delta);row.put("ObservedNewInfectionEvents",delta);lastReceiptVoices=InfectionAudioClientProbe.count;row.put("ObservedTotalInfectionEvents",InfectionAudioClientProbe.count);row.put("ObservedTotalMedicWorkEvents",InfectionAudioClientProbe.medicWorkCount);row.put("LastMedicWorkPosition",InfectionAudioClientProbe.lastMedicWorkPosition==null?null:pos(InfectionAudioClientProbe.lastMedicWorkPosition));row.put("Listener",pos(client.player.getPos()));row.put("CowPosition",pos(TARGET));row.put("DistanceToCow",client.player.getPos().distanceTo(TARGET));row.put("LastVoicePosition",InfectionAudioClientProbe.lastPosition==null?null:pos(InfectionAudioClientProbe.lastPosition));row.put("LastVoiceVolume",InfectionAudioClientProbe.lastVolume);row.put("VoiceRelative",InfectionAudioClientProbe.lastRelative);row.put("VoiceAttenuation",InfectionAudioClientProbe.lastAttenuation);receipts.add(row);org.slf4j.LoggerFactory.getLogger("RustRuinProximityAudio").info("GRAPHICAL_PROXIMITY_AUDIO_PASS {}",action);}
    private void server(MinecraftClient client,Consumer<ServerPlayerEntity> action){require(ready,"Overlapping audio server actions");ready=false;client.getServer().execute(()->{try{var player=client.getServer().getPlayerManager().getPlayer(playerId);require(player!=null,"Connected audio player disappeared");action.accept(player);}catch(Throwable problem){failure=problem;}finally{ready=true;}});}
    private static Entity entity(MinecraftClient client,UUID id){for(var entity:client.world.getEntities())if(entity.getUuid().equals(id))return entity;return null;}
    private static void aim(MinecraftClient client,Vec3d point){var d=point.subtract(client.player.getEyePos());client.player.setYaw((float)Math.toDegrees(Math.atan2(-d.x,d.z)));client.player.setPitch((float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));}
    private void shot(MinecraftClient client,String name){try(var frame=ScreenshotRecorder.takeScreenshot(client.getFramebuffer())){var directory=client.runDirectory.toPath().resolve("screenshots");Files.createDirectories(directory);frame.writeTo(directory.resolve("rust-and-ruin-"+viewer+"-audio-"+name+".png"));}catch(Exception problem){throw new IllegalStateException(problem);}}
    private void next(){phase++;phaseTicks=0;}
    private static void keys(MinecraftClient client){client.options.forwardKey.setPressed(false);client.options.backKey.setPressed(false);client.options.leftKey.setPressed(false);client.options.rightKey.setPressed(false);client.options.jumpKey.setPressed(false);client.options.sneakKey.setPressed(false);client.options.useKey.setPressed(false);}
    private void restore(MinecraftClient client){keys(client);client.setScreen(null);client.options.setPerspective(oldPerspective);client.options.getFov().setValue(oldFov);client.options.getShowSubtitles().setValue(oldSubtitles);InfectionAudioClientProbe.restore(oldProbe);if(restoreQueued)return;restoreQueued=true;if(client.getServer()==null){restored=true;return;}client.getServer().execute(()->{try{if(oldConfig!=null)TetanusMod.CONFIG=oldConfig;fixtures.forEach(Entity::discard);if(oldWorld!=null){for(var entry:blocks.entrySet())oldWorld.setBlockState(entry.getKey(),entry.getValue());oldWorld.getServer().setDifficulty(oldDifficulty,true);}var player=client.getServer().getPlayerManager().getPlayer(playerId);if(player!=null&&oldWorld!=null){player.closeHandledScreen();player.changeGameMode(oldMode);player.setNoGravity(oldGravity);player.getAbilities().flying=oldFlying;player.getAbilities().invulnerable=oldInvulnerable;player.sendAbilitiesUpdate();player.removeStatusEffect(StatusEffects.NIGHT_VISION);if(oldVision!=null)player.addStatusEffect(oldVision);player.getInventory().readNbt(oldInventory);player.getInventory().selectedSlot=oldSlot;player.getHungerManager().readNbt(oldHunger);player.setHealth(Math.min(oldHealth,player.getMaxHealth()));player.teleport(oldWorld,oldPosition.x,oldPosition.y,oldPosition.z,oldYaw,oldPitch);player.playerScreenHandler.syncState();if(!oldOperator)player.getServer().getPlayerManager().removeFromOperators(player.getGameProfile());}}catch(Throwable problem){failure=problem;}finally{restored=true;}});}
    private static Map<String,Object> pos(Vec3d p){return Map.of("x",p.x,"y",p.y,"z",p.z);}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
