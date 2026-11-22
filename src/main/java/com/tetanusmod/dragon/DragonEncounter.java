package com.tetanusmod.dragon;

import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.entity.BossAbilities;
import com.tetanusmod.effect.Infection;
import com.tetanusmod.registry.ModAdvancements;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.util.*;

/** Adds a distinct repeatable encounter to the native End fight and death/XP sequence. */
public final class DragonEncounter {
    public static final int HEALTH=1100,GRAB_COOLDOWN=2400,MAX_HOLD_TICKS=120;
    public static final float GRAB_CHANCE=.15F;
    private DragonEncounter(){}
    public static final class Data {
        public final BossAbilities.State abilities=new BossAbilities.State();
        public final Map<UUID,Float> damage=new HashMap<>();
        public final Set<UUID> closePass=new HashSet<>();
        public long nextGrab;
        public boolean rewarded;
        public boolean interruptedGrab;
        public UUID carried;
        public int heldTicks;
        public double startY;
        public Vec3d lastSafe;
        public PhaseType<?> previousPhase=PhaseType.HOLDING_PATTERN;
        public void write(NbtCompound nbt){
            abilities.write(nbt);
            nbt.putLong("RustRuinDragonNextGrab",nextGrab);nbt.putBoolean("RustRuinDragonRewarded",rewarded);nbt.putBoolean("RustRuinDragonInterruptedGrab",carried!=null||interruptedGrab);
            var list=new NbtList();for(var entry:damage.entrySet()){var row=new NbtCompound();row.putUuid("Player",entry.getKey());row.putFloat("Damage",entry.getValue());list.add(row);}nbt.put("RustRuinDragonDamage",list);
        }
        public void read(NbtCompound nbt,long now){
            abilities.read(nbt);
            nextGrab=Math.max(0,Math.min(now+GRAB_COOLDOWN,nbt.getLong("RustRuinDragonNextGrab")));rewarded=nbt.getBoolean("RustRuinDragonRewarded");interruptedGrab=nbt.getBoolean("RustRuinDragonInterruptedGrab");damage.clear();
            var list=nbt.getList("RustRuinDragonDamage",10);for(int i=0;i<Math.min(1024,list.size());i++){var row=list.getCompound(i);float amount=row.getFloat("Damage");if(row.containsUuid("Player")&&Float.isFinite(amount)&&amount>0)damage.put(row.getUuid("Player"),Math.min(20,amount));}
            carried=null;lastSafe=null;heldTicks=0;closePass.clear();
        }
    }
    public static Data data(EnderDragonEntity dragon){return ((DragonEncounterState)dragon).rustruin$dragonEncounter();}
    public static boolean specialCrystals(List<EndCrystalEntity> crystals){return crystals!=null&&crystals.size()==4&&crystals.stream().allMatch(c->c instanceof TetanizedEndCrystalEntity)&&crystals.stream().map(Entity::getUuid).distinct().count()==4;}
    public static void summoned(EnderDragonEntity dragon,List<EndCrystalEntity> crystals){
        if(!(dragon.getWorld() instanceof ServerWorld world)||!specialCrystals(crystals))return;
        Infection.silently(()->MobVariants.makePermanent(dragon));dragon.setCustomName(Text.translatable("entity.tetanusmod.tetanized_ender_dragon"));
        var center=dragon.getFightOrigin();
        if(world.getRegistryKey().equals(World.END))for(var player:world.getPlayers())if(!player.isSpectator()&&horizontalDistance(player.getPos(),Vec3d.ofCenter(center))<=256*256)ModAdvancements.award(player,"dragon_summoned","summon_dragon");
    }
    private static double horizontalDistance(Vec3d a,Vec3d b){double x=a.x-b.x,z=a.z-b.z;return x*x+z*z;}
    public static void recordDamage(EnderDragonEntity dragon,DamageSource source,float healthBefore){
        var player=source.getAttacker() instanceof ServerPlayerEntity actor?actor:DragonExplosionAttribution.player(source,dragon.getWorld());
        if(!MobVariants.permanent(dragon)||player==null||!Float.isFinite(healthBefore))return;
        float actual=Math.max(0,healthBefore-dragon.getHealth());if(!Float.isFinite(actual)||actual<=0)return;
        var ledger=data(dragon).damage;if(ledger.containsKey(player.getUuid())||ledger.size()<1024)ledger.merge(player.getUuid(),Math.min(20,actual),(old,added)->Math.min(20,old+added));
    }
    public static void deathTick(EnderDragonEntity dragon){
        if(dragon.ticksSinceDeath<199)return;
        finishRewards(dragon);
    }
    /** Native /kill bypasses updatePostDeath entirely; special trophies must still be recoverable. */
    public static void killedInstantly(EnderDragonEntity dragon){finishRewards(dragon);}
    private static void finishRewards(EnderDragonEntity dragon){
        if(!(dragon.getWorld() instanceof ServerWorld world)||!MobVariants.permanent(dragon))return;
        release(dragon);var state=data(dragon);
        if(state.rewarded)return;state.rewarded=true;
        for(var entry:state.damage.entrySet())if(entry.getValue()>=20){var player=world.getServer().getPlayerManager().getPlayer(entry.getKey());if(player!=null)ModAdvancements.award(player,"dragon_defeated","defeat_dragon");else DragonRewardState.get(world.getServer()).add(entry.getKey());}
        // The fountain reopens at native death tick200. Loot above its center falls
        // through the End portal and vanishes to Overworld spawn before players arrive.
        var fight=dragon.getFight();
        // Spawn-egg/operator dragons have no canonical fight origin; their rewards
        // must stay by the actual boss instead of appearing at world0,0.
        var center=fight!=null&&dragon.getUuid().equals(fight.getDragonUuid())?dragon.getFightOrigin():dragon.getBlockPos();
        Vec3d origin=rewardPosition(world,center);
        world.playSound(null,origin.x,origin.y,origin.z,com.tetanusmod.registry.ModSounds.DRAGON_DEFEATED,net.minecraft.sound.SoundCategory.HOSTILE,1,1);
        if(!world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT))return;
        for(var stack:List.of(DragonContent.SWORD.getDefaultStack(),new ItemStack(DragonContent.EGG_ITEM))){
            var item=new ItemEntity(world,origin.x,origin.y,origin.z,stack);
            item.setToDefaultPickupDelay();item.setNeverDespawn();item.setGlowing(true);
            item.setVelocity(0,.12,0);world.spawnEntity(item);
        }
    }
    /** Reward landing is outside the radius3 fountain, with real solid ground beneath it. */
    public static Vec3d rewardPosition(ServerWorld world,BlockPos center){
        for(int radius=7;radius<=15;radius++)for(var direction:Direction.Type.HORIZONTAL){
            int x=center.getX()+direction.getOffsetX()*radius,z=center.getZ()+direction.getOffsetZ()*radius;
            int y=world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z);
            var floor=new BlockPos(x,y-1,z);
            if(y>world.getBottomY()+1&&y<world.getTopY()-3&&world.getWorldBorder().contains(floor)
                    &&world.getBlockState(floor).isSolidBlock(world,floor)&&world.getFluidState(floor).isEmpty()
                    &&world.isAir(floor.up())&&world.isAir(floor.up(2)))return new Vec3d(x+.5,y+.25,z+.5);
        }
        // Flat void fixtures have no island. Still stay outside the fountain column.
        return new Vec3d(center.getX()+7.5,Math.max(world.getBottomY()+4,Math.min(world.getTopY()-4,world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,center.getX()+7,center.getZ())+2)),center.getZ()+.5);
    }
    private static boolean eligible(PlayerEntity player){return player.isAlive()&&!player.isCreative()&&!player.isSpectator()&&!player.hasVehicle()&&!player.isSleeping();}
    public static boolean tryGrab(EnderDragonEntity dragon,ServerPlayerEntity player,float roll){
        if(!(dragon.getWorld() instanceof ServerWorld world)||!MobVariants.permanent(dragon)||!dragon.isAlive()||dragon.getHealth()<=0||!eligible(player)||player.getWorld()!=world)return false;
        var state=data(dragon);if(state.carried!=null||world.getTime()<state.nextGrab||roll>=GRAB_CHANCE||roll<0||!Float.isFinite(roll)||player.squaredDistanceTo(dragon.head)>49)return false;
        Vec3d mouth=mouth(dragon);if(!clear(world,player,mouth)||!player.startRiding(dragon,true))return false;
        state.carried=player.getUuid();state.heldTicks=0;state.startY=mouth.y;state.lastSafe=mouth;state.nextGrab=world.getTime()+GRAB_COOLDOWN;state.previousPhase=dragon.getPhaseManager().getCurrent().getType();dragon.getPhaseManager().setPhase(PhaseType.HOVER);return true;
    }
    public static void tick(EnderDragonEntity dragon){
        if(!(dragon.getWorld() instanceof ServerWorld world)||!MobVariants.permanent(dragon))return;var state=data(dragon);
        // Native DragonPhase persists HOVER; an interrupted grab must not strand a reloaded boss there.
        if(state.interruptedGrab){
            state.interruptedGrab=false;
            for(var passenger:List.copyOf(dragon.getPassengerList()))if(passenger instanceof PlayerEntity)passenger.stopRiding();
            if(dragon.isAlive()&&dragon.getHealth()>0&&dragon.getPhaseManager().getCurrent().getType()==PhaseType.HOVER)dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
        }
        if(!dragon.isAlive()||dragon.getHealth()<=0||dragon.getPhaseManager().getCurrent().getType()==PhaseType.DYING){state.abilities.cancelWindup();release(dragon);return;}
        if(state.carried!=null){
            state.abilities.cancelWindup();
            var player=world.getServer().getPlayerManager().getPlayer(state.carried);
            if(player==null||player.getWorld()!=world||!player.isAlive()||player.getVehicle()!=dragon||player.isCreative()||player.isSpectator()){release(dragon);return;}
            Vec3d point=mouth(dragon);if(!clear(world,player,point)){release(dragon);return;}state.lastSafe=point;
            state.heldTicks++;
            // Head parts lag the body by a native movement tick. Measure the mouth,
            // not the body's rise, so the passenger really reaches ten blocks.
            double remaining=state.startY+10-point.y;
            if(remaining>0){
                double y=Math.min(world.getTopY()-4,dragon.getY()+Math.min(.4,remaining));
                dragon.setPosition(dragon.getX(),y,dragon.getZ());
            }
            dragon.setVelocity(0,0,0);
            if(state.heldTicks>=MAX_HOLD_TICKS||remaining<=.05&&state.heldTicks>=20)release(dragon);
            return;
        }
        // A saved player vehicle must never leave a player stuck after loading an interrupted grab.
        BossAbilities.tick(dragon,state.abilities);
        for(var passenger:List.copyOf(dragon.getPassengerList()))if(passenger instanceof PlayerEntity)passenger.stopRiding();
        var nearby=new HashSet<UUID>();for(var player:world.getPlayers())if(eligible(player)&&player.squaredDistanceTo(dragon.head)<=49){nearby.add(player.getUuid());if(!state.closePass.contains(player.getUuid()))tryGrab(dragon,player,dragon.getRandom().nextFloat());}
        state.closePass.clear();state.closePass.addAll(nearby);
    }
    public static Vec3d mouth(EnderDragonEntity dragon){return dragon.head.getPos().add(0,Math.max(.4,dragon.head.getHeight()*.3),0);}
    public static Vec3d passengerPosition(EnderDragonEntity dragon,Entity passenger){return MobVariants.permanent(dragon)&&passenger instanceof PlayerEntity&&passenger.getVehicle()==dragon?mouth(dragon):null;}
    public static boolean isCarriedBy(Entity passenger,Entity attacker){return attacker instanceof EnderDragonEntity dragon&&MobVariants.permanent(dragon)&&passenger.getVehicle()==dragon;}
    private static boolean clear(ServerWorld world,PlayerEntity player,Vec3d point){
        if(point.y<world.getBottomY()+2||point.y>world.getTopY()-3||!world.getWorldBorder().contains(BlockPos.ofFloored(point)))return false;
        var box=player.getBoundingBox().offset(point.subtract(player.getPos()));if(world.getBlockCollisions(player,box).iterator().hasNext())return false;
        return world.getFluidState(BlockPos.ofFloored(point)).isEmpty()&&world.getFluidState(BlockPos.ofFloored(point).up()).isEmpty();
    }
    public static void release(EnderDragonEntity dragon){
        var state=data(dragon);if(state.carried==null)return;
        if(dragon.getWorld() instanceof ServerWorld world){var player=world.getServer().getPlayerManager().getPlayer(state.carried);if(player==null)for(var passenger:dragon.getPassengerList())if(passenger instanceof ServerPlayerEntity actor&&actor.getUuid().equals(state.carried)){player=actor;break;}if(player!=null&&player.getWorld()==world&&player.getVehicle()==dragon){player.stopRiding();if(state.lastSafe!=null&&clear(world,player,state.lastSafe))player.teleport(world,state.lastSafe.x,state.lastSafe.y,state.lastSafe.z,player.getYaw(),player.getPitch());player.setVelocity(0,-.1,0);player.fallDistance=0;}}
        state.carried=null;state.lastSafe=null;state.heldTicks=0;if(dragon.isAlive()&&dragon.getHealth()>0&&dragon.getPhaseManager().getCurrent().getType()==PhaseType.HOVER)dragon.getPhaseManager().setPhase(state.previousPhase==PhaseType.DYING?PhaseType.HOLDING_PATTERN:state.previousPhase);
    }
    public static void initialize(){
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{if(DragonRewardState.get(server).remove(handler.player.getUuid()))ModAdvancements.award(handler.player,"dragon_defeated","defeat_dragon");});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{if(handler.player.getVehicle() instanceof EnderDragonEntity dragon)release(dragon);});
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{for(var player:List.copyOf(server.getPlayerManager().getPlayerList()))if(player.getVehicle() instanceof EnderDragonEntity dragon)release(dragon);});
    }
}
