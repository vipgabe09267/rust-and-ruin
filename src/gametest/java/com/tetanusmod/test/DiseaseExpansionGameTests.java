package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.*;
import com.tetanusmod.expansion.*;
import com.tetanusmod.entity.*;
import com.tetanusmod.registry.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.*;
import net.minecraft.enchantment.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.*;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

public class DiseaseExpansionGameTests implements FabricGameTest {
    private void awarded(TestContext ctx,ServerPlayerEntity player,String id,boolean expected){var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id(id));ctx.assertTrue(advancement!=null&&player.getAdvancementTracker().getProgress(advancement).isDone()==expected,"Wrong advancement state: "+id);}
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabiesRemovesOneHeartPerMinuteAndStopsAtTwo(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();player.setNoGravity(true);Rabies.expose(player,null);
        for(int minute=1;minute<=10;minute++){
            ((ExpansionState)player).rustruin$setRabiesTicks(1199);player.tick();
            ctx.assertTrue(player.getMaxHealth()==Math.max(4,20-minute*2),"Wrong Rabies heart capacity at minute "+minute);
            ctx.assertTrue(player.getHealth()>0&&player.hasStatusEffect(ExpansionEffects.RABIES),"Rabies killed or expired naturally");
        }
        ctx.assertTrue(player.getStatusEffect(StatusEffects.NAUSEA)!=null&&player.getStatusEffect(StatusEffects.NAUSEA).getDuration()>=199,"Two-heart phase did not apply ten seconds of nausea");
        player.removeStatusEffect(StatusEffects.NAUSEA);for(int tick=0;tick<1199;tick++)player.tick();
        ctx.assertFalse(player.hasStatusEffect(StatusEffects.NAUSEA),"Rabies nausea repeated before a minute");player.tick();ctx.assertTrue(player.hasStatusEffect(StatusEffects.NAUSEA),"Rabies nausea failed to repeat after a minute");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabiesSavesHeartLossAndOnlyRabiesCureRemovesIt(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();player.setNoGravity(true);Rabies.expose(player,null);var state=(ExpansionState)player;
        state.rustruin$setRabiesTicks(1199);player.tick();state.rustruin$setRabiesTicks(1199);player.tick();
        var copy=ctx.createMockSurvivalPlayer();copy.readNbt(player.writeNbt(new NbtCompound()));Rabies.loaded(copy);
        ctx.assertTrue(copy.getMaxHealth()==16&&((ExpansionState)copy).rustruin$getLostHealth()==4,"Rabies save lost heart penalty");
        Items.MILK_BUCKET.finishUsing(new ItemStack(Items.MILK_BUCKET),ctx.getWorld(),copy);ModItems.ANTITOXIN.finishUsing(new ItemStack(ModItems.ANTITOXIN),ctx.getWorld(),copy);
        ctx.assertTrue(copy.hasStatusEffect(ExpansionEffects.RABIES)&&copy.getMaxHealth()==16,"Milk or Tetanus Antitoxin cured Rabies");
        var result=ExpansionItems.RABIES_CURE.finishUsing(new ItemStack(ExpansionItems.RABIES_CURE),ctx.getWorld(),copy);
        ctx.assertTrue(result.isOf(Items.GLASS_BOTTLE)&&!copy.hasStatusEffect(ExpansionEffects.RABIES)&&copy.getMaxHealth()==20&&((ExpansionState)copy).rustruin$getLostHealth()==0,"Rabies Cure failed to restore heart capacity");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabiesVaccinePreventsButDoesNotCure(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();player.setNoGravity(true);var stack=new ItemStack(ExpansionItems.RABIES_VACCINE,2);ExpansionItems.RABIES_VACCINE.finishUsing(stack,ctx.getWorld(),player);
        ctx.assertTrue(stack.getCount()==1&&player.getInventory().count(Items.GLASS_BOTTLE)==1&&player.getStatusEffect(ExpansionEffects.RABIES_RESISTANCE).getDuration()==24000,"Rabies vaccine use accounting failed");
        player.getRandom().setSeed(1);ctx.assertFalse(Rabies.expose(player,null),"Deterministic protected exposure infected player");
        Items.MILK_BUCKET.finishUsing(new ItemStack(Items.MILK_BUCKET),ctx.getWorld(),player);ctx.assertTrue(Rabies.expose(player,null),"Unprotected exposure failed");
        ExpansionItems.RABIES_VACCINE.finishUsing(new ItemStack(ExpansionItems.RABIES_VACCINE),ctx.getWorld(),player);ctx.assertTrue(player.hasStatusEffect(ExpansionEffects.RABIES),"Rabies vaccine cured an existing infection");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabiesTonicsRejectWrongEmptyAndSpectatorUse(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();player.setNoGravity(true);Rabies.expose(player,null);var fake=new ItemStack(Items.DIAMOND);
        ExpansionItems.RABIES_CURE.finishUsing(fake,ctx.getWorld(),player);ExpansionItems.RABIES_VACCINE.finishUsing(ItemStack.EMPTY,ctx.getWorld(),player);
        ctx.assertTrue(player.hasStatusEffect(ExpansionEffects.RABIES)&&!player.hasStatusEffect(ExpansionEffects.RABIES_RESISTANCE)&&fake.getCount()==1,"Invalid tonic stack changed disease");
        var spectator=new net.minecraft.entity.player.PlayerEntity(ctx.getWorld(),BlockPos.ORIGIN,0,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"rabies-spectator")){public boolean isSpectator(){return true;}public boolean isCreative(){return false;}};
        var dose=new ItemStack(ExpansionItems.RABIES_VACCINE);ExpansionItems.RABIES_VACCINE.finishUsing(dose,ctx.getWorld(),spectator);ctx.assertTrue(dose.getCount()==1&&!spectator.hasStatusEffect(ExpansionEffects.RABIES_RESISTANCE),"Spectator consumed Rabies vaccine");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabidMobsHaveExtraHealthArmorAndSaveTheirVariant(TestContext ctx){
        for(var type:java.util.List.of(EntityType.COW,EntityType.ZOMBIE,EntityType.WOLF)){
            var mob=type.create(ctx.getWorld());float original=mob.getMaxHealth();double armor=mob.getAttributeValue(EntityAttributes.GENERIC_ARMOR);Rabies.makeRabid(mob);
            ctx.assertTrue(Rabies.rabid(mob)&&mob.getMaxHealth()==original+30&&mob.getAttributeValue(EntityAttributes.GENERIC_ARMOR)==armor+7,"Rabid attributes incorrect for "+type);
            var copy=type.create(ctx.getWorld());copy.readNbt(mob.writeNbt(new NbtCompound()));Rabies.loaded(copy);Rabies.loaded(copy);
            ctx.assertTrue(Rabies.rabid(copy)&&copy.getMaxHealth()==original+30,"Rabid save or attribute modifiers stacked");
        }
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void onlyRabidAnimalsDropRabidMeat(TestContext ctx){
        var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(3,4,3));Rabies.makeRabid(cow);cow.damage(ctx.getWorld().getDamageSources().genericKill(),10000);
        var drops=ctx.getWorld().getEntitiesByClass(ItemEntity.class,cow.getBoundingBox().expand(2),item->true);
        ctx.assertTrue(!drops.isEmpty()&&drops.stream().allMatch(item->item.getStack().isOf(ExpansionItems.RABID_MEAT)),"Rabid animal dropped ordinary loot");drops.forEach(Entity::discard);
        var zombie=ctx.spawnEntity(EntityType.ZOMBIE,new Vec3d(3,4,3));Rabies.makeRabid(zombie);zombie.damage(ctx.getWorld().getDamageSources().genericKill(),10000);
        ctx.assertTrue(ctx.getWorld().getEntitiesByClass(ItemEntity.class,zombie.getBoundingBox().expand(2),item->item.getStack().isOf(ExpansionItems.RABID_MEAT)).isEmpty(),"Rabid hostile dropped Rabid Meat");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabidMeleeInfectsAndCookedRabidMeatIsSafe(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();player.setNoGravity(true);var wolf=EntityType.WOLF.create(ctx.getWorld());Rabies.makeRabid(wolf);
        player.damage(ctx.getWorld().getDamageSources().mobAttack(wolf),5);ctx.assertTrue(player.hasStatusEffect(ExpansionEffects.RABIES),"Rabid melee did not transmit Rabies");
        RabiesEffect.cure(player);ExpansionItems.COOKED_RABID_MEAT.finishUsing(new ItemStack(ExpansionItems.COOKED_RABID_MEAT),ctx.getWorld(),player);ctx.assertFalse(player.hasStatusEffect(ExpansionEffects.RABIES),"Cooked Rabid Meat infected player");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void manureAndStandaloneLockjawHaveActualEffects(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();player.setNoGravity(true);Items.POTION.finishUsing(PotionUtil.setPotion(new ItemStack(Items.POTION),ModBrewing.MANURE),ctx.getWorld(),player);
        ctx.assertTrue(player.getStatusEffect(StatusEffects.NAUSEA).getDuration()==100,"Manure potion is not five seconds of nausea");
        for(int i=0;i<101;i++)player.tick();ctx.assertFalse(player.hasStatusEffect(StatusEffects.NAUSEA),"Manure nausea lasted too long");
        var lockjaw=BrewingRecipeRegistry.craft(new ItemStack(Items.PUFFERFISH),PotionUtil.setPotion(new ItemStack(Items.POTION),ModBrewing.TETANUS));
        ctx.assertTrue(PotionUtil.getPotion(lockjaw)==ModBrewing.LOCKJAW,"Lockjaw brewing failed");Items.POTION.finishUsing(lockjaw,ctx.getWorld(),player);
        for(int i=0;i<10;i++)player.tick();ctx.assertTrue(player.hasStatusEffect(TetanusMod.LOCKJAW)&&!player.hasStatusEffect(TetanusMod.TETANUS),"Standalone Lockjaw was removed by disease cleanup");
        Items.MILK_BUCKET.finishUsing(new ItemStack(Items.MILK_BUCKET),ctx.getWorld(),player);ctx.assertTrue(player.hasStatusEffect(TetanusMod.LOCKJAW),"Milk cured standalone Lockjaw");
        ModItems.ANTITOXIN.finishUsing(new ItemStack(ModItems.ANTITOXIN),ctx.getWorld(),player);ctx.assertFalse(player.hasStatusEffect(TetanusMod.LOCKJAW),"Antitoxin failed standalone Lockjaw");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabiesBrewingAndSplashPreservePermanentDisease(TestContext ctx){
        var potion=BrewingRecipeRegistry.craft(new ItemStack(ExpansionItems.RABID_MEAT),PotionUtil.setPotion(new ItemStack(Items.POTION),ModBrewing.MANURE));
        ctx.assertTrue(PotionUtil.getPotion(potion)==ModBrewing.RABIES,"Rabies brewing failed");
        var splash=BrewingRecipeRegistry.craft(new ItemStack(Items.GUNPOWDER),potion);ctx.assertTrue(splash.isOf(Items.SPLASH_POTION),"Rabies splash brewing failed");
        var player=ctx.createMockSurvivalPlayer();player.setNoGravity(true);Items.POTION.finishUsing(potion,ctx.getWorld(),player);ctx.assertTrue(player.getStatusEffect(ExpansionEffects.RABIES).isInfinite(),"Drinkable Rabies was not permanent");
        var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(3,4,3));cow.setAiDisabled(true);cow.setNoGravity(true);
        ctx.runAtTick(2,()->{new TestSplash(ctx.getWorld(),splash).hit(cow);ctx.assertTrue(cow.hasStatusEffect(ExpansionEffects.RABIES)&&cow.getStatusEffect(ExpansionEffects.RABIES).isInfinite(),"Splash Rabies lost permanent duration");ctx.complete();});
    }
    private static final class TestSplash extends net.minecraft.entity.projectile.thrown.PotionEntity {TestSplash(net.minecraft.world.World world,ItemStack stack){super(EntityType.POTION,world);setItem(stack);}void hit(LivingEntity entity){setPosition(entity.getPos());onCollision(new net.minecraft.util.hit.EntityHitResult(entity));}}
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void tetanusTwoAndShieldBooksApplyAtAnAnvil(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();player.setNoGravity(true);
        for(Item weapon:new Item[]{Items.IRON_SWORD,Items.BOW,Items.CROSSBOW,Items.TRIDENT,ExpansionItems.SPEAR,Items.IRON_AXE,ModItems.REBAR,ModItems.METAL_PIPE}){
            var anvil=new AnvilScreenHandler(1,player.getInventory());anvil.getSlot(0).setStack(new ItemStack(weapon));anvil.getSlot(1).setStack(ExpansionItems.tetanusBook(2));
            ctx.assertTrue(EnchantmentHelper.getLevel(ModItems.TETANUS_ENCHANTMENT,anvil.getSlot(2).getStack())==2,"Tetanus II rejected combat item "+weapon);
        }
        var anvil=new AnvilScreenHandler(1,player.getInventory());anvil.getSlot(0).setStack(new ItemStack(Items.SHIELD));anvil.getSlot(1).setStack(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(ExpansionItems.TETANUS_PROTECTION,1)));
        ctx.assertTrue(EnchantmentHelper.getLevel(ExpansionItems.TETANUS_PROTECTION,anvil.getSlot(2).getStack())==1,"Shield protection book failed");
        ctx.assertFalse(ExpansionItems.TETANUS_PROTECTION.isAcceptableItem(new ItemStack(Items.IRON_SWORD)),"Shield enchantment accepts swords");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void tetanusTwoCausesDisorientationAndPeriodicTwoDamage(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();player.setNoGravity(true);var sword=new ItemStack(Items.IRON_SWORD);sword.addEnchantment(ModItems.TETANUS_ENCHANTMENT,2);player.setStackInHand(Hand.MAIN_HAND,sword);
        var cow=EntityType.COW.create(ctx.getWorld());cow.damage(ctx.getWorld().getDamageSources().playerAttack(player),1);
        ctx.assertTrue(cow.hasStatusEffect(TetanusMod.TETANUS)&&cow.getStatusEffect(TetanusMod.TETANUS).getAmplifier()==1&&cow.hasStatusEffect(ExpansionEffects.DISORIENTED),"Tetanus II did not inflict disorientation");
        ctx.assertTrue(cow.getStatusEffect(ExpansionEffects.DISORIENTED).getDuration()==cow.getStatusEffect(TetanusMod.TETANUS).getDuration(),"Tetanus II symptoms ended early");Items.MILK_BUCKET.finishUsing(new ItemStack(Items.MILK_BUCKET),ctx.getWorld(),cow);ctx.assertTrue(cow.hasStatusEffect(ExpansionEffects.DISORIENTED),"Milk removed Tetanus II symptoms");ModItems.ANTITOXIN.finishUsing(new ItemStack(ModItems.ANTITOXIN),ctx.getWorld(),cow);ctx.assertFalse(cow.hasStatusEffect(ExpansionEffects.DISORIENTED),"Antitoxin left Tetanus II symptoms");
        var victim=ctx.createMockSurvivalPlayer();victim.setNoGravity(true);victim.addStatusEffect(new StatusEffectInstance(ExpansionEffects.DISORIENTED,400));victim.setHealth(20);
        for(int i=0;i<200;i++)victim.tick();ctx.assertTrue(victim.getHealth()==18&&victim.hasStatusEffect(StatusEffects.NAUSEA),"Disorientation periodic damage/nausea incorrect: "+victim.getHealth());ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void disorientedMobsSometimesMissAttacks(TestContext ctx){
        var zombie=EntityType.ZOMBIE.create(ctx.getWorld());zombie.addStatusEffect(new StatusEffectInstance(ExpansionEffects.DISORIENTED,2000));zombie.getRandom().setSeed(7);var cow=EntityType.COW.create(ctx.getWorld());int misses=0,hits=0;
        for(int i=0;i<100;i++){cow.setHealth(10);cow.timeUntilRegen=0;if(zombie.tryAttack(cow))hits++;else misses++;}
        ctx.assertTrue(hits>20&&misses>20,"Disoriented mob did not alternate successful/missed attacks: "+hits+"/"+misses);ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void shieldAndGlovePreventionHaveExactBoundariesAndAdvancements(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();player.getAbilities().creativeMode=false;var shield=new ItemStack(Items.SHIELD);shield.addEnchantment(ExpansionItems.TETANUS_PROTECTION,1);player.setStackInHand(Hand.OFF_HAND,shield);
        ctx.assertFalse(InfectionProtection.allows(player,true,0.649F,1),"Shield failed 65% I protection");ctx.assertTrue(InfectionProtection.allows(player,true,0.65F,1),"Shield exceeded I chance");
        ctx.assertFalse(InfectionProtection.allows(player,true,0.399F,2),"Shield failed 40% II protection");ctx.assertTrue(InfectionProtection.allows(player,true,0.4F,2),"Shield exceeded II chance");awarded(ctx,player,"shield_protection",true);
        player.setStackInHand(Hand.OFF_HAND,ItemStack.EMPTY);((GloveInventory)player).rustruin$gloves().setStack(0,new ItemStack(ModItems.PROTECTIVE_GLOVES));
        ctx.assertFalse(InfectionProtection.allows(player,true,0,0),"Gloves failed deterministic prevention");awarded(ctx,player,"glove_protection",true);player.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void cureAdvancementRequiresActualDisease(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();ModItems.ANTITOXIN.finishUsing(new ItemStack(ModItems.ANTITOXIN),ctx.getWorld(),player);awarded(ctx,player,"antitoxin",false);
        player.addStatusEffect(new StatusEffectInstance(TetanusMod.LOCKJAW,1200));ModItems.ANTITOXIN.finishUsing(new ItemStack(ModItems.ANTITOXIN),ctx.getWorld(),player);awarded(ctx,player,"antitoxin",true);
        Rabies.expose(player,null);awarded(ctx,player,"rabies",true);ExpansionItems.RABIES_CURE.finishUsing(new ItemStack(ExpansionItems.RABIES_CURE),ctx.getWorld(),player);awarded(ctx,player,"rabies_cured",true);player.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void weakArrowsAndCombinedEnchantedArrowsHaveDifferentStrength(TestContext ctx){
        float chance=TetanusMod.CONFIG.animalConversionChance;int min=TetanusMod.CONFIG.minDurationSeconds,max=TetanusMod.CONFIG.maxDurationSeconds;
        try{
            TetanusMod.CONFIG.animalConversionChance=0;TetanusMod.CONFIG.minDurationSeconds=300;TetanusMod.CONFIG.maxDurationSeconds=300;
            var cow=EntityType.COW.create(ctx.getWorld());var arrow=new TetanizedArrowEntity(TetanusMod.TETANIZED_ARROW_ENTITY,ctx.getWorld());
            var source=new net.minecraft.entity.damage.DamageSource(ctx.getWorld().getRegistryManager().get(net.minecraft.registry.RegistryKeys.DAMAGE_TYPE).entryOf(net.minecraft.entity.damage.DamageTypes.ARROW),arrow,null);
            CombatInfection.rangedHit(cow,source);ctx.assertTrue(cow.getStatusEffect(TetanusMod.TETANUS).getDuration()==1200&&!cow.hasStatusEffect(ExpansionEffects.DISORIENTED),"Unenchanted infected arrow was not weaker");
            ((CombatInfection.RangedCarrier)(Object)arrow).rustruin$weaponLevel(2);CombatInfection.rangedHit(cow,source);
            ctx.assertTrue(cow.getStatusEffect(TetanusMod.TETANUS).getDuration()==7500&&cow.getStatusEffect(ExpansionEffects.DISORIENTED).getDuration()==7500,"Combined arrow and Tetanus II failed bonus duration");
        }finally{TetanusMod.CONFIG.animalConversionChance=chance;TetanusMod.CONFIG.minDurationSeconds=min;TetanusMod.CONFIG.maxDurationSeconds=max;}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void blockedCombinedArrowCannotExtendExistingDisease(TestContext ctx){
        var cow=EntityType.COW.create(ctx.getWorld());Infection.apply(cow,null);int original=cow.getStatusEffect(TetanusMod.TETANUS).getDuration();cow.addStatusEffect(new StatusEffectInstance(TetanusMod.VACCINATED,24000));
        float chance=TetanusMod.CONFIG.vaccineProtectionChance;try{TetanusMod.CONFIG.vaccineProtectionChance=1;var arrow=new TetanizedArrowEntity(TetanusMod.TETANIZED_ARROW_ENTITY,ctx.getWorld());((CombatInfection.RangedCarrier)(Object)arrow).rustruin$weaponLevel(2);CombatInfection.rangedHit(cow,new net.minecraft.entity.damage.DamageSource(ctx.getWorld().getRegistryManager().get(net.minecraft.registry.RegistryKeys.DAMAGE_TYPE).entryOf(net.minecraft.entity.damage.DamageTypes.ARROW),arrow,null));ctx.assertTrue(cow.getStatusEffect(TetanusMod.TETANUS).getDuration()==original&&!cow.hasStatusEffect(ExpansionEffects.DISORIENTED),"Prevented arrow refreshed illness or disoriented target");}finally{TetanusMod.CONFIG.vaccineProtectionChance=chance;}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void withersAlwaysDropExactlyOneBookWithRareUpgrade(TestContext ctx){
        var random=new java.util.Random(55);int rare=0;
        for(int i=0;i<40;i++){
            var wither=ctx.spawnEntity(EntityType.WITHER,new Vec3d(3,4,3));wither.setAiDisabled(true);wither.getRandom().setSeed(random.nextLong());MobVariants.makePermanent(wither);wither.damage(ctx.getWorld().getDamageSources().genericKill(),10000);
            var drops=ctx.getWorld().getEntitiesByClass(ItemEntity.class,wither.getBoundingBox().expand(4),item->true);
            ctx.assertTrue(drops.stream().filter(item->EnchantmentHelper.get(item.getStack()).getOrDefault(ModItems.TETANUS_ENCHANTMENT,0)>0).mapToInt(item->item.getStack().getCount()).sum()==1,"Wither must drop exactly one book, with II replacing I");
            if(drops.stream().anyMatch(item->EnchantmentHelper.get(item.getStack()).getOrDefault(ModItems.TETANUS_ENCHANTMENT,0)==2))rare++;
            drops.forEach(Entity::discard);wither.discard();
        }
        ctx.assertTrue(rare>=10&&rare<=30,"Replacement Tetanus II chance outside expected seeded range: "+rare);ctx.complete();
    }
}