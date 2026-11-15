package com.tetanusmod.entity;

import net.minecraft.entity.*;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.util.*;

/** Server-authoritative waves, safe physical placement and bounded saved ownership. */
public final class WitherBossEncounter {
    public static final int FIRST_MIN_COOLDOWN=200,FIRST_MAX_COOLDOWN=300;
    public static final int MIN_COOLDOWN=600,MAX_COOLDOWN=1200,MAX_ACTIVE=6,WAVE_SIZE=2,MINION_LIFETIME=2400;
    private static final int SEARCH_DOWN=24,SEARCH_UP=4,PLACEMENT_RETRY=20;
    private WitherBossEncounter(){}
    public static void tick(WitherEntity boss){
        if(!(boss.getWorld() instanceof ServerWorld world)||!boss.isAlive()||!MobVariants.permanent(boss)
                ||boss.getInvulnerableTimer()>0||world.getDifficulty()==Difficulty.PEACEFUL)return;
        var state=(WitherBossState)boss;prune(world,state);
        // Prefer a combatant, but keep the encounter visible to nearby Creative observers too.
        var target=world.getNonSpectatingEntities(ServerPlayerEntity.class,boss.getBoundingBox().expand(64)).stream()
                .filter(ServerPlayerEntity::isAlive)
                .min(Comparator.comparingInt((ServerPlayerEntity p)->p.isCreative()?1:0).thenComparingDouble(boss::squaredDistanceTo)).orElse(null);
        if(target==null)return;
        int cooldown=state.rustruin$minionCooldown();
        if(cooldown<0){state.rustruin$minionCooldown(firstCooldown(boss));return;}
        if(cooldown>1){state.rustruin$minionCooldown(cooldown-1);return;}
        int spawned=spawnWave(boss,target);
        // A temporarily blocked floor should not postpone the entire encounter by another minute.
        state.rustruin$minionCooldown(spawned==0&&state.rustruin$minionLeases().size()<MAX_ACTIVE?PLACEMENT_RETRY:nextCooldown(boss));
    }
    public static int firstCooldown(WitherEntity boss){return FIRST_MIN_COOLDOWN+boss.getRandom().nextInt(FIRST_MAX_COOLDOWN-FIRST_MIN_COOLDOWN+1);}
    public static int nextCooldown(WitherEntity boss){return MIN_COOLDOWN+boss.getRandom().nextInt(MAX_COOLDOWN-MIN_COOLDOWN+1);}
    public static int spawnWave(WitherEntity boss,ServerPlayerEntity target){
        if(!(boss.getWorld() instanceof ServerWorld world)||!boss.isAlive()||!MobVariants.permanent(boss)
                ||target==null||!target.isAlive()||target.isSpectator()||target.getWorld()!=world
                ||world.getDifficulty()==Difficulty.PEACEFUL)return 0;
        var state=(WitherBossState)boss;prune(world,state);
        int budget=Math.min(WAVE_SIZE,MAX_ACTIVE-state.rustruin$minionLeases().size()),spawned=0;
        for(int attempt=0;attempt<32&&spawned<budget;attempt++){
            var minion=WitherMinions.TYPE.create(world);if(minion==null)break;
            double angle=boss.getRandom().nextDouble()*Math.PI*2,radius=3+boss.getRandom().nextDouble()*5;
            int x=MathHelper.floor(target.getX()+Math.cos(angle)*radius),z=MathHelper.floor(target.getZ()+Math.sin(angle)*radius);
            var feet=findNearPlayer(world,minion,x,z,MathHelper.ceil(target.getY()));
            if(feet==null){minion.discard();continue;}
            minion.refreshPositionAndAngles(x+0.5,feet.getY(),z+0.5,boss.getRandom().nextFloat()*360,0);
            minion.initialize(world,world.getLocalDifficulty(feet),SpawnReason.MOB_SUMMONED,null,null);
            minion.assignBoss(boss.getUuid(),world.getTime()+MINION_LIFETIME);
            if(!target.isCreative())minion.setTarget(target);
            if(world.spawnEntity(minion)){
                state.rustruin$minionLeases().put(minion.getUuid(),minion.expiresAt());spawned++;
                world.spawnParticles(net.minecraft.particle.ParticleTypes.SMOKE,minion.getX(),minion.getBodyY(0.5),minion.getZ(),8,0.25,0.3,0.25,0.02);
            }else minion.discard();
        }
        if(spawned>0)world.playSound(null,target.getBlockPos(),net.minecraft.sound.SoundEvents.ENTITY_WITHER_SPAWN,boss.getSoundCategory(),0.4F,1.4F);
        return spawned;
    }
    private static BlockPos findNearPlayer(ServerWorld world,WitherMinionEntity minion,int x,int z,int playerY){
        // Nearby floor first. Starting at the flying boss's height could hide every wave above a cave roof.
        for(int distance=0;distance<=SEARCH_DOWN;distance++){
            var down=new BlockPos(x,playerY-distance,z);if(safePosition(world,minion,down))return down;
            if(distance>0&&distance<=SEARCH_UP){var up=new BlockPos(x,playerY+distance,z);if(safePosition(world,minion,up))return up;}
        }
        return null;
    }
    /** Boss summons may appear in light, but never in fluids, inside blocks or without a floor. */
    public static boolean safePosition(ServerWorld world,WitherMinionEntity minion,BlockPos feet){
        if(!world.isChunkLoaded(feet)||!world.getWorldBorder().contains(feet)||feet.getY()<=world.getBottomY()||feet.getY()+2>=world.getTopY())return false;
        if(!world.getBlockState(feet.down()).isSideSolidFullSquare(world,feet.down(),Direction.UP))return false;
        minion.setPosition(feet.getX()+0.5,feet.getY(),feet.getZ()+0.5);
        var box=minion.getBoundingBox();
        return world.getWorldBorder().contains(box)&&world.isSpaceEmpty(minion,box)&&!world.containsFluid(box)
                &&world.getBlockState(feet).getFluidState().isEmpty()&&world.getBlockState(feet.up()).getFluidState().isEmpty();
    }
    public static void prune(ServerWorld world,WitherBossState state){
        long now=world.getTime();state.rustruin$minionLeases().entrySet().removeIf(entry->{
            if(entry.getValue()<=now)return true;
            var existing=world.getEntity(entry.getKey());
            // A null lookup can be an unloaded entity, so retain its bounded lease until expiry.
            return existing!=null&&(!(existing instanceof WitherMinionEntity)||!existing.isAlive()||existing.isRemoved());
        });
    }
    public static void bossDied(WitherEntity boss){
        if(!(boss.getWorld() instanceof ServerWorld world))return;
        var state=(WitherBossState)boss;
        for(UUID id:new ArrayList<>(state.rustruin$minionLeases().keySet())){
            var minion=world.getEntity(id);if(minion instanceof WitherMinionEntity summoned&&boss.getUuid().equals(summoned.bossUuid()))summoned.discard();
        }
        state.rustruin$minionLeases().clear();
    }
}