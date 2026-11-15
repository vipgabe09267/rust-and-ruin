package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.beacon.*;
import com.tetanusmod.client.VariantSkinPalette;
import com.tetanusmod.expansion.ExpansionBlocks;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.*;
import net.minecraft.screen.*;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import java.util.*;

/** Actual inventory payments, current base validation, saved selections and dark-skin regressions. */
public final class BeaconUpgradeGameTests implements FabricGameTest {
    private static final String SPACE="tetanusmod:test/camp_empty";
    private record Fixture(PlayerEntity player,BlockPos pos,TetanizedBeaconEntity beacon,TetanizedBeaconScreenHandler menu) { }
    private Fixture fixture(TestContext ctx) { return fixture(ctx,ctx.createMockSurvivalPlayer()); }
    private Fixture fixture(TestContext ctx,PlayerEntity player) {
        var relative=new BlockPos(7,6,7);var pos=ctx.getAbsolutePos(relative);
        for(int layer=1;layer<=4;layer++)for(int x=-layer;x<=layer;x++)for(int z=-layer;z<=layer;z++)ctx.setBlockState(relative.add(x,-layer,z),ExpansionBlocks.RUSTED_IRON_BLOCK);
        ctx.setBlockState(relative,ModBeacons.BLOCK);
        var beacon=(TetanizedBeaconEntity)ctx.getWorld().getBlockEntity(pos);
        player.setPosition(Vec3d.ofCenter(pos).add(0,1,0));
        var menu=(TetanizedBeaconScreenHandler)beacon.createMenu(1,player.getInventory(),player);
        return new Fixture(player,pos,beacon,menu);
    }
    private void pay(Fixture f) {
        f.menu.getSlot(0).setStack(new ItemStack(Items.NETHERITE_INGOT));
        f.menu.getSlot(TetanizedBeaconScreenHandler.STAR_SLOT).setStack(new ItemStack(ModItems.TETANIZED_WITHER_STAR));
    }
    private void chooseIV(Fixture f) {
        f.menu.onButtonClick(f.player,TetanizedBeaconScreenHandler.SELECT_LEVEL_IV);
        f.menu.setEffects(Optional.of(StatusEffects.HASTE),Optional.of(StatusEffects.HASTE));
    }
    @GameTest(templateName=SPACE,batchId="beacon_upgrade")
    public void levelIVConsumesOneStarAndOneIngotForEveryConfirmation(TestContext ctx) {
        var f=fixture(ctx);pay(f);chooseIV(f);
        ctx.assertTrue(f.beacon.isLevelIV()&&f.menu.getPrimaryEffect()==StatusEffects.HASTE&&f.menu.getSecondaryEffect()==StatusEffects.HASTE,"Full beacon did not retain paid level-IV selection");
        ctx.assertTrue(f.menu.getSlot(0).getStack().isEmpty()&&f.menu.getSlot(TetanizedBeaconScreenHandler.STAR_SLOT).getStack().isEmpty(),"Level IV did not consume exactly one of both payments");
        f.menu.getSlot(0).setStack(new ItemStack(Items.NETHERITE_INGOT));chooseIV(f);
        ctx.assertTrue(f.menu.getSlot(0).getStack().isOf(Items.NETHERITE_INGOT)&&f.beacon.isLevelIV(),"Reconfirming IV without another star spent payment or revoked existing power");
        pay(f);chooseIV(f);ctx.assertTrue(f.menu.getSlot(0).getStack().isEmpty()&&f.menu.getSlot(TetanizedBeaconScreenHandler.STAR_SLOT).getStack().isEmpty(),"Second IV confirmation reused a previously paid star");ctx.complete();
    }
    @GameTest(templateName=SPACE,batchId="beacon_upgrade")
    public void invalidIVAndReplayedRequestCannotSpendPaymentsOrGrantIV(TestContext ctx) {
        var f=fixture(ctx);pay(f);
        f.menu.onButtonClick(f.player,TetanizedBeaconScreenHandler.SELECT_LEVEL_IV);
        f.menu.setEffects(Optional.of(StatusEffects.HASTE),Optional.of(StatusEffects.REGENERATION));
        ctx.assertTrue(!f.beacon.isLevelIV()&&f.menu.getSlot(0).hasStack()&&f.menu.hasStar(),"Invalid IV regeneration selection consumed payments or granted IV");
        // No second native button request: the failed IV request must have been cleared.
        f.menu.setEffects(Optional.of(StatusEffects.HASTE),Optional.of(StatusEffects.HASTE));
        ctx.assertTrue(!f.beacon.isLevelIV()&&f.menu.getSlot(0).getStack().isEmpty()&&f.menu.hasStar(),"Stale request upgraded a later ordinary selection");
        f.menu.getSlot(0).setStack(new ItemStack(Items.NETHERITE_INGOT));
        ctx.assertFalse(f.menu.onButtonClick(f.player,Integer.MAX_VALUE),"Unlisted menu button accepted");
        f.menu.setEffects(Optional.of(StatusEffects.POISON),Optional.of(StatusEffects.POISON));
        ctx.assertTrue(f.menu.getSlot(0).hasStack()&&f.menu.hasStar()&&!f.beacon.isLevelIV(),"Invalid effect bypassed validation");ctx.complete();
    }
    @GameTest(templateName=SPACE,batchId="beacon_upgrade")
    public void missingPyramidIsRecheckedBeforeChargingEvenWithCachedLevelFour(TestContext ctx) {
        var f=fixture(ctx);pay(f);f.menu.setProperty(0,4);
        ctx.getWorld().setBlockState(f.pos.add(4,-4,4),Blocks.AIR.getDefaultState());chooseIV(f);
        ctx.assertTrue(!f.beacon.isLevelIV()&&f.menu.getSlot(0).hasStack()&&f.menu.hasStar(),"A stale four-layer property charged for IV after the base broke");ctx.complete();
    }
    @GameTest(templateName=SPACE,batchId="beacon_upgrade")
    public void outOfRangeAndReplacedBeaconMenusCannotChargeAnUpgrade(TestContext ctx) {
        var f=fixture(ctx);pay(f);
        f.player.setPosition(Vec3d.ofCenter(f.pos).add(20,0,0));
        ctx.assertFalse(f.menu.onButtonClick(f.player,TetanizedBeaconScreenHandler.SELECT_LEVEL_IV),"Out-of-range upgrade request accepted");
        f.menu.setEffects(Optional.of(StatusEffects.HASTE),Optional.of(StatusEffects.HASTE));
        ctx.assertTrue(f.menu.getSlot(0).hasStack()&&f.menu.hasStar()&&!f.beacon.isLevelIV(),"Out-of-range menu spent currency");
        f.player.setPosition(Vec3d.ofCenter(f.pos));f.menu.onButtonClick(f.player,TetanizedBeaconScreenHandler.SELECT_LEVEL_IV);
        ctx.getWorld().setBlockState(f.pos,Blocks.AIR.getDefaultState());ctx.getWorld().setBlockState(f.pos,ModBeacons.BLOCK.getDefaultState());
        f.menu.setEffects(Optional.of(StatusEffects.HASTE),Optional.of(StatusEffects.HASTE));
        var replacement=(TetanizedBeaconEntity)ctx.getWorld().getBlockEntity(f.pos);
        ctx.assertTrue(!replacement.isLevelIV()&&!f.beacon.isLevelIV()&&f.menu.getSlot(0).hasStack()&&f.menu.hasStar(),"Old menu charged or powered a replacement beacon");ctx.complete();
    }
    @GameTest(templateName=SPACE,batchId="beacon_upgrade")
    public void paidIVSurvivesNativeNbtButNormalSelectionClearsIt(TestContext ctx) {
        var f=fixture(ctx);pay(f);chooseIV(f);
        var nbt=f.beacon.createNbtWithId();
        var loaded=BlockEntity.createFromNbt(f.pos,ModBeacons.BLOCK.getDefaultState(),nbt);
        ctx.assertTrue(loaded instanceof TetanizedBeaconEntity&&((TetanizedBeaconEntity)loaded).isLevelIV(),"Current paid IV selection was lost after native NBT reload");
        f.menu.getSlot(0).setStack(new ItemStack(Items.NETHERITE_INGOT));
        f.menu.setEffects(Optional.of(StatusEffects.SPEED),Optional.of(StatusEffects.REGENERATION));
        ctx.assertTrue(!f.beacon.isLevelIV()&&f.menu.getPrimaryEffect()==StatusEffects.SPEED&&f.menu.getSecondaryEffect()==StatusEffects.REGENERATION,"A later standard selection retained a free IV flag");
        var old=f.beacon.createNbt();old.remove("RustRuinLevelIV");f.beacon.readNbt(old);ctx.assertFalse(f.beacon.isLevelIV(),"Legacy beacon without a paid flag gained IV");ctx.complete();
    }
    @GameTest(templateName=SPACE,batchId="beacon_upgrade")
    public void levelIVNeedsFourLayersToApplyAndKeepsPassiveResistance(TestContext ctx) {
        var f=fixture(ctx);pay(f);chooseIV(f);
        var player=ctx.createMockCreativeServerPlayerInWorld();player.setPosition(Vec3d.ofCenter(f.pos));
        BeaconPower.apply(ctx.getWorld(),f.pos,4,StatusEffects.HASTE,StatusEffects.HASTE);
        ctx.assertTrue(player.getStatusEffect(StatusEffects.HASTE)!=null&&player.getStatusEffect(StatusEffects.HASTE).getAmplifier()==3,"Paid beacon did not grant Haste IV");
        ctx.assertTrue(player.hasStatusEffect(TetanusMod.VACCINATED),"IV selection removed passive Tetanus Resistance");
        player.removeStatusEffect(StatusEffects.HASTE);BeaconPower.apply(ctx.getWorld(),f.pos,3,StatusEffects.HASTE,StatusEffects.HASTE);
        ctx.assertTrue(player.getStatusEffect(StatusEffects.HASTE).getAmplifier()==1,"Incomplete pyramid retained level-IV strength");player.discard();ctx.complete();
    }
    @GameTest(templateName=SPACE,batchId="beacon_upgrade")
    public void starSlotShiftClicksExactlyOneAndReturnsUnusedCurrency(TestContext ctx) {
        // Vanilla returns container inventory only for a genuine server player, not
        // createMockSurvivalPlayer's bare PlayerEntity test double.
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try {
            player.changeGameMode(GameMode.SURVIVAL);
            var f=fixture(ctx,player);player.getInventory().clear();
            ctx.assertTrue(player.isAlive()&&!player.isDisconnected(),"Close-menu player fixture is dead or disconnected");
            ctx.assertFalse(f.menu.getSlot(0).canInsert(new ItemStack(ModItems.TETANIZED_WITHER_STAR)),"Star is accepted as ordinary ingot payment");
            ctx.assertFalse(f.menu.getSlot(TetanizedBeaconScreenHandler.STAR_SLOT).canInsert(new ItemStack(Items.NETHER_STAR)),"Clean star is accepted for IV");
            f.menu.getSlot(1).setStack(new ItemStack(ModItems.TETANIZED_WITHER_STAR,2));f.menu.quickMove(player,1);
            ctx.assertTrue(f.menu.getSlot(TetanizedBeaconScreenHandler.STAR_SLOT).getStack().getCount()==1&&f.menu.getSlot(1).getStack().getCount()==1,"Shift-click did not move exactly one star into the one-item slot");
            f.menu.quickMove(player,TetanizedBeaconScreenHandler.STAR_SLOT);
            ctx.assertTrue(!f.menu.hasStar()&&player.getInventory().count(ModItems.TETANIZED_WITHER_STAR)==2,"Shift-clicking unused star back lost or duplicated it");
            f.menu.getSlot(TetanizedBeaconScreenHandler.STAR_SLOT).setStack(new ItemStack(ModItems.TETANIZED_WITHER_STAR));
            int before=player.getInventory().count(ModItems.TETANIZED_WITHER_STAR);
            var area=new Box(player.getBlockPos()).expand(8);
            int droppedBefore=ctx.getWorld().getEntitiesByClass(ItemEntity.class,area,e->e.getStack().isOf(ModItems.TETANIZED_WITHER_STAR)).stream().mapToInt(e->e.getStack().getCount()).sum();
            f.menu.onClosed(player);
            int after=player.getInventory().count(ModItems.TETANIZED_WITHER_STAR),remaining=f.menu.getSlot(TetanizedBeaconScreenHandler.STAR_SLOT).getStack().getCount();
            int droppedAfter=ctx.getWorld().getEntitiesByClass(ItemEntity.class,area,e->e.getStack().isOf(ModItems.TETANIZED_WITHER_STAR)).stream().mapToInt(e->e.getStack().getCount()).sum();
            ctx.assertTrue(remaining==0&&after==before+1&&droppedAfter==droppedBefore,"Native close did not return exactly one unused star: inventory="+before+"->"+after+", slot="+remaining+", drops="+droppedBefore+"->"+droppedAfter+", disconnected="+player.isDisconnected());
        } finally { player.discard(); }
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void blackEndermanSkinGetsVisibleRustWithoutLosingTransparencyOrPurpleDetail(TestContext ctx) {
        int rust=0,changed=0,seed=new net.minecraft.util.Identifier("minecraft","textures/entity/enderman/enderman.png").hashCode();
        for(int x=0;x<64;x++)for(int y=0;y<32;y++) {
            int pixel=0xFF101010;
            int color=VariantSkinPalette.tetanized(pixel,x,y,seed,true);
            if((color&255)>80)rust++;
            if(color!=pixel)changed++;
            ctx.assertTrue((color>>>24)==255,"Enderman recoloring changed its alpha");
        }
        ctx.assertTrue(rust>300&&rust<1300&&changed>1900,"Dark Enderman skin remains unreadably black or uniformly orange: rust="+rust+", changed="+changed);
        ctx.assertTrue(VariantSkinPalette.tetanized(0x00010203,2,3,seed,true)==0x00010203,"Transparent UV pixels became visible");
        ctx.assertTrue(VariantSkinPalette.tetanized(0xFFDF20E0,2,3,seed,true)==0xFFDF20E0,"Purple Enderman face detail was recolored");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void ordinaryMobPaletteKeepsItsExistingColors(TestContext ctx) {
        ctx.assertTrue(VariantSkinPalette.tetanized(0xFF101010,0,0,0,false)==0xFF0A0C10,"Ordinary dark mob palette changed");
        ctx.assertTrue(VariantSkinPalette.tetanized(0xFF646464,0,0,0,false)==0xFF183A87,"Ordinary rusty patch palette changed");ctx.complete();
    }
}
