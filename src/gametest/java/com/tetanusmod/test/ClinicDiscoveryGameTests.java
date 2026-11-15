package com.tetanusmod.test;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.expansion.ExpansionBlocks;
import com.tetanusmod.expansion.MedicVillage;
import com.tetanusmod.medical.MedicalWorkstations;
import com.tetanusmod.world.ClinicStructure;
import com.tetanusmod.world.MedicStructurePiece;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.*;
import net.minecraft.test.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.chunk.placement.*;
import net.minecraft.world.gen.noise.NoiseConfig;
import java.nio.file.*;
import java.util.*;

/** Native grid selection, saved starts, FULL chunks and residents, never manual template placement. */
public class ClinicDiscoveryGameTests implements FabricGameTest {
    private static final List<String> THEMES=List.of("plains","desert","savanna","snowy","taiga");
    private record Site(String theme,ChunkPos chunk,MedicStructurePiece planned){}

    @GameTest(templateName=EMPTY_STRUCTURE,batchId="clinic_discovery")
    public void entireClinicFootprintMustRemainInsideNativeReferenceWindow(TestContext ctx)throws Exception{
        var guard=ClinicStructure.class.getDeclaredMethod("withinReferenceWindow",ChunkPos.class,int.class,int.class);guard.setAccessible(true);
        for(var start:List.of(new ChunkPos(0,0),new ChunkPos(-452,-126),new ChunkPos(73,129))){
            int minX=(start.x-8)*16,minZ=(start.z-8)*16,maxX=(start.x+9)*16-1,maxZ=(start.z+9)*16-1;
            ctx.assertTrue((Boolean)guard.invoke(null,start,minX,minZ),"Legal negative reference edge rejected");
            ctx.assertTrue((Boolean)guard.invoke(null,start,maxX-8,maxZ-12),"Legal positive reference edge rejected");
            for(int[] p:new int[][]{{minX-1,minZ},{minX,minZ-1},{maxX-7,minZ},{minX,maxZ-11}})
                ctx.assertTrue(!(Boolean)guard.invoke(null,start,p[0],p[1]),"Clinic can escape the native eight-chunk reference window at "+Arrays.toString(p));
        }
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE,batchId="clinic_discovery",tickLimit=3600)
    public void allFiveThemesActuallyGenerateFromNativeVillageCandidates(TestContext ctx){
        var world=ctx.getWorld().getServer().getWorld(TentNoiseTestWorldBootstrap.KEY);
        ctx.assertTrue(world!=null&&world.getChunkManager().getChunkGenerator() instanceof NoiseChunkGenerator,"Clinic coverage needs the genuinely generated normal Overworld fixture");
        var sites=findThemes(ctx,world,world.getSeed(),world.getChunkManager().getChunkGenerator(),world.getChunkManager().getNoiseConfig(),world.getChunkManager().getStructurePlacementCalculator());
        writeManifest(world.getSeed(),sites,"clinic-native-candidates.json");
        var tickets=new ArrayList<ChunkPos>();var observers=new ArrayList<Observer>();var pieces=new ArrayList<MedicStructurePiece>();
        final boolean[] finished={false};
        try{
            var type=world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(TetanusMod.id("medic_clinic"));
            for(var site:sites){
                var start=world.getChunk(site.chunk.x,site.chunk.z,ChunkStatus.STRUCTURE_STARTS,true).getStructureStart(type);
                ctx.assertTrue(start!=null&&start.hasChildren()&&start.getChildren().size()==1&&start.getChildren().get(0) instanceof MedicStructurePiece,"A native-frequency eligible clinic candidate has no saved start: "+site);
                var parentType=world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(new Identifier("minecraft","village_"+site.theme));
                var parent=world.getChunk(site.chunk.x,site.chunk.z,ChunkStatus.STRUCTURE_STARTS,true).getStructureStart(parentType);
                ctx.assertTrue(parent!=null&&parent.hasChildren(),"Clinic predicted a "+site.theme+" village that native generation did not actually select: "+site.chunk);
                world.getChunk(site.chunk.x,site.chunk.z);
                var piece=(MedicStructurePiece)start.getChildren().get(0);
                ctx.assertTrue(theme(world,piece).equals(site.theme)&&piece.getPos().equals(site.planned.getPos()),"Native saved clinic differs from its deterministic actual-biome plan: "+site);
                var bounds=piece.getBoundingBox();assertReferenceWindow(ctx,start.getPos(),bounds);
                for(int x=bounds.getMinX()>>4;x<=bounds.getMaxX()>>4;x++)for(int z=bounds.getMinZ()>>4;z<=bounds.getMaxZ()>>4;z++){
                    var chunk=new ChunkPos(x,z);if(!world.getForcedChunks().contains(chunk.toLong())){world.setChunkForced(x,z,true);tickets.add(chunk);}world.getChunk(x,z);
                }
                assertBuilding(ctx,world,site.theme,piece,false);pieces.add(piece);observers.add(new Observer(world,piece));
                org.slf4j.LoggerFactory.getLogger("RustRuinClinicDiscovery").info("NATIVE_CLINIC_FULL seed={} theme={} start={} origin={} box={}",world.getSeed(),site.theme,site.chunk,piece.getPos(),bounds);
            }
            // Prequeue every callback before execution. Inserting tasks during the native
            // task-map iteration can invalidate its fastutil iterator.
            for(int tick=40;tick<=3200;tick+=10){final int deadline=tick;ctx.runAtTick(tick,()->{
                if(finished[0])return;
                try{
                    for(var observer:observers)observer.acknowledge();
                    boolean ready=true;for(int i=0;i<sites.size();i++)if(!tickingReady(world,pieces.get(i))||!residentVisible(world,sites.get(i).theme,pieces.get(i)))ready=false;
                    if(!ready){if(deadline==3200)throw new IllegalStateException("Native clinic residents never entered tracked chunks: "+residentSummary(world,pieces));return;}
                    for(int i=0;i<sites.size();i++){assertBuilding(ctx,world,sites.get(i).theme,pieces.get(i));assertResident(ctx,world,sites.get(i).theme,pieces.get(i));}
                    finished[0]=true;cleanup(world,observers,tickets);ctx.complete();
                }catch(RuntimeException|Error failure){finished[0]=true;cleanup(world,observers,tickets);throw failure;}
            });}
        }catch(RuntimeException|Error failure){finished[0]=true;cleanup(world,observers,tickets);throw failure;}
    }

    @GameTest(templateName=EMPTY_STRUCTURE,batchId="clinic_planning",tickLimit=3600)
    public void allFiveActualBiomePlansAreReachableAcrossTwoNativeSeeds(TestContext ctx){
        var world=ctx.getWorld();var registries=world.getRegistryManager();
        for(long seed:new long[]{0L,246813579L}){
            var generator=ChunkGenerator.CODEC.parse(RegistryOps.of(JsonOps.INSTANCE,registries),JsonParser.parseString("{\"type\":\"minecraft:noise\",\"settings\":\"minecraft:overworld\",\"biome_source\":{\"type\":\"minecraft:multi_noise\",\"preset\":\"minecraft:overworld\"}}")).result().orElseThrow();
            var settings=registries.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS).get(ChunkGeneratorSettings.OVERWORLD);
            var noise=NoiseConfig.create(settings,registries.getWrapperOrThrow(RegistryKeys.NOISE_PARAMETERS),seed);
            var calculator=StructurePlacementCalculator.create(noise,seed,generator.getBiomeSource(),registries.getWrapperOrThrow(RegistryKeys.STRUCTURE_SET));
            var sites=findThemes(ctx,world,seed,generator,noise,calculator);writeManifest(seed,sites,"clinic-planned-candidates-"+seed+".json");
            for(var site:sites)assertReferenceWindow(ctx,site.chunk,site.planned.getBoundingBox());
        }
        ctx.complete();
    }

    private static List<Site> findThemes(TestContext ctx,ServerWorld world,long seed,ChunkGenerator generator,NoiseConfig noise,StructurePlacementCalculator calculator){
        var registries=world.getRegistryManager();var structures=registries.get(RegistryKeys.STRUCTURE);
        var clinic=structures.get(TetanusMod.id("medic_clinic"));
        var grid=(RandomSpreadStructurePlacement)registries.get(RegistryKeys.STRUCTURE_SET).get(TetanusMod.id("medic_clinics")).placement();
        var found=new LinkedHashMap<String,Site>();int checked=0;
        for(int ring=0;ring<=32&&found.size()<THEMES.size();ring++)for(int rx=-ring;rx<=ring;rx++)for(int rz=-ring;rz<=ring;rz++){
            if(Math.max(Math.abs(rx),Math.abs(rz))!=ring||found.size()==THEMES.size())continue;
            var chunk=grid.getStartChunk(seed,rx*grid.getSpacing(),rz*grid.getSpacing());
            if(!grid.shouldGenerate(calculator,chunk.x,chunk.z))continue;
            // Cheap actual-biome prefilter only selects a test location; production still
            // performs the native village jigsaw/biome validation for every accepted plan.
            var biome=generator.getBiomeSource().getBiome((chunk.getStartX()+8)>>2,70>>2,(chunk.getStartZ()+8)>>2,noise.getMultiNoiseSampler());
            boolean wanted=false;for(String theme:THEMES)if(!found.containsKey(theme)&&structures.get(new Identifier("minecraft","village_"+theme)).getValidBiomes().contains(biome)){wanted=true;break;}
            if(!wanted)continue;checked++;
            var start=clinic.createStructureStart(registries,generator,generator.getBiomeSource(),noise,world.getStructureTemplateManager(),seed,chunk,0,world,clinic.getValidBiomes()::contains);
            if(!start.hasChildren())continue;
            ctx.assertTrue(start.getChildren().size()==1&&start.getChildren().get(0) instanceof MedicStructurePiece,"Clinic produced unexpected native pieces");
            var piece=(MedicStructurePiece)start.getChildren().get(0);String actual=theme(world,piece);
            ctx.assertTrue(THEMES.contains(actual),"Native clinic uses an unknown theme: "+actual);
            if(found.putIfAbsent(actual,new Site(actual,chunk,piece))==null)org.slf4j.LoggerFactory.getLogger("RustRuinClinicDiscovery").info("NATIVE_CLINIC_PLAN seed={} theme={} regionRing={} checkedPlans={} start={} origin={}",seed,actual,ring,checked,chunk,piece.getPos());
        }
        ctx.assertTrue(found.size()==THEMES.size(),"Native normal-Overworld seed "+seed+" produced only "+found.keySet()+" in 4,225 eligible-grid regions / "+checked+" actual-biome plans");
        var ordered=new ArrayList<Site>();for(String theme:THEMES)ordered.add(found.get(theme));return ordered;
    }
    private static String theme(ServerWorld world,MedicStructurePiece piece){
        var id=piece.toNbt(StructureContext.from(world)).getString("Template");
        return id.substring(id.lastIndexOf('/')+1).replace("_clinic","");
    }
    private static void assertReferenceWindow(TestContext ctx,ChunkPos start,BlockBox bounds){
        ctx.assertTrue((bounds.getMinX()>>4)>=start.x-8&&(bounds.getMaxX()>>4)<=start.x+8&&(bounds.getMinZ()>>4)>=start.z-8&&(bounds.getMaxZ()>>4)<=start.z+8,"Native clinic footprint escaped the structure-reference radius: "+start+" / "+bounds);
    }
    private static void assertBuilding(TestContext ctx,ServerWorld world,String theme,MedicStructurePiece piece){assertBuilding(ctx,world,theme,piece,true);}
    private static void assertBuilding(TestContext ctx,ServerWorld world,String theme,MedicStructurePiece piece,boolean poiReady){
        var pos=piece.getPos();var bounds=piece.getBoundingBox();
        ctx.assertTrue(bounds.getBlockCountX()==9&&bounds.getBlockCountY()==11&&bounds.getBlockCountZ()==13,"Natural clinic has an invalid footprint: "+theme);
        var placement=piece.getPlacementData().copy().setBoundingBox(null);int foundation=0;
        for(var block:List.of(Blocks.STONE_BRICKS,Blocks.CUT_SANDSTONE,Blocks.POLISHED_ANDESITE))for(var info:piece.getTemplate().getInfosForBlock(pos,placement,block))if(info.pos().getY()==pos.getY()){
            ctx.assertTrue(world.getBlockState(info.pos()).equals(info.state()),"Actual FULL clinic foundation is missing/replaced: "+theme+" "+info.pos()+" "+world.getBlockState(info.pos())+" expected "+info.state());foundation++;
        }
        ctx.assertTrue(foundation==77,"Natural clinic template must have its complete 7x11 constructed foundation: "+theme+" / "+foundation);
        var lower=pos.add(6,2,6);var upper=lower.up();var state=world.getBlockState(lower);
        ctx.assertTrue(state.isOf(ExpansionBlocks.MEDIC_CABINET)&&state.get(DoorBlock.HALF)==DoubleBlockHalf.LOWER&&state.get(DoorBlock.FACING)==Direction.WEST&&state.get(com.tetanusmod.expansion.MedicineCabinetBlock.COMPLETE),"Actual native clinic cabinet lower half missing/misoriented: "+theme+" "+lower+" "+state);
        ctx.assertTrue(world.getBlockState(upper).isOf(ExpansionBlocks.MEDIC_CABINET)&&world.getBlockState(upper).get(DoorBlock.HALF)==DoubleBlockHalf.UPPER,"Actual native clinic cabinet upper half missing: "+theme);
        // A FULL chunk can still await native palette/POI indexing. Require the
        // exact storage assertions after real player presence and ticking readiness.
        if(poiReady){
            ctx.assertTrue(world.getPointOfInterestStorage().hasTypeAt(MedicVillage.JOB_KEY,lower),"Actual native clinic cabinet is absent from native POI storage: "+theme);
            ctx.assertTrue(world.getPointOfInterestStorage().getType(upper).isEmpty(),"Native clinic cabinet upper half creates duplicate POI: "+theme);
        }
        var writers=piece.getTemplate().getInfosForBlock(pos,placement,MedicalWorkstations.WRITER);
        ctx.assertTrue(writers.size()==1,"Naturally generated clinic lacks its Prescription Writer: "+theme);
        var writer=writers.get(0);var aisle=writer.pos().offset(writer.state().get(HorizontalFacingBlock.FACING)).down();
        ctx.assertTrue(world.getBlockState(writer.pos()).equals(writer.state())&&world.getBlockState(writer.pos().down()).isSolidBlock(world,writer.pos().down()),"Natural clinic writer is missing/misoriented or unsupported: "+theme);
        ctx.assertTrue(world.isAir(aisle)&&world.isAir(aisle.up())&&world.getBlockState(aisle.down()).isSolidBlock(world,aisle.down()),"Natural clinic writer blocks its Survival approach: "+theme);
        var chest=pos.add(6,2,10);ctx.assertTrue(world.getBlockState(chest).isOf(Blocks.CHEST)&&world.getBlockEntity(chest)!=null&&world.getBlockEntity(chest).createNbt().getString("LootTable").equals("tetanusmod:chests/clinic"),"Actual clinic has no unopened loot chest: "+theme);
        var entry=pos.add(4,2,1);ctx.assertTrue(world.isAir(entry)&&world.isAir(entry.up())&&world.getBlockState(entry.down()).isSolidBlock(world,entry.down()),"Actual native clinic porch is obstructed or floating: "+theme+" "+entry);
        for(int z=2;z<=12;z++)ctx.assertTrue(world.getBlockState(pos.add(4,9,z)).isOf(Blocks.SPRUCE_SLAB)||theme.equals("desert")&&world.getBlockState(pos.add(4,9,z)).isOf(Blocks.SMOOTH_SANDSTONE_SLAB)||theme.equals("savanna")&&world.getBlockState(pos.add(4,9,z)).isOf(Blocks.ACACIA_SLAB),"Actual native clinic roof ridge missing: "+theme+" at "+pos.add(4,9,z));
    }
    private static boolean tickingReady(ServerWorld world,MedicStructurePiece piece){
        var b=piece.getBoundingBox();for(int x=b.getMinX()>>4;x<=b.getMaxX()>>4;x++)for(int z=b.getMinZ()>>4;z<=b.getMaxZ()>>4;z++)if(!world.getChunkManager().isTickingFutureReady(ChunkPos.toLong(x,z)))return false;return true;
    }
    private static Box residentArea(MedicStructurePiece piece){return Box.of(Vec3d.ofCenter(piece.getBoundingBox().getCenter()),57,35,61);}
    private static boolean isEstablishedThemeMedic(VillagerEntity entity,String theme){return entity.getVillagerData().getProfession()==MedicVillage.MEDIC&&entity.getExperience()>0&&Registries.VILLAGER_TYPE.getId(entity.getVillagerData().getType()).equals(new Identifier("minecraft",theme.equals("snowy")?"snow":theme));}
    private static boolean residentVisible(ServerWorld world,String theme,MedicStructurePiece piece){return !world.getEntitiesByClass(VillagerEntity.class,residentArea(piece),entity->isEstablishedThemeMedic(entity,theme)).isEmpty();}
    private static void assertResident(TestContext ctx,ServerWorld world,String theme,MedicStructurePiece piece){
        var medics=world.getEntitiesByClass(VillagerEntity.class,residentArea(piece),entity->isEstablishedThemeMedic(entity,theme));
        String expected=theme.equals("snowy")?"snow":theme;
        ctx.assertTrue(medics.size()==1&&Registries.VILLAGER_TYPE.getId(medics.get(0).getVillagerData().getType()).equals(new Identifier("minecraft",expected)),"Actual clinic lacks exactly one naturally spawned, biome-themed medic: "+theme+" / "+medics);
        ctx.assertTrue(medics.get(0).getExperience()>0&&!medics.get(0).writeNbt(new net.minecraft.nbt.NbtCompound()).getBoolean("RustRuinFieldMedic"),"Native village medic lost profession stability or incorrectly uses field scarcity: "+theme);
    }
    private static String residentSummary(ServerWorld world,List<MedicStructurePiece> pieces){var values=new ArrayList<String>();for(var piece:pieces)values.add(theme(world,piece)+":"+world.getEntitiesByClass(VillagerEntity.class,residentArea(piece),e->true));return values.toString();}
    private static void writeManifest(long seed,List<Site> sites,String name){
        var root=new JsonObject();root.addProperty("Seed",seed);root.addProperty("Scope",name.contains("planned")?"Native start planning only; no FULL placement":"Native real normal-Overworld FULL placement and resident validation required by the owning test");var entries=new JsonArray();
        for(var site:sites){var row=new JsonObject();row.addProperty("Theme",site.theme);var center=site.planned.getBoundingBox().getCenter();row.add("ObserverOrigin",point(center.getX(),140,center.getZ()));row.add("ExpectedStartChunk",point(site.chunk.x,0,site.chunk.z));row.add("PieceOrigin",point(site.planned.getPos().getX(),site.planned.getPos().getY(),site.planned.getPos().getZ()));entries.add(row);}root.add("Themes",entries);
        try{Files.writeString(Path.of(name),new GsonBuilder().setPrettyPrinting().create().toJson(root));}catch(java.io.IOException failure){throw new IllegalStateException("Cannot record actual clinic candidates",failure);}
    }
    private static JsonObject point(int x,int y,int z){var point=new JsonObject();point.addProperty("x",x);point.addProperty("y",y);point.addProperty("z",z);return point;}
    private static void cleanup(ServerWorld world,List<Observer> observers,List<ChunkPos> tickets){try{for(var observer:observers)observer.close();}finally{for(var ticket:tickets)world.setChunkForced(ticket.x,ticket.z,false);tickets.clear();}}
    private static final class Observer implements AutoCloseable {
        private final Connection connection=new Connection();private final net.minecraft.server.network.ServerPlayerEntity player;private boolean closed;
        private Observer(ServerWorld world,MedicStructurePiece piece){
            player=new net.minecraft.server.network.ServerPlayerEntity(world.getServer(),world,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"clinic-observer"));
            try{world.getServer().getPlayerManager().onPlayerConnect(connection,player);player.changeGameMode(net.minecraft.world.GameMode.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;var center=piece.getBoundingBox().getCenter();player.teleport(world,center.getX()+0.5,piece.getPos().getY()+2,center.getZ()+0.5,0,0);acknowledge();}catch(RuntimeException failure){close();throw failure;}
        }
        private void acknowledge(){if(connection.teleport>=0){int id=connection.teleport;connection.teleport=-1;player.networkHandler.onTeleportConfirm(new net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket(id));}}
        @Override public void close(){if(closed)return;closed=true;player.closeHandledScreen();var manager=player.getServer().getPlayerManager();if(manager.getPlayerList().contains(player))manager.remove(player);player.discard();if(manager.getPlayerList().contains(player))throw new IllegalStateException("Clinic observer leaked into native PlayerManager");}
    }
    private static final class Connection extends net.minecraft.network.ClientConnection {
        private int teleport=-1;private Connection(){super(net.minecraft.network.NetworkSide.SERVERBOUND);}
        @Override public void send(net.minecraft.network.packet.Packet<?> packet,net.minecraft.network.PacketCallbacks callbacks){if(packet instanceof net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket position)teleport=position.getTeleportId();super.send(packet,callbacks);}
    }
}