package com.tetanusmod.test;

import com.google.gson.GsonBuilder;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.expansion.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.effect.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.processor.JigsawReplacementStructureProcessor;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Manual native-template visual inspection only; this makes no natural village/camp frequency claims. */
public final class GraphicalClinicFacingAudit {
    private static final BlockPos ORIGIN=new BlockPos(128,3,112);
    private static final String[] THEMES={"plains","desert","savanna","snowy","taiga"};
    private record Camera(Vec3d position,float yaw,float pitch){}
    private final List<Map<String,Object>> evidence=new ArrayList<>();
    private final String viewer=FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
    private UUID playerId;
    private volatile Fixture fixture;
    private CompletableFuture<?> pending;
    private Camera camera;
    private int phase,theme,ticks,phaseTicks,settled;
    private boolean started,finished,restoreQueued;
    private Perspective oldPerspective;
    private int oldFov;

    public void start(MinecraftClient client){
        require(!started&&client.getServer()!=null&&client.player!=null,"Clinic visual audit needs one integrated graphical player and can start only once");started=true;
        playerId=client.player.getUuid();oldPerspective=client.options.getPerspective();oldFov=client.options.getFov().getValue();client.setScreen(null);release(client);client.options.setPerspective(Perspective.FIRST_PERSON);client.options.getFov().setValue(75);
        pending=server(client,p->{fixture=new Fixture(p);return fixture;});
    }
    public boolean tick(MinecraftClient client){
        try{return tickInternal(client);}catch(RuntimeException|Error problem){restore(client);throw problem;}
    }
    @SuppressWarnings("unchecked")
    private boolean tickInternal(MinecraftClient client){
        require(started,"Clinic visual audit not started");if(finished)return true;require(++ticks<1800,"Clinic facing visual audit timed out at theme="+theme+" phase="+phase);phaseTicks++;
        switch(phase){
            case 0->{if(pending.isDone()){pending.join();next(1);}}
            case 1->{pending=server(client,p->{fixture.place(p,THEMES[theme]);return fixture.camera(p,false);});next(2);}
            case 2->{if(pending.isDone()){camera=(Camera)pending.join();settled=0;next(3);}}
            case 3->{if(visible(client)&&++settled>=30){screenshot(client,"interior");next(4);}else if(!visible(client))settled=0;}
            case 4->{if(phaseTicks>=15){pending=server(client,p->fixture.camera(p,true));next(5);}}
            case 5->{if(pending.isDone()){camera=(Camera)pending.join();settled=0;next(6);}}
            case 6->{if(visible(client)&&++settled>=30){screenshot(client,"cabinet-front");pending=server(client,p->fixture.proof(THEMES[theme]));next(7);}else if(!visible(client))settled=0;}
            case 7->{if(pending.isDone()){var row=new LinkedHashMap<>((Map<String,Object>)pending.join());row.put("InteriorPNG",file("interior"));row.put("CabinetFrontPNG",file("cabinet-front"));evidence.add(row);next(8);}}
            case 8->{if(phaseTicks>=15){if(++theme<THEMES.length)next(1);else{restore(client);next(9);}}}
            case 9->{if(pending.isDone()){pending.join();if(fixture.clientRestored(client)){writeReport();finished=true;return true;}}require(phaseTicks<250,"Clinic fixture restoration did not reach actual client");}
            default->throw new IllegalStateException("Unknown clinic visual phase "+phase);
        }
        return false;
    }
    private boolean visible(MinecraftClient client){
        if(client.player==null||client.world==null||camera==null)return false;
        client.player.setYaw(camera.yaw());client.player.setPitch(camera.pitch());
        var cabinet=client.world.getBlockState(ORIGIN.add(6,2,6));
        return client.player.isSpectator()&&client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)&&client.player.squaredDistanceTo(camera.position())<0.02
                &&cabinet.isOf(ExpansionBlocks.MEDIC_CABINET)&&cabinet.get(DoorBlock.FACING)==Direction.WEST&&client.world.getBlockState(ORIGIN.add(4,1,5)).isFullCube(client.world,ORIGIN.add(4,1,5));
    }
    private <T> CompletableFuture<T> server(MinecraftClient client,Function<ServerPlayerEntity,T> action){var future=new CompletableFuture<T>();client.getServer().execute(()->{try{var player=client.getServer().getPlayerManager().getPlayer(playerId);require(player!=null,"Clinic visual player disappeared");future.complete(action.apply(player));}catch(Throwable problem){future.completeExceptionally(problem);}});return future;}
    private void next(int value){phase=value;phaseTicks=0;}
    private String file(String view){return "rust-and-ruin-"+viewer+"-clinic-facing-"+THEMES[theme]+"-"+view+".png";}
    private void screenshot(MinecraftClient client,String view){try(var frame=ScreenshotRecorder.takeScreenshot(client.getFramebuffer())){var folder=client.runDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);frame.writeTo(folder.resolve(file(view)));}catch(Exception problem){throw new IllegalStateException("Cannot save native clinic facing image",problem);}}
    private void restore(MinecraftClient client){
        release(client);client.options.setPerspective(oldPerspective);client.options.getFov().setValue(oldFov);if(restoreQueued)return;restoreQueued=true;
        if(fixture!=null&&client.getServer()!=null)pending=server(client,p->{fixture.restore(p);return null;});
    }
    private void writeReport(){
        require(evidence.size()==5,"Not all five clinic themes were visually captured");var report=new LinkedHashMap<String,Object>();report.put("Result","PASS");report.put("ActualGraphicalClient",true);report.put("ManualNativeTemplatePlacements",5);report.put("NaturalWorldgenClaims",0);report.put("IndependentOfNaturalMedicCampReport",true);report.put("Camera","Native Spectator camera with actual Night Vision, to inspect furniture without bed collision");report.put("TemplateEntitiesIgnored",true);report.put("ExpectedCabinetFacing","west, into the aisle");report.put("OriginalPlayerModePhysicsAndBlocksRestored",true);report.put("Themes",evidence);
        try{Files.writeString(Path.of("graphical-clinic-facing-"+viewer+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}catch(Exception problem){throw new IllegalStateException("Cannot save clinic facing report",problem);}
    }
    private static void release(MinecraftClient client){client.options.useKey.setPressed(false);client.options.forwardKey.setPressed(false);client.options.backKey.setPressed(false);client.options.leftKey.setPressed(false);client.options.rightKey.setPressed(false);client.options.jumpKey.setPressed(false);client.options.sneakKey.setPressed(false);}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}

    private static final class Fixture {
        private final ServerWorld world;
        private final Vec3d oldPosition,oldVelocity;
        private final float oldYaw,oldPitch,oldFall;
        private final GameMode oldMode;
        private final boolean oldGravity,oldFlying,oldInvulnerable;
        private final StatusEffectInstance oldNightVision;
        private final Map<BlockPos,BlockState> replaced=new LinkedHashMap<>();
        private boolean restored;
        Fixture(ServerPlayerEntity player){
            world=player.getServerWorld();oldPosition=player.getPos();oldVelocity=player.getVelocity();oldYaw=player.getYaw();oldPitch=player.getPitch();oldFall=player.fallDistance;oldMode=player.interactionManager.getGameMode();oldGravity=player.hasNoGravity();oldFlying=player.getAbilities().flying;oldInvulnerable=player.getAbilities().invulnerable;var vision=player.getStatusEffect(StatusEffects.NIGHT_VISION);oldNightVision=vision==null?null:new StatusEffectInstance(vision);
            for(int x=0;x<9;x++)for(int y=0;y<11;y++)for(int z=0;z<13;z++){var pos=ORIGIN.add(x,y,z);require(world.getBlockEntity(pos)==null,"Manual clinic fixture would replace an existing block entity at "+pos);replaced.put(pos,world.getBlockState(pos));}
        }
        void place(ServerPlayerEntity player,String biome){
            clearFixtureContainers();var template=world.getStructureTemplateManager().getTemplate(TetanusMod.id("village/"+biome+"_clinic")).orElseThrow();
            require(template.place(world,ORIGIN,ORIGIN,new StructurePlacementData().setIgnoreEntities(true).addProcessor(JigsawReplacementStructureProcessor.INSTANCE),world.random,3),"Manual native clinic template did not place: "+biome);
            proof(biome);player.closeHandledScreen();player.changeGameMode(GameMode.SPECTATOR);player.setNoGravity(true);player.getAbilities().flying=true;player.sendAbilitiesUpdate();player.fallDistance=0;player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,2400,0,false,false,false));
        }
        Camera camera(ServerPlayerEntity player,boolean close){
            var point=close?new Vec3d(3.6,2,6.5):new Vec3d(4.3,2,4.4);point=point.add(ORIGIN.getX(),ORIGIN.getY(),ORIGIN.getZ());
            var target=(close?new Vec3d(6.2,3,6.5):new Vec3d(4.6,3,8.1)).add(ORIGIN.getX(),ORIGIN.getY(),ORIGIN.getZ());var delta=target.subtract(point.add(0,player.getStandingEyeHeight(),0));float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z)));
            player.teleport(world,point.x,point.y,point.z,yaw,pitch);player.setVelocity(Vec3d.ZERO);return new Camera(point,yaw,pitch);
        }
        Map<String,Object> proof(String biome){
            var pos=ORIGIN.add(6,2,6);var lower=world.getBlockState(pos);var upper=world.getBlockState(pos.up());
            require(lower.isOf(ExpansionBlocks.MEDIC_CABINET)&&upper.isOf(ExpansionBlocks.MEDIC_CABINET)&&lower.get(DoorBlock.FACING)==Direction.WEST&&upper.get(DoorBlock.FACING)==Direction.WEST,"Clinic cabinet is not aisle-facing in "+biome);
            require(lower.get(DoorBlock.HALF)==DoubleBlockHalf.LOWER&&upper.get(DoorBlock.HALF)==DoubleBlockHalf.UPPER&&lower.get(MedicineCabinetBlock.COMPLETE)&&upper.get(MedicineCabinetBlock.COMPLETE),"Clinic cabinet halves invalid in "+biome);
            for(int y=0;y<2;y++){var part=pos.up(y);require(world.getBlockState(part.west()).isAir(),"Cabinet aisle blocked in "+biome);require(world.getBlockState(part.east()).isFullCube(world,part.east()),"Cabinet back not against wall in "+biome);}
            var row=new LinkedHashMap<String,Object>();row.put("Theme",biome);row.put("NativeTemplate","tetanusmod:village/"+biome+"_clinic");row.put("CabinetLower",lower.toString());row.put("CabinetUpper",upper.toString());row.put("FrontAisleFree",true);row.put("BackAgainstWall",true);return row;
        }
        private void clearFixtureContainers(){
            // These manually placed visual chests are never looted. Avoid generating/scattering
            // their lazy loot during theme replacement or cleanup.
            for(var pos:replaced.keySet()){var blockEntity=world.getBlockEntity(pos);if(blockEntity instanceof LootableContainerBlockEntity loot)loot.setLootTable(null,0);if(blockEntity instanceof Inventory inventory)inventory.clear();}
        }
        void restore(ServerPlayerEntity player){
            if(restored)return;restored=true;clearFixtureContainers();for(var entry:replaced.entrySet())world.setBlockState(entry.getKey(),entry.getValue(),18);player.removeStatusEffect(StatusEffects.NIGHT_VISION);if(oldNightVision!=null)player.addStatusEffect(new StatusEffectInstance(oldNightVision));player.changeGameMode(oldMode);player.setNoGravity(oldGravity);player.getAbilities().flying=oldFlying;player.getAbilities().invulnerable=oldInvulnerable;player.sendAbilitiesUpdate();player.fallDistance=oldFall;player.teleport(world,oldPosition.x,oldPosition.y,oldPosition.z,oldYaw,oldPitch);player.setVelocity(oldVelocity);
            require(player.interactionManager.getGameMode()==oldMode&&player.hasNoGravity()==oldGravity,"Clinic fixture changed the saved player mode/physics");for(var entry:replaced.entrySet())require(world.getBlockState(entry.getKey()).equals(entry.getValue()),"Clinic fixture failed to restore block "+entry.getKey());
        }
        boolean clientRestored(MinecraftClient client){return client.player!=null&&client.player.isSpectator()==(oldMode==GameMode.SPECTATOR)&&client.player.getAbilities().creativeMode==(oldMode==GameMode.CREATIVE)&&client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)==(oldNightVision!=null)&&client.player.squaredDistanceTo(oldPosition)<0.1;}
    }
}