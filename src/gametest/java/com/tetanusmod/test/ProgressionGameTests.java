package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.beacon.*;
import com.tetanusmod.entity.*;
import com.tetanusmod.registry.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.*;
import net.minecraft.potion.*;
import net.minecraft.recipe.*;
import net.minecraft.screen.*;
import net.minecraft.test.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import java.util.*;

public class ProgressionGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void lockjawSonataAdvancement(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        com.tetanusmod.effect.Infection.apply(player,null);
        var state=(com.tetanusmod.effect.InfectionState)player;state.tetanus$setInfection(1600,2400);
        TetanusMod.TETANUS.applyUpdateEffect(player,0);com.tetanusmod.effect.LockjawEffect.flush(player);
        var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id("lockjaw_sonata"));
        ctx.assertTrue(player.hasStatusEffect(TetanusMod.LOCKJAW)&&advancement!=null&&player.getAdvancementTracker().getProgress(advancement).isDone(),"Lockjaw Sonata advancement not awarded");
        player.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void visibleLockjawBlocksFoodAndCures(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();player.getHungerManager().setFoodLevel(10);
        com.tetanusmod.effect.Infection.apply(player,null);
        var state=(com.tetanusmod.effect.InfectionState)player;state.tetanus$setInfection(1600,2400);
        double original=TetanusMod.CONFIG.spasmFrequency;
        try {
            TetanusMod.CONFIG.spasmFrequency=0;TetanusMod.TETANUS.applyUpdateEffect(player,0);com.tetanusmod.effect.LockjawEffect.flush(player);
            ctx.assertTrue(player.hasStatusEffect(TetanusMod.LOCKJAW)&&player.getStatusEffect(TetanusMod.LOCKJAW).getDuration()==800,"Lockjaw did not last through the remaining infection");
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.BREAD));
            var result=net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(player,ctx.getWorld(),Hand.MAIN_HAND);
            ctx.assertTrue(result.getResult()==ActionResult.FAIL,"Lockjaw allowed eating");
            player.removeStatusEffect(TetanusMod.LOCKJAW);state.tetanus$setInfection(2000,2400);TetanusMod.TETANUS.applyUpdateEffect(player,0);com.tetanusmod.effect.LockjawEffect.flush(player);
            ctx.assertTrue(player.hasStatusEffect(TetanusMod.LOCKJAW),"Lockjaw was not restored during the continuous final phase");
            player.removeStatusEffect(TetanusMod.TETANUS);player.tick();
            ctx.assertFalse(player.hasStatusEffect(TetanusMod.LOCKJAW),"Curing Tetanus did not clear lockjaw");
        }finally{TetanusMod.CONFIG.spasmFrequency=original;}
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rustGolemChanceHealthAndAdvancement(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUST,3));
        var golem=ctx.spawnEntity(EntityType.IRON_GOLEM,new Vec3d(3,3,3));
        ctx.assertFalse(RustGolems.attempt(player,Hand.MAIN_HAND,golem,0.18F),"18% failure boundary failed");
        ctx.assertTrue(player.getMainHandStack().getCount()==2&&!MobVariants.permanent(golem),"Failed attempt did not consume exactly one Rust");
        ctx.assertTrue(RustGolems.attempt(player,Hand.MAIN_HAND,golem,0.179F),"18% success boundary failed");
        ctx.assertTrue(golem.getMaxHealth()==200&&golem.getHealth()==200&&player.getMainHandStack().getCount()==1,"Tetanized golem must have 200 health");
        ctx.assertFalse(RustGolems.attempt(player,Hand.MAIN_HAND,golem,0),"Existing golem could be converted twice");
        ctx.assertTrue(player.getMainHandStack().getCount()==1,"Already tetanized golem consumed Rust");
        var serverPlayer=ctx.createMockCreativeServerPlayerInWorld();serverPlayer.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUST));
        var second=ctx.spawnEntity(EntityType.IRON_GOLEM,new Vec3d(5,3,5));RustGolems.attempt(serverPlayer,Hand.MAIN_HAND,second,0);
        var advancement=serverPlayer.getServer().getAdvancementLoader().get(TetanusMod.id("rusty_guardian"));
        ctx.assertTrue(advancement!=null&&serverPlayer.getAdvancementTracker().getProgress(advancement).isDone(),"Golem advancement missing");
        var loaded=EntityType.IRON_GOLEM.create(ctx.getWorld());loaded.readNbt(second.writeNbt(new net.minecraft.nbt.NbtCompound()));MobVariants.loaded(loaded);
        ctx.assertTrue(loaded.getMaxHealth()==200,"Golem health did not persist");serverPlayer.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=180)
    public void floatingNailsRustInShallowWater(TestContext ctx) {
        for(int x=1;x<7;x++)for(int z=1;z<7;z++) {ctx.setBlockState(x,2,z,Blocks.STONE);ctx.setBlockState(x,3,z,Blocks.WATER);}
        var pos=ctx.getAbsolute(new Vec3d(3.5,3.9,3.5));
        var item=new ItemEntity(ctx.getWorld(),pos.x,pos.y,pos.z,new ItemStack(ModItems.NAIL,7));
        item.setPickupDelay(32767);item.setVelocity(Vec3d.ZERO);ctx.getWorld().spawnEntity(item);
        ctx.runAtTick(95,()->ctx.assertTrue(item.getStack().isOf(ModItems.NAIL),"Floating nails rusted too early"));
        ctx.runAtTick(150,()->{ctx.assertTrue(item.getStack().isOf(ModItems.RUSTED_NAIL)&&item.getStack().getCount()==7,"Floating water-surface nails did not rust: "+item.getPos()+" "+item.getStack());ctx.complete();});
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void allVanillaMobsCanBeTetanized(TestContext ctx) {
        int count=0;
        for(var type:net.minecraft.registry.Registries.ENTITY_TYPE) {
            Entity entity=type.create(ctx.getWorld());
            if(!(entity instanceof MobEntity mob))continue;
            MobVariants.makePermanent(mob);
            ctx.assertTrue(MobVariants.permanent(mob),"Cannot tetanize "+type);
            if(!(mob instanceof WitherEntity))ctx.assertTrue(MobVariants.eligibleRoll(mob,0.049F),"Mob ineligible for mutation: "+type);
            count++;
        }
        ctx.assertTrue(count>60,"Vanilla mob coverage unexpectedly low");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void brewingChainAndSplashConversion(TestContext ctx) {
        ItemStack water=PotionUtil.setPotion(new ItemStack(Items.POTION),Potions.WATER);
        var manure=BrewingRecipeRegistry.craft(new ItemStack(ModItems.COW_MANURE),water);
        ctx.assertTrue(PotionUtil.getPotion(manure)==ModBrewing.MANURE,"Water + manure failed");
        var tetanus=BrewingRecipeRegistry.craft(new ItemStack(ModItems.RUST),manure);
        var salmonella=BrewingRecipeRegistry.craft(new ItemStack(ModItems.INFECTED_MEAT),manure);
        ctx.assertTrue(PotionUtil.getPotion(tetanus)==ModBrewing.TETANUS,"Manure + rust failed");
        ctx.assertTrue(PotionUtil.getPotion(salmonella)==ModBrewing.SALMONELLA,"Manure + meat failed");
        for(var bottle:List.of(tetanus,salmonella)) {
            var splash=BrewingRecipeRegistry.craft(new ItemStack(Items.GUNPOWDER),bottle);
            ctx.assertTrue(splash.isOf(Items.SPLASH_POTION)&&PotionUtil.getPotion(splash)==PotionUtil.getPotion(bottle),"Gunpowder splash conversion failed");
            ctx.assertFalse(PotionUtil.getPotionEffects(splash).isEmpty(),"Splash potion has no disease effect");
        }
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void manureCollectionAndCooldown(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(3,3,3));
        player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE,2));
        var callback=net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker();
        callback.interact(player,ctx.getWorld(),Hand.MAIN_HAND,cow,null);
        ctx.assertTrue(player.getMainHandStack().getCount()==1,"Manure collection did not consume a bottle");
        ctx.assertTrue(player.getInventory().contains(new ItemStack(ModItems.COW_MANURE)),"Manure not delivered");
        callback.interact(player,ctx.getWorld(),Hand.MAIN_HAND,cow,null);
        ctx.assertTrue(player.getMainHandStack().getCount()==1,"Collection cooldown failed");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void newAdvancementsAwarded(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        player.addStatusEffect(new StatusEffectInstance(TetanusMod.SALMONELLA,1200));
        var strange=player.getServer().getAdvancementLoader().get(TetanusMod.id("why_did_i_eat_that"));
        ctx.assertTrue(strange!=null&&player.getAdvancementTracker().getProgress(strange).isDone(),"Why did I eat that? not awarded");
        float old=TetanusMod.CONFIG.mobConversionChance;
        try {
            TetanusMod.CONFIG.mobConversionChance=1;
            var spider=ctx.spawnEntity(EntityType.SPIDER,new Vec3d(3,3,3));MobVariants.projectileExposure(spider,player);
            var converted=player.getServer().getAdvancementLoader().get(TetanusMod.id("tetanized_mob"));
            ctx.assertTrue(MobVariants.permanent(spider)&&converted!=null&&player.getAdvancementTracker().getProgress(converted).isDone(),"Tetanized Mob not awarded");
        }finally{TetanusMod.CONFIG.mobConversionChance=old;player.discard();}
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void hooklinePullsAndInfects(TestContext ctx) {
        for(int x=2;x<=4;x++)for(int y=3;y<=5;y++)for(int z=1;z<=6;z++)ctx.setBlockState(x,y,z,Blocks.AIR);
        var player=ctx.createMockSurvivalPlayer();player.setPosition(ctx.getAbsolute(new Vec3d(3.5,3,1.5)));player.setYaw(0);player.setPitch(0);
        var target=ctx.spawnEntity(EntityType.COW,new Vec3d(3.5,3.5,5.5));target.setNoGravity(true);target.setAiDisabled(true);
        player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUSTY_HOOKLINE));
        // Let the server register the spawned cow in its chunk entity lookup before raycasting.
        ctx.runAtTick(2,()->{
        ctx.assertTrue(ctx.getWorld().getOtherEntities(player,target.getBoundingBox()).contains(target),"Hookline target is not in the world entity lookup");
        ModItems.RUSTY_HOOKLINE.use(ctx.getWorld(),player,Hand.MAIN_HAND);
        ctx.assertTrue(target.hasStatusEffect(TetanusMod.TETANUS),"Hookline did not infect: health="+target.getHealth()+" pos="+target.getPos()+" player="+player.getPos()+" damage="+player.getMainHandStack().getDamage());
        ctx.assertTrue(target.getVelocity().z<0,"Hookline did not pull toward player");
        ctx.assertTrue(player.getMainHandStack().getDamage()==1&&player.getItemCooldownManager().isCoolingDown(ModItems.RUSTY_HOOKLINE),"Hookline cost or cooldown missing");ctx.complete();
        });
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void caltropsShapelessCraftingAndBeaconRecipe(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();var grid=new CraftingInventory(new CraftingScreenHandler(1,player.getInventory()),3,3);
        for(int slot:new int[]{0,2,6,8})grid.setStack(slot,new ItemStack(ModItems.NAIL));grid.setStack(4,new ItemStack(Items.IRON_NUGGET));
        var recipe=ctx.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING,grid,ctx.getWorld()).orElseThrow();
        var result=recipe.craft(grid,ctx.getWorld().getRegistryManager());
        ctx.assertTrue(result.isOf(ModBlocks.CALTROPS.asItem())&&result.getCount()==4,"Caltrops shapeless recipe failed");
        grid.clear();for(int i:new int[]{0,1,2,3,5})grid.setStack(i,new ItemStack(Items.GLASS));for(int i=6;i<9;i++)grid.setStack(i,new ItemStack(Items.OBSIDIAN));grid.setStack(4,new ItemStack(ModItems.TETANIZED_WITHER_STAR));
        recipe=ctx.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING,grid,ctx.getWorld()).orElseThrow();
        ctx.assertTrue(recipe.craft(grid,ctx.getWorld().getRegistryManager()).isOf(ModBeacons.ITEM),"Tetanized beacon recipe failed");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void brushDetetanizesStar(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.WIRE_BRUSH));player.setStackInHand(Hand.OFF_HAND,new ItemStack(ModItems.TETANIZED_WITHER_STAR));
        ModItems.WIRE_BRUSH.use(ctx.getWorld(),player,Hand.MAIN_HAND);
        ctx.assertTrue(player.getOffHandStack().isOf(Items.NETHER_STAR),"Brush did not restore Nether Star");ctx.complete();
    }
    private void pyramid(TestContext ctx,BlockPos pos,Block material,int levels) {
        for(int layer=1;layer<=levels;layer++)for(int x=-layer;x<=layer;x++)for(int z=-layer;z<=layer;z++)ctx.setBlockState(pos.add(x,-layer,z),material);
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void beaconAllowedBasesAndPersistence(TestContext ctx) {
        var pos=new BlockPos(4,6,4);ctx.setBlockState(pos,ModBeacons.BLOCK);
        for(Block block:List.of(com.tetanusmod.expansion.ExpansionBlocks.RUSTED_IRON_BLOCK)) {
            pyramid(ctx,pos,block,4);ctx.assertTrue(BeaconPower.level(ctx.getWorld(),ctx.getAbsolutePos(pos))==4,"Rejected valid beacon base "+block);
        }
        for(Block block:List.of(Blocks.IRON_BLOCK,Blocks.DIAMOND_BLOCK,Blocks.EMERALD_BLOCK,Blocks.GOLD_BLOCK,Blocks.COPPER_BLOCK,Blocks.NETHERITE_BLOCK)) {
            pyramid(ctx,pos,block,1);ctx.assertTrue(BeaconPower.level(ctx.getWorld(),ctx.getAbsolutePos(pos))==0,"Accepted invalid beacon base "+block);
        }
        var beacon=ctx.getWorld().getBlockEntity(ctx.getAbsolutePos(pos));
        var nbt=beacon.createNbtWithId();
        ctx.assertTrue(nbt.getString("id").equals("tetanusmod:tetanized_beacon"),"Beacon saved as vanilla type");
        ctx.assertTrue(BlockEntity.createFromNbt(ctx.getAbsolutePos(pos),ModBeacons.BLOCK.getDefaultState(),nbt) instanceof TetanizedBeaconEntity,"Beacon reload lost custom type");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void beaconPaymentAndStrongPowers(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();player.setPosition(ctx.getAbsolute(new Vec3d(3,3,3)));
        var menu=new TetanizedBeaconScreenHandler(1,player.getInventory());
        ctx.assertTrue(menu.getSlot(0).canInsert(new ItemStack(Items.NETHERITE_INGOT)),"Beacon rejects netherite");
        ctx.assertFalse(menu.getSlot(0).canInsert(new ItemStack(Items.IRON_INGOT)),"Beacon accepts ordinary payment");
        menu.setProperty(0,4);menu.getSlot(0).setStack(new ItemStack(Items.NETHERITE_INGOT));
        menu.setEffects(Optional.of(StatusEffects.HASTE),Optional.of(StatusEffects.HASTE));
        ctx.assertTrue(menu.getSlot(0).getStack().isEmpty()&&menu.getPrimaryEffect()==StatusEffects.HASTE&&menu.getSecondaryEffect()==StatusEffects.HASTE,"Netherite payment did not activate and consume correctly");
        menu.getSlot(0).setStack(new ItemStack(Items.IRON_INGOT));menu.setEffects(Optional.of(StatusEffects.SPEED),Optional.empty());
        ctx.assertTrue(menu.getSlot(0).getStack().isOf(Items.IRON_INGOT)&&menu.getPrimaryEffect()==StatusEffects.HASTE,"Invalid payment changed beacon powers");
        var real=ctx.createMockCreativeServerPlayerInWorld();real.setPosition(player.getPos());
        var pos=BlockPos.ofFloored(real.getPos());
        BeaconPower.apply(ctx.getWorld(),pos,4,StatusEffects.HASTE,StatusEffects.REGENERATION);
        ctx.assertTrue(real.getStatusEffect(StatusEffects.HASTE).getAmplifier()==1&&real.getStatusEffect(StatusEffects.REGENERATION).getAmplifier()==1,"Beacon should grant Haste II + Regeneration II");
        real.removeStatusEffect(StatusEffects.HASTE);BeaconPower.apply(ctx.getWorld(),pos,4,StatusEffects.HASTE,StatusEffects.HASTE);
        ctx.assertTrue(real.getStatusEffect(StatusEffects.HASTE).getAmplifier()==2,"Beacon upgrade should grant Haste III");real.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=180)
    public void actualBeaconTicksApplyPowers(TestContext ctx) {
        var pos=new BlockPos(4,6,4);pyramid(ctx,pos,com.tetanusmod.expansion.ExpansionBlocks.RUSTED_IRON_BLOCK,4);ctx.setBlockState(pos,ModBeacons.BLOCK);
        var beacon=(TetanizedBeaconEntity)ctx.getWorld().getBlockEntity(ctx.getAbsolutePos(pos));
        var nbt=beacon.createNbt();nbt.putInt("Primary",net.minecraft.registry.Registries.STATUS_EFFECT.getRawId(StatusEffects.SPEED));nbt.putInt("Secondary",net.minecraft.registry.Registries.STATUS_EFFECT.getRawId(StatusEffects.SPEED));beacon.readNbt(nbt);
        var player=ctx.createMockCreativeServerPlayerInWorld();player.setPosition(ctx.getAbsolute(new Vec3d(3,6,3)));
        ctx.runAtTick(120,()->{ctx.assertTrue(player.hasStatusEffect(StatusEffects.SPEED)&&player.getStatusEffect(StatusEffects.SPEED).getAmplifier()==2,"Placed beacon did not produce Speed III");player.discard();ctx.complete();});
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void enhancedWitherRitualConsumesNetherite(TestContext ctx) {
        float chance=TetanusMod.CONFIG.witherConversionChance;
        try {
            TetanusMod.CONFIG.witherConversionChance=1;
            ctx.setBlockState(3,2,3,ModBlocks.TETANIZED_SOUL_SAND);
            ctx.setBlockState(2,3,3,ModBlocks.TETANIZED_SOUL_SAND);ctx.setBlockState(4,3,3,ModBlocks.TETANIZED_SOUL_SAND);ctx.setBlockState(3,3,3,Blocks.NETHERITE_BLOCK);
            for(int x=2;x<=4;x++)ctx.setBlockState(x,4,3,Blocks.WITHER_SKELETON_SKULL);
            var skull=ctx.getAbsolutePos(new BlockPos(4,4,3));
            WitherSkullBlock.onPlaced(ctx.getWorld(),skull,(net.minecraft.block.entity.SkullBlockEntity)ctx.getWorld().getBlockEntity(skull));
            var withers=ctx.getWorld().getEntitiesByClass(WitherEntity.class,new Box(ctx.getAbsolute(new Vec3d(0,0,0)),ctx.getAbsolute(new Vec3d(8,8,8))),e->true);
            ctx.assertTrue(withers.size()==1&&MobVariants.permanent(withers.get(0)),"Netherite ritual did not summon tetanized wither");
            ctx.assertTrue(ctx.getWorld().isAir(ctx.getAbsolutePos(new BlockPos(3,3,3))),"Ritual did not consume netherite");withers.forEach(Entity::discard);
        }finally{TetanusMod.CONFIG.witherConversionChance=chance;}
        ctx.complete();
    }
}