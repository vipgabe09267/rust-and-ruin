package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.beacon.*;
import com.tetanusmod.effect.*;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.screen.*;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import java.util.Optional;

public class FinalAuditGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void beaconRejectsForgedTiersWithoutTakingPayment(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();
        var menu=new TetanizedBeaconScreenHandler(1,player.getInventory());
        menu.getSlot(0).setStack(new ItemStack(Items.NETHERITE_INGOT));
        for(int level=0;level<=4;level++) {
            menu.setProperty(0,level);
            for(var effect:new StatusEffect[]{TetanusMod.TETANUS,StatusEffects.REGENERATION,StatusEffects.POISON}) {
                menu.setEffects(Optional.of(effect),Optional.empty());
                ctx.assertTrue(menu.getSlot(0).getStack().isOf(Items.NETHERITE_INGOT)&&menu.getPrimaryEffect()==null,"Forged primary consumed payment or selected a power");
            }
        }
        menu.setProperty(0,1);
        menu.setEffects(Optional.of(StatusEffects.STRENGTH),Optional.empty());
        menu.setEffects(Optional.of(StatusEffects.SPEED),Optional.of(StatusEffects.REGENERATION));
        ctx.assertTrue(menu.hasPayment()&&menu.getPrimaryEffect()==null,"Insufficient pyramid tier accepted");
        menu.setProperty(0,4);
        menu.setEffects(Optional.of(StatusEffects.HASTE),Optional.of(StatusEffects.STRENGTH));
        ctx.assertTrue(menu.hasPayment()&&menu.getPrimaryEffect()==null,"Unrelated secondary accepted");
        menu.setEffects(Optional.of(StatusEffects.HASTE),Optional.of(StatusEffects.REGENERATION));
        ctx.assertTrue(!menu.hasPayment()&&menu.getPrimaryEffect()==StatusEffects.HASTE&&menu.getSecondaryEffect()==StatusEffects.REGENERATION,"Valid full-pyramid selection rejected");
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void beaconTierUnlocksAndCorruptedPrimary(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();player.setPosition(ctx.getAbsolute(new Vec3d(3,3,3)));
        try {
            for(int level=1;level<=4;level++) {
                for(var primary:new StatusEffect[]{StatusEffects.SPEED,StatusEffects.HASTE,StatusEffects.RESISTANCE,StatusEffects.JUMP_BOOST,StatusEffects.STRENGTH}) {
                    int required=primary==StatusEffects.STRENGTH?3:primary==StatusEffects.RESISTANCE||primary==StatusEffects.JUMP_BOOST?2:1;
                    var menu=new TetanizedBeaconScreenHandler(1,player.getInventory());menu.setProperty(0,level);menu.getSlot(0).setStack(new ItemStack(Items.NETHERITE_INGOT));
                    menu.setEffects(Optional.of(primary),Optional.empty());
                    ctx.assertTrue(menu.hasPayment()==(level<required),"Wrong tier for "+primary+" at "+level);
                }
            }
            BeaconPower.apply(ctx.getWorld(),player.getBlockPos(),4,StatusEffects.POISON,null);
            BeaconPower.apply(ctx.getWorld(),player.getBlockPos(),1,StatusEffects.STRENGTH,null);
            BeaconPower.apply(ctx.getWorld(),player.getBlockPos(),4,StatusEffects.SPEED,StatusEffects.POISON);
            ctx.assertTrue(!player.hasStatusEffect(StatusEffects.POISON)&&!player.hasStatusEffect(StatusEffects.STRENGTH),"Forged or under-tier saved primary was applied");
            BeaconPower.apply(ctx.getWorld(),player.getBlockPos(),3,StatusEffects.STRENGTH,null);
            ctx.assertTrue(player.hasStatusEffect(StatusEffects.STRENGTH)&&player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier()==1,"Valid Strength II was not applied");
        }finally{player.discard();}
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void offhandHooklineChargesOnlyOneExposure(TestContext ctx) {
        for(int x=2;x<=4;x++)for(int y=3;y<=5;y++)for(int z=1;z<=6;z++)ctx.setBlockState(x,y,z,net.minecraft.block.Blocks.AIR);
        var player=ctx.createMockSurvivalPlayer();player.setPosition(ctx.getAbsolute(new Vec3d(3.5,3,1.5)));player.setYaw(0);player.setPitch(0);
        player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUSTED_REBAR));player.setStackInHand(Hand.OFF_HAND,new ItemStack(ModItems.RUSTY_HOOKLINE));
        var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(3.5,3.5,5.5));cow.setAiDisabled(true);cow.setNoGravity(true);
        cow.equipStack(EquipmentSlot.OFFHAND,new ItemStack(ModItems.PROTECTIVE_GLOVES));
        ctx.runAtTick(2,()->{
            float glove=TetanusMod.CONFIG.gloveProtectionChance,animal=TetanusMod.CONFIG.animalConversionChance;
            try {
                TetanusMod.CONFIG.gloveProtectionChance=0;TetanusMod.CONFIG.animalConversionChance=0;
                ModItems.RUSTY_HOOKLINE.use(ctx.getWorld(),player,Hand.OFF_HAND);
                ctx.assertTrue(cow.getHealth()==9&&cow.hasStatusEffect(TetanusMod.TETANUS),"Offhand hookline hit or infection failed");
                ctx.assertTrue(cow.getOffHandStack().getDamage()==1,"Hookline charged two exposures from main-hand weapon");
                ctx.assertTrue(player.getOffHandStack().getDamage()==1&&player.getMainHandStack().getDamage()==0,"Wrong hand charged durability");
            }finally{TetanusMod.CONFIG.gloveProtectionChance=glove;TetanusMod.CONFIG.animalConversionChance=animal;}
            ctx.complete();
        });
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void itemDamageScopeRestoresAfterExceptions(TestContext ctx) {
        ctx.assertFalse(Infection.meleeSuppressed(),"Scope began suppressed");
        try {
            Infection.withoutMeleeExposure(()->{
                ctx.assertTrue(Infection.meleeSuppressed(),"Active-item scope not set");
                Infection.withoutMeleeExposure(()->true);
                ctx.assertTrue(Infection.meleeSuppressed(),"Nested scope cleared its parent");
                throw new IllegalStateException("fixture");
            });
        }catch(IllegalStateException expected){}
        ctx.assertFalse(Infection.meleeSuppressed(),"Scope leaked to later ordinary melee hits");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void antitoxinRejectsInvalidAndSpectatorUse(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();Infection.apply(player,null);
        var diamond=new ItemStack(Items.DIAMOND);
        ModItems.ANTITOXIN.finishUsing(ItemStack.EMPTY,ctx.getWorld(),player);ModItems.ANTITOXIN.finishUsing(diamond,ctx.getWorld(),player);
        ctx.assertTrue(player.hasStatusEffect(TetanusMod.TETANUS)&&diamond.getCount()==1&&player.getInventory().count(Items.GLASS_BOTTLE)==0,"Invalid stack cured disease or produced a bottle");
        var spectator=new net.minecraft.entity.player.PlayerEntity(ctx.getWorld(),BlockPos.ORIGIN,0,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"antitoxin-spectator")) {
            public boolean isSpectator(){return true;}public boolean isCreative(){return false;}
        };
        spectator.addStatusEffect(new StatusEffectInstance(TetanusMod.TETANUS,6000));var dose=new ItemStack(ModItems.ANTITOXIN);
        ModItems.ANTITOXIN.finishUsing(dose,ctx.getWorld(),spectator);
        ctx.assertTrue(dose.getCount()==1&&spectator.hasStatusEffect(TetanusMod.TETANUS),"Spectator consumed antitoxin");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void antitoxinContainersAndFullInventory(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);player.getAbilities().creativeMode=false;
        try {
            player.getInventory().clear();var doses=new ItemStack(ModItems.ANTITOXIN,2);Infection.apply(player,null);
            ctx.assertTrue(ModItems.ANTITOXIN.finishUsing(doses,ctx.getWorld(),player)==doses&&doses.getCount()==1&&player.getInventory().count(Items.GLASS_BOTTLE)==1,"Stacked antitoxin bottle accounting failed");
            ctx.assertFalse(player.hasStatusEffect(TetanusMod.TETANUS),"Valid antitoxin did not cure");
            for(int i=0;i<player.getInventory().main.size();i++)player.getInventory().main.set(i,new ItemStack(Items.COBBLESTONE,64));
            var second=new ItemStack(ModItems.ANTITOXIN,2);ModItems.ANTITOXIN.finishUsing(second,ctx.getWorld(),player);
            var drops=ctx.getWorld().getEntitiesByClass(ItemEntity.class,player.getBoundingBox().expand(2),e->e.getStack().isOf(Items.GLASS_BOTTLE));
            ctx.assertTrue(second.getCount()==1&&drops.stream().mapToInt(e->e.getStack().getCount()).sum()==1,"Full inventory lost or duplicated antitoxin bottle");drops.forEach(Entity::discard);
            var last=ModItems.ANTITOXIN.finishUsing(new ItemStack(ModItems.ANTITOXIN),ctx.getWorld(),player);
            ctx.assertTrue(last.isOf(Items.GLASS_BOTTLE)&&last.getCount()==1,"Last antitoxin did not return a bottle");
        }finally{player.discard();}
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void reflectedThornsCannotInheritRustyWeaponInfection(TestContext ctx) {
        var wearer=ctx.createMockSurvivalPlayer();wearer.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUSTED_REBAR));
        var target=EntityType.COW.create(ctx.getWorld());
        target.damage(ctx.getWorld().getDamageSources().thorns(wearer),1);
        ctx.assertTrue(target.getHealth()==9&&!target.hasStatusEffect(TetanusMod.TETANUS),"Reflected armor damage inherited main-hand Tetanus");
        target.timeUntilRegen=0;target.damage(ctx.getWorld().getDamageSources().playerAttack(wearer),1);
        ctx.assertTrue(target.hasStatusEffect(TetanusMod.TETANUS),"Ordinary rusty melee stopped infecting");ctx.complete();
    }}