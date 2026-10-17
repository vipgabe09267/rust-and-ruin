package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.registry.*;
import com.tetanusmod.rust.FishingSalvage;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.*;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.item.*;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

public class BalanceGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void caltropAudioComesFromBlock(TestContext ctx) {
        var pos=new BlockPos(2,3,2);
        ctx.setBlockState(pos,ModBlocks.RUSTED_CALTROPS.getDefaultState());
        var trap=(com.tetanusmod.block.CaltropsBlockEntity)ctx.getWorld().getBlockEntity(ctx.getAbsolutePos(pos));
        var target=ctx.spawnEntity(EntityType.COW,new Vec3d(4,3,4));
        SoundProbe.begin();
        try {
            trap.stepOn(target,true);
            ctx.assertTrue(SoundProbe.count==1,"Caltrop must play exactly one positional infection sound");
            ctx.assertTrue(SoundProbe.position.squaredDistanceTo(Vec3d.ofCenter(ctx.getAbsolutePos(pos)))<0.001,"Caltrop audio played at victim rather than block");
        }finally{SoundProbe.enabled=false;}
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void arrowAudioPlaysAtImpactOnly(TestContext ctx)throws Exception {
        var player=ctx.createMockSurvivalPlayer();player.setPosition(ctx.getAbsolute(new Vec3d(2,3,2)));
        var bow=new ItemStack(Items.BOW);player.setStackInHand(Hand.MAIN_HAND,bow);
        player.setStackInHand(Hand.OFF_HAND,new ItemStack(ModItems.TETANIZED_ARROW,3));
        var target=ctx.spawnEntity(EntityType.COW,new Vec3d(5,3,5));
        SoundProbe.begin();
        try {
            Items.BOW.onStoppedUsing(bow,ctx.getWorld(),player,71980);
            ctx.assertTrue(SoundProbe.count==0,"Bow played infection audio at shooter");
            var arrow=ctx.getWorld().getEntitiesByType(TetanusMod.TETANIZED_ARROW_ENTITY,player.getBoundingBox().expand(4),e->true).get(0);
            Vec3d impact=target.getPos().add(0,0.6,0);
            var method=com.tetanusmod.entity.TetanizedArrowEntity.class.getDeclaredMethod("onEntityHit",net.minecraft.util.hit.EntityHitResult.class);
            method.setAccessible(true);method.invoke(arrow,new net.minecraft.util.hit.EntityHitResult(target,impact));
            ctx.assertTrue(SoundProbe.count==1,"Arrow must play one infection sound on impact");
            ctx.assertTrue(SoundProbe.position.squaredDistanceTo(impact)<0.001,"Arrow sound played away from impact");
            arrow.discard();
            ctx.assertTrue(net.minecraft.registry.Registries.SOUND_EVENT.containsId(TetanusMod.id("tetanus_effect"))
                    &&net.minecraft.registry.Registries.SOUND_EVENT.containsId(TetanusMod.id("spasm"))
                    &&net.minecraft.registry.Registries.SOUND_EVENT.containsId(TetanusMod.id("vomit")),"Custom sounds unavailable to playsound");
        }finally{SoundProbe.enabled=false;}
        ctx.complete();
    }
    private java.util.List<ItemEntity> drops(TestContext ctx,Vec3d pos) {
        return ctx.getWorld().getEntitiesByClass(ItemEntity.class,new Box(pos,pos).expand(5),e->true);
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void animalsReplaceAllOrdinaryLoot(TestContext ctx) {
        var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(3,3,3));
        MobVariants.makePermanent(cow);
        cow.damage(ctx.getWorld().getDamageSources().genericKill(),1000);
        var drops=drops(ctx,cow.getPos());
        ctx.assertTrue(!drops.isEmpty()&&drops.stream().allMatch(e->e.getStack().isOf(ModItems.INFECTED_MEAT)),"Tetanized cow dropped leather or ordinary meat");
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void hostileDropsAreModest(TestContext ctx) {
        var zombie=ctx.spawnEntity(EntityType.ZOMBIE,new Vec3d(3,3,3));
        MobVariants.makePermanent(zombie);zombie.damage(ctx.getWorld().getDamageSources().genericKill(),1000);
        var drops=drops(ctx,zombie.getPos());
        int iron=drops.stream().filter(e->e.getStack().isOf(Items.IRON_NUGGET)).mapToInt(e->e.getStack().getCount()).sum();
        ctx.assertTrue(iron>=1&&iron<=3,"Mutant iron reward must be one to three nuggets");
        ctx.assertTrue(drops.stream().anyMatch(e->e.getStack().isOf(ModItems.RUST)),"Rust reward missing");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void brushingGivesExactlyOneRust(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();
        player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.WIRE_BRUSH));
        player.setStackInHand(Hand.OFF_HAND,new ItemStack(ModItems.RUSTED_NAIL,8));
        ModItems.WIRE_BRUSH.use(ctx.getWorld(),player,Hand.MAIN_HAND);
        ctx.assertTrue(player.getOffHandStack().isOf(ModItems.NAIL)&&player.getOffHandStack().getCount()==8,"Brush failed to clean stack");
        int rust=0;for(int i=0;i<player.getInventory().size();i++)if(player.getInventory().getStack(i).isOf(ModItems.RUST))rust+=player.getInventory().getStack(i).getCount();
        ctx.assertTrue(rust==1,"Cleaning should return exactly one Rust");
        ModItems.WIRE_BRUSH.use(ctx.getWorld(),player,Hand.MAIN_HAND);
        ctx.assertTrue(player.getMainHandStack().getDamage()==8,"Clean items consumed brush durability");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void fishingSalvageChanceAndNoDuplication(TestContext ctx) {
        Vec3d pos=ctx.getAbsolute(new Vec3d(3,3,3));
        var rod=new ItemEntity(ctx.getWorld(),pos.x,pos.y,pos.z,new ItemStack(Items.FISHING_ROD));
        ctx.getWorld().spawnEntity(rod);
        ctx.assertTrue(FishingSalvage.breakRod(rod,0.149F),"15% hook success boundary failed");
        ctx.assertFalse(FishingSalvage.breakRod(rod,0),"Destroyed rod could be salvaged twice");
        var drops=drops(ctx,pos);
        ctx.assertTrue(drops.stream().filter(e->e.getStack().isOf(Items.STICK)).mapToInt(e->e.getStack().getCount()).sum()==2,"Rod must return two sticks");
        ctx.assertTrue(drops.stream().filter(e->e.getStack().isOf(Items.STRING)).mapToInt(e->e.getStack().getCount()).sum()==2,"Rod must return two string");
        ctx.assertTrue(drops.stream().filter(e->e.getStack().isOf(ModItems.RUSTY_FISHING_HOOK)).count()==1,"Hook count wrong");
        drops.forEach(Entity::discard);
        var failed=new ItemEntity(ctx.getWorld(),pos.x,pos.y,pos.z,new ItemStack(Items.FISHING_ROD));
        ctx.assertFalse(FishingSalvage.breakRod(failed,0.15F),"15% failure boundary failed");
        ctx.assertFalse(drops(ctx,pos).stream().anyMatch(e->e.getStack().isOf(ModItems.RUSTY_FISHING_HOOK)),"Failed roll dropped hook");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=150)
    public void waterBreaksRodAfterFiveSeconds(TestContext ctx) {
        var pos=new BlockPos(3,3,3);
        ctx.setBlockState(pos.down(),Blocks.STONE);
        for(int y=0;y<5;y++) {
            for(var d:Direction.Type.HORIZONTAL)ctx.setBlockState(pos.up(y).offset(d),Blocks.GLASS);
            ctx.setBlockState(pos.up(y),Blocks.WATER);
        }
        var absolute=ctx.getAbsolute(Vec3d.ofCenter(pos));
        var rod=new ItemEntity(ctx.getWorld(),absolute.x,absolute.y,absolute.z,new ItemStack(Items.FISHING_ROD));
        rod.setNoGravity(true);rod.setVelocity(Vec3d.ZERO);ctx.getWorld().spawnEntity(rod);
        ctx.runAtTick(95,()->ctx.assertTrue(rod.isAlive(),"Rod broke before five seconds"));
        ctx.runAtTick(115,()->{ctx.assertFalse(rod.isAlive(),"Water did not break rod");ctx.assertTrue(drops(ctx,absolute).stream().anyMatch(e->e.getStack().isOf(Items.STRING)),"Water salvage missing");ctx.complete();});
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void salmonellaChanceAndSymptoms(TestContext ctx) {
        float chance=TetanusMod.CONFIG.salmonellaChance;
        var player=ctx.createMockSurvivalPlayer();
        try {
            TetanusMod.CONFIG.salmonellaChance=0;
            ModItems.INFECTED_MEAT.finishUsing(new ItemStack(ModItems.INFECTED_MEAT),ctx.getWorld(),player);
            ctx.assertFalse(player.hasStatusEffect(TetanusMod.SALMONELLA),"Zero chance infected player");
            TetanusMod.CONFIG.salmonellaChance=1;
            ModItems.INFECTED_MEAT.finishUsing(new ItemStack(ModItems.INFECTED_MEAT),ctx.getWorld(),player);
            ctx.assertTrue(player.hasStatusEffect(TetanusMod.SALMONELLA),"Guaranteed salmonella failed");
            player.getHungerManager().setFoodLevel(20);player.setHealth(20);player.timeUntilRegen=0;
            TetanusMod.SALMONELLA.applyUpdateEffect(player,0);
            ctx.assertTrue(player.getHungerManager().getFoodLevel()==18&&player.getHealth()==19.5F,"Vomit hunger or gentle damage wrong");
            player.setHealth(1);player.timeUntilRegen=0;TetanusMod.SALMONELLA.applyUpdateEffect(player,0);
            ctx.assertTrue(player.getHealth()==1,"Salmonella damage killed player");
            ctx.assertTrue(TetanusMod.SALMONELLA.canApplyUpdateEffect(1000,0)&&!TetanusMod.SALMONELLA.canApplyUpdateEffect(999,0),"Vomit interval wrong");
            player.removeStatusEffect(TetanusMod.SALMONELLA);
            ModItems.COOKED_INFECTED_MEAT.finishUsing(new ItemStack(ModItems.COOKED_INFECTED_MEAT),ctx.getWorld(),player);
            ctx.assertFalse(player.hasStatusEffect(TetanusMod.SALMONELLA),"Cooked meat caused salmonella");
        }finally{TetanusMod.CONFIG.salmonellaChance=chance;}
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void soulSandRecipe(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();
        var grid=new CraftingInventory(new CraftingScreenHandler(1,player.getInventory()),3,3);
        grid.setStack(1,new ItemStack(ModItems.RUST));grid.setStack(4,new ItemStack(Items.SOUL_SAND));grid.setStack(8,new ItemStack(Items.IRON_INGOT));
        var recipe=ctx.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING,grid,ctx.getWorld()).orElseThrow();
        ctx.assertTrue(recipe.craft(grid,ctx.getWorld().getRegistryManager()).isOf(ModBlocks.TETANIZED_SOUL_SAND_ITEM),"Special soul sand recipe failed");ctx.complete();
    }
    private void ritual(TestContext ctx,boolean special,float chance,boolean expected) {
        float original=TetanusMod.CONFIG.witherConversionChance;
        try {
            TetanusMod.CONFIG.witherConversionChance=chance;
            Block sand=special?ModBlocks.TETANIZED_SOUL_SAND:Blocks.SOUL_SAND;
            ctx.setBlockState(3,2,3,sand);
            for(int x=2;x<=4;x++)ctx.setBlockState(x,3,3,sand);
            for(int x=2;x<=4;x++)ctx.setBlockState(x,4,3,Blocks.WITHER_SKELETON_SKULL.getDefaultState());
            BlockPos skull=ctx.getAbsolutePos(new BlockPos(4,4,3));
            WitherSkullBlock.onPlaced(ctx.getWorld(),skull,(SkullBlockEntity)ctx.getWorld().getBlockEntity(skull));
            var withers=ctx.getWorld().getEntitiesByClass(WitherEntity.class,new Box(ctx.getAbsolute(new Vec3d(0,0,0)),ctx.getAbsolute(new Vec3d(8,8,8))),e->true);
            ctx.assertTrue(withers.size()==1,"Wither ritual did not spawn exactly one boss");
            var wither=withers.get(0);
            ctx.assertTrue(MobVariants.permanent(wither)==expected,"Ritual used incorrect mutation chance or sand");
            ctx.assertTrue(wither.getMaxHealth()==(expected?600:300),"Ritual boss health wrong");wither.discard();
        }finally{TetanusMod.CONFIG.witherConversionChance=original;}
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void specialRitualCreatesMutant(TestContext ctx){ritual(ctx,true,1,true);}
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void ordinaryRitualStaysVanilla(TestContext ctx){ritual(ctx,false,1,false);}
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void failedSpecialRollCreatesNormalWither(TestContext ctx){ritual(ctx,true,0,false);}
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void summonCommandsAndWitherAdvancement(TestContext ctx)throws Exception {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        player.setPosition(ctx.getAbsolute(new Vec3d(3,3,3)));
        var source=player.getCommandSource().withLevel(2);
        var dispatcher=ctx.getWorld().getServer().getCommandManager().getDispatcher();
        int result=dispatcher.execute("rustandruin summon minecraft:wither ~ ~ ~",source);
        ctx.assertTrue(result==1,"Namespaced summon command failed");
        var wither=ctx.getWorld().getEntitiesByClass(WitherEntity.class,player.getBoundingBox().expand(5),e->true).get(0);
        ctx.assertTrue(MobVariants.permanent(wither)&&wither.getMaxHealth()==600,"Command summoned ordinary wither");
        wither.damage(ctx.getWorld().getDamageSources().playerAttack(player),10000);
        var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id("nice_one"));
        ctx.assertTrue(advancement!=null&&player.getAdvancementTracker().getProgress(advancement).isDone(),"Wither kill advancement missing");
        var rod=new ItemEntity(ctx.getWorld(),player.getX(),player.getY(),player.getZ(),new ItemStack(Items.FISHING_ROD));rod.setThrower(player.getUuid());
        FishingSalvage.breakRod(rod,0);
        var hook=player.getServer().getAdvancementLoader().get(TetanusMod.id("rusty_hook"));
        ctx.assertTrue(hook!=null&&player.getAdvancementTracker().getProgress(hook).isDone(),"Hook salvage advancement missing");
        int invalid=0;try{dispatcher.execute("rustandruin summon minecraft:item",source);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){invalid++;}
        ctx.assertTrue(invalid==1,"Summon accepted a non-mob");player.discard();ctx.complete();
    }
}
