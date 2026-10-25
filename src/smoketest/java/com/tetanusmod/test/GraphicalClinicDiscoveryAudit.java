package com.tetanusmod.test;

import com.google.gson.*;
import com.tetanusmod.TetanusMod;
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
import net.minecraft.entity.effect.*;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.*;
import net.minecraft.text.*;
import net.minecraft.util.*;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/** Five real, naturally generated clinics reached through connected-client locate commands. */
public final class GraphicalClinicDiscoveryAudit {
    private static volatile GraphicalClinicDiscoveryAudit active;
    private static final Set<String> THEMES=Set.of("plains","desert","savanna","snowy","taiga");
    private record Candidate(String theme,Vec3d observer,ChunkPos start,BlockPos plannedOrigin){}
    private record Camera(Vec3d position,Vec3d target){}
    private record Locate(BlockPos start,BlockPos entrance,String nativeMessage,String entranceMessage){}
    private final List<Text> serverMessages=new CopyOnWriteArrayList<>();
    private final Set<ChatHudLine> oldChat=Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<Map<String,Object>> evidence=new ArrayList<>();
    private final String viewer=FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
    private List<Candidate> candidates;
    private UUID playerId,medicId;
    private CompletableFuture<?> pending;
    private Snapshot snapshot;
    private Perspective originalPerspective;
    private int originalFov,index,phase,phaseTicks,ticks,settled,messageBase,readinessPolls,portraitAttempts;
    private long seed,generationFinishedAt;
    private boolean started,complete,restoreQueued;
    private Path output,manifestPath;
    private String pendingCommand;
    private Locate aliasLocate,nativeLocate;
    private MedicStructurePiece piece;
    private BlockBox bounds;
    private BlockPos cabinet,chest,entrance;
    private Camera camera;
    private Map<String,Object> row;

    public static void observeServerFeedback(UUID player,Text text){
        var audit=active;if(audit!=null&&player.equals(audit.playerId))audit.serverMessages.add(text.copy());
    }
    public void start(MinecraftClient client){
        require(!started&&client.getServer()!=null&&client.player!=null,"Natural clinic audit requires one integrated connected player");
        started=true;playerId=client.player.getUuid();active=this;output=client.runDirectory.toPath();
        manifestPath=Path.of(System.getProperty("rustruin.clinicCandidates",output.resolve("clinic-native-candidates.json").toString()));
        readCandidates();originalPerspective=client.options.getPerspective();originalFov=client.options.getFov().getValue();
        release(client);client.setScreen(null);client.options.setPerspective(Perspective.FIRST_PERSON);client.options.getFov().setValue(70);
        pending=server(client,player->{
            var world=player.getServerWorld();require(world.getRegistryKey().equals(World.OVERWORLD)&&world.getChunkManager().getChunkGenerator() instanceof NoiseChunkGenerator,"Natural clinic proof requires the real normal Overworld");
            require(world.getSeed()==seed&&world.getServer().getSaveProperties().getGeneratorOptions().shouldGenerateStructures(),"Candidate seed differs from the actual structure-enabled Overworld");
            snapshot=new Snapshot(player);snapshot.prepare(player);return null;
        });
    }
    public boolean tick(MinecraftClient client){
        try{return tickInternal(client);}catch(RuntimeException|Error problem){restore(client);throw problem;}
    }
    @SuppressWarnings("unchecked")
    private boolean tickInternal(MinecraftClient client){
        require(started,"Natural clinic audit has not started");if(complete)return true;
        require(++ticks<12000,"Natural clinic audit timed out at theme="+theme()+" phase="+phase);phaseTicks++;
        switch(phase){
            case 0->{if(done()){pending.join();next(1);}}
            case 1->{
                row=new LinkedHashMap<>();row.put("Theme",theme());row.put("ObserverOrigin",position(observerOrigin()));row.put("ManifestSuggestedObserverOrigin",position(candidate().observer));row.put("ExpectedStartChunk",chunk(candidate().start));row.put("NativeCommands",new ArrayList<Map<String,Object>>());
                pending=server(client,player->{var p=observerOrigin();player.closeHandledScreen();player.teleport(player.getServerWorld(),p.x,p.y,p.z,0,0);player.setVelocity(Vec3d.ZERO);return null;});next(2);
            }
            case 2->{if(done()){pending.join();if(phaseTicks>=30&&client.player.squaredDistanceTo(observerOrigin())<0.04&&client.player.hasPermissionLevel(2)){beginCommand(client,"rustandruin locate medic");next(3);}}}
            case 3->{var reply=awaitCommand(client);if(reply!=null){aliasLocate=reply;recordCommand(reply);beginCommand(client,"locate structure rustandruin:medic_clinic");next(4);}}
            case 4->{
                var reply=awaitCommand(client);if(reply==null)return false;nativeLocate=reply;recordCommand(reply);
                require(aliasLocate.start.equals(reply.start)&&aliasLocate.entrance.equals(reply.entrance),"Native and convenience locate commands disagree");
                require(new ChunkPos(reply.start).equals(candidate().start),"Locate found a different clinic start than the native themed candidate: "+reply.start+" expected "+candidate().start);
                screenshot(client,"locate-chat");client.setScreen(null);pending=server(client,this::loadClinic);next(5);
            }
            case 5->{if(done()){camera=(Camera)pending.join();pending=null;settled=0;next(6);}}
            case 6->{
                if(phaseTicks>=1200)throw new IllegalStateException("Native clinic residents never became ready: "+row.get("NativeReadiness"));
                if(!atCamera(client)||client.world.getTime()-generationFinishedAt<40)return false;
                if(pending!=null&&!done())return false;
                if(pending!=null){var ready=(Boolean)pending.join();pending=null;if(ready){next(7);return false;}}
                if(phaseTicks%10==0)pending=server(client,this::residentsReady);
            }
            case 7->{
                if(!atCamera(client)||clientMedic(client)==null){settled=0;return false;}
                if(++settled<30)return false;row.put("ClientMedicUuid",medicId.toString());screenshot(client,"exterior");
                pending=server(client,p->move(p,local(4.5,2,4.5),Vec3d.ofCenter(cabinet).add(0,0.5,0)));next(8);
            }
            case 8->{if(done()){camera=(Camera)pending.join();settled=0;next(9);}}
            case 9->{
                if(!atCamera(client)||!clientCabinet(client)){settled=0;return false;}if(++settled<30)return false;screenshot(client,"cabinet-interior");
                pending=server(client,this::medicCamera);next(10);
            }
            case 10->{if(done()){camera=(Camera)pending.join();settled=0;next(11);}}
            case 11->{
                var medic=clientMedic(client);if(!atCamera(client)||medic==null){settled=0;if(phaseTicks>=40)retryMedicCamera(client,"Observer position or native medic tracking changed");return false;}aim(client,medic.getBoundingBox().getCenter());
                if(!(client.crosshairTarget instanceof EntityHitResult hit)||!hit.getEntity().getUuid().equals(medicId)){settled=0;if(phaseTicks>=40)retryMedicCamera(client,"Native client crosshair no longer reaches the moving medic");return false;}
                if(++settled<30)return false;row.put("ClientMedicCrosshairUuid",medicId.toString());screenshot(client,"medic");
                pending=server(client,p->{var state=p.getServerWorld().getBlockState(chest);var stand=Vec3d.ofBottomCenter(chest.offset(state.get(ChestBlock.FACING),1));require(p.getServerWorld().isAir(BlockPos.ofFloored(stand))&&p.getServerWorld().isAir(BlockPos.ofFloored(stand).up()),"Native chest approach is obstructed");return move(p,stand,Vec3d.ofCenter(chest).add(0,-0.25,0));});next(12);
            }
            case 12->{if(done()){camera=(Camera)pending.join();settled=0;next(13);}}
            case 13->{
                if(!atCamera(client)){settled=0;return false;}if(++settled<30)return false;
                if(!(client.crosshairTarget instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK||!hit.getBlockPos().equals(chest)){require(phaseTicks<250,"Native client ray did not reach clinic chest: "+client.crosshairTarget);return false;}
                client.options.useKey.setPressed(true);next(14);
            }
            case 14->{
                if(phaseTicks>=2)client.options.useKey.setPressed(false);
                if(!(client.currentScreen instanceof GenericContainerScreen menu)){require(phaseTicks<250,"Native client did not open clinic loot menu");return false;}
                if(phaseTicks<20)return false;int stacks=0;for(int i=0;i<27;i++)if(!menu.getScreenHandler().getSlot(i).getStack().isEmpty())stacks++;
                require(stacks>0,"Native clinic loot menu is empty");row.put("ClientGeneratedLootStacks",stacks);screenshot(client,"loot-menu");client.player.closeHandledScreen();
                pending=server(client,p->{var inventory=p.getServerWorld().getBlockEntity(chest);require(inventory instanceof ChestBlockEntity,"Native clinic chest disappeared");int count=0;var box=(ChestBlockEntity)inventory;for(int i=0;i<box.size();i++)if(!box.getStack(i).isEmpty())count++;require(count>0,"Server native loot generation failed");row.put("ServerGeneratedLootStacks",count);return null;});next(15);
            }
            case 15->{if(done()){pending.join();evidence.add(new LinkedHashMap<>(row));org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_NATURAL_CLINIC_PASS seed={} theme={} start={} entrance={} medic={} native-loot=true",seed,theme(),candidate().start,entrance,medicId);if(++index<candidates.size())next(1);else{restore(client);next(16);}}}
            case 16->{if(done()){pending.join();if(snapshot.clientRestored(client)){writeReport();complete=true;return true;}}require(phaseTicks<300,"Original clinic observer state did not synchronize after restoration");}
            default->throw new IllegalStateException("Unknown natural clinic phase "+phase);
        }
        return false;
    }
    private void readCandidates(){
        try{
            var root=JsonParser.parseString(Files.readString(manifestPath)).getAsJsonObject();seed=root.get("Seed").getAsLong();var result=new ArrayList<Candidate>();var seen=new HashSet<String>();
            for(var value:root.getAsJsonArray("Themes")){var entry=value.getAsJsonObject();String name=entry.get("Theme").getAsString();require(THEMES.contains(name)&&seen.add(name),"Unknown or duplicate native clinic theme "+name);var p=entry.getAsJsonObject("ExpectedStartChunk");result.add(new Candidate(name,vec(entry.getAsJsonObject("ObserverOrigin")),new ChunkPos(p.get("x").getAsInt(),p.get("z").getAsInt()),block(entry.getAsJsonObject("PieceOrigin"))));}
            require(seen.equals(THEMES),"Native candidate manifest does not cover all five village themes");candidates=List.copyOf(result);
        }catch(Exception problem){throw new IllegalStateException("Cannot read native normal-world clinic candidates "+manifestPath,problem);}
    }
    private Camera loadClinic(ServerPlayerEntity player){
        var world=player.getServerWorld();var type=world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(TetanusMod.id("medic_clinic"));
        var start=world.getChunk(candidate().start.x,candidate().start.z,ChunkStatus.STRUCTURE_STARTS,true).getStructureStart(type);
        require(start!=null&&start.hasChildren()&&start.getChildren().size()==1&&start.getChildren().get(0) instanceof MedicStructurePiece,"Actual native locate identifies no saved clinic");piece=(MedicStructurePiece)start.getChildren().get(0);bounds=piece.getBoundingBox();
        String template=piece.toNbt(StructureContext.from(world)).getString("Template");require(template.equals("tetanusmod:village/"+theme()+"_clinic")&&piece.getPos().equals(candidate().plannedOrigin),"Actual clinic theme/origin differs from native candidate manifest: "+template+" "+piece.getPos());
        require(bounds.getBlockCountX()==9&&bounds.getBlockCountY()==11&&bounds.getBlockCountZ()==13,"Native clinic footprint is unexpected");
        for(int x=bounds.getMinX()>>4;x<=bounds.getMaxX()>>4;x++)for(int z=bounds.getMinZ()>>4;z<=bounds.getMaxZ()>>4;z++){require(Math.abs(x-start.getPos().x)<=8&&Math.abs(z-start.getPos().z)<=8,"Clinic escapes native structure-reference range");world.getChunk(x,z);}
        int foundation=0;var placement=piece.getPlacementData().copy().setBoundingBox(null);
        for(var material:List.of(Blocks.STONE_BRICKS,Blocks.CUT_SANDSTONE,Blocks.POLISHED_ANDESITE))for(var info:piece.getTemplate().getInfosForBlock(piece.getPos(),placement,material))if(info.pos().getY()==piece.getPos().getY()){foundation++;require(world.getBlockState(info.pos()).equals(info.state()),"Native clinic foundation is missing: "+info.pos());}
        require(foundation==77,"Native clinic lacks its77 authored foundation blocks");
        cabinet=localBlock(6,2,6);chest=localBlock(6,2,10);entrance=piece.clinicEntrance();
        require(entrance.equals(aliasLocate.entrance)&&entrance.equals(nativeLocate.entrance),"Actual building entrance differs from both command hints");
        require(world.getBlockState(entrance).getCollisionShape(world,entrance).isEmpty()&&world.getBlockState(entrance.up()).getCollisionShape(world,entrance.up()).isEmpty()&&!world.getBlockState(entrance.down()).getCollisionShape(world,entrance.down()).isEmpty(),"Exact clinic entrance is obstructed or unsupported");
        var lower=world.getBlockState(cabinet);var upper=world.getBlockState(cabinet.up());require(lower.isOf(ExpansionBlocks.MEDIC_CABINET)&&upper.isOf(ExpansionBlocks.MEDIC_CABINET)&&lower.get(DoorBlock.HALF)==DoubleBlockHalf.LOWER&&upper.get(DoorBlock.HALF)==DoubleBlockHalf.UPPER&&lower.get(DoorBlock.FACING)==Direction.WEST&&lower.get(MedicineCabinetBlock.COMPLETE),"Natural clinic cabinet missing or facing away from aisle");
        row.put("NativePoiIndexedAtGeneration",world.getPointOfInterestStorage().hasTypeAt(MedicVillage.JOB_KEY,cabinet));
        require(world.getBlockEntity(chest) instanceof ChestBlockEntity&&world.getBlockEntity(chest).createNbt().getString("LootTable").equals("tetanusmod:chests/clinic"),"Natural clinic lacks an unopened native loot chest");
        row.put("SavedTemplate",template);row.put("ActualStartChunk",chunk(start.getPos()));row.put("PieceOrigin",point(piece.getPos()));row.put("Bounds",bounds.toString());row.put("ExactNativeEntrance",point(entrance));row.put("NativeFoundationBlocks",foundation);row.put("NativeCabinet",point(cabinet));row.put("NativeChest",point(chest));row.put("UnopenedLootTable","tetanusmod:chests/clinic");
        generationFinishedAt=world.getTime();readinessPolls=0;portraitAttempts=0;medicId=null;return exterior(player);
    }
    private boolean residentsReady(ServerPlayerEntity player){
        var world=player.getServerWorld();var pendingChunks=new ArrayList<Map<String,Integer>>();
        for(int x=bounds.getMinX()>>4;x<=bounds.getMaxX()>>4;x++)for(int z=bounds.getMinZ()>>4;z<=bounds.getMaxZ()>>4;z++)if(!world.getChunkManager().isTickingFutureReady(ChunkPos.toLong(x,z)))pendingChunks.add(Map.of("x",x,"z",z));
        var area=new Box(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX()+1,bounds.getMaxY()+1,bounds.getMaxZ()+1).expand(24);
        String expected=theme().equals("snowy")?"snow":theme();
        var medics=world.getEntitiesByClass(VillagerEntity.class,area,v->v.getVillagerData().getProfession()==MedicVillage.MEDIC&&v.getExperience()>0&&Registries.VILLAGER_TYPE.getId(v.getVillagerData().getType()).equals(new Identifier("minecraft",expected)));
        boolean indexed=world.getPointOfInterestStorage().hasTypeAt(MedicVillage.JOB_KEY,cabinet);
        var observation=Map.<String,Object>of("Poll",++readinessPolls,"NativeMedics",medics.size(),"PendingTickingChunks",pendingChunks,"NativeWorldTicksSinceGeneration",world.getTime()-generationFinishedAt,"NativePoiIndexed",indexed);row.put("NativeReadiness",observation);
        if(!pendingChunks.isEmpty()||medics.isEmpty()||!indexed)return false;
        require(world.getPointOfInterestStorage().getType(cabinet.up()).isEmpty(),"Natural clinic upper cabinet creates a duplicate workstation POI");row.put("NativePoiIndexed",true);
        require(medics.size()==1,"Natural clinic has multiple unexpected medics: "+medics);var medic=medics.get(0);
        require(Registries.VILLAGER_TYPE.getId(medic.getVillagerData().getType()).equals(new Identifier("minecraft",expected))&&medic.getExperience()>0,"Native village medic lacks themed type or stable profession");
        require(!medic.writeNbt(new net.minecraft.nbt.NbtCompound()).getBoolean("RustRuinFieldMedic"),"Ordinary clinic medic incorrectly uses cave-field scarcity");medicId=medic.getUuid();row.put("NativeMedicUuid",medicId.toString());row.put("NativeMedicVillagerType",expected);row.put("NativeMedicExperience",medic.getExperience());return true;
    }
    private void retryMedicCamera(MinecraftClient client,String reason){
        row.put("LastMedicObserverRetry",reason);pending=server(client,this::medicCamera);next(10);
    }
    /** Find a real collision-free observer position without changing the villager or its AI. */
    @SuppressWarnings("unchecked")
    private Camera medicCamera(ServerPlayerEntity player){
        var world=player.getServerWorld();var entity=world.getEntity(medicId);require(entity instanceof VillagerEntity,"Native medic disappeared before its portrait");var medic=(VillagerEntity)entity;
        require(++portraitAttempts<=8,"Native medic observer could not maintain a clear view after eight bounded attempts");var target=medic.getBoundingBox().getCenter();
        for(double height:new double[]{0,0.0625,0.5,1})for(double radius:new double[]{3,2.5,3.5})for(int direction=0;direction<8;direction++){
            double angle=Math.toRadians(medic.bodyYaw+(direction+portraitAttempts-1)*45);var position=medic.getPos().add(-Math.sin(angle)*radius,height,Math.cos(angle)*radius);var eyes=position.add(0,player.getEyeHeight(player.getPose()),0);
            double distance=eyes.distanceTo(target);if(distance<2||distance>4)continue;
            var observerBox=player.getBoundingBox().offset(position.subtract(player.getPos()));if(!world.isSpaceEmpty(player,observerBox))continue;
            var ray=world.raycast(new RaycastContext(eyes,target,RaycastContext.ShapeType.OUTLINE,RaycastContext.FluidHandling.NONE,player));if(ray.getType()!=HitResult.Type.MISS)continue;
            var visits=(List<Map<String,Object>>)row.computeIfAbsent("NativeMedicObserverViews",key->new ArrayList<Map<String,Object>>());visits.add(Map.of("Attempt",portraitAttempts,"NativeMedicPosition",position(medic.getPos()),"NativeMedicPose",medic.getPose().name(),"ObserverPosition",position(position),"NativeTarget",position(target),"EyeToTargetDistance",distance,"CollisionFree",true,"NativeBlockRayClear",true));
            return move(player,position,target);
        }
        throw new IllegalStateException("No collision-free native2-4block observer view reaches the medic at "+medic.getPos()+" pose="+medic.getPose());
    }
    private Camera exterior(ServerPlayerEntity player){
        var world=player.getServerWorld();var target=local(4.5,4.5,6.5);
        for(double y:new double[]{5,7,9})for(double x:new double[]{4.5,-5.5,13.5}){
            var position=local(x,y,-10.5);var feet=BlockPos.ofFloored(position);if(!world.isAir(feet)||!world.isAir(feet.up()))continue;
            var hit=world.raycast(new RaycastContext(position.add(0,1.62,0),target,RaycastContext.ShapeType.VISUAL,RaycastContext.FluidHandling.NONE,player));
            if(hit.getType()==HitResult.Type.BLOCK&&bounds.contains(hit.getBlockPos()))return move(player,position,target);
        }
        throw new IllegalStateException("No unmodified air viewpoint sees the native clinic exterior");
    }
    private Camera move(ServerPlayerEntity player,Vec3d position,Vec3d target){
        player.teleport(player.getServerWorld(),position.x,position.y,position.z,0,0);player.setVelocity(Vec3d.ZERO);return new Camera(position,target);
    }
    private Vec3d local(double x,double y,double z){return StructureTemplate.transformAround(new Vec3d(x,y,z),BlockMirror.NONE,piece.getRotation(),BlockPos.ORIGIN).add(Vec3d.of(piece.getPos()));}
    private BlockPos localBlock(int x,int y,int z){return StructureTemplate.transform(piece.getPlacementData(),new BlockPos(x,y,z)).add(piece.getPos());}
    private boolean atCamera(MinecraftClient client){if(camera==null||client.player==null)return false;aim(client,camera.target);return client.player.getAbilities().creativeMode&&client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)&&client.player.squaredDistanceTo(camera.position)<0.04;}
    private VillagerEntity clientMedic(MinecraftClient client){if(medicId==null)return null;for(var entity:client.world.getEntities())if(entity.getUuid().equals(medicId)&&entity instanceof VillagerEntity villager&&villager.getVillagerData().getProfession()==MedicVillage.MEDIC)return villager;return null;}
    private boolean clientCabinet(MinecraftClient client){var a=client.world.getBlockState(cabinet);var b=client.world.getBlockState(cabinet.up());return a.isOf(ExpansionBlocks.MEDIC_CABINET)&&b.isOf(ExpansionBlocks.MEDIC_CABINET)&&a.get(DoorBlock.FACING)==Direction.WEST&&a.get(MedicineCabinetBlock.COMPLETE);}
    private void beginCommand(MinecraftClient client,String command){pendingCommand=command;messageBase=serverMessages.size();oldChat.clear();oldChat.addAll(chat(client));client.setScreen(new ChatScreen(""));client.player.networkHandler.sendChatCommand(command);}
    private Locate awaitCommand(MinecraftClient client){
        var observed=List.copyOf(serverMessages);Locate server=findLocate(observed.subList(messageBase,observed.size()));Locate actualClient=findLocate(chat(client).stream().filter(line->!oldChat.contains(line)).map(ChatHudLine::content).toList());
        require(phaseTicks<1200,"Native clinic command feedback timed out: /"+pendingCommand+" messages="+serverMessages.stream().map(Text::getString).toList());
        if(server==null||actualClient==null||phaseTicks<30)return null;require(server.start.equals(actualClient.start)&&server.entrance.equals(actualClient.entrance),"Server/client clinic command coordinates differ");return server;
    }
    private static Locate findLocate(List<Text> texts){
        Text result=null,hint=null;for(var text:texts)if(text.getContent() instanceof TranslatableTextContent content){if(content.getKey().equals("commands.locate.structure.success")&&text.getString().contains("rustandruin:medic_clinic"))result=text;if(content.getKey().equals("commands.rustandruin.clinic_entrance"))hint=text;if(content.getKey().equals("commands.locate.structure.not_found"))throw new IllegalStateException("Native clinic locate returned not found: "+text.getString());}
        if(result==null||hint==null)return null;var start=coordinates(result);var entrance=coordinates(hint);require(start!=null&&entrance!=null,"Native clinic result lost coordinate components");return new Locate(new BlockPos(start.getX(),0,start.getZ()),entrance,result.getString(),hint.getString());
    }
    private static BlockPos coordinates(Text text){if(text.getContent() instanceof TranslatableTextContent content){var args=content.getArgs();if(content.getKey().equals("chat.coordinates")&&args.length>=3)return new BlockPos(number(args[0]),number(args[1]),number(args[2]));for(var arg:args)if(arg instanceof Text child){var found=coordinates(child);if(found!=null)return found;}}for(var child:text.getSiblings()){var found=coordinates(child);if(found!=null)return found;}return null;}
    private static int number(Object value){return value instanceof Number n?n.intValue():value.toString().equals("~")?0:Integer.parseInt(value.toString());}
    @SuppressWarnings("unchecked") private void recordCommand(Locate reply){((List<Map<String,Object>>)row.get("NativeCommands")).add(Map.of("Command","/"+pendingCommand,"ActualServerAndClientCoordinatesAgreed",true,"NativeMessage",reply.nativeMessage,"EntranceMessage",reply.entranceMessage,"StartBlockCoordinates",point(reply.start),"ExactEntrance",point(reply.entrance)));}
    @SuppressWarnings("unchecked") private static List<ChatHudLine> chat(MinecraftClient client){try{var field=client.inGameHud.getChatHud().getClass().getDeclaredField("messages");field.setAccessible(true);return List.copyOf((List<ChatHudLine>)field.get(client.inGameHud.getChatHud()));}catch(Exception problem){throw new IllegalStateException("Cannot inspect actual native clinic command chat",problem);}}
    private <T> CompletableFuture<T> server(MinecraftClient client,Function<ServerPlayerEntity,T> action){var future=new CompletableFuture<T>();client.getServer().execute(()->{try{var player=client.getServer().getPlayerManager().getPlayer(playerId);require(player!=null,"Native clinic observer disappeared");future.complete(action.apply(player));}catch(Throwable problem){future.completeExceptionally(problem);}});return future;}
    private boolean done(){return pending!=null&&pending.isDone();}
    private Candidate candidate(){return candidates.get(index);}
    private Vec3d observerOrigin(){return new Vec3d(candidate().start.getStartX()+8,candidate().observer.y,candidate().start.getStartZ()+8);}
    private String theme(){return candidates==null||index>=candidates.size()?"finished":candidate().theme;}
    private void next(int value){phase=value;phaseTicks=0;settled=0;}
    private String file(String label){return "rust-and-ruin-"+viewer+"-clinic-natural-"+theme()+"-"+label+".png";}
    private void screenshot(MinecraftClient client,String label){try(var frame=ScreenshotRecorder.takeScreenshot(client.getFramebuffer())){var folder=output.resolve("screenshots");Files.createDirectories(folder);frame.writeTo(folder.resolve(file(label)));row.put(label+"PNG",file(label));}catch(Exception problem){throw new IllegalStateException("Cannot capture real natural clinic frame",problem);}}
    private void restore(MinecraftClient client){release(client);client.setScreen(null);client.options.setPerspective(originalPerspective);client.options.getFov().setValue(originalFov);active=null;if(restoreQueued)return;restoreQueued=true;if(snapshot!=null&&client.getServer()!=null)pending=server(client,p->{snapshot.restore(p);return null;});}
    private void writeReport(){
        require(evidence.size()==5,"Natural clinic report lacks one of the five village themes");int commandCount=evidence.stream().mapToInt(value->((List<?>)value.get("NativeCommands")).size()).sum();require(commandCount==10,"Natural clinic proof did not execute both native commands for every theme");var report=new LinkedHashMap<String,Object>();report.put("Result","PASS");report.put("ActualGraphicalClient",true);report.put("Seed",seed);report.put("CandidateManifest",manifestPath.getFileName().toString());report.put("NativeClientNetworkCommands",commandCount);report.put("ManuallyPlacedTemplates",0);report.put("ManuallyInsertedStarts",0);report.put("ManuallyPlacedPoi",0);report.put("ManuallySpawnedResidents",0);report.put("Setup","Creative flight, Night Vision and observer teleports to native pre-scanned candidate neighborhoods; no terrain, resident AI or loot edits");report.put("OriginalPlayerStateRestored",true);report.put("Themes",evidence);
        try{Files.writeString(output.resolve("graphical-clinic-natural-"+viewer+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}catch(Exception problem){throw new IllegalStateException("Cannot preserve natural clinic report",problem);}
    }
    private static void release(MinecraftClient client){client.options.useKey.setPressed(false);client.options.forwardKey.setPressed(false);client.options.backKey.setPressed(false);client.options.leftKey.setPressed(false);client.options.rightKey.setPressed(false);client.options.jumpKey.setPressed(false);client.options.sneakKey.setPressed(false);}
    private static void aim(MinecraftClient client,Vec3d target){var d=target.subtract(client.player.getEyePos());client.player.setYaw((float)Math.toDegrees(Math.atan2(-d.x,d.z)));client.player.setPitch((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));}
    private static Vec3d vec(JsonObject json){return new Vec3d(json.get("x").getAsDouble(),json.get("y").getAsDouble(),json.get("z").getAsDouble());}
    private static BlockPos block(JsonObject json){return BlockPos.ofFloored(vec(json));}
    private static Map<String,Object> position(Vec3d p){return Map.of("x",p.x,"y",p.y,"z",p.z);}
    private static Map<String,Integer> point(BlockPos p){return Map.of("x",p.getX(),"y",p.getY(),"z",p.getZ());}
    private static Map<String,Integer> chunk(ChunkPos p){return Map.of("x",p.x,"z",p.z);}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private static final class Snapshot {
        final ServerWorld world;final Vec3d position,velocity;final float yaw,pitch,fall;final GameMode mode;final boolean gravity,flying,invulnerable,operator;final StatusEffectInstance vision;
        Snapshot(ServerPlayerEntity p){world=p.getServerWorld();position=p.getPos();velocity=p.getVelocity();yaw=p.getYaw();pitch=p.getPitch();fall=p.fallDistance;mode=p.interactionManager.getGameMode();gravity=p.hasNoGravity();flying=p.getAbilities().flying;invulnerable=p.getAbilities().invulnerable;operator=p.getServer().getPlayerManager().isOperator(p.getGameProfile());var effect=p.getStatusEffect(StatusEffects.NIGHT_VISION);vision=effect==null?null:new StatusEffectInstance(effect);}
        void prepare(ServerPlayerEntity p){if(!operator)p.getServer().getPlayerManager().addToOperators(p.getGameProfile());p.changeGameMode(GameMode.CREATIVE);p.setNoGravity(true);p.getAbilities().flying=true;p.getAbilities().invulnerable=true;p.sendAbilitiesUpdate();p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,20000,0,false,false,false));p.setVelocity(Vec3d.ZERO);p.fallDistance=0;}
        void restore(ServerPlayerEntity p){p.closeHandledScreen();p.changeGameMode(mode);p.setNoGravity(gravity);p.getAbilities().flying=flying;p.getAbilities().invulnerable=invulnerable;p.sendAbilitiesUpdate();p.removeStatusEffect(StatusEffects.NIGHT_VISION);if(vision!=null)p.addStatusEffect(vision);p.teleport(world,position.x,position.y,position.z,yaw,pitch);p.setVelocity(velocity);p.fallDistance=fall;if(!operator)p.getServer().getPlayerManager().removeFromOperators(p.getGameProfile());}
        boolean clientRestored(MinecraftClient c){return c.player!=null&&c.world.getRegistryKey().equals(world.getRegistryKey())&&c.player.squaredDistanceTo(position)<0.16&&c.player.getAbilities().creativeMode==(mode==GameMode.CREATIVE)&&c.player.isSpectator()==(mode==GameMode.SPECTATOR);}
    }
}
