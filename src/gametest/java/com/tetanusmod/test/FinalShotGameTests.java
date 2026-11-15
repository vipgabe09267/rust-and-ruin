package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.expansion.*;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/** Native firing and weapon destruction, with every projectile checked independently. */
public final class FinalShotGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="final_shot_bow")
    public void breakingBowKeepsTetanusTwoOnItsRealLastArrowAndPlainControlStaysPlain(TestContext ctx){
        for(boolean enchanted:new boolean[]{true,false}){
            var player=player(ctx);var bow=weapon(Items.BOW,enchanted,false);bow.setDamage(bow.getMaxDamage()-1);player.setStackInHand(Hand.MAIN_HAND,bow);player.getInventory().setStack(1,new ItemStack(Items.ARROW,4));
            try{Items.BOW.onStoppedUsing(bow,ctx.getWorld(),player,Items.BOW.getMaxUseTime(bow)-25);var shots=shots(ctx,player);ctx.assertTrue(bow.isEmpty()&&shots.size()==1,"Native final bow shot did not break and fire exactly once");checkAndHit(ctx,shots,player,enchanted?2:0);}finally{clear(ctx,player);}
        }ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="final_shot_crossbow")
    public void breakingCrossbowKeepsTetanusTwoOnItsLastArrowAndPlainControlStaysPlain(TestContext ctx){
        for(boolean enchanted:new boolean[]{true,false}){
            var player=player(ctx);var bow=weapon(Items.CROSSBOW,enchanted,false);bow.setDamage(bow.getMaxDamage()-1);player.setStackInHand(Hand.MAIN_HAND,bow);player.getInventory().setStack(1,new ItemStack(Items.ARROW,4));
            try{loadAndFire(ctx,player,bow);var shots=shots(ctx,player);ctx.assertTrue(bow.isEmpty()&&shots.size()==1,"Native final crossbow shot did not break and fire exactly once");checkAndHit(ctx,shots,player,enchanted?2:0);}finally{clear(ctx,player);}
        }ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="final_shot_multishot")
    public void everyMultishotArrowRetainsTheEnchantmentWhenTheFirstArrowBreaksTheCrossbow(TestContext ctx){
        for(boolean enchanted:new boolean[]{true,false}){
            var player=player(ctx);var bow=weapon(Items.CROSSBOW,enchanted,true);bow.setDamage(bow.getMaxDamage()-1);player.setStackInHand(Hand.MAIN_HAND,bow);player.getInventory().setStack(1,new ItemStack(Items.ARROW,4));
            try{loadAndFire(ctx,player,bow);var shots=shots(ctx,player);ctx.assertTrue(bow.isEmpty()&&shots.size()==3,"Broken native Multishot crossbow failed to emit its complete volley");checkAndHit(ctx,shots,player,enchanted?2:0);}finally{clear(ctx,player);}
        }ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="final_shot_firework")
    public void allThreeNativeFireworksKeepTheirSnapshotWhenTheFirstRocketBreaksTheCrossbow(TestContext ctx){
        for(boolean enchanted:new boolean[]{true,false}){
            var player=player(ctx);var bow=weapon(Items.CROSSBOW,enchanted,true);bow.setDamage(bow.getMaxDamage()-1);player.setStackInHand(Hand.MAIN_HAND,bow);player.setStackInHand(Hand.OFF_HAND,new ItemStack(Items.FIREWORK_ROCKET,4));
            try{loadAndFire(ctx,player,bow);var shots=shots(ctx,player);ctx.assertTrue(bow.isEmpty()&&shots.size()==3&&shots.stream().allMatch(p->p instanceof FireworkRocketEntity),"Native final Multishot firework volley was not actually fired");for(var shot:shots)checkSnapshot(ctx,shot,enchanted?2:0);}finally{clear(ctx,player);}
        }ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="final_shot_compatibility")
    public void normalTridentAndUnbrokenMultishotStillSnapshotWithoutCrossActionLeakage(TestContext ctx){
        for(boolean enchanted:new boolean[]{true,false}){
            var player=player(ctx);var trident=weapon(Items.TRIDENT,enchanted,false);trident.setDamage(trident.getMaxDamage()-2);player.setStackInHand(Hand.MAIN_HAND,trident);
            try{ctx.assertTrue(Items.TRIDENT.use(ctx.getWorld(),player,Hand.MAIN_HAND).getResult().isAccepted(),"Vanilla near-break trident use was unexpectedly refused");Items.TRIDENT.onStoppedUsing(trident,ctx.getWorld(),player,Items.TRIDENT.getMaxUseTime(trident)-25);var shots=shots(ctx,player);ctx.assertTrue(shots.size()==1&&shots.get(0) instanceof TridentEntity,"Native compatible trident throw failed");checkAndHit(ctx,shots,player,enchanted?2:0);}finally{clear(ctx,player);}
            player=player(ctx);var bow=weapon(Items.CROSSBOW,enchanted,true);player.setStackInHand(Hand.MAIN_HAND,bow);player.getInventory().setStack(1,new ItemStack(Items.ARROW,4));
            try{loadAndFire(ctx,player,bow);var shots=shots(ctx,player);ctx.assertTrue(!bow.isEmpty()&&shots.size()==3,"Normal Multishot compatibility fixture failed");checkAndHit(ctx,shots,player,enchanted?2:0);}finally{clear(ctx,player);}
        }ctx.complete();
    }
    private static ItemStack weapon(Item item,boolean enchanted,boolean multishot){var stack=new ItemStack(item);if(enchanted)stack.addEnchantment(ModItems.TETANUS_ENCHANTMENT,2);if(multishot)stack.addEnchantment(Enchantments.MULTISHOT,1);return stack;}
    private static PlayerEntity player(TestContext ctx){var p=ctx.createMockSurvivalPlayer();p.getInventory().clear();p.setPosition(ctx.getAbsolute(new Vec3d(3.5,4,3.5)));p.setNoGravity(true);p.setYaw(0);p.setPitch(0);ctx.assertTrue(!p.getAbilities().creativeMode,"Firing fixture must consume native durability");return p;}
    private static void loadAndFire(TestContext ctx,PlayerEntity player,ItemStack bow){Items.CROSSBOW.onStoppedUsing(bow,ctx.getWorld(),player,Items.CROSSBOW.getMaxUseTime(bow)-30);ctx.assertTrue(CrossbowItem.isCharged(bow),"Native crossbow failed to load its real ammunition");CrossbowItem.shootAll(ctx.getWorld(),player,Hand.MAIN_HAND,bow,3.15F,0);}
    private static List<ProjectileEntity> shots(TestContext ctx,PlayerEntity player){return ctx.getWorld().getEntitiesByClass(ProjectileEntity.class,player.getBoundingBox().expand(5),p->p.getOwner()==player);}
    private static void checkSnapshot(TestContext ctx,ProjectileEntity shot,int expected){ctx.assertTrue(((CombatInfection.RangedCarrier)shot).rustruin$weaponLevel()==expected,"Actual projectile lost or borrowed the firing enchantment: "+shot.getType());var copy=(ProjectileEntity)shot.getType().create(ctx.getWorld());try{copy.readNbt(shot.writeNbt(new NbtCompound()));ctx.assertTrue(((CombatInfection.RangedCarrier)copy).rustruin$weaponLevel()==expected,"Saved final projectile lost its snapshot");}finally{copy.discard();}}
    private static void checkAndHit(TestContext ctx,List<ProjectileEntity> shots,PlayerEntity player,int expected){for(var shot:shots){checkSnapshot(ctx,shot,expected);var cow=EntityType.COW.create(ctx.getWorld());var source=shot instanceof TridentEntity trident?ctx.getWorld().getDamageSources().trident(trident,player):ctx.getWorld().getDamageSources().arrow((PersistentProjectileEntity)shot,player);ctx.assertTrue(cow.damage(source,1),"Native projectile damage was not accepted");if(expected==2)ctx.assertTrue(cow.hasStatusEffect(TetanusMod.TETANUS)&&cow.getStatusEffect(TetanusMod.TETANUS).getAmplifier()==1&&cow.hasStatusEffect(ExpansionEffects.DISORIENTED),"Final native hit did not apply advanced Tetanus");else ctx.assertFalse(cow.hasStatusEffect(TetanusMod.TETANUS)||cow.hasStatusEffect(ExpansionEffects.DISORIENTED),"Plain projectile borrowed the previous action's infection");cow.discard();}}
    private static void clear(TestContext ctx,PlayerEntity player){shots(ctx,player).forEach(Entity::discard);player.discard();}
}