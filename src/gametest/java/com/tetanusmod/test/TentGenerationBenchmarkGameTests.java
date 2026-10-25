package com.tetanusmod.test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.world.CaveMedicStructure;
import com.tetanusmod.world.MedicStructurePiece;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.structure.StructureStart;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Bounded native noise/start planning benchmark; full native placement is covered separately. */
public class TentGenerationBenchmarkGameTests implements FabricGameTest {
    private static final long[] SEEDS={0,1,246813579L,918731L,8675309L,-741L};
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("RustRuinCampBenchmark");

    @GameTest(templateName=EMPTY_STRUCTURE,batchId="medic_generation_benchmark",tickLimit=2400)
    public void widerSearchImprovesCampCoverageAcrossSixNativeSeeds(TestContext ctx)throws Exception{
        var world=ctx.getWorld();var registry=world.getRegistryManager();
        var ops=RegistryOps.of(JsonOps.INSTANCE,registry);
        var generator=ChunkGenerator.CODEC.parse(ops,JsonParser.parseString("{\"type\":\"minecraft:noise\",\"settings\":\"minecraft:overworld\",\"biome_source\":{\"type\":\"minecraft:multi_noise\",\"preset\":\"minecraft:overworld\"}}")).result().orElseThrow();
        var current=(CaveMedicStructure)registry.get(RegistryKeys.STRUCTURE).get(TetanusMod.id("medic_tent"));
        var previous=new PreviousSearch(current);
        var placement=(RandomSpreadStructurePlacement)registry.get(RegistryKeys.STRUCTURE_SET).get(TetanusMod.id("medic_tents")).placement();
        var settings=registry.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS).get(ChunkGeneratorSettings.OVERWORLD);
        var rows=new ArrayList<Map<String,Object>>();int oldTotal=0,newTotal=0;double oldDistances=0,newDistances=0;
        var bean=ManagementFactory.getThreadMXBean();
        var memory=bean instanceof com.sun.management.ThreadMXBean extended&&extended.isThreadAllocatedMemorySupported()?extended:null;
        boolean enableCpu=bean.isCurrentThreadCpuTimeSupported()&&!bean.isThreadCpuTimeEnabled();
        boolean enableMemory=memory!=null&&!memory.isThreadAllocatedMemoryEnabled();
        if(enableCpu)bean.setThreadCpuTimeEnabled(true);if(enableMemory)memory.setThreadAllocatedMemoryEnabled(true);
        try{
            for(int seedIndex=0;seedIndex<SEEDS.length;seedIndex++){
                long seed=SEEDS[seedIndex];var noise=NoiseConfig.create(settings,registry.getWrapperOrThrow(RegistryKeys.NOISE_PARAMETERS),seed);
                var attempts=new ArrayList<Map<String,Object>>();int oldFound=0,newFound=0;double oldNearest=Double.POSITIVE_INFINITY,newNearest=Double.POSITIVE_INFINITY;
                ChunkPos nearestNew=null;net.minecraft.util.math.BlockBox nearestBounds=null;
                var warm=placement.getStartChunk(seed,0,0);
                create(previous,ctx,generator,noise,seed,warm);create(current,ctx,generator,noise,seed,warm);
                int index=0;
                for(int regionX=-1;regionX<=1;regionX++)for(int regionZ=-1;regionZ<=1;regionZ++){
                    var candidate=placement.getStartChunk(seed,regionX*placement.getSpacing(),regionZ*placement.getSpacing());
                    Metric oldResult,newResult;
                    // Alternate order to avoid systematically giving the wider search warmed code/caches.
                    if(((index++ + seedIndex)&1)==0){oldResult=measure(previous,ctx,generator,noise,seed,candidate,bean,memory);newResult=measure(current,ctx,generator,noise,seed,candidate,bean,memory);}
                    else{newResult=measure(current,ctx,generator,noise,seed,candidate,bean,memory);oldResult=measure(previous,ctx,generator,noise,seed,candidate,bean,memory);}
                    var row=new LinkedHashMap<String,Object>();row.put("regionX",regionX);row.put("regionZ",regionZ);row.put("startChunkX",candidate.x);row.put("startChunkZ",candidate.z);row.put("legacy16Sites",oldResult.json());row.put("current169Sites",newResult.json());attempts.add(row);
                    if(oldResult.start.hasChildren()){
                        ctx.assertTrue(newResult.start.hasChildren()&&oldResult.start.getChildren().get(0).getBoundingBox().equals(newResult.start.getChildren().get(0).getBoundingBox())&&((MedicStructurePiece)oldResult.start.getChildren().get(0)).getRotation()==((MedicStructurePiece)newResult.start.getChildren().get(0)).getRotation(),"Wider search lost a legacy start or changed its exact placement: seed="+seed+" candidate="+candidate);
                        oldFound++;oldNearest=Math.min(oldNearest,distance(oldResult.start));
                    }
                    if(newResult.start.hasChildren()){
                        assertFullNativeSafety(ctx,generator,noise,newResult.start);
                        newFound++;double distance=distance(newResult.start);
                        if(distance<newNearest){newNearest=distance;nearestNew=candidate;nearestBounds=newResult.start.getChildren().get(0).getBoundingBox();}
                    }
                }
                var row=new LinkedHashMap<String,Object>();row.put("seed",seed);row.put("sampledNativeGridRegions",9);row.put("legacyPlaced",oldFound);row.put("currentPlaced",newFound);row.put("legacyNearestCampCentreXZ",Double.isFinite(oldNearest)?oldNearest:null);row.put("currentNearestCampCentreXZ",Double.isFinite(newNearest)?newNearest:null);row.put("attempts",attempts);rows.add(row);
                oldTotal+=oldFound;newTotal+=newFound;oldDistances+=Double.isFinite(oldNearest)?oldNearest:7680;newDistances+=Double.isFinite(newNearest)?newNearest:7680;
                LOG.info("CAMP_SEARCH_SEED seed={} legacyPlaced={}/9 currentPlaced={}/9 legacyNearest={} currentNearest={}",seed,oldFound,newFound,Double.isFinite(oldNearest)?Math.round(oldNearest):"none",Double.isFinite(newNearest)?Math.round(newNearest):"none");
                if(nearestNew!=null){var repeated=create(current,ctx,generator,noise,seed,nearestNew);ctx.assertTrue(repeated.hasChildren()&&repeated.getChildren().get(0).getBoundingBox().equals(nearestBounds),"Camp search is nondeterministic for seed "+seed);}
            }
        }finally{if(enableMemory)memory.setThreadAllocatedMemoryEnabled(false);if(enableCpu)bean.setThreadCpuTimeEnabled(false);}
        var report=new LinkedHashMap<String,Object>();report.put("scope","Native noise columns, grid candidates and Structure.createStructureStart planning; not manually placed or inserted structures");report.put("currentTraversal","Original16-site shuffle/rolls first,153 shuffled fallback sites only on failure; cache shared");report.put("regionCountPerSeed",9);report.put("seedCount",SEEDS.length);report.put("legacyTotalPlaced",oldTotal);report.put("currentTotalPlaced",newTotal);report.put("legacyMissingSeedDistancePenalty",7680);report.put("legacyDistanceSumWithPenalty",oldDistances);report.put("currentDistanceSumWithPenalty",newDistances);report.put("maxCachedColumnsPerCurrentAttempt",63*63);report.put("maxFullCentreColumnsPerAttempt",169);report.put("placementSafety","Legacy sites use full native columns; fallback bounded samples only prefilter, followed by full native validation after a successful roll");report.put("fitColumnBottomY",-64);report.put("fitColumnTopYExclusive",64);report.put("maxCachedBlockStateReferencesAtOverworldHeight384",63*63*384);report.put("boundedOnlyCacheReferenceEstimate",169*384+(63*63-169)*128);report.put("cacheUpgrades","Neighbor-fit columns are replaced with full columns for surface/legacy/final placement proof; cumulative allocation includes those upgrades");report.put("allocationMetric","Cumulative allocations on the synchronous planning thread, not retained/peak memory; null when unavailable");report.put("seeds",rows);
        var output=FabricLoader.getInstance().getGameDir().resolve("tent-generation-benchmark.json");Files.writeString(output,new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(report),StandardCharsets.UTF_8);
        LOG.info("CAMP_SEARCH_TOTAL legacyPlaced={}/54 currentPlaced={}/54 report={}",oldTotal,newTotal,output);
        ctx.assertTrue(rows.stream().allMatch(row->((Number)row.get("currentPlaced")).intValue()>0),"Wider search still leaves a sampled seed without a camp in its nearest nine native grid cells; inspect benchmark JSON");
        ctx.assertTrue(newTotal>oldTotal,"Wider search did not improve aggregate suitable-camp coverage: "+oldTotal+" -> "+newTotal);
        ctx.assertTrue(newDistances<=oldDistances,"Wider search worsened aggregate nearest-camp distances in the fixed multi-seed sample");ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE,batchId="medic_generation_benchmark",tickLimit=1200)
    public void cachedSurfaceMatchesNativeWorldSurfaceHeightAcrossSeeds(TestContext ctx)throws Exception{
        var registry=ctx.getWorld().getRegistryManager();
        var generator=ChunkGenerator.CODEC.parse(RegistryOps.of(JsonOps.INSTANCE,registry),JsonParser.parseString("{\"type\":\"minecraft:noise\",\"settings\":\"minecraft:overworld\",\"biome_source\":{\"type\":\"minecraft:multi_noise\",\"preset\":\"minecraft:overworld\"}}")).result().orElseThrow();
        var settings=registry.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS).get(ChunkGeneratorSettings.OVERWORLD);
        var surface=CaveMedicStructure.class.getDeclaredMethod("surfaceHeight",VerticalBlockSample.class,HeightLimitView.class);surface.setAccessible(true);
        for(long seed:SEEDS){
            var noise=NoiseConfig.create(settings,registry.getWrapperOrThrow(RegistryKeys.NOISE_PARAMETERS),seed);
            for(int x:new int[]{-2031,-7,509})for(int z:new int[]{-1043,13,2051}){
                int expected=generator.getHeight(x,z,Heightmap.Type.WORLD_SURFACE_WG,ctx.getWorld(),noise);
                int actual=(Integer)surface.invoke(null,generator.getColumnSample(x,z,ctx.getWorld(),noise),ctx.getWorld());
                ctx.assertTrue(actual==expected,"Cached surface differs from native WORLD_SURFACE_WG at "+x+","+z+" seed="+seed+": "+actual+" vs "+expected);
            }
        }ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE,batchId="medic_generation_benchmark",tickLimit=1200)
    public void boundedFitColumnsMatchFullNoiseAndUpgradeForSurfaceProof(TestContext ctx)throws Exception{
        var world=ctx.getWorld();var registry=world.getRegistryManager();
        var generator=ChunkGenerator.CODEC.parse(RegistryOps.of(JsonOps.INSTANCE,registry),JsonParser.parseString("{\"type\":\"minecraft:noise\",\"settings\":\"minecraft:overworld\",\"biome_source\":{\"type\":\"minecraft:multi_noise\",\"preset\":\"minecraft:overworld\"}}")).result().orElseThrow();
        var settings=registry.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS).get(ChunkGeneratorSettings.OVERWORLD);
        var columnsType=Arrays.stream(CaveMedicStructure.class.getDeclaredClasses()).filter(type->type.getSimpleName().equals("Columns")).findFirst().orElseThrow();
        var constructor=columnsType.getDeclaredConstructor(Structure.Context.class);constructor.setAccessible(true);
        var at=columnsType.getDeclaredMethod("at",int.class,int.class);at.setAccessible(true);
        var center=columnsType.getDeclaredMethod("center",int.class,int.class);center.setAccessible(true);
        for(long seed:SEEDS){
            var noise=NoiseConfig.create(settings,registry.getWrapperOrThrow(RegistryKeys.NOISE_PARAMETERS),seed);
            var context=new Structure.Context(registry,generator,generator.getBiomeSource(),noise,world.getStructureTemplateManager(),seed,new ChunkPos(0,0),world,entry->true);
            var columns=constructor.newInstance(context);
            for(int x:new int[]{-2031,-7,509})for(int z:new int[]{-1043,13,2051}){
                var full=generator.getColumnSample(x,z,world,noise);var bounded=(VerticalBlockSample)at.invoke(columns,x,z);
                for(int y=-60;y<=53;y++)ctx.assertTrue(bounded.getState(y).equals(full.getState(y)),"Bounded native fit column changed terrain/fluid at "+x+","+y+","+z+" seed="+seed);
                var upgraded=(VerticalBlockSample)center.invoke(columns,x,z);
                for(int y=world.getBottomY();y<world.getTopY();y++)ctx.assertTrue(upgraded.getState(y).equals(full.getState(y)),"Centre column was not fully upgraded for surface proof: "+x+","+y+","+z+" seed="+seed);
                ctx.assertTrue(at.invoke(columns,x,z)==upgraded,"A later fit lookup discarded its full native centre sample");
            }
        }ctx.complete();
    }
    private record Metric(StructureStart start,long wallNanos,Long cpuNanos,Long allocatedBytes){
        Map<String,Object> json(){
            var result=new LinkedHashMap<String,Object>();result.put("placed",start.hasChildren());result.put("wallMs",wallNanos/1_000_000.0);result.put("cpuMs",cpuNanos==null?null:cpuNanos/1_000_000.0);result.put("allocatedBytes",allocatedBytes);
            result.put("bounds",start.hasChildren()?start.getChildren().get(0).getBoundingBox().toString():null);return result;
        }
    }
    private static Metric measure(Structure structure,TestContext ctx,ChunkGenerator generator,NoiseConfig noise,long seed,ChunkPos candidate,java.lang.management.ThreadMXBean bean,com.sun.management.ThreadMXBean memory){
        long id=Thread.currentThread().getId(),wall=System.nanoTime();Long cpu=bean.isCurrentThreadCpuTimeSupported()?bean.getCurrentThreadCpuTime():null,allocated=memory==null?null:memory.getThreadAllocatedBytes(id);
        var start=create(structure,ctx,generator,noise,seed,candidate);
        return new Metric(start,System.nanoTime()-wall,cpu==null?null:bean.getCurrentThreadCpuTime()-cpu,allocated==null?null:memory.getThreadAllocatedBytes(id)-allocated);
    }
    private static StructureStart create(Structure structure,TestContext ctx,ChunkGenerator generator,NoiseConfig noise,long seed,ChunkPos candidate){
        var world=ctx.getWorld();return structure.createStructureStart(world.getRegistryManager(),generator,generator.getBiomeSource(),noise,world.getStructureTemplateManager(),seed,candidate,0,world,structure.getValidBiomes()::contains);
    }
    private static void assertFullNativeSafety(TestContext ctx,ChunkGenerator generator,NoiseConfig noise,StructureStart start){
        var box=start.getChildren().get(0).getBoundingBox();int floor=box.getMinY();
        for(int x=box.getMinX()-1;x<=box.getMaxX()+1;x++)for(int z=box.getMinZ()-1;z<=box.getMaxZ()+1;z++){
            var full=generator.getColumnSample(x,z,ctx.getWorld(),noise);
            for(int y=floor;y<=box.getMaxY()+1;y++)ctx.assertTrue(full.getState(y).getFluidState().isEmpty(),"Selected multi-seed camp accepted full-native water/lava at "+x+","+y+","+z);
            if(x>=box.getMinX()+3&&x<=box.getMinX()+9&&z>=box.getMinZ()+3&&z<=box.getMinZ()+9)
                for(int y=floor+1;y<=floor+4;y++)ctx.assertTrue(full.getState(y).isAir(),"Selected multi-seed camp lacks its natural full-native open interior");
        }
    }    private static double distance(StructureStart start){var center=start.getChildren().get(0).getBoundingBox().getCenter();return Math.hypot(center.getX(),center.getZ());}

    /** Same physical fit and native seed path, retaining the committed 0..12 search extent. */
    private static final class PreviousSearch extends Structure {
        private final CaveMedicStructure current;
        private final java.lang.reflect.Method search;
        private PreviousSearch(CaveMedicStructure current)throws ReflectiveOperationException{
            super(new Config(current.getValidBiomes(),current.getStructureSpawns(),current.getFeatureGenerationStep(),current.getTerrainAdaptation()));this.current=current;
            search=CaveMedicStructure.class.getDeclaredMethod("findSite",Context.class,int.class);search.setAccessible(true);
        }
        @SuppressWarnings("unchecked") @Override protected Optional<StructurePosition> getStructurePosition(Context context){
            try{return (Optional<StructurePosition>)search.invoke(current,context,12);}catch(ReflectiveOperationException failure){throw new IllegalStateException("Cannot run native legacy-size camp search",failure);}
        }
        @Override public StructureType<?> getType(){return current.getType();}
    }
}