package com.tetanusmod.test;

import com.google.gson.GsonBuilder;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.expansion.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Real connected-client walking, blocked-body interaction, and jaw raycast regression. */
public final class GraphicalTrapRegressionAudit {
    private static volatile GraphicalTrapRegressionAudit activeHoldObserver;
    static { ServerTickEvents.END_WORLD_TICK.register(world->{var audit=activeHoldObserver;if(audit!=null)audit.observeServerTick(world);}); }
    private static final BlockPos TRAP=new BlockPos(192,4,64);
    private static final BlockPos BODY_TRAP=TRAP.add(0,0,4),SIDE_TRAP=TRAP.add(0,1,-4),CREATIVE_TRAP=TRAP.add(0,0,8);
    private final List<Map<String,Object>> evidence=new ArrayList<>();
    private final List<Map<String,Object>> holdSamples=new CopyOnWriteArrayList<>();
    private volatile boolean observeHold;
    private Path holdReportPath;
    private final Map<BlockPos,BlockState> replaced=new LinkedHashMap<>();
    private final List<Entity> fixtures=new ArrayList<>();
    private final String viewer=FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
    private UUID playerId,cowId;
    private volatile boolean serverReady=true,restored;
    private volatile Throwable failure;
    private boolean started,complete,restoreQueued;
    private int phase,ticks,phaseTicks;
    private long preparedAt;
    private Vec3d anchor,capturePositionSnapshot,oldPosition;
    private float oldYaw,oldPitch,oldHealth;
    private boolean oldGravity,oldFlying,oldInvulnerable;
    private GameMode oldMode;
    private ServerWorld oldWorld;
    private NbtList oldInventory;
    private NbtCompound oldHunger;
    private int oldSelected;
    private List<StatusEffectInstance> oldEffects;
    private Perspective oldPerspective;
    private int oldFov;

    public void start(MinecraftClient client){
        if(started)throw new IllegalStateException("Trap regression started twice");started=true;
        holdReportPath=client.runDirectory.toPath().resolve("graphical-trap-hold-samples-"+viewer+".json");
        playerId=client.player.getUuid();oldPerspective=client.options.getPerspective();oldFov=client.options.getFov().getValue();
        client.setScreen(null);clearKeys(client);client.options.setPerspective(Perspective.FIRST_PERSON);client.options.getFov().setValue(60);
        server(client,player->{
            oldWorld=player.getServerWorld();oldPosition=player.getPos();oldYaw=player.getYaw();oldPitch=player.getPitch();oldHealth=player.getHealth();
            oldMode=player.interactionManager.getGameMode();oldGravity=player.hasNoGravity();oldFlying=player.getAbilities().flying;oldInvulnerable=player.getAbilities().invulnerable;
            oldInventory=player.getInventory().writeNbt(new NbtList());oldSelected=player.getInventory().selectedSlot;
            oldHunger=new NbtCompound();player.getHungerManager().writeNbt(oldHunger);
            oldEffects=player.getStatusEffects().stream().map(StatusEffectInstance::new).toList();
            var world=player.getServerWorld();
            for(int x=-5;x<=5;x++)for(int z=-7;z<=11;z++)for(int y=-1;y<=5;y++){
                var pos=TRAP.add(x,y,z);if(world.getBlockEntity(pos)!=null)throw new IllegalStateException("Trap fixture would replace an existing block entity at "+pos);
                replaced.put(pos,world.getBlockState(pos));
            }
            for(var pos:replaced.keySet())world.setBlockState(pos,pos.getY()==TRAP.getY()-1?Blocks.STONE.getDefaultState():Blocks.AIR.getDefaultState());
            world.setBlockState(TRAP,ExpansionBlocks.BEAR_TRAP.getDefaultState());
            player.closeHandledScreen();player.clearStatusEffects();player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,2400,0,false,false,false));
            player.changeGameMode(GameMode.SURVIVAL);player.setNoGravity(false);player.getAbilities().flying=false;player.getAbilities().invulnerable=false;player.sendAbilitiesUpdate();
            player.getInventory().clear();player.getInventory().selectedSlot=0;player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ExpansionItems.TRAP_RESET_TOOL));player.setHealth(20);player.getHungerManager().setFoodLevel(16);
            player.teleport(world,TRAP.getX()-1.8,TRAP.getY(),TRAP.getZ()+0.5,-90,25);player.setVelocity(Vec3d.ZERO);preparedAt=world.getTime();
        });
    }
    public boolean tick(MinecraftClient client){
        try{return tickInternal(client);}catch(RuntimeException|Error problem){restore(client);throw problem;}
    }
    private boolean tickInternal(MinecraftClient client){
        if(!started)throw new IllegalStateException("Trap regression not started");if(complete)return true;
        if(failure!=null){restore(client);throw new IllegalStateException("Native trap regression server action failed",failure);}
        if(++ticks>1800)throw new IllegalStateException("Native trap regression timed out at phase "+phase+", crosshair="+client.crosshairTarget);
        if(!serverReady)return false;phaseTicks++;
        switch(phase){
            case 0 -> {
                client.player.getInventory().selectedSlot=0;
                if(client.world.getTime()-preparedAt<140||client.player.squaredDistanceTo(new Vec3d(TRAP.getX()-1.8,TRAP.getY(),TRAP.getZ()+0.5))>0.04
                        ||!client.player.getMainHandStack().isOf(ExpansionItems.TRAP_RESET_TOOL)||client.player.getAbilities().creativeMode)return false;
                screenshot(client,"survival-before");client.player.setYaw(-90);client.player.setPitch(25);client.options.forwardKey.setPressed(true);next();
            }
            case 1 -> {
                if(!trap(client,TRAP).holds(client.player))return false;
                clearKeys(client);server(client,player->{var trap=trap(player,TRAP);require(trap.holds(player),"Actual Survival player did not enter and trigger trap");require(player.getHealth()==17,"Native Survival trap injury was not exactly3HP");capturePositionSnapshot=player.getPos();var captured=trap.createNbt();require(captured.containsUuid("Captured")&&captured.getUuid("Captured").equals(playerId),"Trap did not persist the actual captured player");anchor=new Vec3d(captured.getDouble("AnchorX"),captured.getDouble("AnchorY"),captured.getDouble("AnchorZ"));observeHold=true;activeHoldObserver=this;sample("server-capture",player,trap,0);record("survival-capture",player,TRAP);});
                screenshot(client,"survival-captured");next();client.options.forwardKey.setPressed(true);client.options.jumpKey.setPressed(true);
            }
            case 2 -> {
                sample("client-tick",client.player,trap(client,TRAP),phaseTicks);
                if(phaseTicks<35)return false;clearKeys(client);if(phaseTicks<45)return false;
                server(client,player->{sample("server-final-hold",player,trap(player,TRAP),phaseTicks);saveHoldSamples();assertObservedHold(player);require(player.getHealth()==17,"Closed trap repeated its initial damage");record("survival-native-movement-held",player,TRAP);observeHold=false;activeHoldObserver=null;});
                screenshot(client,"survival-held");aim(client,Vec3d.of(TRAP).add(0.5,0.3,0.5));next();
            }
            case 3 -> {
                if(phaseTicks<20||!(client.crosshairTarget instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK||!hit.getBlockPos().equals(TRAP))return false;
                require(hit.getSide()==Direction.UP,"Actual top reset crosshair did not select the top jaw");screenshot(client,"top-before-reset");client.options.useKey.setPressed(true);next();
            }
            case 4 -> {
                if(phaseTicks>=2)client.options.useKey.setPressed(false);
                if(phaseTicks<4||trap(client,TRAP).getCachedState().get(BearTrapBlock.CLOSED))return false;
                server(client,player->{assertReset(player,TRAP,1);record("native-top-click-reset",player,TRAP);prepareCow(player,BODY_TRAP);player.teleport(player.getServerWorld(),BODY_TRAP.getX()-2,BODY_TRAP.getY(),BODY_TRAP.getZ()+0.5,-90,20);});
                screenshot(client,"top-after-reset");next();
            }
            case 5 -> {
                var cow=client.world.getEntityById(findClientCow(client));
                if(cow==null||!trap(client,BODY_TRAP).holds(cow))return false;
                aim(client,cow.getPos().add(0,0.8,0));
                if(phaseTicks<20||!(client.crosshairTarget instanceof EntityHitResult entityHit)||!entityHit.getEntity().getUuid().equals(cowId))return false;
                screenshot(client,"captive-blocking-jaws");client.options.useKey.setPressed(true);next();
            }
            case 6 -> {
                if(phaseTicks>=2)client.options.useKey.setPressed(false);
                if(phaseTicks<4||trap(client,BODY_TRAP).getCachedState().get(BearTrapBlock.CLOSED))return false;
                server(client,player->{assertReset(player,BODY_TRAP,2);record("native-captive-entity-click-reset",player,BODY_TRAP);prepareCow(player,SIDE_TRAP);player.teleport(player.getServerWorld(),SIDE_TRAP.getX()-2,TRAP.getY(),SIDE_TRAP.getZ()+0.5,-90,0);});
                screenshot(client,"captive-after-reset");client.options.sneakKey.setPressed(true);next();
            }
            case 7 -> {
                if(!client.player.isInSneakingPose())return false;
                aim(client,Vec3d.of(SIDE_TRAP).add(0.5,6.5/16.0,0.5));
                if(phaseTicks<20||!(client.crosshairTarget instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK||!hit.getBlockPos().equals(SIDE_TRAP))return false;
                // The creature obstructs this ray as well. It is removed after
                // naturally closing the raised trap so the actual side is exposed.
                require(hit.getSide()==Direction.WEST,"Actual crouched side ray selected "+hit.getSide());screenshot(client,"side-before-reset");client.options.useKey.setPressed(true);next();
            }
            case 8 -> {
                if(phaseTicks>=2)client.options.useKey.setPressed(false);
                if(phaseTicks<4||trap(client,SIDE_TRAP).getCachedState().get(BearTrapBlock.CLOSED))return false;
                client.options.sneakKey.setPressed(false);
                server(client,player->{assertReset(player,SIDE_TRAP,3);record("native-side-click-reset",player,SIDE_TRAP);player.getServerWorld().setBlockState(CREATIVE_TRAP,ExpansionBlocks.BEAR_TRAP.getDefaultState());player.changeGameMode(GameMode.CREATIVE);player.setNoGravity(false);player.getAbilities().flying=false;player.sendAbilitiesUpdate();player.teleport(player.getServerWorld(),CREATIVE_TRAP.getX()-1.8,CREATIVE_TRAP.getY(),CREATIVE_TRAP.getZ()+0.5,-90,25);});
                screenshot(client,"side-after-reset");next();
            }
            case 9 -> {
                if(!client.player.getAbilities().creativeMode||client.player.getAbilities().flying||client.player.squaredDistanceTo(new Vec3d(CREATIVE_TRAP.getX()-1.8,CREATIVE_TRAP.getY(),CREATIVE_TRAP.getZ()+0.5))>0.04)return false;
                screenshot(client,"creative-before");client.player.setYaw(-90);client.player.setPitch(25);client.options.forwardKey.setPressed(true);next();
            }
            case 10 -> {
                if(client.player.getX()<CREATIVE_TRAP.getX()+1.2)return false;clearKeys(client);
                server(client,player->{require(player.interactionManager.getGameMode()==GameMode.CREATIVE&&!player.getAbilities().flying,"Creative fixture was flying or in wrong mode");require(player.getX()>CREATIVE_TRAP.getX()+1&&trap(player,CREATIVE_TRAP).armed()&&!trap(player,CREATIVE_TRAP).holds(player),"Native Creative walking did not bypass the trap");require(player.getHealth()==17,"Creative crossing lost health");record("creative-native-walking-immune",player,CREATIVE_TRAP);});
                screenshot(client,"creative-bypass");next();
            }
            case 11 -> {
                var report=new LinkedHashMap<String,Object>();report.put("Result","PASS");report.put("ConnectedPlayer",playerId.toString());report.put("NativeForwardAndJumpKeys",true);report.put("NativeUseKeyClicks",3);report.put("DirectTriggerCalls",0);report.put("DirectResetCalls",0);report.put("Evidence",evidence);
                try{Files.writeString(client.runDirectory.toPath().resolve("graphical-trap-regression-"+viewer+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}catch(Exception problem){throw new IllegalStateException(problem);}
                restore(client);next();
            }
            case 12 -> {if(!restored)return false;complete=true;return true;}
            default -> throw new IllegalStateException("Unknown native trap phase "+phase);
        }
        return false;
    }
    private void observeServerTick(ServerWorld world){
        if(!observeHold||world!=oldWorld)return;
        var player=world.getServer().getPlayerManager().getPlayer(playerId);
        if(player==null||!(world.getBlockEntity(TRAP) instanceof BearTrapBlockEntity trap))return;
        sample("server-end-world-tick",player,trap,world.getTime());
    }
    private void sample(String source,net.minecraft.entity.player.PlayerEntity player,BearTrapBlockEntity trap,long tick){
        var nbt=trap.createNbt();var nativeAnchor=new Vec3d(nbt.getDouble("AnchorX"),nbt.getDouble("AnchorY"),nbt.getDouble("AnchorZ"));
        var row=new LinkedHashMap<String,Object>();row.put("Source",source);row.put("Tick",tick);row.put("Position",player.getPos().toString());row.put("Velocity",player.getVelocity().toString());row.put("OnGround",player.isOnGround());row.put("Holds",trap.holds(player));row.put("Closed",trap.getCachedState().get(BearTrapBlock.CLOSED));row.put("HasCapturedUuid",nbt.containsUuid("Captured"));row.put("NativeAnchor",nativeAnchor.toString());row.put("DistanceSquaredToNativeAnchor",player.squaredDistanceTo(nativeAnchor));row.put("CapturedAnchor",anchor==null?"NONE":anchor.toString());row.put("DistanceSquaredToCapturedAnchor",anchor==null?-1:player.squaredDistanceTo(anchor));row.put("SnapshotAnchor",capturePositionSnapshot==null?"NONE":capturePositionSnapshot.toString());row.put("DistanceSquaredToSnapshot",capturePositionSnapshot==null?-1:player.squaredDistanceTo(capturePositionSnapshot));holdSamples.add(row);
        if(!source.equals("client-tick"))org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_TRAP_HOLD_SAMPLE {}",row);
    }
    private void assertObservedHold(ServerPlayerEntity player){
        // Server tasks can run between a movement packet and the block-entity
        // correction. Judge restraint after real world ticks, against the trap's
        // captured anchor, rather than a late snapshot of the player's position.
        var nativeTicks=holdSamples.stream().filter(row->row.get("Source").equals("server-end-world-tick")).toList();
        require(nativeTicks.size()>=30,"Forward/jump test observed fewer than30 authoritative world ticks: "+nativeTicks.size());
        for(var row:nativeTicks){
            require(Boolean.TRUE.equals(row.get("HasCapturedUuid"))&&Boolean.TRUE.equals(row.get("Holds"))&&Boolean.TRUE.equals(row.get("Closed")),"Native trap lost capture during actual forward/jump input: "+row);
            require(((Number)row.get("DistanceSquaredToCapturedAnchor")).doubleValue()<0.001,"Actual forward/jump input escaped the captured anchor after a native world tick: "+row);
            require(row.get("NativeAnchor").equals(anchor.toString()),"Trap anchor changed while restraining the player: "+row);
        }
        require(trap(player,TRAP).holds(player),"Trap lost capture between final native sample and assertion");
    }
    private void saveHoldSamples(){
        var report=new LinkedHashMap<String,Object>();report.put("Samples",holdSamples);
        report.put("MaxServerDistanceSquaredToNativeAnchor",holdSamples.stream().filter(row->row.get("Source").equals("server-end-world-tick")&&Boolean.TRUE.equals(row.get("HasCapturedUuid"))).mapToDouble(row->((Number)row.get("DistanceSquaredToNativeAnchor")).doubleValue()).max().orElse(-1));
        report.put("MaxClientDistanceSquaredToNativeAnchor",holdSamples.stream().filter(row->row.get("Source").equals("client-tick")).mapToDouble(row->((Number)row.get("DistanceSquaredToNativeAnchor")).doubleValue()).max().orElse(-1));
        try{Files.writeString(holdReportPath,new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}catch(Exception problem){throw new IllegalStateException("Cannot save native trap hold diagnostics",problem);}
    }
    private int findClientCow(MinecraftClient client){for(var entity:client.world.getEntities())if(entity.getUuid().equals(cowId))return entity.getId();return -1;}
    private void prepareCow(ServerPlayerEntity player,BlockPos pos){
        var world=player.getServerWorld();world.setBlockState(pos.down(),Blocks.STONE.getDefaultState());world.setBlockState(pos,ExpansionBlocks.BEAR_TRAP.getDefaultState());
        var cow=EntityType.COW.create(world);require(cow!=null,"Cannot create native cow fixture");cow.setAiDisabled(true);cow.setNoGravity(true);
        cow.refreshPositionAndAngles(pos.getX()+0.5,pos.getY(),pos.getZ()-0.9,0,0);world.spawnEntity(cow);fixtures.add(cow);cow.move(MovementType.SELF,new Vec3d(0,0,1.4));
        require(trap(player,pos).holds(cow),"Native cow movement did not close trap");cowId=cow.getUuid();
        if(pos.equals(SIDE_TRAP))cow.discard();
    }
    private void assertReset(ServerPlayerEntity player,BlockPos pos,int expectedDamage){
        var trap=trap(player,pos);require(!trap.getCachedState().get(BearTrapBlock.CLOSED)&&!trap.armed(),"Native click did not open trap with a safe grace period");
        require(player.getMainHandStack().isOf(ExpansionItems.TRAP_RESET_TOOL)&&player.getMainHandStack().getDamage()==expectedDamage,"Native click charged wrong tool durability");
        require(player.getServerWorld().getBlockState(pos.down()).isOf(Blocks.STONE),"Reset changed the support block");
        var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id("trap_reset"));require(player.getAdvancementTracker().getProgress(advancement).isDone(),"Native click did not earn reset advancement");
    }
    private void record(String action,ServerPlayerEntity player,BlockPos pos){
        var row=new LinkedHashMap<String,Object>();row.put("Action",action);row.put("Mode",player.interactionManager.getGameMode().getName());row.put("PlayerPosition",player.getPos().toString());row.put("Health",player.getHealth());row.put("TrapPosition",pos.toShortString());row.put("Closed",trap(player,pos).getCachedState().get(BearTrapBlock.CLOSED));row.put("HoldsPlayer",trap(player,pos).holds(player));row.put("ToolDamage",player.getMainHandStack().getDamage());evidence.add(row);
        org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_TRAP_REGRESSION_PASS {}",action);
    }
    private void server(MinecraftClient client,Consumer<ServerPlayerEntity> action){
        require(serverReady,"Overlapping native trap server actions");serverReady=false;client.getServer().execute(()->{try{var player=client.getServer().getPlayerManager().getPlayer(playerId);require(player!=null,"Connected trap actor disappeared");action.accept(player);}catch(Throwable problem){failure=problem;}finally{serverReady=true;}});
    }
    private static BearTrapBlockEntity trap(ServerPlayerEntity player,BlockPos pos){var be=player.getServerWorld().getBlockEntity(pos);require(be instanceof BearTrapBlockEntity,"Server trap missing at "+pos);return (BearTrapBlockEntity)be;}
    private static BearTrapBlockEntity trap(MinecraftClient client,BlockPos pos){var be=client.world.getBlockEntity(pos);require(be instanceof BearTrapBlockEntity,"Client trap missing at "+pos);return (BearTrapBlockEntity)be;}
    private static void aim(MinecraftClient client,Vec3d target){var delta=target.subtract(client.player.getEyePos());client.player.setYaw((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));client.player.setPitch((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));}
    private static void clearKeys(MinecraftClient client){client.options.forwardKey.setPressed(false);client.options.backKey.setPressed(false);client.options.leftKey.setPressed(false);client.options.rightKey.setPressed(false);client.options.jumpKey.setPressed(false);client.options.sneakKey.setPressed(false);client.options.useKey.setPressed(false);}
    private void next(){phase++;phaseTicks=0;}
    private void screenshot(MinecraftClient client,String label){
        try(var frame=ScreenshotRecorder.takeScreenshot(client.getFramebuffer())){var folder=client.runDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);frame.writeTo(folder.resolve("rust-and-ruin-"+viewer+"-trap-"+label+".png"));}catch(Exception problem){throw new IllegalStateException("Cannot capture native trap regression",problem);}
    }
    private void restore(MinecraftClient client){
        observeHold=false;if(activeHoldObserver==this)activeHoldObserver=null;if(!holdSamples.isEmpty())saveHoldSamples();
        clearKeys(client);client.options.setPerspective(oldPerspective);client.options.getFov().setValue(oldFov);if(client.player!=null)client.player.getInventory().selectedSlot=oldSelected;
        if(restoreQueued)return;restoreQueued=true;
        if(client.getServer()==null){restored=true;return;}
        client.getServer().execute(()->{try{
            fixtures.forEach(Entity::discard);
            var player=client.getServer().getPlayerManager().getPlayer(playerId);
            if(player!=null&&oldWorld!=null){
                for(var entry:replaced.entrySet())oldWorld.setBlockState(entry.getKey(),entry.getValue());
                player.closeHandledScreen();player.clearStatusEffects();if(oldEffects!=null)for(var effect:oldEffects)player.addStatusEffect(new StatusEffectInstance(effect));
                player.changeGameMode(oldMode);player.setNoGravity(oldGravity);player.getAbilities().flying=oldFlying;player.getAbilities().invulnerable=oldInvulnerable;player.sendAbilitiesUpdate();
                if(oldInventory!=null)player.getInventory().readNbt(oldInventory);player.getInventory().selectedSlot=oldSelected;if(oldHunger!=null)player.getHungerManager().readNbt(oldHunger);
                player.setHealth(Math.min(oldHealth,player.getMaxHealth()));player.teleport(oldWorld,oldPosition.x,oldPosition.y,oldPosition.z,oldYaw,oldPitch);player.playerScreenHandler.syncState();
            }
        }catch(Throwable problem){failure=problem;}finally{restored=true;}});
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
