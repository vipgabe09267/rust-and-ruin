package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.beacon.*;
import com.tetanusmod.entity.*;
import com.tetanusmod.expansion.*;
import com.tetanusmod.registry.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.projectile.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

public class ExpansionRegressionGameTests implements FabricGameTest {
    private void awarded(TestContext ctx,ServerPlayerEntity player,String id,boolean expected){var a=player.getServer().getAdvancementLoader().get(TetanusMod.id(id));ctx.assertTrue(a!=null&&player.getAdvancementTracker().getProgress(a).isDone()==expected,"Wrong advancement: "+id);}
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void firedBowAndTridentRetainEnchantmentAfterWeaponSwitch(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();player.setPosition(ctx.getAbsolute(new Vec3d(3.5,4,3.5)));player.setNoGravity(true);player.setYaw(0);player.setPitch(0);
        for(Item weapon:new Item[]{Items.BOW,Items.TRIDENT}){
            var stack=new ItemStack(weapon);stack.addEnchantment(ModItems.TETANUS_ENCHANTMENT,2);player.setStackInHand(Hand.MAIN_HAND,stack);player.getInventory().setStack(1,new ItemStack(Items.ARROW,4));
            weapon.onStoppedUsing(stack,ctx.getWorld(),player,weapon.getMaxUseTime(stack)-25);player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);
            var shots=ctx.getWorld().getEntitiesByClass(ProjectileEntity.class,player.getBoundingBox().expand(4),p->p.getOwner()==player);
            ctx.assertTrue(shots.size()==1,"Weapon did not fire exactly one projectile: "+weapon);
            var shot=shots.get(0);ctx.assertTrue(((CombatInfection.RangedCarrier)shot).rustruin$weaponLevel()==2,"Fired projectile lost weapon level: "+weapon);
            var copy=(ProjectileEntity)shot.getType().create(ctx.getWorld());copy.readNbt(shot.writeNbt(new NbtCompound()));ctx.assertTrue(((CombatInfection.RangedCarrier)copy).rustruin$weaponLevel()==2,"Saved projectile lost weapon enchantment");
            var cow=EntityType.COW.create(ctx.getWorld());cow.damage(ctx.getWorld().getDamageSources().arrow((PersistentProjectileEntity)shot,player),1);ctx.assertTrue(cow.hasStatusEffect(ExpansionEffects.DISORIENTED),"Actual ranged damage did not apply Tetanus II");shot.discard();
        }
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void firedCrossbowSnapshotsTetanusTwo(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();player.setPosition(ctx.getAbsolute(new Vec3d(3.5,4,3.5)));var bow=new ItemStack(Items.CROSSBOW);bow.addEnchantment(ModItems.TETANUS_ENCHANTMENT,2);player.setStackInHand(Hand.MAIN_HAND,bow);player.getInventory().setStack(1,new ItemStack(Items.ARROW,4));
        Items.CROSSBOW.onStoppedUsing(bow,ctx.getWorld(),player,Items.CROSSBOW.getMaxUseTime(bow)-30);ctx.assertTrue(CrossbowItem.isCharged(bow),"Crossbow failed to load ordinary arrows");CrossbowItem.shootAll(ctx.getWorld(),player,Hand.MAIN_HAND,bow,3.15F,0);
        var shots=ctx.getWorld().getEntitiesByClass(ProjectileEntity.class,player.getBoundingBox().expand(4),p->p.getOwner()==player);ctx.assertTrue(shots.size()==1&&((CombatInfection.RangedCarrier)shots.get(0)).rustruin$weaponLevel()==2,"Crossbow shot lost enchantment level");shots.forEach(Entity::discard);ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void protectedShieldActuallyBlocksAnEnchantedAttack(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();player.changeGameMode(GameMode.SURVIVAL);player.getAbilities().creativeMode=false;player.getAbilities().invulnerable=false;((com.tetanusmod.test.mixin.ServerPlayerTestAccessor)player).rustruin$joinInvulnerability(0);player.setNoGravity(true);player.setPosition(ctx.getAbsolute(new Vec3d(3,4,3)));player.setYaw(0);
        var shield=new ItemStack(Items.SHIELD);shield.addEnchantment(ExpansionItems.TETANUS_PROTECTION,1);player.setStackInHand(Hand.OFF_HAND,shield);player.setCurrentHand(Hand.OFF_HAND);
        var zombie=ctx.spawnEntity(EntityType.ZOMBIE,new Vec3d(3,4,5));zombie.setAiDisabled(true);var sword=new ItemStack(Items.IRON_SWORD);sword.addEnchantment(ModItems.TETANUS_ENCHANTMENT,2);zombie.equipStack(EquipmentSlot.MAINHAND,sword);
        for(int i=0;i<6;i++)player.playerTick();ctx.assertTrue(player.isBlocking(),"Test player did not raise shield");float health=player.getHealth();player.damage(ctx.getWorld().getDamageSources().mobAttack(zombie),6);
        ctx.assertTrue(player.getHealth()==health&&!player.hasStatusEffect(TetanusMod.TETANUS),"Shield block allowed damage/infection");awarded(ctx,player,"shield_protection",true);player.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void returningUpwardRustyNailHasUniqueDeathAndAdvancement(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();player.changeGameMode(GameMode.SURVIVAL);player.getAbilities().creativeMode=false;player.getAbilities().invulnerable=false;((com.tetanusmod.test.mixin.ServerPlayerTestAccessor)player).rustruin$joinInvulnerability(0);player.setPosition(ctx.getAbsolute(new Vec3d(3,4,3)));player.setPitch(-90);player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUSTED_NAIL,2));
        ModItems.RUSTED_NAIL.use(ctx.getWorld(),player,Hand.MAIN_HAND);var nails=ctx.getWorld().getEntitiesByClass(NailEntity.class,player.getBoundingBox().expand(3),n->n.getOwner()==player);ctx.assertTrue(nails.size()==1&&nails.get(0).wasThrownUpward()&&player.getMainHandStack().getCount()==1,"Upward toss was not tracked/consumed");
        var source=ModDamage.nail(nails.get(0),player);ctx.assertTrue(source.getDeathMessage(player).getString().contains("rusty nail"),"Self nail death message missing");boolean pvp=ctx.getWorld().getServer().isPvpEnabled();try{ctx.getWorld().getServer().setPvpEnabled(true);ctx.assertTrue(player.damage(source,10000)&&!player.isAlive(),"Returning nail did not kill the test player");awarded(ctx,player,"why_did_i_do_that",true);}finally{ctx.getWorld().getServer().setPvpEnabled(pvp);}player.discard();nails.forEach(Entity::discard);ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void ordinarySelfNailDeathDoesNotAwardUpwardAdvancement(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();player.getAbilities().invulnerable=false;((com.tetanusmod.test.mixin.ServerPlayerTestAccessor)player).rustruin$joinInvulnerability(0);var nail=new NailEntity(ctx.getWorld(),player);player.damage(ModDamage.nail(nail,player),10000);awarded(ctx,player,"why_did_i_do_that",false);player.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void partialInventorySmithingConsumesOnlyTransferredArrows(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();for(int i=0;i<36;i++)player.getInventory().setStack(i,new ItemStack(Items.COBBLESTONE,64));player.getInventory().setStack(35,new ItemStack(ModItems.TETANIZED_ARROW,61));
        var menu=new SmithingScreenHandler(1,player.getInventory());menu.getSlot(1).setStack(new ItemStack(Items.ARROW,64));menu.getSlot(2).setStack(new ItemStack(ModItems.RUST,2));menu.quickMove(player,3);
        ctx.assertTrue(player.getInventory().count(ModItems.TETANIZED_ARROW)==64&&menu.getSlot(1).getStack().getCount()==61&&menu.getSlot(2).getStack().getCount()==1,"Partial inventory lost or duplicated arrows/Rust");menu.quickMove(player,3);ctx.assertTrue(menu.getSlot(1).getStack().getCount()==61&&menu.getSlot(2).getStack().getCount()==1,"Full inventory consumed recipe inputs");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void smithingAdvancementOnlyAwardsSuccessfulOutput(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();var menu=new SmithingScreenHandler(1,player.getInventory());menu.getSlot(1).setStack(new ItemStack(Items.ARROW,16));menu.getSlot(2).setStack(new ItemStack(ModItems.RUST));awarded(ctx,player,"rust_arrows",false);menu.quickMove(player,3);awarded(ctx,player,"rust_arrows",true);player.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void closedBearTrapPreservesVictimWhenItRusts(TestContext ctx){
        var pos=new BlockPos(3,3,3);ctx.setBlockState(pos,ExpansionBlocks.BEAR_TRAP);var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(3.5,3,3.5));var trap=(BearTrapBlockEntity)ctx.getBlockEntity(pos);trap.trigger(cow);ctx.setBlockState(pos.up(),Blocks.WATER);
        int ticks=TetanusMod.CONFIG.waterRustTicks;for(int i=0;i<ticks;i++)BearTrapBlockEntity.tick(ctx.getWorld(),ctx.getAbsolutePos(pos),ctx.getWorld().getBlockState(ctx.getAbsolutePos(pos)),trap);
        var rusty=(BearTrapBlockEntity)ctx.getBlockEntity(pos);ctx.assertTrue(ctx.getWorld().getBlockState(ctx.getAbsolutePos(pos)).isOf(ExpansionBlocks.RUSTED_BEAR_TRAP)&&rusty.createNbt().getUuid("Captured").equals(cow.getUuid()),"Rusting a closed trap lost captured victim");rusty.reset();ctx.assertFalse(cow.hasStatusEffect(StatusEffects.SLOWNESS),"Rusted trap could not release original victim");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabiesVaccineAlsoPreventsDirectPotionStatus(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();player.addStatusEffect(new StatusEffectInstance(ExpansionEffects.RABIES_RESISTANCE,24000));player.getRandom().setSeed(1);
        ctx.assertFalse(player.addStatusEffect(new StatusEffectInstance(ExpansionEffects.RABIES,-1)),"Vaccine failed potion-style exposure");ctx.assertFalse(player.hasStatusEffect(ExpansionEffects.RABIES),"Blocked potion still infected");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=180)
    public void realBeaconActivationKeepsAdvancementsSeparate(TestContext ctx){
        var pos=new BlockPos(4,6,4);for(int layer=1;layer<=4;layer++)for(int x=-layer;x<=layer;x++)for(int z=-layer;z<=layer;z++)ctx.setBlockState(pos.add(x,-layer,z),ExpansionBlocks.RUSTED_IRON_BLOCK);ctx.setBlockState(pos,ModBeacons.BLOCK);
        for(int y=7;y<ctx.getWorld().getTopY();y++)ctx.getWorld().setBlockState(ctx.getAbsolutePos(new BlockPos(4,y,4)),Blocks.AIR.getDefaultState());
        var player=ctx.createMockCreativeServerPlayerInWorld();player.setNoGravity(true);player.setPosition(ctx.getAbsolute(new Vec3d(4,7,4)));ModBeacons.BLOCK.onPlaced(ctx.getWorld(),ctx.getAbsolutePos(pos),ModBeacons.BLOCK.getDefaultState(),player,new ItemStack(ModBeacons.BLOCK));awarded(ctx,player,"beacon_placed",true);
        var beacon=(TetanizedBeaconEntity)ctx.getBlockEntity(pos);var nbt=beacon.createNbt();nbt.putInt("Primary",net.minecraft.registry.Registries.STATUS_EFFECT.getRawId(StatusEffects.HASTE));nbt.putInt("Secondary",net.minecraft.registry.Registries.STATUS_EFFECT.getRawId(StatusEffects.HASTE));beacon.readNbt(nbt);
        ctx.runAtTick(150,()->{ctx.assertTrue(player.hasStatusEffect(TetanusMod.VACCINATED)&&player.getStatusEffect(StatusEffects.HASTE).getAmplifier()==2,"Real beacon did not grant passive resistance/Haste III");awarded(ctx,player,"beacon_full",true);for(String id:new String[]{"nether/create_beacon","nether/create_full_beacon"}){var advancement=player.getServer().getAdvancementLoader().get(new Identifier(id));ctx.assertTrue(!player.getAdvancementTracker().getProgress(advancement).isDone(),"Custom beacon awarded vanilla advancement "+id);}player.discard();ctx.complete();});
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void creativeGlovePacketsRejectSurvivalAndWrongItems(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();var gloves=new ItemStack(ModItems.PROTECTIVE_GLOVES);gloves.setDamage(31);
        player.networkHandler.onCreativeInventoryAction(new net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket(46,gloves));ctx.assertTrue(GloveInventory.equipped(player).getDamage()==31,"Creative glove packet failed");
        for(var invalid:java.util.List.of(new ItemStack(Items.DIAMOND),new ItemStack(ModItems.PROTECTIVE_GLOVES,2)))player.networkHandler.onCreativeInventoryAction(new net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket(46,invalid));ctx.assertTrue(GloveInventory.equipped(player).getDamage()==31,"Invalid creative packet changed gloves");
        player.changeGameMode(GameMode.SURVIVAL);player.networkHandler.onCreativeInventoryAction(new net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket(46,ItemStack.EMPTY));ctx.assertTrue(GloveInventory.equipped(player).getDamage()==31,"Survival player could use creative glove packet");player.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void deathRespawnCuresRabiesEvenWithKeepInventory(TestContext ctx){
        var old=ctx.createMockCreativeServerPlayerInWorld();Rabies.expose(old,null);((ExpansionState)old).rustruin$setLostHealth(10);RabiesEffect.restorePenalty(old);
        var next=ctx.createMockCreativeServerPlayerInWorld();next.copyFrom(old,false);ctx.assertTrue(!next.hasStatusEffect(ExpansionEffects.RABIES)&&next.getMaxHealth()==20,"Rabies/heart penalty survived death respawn");old.discard();next.discard();ctx.complete();
    }    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabidMutationSurvivesVanillaMobConversion(TestContext ctx){
        var zombie=ctx.spawnEntity(EntityType.ZOMBIE,new Vec3d(3,4,3));Rabies.makeRabid(zombie);var drowned=zombie.convertTo(EntityType.DROWNED,true);
        ctx.assertTrue(drowned!=null&&Rabies.rabid(drowned)&&drowned.getMaxHealth()==50,"Rabid zombie conversion lost variant or stacked health");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabidHuntGoalIsInstalledAndAttacksPlayers(TestContext ctx){
        for(int x=2;x<=4;x++)for(int y=3;y<=5;y++)for(int z=2;z<=5;z++)ctx.setBlockState(x,y,z,Blocks.AIR);
        var player=new ServerPlayerEntity(ctx.getWorld().getServer(),ctx.getWorld(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"rabid-ai-test"));ctx.getWorld().getServer().getPlayerManager().onPlayerConnect(new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND),player);player.changeGameMode(GameMode.SURVIVAL);player.getAbilities().creativeMode=false;player.getAbilities().invulnerable=false;((com.tetanusmod.test.mixin.ServerPlayerTestAccessor)player).rustruin$joinInvulnerability(0);player.setNoGravity(true);player.setPosition(ctx.getAbsolute(new Vec3d(3.5,3,4.5)));
        var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(3.5,3,3.5));Rabies.makeRabid(cow);cow.setAiDisabled(true);var goals=((Rabies.MobGoals)cow).rustruin$goals().getGoals();var goal=goals.stream().map(net.minecraft.entity.ai.goal.PrioritizedGoal::getGoal).filter(g->g instanceof Rabies.RabidHuntGoal).findFirst().orElseThrow();
        ctx.runAtTick(2,()->{player.timeUntilRegen=0;player.setHealth(20);ctx.assertTrue(goal.canStart(),"Rabid cow did not target Survival player");goal.tick();ctx.assertTrue(player.getHealth()==15&&player.hasStatusEffect(ExpansionEffects.RABIES),"Rabid hunt AI did not deal five damage and expose player");player.discard();ctx.complete();});
    }    @GameTest(templateName=EMPTY_STRUCTURE)
    public void barbedWireCannotApplySymptomsToSpectators(TestContext ctx){
        var spectator=new net.minecraft.entity.player.PlayerEntity(ctx.getWorld(),BlockPos.ORIGIN,0,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"trap-spectator")){public boolean isSpectator(){return true;}public boolean isCreative(){return false;}};
        ctx.setBlockState(new BlockPos(3,3,3),ModBlocks.RUSTED_BARBED_WIRE);((com.tetanusmod.block.CaltropsBlockEntity)ctx.getBlockEntity(new BlockPos(3,3,3))).stepOn(spectator,true);
        ctx.assertTrue(!spectator.hasStatusEffect(StatusEffects.SLOWNESS)&&!spectator.hasStatusEffect(TetanusMod.TETANUS)&&spectator.getHealth()==20,"Barbed wire applied symptoms to spectator");ctx.complete();
    }    @GameTest(templateName=EMPTY_STRUCTURE)
    public void nailItemEnforcesItsOwnCooldown(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();player.setPosition(ctx.getAbsolute(new Vec3d(3,4,3)));player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.NAIL,4));ModItems.NAIL.use(ctx.getWorld(),player,Hand.MAIN_HAND);ModItems.NAIL.use(ctx.getWorld(),player,Hand.MAIN_HAND);
        ctx.assertTrue(player.getMainHandStack().getCount()==3&&ctx.getWorld().getEntitiesByClass(NailEntity.class,player.getBoundingBox().expand(3),n->n.getOwner()==player).size()==1,"Repeated item call bypassed nail cooldown");ctx.getWorld().getEntitiesByClass(NailEntity.class,player.getBoundingBox().expand(3),n->n.getOwner()==player).forEach(Entity::discard);ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void tetanusBooksRejectNailsAndHooklines(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();for(int level:new int[]{1,2})for(Item item:new Item[]{ModItems.NAIL,ModItems.RUSTED_NAIL,ModItems.BENT_NAIL,ModItems.RUSTED_BENT_NAIL,ModItems.RUSTY_HOOKLINE,Items.IRON_PICKAXE,Items.IRON_SHOVEL,Items.IRON_HOE}){var menu=new AnvilScreenHandler(1,player.getInventory());menu.getSlot(0).setStack(new ItemStack(item));menu.getSlot(1).setStack(ExpansionItems.tetanusBook(level));ctx.assertTrue(menu.getSlot(2).getStack().isEmpty(),"Tetanus book accepted excluded item: "+item);}ctx.complete();
    }}