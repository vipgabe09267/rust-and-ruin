package com.tetanusmod.test;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.*;
import com.tetanusmod.entity.*;
import com.tetanusmod.registry.*;
import com.tetanusmod.rust.RustingSystem;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.enchantment.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

public class ExpansionGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void witherRollOnceAndNeverStacks(TestContext ctx) {
        float originalChance=TetanusMod.CONFIG.witherConversionChance;
        try {
            ctx.assertTrue(originalChance==0.05F,"Default wither chance is not 5%");
            TetanusMod.CONFIG.witherConversionChance=1;
            var mutant=new WitherEntity(EntityType.WITHER,ctx.getWorld()); MobVariants.rollSpecialWither(mutant);
            ctx.assertTrue(MobVariants.permanent(mutant)&&mutant.getMaxHealth()==600,"Successful wither roll failed");
            MobVariants.loaded(mutant);ctx.assertTrue(mutant.getMaxHealth()==600,"Wither health stacked on reload");
            TetanusMod.CONFIG.witherConversionChance=0;
            var normal=new WitherEntity(EntityType.WITHER,ctx.getWorld());MobVariants.loaded(normal);
            TetanusMod.CONFIG.witherConversionChance=1;MobVariants.loaded(normal);
            ctx.assertFalse(MobVariants.permanent(normal),"Ordinary wither rerolled its spawn chance");
            var creeper=new net.minecraft.entity.mob.CreeperEntity(EntityType.CREEPER,ctx.getWorld());MobVariants.makePermanent(creeper);
            NbtCompound saved=new NbtCompound();creeper.writeNbt(saved);
            ctx.assertTrue(saved.getByte("ExplosionRadius")==4,"Mutant creeper explosion bonus missing");
        } finally { TetanusMod.CONFIG.witherConversionChance=originalChance; }
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rustyNailCanInfectEnderman(TestContext ctx) {
        var enderman=ctx.spawnEntity(EntityType.ENDERMAN,new Vec3d(3.5,3,5.5));enderman.setAiDisabled(true);enderman.setNoGravity(true);
        var nail=new NailEntity(TetanusMod.NAIL_ENTITY,ctx.getWorld());nail.setItem(new ItemStack(ModItems.RUSTED_NAIL));
        nail.setPosition(ctx.getAbsolute(new Vec3d(3.5,3.7,2.5)));nail.setVelocity(0,0,0.7);ctx.getWorld().spawnEntity(nail);
        ctx.runAtTick(12,()->{
            ctx.assertTrue(enderman.hasStatusEffect(TetanusMod.TETANUS),"Enderman projectile immunity blocked rust infection");
            ctx.assertFalse(nail.isAlive(),"Rusty nail not consumed on enderman hit");ctx.complete();
        });
    }
    private CowEntity cow(TestContext ctx) {
        var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(3.5,3,5.5));
        cow.setAiDisabled(true); cow.setNoGravity(true); return cow;
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void permanentVariantsAndPersistence(TestContext ctx) {
        var zombie=new ZombieEntity(EntityType.ZOMBIE,ctx.getWorld());
        float original=zombie.getMaxHealth();
        MobVariants.makePermanent(zombie);
        ctx.assertTrue(MobVariants.permanent(zombie)&&zombie.getMaxHealth()==original*1.5F,"Zombie mutation missing health bonus");
        ctx.assertTrue(zombie.hasStatusEffect(TetanusMod.TETANUS)&&zombie.getStatusEffect(TetanusMod.TETANUS).getDuration()==-1,"Permanent infection not infinite");
        NbtCompound nbt=new NbtCompound(); zombie.writeNbt(nbt);
        var loaded=new ZombieEntity(EntityType.ZOMBIE,ctx.getWorld()); loaded.readNbt(nbt); MobVariants.loaded(loaded); MobVariants.loaded(loaded);
        ctx.assertTrue(MobVariants.permanent(loaded)&&loaded.getMaxHealth()==original*1.5F,"Reload lost or stacked mutation");
        var animal=cow(ctx); double speed=animal.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        MobVariants.makePermanent(animal);
        ctx.assertTrue(animal.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)<speed,"Tetanized animal is not slower");
        ctx.assertTrue(MobVariants.eligibleRoll(animal,0.019F)&&!MobVariants.eligibleRoll(animal,0.02F),"Animal 2% boundary wrong");
        ctx.assertTrue(MobVariants.eligibleRoll(zombie,0.009F)&&!MobVariants.eligibleRoll(zombie,0.01F),"Mob 1% boundary wrong");
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void gentleMobDamageAndPermanentTicks(TestContext ctx) {
        var cow=cow(ctx); Infection.apply(cow,null);
        var state=(InfectionState)cow;
        state.tetanus$setInfection(200,2400); TetanusMod.TETANUS.applyUpdateEffect(cow,0);
        ctx.assertTrue(cow.getHealth()==9,"Mob must lose one point per 200 ticks");
        cow.timeUntilRegen=0; cow.setHealth(1);
        state.tetanus$setInfection(400,2400); TetanusMod.TETANUS.applyUpdateEffect(cow,0);
        ctx.assertTrue(cow.getHealth()==1,"Gentle mob damage killed the mob");
        cow.setHealth(10); MobVariants.makePermanent(cow);
        TetanusMod.TETANUS.applyUpdateEffect(cow,0);
        ctx.assertTrue(state.tetanus$getTotal()==6000&&state.tetanus$getElapsed()==1,"Infinite infection progress keeps resetting");
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void legacyIronToolsPreserveNbtAndCanBeCleaned(TestContext ctx) {
        ctx.assertTrue(ModIronTools.PAIRS.size()==5,"Missing vanilla iron tool variants");
        for(var entry:ModIronTools.PAIRS.entrySet()) {
            ItemStack stack=new ItemStack(entry.getKey()); stack.setDamage(93); stack.addEnchantment(Enchantments.UNBREAKING,2);
            var rusty=RustingSystem.convert(stack,entry.getValue());
            ctx.assertTrue(rusty.isOf(entry.getValue())&&rusty.getDamage()==93&&rusty.hasEnchantments(),"Iron rust conversion lost data");
            ctx.assertTrue(RustingSystem.rusted(entry.getKey())==null,"Retired iron tools still acquired rust variants");
            ctx.assertTrue(RustingSystem.clean(rusty.getItem())==entry.getKey(),"Brush cannot restore iron tool");
            ctx.assertTrue(rusty.getMaxDamage()==250,"Rusted iron durability changed");
        }
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rustyIronTwentyPercentAndEnchantmentAnvil(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();
        var handler=new AnvilScreenHandler(1,player.getInventory());
        handler.getSlot(0).setStack(new ItemStack(Items.IRON_SWORD));
        handler.getSlot(1).setStack(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(ModItems.TETANUS_ENCHANTMENT,1)));
        ItemStack enchanted=handler.getSlot(2).getStack();
        ctx.assertTrue(EnchantmentHelper.getLevel(ModItems.TETANUS_ENCHANTMENT,enchanted)==1,"Tetanus book cannot be applied at an anvil");
        var target=cow(ctx); player.setStackInHand(Hand.MAIN_HAND,enchanted.copy());
        target.damage(ctx.getWorld().getDamageSources().playerAttack(player),1);
        ctx.assertTrue(target.hasStatusEffect(TetanusMod.TETANUS),"Enchanted melee hit failed to infect");
        player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModIronTools.SWORD));target.getRandom().setSeed(61823L); int infected=0;
        for(int i=0;i<200;i++) {
            target.setHealth(10); target.timeUntilRegen=0; target.removeStatusEffect(TetanusMod.TETANUS);
            target.damage(ctx.getWorld().getDamageSources().playerAttack(player),0.1F);
            if(target.hasStatusEffect(TetanusMod.TETANUS)) infected++;
        }
        ctx.assertTrue(infected>15&&infected<70,"Rusted iron infection rate is inconsistent with 20%: "+infected);
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void infinityStillConsumesTetanizedArrows(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer(); player.setPosition(ctx.getAbsolute(new Vec3d(3,3,3)));
        ItemStack bow=new ItemStack(Items.BOW); bow.addEnchantment(Enchantments.INFINITY,1);
        player.setStackInHand(Hand.MAIN_HAND,bow); player.setStackInHand(Hand.OFF_HAND,new ItemStack(ModItems.TETANIZED_ARROW,3));
        Items.BOW.onStoppedUsing(bow,ctx.getWorld(),player,72000-20);
        ctx.assertTrue(player.getOffHandStack().getCount()==2,"Infinity made infected arrows free");
        var arrows=ctx.getWorld().getEntitiesByType(TetanusMod.TETANIZED_ARROW_ENTITY,player.getBoundingBox().expand(4),e->true);
        ctx.assertTrue(arrows.size()==1,"Bow did not shoot custom arrow");
        ctx.assertTrue(arrows.get(0).pickupType==PersistentProjectileEntity.PickupPermission.ALLOWED,"Custom arrow pickup differs from ordinary arrows");
        arrows.forEach(Entity::discard); ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void infectedArrowPhysicalHit(TestContext ctx) {
        var target=cow(ctx);
        var arrow=new TetanizedArrowEntity(TetanusMod.TETANIZED_ARROW_ENTITY,ctx.getWorld());
        arrow.setPosition(ctx.getAbsolute(new Vec3d(3.5,3.7,2.5))); arrow.setVelocity(0,0,0.7); ctx.getWorld().spawnEntity(arrow);
        ctx.runAtTick(12,()->{
            ctx.assertTrue(target.hasStatusEffect(TetanusMod.TETANUS),"Arrow collision failed to infect");
            ctx.assertFalse(arrow.isAlive(),"Arrow survived living target impact"); ctx.complete();
        });
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void witherDoubleHealthAndLoot(TestContext ctx) {
        var wither=new WitherEntity(EntityType.WITHER,ctx.getWorld()); wither.setAiDisabled(true);
        wither.setPosition(ctx.getAbsolute(new Vec3d(3,3,3))); MobVariants.makePermanent(wither);
        ctx.assertTrue(wither.getMaxHealth()==600&&wither.getHealth()==600,"Tetanized wither must have 600 health");
        ctx.assertTrue(wither.getCustomName().getContent() instanceof net.minecraft.text.TranslatableTextContent text
                &&text.getKey().equals("entity.tetanusmod.tetanized_wither"),"Wither boss name missing");
        wither.damage(ctx.getWorld().getDamageSources().genericKill(),10000);
        var items=ctx.getWorld().getEntitiesByClass(ItemEntity.class,wither.getBoundingBox().expand(6),e->true);
        ctx.assertTrue(items.stream().anyMatch(e->e.getStack().isOf(Items.NETHER_STAR)),"Wither lost normal nether star");
        ctx.assertTrue(items.stream().anyMatch(e->e.getStack().isOf(Items.ENCHANTED_BOOK)
                &&EnchantmentHelper.get(e.getStack()).getOrDefault(ModItems.TETANUS_ENCHANTMENT,0)==1),"Wither did not drop Tetanus I");
        ctx.assertTrue(items.size()>=4,"Bonus wither loot missing"); ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void animalMeatCookingAndConsumption(TestContext ctx) {
        var target=cow(ctx); MobVariants.makePermanent(target); target.damage(ctx.getWorld().getDamageSources().genericKill(),1000);
        ctx.assertTrue(ctx.getWorld().getEntitiesByClass(ItemEntity.class,target.getBoundingBox().expand(5),
                e->e.getStack().isOf(ModItems.INFECTED_MEAT)).size()>0,"Animal did not drop infected meat");
        var player=ctx.createMockSurvivalPlayer();
        ModItems.INFECTED_MEAT.finishUsing(new ItemStack(ModItems.INFECTED_MEAT),ctx.getWorld(),player);
        ctx.assertTrue(player.hasStatusEffect(TetanusMod.SALMONELLA)&&!player.hasStatusEffect(TetanusMod.TETANUS),"Raw meat must cause Salmonella only");
        player.clearStatusEffects();
        ModItems.COOKED_INFECTED_MEAT.finishUsing(new ItemStack(ModItems.COOKED_INFECTED_MEAT),ctx.getWorld(),player);
        ctx.assertTrue(!player.hasStatusEffect(TetanusMod.TETANUS)&&!player.hasStatusEffect(TetanusMod.SALMONELLA),"Cooked meat still infects");
        ctx.assertTrue(ctx.getWorld().getRecipeManager().get(TetanusMod.id("cooked_infected_meat_smelting")).isPresent(),"Cooking recipe missing");
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void playerAdvancementsAndAudioHook(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld(); player.setPosition(ctx.getAbsolute(new Vec3d(3,3,3)));
        Infection.apply(player,null);
        var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id("infected"));
        ctx.assertTrue(advancement!=null&&player.getAdvancementTracker().getProgress(advancement).isDone(),"Infection advancement not awarded");
        var item=new ItemEntity(ctx.getWorld(),player.getX(),player.getY(),player.getZ(),new ItemStack(ModItems.RUSTED_NAIL));
        ModAdvancements.rusted(item);
        var rust=player.getServer().getAdvancementLoader().get(TetanusMod.id("rusty_business"));
        ctx.assertTrue(rust!=null&&player.getAdvancementTracker().getProgress(rust).isDone(),"Rust advancement not awarded");
        ctx.assertTrue(TetanusMod.CONFIG.infectionSoundVolume>0.85F,"Infection sound volume not increased");
        player.discard(); ctx.complete();
    }
}
