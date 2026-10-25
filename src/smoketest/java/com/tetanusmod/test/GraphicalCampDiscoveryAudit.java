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
    private final List<Text> serverMessages=new CopyOnWriteArrayList<>();
    private final List<Map<String,Object>> commands=new ArrayList<>();
    private final Map<String,Object> campEvidence=new LinkedHashMap<>();
    private final Set<ChatHudLine> oldChatLines=Collections.newSetFromMap(new IdentityHashMap<>());
    private final String viewer=FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
    private UUID playerId,medicId,golemId;
    private volatile Throwable failure;
    private volatile boolean serverReady=true,restored;
    private boolean started,complete,restoreQueued,oldOperator;
    private int ticks,phase,phaseTicks,serverMessageBase;
    private String pendingCommand,pendingKey;
    private Feedback structureFeedback;
    private StructureStart structureStart;
    private BlockBox bounds;
    private BlockPos cabinet,chest;
    private ServerWorld originalWorld;
    private Vec3d originalPosition,room;
    private GameMode originalMode;
    private boolean originalGravity,originalFlying,originalInvulnerable;
    private float originalYaw,originalPitch;
    private StatusEffectInstance originalVision;
    private Perspective originalPerspective;
    private int originalFov;
    private long generationFinishedAt;
    private record Feedback(String key,BlockPos position,int distance,String text){}

    public static void observeServerFeedback(UUID player,Text message){
        var audit=active;if(audit!=null&&player.equals(audit.playerId))audit.serverMessages.add(message.copy());
    }
    public void start(MinecraftClient client){
        require(!started,"Camp discovery audit started twice");started=true;playerId=client.player.getUuid();active=this;
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
            campEvidence.put("OriginalSpawnPosition",position(originalPosition));
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
                if(phaseTicks<40||!client.player.hasPermissionLevel(2)||!client.player.getAbilities().creativeMode)return false;
                beginCommand(client,"locate structure tetanusmod:medic_tent","commands.locate.structure.success");next();
            }
            case 1 -> {
                var reply=awaitCommand(client);if(reply==null)return false;structureFeedback=reply;
                screenshot(client,"locate-structure-chat");beginCommand(client,"rustandruin locate tent","commands.locate.structure.success");next();
            }
            case 2 -> {
                var reply=awaitCommand(client);if(reply==null)return false;
                require(sameXZ(reply.position(),structureFeedback.position()),"Alias and native locate disagree from the unchanged spawn position");
                screenshot(client,"locate-alias-chat");client.setScreen(null);
                server(client,this::loadLocatedCamp);next();
            }
            case 3 -> {
                if(client.world.getTime()-generationFinishedAt<60||client.player.squaredDistanceTo(room)>0.16){
                    if(phaseTicks>=400)failWithDiagnostics(client,"room-arrival","Client time/position did not settle");
                    return false;
                }
                server(client,this::inspectResidents);next();
            }
            case 4 -> {
                var medic=clientEntity(client,medicId);var golem=clientEntity(client,golemId);
                var lower=client.world.getBlockState(cabinet);var upper=client.world.getBlockState(cabinet.up());
                boolean cabinetSynced=lower.isOf(ExpansionBlocks.FIELD_MEDIC_CABINET)&&upper.isOf(ExpansionBlocks.FIELD_MEDIC_CABINET)
                        &&lower.get(DoorBlock.HALF)==DoubleBlockHalf.LOWER&&upper.get(DoorBlock.HALF)==DoubleBlockHalf.UPPER
                        &&lower.get(MedicineCabinetBlock.COMPLETE)&&upper.get(MedicineCabinetBlock.COMPLETE);
                boolean chestSynced=client.world.getBlockEntity(chest) instanceof ChestBlockEntity;
                if(medic==null||golem==null||!cabinetSynced||!chestSynced){
                    if(phaseTicks>=240)failWithDiagnostics(client,"client-camp-sync","medic="+medic+", golem="+golem+", cabinetSynced="+cabinetSynced+", chestSynced="+chestSynced);
                    return false;
                }
                require(medic instanceof VillagerEntity&&golem instanceof IronGolemEntity,"Actual client received incorrect naturally generated camp residents");
                boolean guardSynced=((IronGolemEntity)golem).getMaxHealth()==200&&MobVariants.permanent((IronGolemEntity)golem);
                if(!guardSynced&&phaseTicks<100)return false;
                require(guardSynced,"Actual client did not receive the200HP Tetanized defender attributes/skin metadata");
                campEvidence.put("ClientMedicUuid",medic.getUuid().toString());campEvidence.put("ClientGuardUuid",golem.getUuid().toString());campEvidence.put("ClientGuardMaxHealth",((IronGolemEntity)golem).getMaxHealth());
                aim(client,Vec3d.ofCenter(cabinet).add(0,0.5,0));next();
            }
            case 5 -> {
                if(phaseTicks<25)return false;screenshot(client,"field-cabinet");
                var medic=clientEntity(client,medicId);require(medic!=null,"Naturally generated medic left client tracking");aim(client,medic.getPos().add(0,1.1,0));next();
            }
            case 6 -> {
                var liveMedic=clientEntity(client,medicId);if(liveMedic==null)return false;aim(client,liveMedic.getPos().add(0,1.1,0));
                if(phaseTicks<25)return false;screenshot(client,"natural-field-medic");
                var golem=clientEntity(client,golemId);require(golem!=null,"Naturally generated defender left client tracking");aim(client,golem.getPos().add(0,1.4,0));next();
            }
            case 7 -> {
                var liveGolem=clientEntity(client,golemId);if(liveGolem==null)return false;aim(client,liveGolem.getPos().add(0,1.4,0));
                if(phaseTicks<25)return false;screenshot(client,"natural-tetanized-guard");
                beginCommand(client,"locate poi tetanusmod:medic_tent","commands.locate.poi.success");next();
            }
            case 8 -> {
                var reply=awaitCommand(client);if(reply==null)return false;
                require(sameXZ(reply.position(),cabinet),"Native nearby tent POI did not identify the naturally generated field cabinet");
                screenshot(client,"locate-poi-chat");client.setScreen(null);
                server(client,player->{
                    var state=player.getServerWorld().getBlockState(chest);var facing=state.get(ChestBlock.FACING);
                    var stand=Vec3d.ofBottomCenter(chest.offset(facing,2));
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
        int lower=0,upper=0;
        for(var mutable:BlockPos.iterate(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX(),bounds.getMaxY(),bounds.getMaxZ())){
            var pos=mutable.toImmutable();var state=world.getBlockState(pos);require(state.getFluidState().isEmpty(),"Actual naturally generated tent is flooded at "+pos);
            if(state.isOf(ExpansionBlocks.FIELD_MEDIC_CABINET)){if(state.get(DoorBlock.HALF)==DoubleBlockHalf.LOWER){lower++;cabinet=pos;}else upper++;}
            if(state.isOf(Blocks.CHEST))chest=pos;
        }
        require(lower==1&&upper==1&&cabinet!=null&&chest!=null,"Natural camp is missing its two-block field cabinet or chest");
        require(world.getBlockEntity(chest)!=null&&world.getBlockEntity(chest).createNbt().getString("LootTable").equals("tetanusmod:chests/medic_tent"),"Natural camp chest has no unopened camp loot table");
        room=new Vec3d(bounds.getMinX()+6.5,bounds.getMinY()+1,bounds.getMinZ()+6.5);
        require(world.getBlockState(BlockPos.ofFloored(room)).isAir()&&world.getBlockState(BlockPos.ofFloored(room).up()).isAir(),"Natural tent room is obstructed");
        campEvidence.put("NativeStartChunk",Map.of("x",start.getPos().x,"z",start.getPos().z));campEvidence.put("BoundingBox",bounds.toString());campEvidence.put("ActualRoomPosition",position(room));campEvidence.put("ActualCabinet",block(cabinet));campEvidence.put("ActualLootChest",block(chest));campEvidence.put("UnopenedLootTable","tetanusmod:chests/medic_tent");campEvidence.put("FullNativeChunksGenerated",true);
        player.teleport(world,room.x,room.y,room.z,0,0);generationFinishedAt=world.getTime();
    }
    private void inspectResidents(ServerPlayerEntity player){
        var world=player.getServerWorld();require(world.getPointOfInterestStorage().hasTypeAt(MedicVillage.FIELD_JOB_KEY,cabinet),"Natural field cabinet is not indexed as its tent POI");
        require(world.getPointOfInterestStorage().getType(cabinet.up()).isEmpty(),"Upper cabinet registered a duplicate POI");
        var box=new Box(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX()+1,bounds.getMaxY()+1,bounds.getMaxZ()+1).expand(32);
        var medics=world.getEntitiesByClass(VillagerEntity.class,box,villager->villager.getVillagerData().getProfession()==MedicVillage.MEDIC&&villager.writeNbt(new NbtCompound()).getBoolean("RustRuinFieldMedic"));
        var golems=world.getEntitiesByClass(IronGolemEntity.class,box,golem->MobVariants.permanent(golem)&&golem.isPlayerCreated()&&golem.getMaxHealth()==200);
        require(medics.size()==1&&golems.size()==1,"Naturally generated camp did not load exactly one field medic and200HP friendly Tetanized guard: "+medics.size()+"/"+golems.size());
        medicId=medics.get(0).getUuid();golemId=golems.get(0).getUuid();campEvidence.put("NativeFieldMedicUuid",medicId.toString());campEvidence.put("NativeGuardUuid",golemId.toString());campEvidence.put("NativeGuardMaxHealth",golems.get(0).getMaxHealth());campEvidence.put("NativeGuardCurrentHealth",golems.get(0).getHealth());campEvidence.put("NativeGuardPlayerCreated",golems.get(0).isPlayerCreated());campEvidence.put("NativeTentPoiIndexed",true);
    }
    private static Feedback feedback(Text text){
        if(!(text.getContent() instanceof TranslatableTextContent content)||!Set.of("commands.locate.structure.success","commands.locate.poi.success").contains(content.getKey())||!text.getString().contains("tetanusmod:medic_tent"))return null;
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
