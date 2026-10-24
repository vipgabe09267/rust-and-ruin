package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.beacon.TetanizedBeaconScreenHandler;
import com.tetanusmod.block.CaltropsBlockEntity;
import com.tetanusmod.entity.*;
import com.tetanusmod.expansion.*;
import com.tetanusmod.registry.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

public class DeepAuditGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabiesLosesExactlyOneHeartWithMutationBonus(TestContext ctx) {
        var zombie=EntityType.ZOMBIE.create(ctx.getWorld());
        MobVariants.makePermanent(zombie);
        zombie.addStatusEffect(new StatusEffectInstance(ExpansionEffects.RABIES,-1));
        ((ExpansionState)zombie).rustruin$setRabiesTicks(1199);
        ExpansionEffects.RABIES.applyUpdateEffect(zombie,0);
        ctx.assertTrue(Math.abs(zombie.getMaxHealth()-28)<0.001,"30 HP mutant must lose 2 HP, got "+zombie.getMaxHealth());
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabiesCannotCrossTwoHeartFloorWithMultipliers(TestContext ctx) {
        var zombie=EntityType.ZOMBIE.create(ctx.getWorld());MobVariants.makePermanent(zombie);
        zombie.addStatusEffect(new StatusEffectInstance(ExpansionEffects.RABIES,-1));
        var state=(ExpansionState)zombie;
        for(int minute=0;minute<30;minute++){state.rustruin$setRabiesTicks(1199);ExpansionEffects.RABIES.applyUpdateEffect(zombie,0);}
        ctx.assertTrue(Math.abs(zombie.getMaxHealth()-4)<0.001,"Multiplied Rabies crossed floor: "+zombie.getMaxHealth());
        RabiesEffect.cure(zombie);ctx.assertTrue(zombie.getMaxHealth()==30,"Cure lost mutation bonus");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void removedEarlyRabiesDoesNotCarryItsTimer(TestContext ctx) {
        var cow=EntityType.COW.create(ctx.getWorld());var state=(ExpansionState)cow;
        cow.addStatusEffect(new StatusEffectInstance(ExpansionEffects.RABIES,-1));state.rustruin$setRabiesTicks(1199);
        cow.removeStatusEffect(ExpansionEffects.RABIES);cow.tick();
        ctx.assertTrue(state.rustruin$getRabiesTicks()==0,"Removed Rabies retained pre-heart-loss timer");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void beaconShiftClickRoutesInventoryAndRespectsPayment(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();var menu=new TetanizedBeaconScreenHandler(1,player.getInventory());
        menu.getSlot(1).setStack(new ItemStack(Items.DIRT,17));
        ctx.assertTrue(!menu.quickMove(player,1).isEmpty()&&menu.getSlot(28).getStack().getCount()==17,"Beacon blocks normal inventory-to-hotbar transfer");
        ctx.assertTrue(!menu.quickMove(player,28).isEmpty()&&menu.getSlot(1).getStack().getCount()==17,"Beacon blocks hotbar-to-inventory transfer");
        menu.getSlot(2).setStack(new ItemStack(Items.IRON_INGOT));menu.quickMove(player,2);
        ctx.assertTrue(menu.getSlot(0).getStack().isEmpty(),"Shift-click bypassed netherite payment rule");
        menu.getSlot(3).setStack(new ItemStack(Items.NETHERITE_INGOT,4));menu.quickMove(player,3);
        ctx.assertTrue(menu.getSlot(0).getStack().isOf(Items.NETHERITE_INGOT)&&menu.getSlot(0).getStack().getCount()==1&&menu.getSlot(3).getStack().getCount()==3,"Netherite stack must transfer exactly one payment");
        ctx.assertTrue(!menu.quickMove(player,0).isEmpty()&&menu.getSlot(0).getStack().isEmpty(),"Payment cannot be retrieved");ctx.complete();
    }
    private VariantSpawnEggItem egg(){return (VariantSpawnEggItem)Registries.ITEM.get(TetanusMod.id("tetanized_cow_spawn_egg"));}
    private void aim(TestContext ctx,net.minecraft.server.network.ServerPlayerEntity player){
        for(int z=1;z<7;z++)for(int y=3;y<6;y++)ctx.setBlockState(3,y,z,Blocks.AIR);
        player.setPosition(ctx.getAbsolute(new Vec3d(3.5,3,1.5)));player.setYaw(0);player.setPitch(0);
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void spawnEggAirUseDoesNotSpawnInsideSolidBlocks(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try{aim(ctx,player);ctx.setBlockState(3,4,3,Blocks.STONE);var stack=new ItemStack(egg(),2);player.setStackInHand(Hand.MAIN_HAND,stack);
            var result=player.interactionManager.interactItem(player,ctx.getWorld(),stack,Hand.MAIN_HAND);
            ctx.assertFalse(result.isAccepted(),"Air-use packet spawned an egg inside a solid block");
        }finally{player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void spawnEggRespectsAdventureWaterPermissions(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try{aim(ctx,player);ctx.setBlockState(3,4,3,Blocks.WATER);player.changeGameMode(GameMode.ADVENTURE);
            var control=new ItemStack(Items.COW_SPAWN_EGG,2);player.setStackInHand(Hand.MAIN_HAND,control);
            ctx.assertFalse(player.interactionManager.interactItem(player,ctx.getWorld(),control,Hand.MAIN_HAND).isAccepted(),"Vanilla control unexpectedly permitted Adventure placement");
            var stack=new ItemStack(egg(),2);player.setStackInHand(Hand.MAIN_HAND,stack);
            ctx.assertFalse(player.interactionManager.interactItem(player,ctx.getWorld(),stack,Hand.MAIN_HAND).isAccepted(),"Variant egg bypassed Adventure CanPlaceOn");
            ctx.assertTrue(stack.getCount()==2,"Denied Adventure egg consumed an item");
        }finally{player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rustyNailRetainsEndermanAttackerCredit(TestContext ctx)throws Exception {
        var target=EntityType.ENDERMAN.create(ctx.getWorld());var player=ctx.createMockSurvivalPlayer();
        var nail=new NailEntity(TetanusMod.NAIL_ENTITY,ctx.getWorld());nail.setOwner(player);nail.setItem(new ItemStack(ModItems.RUSTED_NAIL));
        var method=NailEntity.class.getDeclaredMethod("onEntityHit",EntityHitResult.class);method.setAccessible(true);
        float chance=TetanusMod.CONFIG.mobConversionChance;
        try{TetanusMod.CONFIG.mobConversionChance=0;
            method.invoke(nail,new EntityHitResult(target));
            ctx.assertTrue(target.getHealth()<target.getMaxHealth(),"Enderman did not take nail damage");
            ctx.assertTrue(target.getAttacker()==player,"Rusty nail lost its Enderman damage/kill attribution");
        }finally{TetanusMod.CONFIG.mobConversionChance=chance;}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=45)
    public void unchangedDryTrapsDoNotDirtyChunks(TestContext ctx) {
        var caltropPos=ctx.getAbsolutePos(new BlockPos(2,3,2));var bearPos=ctx.getAbsolutePos(new BlockPos(3,3,2));
        ctx.getWorld().setBlockState(caltropPos,ModBlocks.CALTROPS.getDefaultState());ctx.getWorld().setBlockState(bearPos,ExpansionBlocks.BEAR_TRAP.getDefaultState());
        ctx.getWorld().setBlockState(caltropPos.up(),Blocks.STONE.getDefaultState());ctx.getWorld().setBlockState(bearPos.up(),Blocks.STONE.getDefaultState());
        ctx.runAtEveryTick(()->{if(ctx.getWorld().getTime()%20!=0)return;
            var chunk=ctx.getWorld().getWorldChunk(caltropPos);chunk.setNeedsSaving(false);
            CaltropsBlockEntity.tick(ctx.getWorld(),caltropPos,ctx.getWorld().getBlockState(caltropPos),(CaltropsBlockEntity)ctx.getWorld().getBlockEntity(caltropPos));
            ctx.assertFalse(chunk.needsSaving(),"Unchanged dry caltrops dirty their chunk");
            var bearChunk=ctx.getWorld().getWorldChunk(bearPos);bearChunk.setNeedsSaving(false);
            BearTrapBlockEntity.tick(ctx.getWorld(),bearPos,ctx.getWorld().getBlockState(bearPos),(BearTrapBlockEntity)ctx.getWorld().getBlockEntity(bearPos));
            ctx.assertFalse(bearChunk.needsSaving(),"Unchanged dry bear trap dirties its chunk");ctx.complete();
        });
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabiesTracksChangingHealthModifiersAndClampedValues(TestContext ctx) {
        var cow=EntityType.COW.create(ctx.getWorld());var attribute=cow.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        var bonus=new EntityAttributeModifier(java.util.UUID.randomUUID(),"Audit health bonus",1,EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        attribute.addTemporaryModifier(bonus);var state=(ExpansionState)cow;state.rustruin$setLostHealth(16);RabiesEffect.restorePenalty(cow);
        ctx.assertTrue(Math.abs(cow.getMaxHealth()-4)<0.001,"Scaled health floor failed");
        attribute.removeModifier(bonus.getId());RabiesEffect.restorePenalty(cow);
        ctx.assertTrue(Math.abs(cow.getMaxHealth()-4)<0.001,"Removing health bonus breached floor");
        RabiesEffect.cure(cow);attribute.setBaseValue(1024);attribute.addTemporaryModifier(bonus);state.rustruin$setLostHealth(2);RabiesEffect.restorePenalty(cow);
        ctx.assertTrue(Math.abs(cow.getMaxHealth()-1022)<0.001,"Attribute clamp swallowed Rabies loss");
        var unchanged=attribute.getModifier(RabiesEffect.HEALTH_PENALTY);RabiesEffect.restorePenalty(cow);
        ctx.assertTrue(attribute.getModifier(RabiesEffect.HEALTH_PENALTY)==unchanged,"Stable Rabies needlessly replaces attribute each tick");
        RabiesEffect.cure(cow);attribute.removeModifier(bonus.getId());attribute.setBaseValue(3);state.rustruin$setLostHealth(10);RabiesEffect.restorePenalty(cow);
        ctx.assertTrue(cow.getMaxHealth()==3,"Rabies raised a naturally low health limit");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabiesMigratesOldPenaltyAndRestoresAfterReload(TestContext ctx) {
        var zombie=EntityType.ZOMBIE.create(ctx.getWorld());MobVariants.makePermanent(zombie);
        zombie.addStatusEffect(new StatusEffectInstance(ExpansionEffects.RABIES,-1));
        ((ExpansionState)zombie).rustruin$setLostHealth(6);
        zombie.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).addTemporaryModifier(new EntityAttributeModifier(RabiesEffect.HEALTH_PENALTY,"Old Rabies",-6,EntityAttributeModifier.Operation.ADDITION));
        RabiesEffect.restorePenalty(zombie);ctx.assertTrue(Math.abs(zombie.getMaxHealth()-24)<0.001,"Legacy addition penalty did not migrate");
        var loaded=EntityType.ZOMBIE.create(ctx.getWorld());loaded.readNbt(zombie.writeNbt(new net.minecraft.nbt.NbtCompound()));RabiesEffect.restorePenalty(loaded);
        ctx.assertTrue(Math.abs(loaded.getMaxHealth()-24)<0.001,"Rabies health bonus lost across reload");
        RabiesEffect.cure(loaded);ctx.assertTrue(loaded.getMaxHealth()==30,"Reloaded cure failed");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void permittedAdventureEggStillWorksOnBlocks(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try{aim(ctx,player);ctx.setBlockState(3,3,3,Blocks.STONE);player.changeGameMode(GameMode.ADVENTURE);
            var stack=new ItemStack(egg(),2);var allowed=new net.minecraft.nbt.NbtList();allowed.add(net.minecraft.nbt.NbtString.of("minecraft:stone"));stack.getOrCreateNbt().put("CanPlaceOn",allowed);player.setStackInHand(Hand.MAIN_HAND,stack);
            var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));var hit=new net.minecraft.util.hit.BlockHitResult(Vec3d.ofCenter(pos),Direction.UP,pos,false);
            ctx.assertTrue(stack.useOnBlock(new ItemUsageContext(player,Hand.MAIN_HAND,hit)).isAccepted()&&stack.getCount()==1,"Valid Adventure CanPlaceOn egg was blocked");
        }finally{player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void survivalEggWorksInWaterAndRejectsEmptyStacks(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try{aim(ctx,player);ctx.setBlockState(3,4,3,Blocks.WATER);player.changeGameMode(GameMode.SURVIVAL);
            var stack=new ItemStack(egg(),2);player.setStackInHand(Hand.MAIN_HAND,stack);
            ctx.assertTrue(player.interactionManager.interactItem(player,ctx.getWorld(),stack,Hand.MAIN_HAND).isAccepted()&&stack.getCount()==1,"Survival water egg failed");
            var pos=ctx.getAbsolutePos(new BlockPos(4,4,4));
            ctx.assertFalse(egg().spawn(ctx.getWorld(),player,ItemStack.EMPTY,pos)||egg().spawn(ctx.getWorld(),player,new ItemStack(Items.DIRT),pos),"Invalid stack spawned a variant");
        }finally{player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void weatherProgressResetIsSavedImmediately(TestContext ctx) {
        var pos=ctx.getAbsolutePos(new BlockPos(2,3,2));ctx.getWorld().setBlockState(pos,ModBlocks.CALTROPS.getDefaultState());ctx.getWorld().setBlockState(pos.up(),Blocks.STONE.getDefaultState());
        var trap=(CaltropsBlockEntity)ctx.getWorld().getBlockEntity(pos);var nbt=trap.createNbt();nbt.putInt("WaterTicks",40);trap.readNbt(nbt);
        var chunk=ctx.getWorld().getWorldChunk(pos);chunk.setNeedsSaving(false);CaltropsBlockEntity.tick(ctx.getWorld(),pos,trap.getCachedState(),trap);
        ctx.assertTrue(chunk.needsSaving()&&trap.createNbt().getInt("WaterTicks")==0,"Interrupted exposure reset was not saved");
        ctx.getWorld().setBlockState(pos,ExpansionBlocks.BEAR_TRAP.getDefaultState());var bear=(BearTrapBlockEntity)ctx.getWorld().getBlockEntity(pos);nbt=bear.createNbt();nbt.putInt("Water",40);bear.readNbt(nbt);chunk.setNeedsSaving(false);BearTrapBlockEntity.tick(ctx.getWorld(),pos,bear.getCachedState(),bear);
        ctx.assertTrue(chunk.needsSaving()&&bear.createNbt().getInt("Water")==0,"Bear trap exposure reset was not saved");ctx.complete();
    }
}
