package com.tetanusmod.test;

import com.google.gson.GsonBuilder;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.expansion.Rabies;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Native automatic despawn in the real graphical client's entity-ticking world. */
public final class GraphicalDespawnAudit {
    private final AtomicReference<Throwable> failure=new AtomicReference<>();
    private volatile Scene scene;
    private int phase,ticks,waitTicks;
    private int originalClientSimulation,originalServerSimulation;
    private boolean started,finished,clientRestored;
    private String viewer;

    public void start(MinecraftClient client) {
        if(started)throw new IllegalStateException("Graphical despawn audit started twice");started=true;
        if(client.getServer()==null||client.player==null)throw new IllegalStateException("Despawn proof requires an actual integrated client");
        viewer=FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
        originalClientSimulation=client.options.getSimulationDistance().getValue();
        originalServerSimulation=client.getServer().getPlayerManager().getSimulationDistance();
        client.options.getSimulationDistance().setValue(12);client.options.useKey.setPressed(false);client.setScreen(null);
        var id=client.player.getUuid();
        client.getServer().execute(()->{
            try{var player=client.getServer().getPlayerManager().getPlayer(id);if(player==null)throw new IllegalStateException("Actual despawn-test player disappeared");scene=new Scene(player,originalServerSimulation);scene.prepare();}
            catch(Throwable problem){failure.set(problem);if(scene!=null)scene.restore();}
        });
    }
    public boolean tick(MinecraftClient client) {
        try{return tickInternal(client);}
        catch(RuntimeException|Error problem){restoreClient(client);if(scene!=null&&client.getServer()!=null)client.getServer().execute(()->scene.restore());throw problem;}
    }
    private boolean tickInternal(MinecraftClient client) {
        if(!started)throw new IllegalStateException("Despawn audit not started");
        if(failure.get()!=null){restoreClient(client);throw new IllegalStateException("Graphical automatic despawn verification failed",failure.get());}
        if(finished)return true;
        if(scene==null||!scene.ready||client.player==null||client.world==null)return false;
        ticks++;
        if(phase==0) {
            if(!scene.beforeReady){server(client,()->scene.observeBefore());return false;}
            boolean allVisible=true;
            for(var specimen:scene.specimens)if(client.world.getEntityById(specimen.entityId)==null)allVisible=false;
            if(!allVisible){if(++waitTicks>400)throw new IllegalStateException("Before-departure mobs never all reached the graphical client");return false;}
            scene.specimens.forEach(specimen->specimen.evidence.put("BeforeClientVisible",true));
            screenshot(client,"despawn-before");phase=1;ticks=0;return false;
        }
        if(phase==1) {
            if(ticks<8)return false;
            server(client,()->scene.depart());phase=2;ticks=0;return false;
        }
        if(phase==2) {
            if(!scene.away){return false;}
            if(ticks==10)screenshot(client,"despawn-away-160-blocks");
            server(client,()->scene.observeAway());
            if(!scene.nativeComplete)return false;
            server(client,()->scene.returnToScene());phase=3;ticks=0;return false;
        }
        if(phase==3) {
            if(!scene.returned||ticks<60)return false;
            for(var specimen:scene.specimens) {
                boolean present=client.world.getEntityById(specimen.entityId)!=null;
                if(specimen.control&&!present){if(++waitTicks>600)throw new IllegalStateException("Persistent control failed to return to the client: "+specimen.label);return false;}
                if(!specimen.control&&present)throw new IllegalStateException("Automatically despawned entity reappeared on the client: "+specimen.label);
                specimen.evidence.put("ClientPresentAfterReturn",present);
            }
            screenshot(client,"despawn-after-native-removal");phase=4;ticks=0;
            server(client,()->scene.verifyReturned());return false;
        }
        if(phase==4) {
            if(!scene.returnVerified||ticks<8)return false;
            var report=new LinkedHashMap<String,Object>();report.put("Result","PASS");report.put("ActualGraphicalClient",true);report.put("NativeAutomaticTickOnly",true);report.put("DirectCheckDespawnCalls",0);report.put("DirectTargetRemovalCalls",0);report.put("SimulationDistanceDuringTest",12);report.put("DistanceBlocks",160);report.put("NativeAwayTicks",scene.awayTicks);report.put("Specimens",scene.specimens.stream().map(specimen->specimen.evidence).toList());
            report.put("Screenshots",List.of(file("despawn-before"),file("despawn-away-160-blocks"),file("despawn-after-native-removal")));
            try{Files.writeString(Path.of("graphical-despawn-"+viewer+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}catch(java.io.IOException problem){restoreClient(client);throw new IllegalStateException(problem);}
            server(client,()->scene.restore());restoreClient(client);phase=5;
            org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_DESPAWN_PASS native automatic removal of six unnamed hostile variants; named and three livestock controls retained");return false;
        }
        if(phase==5&&scene.restored){finished=true;return true;}
        return false;
    }
    private void server(MinecraftClient client,Runnable action){client.getServer().execute(()->{try{action.run();}catch(Throwable problem){failure.set(problem);scene.restore();}});}
    private String file(String name){return "rust-and-ruin-"+viewer+"-"+name+".png";}
    private void screenshot(MinecraftClient client,String name){ScreenshotRecorder.saveScreenshot(client.runDirectory,file(name),client.getFramebuffer(),text->{});}
    private void restoreClient(MinecraftClient client){if(!clientRestored){clientRestored=true;client.options.getSimulationDistance().setValue(originalClientSimulation);client.options.useKey.setPressed(false);}}

    private static final class Specimen {
        final String label;final MobEntity entity;final boolean control;final UUID uuid;final int entityId;
        final Map<String,Object> evidence=new LinkedHashMap<>();
        int beforeAge;
        Specimen(String label,MobEntity entity,boolean control){this.label=label;this.entity=entity;this.control=control;uuid=entity.getUuid();entityId=entity.getId();evidence.put("Label",label);evidence.put("Uuid",uuid.toString());evidence.put("EntityId",entityId);evidence.put("Control",control);evidence.put("SpawnReason","NATURAL");evidence.put("PersistentBefore",entity.isPersistent());evidence.put("AiDisabledBefore",entity.isAiDisabled());}
    }
    private static final class Scene {
        final ServerPlayerEntity player;final ServerWorld world;final int originalSimulation;final GameMode originalMode;final Vec3d originalPosition;final float originalYaw,originalPitch;final boolean originalNoGravity,originalFlying,originalMobSpawning;
        final List<Specimen> specimens=new ArrayList<>();final List<ChunkPos> addedForced=new ArrayList<>();
        BlockPos origin;long preparedAt,leftAt;volatile boolean ready,beforeReady,away,nativeComplete,returned,returnVerified;volatile long awayTicks;volatile boolean restored;
        Scene(ServerPlayerEntity player,int simulation){this.player=player;world=player.getServerWorld();originalSimulation=simulation;originalMode=player.interactionManager.getGameMode();originalPosition=player.getPos();originalYaw=player.getYaw();originalPitch=player.getPitch();originalNoGravity=player.hasNoGravity();originalFlying=player.getAbilities().flying;originalMobSpawning=world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING);}
        void prepare() {
            require(world.getDifficulty()!=Difficulty.PEACEFUL,"Peaceful difficulty cannot prove distance-based hostile despawn");
            player.getServer().getPlayerManager().setSimulationDistance(12);world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,world.getServer());
            player.changeGameMode(GameMode.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;player.sendAbilitiesUpdate();
            int x=((player.getBlockX()>>4)+32)<<4,z=((player.getBlockZ()>>4)+32)<<4;
            origin=new BlockPos(x,Math.min(world.getTopY()-16,Math.max(world.getSeaLevel()+8,player.getBlockY()+12)),z);
            for(int cx=origin.getX()>>4;cx<=(origin.getX()+50)>>4;cx++)for(int cz=origin.getZ()>>4;cz<=(origin.getZ()+4)>>4;cz++){
                var chunk=new ChunkPos(cx,cz);if(!world.getForcedChunks().contains(chunk.toLong())){world.setChunkForced(cx,cz,true);addedForced.add(chunk);}world.getChunk(cx,cz);
            }
            for(int cell=0;cell<10;cell++)buildPen(cell);
            camera();
            add("Tetanized Zombie",EntityType.ZOMBIE,false,false,0);
            add("Rabid Zombie",EntityType.ZOMBIE,true,false,1);
            add("Tetanized Skeleton",EntityType.SKELETON,false,false,2);
            add("Tetanized Creeper",EntityType.CREEPER,false,false,3);
            add("Rabid Husk",EntityType.HUSK,true,false,4);
            add("Rabid Drowned",EntityType.DROWNED,true,false,5);
            var named=add("Name-tagged Tetanized Zombie",EntityType.ZOMBIE,false,true,6);
            var tag=new ItemStack(Items.NAME_TAG);tag.setCustomName(Text.literal("Named persistence control"));player.setStackInHand(Hand.MAIN_HAND,tag);tag.useOnEntity(player,named.entity,Hand.MAIN_HAND);
            require(named.entity.hasCustomName()&&named.entity.isPersistent(),"Native name tag did not make its control persistent");named.evidence.put("PersistentBefore",true);named.evidence.put("NamedUsingNativeNameTag",true);
            add("Tetanized Cow",EntityType.COW,false,true,7);
            add("Rabid Cow",EntityType.COW,true,true,8);
            addVanillaCow(9);
            player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);preparedAt=world.getTime();ready=true;
        }
        private void buildPen(int cell) {
            int start=cell*5;
            for(int dx=0;dx<=4;dx++)for(int dz=0;dz<=4;dz++)for(int y=0;y<=4;y++){
                var state=y==0||y==4?Blocks.STONE.getDefaultState():dx==0||dx==4||dz==0||dz==4?Blocks.GLASS.getDefaultState():Blocks.AIR.getDefaultState();world.setBlockState(origin.add(start+dx,y,dz),state);
            }
        }
        private MobEntity natural(EntityType<?> type,int cell,boolean rabid) {
            for(int attempt=0;attempt<16;attempt++){
                var created=type.create(world);require(created instanceof MobEntity,"Not a mob fixture: "+type);var mob=(MobEntity)created;
                var pos=origin.add(cell*5+2,1,2);mob.refreshPositionAndAngles(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,180,0);
                mob.initialize(world,world.getLocalDifficulty(pos),SpawnReason.NATURAL,null,null);
                if(mob.isPersistent()||mob.cannotDespawn()||mob.hasVehicle()||mob.hasPassengers()||(rabid&&MobVariants.permanent(mob))||(!rabid&&Rabies.rabid(mob)))continue;
                return mob;
            }
            throw new IllegalStateException("Could not initialize an ordinary, unkept natural fixture: "+type);
        }
        private Specimen add(String label,EntityType<?> type,boolean rabid,boolean control,int cell) {
            var mob=natural(type,cell,rabid);if(rabid)Rabies.makeRabid(mob);else MobVariants.makePermanent(mob);
            require(rabid?Rabies.rabid(mob):MobVariants.permanent(mob),"Mutation missing: "+label);require(!mob.isAiDisabled(),"Fixture disabled native mob AI");require(!mob.hasCustomName()&&!mob.isPersistent(),"Mutation forced a target to remain persistent: "+label);
            require(world.spawnEntity(mob),"Could not spawn "+label);var specimen=new Specimen(label,mob,control);specimen.evidence.put("CannotDespawnBefore",mob.cannotDespawn());specimens.add(specimen);return specimen;
        }
        private void addVanillaCow(int cell) {
            for(int attempt=0;attempt<16;attempt++){
                var cow=natural(EntityType.COW,cell,false);if(MobVariants.permanent(cow)||Rabies.rabid(cow))continue;require(world.spawnEntity(cow),"Could not spawn native cow control");specimens.add(new Specimen("Vanilla Cow",cow,true));return;
            }
            throw new IllegalStateException("Could not prepare an unmutated cow control");
        }
        private void camera(){player.teleport(world,origin.getX()+25,origin.getY()+5,origin.getZ()-40,0,5);player.setVelocity(Vec3d.ZERO);}
        void observeBefore() {
            if(beforeReady||world.getTime()-preparedAt<40)return;
            for(var specimen:specimens){require(world.getEntity(specimen.uuid)==specimen.entity&&specimen.entity.isAlive(),"Fixture vanished while player was near: "+specimen.label);require(world.shouldTickEntity(specimen.entity.getBlockPos()),"Fixture chunk is not entity-ticking before departure");require(specimen.entity.age>0,"Fixture never received a native entity tick");}
            beforeReady=true;
        }
        void depart() {
            if(away)return;
            for(var specimen:specimens)specimen.beforeAge=specimen.entity.age;
            player.teleport(world,origin.getX()+25,origin.getY()+5,origin.getZ()+162.5,180,0);player.setVelocity(Vec3d.ZERO);
            for(var specimen:specimens){var nearest=world.getClosestPlayer(specimen.entity,-1);require(nearest!=null,"No native nearest player after departure");double distance=Math.sqrt(specimen.entity.squaredDistanceTo(nearest));require(distance>128,"A specimen still has a player inside the hard-despawn distance");specimen.evidence.put("NearestPlayerDistanceOnDeparture",distance);}
            leftAt=world.getTime();away=true;
        }
        void observeAway() {
            if(!away||nativeComplete)return;awayTicks=world.getTime()-leftAt;
            for(var specimen:specimens){var pos=specimen.entity.getBlockPos();var chunk=new ChunkPos(pos);require(world.getChunkManager().isChunkLoaded(chunk.x,chunk.z),"Fixture chunk unloaded instead of proving despawn");require(world.shouldTickEntity(pos),"Fixture stopped entity-ticking while the player was away");specimen.evidence.put("ChunkLoadedWhileAway",true);specimen.evidence.put("EntityTickingWhileAway",true);}
            if(awayTicks<40)return;
            for(var specimen:specimens){
                if(specimen.control){require(world.getEntity(specimen.uuid)==specimen.entity&&specimen.entity.isAlive(),"Native persistence/livestock control disappeared: "+specimen.label);require(specimen.entity.age-specimen.beforeAge>=20,"Control did not receive enough automatic native ticks while away");specimen.evidence.put("NativeTicksWhileAway",specimen.entity.age-specimen.beforeAge);}
                else{require(specimen.entity.isRemoved(),"Unnamed hostile variant did not automatically despawn: "+specimen.label);require(specimen.entity.getRemovalReason()==Entity.RemovalReason.DISCARDED,"Target was killed or unloaded instead of naturally discarded: "+specimen.label);require(world.getEntity(specimen.uuid)==null,"Removed target remains in native server lookup");specimen.evidence.put("NativeRemovalReason",specimen.entity.getRemovalReason().name());specimen.evidence.put("ServerUuidAbsentWhileAway",true);}
            }
            nativeComplete=true;
        }
        void returnToScene(){if(returned)return;camera();returned=true;}
        void verifyReturned(){
            for(var specimen:specimens){boolean present=world.getEntity(specimen.uuid)!=null;require(present==specimen.control,"Server lookup changed after returning to the still-loaded scene: "+specimen.label);specimen.evidence.put("ServerPresentAfterReturn",present);}
            returnVerified=true;
        }
        void restore(){
            if(restored)return;
            world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(originalMobSpawning,world.getServer());
            player.getServer().getPlayerManager().setSimulationDistance(originalSimulation);
            for(var chunk:addedForced)world.setChunkForced(chunk.x,chunk.z,false);
            player.changeGameMode(originalMode);player.setNoGravity(originalNoGravity);player.getAbilities().flying=originalFlying;player.sendAbilitiesUpdate();player.teleport(world,originalPosition.x,originalPosition.y,originalPosition.z,originalYaw,originalPitch);
            restored=true;
        }
        private static void require(boolean value,String description){if(!value)throw new IllegalStateException(description);}
    }
}
