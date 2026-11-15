package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.dragon.*;
import com.tetanusmod.entity.MobVariants;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.*;
import net.minecraft.entity.boss.dragon.*;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import net.minecraft.world.gen.feature.EndSpikeFeature;
import java.util.*;

/** Production regressions for portal-lost loot, pillar identity and the boss HUD. */
public class DragonPatchGameTests implements FabricGameTest {
    private static final String SPACE="tetanusmod:test/camp_empty";

    @GameTest(templateName=SPACE,batchId="dragon_patch",tickLimit=360)
    public void actualDeathLeavesRecoverableRewardsOutsidePortalAfterTwelveSeconds(TestContext ctx) {
        var world=ctx.getWorld();var center=ctx.getAbsolutePos(new BlockPos(22,7,22));
        for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++){
            world.setBlockState(center.add(x,-1,z),Blocks.OBSIDIAN.getDefaultState());
            for(int y=0;y<=5;y++)world.setBlockState(center.add(x,y,z),Blocks.AIR.getDefaultState());
        }
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)world.setBlockState(center.add(x,0,z),Blocks.END_PORTAL.getDefaultState());
        var dragon=EntityType.ENDER_DRAGON.create(world);
        dragon.setFightOrigin(new BlockPos(center.getX(),0,center.getZ()));
        dragon.setPosition(Vec3d.ofCenter(center.up(8)));dragon.getPhaseManager().setPhase(PhaseType.HOVER);
        MobVariants.makePermanent(dragon);dragon.setHealth(0);dragon.ticksSinceDeath=148;world.spawnEntity(dragon);
        var area=new Box(center).expand(15);
        ctx.runAtTick(240,()->{
            var rewards=world.getEntitiesByClass(ItemEntity.class,area,e->e.getStack().isOf(DragonContent.SWORD)||e.getStack().isOf(DragonContent.EGG_ITEM));
            try{
                ctx.assertTrue(dragon.isRemoved()&&DragonEncounter.data(dragon).rewarded,"Native dragon death did not finish and record its reward");
                ctx.assertTrue(rewards.size()==2,"Sword/egg fell through the portal or failed to spawn after actual death: "+rewards.size());
                ctx.assertTrue(rewards.stream().filter(e->e.getStack().isOf(DragonContent.SWORD)).count()==1&&rewards.stream().filter(e->e.getStack().isOf(DragonContent.EGG_ITEM)).count()==1,"Reward pair contains wrong items");
                for(var reward:rewards){
                    ctx.assertTrue(!reward.cannotPickup()&&reward.isOnGround(),"Reward was not collectable on the island floor");
                    ctx.assertTrue(Math.abs(reward.getX()-center.getX())>=5||Math.abs(reward.getZ()-center.getZ())>=5,"Reward remained above the fountain");
                    ctx.assertTrue(reward.writeNbt(new NbtCompound()).getShort("Age")==-32768,"Unique trophy loot can despawn before retrieval");
                }
            }finally{for(var item:rewards)item.discard();dragon.discard();}
            ctx.complete();
        });
    }

    @GameTest(templateName=SPACE,batchId="dragon_patch")
    public void nativeFightAppliesCustomDragonBarAndRestoresOrdinaryStyle(TestContext ctx)throws Exception {
        var world=ctx.getWorld();var center=ctx.getAbsolutePos(new BlockPos(12,9,12));
        var fight=new EnderDragonFight(world,world.getSeed(),EnderDragonFight.Data.DEFAULT,center);
        var dragon=EntityType.ENDER_DRAGON.create(world);dragon.setPosition(Vec3d.ofCenter(center));MobVariants.makePermanent(dragon);
        var uuid=EnderDragonFight.class.getDeclaredField("dragonUuid");uuid.setAccessible(true);uuid.set(fight,dragon.getUuid());
        var field=EnderDragonFight.class.getDeclaredField("bossBar");field.setAccessible(true);var bar=(ServerBossBar)field.get(fight);
        try{
            fight.updateFight(dragon);
            ctx.assertTrue(bar.getName().equals(net.minecraft.text.Text.translatable("entity.tetanusmod.tetanized_ender_dragon"))&&bar.getColor()==BossBar.Color.PURPLE&&bar.getStyle()==BossBar.Style.NOTCHED_10,"Special fight has vanilla name/color/style");
            ctx.assertTrue(Math.abs(bar.getPercent()-1)<.001,"Custom bar is not linked to native health");
            dragon.setHealth(550);fight.updateFight(dragon);ctx.assertTrue(Math.abs(bar.getPercent()-.5)<.001,"Custom bar ignores actual1100HP maximum");
            var ordinary=EntityType.ENDER_DRAGON.create(world);uuid.set(fight,ordinary.getUuid());fight.updateFight(ordinary);
            ctx.assertTrue(bar.getName().equals(ordinary.getDisplayName())&&bar.getColor()==BossBar.Color.PINK&&bar.getStyle()==BossBar.Style.PROGRESS,"Ordinary fight inherited infected bar style");ordinary.discard();
        }finally{dragon.discard();}ctx.complete();
    }

    @GameTest(templateName=SPACE,batchId="dragon_patch")
    public void pillarConversionRetainsNativeBeamProtectionAndDoesNotTouchOtherCrystals(TestContext ctx) {
        var world=ctx.getWorld();var spike=EndSpikeFeature.getSpikes(world).get(0);
        var base=new BlockPos(spike.getCenterX(),spike.getHeight()+1,spike.getCenterZ());world.getChunk(base);
        var original=new EndCrystalEntity(EntityType.END_CRYSTAL,world);original.setPosition(Vec3d.ofBottomCenter(base));original.setInvulnerable(true);original.setShowBottom(false);original.setBeamTarget(base.up(30));world.spawnEntity(original);
        var unrelated=new EndCrystalEntity(EntityType.END_CRYSTAL,world);unrelated.setPosition(Vec3d.ofBottomCenter(base.add(7,0,0)));world.spawnEntity(unrelated);
        var area=new Box(base).expand(2);List<TetanizedEndCrystalEntity> replacements=List.of();
        try{
            DragonCrystalRitual.convertPillars(world);replacements=world.getEntitiesByClass(TetanizedEndCrystalEntity.class,area,e->true);
            ctx.assertTrue(original.isRemoved()&&replacements.size()==1&&!unrelated.isRemoved(),"Pillar replacement changed unrelated crystal or lost native healing crystal");
            var replacement=replacements.get(0);ctx.assertTrue(replacement.isInvulnerable()&&!replacement.shouldShowBottom()&&base.up(30).equals(replacement.getBeamTarget()),"Ritual replacement lost beam or native protection flags");
            ctx.assertTrue(DragonCrystalRitual.convertPillars(world)==0,"Repeated ritual ticks duplicate pillar crystals");
            var save=replacement.writeNbt(new NbtCompound());var reload=new TetanizedEndCrystalEntity(DragonContent.CRYSTAL_ENTITY,world);reload.readNbt(save);ctx.assertTrue(reload.getType()==DragonContent.CRYSTAL_ENTITY&&base.up(30).equals(reload.getBeamTarget()),"Saved pillar crystal lost variant identity");reload.discard();
        }finally{original.discard();unrelated.discard();for(var e:replacements)e.discard();}ctx.complete();
    }

    @GameTest(templateName=SPACE,batchId="dragon_patch")
    public void realSpecialCrystalExplosionInfectsSurvivorsButOrdinaryCrystalDoesNot(TestContext ctx) {
        var world=ctx.getWorld();var pos=Vec3d.ofCenter(ctx.getAbsolutePos(new BlockPos(23,16,23)));
        for(boolean special:new boolean[]{false,true}){
            var victim=EntityType.IRON_GOLEM.create(world);victim.setPosition(pos.add(5,0,0));victim.setAiDisabled(true);world.spawnEntity(victim);
            EndCrystalEntity crystal=special?new TetanizedEndCrystalEntity(DragonContent.CRYSTAL_ENTITY,world):new EndCrystalEntity(EntityType.END_CRYSTAL,world);crystal.setPosition(pos);world.spawnEntity(crystal);
            try{
                ctx.assertTrue(crystal.damage(world.getDamageSources().generic(),1)&&crystal.isRemoved(),"Crystal did not explode through its real damage path");
                ctx.assertTrue(victim.isAlive()&&victim.getHealth()<100,"Fixture was not a surviving victim of native crystal blast damage");
                ctx.assertTrue(victim.hasStatusEffect(TetanusMod.TETANUS)==special,"Crystal explosion infection identity is wrong");
            }finally{victim.discard();crystal.discard();}
        }ctx.complete();
    }
    @GameTest(templateName=SPACE,batchId="dragon_patch")
    public void nativeKillCommandPathAlsoDropsExactlyOneTrophyPair(TestContext ctx) {
        var world=ctx.getWorld();var at=ctx.getAbsolutePos(new BlockPos(20,8,20));
        var d=EntityType.ENDER_DRAGON.create(world);d.setFightOrigin(BlockPos.ORIGIN);d.setPosition(Vec3d.ofCenter(at));MobVariants.makePermanent(d);
        try{
            d.kill();d.kill();
            var rewards=world.getEntitiesByClass(ItemEntity.class,new Box(at).expand(20),e->e.getStack().isOf(DragonContent.SWORD)||e.getStack().isOf(DragonContent.EGG_ITEM));
            ctx.assertTrue(d.isRemoved()&&rewards.size()==2,"Native instant-kill command skipped or duplicated trophies");
            for(var item:rewards)item.discard();
        }finally{d.discard();}ctx.complete();
    }

    @GameTest(templateName=SPACE,batchId="dragon_patch",tickLimit=180)
    public void nativeHoverTransitionStillLiftsTheActualMouthTenBlocks(TestContext ctx) {
        var world=ctx.getWorld();var at=ctx.getAbsolutePos(new BlockPos(24,22,24));
        var player=new net.minecraft.server.network.ServerPlayerEntity(world.getServer(),world,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"pitched-grab"));
        world.getServer().getPlayerManager().onPlayerConnect(new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND),player);
        player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);player.getAbilities().invulnerable=true;player.setNoGravity(true);
        var dragon=EntityType.ENDER_DRAGON.create(world);dragon.setPosition(Vec3d.ofCenter(at));MobVariants.makePermanent(dragon);
        dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);dragon.setPitch(65);dragon.head.setPosition(dragon.getPos().add(0,8,0));
        player.setPosition(dragon.head.getPos());double start=DragonEncounter.mouth(dragon).y;
        ctx.assertTrue(DragonEncounter.tryGrab(dragon,player,0),"Pitched close-pass fixture did not mount through actual exposure path");
        world.spawnEntity(dragon);
        final double[] highest={start};
        ctx.runAtEveryTick(()->highest[0]=Math.max(highest[0],player.getY()));
        ctx.runAtTick(100,()->{
            try{
                ctx.assertTrue(!player.hasVehicle()&&DragonEncounter.data(dragon).carried==null,"Pitched native grab never released");
                ctx.assertTrue(highest[0]-start>=9.8&&highest[0]-start<=10.75,"Flight-to-HOVER head drop prevented actual ten-block passenger lift: "+(highest[0]-start));
                ctx.assertTrue(DragonEncounter.data(dragon).nextGrab>world.getTime(),"Native carry lost its two-minute cooldown");
            }finally{DragonEncounter.release(dragon);dragon.discard();world.getServer().getPlayerManager().remove(player);player.discard();}
            ctx.complete();
        });
    }

    @GameTest(templateName=SPACE,batchId="dragon_patch")
    public void staleCompletedOrAbortedRitualListCannotConvertMorePillars(TestContext ctx)throws Exception {
        var world=ctx.getWorld();var spike=EndSpikeFeature.getSpikes(world).get(0);
        var base=new BlockPos(spike.getCenterX(),spike.getHeight()+1,spike.getCenterZ());world.getChunk(base);
        var ordinary=new EndCrystalEntity(EntityType.END_CRYSTAL,world);ordinary.setPosition(Vec3d.ofBottomCenter(base));world.spawnEntity(ordinary);
        var fight=new EnderDragonFight(world,world.getSeed(),EnderDragonFight.Data.DEFAULT);
        var field=EnderDragonFight.class.getDeclaredField("crystals");field.setAccessible(true);
        var offerings=new ArrayList<EndCrystalEntity>();for(int i=0;i<4;i++){var c=new TetanizedEndCrystalEntity(DragonContent.CRYSTAL_ENTITY,world);c.discard();offerings.add(c);}field.set(fight,offerings);
        try{fight.tick();ctx.assertTrue(!ordinary.isRemoved(),"A completed/aborted stale offering list kept infecting ordinary pillar crystals");}
        finally{ordinary.discard();for(var c:offerings)c.discard();}ctx.complete();
    }

}
