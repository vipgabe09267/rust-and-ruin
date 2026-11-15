package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.config.ModConfig;
import com.tetanusmod.entity.*;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import java.util.*;

/** Real native death/damage/spawn routes for the November boss balance fixes. */
public final class WitherRevisionGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="wither_revision_config")
    public void defaultChanceAndLegacyDefaultsMigrateWithoutOverwritingCustomChance(TestContext ctx){
        ctx.assertTrue(new ModConfig().witherConversionChance==0.75F,"New Wither chance is not75 percent");
        ctx.assertTrue(ModConfig.fromJson("{\"balanceRevision\":5,\"witherConversionChance\":0.05}").witherConversionChance==0.75F,"Existing5 percent servers were not upgraded");
        ctx.assertTrue(ModConfig.fromJson("{\"witherConversionChance\":0.02}").witherConversionChance==0.75F,"Legacy2 percent default was not upgraded");
        ctx.assertTrue(ModConfig.fromJson("{\"balanceRevision\":5,\"witherConversionChance\":0.4}").witherConversionChance==0.4F,"Custom Wither chance was overwritten");
        ctx.assertTrue(ModConfig.fromJson("{\"balanceRevision\":5,\"witherConversionChance\":0}").witherConversionChance==0,"Disabled Wither mutation was overwritten");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="wither_revision_health")
    public void bossHealthIs650AndLegacySavedHealthMigratesProportionally(TestContext ctx){
        var boss=EntityType.WITHER.create(ctx.getWorld());MobVariants.makePermanent(boss);
        ctx.assertTrue(boss.getMaxHealth()==650&&boss.getHealth()==650,"New special Wither does not have650HP");
        var max=boss.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);var id=UUID.fromString("a70b1111-6adf-4cca-8713-c481a7c5e011");
        max.removeModifier(id);max.addPersistentModifier(new EntityAttributeModifier(id,"Legacy mutation",1,EntityAttributeModifier.Operation.MULTIPLY_TOTAL));boss.setHealth(300);
        var saved=boss.writeNbt(new NbtCompound());var restored=EntityType.WITHER.create(ctx.getWorld());restored.readNbt(saved);MobVariants.loaded(restored);MobVariants.loaded(restored);
        ctx.assertTrue(restored.getMaxHealth()==650&&Math.abs(restored.getHealth()-325)<0.01,"Legacy half-health boss was healed, stacked or lost its health percentage");
        ctx.assertFalse(restored.hasStatusEffect(TetanusMod.TETANUS),"Special boss infects itself");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="wither_revision_loot")
    public void actualDeathsDropFiftyRealXpAndOneReplacementBook(TestContext ctx){
        var pos=ctx.getAbsolute(new Vec3d(4,6,4));int upgraded=0;
        for(int seed=0;seed<64;seed++){
            clear(ctx,pos);var boss=EntityType.WITHER.create(ctx.getWorld());boss.setAiDisabled(true);boss.setPosition(pos);ctx.getWorld().spawnEntity(boss);MobVariants.makePermanent(boss);boss.getRandom().setSeed(739L*seed+31);
            boss.damage(ctx.getWorld().getDamageSources().genericKill(),10000);
            var drops=ctx.getWorld().getEntitiesByClass(ItemEntity.class,new Box(pos,pos).expand(8),e->true);
            var books=drops.stream().filter(e->EnchantmentHelper.get(e.getStack()).getOrDefault(ModItems.TETANUS_ENCHANTMENT,0)>0).toList();
            ctx.assertTrue(books.size()==1&&books.get(0).getStack().getCount()==1,"A special Wither dropped zero or two books");
            int level=EnchantmentHelper.get(books.get(0).getStack()).getOrDefault(ModItems.TETANUS_ENCHANTMENT,0);ctx.assertTrue(level==1||level==2,"Invalid Wither book level");if(level==2)upgraded++;
            ctx.assertTrue(drops.stream().filter(e->e.getStack().isOf(ModItems.TETANIZED_WITHER_STAR)).mapToInt(e->e.getStack().getCount()).sum()==1,"Special star missing or duplicated");
            ctx.assertFalse(drops.stream().anyMatch(e->e.getStack().isOf(Items.NETHER_STAR)),"Special Wither also dropped an ordinary star");
            var orbs=ctx.getWorld().getEntitiesByClass(ExperienceOrbEntity.class,new Box(pos,pos).expand(8),e->true);int xp=0;
            for(var orb:orbs){var data=orb.writeNbt(new NbtCompound());xp+=orb.getExperienceAmount()*Math.max(1,data.getInt("Count"));}
            ctx.assertTrue(xp==50,"Boss dropped "+xp+" real XP instead of50, or duplicated vanilla XP");boss.discard();
        }
        ctx.assertTrue(upgraded>=18&&upgraded<=46,"Seeded50 percent replacement chance is implausible: "+upgraded+"/64");clear(ctx,pos);ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="wither_revision_xp_rules")
    public void disabledMobLootAndDisabledExperienceStillSuppressBossXp(TestContext ctx){
        var pos=ctx.getAbsolute(new Vec3d(4,6,4));boolean loot=ctx.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_LOOT);
        try{
            for(boolean disableRule:new boolean[]{false,true}){
                clear(ctx,pos);var boss=EntityType.WITHER.create(ctx.getWorld());boss.setAiDisabled(true);boss.setPosition(pos);ctx.getWorld().spawnEntity(boss);MobVariants.makePermanent(boss);
                ctx.getWorld().getGameRules().get(GameRules.DO_MOB_LOOT).set(!disableRule,ctx.getWorld().getServer());if(!disableRule)boss.disableExperienceDropping();
                boss.damage(ctx.getWorld().getDamageSources().genericKill(),10000);
                ctx.assertTrue(ctx.getWorld().getEntitiesByClass(ExperienceOrbEntity.class,new Box(pos,pos).expand(8),e->true).isEmpty(),"Boss ignored native XP suppression");boss.discard();
            }
        }finally{ctx.getWorld().getGameRules().get(GameRules.DO_MOB_LOOT).set(loot,ctx.getWorld().getServer());clear(ctx,pos);}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="wither_revision_infection")
    public void successfulSkullDamageCanInfectVaccinatedPlayersWithoutBypassingProtection(TestContext ctx){
        var target=ctx.createMockSurvivalPlayer();var boss=EntityType.WITHER.create(ctx.getWorld());MobVariants.makePermanent(boss);var skull=new WitherSkullEntity(ctx.getWorld(),boss,0,0,1);
        float original=TetanusMod.CONFIG.vaccineProtectionChance;int infected=0;
        try{
            TetanusMod.CONFIG.vaccineProtectionChance=0.8F;target.addStatusEffect(new StatusEffectInstance(TetanusMod.VACCINATED,24000));target.getRandom().setSeed(657239);
            for(int hit=0;hit<256;hit++){
                target.removeStatusEffect(TetanusMod.TETANUS);target.setHealth(target.getMaxHealth());target.timeUntilRegen=0;
                ctx.assertTrue(target.damage(ctx.getWorld().getDamageSources().witherSkull(skull,boss),0.25F),"Native Wither skull damage did not land");if(target.hasStatusEffect(TetanusMod.TETANUS))infected++;
            }
            ctx.assertTrue(infected>=20&&infected<=90,"Vaccine was bypassed or immune to every Wither hit: "+infected+"/256 infected");
            TetanusMod.CONFIG.vaccineProtectionChance=1;target.removeStatusEffect(TetanusMod.TETANUS);target.timeUntilRegen=0;target.damage(ctx.getWorld().getDamageSources().witherSkull(skull,boss),0.25F);
            ctx.assertFalse(target.hasStatusEffect(TetanusMod.TETANUS),"Boss bypasses configured vaccine prevention");
            var ordinary=EntityType.WITHER.create(ctx.getWorld());var ordinarySkull=new WitherSkullEntity(ctx.getWorld(),ordinary,0,0,1);
            TetanusMod.CONFIG.vaccineProtectionChance=0;target.removeStatusEffect(TetanusMod.TETANUS);target.timeUntilRegen=0;target.damage(ctx.getWorld().getDamageSources().witherSkull(ordinarySkull,ordinary),0.25F);
            ctx.assertFalse(target.hasStatusEffect(TetanusMod.TETANUS),"Ordinary Wither unexpectedly transmits Tetanus");
        }finally{TetanusMod.CONFIG.vaccineProtectionChance=original;target.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="wither_revision_minion_waves")
    public void wavesHaveTwoSmallIronSwordMinionsAndRespectLiveCapAndBossDeath(TestContext ctx){
        floor(ctx);var player=player(ctx);var boss=boss(ctx);var state=(WitherBossState)boss;
        try{
            for(int wave=0;wave<3;wave++)ctx.assertTrue(WitherBossEncounter.spawnWave(boss,player)==2,"Safe arena did not receive exactly two minions");
            ctx.assertTrue(state.rustruin$minionLeases().size()==6&&WitherBossEncounter.spawnWave(boss,player)==0,"Boss exceeded its bounded live guard");
            var summons=state.rustruin$minionLeases().keySet().stream().map(ctx.getWorld()::getEntity).map(e->(WitherMinionEntity)e).toList();
            for(var minion:summons){minion.setAiDisabled(true);ctx.assertTrue(minion.getMaxHealth()==20&&minion.getHealth()==20,"Minion does not have20health");ctx.assertTrue(minion.getMainHandStack().isOf(Items.IRON_SWORD),"Minion has the wrong weapon");ctx.assertTrue(minion.getHeight()<1.3F&&!minion.isPersistent(),"Tiny minion hitbox or native persistence wrong");ctx.assertFalse(minion.canPickUpLoot(),"Temporary minion can steal and destroy player gear");ctx.assertTrue(MobVariants.permanent(minion)&&!minion.hasStatusEffect(TetanusMod.TETANUS),"Minion appearance or self-damage state wrong");}
            boss.damage(ctx.getWorld().getDamageSources().genericKill(),10000);ctx.assertTrue(summons.stream().allMatch(Entity::isRemoved)&&state.rustruin$minionLeases().isEmpty(),"Boss death left active summons behind");
        }finally{WitherBossEncounter.bossDied(boss);boss.discard();cleanup(player);}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="wither_revision_minion_timing")
    public void cooldownIsRandomSavedAndNeverProducesAnEarlyWave(TestContext ctx){
        floor(ctx);var player=player(ctx);var boss=boss(ctx);var state=(WitherBossState)boss;var values=new HashSet<Integer>();
        try{
            for(int sample=0;sample<100;sample++){int delay=WitherBossEncounter.nextCooldown(boss);ctx.assertTrue(delay>=600&&delay<=1200,"Wave delay outside30–60seconds");values.add(delay);}ctx.assertTrue(values.size()>20,"Wave cooldown is not random");
            state.rustruin$minionCooldown(600);for(int tick=0;tick<599;tick++)WitherBossEncounter.tick(boss);ctx.assertTrue(state.rustruin$minionLeases().isEmpty(),"Boss summoned before30seconds");WitherBossEncounter.tick(boss);ctx.assertTrue(state.rustruin$minionLeases().size()==2,"Scheduled wave failed");
            state.rustruin$minionCooldown(837);var nbt=boss.writeNbt(new NbtCompound());var loaded=EntityType.WITHER.create(ctx.getWorld());loaded.readNbt(nbt);ctx.assertTrue(((WitherBossState)loaded).rustruin$minionCooldown()==837&&((WitherBossState)loaded).rustruin$minionLeases().size()==2,"Save/load lost cooldown or ownership cap");
        }finally{WitherBossEncounter.bossDied(boss);boss.discard();cleanup(player);}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="wither_revision_minion_safety")
    public void summonsRejectFluidsCollisionAndUnsupportedGroundButAllowBrightArenas(TestContext ctx){
        floor(ctx);var minion=WitherMinions.TYPE.create(ctx.getWorld());var feet=ctx.getAbsolutePos(new BlockPos(4,2,4));
        ctx.assertTrue(WitherBossEncounter.safePosition(ctx.getWorld(),minion,feet),"Open floor rejected");
        for(var fluid:List.of(Blocks.LAVA,Blocks.WATER)){ctx.getWorld().setBlockState(feet,fluid.getDefaultState());ctx.assertFalse(WitherBossEncounter.safePosition(ctx.getWorld(),minion,feet),"Summon appeared in fluid");}
        ctx.getWorld().setBlockState(feet,Blocks.STONE.getDefaultState());ctx.assertFalse(WitherBossEncounter.safePosition(ctx.getWorld(),minion,feet),"Summon appeared inside solid terrain");
        ctx.getWorld().setBlockState(feet,Blocks.AIR.getDefaultState());ctx.getWorld().setBlockState(feet.down(),Blocks.AIR.getDefaultState());ctx.assertFalse(WitherBossEncounter.safePosition(ctx.getWorld(),minion,feet),"Summon appeared without ground");
        ctx.getWorld().setBlockState(feet.down(),Blocks.SEA_LANTERN.getDefaultState());ctx.assertTrue(WitherBossEncounter.safePosition(ctx.getWorld(),minion,feet),"Deliberate boss summon incorrectly requires a dark floor");minion.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="wither_revision_minion_expiry")
    public void minionExpirySurvivesSavingAndDoesNotDropFarmableEquipment(TestContext ctx){
        var pos=ctx.getAbsolute(new Vec3d(4,4,4));clear(ctx,pos);var minion=WitherMinions.TYPE.create(ctx.getWorld());minion.assignBoss(UUID.randomUUID(),ctx.getWorld().getTime()+2400);var saved=minion.writeNbt(new NbtCompound());saved.putBoolean("CanPickUpLoot",true);var loaded=WitherMinions.TYPE.create(ctx.getWorld());loaded.readNbt(saved);
        ctx.assertTrue(loaded.bossUuid().equals(minion.bossUuid())&&loaded.expiresAt()==minion.expiresAt(),"Minion lifetime or owner reset during loading");ctx.assertFalse(loaded.canPickUpLoot(),"NBT load restored unsafe temporary-minion looting");
        loaded.setPosition(pos);ctx.getWorld().spawnEntity(loaded);loaded.assignBoss(minion.bossUuid(),ctx.getWorld().getTime());loaded.tick();ctx.assertTrue(loaded.isRemoved(),"Expired summon survived");
        minion.setPosition(pos);ctx.getWorld().spawnEntity(minion);minion.damage(ctx.getWorld().getDamageSources().genericKill(),10000);ctx.assertTrue(ctx.getWorld().getEntitiesByClass(ItemEntity.class,new Box(pos,pos).expand(8),e->true).isEmpty(),"Summon dropped swords/skulls/mutation salvage");ctx.assertTrue(ctx.getWorld().getEntitiesByClass(ExperienceOrbEntity.class,new Box(pos,pos).expand(8),e->true).isEmpty(),"Summon created an unlimited XP farm");minion.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="wither_revision_no_gear_theft")
    public void temporaryMinionsCannotAcquireDroppedValuableGearOrBecomePersistent(TestContext ctx){
        floor(ctx);var world=ctx.getWorld();ctx.assertTrue(world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING),"Looting control requires native mob griefing enabled");var minion=WitherMinions.TYPE.create(world);var nativeControl=EntityType.WITHER_SKELETON.create(world);var at=ctx.getAbsolute(new Vec3d(3.5,2,3.5));var controlAt=ctx.getAbsolute(new Vec3d(9.5,2,3.5));
        minion.setPosition(at);minion.initialize(world,world.getLocalDifficulty(BlockPos.ofFloored(at)),SpawnReason.MOB_SUMMONED,null,null);minion.setAiDisabled(true);world.spawnEntity(minion);nativeControl.setPosition(controlAt);nativeControl.setAiDisabled(true);nativeControl.setCanPickUpLoot(true);world.spawnEntity(nativeControl);
        var valuable=new ItemEntity(world,at.x,at.y,at.z,new ItemStack(Items.DIAMOND_CHESTPLATE));valuable.setPickupDelay(0);valuable.setVelocity(Vec3d.ZERO);world.spawnEntity(valuable);var controlDrop=new ItemEntity(world,controlAt.x,controlAt.y,controlAt.z,new ItemStack(Items.DIAMOND_CHESTPLATE));controlDrop.setPickupDelay(0);controlDrop.setVelocity(Vec3d.ZERO);world.spawnEntity(controlDrop);
        try{
            minion.tickMovement();nativeControl.tickMovement();
            ctx.assertTrue(valuable.isAlive()&&valuable.getStack().isOf(Items.DIAMOND_CHESTPLATE)&&valuable.getStack().getCount()==1,"Temporary minion consumed player-dropped valuable equipment");ctx.assertTrue(minion.getEquippedStack(EquipmentSlot.CHEST).isEmpty()&&minion.getMainHandStack().isOf(Items.IRON_SWORD),"Temporary minion replaced its intended equipment");ctx.assertTrue(!minion.canPickUpLoot()&&!minion.isPersistent()&&minion.canImmediatelyDespawn(20000),"Looting changed minion vanilla-despawn eligibility");
            ctx.assertTrue(controlDrop.isRemoved()&&nativeControl.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.DIAMOND_CHESTPLATE)&&nativeControl.isPersistent(),"Native positive control did not acquire the same dropped armor");
        }finally{minion.discard();nativeControl.discard();valuable.discard();controlDrop.discard();}ctx.complete();
    }
    private static void floor(TestContext ctx){for(int x=-8;x<=14;x++)for(int z=-8;z<=14;z++){ctx.setBlockState(x,1,z,Blocks.STONE);for(int y=2;y<=6;y++)ctx.setBlockState(x,y,z,Blocks.AIR);}}
    private static ServerPlayerEntity player(TestContext ctx){
        var world=ctx.getWorld();var p=new ServerPlayerEntity(world.getServer(),world,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"wither-fixture"));
        world.getServer().getPlayerManager().onPlayerConnect(new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND),p);
        p.changeGameMode(GameMode.SURVIVAL);p.setNoGravity(true);p.setPosition(ctx.getAbsolute(new Vec3d(3.5,2,3.5)));return p;
    }
    private static WitherEntity boss(TestContext ctx){var b=EntityType.WITHER.create(ctx.getWorld());b.setAiDisabled(true);b.setPosition(ctx.getAbsolute(new Vec3d(3.5,4,3.5)));ctx.getWorld().spawnEntity(b);MobVariants.makePermanent(b);return b;}
    private static void cleanup(ServerPlayerEntity player){player.getServer().getPlayerManager().remove(player);player.discard();}
    private static void clear(TestContext ctx,Vec3d pos){for(var item:ctx.getWorld().getEntitiesByClass(ItemEntity.class,new Box(pos,pos).expand(8),e->true))item.discard();for(var orb:ctx.getWorld().getEntitiesByClass(ExperienceOrbEntity.class,new Box(pos,pos).expand(8),e->true))orb.discard();}
}
