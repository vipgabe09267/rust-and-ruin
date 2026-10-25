package com.tetanusmod.test;

import com.google.gson.GsonBuilder;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.expansion.*;
import com.tetanusmod.world.MedicStructurePiece;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.*;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Native connected-client discovery in a normal, naturally generated Overworld. */
public final class GraphicalCampDiscoveryAudit {
    private static volatile GraphicalCampDiscoveryAudit active;
    private static final Vec3d OBSERVER_ORIGIN=new Vec3d(-4.5,140,14.5);
    private final List<Text> serverMessages=new CopyOnWriteArrayList<>();
    private final List<Map<String,Object>> commands=new ArrayList<>();
    private final Map<String,Object> campEvidence=new LinkedHashMap<>();
    private final Set<ChatHudLine> oldChatLines=Collections.newSetFromMap(new IdentityHashMap<>());
    private final String viewer=FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
    private UUID playerId,medicId,golemId,farmerId;
    private volatile Throwable failure;
    private volatile boolean serverReady=true,restored,residentInspectionReady;
    private boolean started,complete,restoreQueued,oldOperator;
    private int ticks,phase,phaseTicks,serverMessageBase;
    private String pendingCommand,pendingKey;
    private Feedback structureFeedback;
    private StructureStart structureStart;
    private BlockBox bounds;
    private BlockPos cabinet,chest,irrigation;
    private ServerWorld originalWorld;
    private Vec3d originalPosition,room,farmView;
    private GameMode originalMode;
    private boolean originalGravity,originalFlying,originalInvulnerable;
    private float originalYaw,originalPitch;
    private StatusEffectInstance originalVision;
    private Perspective originalPerspective;
    private int originalFov;
    private long generationFinishedAt;
    private Path diagnosticRoot;
    private int expectedFoundation,matchingFoundation,residentReadinessPolls;
    private record Feedback(String key,BlockPos position,int distance,String text){}

    public static void observeServerFeedback(UUID player,Text message){
        var audit=active;if(audit!=null&&player.equals(audit.playerId))audit.serverMessages.add(message.copy());
    }
    public void start(MinecraftClient client){
        require(!started,"Camp discovery audit started twice");started=true;playerId=client.player.getUuid();active=this;diagnosticRoot=client.runDirectory.toPath();
        originalPerspective=client.options.getPerspective();originalFov=client.options.getFov().getValue();
        clearKeys(client);client.setScreen(null);client.options.setPerspective(Perspective.FIRST_PERSON);client.options.getFov().setValue(60);
        server(client,player->{
            var world=player.getServerWorld();require(world.getRegistryKey().equals(World.OVERWORLD),"Graphical camp discovery requires the real Overworld");
            require(world.getChunkManager().getChunkGenerator() instanceof NoiseChunkGenerator,"Camp discovery cannot use a flat or manually seeded structure fixture");
            require(world.getServer().getSaveProperties().getGeneratorOptions().shouldGenerateStructures(),"Normal-world structure generation is disabled");
            originalWorld=world;originalPosition=player.getPos();originalYaw=player.getYaw();originalPitch=player.getPitch();originalMode=player.interactionManager.getGameMode();
            originalGravity=player.hasNoGravity();originalFlying=player.getAbilities().flying;originalInvulnerable=player.getAbilities().invulnerable;
            var vision=player.getStatusEffect(StatusEffects.NIGHT_VISION);originalVision=vision==null?null:new StatusEffectInstance(vision);
            oldOperator=world.getServer().getPlayerManager().isOperator(player.getGameProfile());if(!oldOperator)world.getServer().getPlayerManager().addToOperators(player.getGameProfile());
            player.changeGameMode(GameMode.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;player.getAbilities().invulnerable=true;player.sendAbilitiesUpdate();
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,12000,0,false,false,false));player.setVelocity(Vec3d.ZERO);
            campEvidence.put("WorldKey",world.getRegistryKey().getValue().toString());campEvidence.put("Seed",world.getSeed());campEvidence.put("StructuresEnabled",true);campEvidence.put("NativeNoiseGenerator",true);
            campEvidence.put("OriginalPlayerPosition",position(originalPosition));
            campEvidence.put("FixedObserverOrigin",position(OBSERVER_ORIGIN));campEvidence.put("ObserverPreparation","Creative observer teleport before native commands; no structures, POI or residents inserted");
            player.teleport(world,OBSERVER_ORIGIN.x,OBSERVER_ORIGIN.y,OBSERVER_ORIGIN.z,0,0);
        });
    }
    public boolean tick(MinecraftClient client){
        try{return tickInternal(client);}catch(RuntimeException|Error problem){restore(client);throw problem;}
    }
    private boolean tickInternal(MinecraftClient client){
        require(started,"Camp discovery audit not started");if(complete)return true;
        if(failure!=null){restore(client);throw new IllegalStateException("Native graphical camp discovery failed",failure);}
        require(++ticks<6000,"Camp discovery timed out in phase "+phase+" awaiting "+pendingCommand+"; actual server feedback="+serverMessages.stream().map(Text::getString).toList());
        if(!serverReady)return false;phaseTicks++;
        switch(phase){
            case 0 -> {
                if(phaseTicks<40||!client.player.hasPermissionLevel(2)||!client.player.getAbilities().creativeMode||client.player.squaredDistanceTo(OBSERVER_ORIGIN)>0.25)return false;
                beginCommand(client,"locate structure rustandruin:medic_tent","commands.locate.structure.success");next();
            }
            case 1 -> {
                var reply=awaitCommand(client);if(reply==null)return false;structureFeedback=reply;
                screenshot(client,"locate-structure-chat");beginCommand(client,"rustandruin locate tent","commands.locate.structure.success");next();
            }
            case 2 -> {
                var reply=awaitCommand(client);if(reply==null)return false;
                require(sameXZ(reply.position(),structureFeedback.position()),"Alias and native locate disagree from the unchanged fixture observer position");
                screenshot(client,"locate-alias-chat");client.setScreen(null);
                server(client,this::loadLocatedCamp);next();
            }
            case 3 -> {
                if(residentInspectionReady){next();return false;}
                if(phaseTicks>=1200){failWithDiagnostics(client,"resident-readiness","Native ticking futures/entity census did not settle: "+campEvidence.get("NativeResidentReadiness"));return false;}
                if(client.world.getTime()-generationFinishedAt<60||client.player.squaredDistanceTo(room)>0.16)return false;
                if(phaseTicks%10==0)server(client,player->{
                    if(!readyForResidentInspection(player))return;
                    inspectResidents(player);inspectLiveFarm(player);residentInspectionReady=true;
                });
            }
            case 4 -> {
                var medic=clientEntity(client,medicId);var golem=clientEntity(client,golemId);var support=clientEntity(client,farmerId);
                var lower=client.world.getBlockState(cabinet);var upper=client.world.getBlockState(cabinet.up());
                boolean cabinetSynced=lower.isOf(ExpansionBlocks.FIELD_MEDIC_CABINET)&&upper.isOf(ExpansionBlocks.FIELD_MEDIC_CABINET)
                        &&lower.get(DoorBlock.HALF)==DoubleBlockHalf.LOWER&&upper.get(DoorBlock.HALF)==DoubleBlockHalf.UPPER
                        &&lower.get(MedicineCabinetBlock.COMPLETE)&&upper.get(MedicineCabinetBlock.COMPLETE);
                boolean chestSynced=client.world.getBlockEntity(chest) instanceof ChestBlockEntity;
                if(medic==null||golem==null||support==null||!cabinetSynced||!chestSynced){
                    if(phaseTicks>=240)failWithDiagnostics(client,"client-camp-sync","medic="+medic+", golem="+golem+", cabinetSynced="+cabinetSynced+", chestSynced="+chestSynced);
                    return false;
                }
                require(medic instanceof VillagerEntity&&golem instanceof IronGolemEntity,"Actual client received incorrect naturally generated camp residents");
                boolean guardSynced=((IronGolemEntity)golem).getMaxHealth()==200&&MobVariants.permanent((IronGolemEntity)golem);
                if(!guardSynced&&phaseTicks<100)return false;
                require(guardSynced,"Actual client did not receive the200HP Tetanized defender attributes/skin metadata");
                require(support instanceof VillagerEntity farmer&&farmer.getVillagerData().getProfession()==net.minecraft.village.VillagerProfession.FARMER,"Actual client support is not a farmer");campEvidence.put("ClientSupportFarmerUuid",support.getUuid().toString());campEvidence.put("ClientMedicUuid",medic.getUuid().toString());campEvidence.put("ClientGuardUuid",golem.getUuid().toString());campEvidence.put("ClientGuardMaxHealth",((IronGolemEntity)golem).getMaxHealth());
                aim(client,Vec3d.ofCenter(cabinet).add(0,0.5,0));next();
            }
            case 5 -> {
                if(phaseTicks<25)return false;screenshot(client,"field-cabinet");
                var medic=clientEntity(client,medicId);require(medic!=null,"Naturally generated medic left client tracking");aim(client,medic.getPos().add(0,1.1,0));next();
            }
            case 6 -> {
                if(phaseTicks<=50){
                    var actor=clientEntity(client,phaseTicks<25?medicId:farmerId);if(actor==null)return false;aim(client,actor.getPos().add(0,1.1,0));
                    if(phaseTicks==25)screenshot(client,"natural-field-medic");
                    if(phaseTicks==50){screenshot(client,"natural-support-farmer");server(client,player->player.teleport(player.getServerWorld(),farmView.x,farmView.y,farmView.z,0,0));}
                    return false;
                }
                aim(client,Vec3d.ofCenter(irrigation).add(0,0.6,0));
                if(client.player.squaredDistanceTo(farmView)>0.16){require(phaseTicks<300,"Native farm observer teleport did not synchronize");return false;}
                if(phaseTicks<85)return false;screenshot(client,"natural-farm");
                server(client,player->player.teleport(player.getServerWorld(),room.x,room.y,room.z,0,0));next();
            }
            case 7 -> {
                var liveGolem=clientEntity(client,golemId);if(liveGolem==null)return false;aim(client,liveGolem.getPos().add(0,1.6,0));
                if(phaseTicks<25||client.player.squaredDistanceTo(room)>0.16)return false;screenshot(client,"natural-tetanized-guard");
                beginCommand(client,"locate poi rustandruin:medic_tent","commands.locate.poi.success");next();
            }
            case 8 -> {
                var reply=awaitCommand(client);if(reply==null)return false;
                require(sameXZ(reply.position(),cabinet),"Native nearby tent POI did not identify the naturally generated field cabinet");
                screenshot(client,"locate-poi-chat");client.setScreen(null);
                server(client,player->{
                    var state=player.getServerWorld().getBlockState(chest);var facing=state.get(ChestBlock.FACING);
                    var stand=Vec3d.ofBottomCenter(chest.offset(facing,1));
                    require(player.getServerWorld().getBlockState(BlockPos.ofFloored(stand)).isAir()&&player.getServerWorld().getBlockState(BlockPos.ofFloored(stand).up()).isAir(),"Natural chest approach is obstructed");
                    player.teleport(player.getServerWorld(),stand.x,stand.y,stand.z,0,0);
                });next();
            }
            case 9 -> {
                aim(client,Vec3d.ofCenter(chest));
                if(phaseTicks<25||!(client.crosshairTarget instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK||!hit.getBlockPos().equals(chest))return false;
                screenshot(client,"natural-loot-chest");client.options.useKey.setPressed(true);next();
            }
            case 10 -> {
                if(phaseTicks>=2)client.options.useKey.setPressed(false);
                if(!(client.currentScreen instanceof GenericContainerScreen menu))return false;
                if(phaseTicks<15)return false;int stacks=0;
                for(int i=0;i<27;i++)if(!menu.getScreenHandler().getSlot(i).getStack().isEmpty())stacks++;
                require(stacks>0,"Actual client opened an empty naturally generated loot chest");campEvidence.put("ClientGeneratedLootStacks",stacks);
                screenshot(client,"opened-natural-loot");client.player.closeHandledScreen();
                server(client,player->{var block=player.getServerWorld().getBlockEntity(chest);require(block instanceof ChestBlockEntity,"Natural loot chest disappeared");var inventory=(ChestBlockEntity)block;int count=0;for(int i=0;i<inventory.size();i++)if(!inventory.getStack(i).isEmpty())count++;require(count>0,"Native chest interaction failed to generate any loot");campEvidence.put("ServerGeneratedLootStacks",count);});next();
            }
            case 11 -> {restore(client);next();}
            case 12 -> {
                if(!restored)return false;
                var report=new LinkedHashMap<String,Object>();report.put("Result","PASS");report.put("PlayerUuid",playerId.toString());report.put("ActualClientNetworkCommands",commands);report.put("Camp",campEvidence);report.put("ManuallyPlacedTemplates",0);report.put("ManuallyInsertedStarts",0);report.put("ManuallyPlacedPoi",0);report.put("ManuallySpawnedResidents",0);report.put("PlayerStateRestored",true);
                try{Files.writeString(client.runDirectory.toPath().resolve("graphical-camp-discovery-"+viewer+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}catch(Exception problem){throw new IllegalStateException(problem);}
                complete=true;return true;
            }
            default -> throw new IllegalStateException("Unknown camp discovery phase "+phase);
        }
        return false;
    }
    private void failWithDiagnostics(MinecraftClient client,String stage,String reason){
        String local="stage="+stage+", reason="+reason+", clientPos="+client.player.getPos()+", clientTime="+client.world.getTime()
                +", generatedAt="+generationFinishedAt+", expectedRoom="+room+", bounds="+bounds
                +", cabinet="+cabinet+", lower="+client.world.getBlockState(cabinet)+", upper="+client.world.getBlockState(cabinet.up())
                +", chest="+chest+", chestBlockEntity="+client.world.getBlockEntity(chest)+", medicUuid="+medicId+", guardUuid="+golemId;
        screenshot(client,"blocked-"+stage);
        server(client,player->{
            var world=player.getServerWorld();
            throw new IllegalStateException(local+"; authoritative serverPos="+player.getPos()+", serverTime="+world.getTime()
                    +", serverWorld="+world.getRegistryKey().getValue()+", flying="+player.getAbilities().flying+", noGravity="+player.hasNoGravity()
                    +", nativeRoomBlock="+world.getBlockState(BlockPos.ofFloored(room))+", nativeRoomAbove="+world.getBlockState(BlockPos.ofFloored(room).up())
                    +", nativeCabinet="+world.getBlockState(cabinet)+", nativeChest="+world.getBlockEntity(chest)
                    +", nativeMedic="+(medicId==null?null:world.getEntity(medicId))+", nativeGuard="+(golemId==null?null:world.getEntity(golemId)));
        });
    }
    private void beginCommand(MinecraftClient client,String command,String key){
        pendingCommand=command;pendingKey=key;serverMessageBase=serverMessages.size();oldChatLines.clear();oldChatLines.addAll(chat(client));
        client.setScreen(new ChatScreen(""));client.player.networkHandler.sendChatCommand(command);
        org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_CAMP_NETWORK_COMMAND /{}",command);
    }
    private Feedback awaitCommand(MinecraftClient client){
        Feedback fromServer=null,fromClient=null;
        for(int i=serverMessageBase;i<serverMessages.size();i++){
            var message=serverMessages.get(i);var feedback=feedback(message);
            if(feedback!=null&&feedback.key().equals(pendingKey)){fromServer=feedback;break;}
            if(message.getContent() instanceof TranslatableTextContent content&&content.getKey().startsWith("commands.locate.")&&!content.getKey().endsWith(".success"))throw new IllegalStateException("Actual native command failed /"+pendingCommand+": "+message.getString());
        }
        for(var line:chat(client))if(!oldChatLines.contains(line)){var feedback=feedback(line.content());if(feedback!=null&&feedback.key().equals(pendingKey)){fromClient=feedback;break;}}
        if(fromServer==null||fromClient==null)return null;
        require(sameXZ(fromServer.position(),fromClient.position())&&fromServer.distance()==fromClient.distance(),"Actual server/client locate feedback differs");
        if(phaseTicks<25)return null;
        var row=new LinkedHashMap<String,Object>();row.put("Command","/"+pendingCommand);row.put("ServerSuccessTranslationKey",fromServer.key());row.put("ClientSuccessTranslationKey",fromClient.key());row.put("ServerMessage",fromServer.text());row.put("ClientChatMessage",fromClient.text());row.put("NativeLocatedX",fromServer.position().getX());row.put("NativeLocatedZ",fromServer.position().getZ());row.put("NativeDisplayedY","~");row.put("NativeDistanceBlocks",fromServer.distance());commands.add(row);
        org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_CAMP_COMMAND_PASS {} x={} z={} distance={}",pendingCommand,fromServer.position().getX(),fromServer.position().getZ(),fromServer.distance());
        return fromServer;
    }
    private void loadLocatedCamp(ServerPlayerEntity player){
        var world=player.getServerWorld();var target=world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(TetanusMod.id("medic_tent"));var chunk=new ChunkPos(structureFeedback.position());
        var start=world.getChunk(chunk.x,chunk.z,ChunkStatus.STRUCTURE_STARTS,true).getStructureStart(target);
        require(start!=null&&start.hasChildren()&&start.getChildren().size()==1&&start.getChildren().get(0) instanceof MedicStructurePiece,"Actual locate did not identify a saved native camp start");
        structureStart=start;bounds=start.getChildren().get(0).getBoundingBox();
        require(bounds.getBlockCountX()==13&&bounds.getBlockCountZ()==13&&bounds.getBlockCountY()==8,"Native camp has unexpected bounds "+bounds);
        for(int x=bounds.getMinX()>>4;x<=bounds.getMaxX()>>4;x++)for(int z=bounds.getMinZ()>>4;z<=bounds.getMaxZ()>>4;z++)world.getChunk(x,z);
        int lower=0,upper=0,beds=0,plots=0,crops=0,rails=0,gates=0,composters=0;
        var campPiece=(MedicStructurePiece)start.getChildren().get(0);irrigation=net.minecraft.structure.StructureTemplate.transform(campPiece.getPlacementData(),new BlockPos(10,1,4)).add(campPiece.getPos());
        recordFluidDiagnostics(world,campPiece,chunk);
        require(expectedFoundation==169&&matchingFoundation==169,"Natural camp foundation was overwritten: "+matchingFoundation+"/"+expectedFoundation+" native template blocks intact");
        farmView=Vec3d.ofBottomCenter(net.minecraft.structure.StructureTemplate.transform(campPiece.getPlacementData(),new BlockPos(11,2,6)).add(campPiece.getPos()));
        require(world.getBlockState(BlockPos.ofFloored(farmView)).isAir()&&world.getBlockState(BlockPos.ofFloored(farmView).up()).isAir(),"Farm observer view is obstructed");
        campEvidence.put("FarmObserverView",position(farmView));
        for(var mutable:BlockPos.iterate(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX(),bounds.getMaxY(),bounds.getMaxZ())){
            var pos=mutable.toImmutable();var state=world.getBlockState(pos);require(state.getFluidState().isEmpty()||(pos.equals(irrigation)&&state.isOf(Blocks.WATER)),"Natural camp flooded outside its exact irrigation cell: "+pos);
            if(state.isOf(Blocks.WHEAT))crops++;if(state.getBlock() instanceof BedBlock&&state.get(BedBlock.PART)==net.minecraft.block.enums.BedPart.HEAD)beds++;if(state.isOf(Blocks.FARMLAND))plots++;if(state.getBlock() instanceof FenceBlock)rails++;if(state.getBlock() instanceof FenceGateBlock)gates++;if(state.isOf(Blocks.COMPOSTER))composters++;
            if(state.isOf(ExpansionBlocks.FIELD_MEDIC_CABINET)){if(state.get(DoorBlock.HALF)==DoubleBlockHalf.LOWER){lower++;cabinet=pos;}else upper++;}
            if(state.isOf(Blocks.CHEST))chest=pos;
        }
        require(crops==8,"Native cave camp crops vanished during generation: "+crops+"/8");campEvidence.put("NativeInitialWheatBlocks",crops);
        require(beds==2&&plots==8&&rails>=20&&gates==6&&composters==1,"Natural camp lacks beds, farming or zombie barriers");require(world.getBlockState(irrigation.down()).isSolidBlock(world,irrigation.down()),"Irrigation basin has no watertight bottom");campEvidence.put("NativeBeds",beds);campEvidence.put("NativeFarmlandBlocks",plots);campEvidence.put("NativeFenceBlocks",rails);campEvidence.put("NativeGateBlocks",gates);campEvidence.put("NativeIrrigation",block(irrigation));
        require(lower==1&&upper==1&&cabinet!=null&&chest!=null,"Natural camp is missing its two-block field cabinet or chest");
        require(world.getBlockEntity(chest)!=null&&world.getBlockEntity(chest).createNbt().getString("LootTable").equals("tetanusmod:chests/medic_tent"),"Natural camp chest has no unopened camp loot table");
        room=new Vec3d(bounds.getMinX()+6.5,bounds.getMinY()+1,bounds.getMinZ()+6.5);
        require(world.getBlockState(BlockPos.ofFloored(room)).isAir()&&world.getBlockState(BlockPos.ofFloored(room).up()).isAir(),"Natural tent room is obstructed");
        campEvidence.put("NativeStartChunk",Map.of("x",start.getPos().x,"z",start.getPos().z));campEvidence.put("BoundingBox",bounds.toString());campEvidence.put("ActualRoomPosition",position(room));campEvidence.put("ActualCabinet",block(cabinet));campEvidence.put("ActualLootChest",block(chest));campEvidence.put("UnopenedLootTable","tetanusmod:chests/medic_tent");campEvidence.put("FullNativeChunksGenerated",true);
        player.teleport(world,room.x,room.y,room.z,0,0);generationFinishedAt=world.getTime();
    }
    /** Block generation can finish before native entity-region IO and ticking futures. */
    private boolean readyForResidentInspection(ServerPlayerEntity player){
        var world=player.getServerWorld();var pending=new ArrayList<Map<String,Integer>>();
        for(int x=bounds.getMinX()>>4;x<=bounds.getMaxX()>>4;x++)for(int z=bounds.getMinZ()>>4;z<=bounds.getMaxZ()>>4;z++)if(!world.getChunkManager().isTickingFutureReady(ChunkPos.toLong(x,z)))pending.add(Map.of("x",x,"z",z));
        var area=new Box(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX()+1,bounds.getMaxY()+1,bounds.getMaxZ()+1).expand(32);
        int medics=world.getEntitiesByClass(VillagerEntity.class,area,v->v.getCommandTags().contains("rustruin_camp_medic")&&v.getVillagerData().getProfession()==MedicVillage.MEDIC&&v.writeNbt(new NbtCompound()).getBoolean("RustRuinFieldMedic")).size();
        int farmers=world.getEntitiesByClass(VillagerEntity.class,area,v->v.getCommandTags().contains("rustruin_camp_support")&&v.getVillagerData().getProfession()==net.minecraft.village.VillagerProfession.FARMER).size();
        var guards=world.getEntitiesByClass(IronGolemEntity.class,area,g->g.getCommandTags().contains(MedicStructurePiece.CAMP_GUARD_TAG)&&MobVariants.permanent(g)&&g.isPlayerCreated()&&g.getMaxHealth()==200);
        var observation=new LinkedHashMap<String,Object>();observation.put("Poll",++residentReadinessPolls);observation.put("NativeWorldTicksSinceGeneration",world.getTime()-generationFinishedAt);observation.put("PendingTickingChunks",pending);observation.put("NativeMedics",medics);observation.put("NativeSupportFarmers",farmers);observation.put("NativeGuards",guards.size());observation.put("NativeGuardUuids",guards.stream().map(g->g.getUuid().toString()).toList());
        campEvidence.put("NativeResidentReadiness",observation);
        if(residentReadinessPolls==1||residentReadinessPolls%10==0||pending.isEmpty()&&medics>0&&farmers>0&&!guards.isEmpty())org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_CAMP_NATIVE_RESIDENT_READINESS {}",observation);
        return pending.isEmpty()&&medics>0&&farmers>0&&!guards.isEmpty();
    }
    private void inspectResidents(ServerPlayerEntity player){
        var world=player.getServerWorld();require(world.getPointOfInterestStorage().hasTypeAt(MedicVillage.FIELD_JOB_KEY,cabinet),"Natural field cabinet is not indexed as its tent POI");
        require(world.getPointOfInterestStorage().getType(cabinet.up()).isEmpty(),"Upper cabinet registered a duplicate POI");
        var box=new Box(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX()+1,bounds.getMaxY()+1,bounds.getMaxZ()+1).expand(32);
        var medics=world.getEntitiesByClass(VillagerEntity.class,box,villager->villager.getVillagerData().getProfession()==MedicVillage.MEDIC&&villager.writeNbt(new NbtCompound()).getBoolean("RustRuinFieldMedic"));
        var golems=world.getEntitiesByClass(IronGolemEntity.class,box,golem->golem.getCommandTags().contains(MedicStructurePiece.CAMP_GUARD_TAG)&&MobVariants.permanent(golem)&&golem.isPlayerCreated()&&golem.getMaxHealth()==200);
        var farmers=world.getEntitiesByClass(VillagerEntity.class,new Box(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX()+1,bounds.getMaxY()+1,bounds.getMaxZ()+1),villager->villager.getCommandTags().contains("rustruin_camp_support")&&villager.getVillagerData().getProfession()==net.minecraft.village.VillagerProfession.FARMER);require(farmers.size()==1,"Natural camp missing support farmer");farmerId=farmers.get(0).getUuid();campEvidence.put("NativeSupportFarmerUuid",farmerId.toString());
        require(medics.size()==1&&golems.size()==1,"Naturally generated camp did not load exactly one field medic and200HP friendly Tetanized guard: "+medics.size()+"/"+golems.size());
        medicId=medics.get(0).getUuid();golemId=golems.get(0).getUuid();campEvidence.put("NativeFieldMedicUuid",medicId.toString());campEvidence.put("NativeGuardUuid",golemId.toString());campEvidence.put("NativeGuardMaxHealth",golems.get(0).getMaxHealth());campEvidence.put("NativeGuardCurrentHealth",golems.get(0).getHealth());campEvidence.put("NativeGuardPlayerCreated",golems.get(0).isPlayerCreated());require(golems.get(0).isPersistent(),"Camp guard lacks resident persistence");campEvidence.put("NativeGuardPersistent",golems.get(0).isPersistent());campEvidence.put("NativeTentPoiIndexed",true);
    }
    private List<Map<String,Object>> observeFoundation(ServerWorld world,MedicStructurePiece piece){
        expectedFoundation=0;matchingFoundation=0;var mismatches=new ArrayList<Map<String,Object>>();var placement=piece.getPlacementData().copy().setBoundingBox(null);
        for(var material:List.of(Blocks.STONE_BRICKS,Blocks.MOSSY_STONE_BRICKS,Blocks.POLISHED_ANDESITE))for(var info:piece.getTemplate().getInfosForBlock(piece.getPos(),placement,material)){
            if(info.pos().getY()!=piece.getPos().getY())continue;expectedFoundation++;var actual=world.getBlockState(info.pos());if(actual.equals(info.state()))matchingFoundation++;else mismatches.add(Map.of("Position",block(info.pos()),"Expected",info.state().toString(),"Actual",actual.toString()));
        }
        return mismatches;
    }
    private void inspectLiveFarm(ServerPlayerEntity player){
        var world=player.getServerWorld();int crops=0,minLight=15;var piece=(MedicStructurePiece)structureStart.getChildren().get(0);var cells=new ArrayList<Map<String,Object>>();
        for(int x=9;x<=11;x++)for(int z=3;z<=5;z++){
            if(x==10&&z==4)continue;var pos=net.minecraft.structure.StructureTemplate.transform(piece.getPlacementData(),new BlockPos(x,2,z)).add(piece.getPos());var state=world.getBlockState(pos);int light=world.getBaseLightLevel(pos,0);
            if(state.isOf(Blocks.WHEAT))crops++;minLight=Math.min(minLight,light);cells.add(Map.of("Position",block(pos),"State",state.toString(),"BaseLight",light,"SkyVisible",world.isSkyVisible(pos)));
        }
        campEvidence.put("NativeLiveFarmCells",cells);campEvidence.put("NativeLiveWheatBlocks",crops);campEvidence.put("NativeMinimumCropLight",minLight);
        var lateFloor=observeFoundation(world,piece);campEvidence.put("NativeLateIntactFoundationBlocks",matchingFoundation);campEvidence.put("NativeLateFoundationMismatches",lateFloor);
        require(expectedFoundation==169&&matchingFoundation==169,"Neighboring generation damaged the live camp foundation: "+lateFloor);
        require(crops>0,"Naturally generated cave farm has no live crops after native chunk lighting and resident ticks");require(minLight>=9,"Cave crop patch is not lit sufficiently for native crop growth: "+minLight);
    }
    /** Persist the complete native fluid observation before any safety assertion can throw. */
    private void recordFluidDiagnostics(ServerWorld world,MedicStructurePiece piece,ChunkPos startChunk){
        var report=new LinkedHashMap<String,Object>();
        report.put("Result","OBSERVED_NOT_A_PASS");report.put("Seed",world.getSeed());report.put("World",world.getRegistryKey().getValue().toString());
        report.put("NativeStartChunk",Map.of("x",startChunk.x,"z",startChunk.z));report.put("PieceOrigin",block(piece.getPos()));report.put("NativeRotation",piece.getRotation().name());
        report.put("Bounds",Map.of("min",block(new BlockPos(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ())),"max",block(new BlockPos(bounds.getMaxX(),bounds.getMaxY(),bounds.getMaxZ()))));
        report.put("PlannedIrrigation",block(irrigation));report.put("PlannedIrrigationState",world.getBlockState(irrigation).toString());report.put("PlannedIrrigationBottom",world.getBlockState(irrigation.down()).toString());
        report.put("Biome",world.getBiome(irrigation).getKey().map(Object::toString).orElse("unkeyed"));
        var crops=new ArrayList<Map<String,Object>>();for(int x=9;x<=11;x++)for(int z=3;z<=5;z++){if(x==10&&z==4)continue;var pos=net.minecraft.structure.StructureTemplate.transform(piece.getPlacementData(),new BlockPos(x,2,z)).add(piece.getPos());crops.add(Map.of("Position",block(pos),"State",world.getBlockState(pos).toString(),"BaseLight",world.getBaseLightLevel(pos,0),"SkyVisible",world.isSkyVisible(pos)));}report.put("NativeCropCellsBeforeSafetyChecks",crops);
        var fluids=new ArrayList<Map<String,Object>>();
        for(var mutable:BlockPos.iterate(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX(),bounds.getMaxY(),bounds.getMaxZ())){
            var pos=mutable.toImmutable();var state=world.getBlockState(pos);var fluid=state.getFluidState();if(fluid.isEmpty())continue;
            var row=new LinkedHashMap<String,Object>();row.put("Position",block(pos));row.put("BlockState",state.toString());row.put("FluidState",fluid.toString());row.put("FluidLevel",fluid.getLevel());row.put("Source",fluid.isStill());row.put("PlannedCell",pos.equals(irrigation));
            row.put("Below",world.getBlockState(pos.down()).toString());row.put("Above",world.getBlockState(pos.up()).toString());
            var adjacent=new LinkedHashMap<String,String>();for(var direction:Direction.Type.HORIZONTAL)adjacent.put(direction.getName(),world.getBlockState(pos.offset(direction)).toString());row.put("HorizontalNeighbours",adjacent);fluids.add(row);
        }
        report.put("NativeFluidCells",fluids);report.put("FluidCellCount",fluids.size());
        var floorMismatches=observeFoundation(world,piece);
        report.put("ExpectedFoundationBlocks",expectedFoundation);report.put("MatchingFoundationBlocks",matchingFoundation);report.put("FoundationMismatches",floorMismatches);
        campEvidence.put("NativeExpectedFoundationBlocks",expectedFoundation);campEvidence.put("NativeIntactFoundationBlocks",matchingFoundation);
        var json=new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n";
        try{Files.writeString(diagnosticRoot.resolve("graphical-camp-fluid-diagnostics-"+viewer+".json"),json);}catch(Exception problem){throw new IllegalStateException("Cannot preserve camp fluid diagnostics",problem);}
        org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_CAMP_FLUID_DIAGNOSTICS seed={} origin={} rotation={} bounds={} planned={} plannedState={} fluids={}",world.getSeed(),piece.getPos(),piece.getRotation(),bounds,irrigation,world.getBlockState(irrigation),fluids);
    }
    private static Feedback feedback(Text text){
        if(!(text.getContent() instanceof TranslatableTextContent content)||!Set.of("commands.locate.structure.success","commands.locate.poi.success").contains(content.getKey())||!(text.getString().contains("tetanusmod:medic_tent")||text.getString().contains("rustandruin:medic_tent")))return null;
        var coordinates=coordinates(text);require(coordinates!=null,"Native locate success has no native coordinates");var args=content.getArgs();require(args.length>=3,"Native locate success lost its distance argument");return new Feedback(content.getKey(),coordinates,number(args[2]),text.getString());
    }
    private static BlockPos coordinates(Text text){
        if(text.getContent() instanceof TranslatableTextContent content){if(content.getKey().equals("chat.coordinates")){var args=content.getArgs();return new BlockPos(number(args[0]),0,number(args[2]));}for(var arg:content.getArgs())if(arg instanceof Text child){var found=coordinates(child);if(found!=null)return found;}}
        for(var child:text.getSiblings()){var found=coordinates(child);if(found!=null)return found;}return null;
    }
    private static int number(Object value){return value instanceof Number number?number.intValue():Integer.parseInt(value.toString());}
    @SuppressWarnings("unchecked") private static List<ChatHudLine> chat(MinecraftClient client){try{var field=client.inGameHud.getChatHud().getClass().getDeclaredField("messages");field.setAccessible(true);return List.copyOf((List<ChatHudLine>)field.get(client.inGameHud.getChatHud()));}catch(Exception problem){throw new IllegalStateException("Cannot inspect actual native client chat",problem);}}
    private static Entity clientEntity(MinecraftClient client,UUID id){for(var entity:client.world.getEntities())if(entity.getUuid().equals(id))return entity;return null;}
    private void server(MinecraftClient client,Consumer<ServerPlayerEntity> action){require(serverReady,"Overlapping camp server observations");serverReady=false;client.getServer().execute(()->{try{var player=client.getServer().getPlayerManager().getPlayer(playerId);require(player!=null,"Connected camp actor disappeared");action.accept(player);}catch(Throwable problem){failure=problem;}finally{serverReady=true;}});}
    private void screenshot(MinecraftClient client,String label){try(var frame=ScreenshotRecorder.takeScreenshot(client.getFramebuffer())){var folder=client.runDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);frame.writeTo(folder.resolve("rust-and-ruin-"+viewer+"-camp-"+label+".png"));}catch(Exception problem){throw new IllegalStateException("Cannot capture actual native camp",problem);}}
    private static void aim(MinecraftClient client,Vec3d target){var delta=target.subtract(client.player.getEyePos());client.player.setYaw((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));client.player.setPitch((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));}
    private static void clearKeys(MinecraftClient client){client.options.forwardKey.setPressed(false);client.options.backKey.setPressed(false);client.options.leftKey.setPressed(false);client.options.rightKey.setPressed(false);client.options.jumpKey.setPressed(false);client.options.sneakKey.setPressed(false);client.options.useKey.setPressed(false);}
    private void next(){phase++;phaseTicks=0;}
    private void restore(MinecraftClient client){
        clearKeys(client);client.setScreen(null);client.options.setPerspective(originalPerspective);client.options.getFov().setValue(originalFov);active=null;
        if(restoreQueued)return;restoreQueued=true;if(client.getServer()==null){restored=true;return;}
        client.getServer().execute(()->{try{var player=client.getServer().getPlayerManager().getPlayer(playerId);if(player!=null&&originalWorld!=null){player.closeHandledScreen();player.changeGameMode(originalMode);player.setNoGravity(originalGravity);player.getAbilities().flying=originalFlying;player.getAbilities().invulnerable=originalInvulnerable;player.sendAbilitiesUpdate();player.removeStatusEffect(StatusEffects.NIGHT_VISION);if(originalVision!=null)player.addStatusEffect(originalVision);player.teleport(originalWorld,originalPosition.x,originalPosition.y,originalPosition.z,originalYaw,originalPitch);if(!oldOperator)player.getServer().getPlayerManager().removeFromOperators(player.getGameProfile());}}catch(Throwable problem){failure=problem;}finally{restored=true;}});
    }
    private static boolean sameXZ(BlockPos a,BlockPos b){return a.getX()==b.getX()&&a.getZ()==b.getZ();}
    private static Map<String,Object> block(BlockPos p){return Map.of("x",p.getX(),"y",p.getY(),"z",p.getZ());}
    private static Map<String,Object> position(Vec3d p){return Map.of("x",p.x,"y",p.y,"z",p.z);}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
