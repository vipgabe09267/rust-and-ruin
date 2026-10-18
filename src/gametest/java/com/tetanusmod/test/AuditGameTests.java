package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.Infection;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class AuditGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void legacyGolemHealthMigrates(TestContext ctx) {
        var golem=EntityType.IRON_GOLEM.create(ctx.getWorld());MobVariants.makePermanent(golem);
        var health=golem.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        var modifier=health.getModifiers().iterator().next();health.removeModifier(modifier);
        health.addPersistentModifier(new EntityAttributeModifier(modifier.getId(),"Legacy mutation",0.5,EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        var loaded=EntityType.IRON_GOLEM.create(ctx.getWorld());loaded.readNbt(golem.writeNbt(new NbtCompound()));
        MobVariants.loaded(loaded);MobVariants.loaded(loaded);
        ctx.assertTrue(loaded.getMaxHealth()==200,"An older tetanized golem retained 150 health instead of migrating to 200");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void repeatHitsKeepPermanentInfectionInfinite(TestContext ctx) {
        var cow=EntityType.COW.create(ctx.getWorld());MobVariants.makePermanent(cow);
        Infection.apply(cow,null);
        ctx.assertTrue(cow.hasStatusEffect(TetanusMod.TETANUS)&&cow.getStatusEffect(TetanusMod.TETANUS).getDuration()==-1,"A repeat hit downgraded permanent Tetanus to a temporary effect");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void spectatorInteractionsDoNotMutateOrCollect(TestContext ctx) {
        var player=new net.minecraft.entity.player.PlayerEntity(ctx.getWorld(),net.minecraft.util.math.BlockPos.ORIGIN,0,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"spectator-audit")) { @Override public boolean isSpectator(){return true;} @Override public boolean isCreative(){return false;} };
        try {
            ctx.assertTrue(player.isSpectator(),"Spectator fixture is invalid");
            var golem=ctx.spawnEntity(EntityType.IRON_GOLEM,new Vec3d(3,3,3));
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUST,2));
            UseEntityCallback.EVENT.invoker().interact(player,ctx.getWorld(),Hand.MAIN_HAND,golem,null);
            int rust=player.getMainHandStack().getCount();
            var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(5,3,5));
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE,2));
            UseEntityCallback.EVENT.invoker().interact(player,ctx.getWorld(),Hand.MAIN_HAND,cow,null);
            ctx.assertTrue(rust==2&&!MobVariants.permanent(golem)&&player.getMainHandStack().getCount()==2&&!player.getInventory().contains(new ItemStack(ModItems.COW_MANURE)),"Spectator mode changed the world or collected manure");
        }finally{player.discard();}
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void corruptedStarHasNormalStarExplosionProtection(TestContext ctx) {
        var damage=new DamageSource(ctx.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(DamageTypes.EXPLOSION));
        var ordinary=new ItemEntity(ctx.getWorld(),0,0,0,new ItemStack(Items.NETHER_STAR));ordinary.damage(damage,100);
        ctx.assertTrue(ordinary.isAlive(),"Vanilla Nether Star control did not resist explosion");
        var corrupted=new ItemEntity(ctx.getWorld(),0,0,0,new ItemStack(ModItems.TETANIZED_WITHER_STAR));corrupted.damage(damage,100);
        ctx.assertTrue(corrupted.isAlive(),"Tetanized Wither Star can be destroyed by nearby Wither skull explosions");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void vanillaConversionPreservesMutation(TestContext ctx) {
        var zombie=ctx.spawnEntity(EntityType.ZOMBIE,new Vec3d(3,3,3));
        MobVariants.makePermanent(zombie);
        var drowned=zombie.convertTo(EntityType.DROWNED,true);
        ctx.assertTrue(drowned!=null&&MobVariants.permanent(drowned)&&drowned.getMaxHealth()==30,"Vanilla mob conversion lost permanent mutation");ctx.complete();
    }
}