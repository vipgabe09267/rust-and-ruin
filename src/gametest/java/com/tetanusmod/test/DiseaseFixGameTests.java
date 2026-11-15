package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.config.ModConfig;
import com.tetanusmod.effect.*;
import com.tetanusmod.entity.*;
import com.tetanusmod.registry.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.PotionUtil;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

public class DiseaseFixGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void defaultsAndExistingConfigsMigrate(TestContext ctx) {
        var defaults=new ModConfig();ctx.assertTrue(defaults.minDurationSeconds==300&&defaults.maxDurationSeconds==600,"New defaults incorrect");
        var migrated=ModConfig.fromJson("{\"minDurationSeconds\":120,\"maxDurationSeconds\":300,\"balanceRevision\":3}");
        ctx.assertTrue(migrated.minDurationSeconds==300&&migrated.maxDurationSeconds==600,"Old defaults did not migrate");
        var custom=ModConfig.fromJson("{\"minDurationSeconds\":400,\"maxDurationSeconds\":700,\"balanceRevision\":3}");
        ctx.assertTrue(custom.minDurationSeconds==400&&custom.maxDurationSeconds==700,"Custom duration was overwritten");
        var legacy=ModConfig.fromJson("{\"minDurationSeconds\":120,\"maxDurationSeconds\":300}");
        ctx.assertTrue(legacy.minDurationSeconds==300&&legacy.balanceRevision==6,"Unversioned config migration failed");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void lockjawWindowsRandomizeAndPersist(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();var state=(InfectionState)player;int min=99999,max=0;
        for(int i=0;i<256;i++) {
            Infection.apply(player,null);int window=state.tetanus$getLockjawWindow();
            ctx.assertTrue(window>=2400&&window<=4800&&window%20==0,"Lockjaw is outside 2-4 minutes");min=Math.min(min,window);max=Math.max(max,window);
        }
        ctx.assertTrue(max>min,"Lockjaw window was not randomized");
        state.tetanus$setLockjaw(3000);var nbt=player.writeNbt(new NbtCompound());var copy=ctx.createMockSurvivalPlayer();copy.readNbt(nbt);
        ctx.assertTrue(((InfectionState)copy).tetanus$getLockjaw()==3000&&((InfectionState)copy).tetanus$getLockjawWindow()==state.tetanus$getLockjawWindow(),"Save/load truncated the long Lockjaw phase");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void continuousLockjawSurvivesAMinuteAndExpires(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();var state=(InfectionState)player;Infection.apply(player,null);
        state.tetanus$setInfection(3599,6000);state.tetanus$setLockjawWindow(2400);
        TetanusMod.TETANUS.applyUpdateEffect(player,0);LockjawEffect.flush(player);ctx.assertFalse(player.hasStatusEffect(TetanusMod.LOCKJAW),"Lockjaw started before its window");
        state.tetanus$setInfection(3600,6000);TetanusMod.TETANUS.applyUpdateEffect(player,0);LockjawEffect.flush(player);
        ctx.assertTrue(player.getStatusEffect(TetanusMod.LOCKJAW).getDuration()==2400,"Final phase not two continuous minutes");
        for(int i=0;i<1200;i++)player.tick();
        ctx.assertTrue(player.hasStatusEffect(TetanusMod.LOCKJAW)&&player.getStatusEffect(TetanusMod.LOCKJAW).getDuration()>=1199,"Lockjaw disappeared before a minute elapsed");
        player.removeStatusEffect(TetanusMod.TETANUS);player.tick();ctx.assertFalse(player.hasStatusEffect(TetanusMod.LOCKJAW),"Recovered player retained Lockjaw");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void milkPreservesDiseaseAndAntitoxinCuresImmediately(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();Infection.apply(player,null);var state=(InfectionState)player;
        state.tetanus$setInfection(5000,8000);state.tetanus$setLockjaw(3000);LockjawEffect.flush(player);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,200));player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,200));
        int duration=player.getStatusEffect(TetanusMod.TETANUS).getDuration();
        var bucket=Items.MILK_BUCKET.finishUsing(new ItemStack(Items.MILK_BUCKET),ctx.getWorld(),player);
        ctx.assertTrue(bucket.isOf(Items.BUCKET)&&player.hasStatusEffect(TetanusMod.TETANUS)&&player.hasStatusEffect(TetanusMod.LOCKJAW),"Milk removed disease or failed to return bucket");
        ctx.assertTrue(state.tetanus$getElapsed()==5000&&player.getStatusEffect(TetanusMod.TETANUS).getDuration()==duration,"Milk restarted disease progression");
        ctx.assertFalse(player.hasStatusEffect(StatusEffects.POISON)||player.hasStatusEffect(StatusEffects.NIGHT_VISION),"Milk stopped clearing other effects");
        var bottle=ModItems.ANTITOXIN.finishUsing(new ItemStack(ModItems.ANTITOXIN),ctx.getWorld(),player);
        ctx.assertTrue(bottle.isOf(Items.GLASS_BOTTLE)&&!player.hasStatusEffect(TetanusMod.TETANUS)&&!player.hasStatusEffect(TetanusMod.LOCKJAW)&&state.tetanus$getLockjaw()==0,"Antitoxin did not immediately clear both diseases");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void temporaryInfectionsDoNotChangeAnimalLoot(TestContext ctx) {
        int count=0;
        for(var type:net.minecraft.registry.Registries.ENTITY_TYPE) {
            if(!(type.create(ctx.getWorld()) instanceof MobEntity animal)||!MobVariants.isAnimal(animal))continue;
            animal.setPosition(ctx.getAbsolute(new Vec3d(3,4,3)));animal.setAiDisabled(true);ctx.getWorld().spawnEntity(animal);
            Infection.apply(animal,null);ctx.assertFalse(MobVariants.permanent(animal),"Temporary fixture mutated");
            animal.damage(ctx.getWorld().getDamageSources().genericKill(),10000);
            var drops=ctx.getWorld().getEntitiesByClass(ItemEntity.class,animal.getBoundingBox().expand(2),e->true);
            ctx.assertTrue(drops.stream().noneMatch(item->item.getStack().isOf(ModItems.INFECTED_MEAT)),"Temporary infection changed ordinary loot for "+type);
            drops.forEach(Entity::discard);animal.discard();count++;
        }
        ctx.assertTrue(count>=30,"Too few animal types tested: "+count);ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void strangeMeatFromRealPickup(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();player.getInventory().clear();
        var item=new ItemEntity(ctx.getWorld(),player.getX(),player.getY(),player.getZ(),new ItemStack(ModItems.INFECTED_MEAT));item.setPickupDelay(0);ctx.getWorld().spawnEntity(item);
        item.onPlayerCollision(player);player.playerScreenHandler.sendContentUpdates();
        var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id("strange_meat"));
        ctx.assertTrue(player.getInventory().contains(new ItemStack(ModItems.INFECTED_MEAT)),"Meat did not enter inventory");
        ctx.assertTrue(advancement!=null&&player.getAdvancementTracker().getProgress(advancement).isDone(),"Collecting infected meat did not grant Strange Meat");player.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void everyOrdinaryMobCanMutateThroughExposure(TestContext ctx) {
        float animalChance=TetanusMod.CONFIG.animalConversionChance,mobChance=TetanusMod.CONFIG.mobConversionChance,golemChance=TetanusMod.CONFIG.golemConversionChance;int count=0;
        try {
            TetanusMod.CONFIG.animalConversionChance=1;TetanusMod.CONFIG.mobConversionChance=1;TetanusMod.CONFIG.golemConversionChance=1;
            for(var type:net.minecraft.registry.Registries.ENTITY_TYPE) {
                if(!(type.create(ctx.getWorld()) instanceof MobEntity mob)||mob instanceof net.minecraft.entity.boss.WitherEntity||mob instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity||mob instanceof com.tetanusmod.entity.WitherMinionEntity)continue;
                Infection.expose(mob,null);ctx.assertTrue(MobVariants.permanent(mob),"Rusty exposure could not mutate "+type);count++;
            }
        }finally{TetanusMod.CONFIG.animalConversionChance=animalChance;TetanusMod.CONFIG.mobConversionChance=mobChance;TetanusMod.CONFIG.golemConversionChance=golemChance;}
        ctx.assertTrue(count>60,"Too few mob types tested");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rustyMeleeCanMutate(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUSTED_REBAR));
        float chance=TetanusMod.CONFIG.animalConversionChance;
        try {
            TetanusMod.CONFIG.animalConversionChance=1;var cow=EntityType.COW.create(ctx.getWorld());
            cow.damage(ctx.getWorld().getDamageSources().playerAttack(player),1);
            ctx.assertTrue(MobVariants.permanent(cow)&&cow.hasStatusEffect(TetanusMod.TETANUS),"Rusty melee did not convert cow");
        }finally{TetanusMod.CONFIG.animalConversionChance=chance;}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void diseasePotionsRandomizeWithoutChangingOtherPotions(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();int min=99999,max=0;
        for(int i=0;i<64;i++) {
            player.removeStatusEffect(TetanusMod.TETANUS);
            Items.POTION.finishUsing(PotionUtil.setPotion(new ItemStack(Items.POTION),ModBrewing.TETANUS),ctx.getWorld(),player);
            int duration=player.getStatusEffect(TetanusMod.TETANUS).getDuration();
            ctx.assertTrue(duration>=6000&&duration<=12000,"Potion Tetanus not 5-10 minutes");min=Math.min(min,duration);max=Math.max(max,duration);
        }
        ctx.assertTrue(max>min,"Potion Tetanus did not randomize");
        var poison=new StatusEffectInstance(StatusEffects.POISON,300);
        ctx.assertTrue(Infection.potionEffect(poison)==poison,"Unrelated potion was modified");ctx.complete();
    }
    private static class Splash extends net.minecraft.entity.projectile.thrown.PotionEntity {
        Splash(net.minecraft.world.World world){super(EntityType.POTION,world);}
        void hit(LivingEntity target){setPosition(target.getPos());setItem(PotionUtil.setPotion(new ItemStack(Items.SPLASH_POTION),ModBrewing.TETANUS));onCollision(new net.minecraft.util.hit.EntityHitResult(target));}
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void actualSplashAndCloudUseLongDurations(TestContext ctx) {
        var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(3,4,3));cow.setAiDisabled(true);cow.setNoGravity(true);
        ctx.runAtTick(2,()->{
            new Splash(ctx.getWorld()).hit(cow);
            ctx.assertTrue(cow.hasStatusEffect(TetanusMod.TETANUS)&&cow.getStatusEffect(TetanusMod.TETANUS).getDuration()>=6000,"Actual splash did not apply 5-10 minute Tetanus");
            cow.removeStatusEffect(TetanusMod.TETANUS);
            var cloud=new AreaEffectCloudEntity(ctx.getWorld(),cow.getX(),cow.getY(),cow.getZ());cloud.setPotion(ModBrewing.TETANUS);cloud.setWaitTime(0);cloud.setRadius(3);
            cloud.age=5;cloud.tick();
            ctx.assertTrue(cow.hasStatusEffect(TetanusMod.TETANUS)&&cow.getStatusEffect(TetanusMod.TETANUS).getDuration()>=6000,"Lingering cloud bypassed the new duration rules");cow.discard();ctx.complete();
        });
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void naturalExpiryClearsBothEffects(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();player.addStatusEffect(new StatusEffectInstance(TetanusMod.TETANUS,3));
        var state=(InfectionState)player;state.tetanus$setInfection(5997,6000);state.tetanus$setLockjawWindow(2400);
        player.tick();ctx.assertTrue(player.hasStatusEffect(TetanusMod.LOCKJAW),"Final seconds had no Lockjaw");
        for(int i=0;i<4;i++)player.tick();
        ctx.assertFalse(player.hasStatusEffect(TetanusMod.TETANUS)||player.hasStatusEffect(TetanusMod.LOCKJAW)||state.tetanus$getLockjaw()>0,"Natural recovery retained disease state");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void mobLootRuleIsRespected(TestContext ctx) {
        var rule=ctx.getWorld().getGameRules().get(net.minecraft.world.GameRules.DO_MOB_LOOT);boolean original=rule.get();
        try {
            rule.set(false,ctx.getWorld().getServer());var animal=ctx.spawnEntity(EntityType.COW,new Vec3d(3,4,3));MobVariants.makePermanent(animal);
            animal.damage(ctx.getWorld().getDamageSources().genericKill(),10000);
            ctx.assertTrue(ctx.getWorld().getEntitiesByClass(ItemEntity.class,animal.getBoundingBox().expand(2),e->true).isEmpty(),"Infected meat ignored doMobLoot=false");animal.discard();
        }finally{rule.set(original,ctx.getWorld().getServer());}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void eatingInfectedMeatAwardsEatingAdvancement(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();float chance=TetanusMod.CONFIG.salmonellaChance;
        try {
            TetanusMod.CONFIG.salmonellaChance=1;
            ModItems.INFECTED_MEAT.finishUsing(new ItemStack(ModItems.INFECTED_MEAT),ctx.getWorld(),player);
            var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id("why_did_i_eat_that"));
            ctx.assertTrue(player.hasStatusEffect(TetanusMod.SALMONELLA)&&advancement!=null&&player.getAdvancementTracker().getProgress(advancement).isDone(),"Eating infected meat did not award Why did I eat that?");
        }finally{TetanusMod.CONFIG.salmonellaChance=chance;player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void eatingWithoutSalmonellaDoesNotAwardDiseaseAdvancement(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();float chance=TetanusMod.CONFIG.salmonellaChance;
        try {
            TetanusMod.CONFIG.salmonellaChance=0;
            ModItems.INFECTED_MEAT.finishUsing(new ItemStack(ModItems.INFECTED_MEAT),ctx.getWorld(),player);
            var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id("why_did_i_eat_that"));
            ctx.assertTrue(!player.hasStatusEffect(TetanusMod.SALMONELLA)&&advancement!=null&&!player.getAdvancementTracker().getProgress(advancement).isDone(),"Why did I eat that? unlocked without Salmonella");
        }finally{TetanusMod.CONFIG.salmonellaChance=chance;player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void actualSalmonellaTicksCauseNauseaVomitAndHungerLoss(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();player.setHealth(20);player.getHungerManager().setFoodLevel(20);
        player.addStatusEffect(new StatusEffectInstance(TetanusMod.SALMONELLA,201));
        player.tick();player.tick();
        ctx.assertTrue(player.hasStatusEffect(StatusEffects.NAUSEA)&&player.getHungerManager().getFoodLevel()==18&&player.getHealth()==19.5F,"Actual Salmonella ticks did not cause sickness and hunger loss");
        player.removeStatusEffect(TetanusMod.SALMONELLA);player.removeStatusEffect(StatusEffects.NAUSEA);SalmonellaEffect.flush(player);
        ctx.assertFalse(player.hasStatusEffect(StatusEffects.NAUSEA),"Cured Salmonella applied queued nausea");ctx.complete();
    }
}