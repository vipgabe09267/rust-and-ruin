package com.tetanusmod.test;

import com.tetanusmod.entity.*;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import java.util.*;

/** Actual world-ticked Wither AI and native death drops for the0.1.1patch. */
public final class WitherPatchGameTests implements FabricGameTest {
    private static final String SPACE="tetanusmod:test/camp_empty";
    @GameTest(templateName=SPACE,batchId="wither_patch_stars")
    public void nativeDeathReplacesOnlyTheSpecialWitherStarAndPreservesCovetedLifetime(TestContext ctx){
        var world=ctx.getWorld();var at=ctx.getAbsolute(new Vec3d(12.5,5,12.5));
        for(boolean special:new boolean[]{false,true}){
            var boss=EntityType.WITHER.create(world);boss.setAiDisabled(true);boss.setPosition(at);world.spawnEntity(boss);if(special)MobVariants.makePermanent(boss);boss.damage(world.getDamageSources().genericKill(),10000);
            var drops=world.getEntitiesByClass(ItemEntity.class,new Box(at,at).expand(8),e->true);int normal=0,infected=0;
            for(var item:drops){if(item.getStack().isOf(Items.NETHER_STAR)||item.getStack().isOf(ModItems.TETANIZED_WITHER_STAR)){ctx.assertTrue(item.writeNbt(new NbtCompound()).getShort("Age")==-6000,"Star lost the native coveted-item lifetime");if(item.getStack().isOf(Items.NETHER_STAR))normal+=item.getStack().getCount();else infected+=item.getStack().getCount();}}
            ctx.assertTrue(normal==(special?0:1)&&infected==(special?1:0),"Native star replacement changed ordinary drops or duplicated the special reward: normal="+normal+" special="+infected);drops.forEach(Entity::discard);boss.discard();
        }ctx.complete();
    }
    @GameTest(templateName=SPACE,batchId="wither_patch_minion_health")
    public void newMinionsHaveTwentyHealthAndSavedTenHealthMinionsMigrateWithoutFullHealing(TestContext ctx){
        var minion=WitherMinions.TYPE.create(ctx.getWorld());ctx.assertTrue(minion.getMaxHealth()==20&&minion.getHealth()==20,"New minion does not have20health");minion.assignBoss(UUID.randomUUID(),ctx.getWorld().getTime()+1700);var maximum=minion.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);maximum.setBaseValue(10);minion.setHealth(5);var saved=minion.writeNbt(new NbtCompound());var loaded=WitherMinions.TYPE.create(ctx.getWorld());
        try{loaded.readNbt(saved);ctx.assertTrue(loaded.getMaxHealth()==20&&loaded.getHealth()==10,"Old half-health minion did not migrate to20health proportionally");ctx.assertTrue(loaded.expiresAt()==minion.expiresAt()&&loaded.bossUuid().equals(minion.bossUuid())&&!loaded.canPickUpLoot()&&!loaded.isPersistent(),"Health migration changed lifetime, ownership, looting or despawn behavior");}finally{minion.discard();loaded.discard();}ctx.complete();
    }
    @GameTest(templateName=SPACE,batchId="wither_patch_native_creative",tickLimit=980)
    public void nativeWorldTicksSummonVisibleWavesForCreativeObserversWithoutTargetingThem(TestContext ctx){
        arena(ctx);var player=player(ctx,true);var boss=boss(ctx,12);var state=(WitherBossState)boss;var first=new ArrayList<WitherMinionEntity>();int[] observed={-1,-1,-1};
        ctx.runAtTick(190,()->observed[0]=state.rustruin$minionLeases().size());
        ctx.runAtTick(320,()->{for(var id:state.rustruin$minionLeases().keySet())if(ctx.getWorld().getEntity(id) instanceof WitherMinionEntity m)first.add(m);observed[1]=state.rustruin$minionCooldown();state.rustruin$minionCooldown(600);});
        ctx.runAtTick(919,()->observed[2]=state.rustruin$minionLeases().size());
        ctx.runAtTick(930,()->{try{
            ctx.assertTrue(observed[0]==0,"First minions appeared before the10second post-shield minimum");ctx.assertTrue(first.size()==2,"Live native mobTick did not summon its first wave for a Creative observer");ctx.assertTrue(observed[1]>=480&&observed[1]<=1200,"Successful first wave did not schedule a30-60second next interval");ctx.assertTrue(observed[2]==2&&state.rustruin$minionLeases().size()==4,"Native AI skipped, doubled, or prematurely repeated its30second minimum wave");
            for(var id:state.rustruin$minionLeases().keySet()){var minion=(WitherMinionEntity)ctx.getWorld().getEntity(id);ctx.assertTrue(minion!=null&&minion.isAlive()&&minion.getMaxHealth()==20,"Native wave minion missing or wrong health");ctx.assertTrue(minion.getTarget()!=player,"Summon attacked a Creative observer");ctx.assertTrue(minion.getY()<ctx.getAbsolutePos(new BlockPos(0,8,0)).getY(),"Flying boss hid its minions above the player's roof");}
        }finally{clean(boss,player);}ctx.complete();});
    }
    @GameTest(templateName=SPACE,batchId="wither_patch_native_survival",tickLimit=360)
    public void nativeWorldTicksPlaceCombatMinionsInsideTheSurvivalArenaBelowAFlyingBoss(TestContext ctx){
        arena(ctx);var player=player(ctx,false);var boss=boss(ctx,12);var state=(WitherBossState)boss;
        ctx.runAtTick(330,()->{try{
            ctx.assertTrue(!player.isCreative()&&!player.isSpectator(),"Survival fixture is not a real combatant");ctx.assertTrue(state.rustruin$minionLeases().size()==2,"Native Survival encounter never summoned its first wave");
            for(var id:state.rustruin$minionLeases().keySet()){var minion=(WitherMinionEntity)ctx.getWorld().getEntity(id);ctx.assertTrue(minion!=null&&minion.isAlive()&&minion.getMaxHealth()==20&&minion.getMainHandStack().isOf(Items.IRON_SWORD),"Native Survival minion lost its health or weapon");ctx.assertTrue(minion.getY()<ctx.getAbsolutePos(new BlockPos(0,8,0)).getY()&&minion.squaredDistanceTo(player)<256,"Minion spawned above the arena roof or away from the combatant");ctx.assertTrue(minion.getTarget()==player,"Native summoned minion does not engage its Survival target");}
        }finally{clean(boss,player);}ctx.complete();});
    }
    @GameTest(templateName=SPACE,batchId="wither_patch_blocked_retry")
    public void nativeTicksRetryBlockedPhysicalPlacementWithoutRestartingTheMinuteLongCycle(TestContext ctx){
        arena(ctx);var player=player(ctx,true);var boss=boss(ctx,12);boss.setInvulTimer(0);var state=(WitherBossState)boss;
        try{
            for(int x=2;x<=23;x++)for(int z=2;z<=23;z++)for(int y=-24;y<=6;y++)ctx.setBlockState(x,y,z,Blocks.BEDROCK);state.rustruin$minionCooldown(1);boss.tick();ctx.assertTrue(state.rustruin$minionLeases().isEmpty()&&state.rustruin$minionCooldown()==20,"Blocked native wave restarted a30-60second wait or spawned inside a block");
            arena(ctx);for(int i=0;i<20;i++)boss.tick();ctx.assertTrue(state.rustruin$minionLeases().size()==2,"Native wave did not recover after its floor became available");
        }finally{clean(boss,player);}ctx.complete();
    }
    private static void arena(TestContext ctx){for(int x=1;x<=25;x++)for(int z=1;z<=25;z++)for(int y=1;y<=8;y++)ctx.setBlockState(x,y,z,y==1||y==8||x==1||x==25||z==1||z==25?Blocks.BEDROCK:Blocks.AIR);}
    private static ServerPlayerEntity player(TestContext ctx,boolean creative){var world=ctx.getWorld();var p=new ServerPlayerEntity(world.getServer(),world,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"wither-native"));world.getServer().getPlayerManager().onPlayerConnect(new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND),p);p.changeGameMode(creative?GameMode.CREATIVE:GameMode.SURVIVAL);p.setNoGravity(true);p.setPosition(ctx.getAbsolute(new Vec3d(12.5,2,12.5)));p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,20000,4));p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,20000,4));return p;}
    private static WitherEntity boss(TestContext ctx,int y){var b=EntityType.WITHER.create(ctx.getWorld());b.setPosition(ctx.getAbsolute(new Vec3d(12.5,y,12.5)));b.setNoGravity(true);b.setInvulTimer(5);ctx.getWorld().spawnEntity(b);MobVariants.makePermanent(b);return b;}
    private static void clean(WitherEntity boss,ServerPlayerEntity player){WitherBossEncounter.bossDied(boss);boss.discard();player.getServer().getPlayerManager().remove(player);player.discard();}
}