package com.tetanusmod.test;

import com.tetanusmod.entity.MobVariants;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.*;
import java.util.UUID;

/** Above-vanilla boss health stays narrow, cached and persistent; ordinary attribute caps remain intact. */
public final class BossHealthGameTests implements FabricGameTest {
    private static final UUID MARKER=UUID.fromString("a70b1111-6adf-4cca-8713-c481a7c5e011");
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="dragon_health_ceiling")
    public void specialDragonReallyHas1100RawMaximumAndCurrentHealth(TestContext ctx){
        var dragon=EntityType.ENDER_DRAGON.create(ctx.getWorld());MobVariants.makePermanent(dragon);
        var health=dragon.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        ctx.assertTrue(EntityAttributes.GENERIC_MAX_HEALTH.clamp(1100)==1024,"Test no longer exercises vanilla's1024cap");
        ctx.assertTrue(health.getValue()==1100&&health.getValue()==1100&&dragon.getMaxHealth()==1100&&dragon.getHealth()==1100,"Native cached maximum/current health is not1100");
        ctx.assertTrue(dragon.writeNbt(new NbtCompound()).getFloat("Health")==1100,"Native entity NBT truncated current boss health");dragon.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="dragon_health_ceiling")
    public void partialDragonHealthAndRawAttributeSurviveNativeNbtRoundTrip(TestContext ctx){
        var dragon=EntityType.ENDER_DRAGON.create(ctx.getWorld());MobVariants.makePermanent(dragon);dragon.setHealth(743.5F);var saved=dragon.writeNbt(new NbtCompound());var loaded=EntityType.ENDER_DRAGON.create(ctx.getWorld());
        try{loaded.readNbt(saved);MobVariants.loaded(loaded);ctx.assertTrue(loaded.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH)==1100&&loaded.getMaxHealth()==1100&&loaded.getHealth()==743.5F,"Native NBT load truncated or healed1100HPboss");MobVariants.loaded(loaded);ctx.assertTrue(loaded.getHealth()==743.5F,"Repeated variant loading healed the boss");}finally{dragon.discard();loaded.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="dragon_health_ceiling")
    public void legacy300HpDragonMigratesWithoutAFullHeal(TestContext ctx){
        var legacy=EntityType.ENDER_DRAGON.create(ctx.getWorld());MobVariants.makePermanent(legacy);var attribute=legacy.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);attribute.removeModifier(MARKER);attribute.addPersistentModifier(new EntityAttributeModifier(MARKER,"Legacy mutation",.5,EntityAttributeModifier.Operation.MULTIPLY_TOTAL));legacy.setHealth(150);
        var loaded=EntityType.ENDER_DRAGON.create(ctx.getWorld());try{loaded.readNbt(legacy.writeNbt(new NbtCompound()));MobVariants.loaded(loaded);ctx.assertTrue(loaded.getMaxHealth()==1100&&loaded.getHealth()==550,"Legacy boss migration failed to preserve50percent health");}finally{legacy.discard();loaded.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="dragon_health_ceiling")
    public void ordinaryEntitiesAndUnrelatedModifiersKeepVanillaHealthCap(TestContext ctx){
        for(var type:java.util.List.of(EntityType.ZOMBIE,EntityType.COW,EntityType.WITHER,EntityType.ENDER_DRAGON)){
            var mob=type.create(ctx.getWorld());var health=mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);health.setBaseValue(2000);ctx.assertTrue(health.getValue()==1024&&mob.getMaxHealth()==1024,"Ordinary entity lost native cap: "+type);health.addPersistentModifier(new EntityAttributeModifier(UUID.randomUUID(),"Unrelated4.5bonus",4.5,EntityAttributeModifier.Operation.MULTIPLY_TOTAL));ctx.assertTrue(health.getValue()==1024,"Unrelated modifier bypassed native cap");mob.discard();
        }
        var zombie=EntityType.ZOMBIE.create(ctx.getWorld());MobVariants.makePermanent(zombie);ctx.assertTrue(zombie.getMaxHealth()==30,"Ordinary mutation health changed");var wither=EntityType.WITHER.create(ctx.getWorld());MobVariants.makePermanent(wither);ctx.assertTrue(wither.getMaxHealth()==650,"Wither health changed while extending dragon ceiling");zombie.discard();wither.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="dragon_health_ceiling")
    public void markedDragonModifiersRespectNativeOrderDebuffsAndBoundedUpperLimit(TestContext ctx){
        var dragon=EntityType.ENDER_DRAGON.create(ctx.getWorld());MobVariants.makePermanent(dragon);var health=dragon.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);var additive=new EntityAttributeModifier(UUID.randomUUID(),"Health reduction",-.5,EntityAttributeModifier.Operation.ADDITION);health.addTemporaryModifier(additive);ctx.assertTrue(health.getValue()==1097.25,"Above-cap health ignored native addition ordering");health.removeModifier(additive);
        var base=new EntityAttributeModifier(UUID.randomUUID(),"Base reduction",-.1,EntityAttributeModifier.Operation.MULTIPLY_BASE);health.addTemporaryModifier(base);ctx.assertTrue(health.getValue()==990,"Health reduction below vanilla ceiling was overridden");health.removeModifier(base);var total=new EntityAttributeModifier(UUID.randomUUID(),"Total reduction",-.5,EntityAttributeModifier.Operation.MULTIPLY_TOTAL);health.addTemporaryModifier(total);ctx.assertTrue(health.getValue()==550,"Total multiplier order changed");health.removeModifier(total);
        health.setBaseValue(1_000_000);ctx.assertTrue(health.getValue()==1100,"Malformed positive base escaped boss-specific1100ceiling");health.setBaseValue(Double.MAX_VALUE);ctx.assertTrue(health.getValue()==1024,"Nonfinite recomputation escaped native clamp");dragon.discard();ctx.complete();
    }
}