package com.tetanusmod.test;

import com.mojang.serialization.JsonOps;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.expansion.ExpansionBlocks;
import com.tetanusmod.expansion.MedicVillage;
import com.tetanusmod.world.CaveMedicStructure;
import com.tetanusmod.world.MedicStructurePiece;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import java.util.ArrayList;
import java.util.List;

/** Native discovery and full generation, rather than manually placing a locatable workstation. */
public class TentDiscoveryGameTests implements FabricGameTest {
    private static final RegistryKey<World> NOISE_WORLD=TentNoiseTestWorldBootstrap.KEY;

    @GameTest(templateName=EMPTY_STRUCTURE,batchId="medic_discovery")
    public void tentGridPreservesLegacyAlignmentAndActualFootprintSpacing(TestContext ctx)throws Exception{
        var registries=ctx.getWorld().getRegistryManager();
        var placement=(RandomSpreadStructurePlacement)registries.get(RegistryKeys.STRUCTURE_SET).get(TetanusMod.id("medic_tents")).placement();
        var encoded=StructurePlacement.TYPE_CODEC.encodeStart(JsonOps.INSTANCE,placement).result().orElseThrow().getAsJsonObject();
        ctx.assertTrue(placement.getSpacing()==160&&placement.getSeparation()==128&&encoded.get("salt").getAsInt()==813260217,"Legacy grid alignment or hard spacing changed");
        ctx.assertTrue((!encoded.has("frequency")||encoded.get("frequency").getAsFloat()==1F),"Eligible caves are still discarded by a second grid-frequency roll");
        var chance=CaveMedicStructure.class.getDeclaredField("SITE_CHANCE");chance.setAccessible(true);
        ctx.assertTrue(Math.abs(chance.getFloat(null)-0.15F)<0.00001F,"Suitable cave sites no longer have the requested 15% chance");
        var tent=registries.get(RegistryKeys.STRUCTURE).get(TetanusMod.id("medic_tent"));
        for(String biome:new String[]{"plains","desert","lush_caves","dripstone_caves"}){
            var entry=registries.get(RegistryKeys.BIOME).entryOf(RegistryKey.of(RegistryKeys.BIOME,new Identifier("minecraft",biome)));
            ctx.assertTrue(tent.getValidBiomes().contains(entry),"Ordinary dry caves are excluded below "+biome);
        }
        var searchField=CaveMedicStructure.class.getDeclaredField("MAX_SITE_OFFSET");searchField.setAccessible(true);
        int footprintSpan=searchField.getInt(null)+12;
        ctx.assertTrue((placement.getSeparation()+1)*16-footprintSpan>2000,"Search window can violate hard camp spacing in the theoretical nearest grid positions");        for(long seed:new long[]{0,1,918731,Long.MAX_VALUE,-741}){
            var points=new ArrayList<ChunkPos>();
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)points.add(placement.getStartChunk(seed,x*160,z*160));
            for(int i=0;i<points.size();i++)for(int j=0;j<i;j++){
                // Subtract the complete local search offset and template width on both axes.
                // This covers opposite rotations and nearest edges, not merely chunk centers.
                long dx=Math.max(0,Math.abs((long)points.get(i).x-points.get(j).x)*16-footprintSpan);
                long dz=Math.max(0,Math.abs((long)points.get(i).z-points.get(j).z)*16-footprintSpan);
                ctx.assertTrue(dx*dx+dz*dz>2000L*2000,"Two complete camp footprints can approach within 2,000 blocks");
            }
        }
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE,batchId="medic_discovery")
    public void campTemplatePlacesAfterLushCaveDecorationAndFluidSprings(TestContext ctx){
        var tent=ctx.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE).get(TetanusMod.id("medic_tent"));
        ctx.assertTrue(tent.getFeatureGenerationStep()==net.minecraft.world.gen.GenerationStep.Feature.TOP_LAYER_MODIFICATION&&tent.getFeatureGenerationStep().ordinal()>net.minecraft.world.gen.GenerationStep.Feature.VEGETAL_DECORATION.ordinal(),"Camp still places before natural cave vegetation/pool features can overwrite its floor");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="medic_discovery",tickLimit=3600)
    public void nativeLocateGeneratesARealDryCampWithMedicGuardLootAndPoi(TestContext ctx){runNaturalDiscovery(ctx,new BlockPos(0,64,0));}
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="medic_discovery",tickLimit=3600)
    public void negativeOriginNaturallyGeneratesTheLushBoundaryCampWithoutFlooding(TestContext ctx){runNaturalDiscovery(ctx,new BlockPos(-5,140,14));}

    private void runNaturalDiscovery(TestContext ctx,BlockPos origin){
        var world=ctx.getWorld().getServer().getWorld(NOISE_WORLD);
        ctx.assertTrue(world!=null,"Real noise test dimension was not loaded; a flat or manually placed camp is insufficient");
        ctx.assertTrue(world.getChunkManager().getChunkGenerator() instanceof net.minecraft.world.gen.chunk.NoiseChunkGenerator,"Discovery fixture is still a flat test world");
        var registries=world.getRegistryManager();
        var registry=registries.get(RegistryKeys.STRUCTURE);
        var key=RegistryKey.of(RegistryKeys.STRUCTURE,TetanusMod.id("medic_tent"));
        var tent=registry.entryOf(key);
        var generator=world.getChunkManager().getChunkGenerator();
        // This is the exact native locate path: placement calculator, grid, start creation,
        // saved chunks and the real world's seed, with no calls to place a template by hand.
        var located=generator.locateStructure(world,RegistryEntryList.of(tent),origin,4,false);
        ctx.assertTrue(located!=null,"No naturally generating camp could be located in the first four 2,560-block grid rings (seed "+world.getSeed()+")");
        var locatedPos=located.getFirst();
        var startChunk=new ChunkPos(locatedPos);
        var start=world.getChunk(startChunk.x,startChunk.z,ChunkStatus.STRUCTURE_STARTS,true).getStructureStart(tent.value());
        ctx.assertTrue(start!=null&&start.hasChildren()&&start.getChildren().size()==1&&start.getChildren().get(0) instanceof MedicStructurePiece,"Native locate did not reference a saved camp start");
        var bounds=start.getChildren().get(0).getBoundingBox();
        ctx.assertTrue(bounds.getBlockCountX()==13&&bounds.getBlockCountZ()==13&&bounds.getBlockCountY()==8&&bounds.getMaxY()<64,"Camp does not occupy the expected underground footprint: "+bounds);
        assertNaturalDrySite(ctx,world,bounds);
        // Repeated native locate must find the same saved start, rather than rerolling a site.
        var repeated=generator.locateStructure(world,RegistryEntryList.of(tent),origin,4,false);
        ctx.assertTrue(repeated!=null&&locatedPos.equals(repeated.getFirst()),"Repeated locate rerolled the camp location");
        var addedTickets=new ArrayList<ChunkPos>();CampObserver observer=null;
        try{
            for(int x=bounds.getMinX()>>4;x<=bounds.getMaxX()>>4;x++)for(int z=bounds.getMinZ()>>4;z<=bounds.getMaxZ()>>4;z++){
                var chunk=new ChunkPos(x,z);
                if(!world.getForcedChunks().contains(chunk.toLong())){world.setChunkForced(x,z,true);addedTickets.add(chunk);}
                world.getChunk(x,z);
            }
            assertConstructedFloor(ctx,world,(MedicStructurePiece)start.getChildren().get(0));
            int initialWheat=0;
            for(var pos:BlockPos.iterate(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX(),bounds.getMaxY(),bounds.getMaxZ()))if(world.getBlockState(pos).isOf(Blocks.WHEAT))initialWheat++;
            ctx.assertTrue(initialWheat==8,"Fresh fully generated cave camp lost crops before any resident/observer ticks: "+initialWheat);
            observer=new CampObserver(world,bounds);final CampObserver present=observer;
            ctx.assertTrue(observer.player.getServerWorld()==world,"Connected observer was not teleported into the real native noise world");
            logResidents(world,bounds,"native-player-arrived");
            runCommand(ctx,world,Vec3d.of(origin),"locate structure tetanusmod:medic_tent");
            runCommand(ctx,world,Vec3d.of(origin),"rustandruin locate tent",((MedicStructurePiece)start.getChildren().get(0)).campEntrance());
            runCommand(ctx,world,Vec3d.of(origin),"locate structure rustandruin:medic_tent",((MedicStructurePiece)start.getChildren().get(0)).campEntrance());
            runCommand(ctx,world,Vec3d.of(origin),"locate structure #rustandruin:medic_tent");
            long initialTicks=TentNoiseTestWorldBootstrap.nativeTicks();boolean[] finished={false};
            // Poll actual native ticking futures and entity visibility, rather than assuming
            // accelerated GameTest ticks give generation workers a fixed wall-clock budget.
            // Every callback is queued before execution; no mutation of the native task map
            // occurs while it is being iterated.
            for(int tick=40;tick<=3000;tick+=10){final int scheduled=tick;
                ctx.runAtTick(tick,()->{
                    if(finished[0])return;
                    present.acknowledge();
                    boolean ready=tickingReady(world,bounds)&&residentsVisible(world,bounds);
                    if(scheduled==40||scheduled==200||scheduled==1000||scheduled==3000)logResidents(world,bounds,"native-ready-poll-"+scheduled);
                    if(!ready&&scheduled<3000)return;
                    finished[0]=true;
                    try{
                        ctx.assertTrue(TentNoiseTestWorldBootstrap.nativeTicks()>initialTicks,"Native noise world never received actual world ticks");
                        ctx.assertTrue(ready,"Native player observer did not make camp chunks/entity tracking ready before deadline");
                        assertPlacedCamp(ctx,world,start);
                        org.slf4j.LoggerFactory.getLogger("RustRuinTentDiscovery").info("NATIVE_TENT_DISCOVERY_PASS seed={} sourceOrigin={} locate={} camp={} startChunk={} nativeWorldTicks={}",world.getSeed(),origin,locatedPos,bounds,start.getPos(),TentNoiseTestWorldBootstrap.nativeTicks()-initialTicks);
                        ctx.complete();
                    }finally{cleanupObservation(world,present,addedTickets);}
                });
            }
        }catch(RuntimeException failure){cleanupObservation(world,observer,addedTickets);throw failure;}
    }

    private static boolean tickingReady(ServerWorld world,BlockBox bounds){
        for(int x=bounds.getMinX()>>4;x<=bounds.getMaxX()>>4;x++)for(int z=bounds.getMinZ()>>4;z<=bounds.getMaxZ()>>4;z++)if(!world.getChunkManager().isTickingFutureReady(ChunkPos.toLong(x,z)))return false;
        return true;
    }
    private static Box campArea(BlockBox bounds){return new Box(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX()+1,bounds.getMaxY()+1,bounds.getMaxZ()+1).expand(24);}
    private static boolean residentsVisible(ServerWorld world,BlockBox bounds){
        var area=campArea(bounds);return !world.getEntitiesByClass(VillagerEntity.class,area,e->e.getVillagerData().getProfession()==MedicVillage.MEDIC&&e.getCommandTags().contains("rustruin_camp_medic")&&e.writeNbt(new NbtCompound()).getBoolean("RustRuinFieldMedic")).isEmpty()&&!world.getEntitiesByClass(VillagerEntity.class,area,e->e.getVillagerData().getProfession()==net.minecraft.village.VillagerProfession.FARMER&&e.getCommandTags().contains("rustruin_camp_support")).isEmpty()&&!world.getEntitiesByClass(IronGolemEntity.class,area,e->MobVariants.permanent(e)&&e.isPlayerCreated()&&e.getMaxHealth()==200&&e.getCommandTags().contains(MedicStructurePiece.CAMP_GUARD_TAG)).isEmpty();
    }
    private static final class CampObserver implements AutoCloseable {
        private final ObserverConnection connection=new ObserverConnection();
        private final net.minecraft.server.network.ServerPlayerEntity player;
        private boolean closed;
        private CampObserver(ServerWorld world,BlockBox bounds){
            player=new net.minecraft.server.network.ServerPlayerEntity(world.getServer(),world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"native-camp-test"));
            try{
                world.getServer().getPlayerManager().onPlayerConnect(connection,player);
                player.changeGameMode(net.minecraft.world.GameMode.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;
                var center=bounds.getCenter();player.teleport(world,center.getX()+0.5,bounds.getMinY()+2,center.getZ()+0.5,0,0);acknowledge();
            }catch(RuntimeException failure){close();throw failure;}
        }
        private void acknowledge(){if(connection.teleport>=0){int id=connection.teleport;connection.teleport=-1;player.networkHandler.onTeleportConfirm(new net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket(id));}}
        @Override public void close(){
            if(closed)return;closed=true;player.closeHandledScreen();
            var manager=player.getServer().getPlayerManager();if(manager.getPlayerList().contains(player))manager.remove(player);player.discard();
            if(manager.getPlayerList().contains(player))throw new IllegalStateException("Native camp observer leaked into the player manager");
        }
    }
    private static final class ObserverConnection extends net.minecraft.network.ClientConnection {
        private int teleport=-1;
        private ObserverConnection(){super(net.minecraft.network.NetworkSide.SERVERBOUND);}
        @Override public void send(net.minecraft.network.packet.Packet<?> packet,net.minecraft.network.PacketCallbacks callbacks){
            if(packet instanceof net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket position)teleport=position.getTeleportId();super.send(packet,callbacks);
        }
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="medic_discovery",tickLimit=600)
    public void tentRejectsWaterLavaAndWaterloggingAtEveryRoomBoundaryVoxel(TestContext ctx)throws Exception{
        // Cache actual native column samples without a world mutation. Exercise the exact
        // production fit predicate independently of a lucky dry natural seed, especially
        // the exterior layer directly above the eight-block-high cleared template.
        var columnsType=java.util.Arrays.stream(CaveMedicStructure.class.getDeclaredClasses()).filter(type->type.getSimpleName().equals("Columns")).findFirst().orElseThrow();
        var constructor=columnsType.getDeclaredConstructor(net.minecraft.world.gen.structure.Structure.Context.class);constructor.setAccessible(true);
        var columns=constructor.newInstance((Object)null);
        var samplesField=columnsType.getDeclaredField("samples");samplesField.setAccessible(true);
        @SuppressWarnings("unchecked") var samples=(java.util.Map<Long,net.minecraft.world.gen.chunk.VerticalBlockSample>)samplesField.get(columns);
        var states=new java.util.HashMap<Long,net.minecraft.block.BlockState[]>();
        for(int x=-1;x<=13;x++)for(int z=-1;z<=13;z++){
            var values=new net.minecraft.block.BlockState[9];java.util.Arrays.fill(values,Blocks.AIR.getDefaultState());values[0]=Blocks.STONE.getDefaultState();
            long key=ChunkPos.toLong(x,z);states.put(key,values);samples.put(key,new net.minecraft.world.gen.chunk.VerticalBlockSample(0,values));
        }
        var fit=CaveMedicStructure.class.getDeclaredMethod("dryRoom",columnsType,int.class,int.class,int.class);fit.setAccessible(true);
        ctx.assertTrue((Boolean)fit.invoke(null,columns,0,0,0),"Dry natural cavern fixture is unexpectedly rejected");
        var waterlogged=Blocks.OAK_FENCE.getDefaultState().with(net.minecraft.state.property.Properties.WATERLOGGED,true);
        for(var wet:List.of(Blocks.WATER.getDefaultState(),Blocks.LAVA.getDefaultState(),waterlogged)){
            for(int x=-1;x<=13;x++)for(int z=-1;z<=13;z++)for(int y=0;y<=8;y++){
                var values=states.get(ChunkPos.toLong(x,z));var original=values[y];values[y]=wet;
                try{ctx.assertFalse((Boolean)fit.invoke(null,columns,0,0,0),"Room accepted adjacent/occupied fluid at "+new BlockPos(x,y,z)+": "+wet);}
                finally{values[y]=original;}
            }
        }
        ctx.assertTrue((Boolean)fit.invoke(null,columns,0,0,0),"Fluid regression fixture did not restore its native dry columns");ctx.complete();
    }

    private static void cleanupObservation(ServerWorld world,CampObserver observer,List<ChunkPos> tickets){
        try{if(observer!=null)observer.close();}finally{releaseTickets(world,tickets);}
    }
    private static void releaseTickets(ServerWorld world,List<ChunkPos> tickets){
        for(var chunk:tickets)world.setChunkForced(chunk.x,chunk.z,false);tickets.clear();
    }
    private static void logResidents(ServerWorld world,BlockBox bounds,String phase){
        var logger=org.slf4j.LoggerFactory.getLogger("RustRuinTentDiscovery");
        var area=new Box(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX()+1,bounds.getMaxY()+1,bounds.getMaxZ()+1).expand(24);
        int expected=world.getStructureTemplateManager().getTemplate(TetanusMod.id("medic_tent")).orElseThrow().writeNbt(new NbtCompound()).getList("entities",10).size();
        var villagers=world.getEntitiesByClass(VillagerEntity.class,area,e->true);
        logger.info("NATIVE_TENT_RESIDENTS phase={} seed={} camp={} expectedTemplateNPCs={} trackedVillagers={} trackedGolems={} nativeWorldTicks={} worldPlayers={} tickingReady={} pendingChunkTasks={} chunkDebug={}",phase,world.getSeed(),bounds,expected,villagers.size(),world.getEntitiesByClass(IronGolemEntity.class,area,e->true).size(),TentNoiseTestWorldBootstrap.nativeTicks(),world.getPlayers().size(),tickingReady(world,bounds),world.getChunkManager().getPendingTasks(),world.getChunkManager().getChunkLoadingDebugInfo(new ChunkPos(bounds.getCenter())));
        try{var field=ServerWorld.class.getDeclaredField("entityManager");field.setAccessible(true);logger.info("NATIVE_TENT_ENTITY_MANAGER phase={} state={}",phase,((net.minecraft.server.world.ServerEntityManager<?>)field.get(world)).getDebugString());}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Cannot inspect native noise-world entity tracking",failure);}
        for(var medic:villagers){var nbt=medic.writeNbt(new NbtCompound());logger.info("NATIVE_TENT_VILLAGER phase={} uuid={} pos={} profession={} xp={} field={}",phase,medic.getUuid(),medic.getPos(),net.minecraft.registry.Registries.VILLAGER_PROFESSION.getId(medic.getVillagerData().getProfession()),medic.getExperience(),nbt.getBoolean("RustRuinFieldMedic"));}
    }
    private static void assertNaturalDrySite(TestContext ctx,ServerWorld world,BlockBox bounds){
        var generator=world.getChunkManager().getChunkGenerator();var noise=world.getChunkManager().getNoiseConfig();
        int floor=bounds.getMinY();
        for(int x=bounds.getMinX()-1;x<=bounds.getMaxX()+1;x++)for(int z=bounds.getMinZ()-1;z<=bounds.getMaxZ()+1;z++){
            var column=generator.getColumnSample(x,z,world,noise);
            for(int y=floor;y<=bounds.getMaxY()+1;y++)ctx.assertTrue(column.getState(y).getFluidState().isEmpty(),"Camp accepted natural water/lava at "+new BlockPos(x,y,z));
            if(x>=bounds.getMinX()+3&&x<=bounds.getMinX()+9&&z>=bounds.getMinZ()+3&&z<=bounds.getMinZ()+9)
                for(int y=floor+1;y<=floor+4;y++)ctx.assertTrue(column.getState(y).isAir(),"Camp excavated its central room instead of using an existing cave");
        }
    }

    private static void assertConstructedFloor(TestContext ctx,ServerWorld world,MedicStructurePiece piece){
        var placement=piece.getPlacementData().copy().setBoundingBox(null);int cells=0;
        for(var block:List.of(Blocks.STONE_BRICKS,Blocks.MOSSY_STONE_BRICKS,Blocks.POLISHED_ANDESITE)){
            for(var info:piece.getTemplate().getInfosForBlock(piece.getPos(),placement,block))if(info.pos().getY()==piece.getPos().getY()){
                ctx.assertTrue(world.getBlockState(info.pos()).equals(info.state()),"Natural decoration overwrote a constructed camp foundation cell at "+info.pos()+": "+world.getBlockState(info.pos())+" vs "+info.state());cells++;
            }
        }
        ctx.assertTrue(cells==169,"Camp template no longer has169 constructed, non-natural foundation cells: "+cells);
    }
    private static void assertPlacedCamp(TestContext ctx,ServerWorld world,StructureStart start){
        var piece=(MedicStructurePiece)start.getChildren().get(0);var bounds=piece.getBoundingBox();BlockPos cabinet=null,chest=null;int lower=0,upper=0;
        assertConstructedFloor(ctx,world,piece);
        var waterPlacement=new net.minecraft.structure.StructurePlacementData().setRotation(piece.getRotation());
        var plannedWater=piece.getTemplate().getInfosForBlock(piece.getPos(),waterPlacement,Blocks.WATER);
        ctx.assertTrue(plannedWater.size()==1&&plannedWater.get(0).state().equals(Blocks.WATER.getDefaultState()),"Camp must contain exactly one planned source-water irrigation cell");
        var source=plannedWater.get(0).pos();
        ctx.assertTrue(world.getBlockState(source.down()).isSolidBlock(world,source.down()),"Planned irrigation basin has no watertight solid bottom");
        var entrance=piece.campEntrance();
        ctx.assertTrue(bounds.contains(entrance)&&world.isAir(entrance)&&world.isAir(entrance.up())&&world.getBlockState(entrance.down()).isSolidBlock(world,entrance.down()),"Exact camp entrance is obstructed or lacks a solid floor");
        for(var mutable:BlockPos.iterate(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX(),bounds.getMaxY(),bounds.getMaxZ())){
            var pos=mutable.toImmutable();var state=world.getBlockState(pos);
            ctx.assertTrue(state.getFluidState().isEmpty()||pos.equals(source)&&state.equals(plannedWater.get(0).state()),"Actual camp generation has unplanned/flowing fluid at "+pos);
            if(state.isOf(ExpansionBlocks.FIELD_MEDIC_CABINET)){
                if(state.get(DoorBlock.HALF)==DoubleBlockHalf.LOWER){lower++;cabinet=pos;}else upper++;
            }
            if(state.isOf(Blocks.CHEST)){chest=pos;}
        }
        ctx.assertTrue(lower==1&&upper==1&&cabinet!=null,"Naturally generated camp lost either field-cabinet half");
        ctx.assertTrue(chest!=null&&world.getBlockEntity(chest)!=null&&world.getBlockEntity(chest).createNbt().getString("LootTable").equals("tetanusmod:chests/medic_tent"),"Natural camp lacks its unopened loot chest");
        ctx.assertTrue(world.getPointOfInterestStorage().hasTypeAt(MedicVillage.FIELD_JOB_KEY,cabinet),"Naturally generated field cabinet was not indexed as a tent POI");
        ctx.assertTrue(world.getPointOfInterestStorage().getType(cabinet.up()).isEmpty(),"Natural cabinet upper half added a duplicate POI");
        var box=new Box(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX()+1,bounds.getMaxY()+1,bounds.getMaxZ()+1).expand(24);
        var medics=world.getEntitiesByClass(VillagerEntity.class,box,e->e.getVillagerData().getProfession()==MedicVillage.MEDIC&&e.getCommandTags().contains("rustruin_camp_medic")&&e.writeNbt(new NbtCompound()).getBoolean("RustRuinFieldMedic"));
        ctx.assertTrue(medics.size()==1&&medics.get(0).writeNbt(new NbtCompound()).getBoolean("RustRuinFieldMedic"),"Natural camp lacks exactly one scarce-stock field medic");
        var residents=new Box(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX()+1,bounds.getMaxY()+1,bounds.getMaxZ()+1);
        var farmers=world.getEntitiesByClass(VillagerEntity.class,residents,e->e.getCommandTags().contains("rustruin_camp_support"));
        ctx.assertTrue(farmers.size()==1&&farmers.get(0).getVillagerData().getProfession()==net.minecraft.village.VillagerProfession.FARMER&&farmers.get(0).getExperience()>=1,"Natural camp lacks exactly one genuine support farmer inside its enclosure");
        var golems=world.getEntitiesByClass(IronGolemEntity.class,box,e->MobVariants.permanent(e)&&e.isPlayerCreated()&&e.getMaxHealth()==200&&e.getCommandTags().contains(MedicStructurePiece.CAMP_GUARD_TAG));
        ctx.assertTrue(golems.size()==1&&MobVariants.permanent(golems.get(0))&&golems.get(0).isPlayerCreated()&&golems.get(0).getMaxHealth()==200,"Natural camp lacks its friendly tetanized defender");
        runCommand(ctx,world,Vec3d.ofCenter(cabinet),"locate poi tetanusmod:medic_tent");
        runCommand(ctx,world,Vec3d.ofCenter(cabinet),"locate poi rustandruin:medic_tent");
    }

    private static void runCommand(TestContext ctx,ServerWorld world,Vec3d origin,String command){runCommand(ctx,world,origin,command,null);}
    private static void runCommand(TestContext ctx,ServerWorld world,Vec3d origin,String command,BlockPos expectedEntrance){
        var messages=new ArrayList<Text>();
        CommandOutput output=new CommandOutput(){
            @Override public void sendMessage(Text message){messages.add(message);}
            @Override public boolean shouldReceiveFeedback(){return true;}
            @Override public boolean shouldTrackOutput(){return true;}
            @Override public boolean shouldBroadcastConsoleToOps(){return false;}
        };
        var source=world.getServer().getCommandSource().withWorld(world).withPosition(origin).withLevel(4).withOutput(output);
        try{world.getServer().getCommandManager().getDispatcher().execute(command,source);}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException failure){throw new IllegalStateException("Native discovery command failed: "+command,failure);}
        String expected=command.startsWith("locate poi ")?"commands.locate.poi.success":"commands.locate.structure.success";
        ctx.assertTrue(messages.stream().anyMatch(message->message.getContent() instanceof net.minecraft.text.TranslatableTextContent text&&text.getKey().equals(expected)),"Discovery command produced no native success feedback: "+command+" / "+messages);
        if(expectedEntrance!=null){
            var entranceMessage=messages.stream().filter(message->message.getContent() instanceof net.minecraft.text.TranslatableTextContent text&&text.getKey().equals("commands.rustandruin.camp_entrance")).findFirst().orElseThrow(()->new IllegalStateException("Branded command omitted exact underground entrance: "+command));
            var content=(net.minecraft.text.TranslatableTextContent)entranceMessage.getContent();
            ctx.assertTrue(content.getArgs().length==1&&content.getArgs()[0] instanceof Text,"Exact entrance feedback lost its coordinate component");
            var coordinate=(Text)content.getArgs()[0];
            ctx.assertTrue(coordinate.getContent() instanceof net.minecraft.text.TranslatableTextContent text&&text.getKey().equals("chat.coordinates"),"Exact entrance feedback did not use native coordinates");
            var args=((net.minecraft.text.TranslatableTextContent)coordinate.getContent()).getArgs();
            ctx.assertTrue(args.length==3&&args[0] instanceof Number&&args[1] instanceof Number&&args[2] instanceof Number,"Exact entrance feedback contains an unknown/non-numeric Y");
            ctx.assertTrue(new BlockPos(((Number)args[0]).intValue(),((Number)args[1]).intValue(),((Number)args[2]).intValue()).equals(expectedEntrance),"Branded command exact entrance disagrees with actual native rotated piece");
            var click=coordinate.getStyle().getClickEvent();
            ctx.assertTrue(click!=null&&click.getAction()==net.minecraft.text.ClickEvent.Action.SUGGEST_COMMAND&&click.getValue().equals("/tp @s "+expectedEntrance.getX()+" "+expectedEntrance.getY()+" "+expectedEntrance.getZ()),"Exact entrance teleport suggestion is missing or inconsistent");
        }
    }
}