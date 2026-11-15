package com.tetanusmod.test;

import com.tetanusmod.dragon.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.*;
import net.minecraft.block.enums.BedPart;
import net.minecraft.enchantment.*;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.*;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.*;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.util.*;

/** Native repair paths stay narrow; attribution never changes explosion damage or leaks between attacks. */
public final class DragonRepairAttributionGameTests implements FabricGameTest {
    private static final String SPACE="tetanusmod:test/camp_empty";
    private ItemStack damaged() {var sword=DragonContent.SWORD.getDefaultStack();sword.setDamage(1000);return sword;}
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void overloadCannotBeRepairedByCombiningInCraftingOrGrindstone(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();var grid=new CraftingInventory(new CraftingScreenHandler(1,player.getInventory()),2,2);
        grid.setStack(0,damaged());grid.setStack(1,damaged());
        ctx.assertTrue(ctx.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING,grid,ctx.getWorld()).isEmpty(),"Two Overload Swords repaired in crafting");
        var grindstone=new GrindstoneScreenHandler(2,player.getInventory());grindstone.getSlot(0).setStack(damaged());grindstone.getSlot(1).setStack(damaged());
        ctx.assertTrue(grindstone.getSlot(2).getStack().isEmpty(),"Two Overload Swords repaired at a grindstone");
        grid.clear();var iron=new ItemStack(Items.IRON_SWORD);iron.setDamage(150);grid.setStack(0,iron);grid.setStack(1,iron.copy());
        ctx.assertTrue(ctx.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING,grid,ctx.getWorld()).isPresent(),"Overload restriction broke normal sword crafting repair");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void oneSwordCanBeDisenchantedWithoutRepairingItsDamage(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();var menu=new GrindstoneScreenHandler(1,player.getInventory());menu.getSlot(0).setStack(damaged());
        var output=menu.getSlot(2).getStack();
        ctx.assertTrue(output.isOf(DragonContent.SWORD)&&output.getDamage()==1000&&!output.hasEnchantments(),"Single-sword disenchant repaired damage or was wrongly blocked");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void anvilRejectsSameSwordAndIngotsButAcceptsNetheriteBlocks(TestContext ctx) {
        var player=ctx.createMockSurvivalPlayer();player.experienceLevel=50;
        var menu=new AnvilScreenHandler(1,player.getInventory());menu.getSlot(0).setStack(damaged());menu.getSlot(1).setStack(damaged());
        ctx.assertTrue(menu.getSlot(2).getStack().isEmpty()&&menu.getLevelCost()==0,"Same-item anvil repair bypassed the material restriction");
        menu.getSlot(1).setStack(new ItemStack(Items.NETHERITE_INGOT));ctx.assertTrue(menu.getSlot(2).getStack().isEmpty(),"A netherite ingot repaired the Overload Sword");
        menu.getSlot(1).setStack(new ItemStack(Items.NETHERITE_BLOCK));
        ctx.assertTrue(menu.getSlot(2).getStack().isOf(DragonContent.SWORD)&&menu.getSlot(2).getStack().getDamage()<1000,"Netherite block repair was rejected");
        menu.onSlotClick(2,0,SlotActionType.PICKUP,player);
        ctx.assertTrue(menu.getCursorStack().isOf(DragonContent.SWORD)&&menu.getCursorStack().getDamage()<1000&&menu.getSlot(1).getStack().isEmpty(),"Native anvil take did not consume the repair block");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void swordBooksAndNativeMendingStillWork(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try {
            player.changeGameMode(GameMode.SURVIVAL);player.getInventory().clear();
            var menu=new AnvilScreenHandler(1,player.getInventory());menu.getSlot(0).setStack(damaged());
            menu.getSlot(1).setStack(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS,5)));
            ctx.assertTrue(EnchantmentHelper.getLevel(Enchantments.SHARPNESS,menu.getSlot(2).getStack())==5,"Repair guard rejected an ordinary sword enchantment book");
            menu.getSlot(1).setStack(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.MENDING,1)));
            var sword=menu.getSlot(2).getStack().copy();ctx.assertTrue(sword.isOf(DragonContent.SWORD)&&EnchantmentHelper.getLevel(Enchantments.MENDING,sword)==1,"Sword rejected Mending at the anvil");
            sword.setDamage(40);player.setStackInHand(Hand.MAIN_HAND,sword);player.experiencePickUpDelay=0;
            var orb=new ExperienceOrbEntity(ctx.getWorld(),player.getX(),player.getY(),player.getZ(),5);ctx.getWorld().spawnEntity(orb);orb.onPlayerCollision(player);
            ctx.assertTrue(sword.getDamage()==30,"Native XP pickup failed to repair the held Overload Sword: "+sword.getDamage());
        } finally { player.discard(); }
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void explosionAttributionRequiresExactSourceAndAlwaysRestoresOuterScope(TestContext ctx) {
        var first=ctx.createMockCreativeServerPlayerInWorld();var second=ctx.createMockCreativeServerPlayerInWorld();
        try {
            var world=ctx.getWorld();var source=world.getDamageSources().badRespawnPoint(Vec3d.ZERO);var other=world.getDamageSources().badRespawnPoint(Vec3d.ZERO);
            ctx.assertTrue(source.getAttacker()==null,"Native bad-respawn damage unexpectedly has an attacker");
            DragonExplosionAttribution.run(first,world,source,()->{
                ctx.assertTrue(DragonExplosionAttribution.player(source,world)==first,"Exact native explosion lacks its actor");
                ctx.assertTrue(DragonExplosionAttribution.player(other,world)==null,"Unrelated explosion borrowed another actor");
                DragonExplosionAttribution.run(second,world,other,()->{ctx.assertTrue(DragonExplosionAttribution.player(other,world)==second,"Nested explosion lost its own actor");return null;});
                ctx.assertTrue(DragonExplosionAttribution.player(source,world)==first,"Nested scope failed to restore its outer actor");return null;
            });
            ctx.assertTrue(DragonExplosionAttribution.player(source,world)==null&&source.getAttacker()==null,"Attribution escaped its scope or modified vanilla DamageSource");
            boolean caught=false;try {DragonExplosionAttribution.run(first,world,source,()->{throw new IllegalStateException("intentional attribution cleanup control");});}catch(IllegalStateException expected){caught=true;}
            ctx.assertTrue(caught&&DragonExplosionAttribution.player(source,world)==null,"Exceptional explosion left stale player attribution");
        } finally { first.discard();second.discard(); }
        ctx.complete();
    }
    private static final class RecorderPig extends PigEntity {
        UUID actor;DamageSource source;
        RecorderPig(World world) {super(EntityType.PIG,world);}
        @Override public boolean damage(DamageSource damage,float amount) {
            if(damage.isOf(DamageTypes.BAD_RESPAWN_POINT)){source=damage;var player=DragonExplosionAttribution.player(damage,getWorld());actor=player==null?null:player.getUuid();}
            return super.damage(damage,amount);
        }
    }
    @GameTest(templateName=SPACE,batchId="dragon_explosion_attribution")
    public void actualAnchorExplosionKeepsItsSourceAndCreditsTheUsingPlayer(TestContext ctx) {
        var player=ctx.createMockCreativeServerPlayerInWorld();var pig=new RecorderPig(ctx.getWorld());
        var pos=ctx.getAbsolutePos(new BlockPos(16,4,16));
        try {
            player.setPosition(Vec3d.ofCenter(pos).add(0,3,0));player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);player.setStackInHand(Hand.OFF_HAND,ItemStack.EMPTY);
            pig.setPosition(Vec3d.ofCenter(pos).add(1.5,0,0));pig.setAiDisabled(true);ctx.getWorld().spawnEntity(pig);
            var state=Blocks.RESPAWN_ANCHOR.getDefaultState().with(RespawnAnchorBlock.CHARGES,1);ctx.getWorld().setBlockState(pos,state);
            state.onUse(ctx.getWorld(),player,Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(pos),Direction.UP,pos,false));
            ctx.assertTrue(pig.source!=null&&pig.source.getAttacker()==null&&player.getUuid().equals(pig.actor),"Actual anchor explosion lost its precise using-player association");
            ctx.assertTrue(DragonExplosionAttribution.player(pig.source,ctx.getWorld())==null,"Native anchor explosion leaked attribution after returning");
        } finally {ctx.getWorld().setBlockState(pos,Blocks.AIR.getDefaultState());pig.discard();player.discard();}
        ctx.complete();
    }
}
