package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.InfectionState;
import com.tetanusmod.expansion.CommandEffects;
import com.tetanusmod.expansion.ExpansionEffects;
import com.tetanusmod.expansion.MedicCases;
import com.tetanusmod.expansion.MedicVillage;
import com.tetanusmod.item.FieldSplintItem;
import com.tetanusmod.medical.ClinicNoticeBoardScreenHandler;
import com.tetanusmod.medical.MedicalWorkstations;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.UUID;

public final class UtilityGameTests implements FabricGameTest {
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noticeBoardOpensNativelyAndCannotReplaceAnActiveCase(TestContext ctx) {
        var world = ctx.getWorld();
        var player = ctx.createMockCreativeServerPlayerInWorld();
        var visitor = ctx.createMockCreativeServerPlayerInWorld();
        var pos = ctx.getAbsolutePos(new BlockPos(2, 3, 2));
        var standing = Vec3d.ofBottomCenter(pos.north());
        var block = MedicalWorkstations.BOARD.getDefaultState();
        world.setBlockState(pos, block);
        try {
            player.setPosition(standing);
            visitor.setPosition(standing);
            var hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.NORTH, pos, false);
            ctx.assertTrue(block.onUse(world, player, Hand.MAIN_HAND, hit).isAccepted()
                    && player.currentScreenHandler instanceof ClinicNoticeBoardScreenHandler, "Notice board did not open its native menu");
            var menu = (ClinicNoticeBoardScreenHandler) player.currentScreenHandler;
            int patrol = ClinicNoticeBoardScreenHandler.CASES.indexOf(MedicCases.CaseFile.CAVE_PATROL);
            ctx.assertTrue(menu.selection() == ClinicNoticeBoardScreenHandler.AVAILABLE, "New player cannot start a case");
            ctx.assertFalse(menu.onButtonClick(visitor, patrol), "Another player used the menu");
            ctx.assertFalse(menu.onButtonClick(player, 1000), "Invalid selection was accepted");
            player.setPosition(standing.add(12, 0, 0));
            ctx.assertFalse(menu.onButtonClick(player, patrol), "Out-of-reach board accepted a case");
            player.setPosition(standing);
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            ctx.assertFalse(menu.onButtonClick(player, patrol), "Removed board accepted a case");
            world.setBlockState(pos, block);
            ctx.assertTrue(menu.onButtonClick(player, patrol) && menu.selection() == patrol
                    && MedicCases.progress(player).activeCase() == MedicCases.CaseFile.CAVE_PATROL, "Board did not select the requested patrol");
            ctx.assertFalse(menu.onButtonClick(player, 0), "A second selection overwrote the active case");
            ctx.assertTrue(MedicCases.progress(visitor).activeCase() == null, "Case leaked between players");
            player.closeHandledScreen();
            ctx.assertFalse(menu.onButtonClick(player, patrol), "Closed menu accepted input");
            ctx.complete();
        } finally {
            player.closeHandledScreen();
            player.discard();
            visitor.discard();
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
        }
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void medicDropsEverythingThatOnlyPartiallyFits(TestContext ctx) {
        var player = ctx.createMockCreativeServerPlayerInWorld();
        player.getAbilities().creativeMode = false;
        var medic = EntityType.VILLAGER.create(ctx.getWorld());
        var center = ctx.getAbsolute(new Vec3d(2.5, 3, 2.5));
        player.setPosition(center);
        medic.setPosition(center);
        medic.setVillagerData(medic.getVillagerData().withProfession(MedicVillage.MEDIC));
        var data = new NbtCompound();
        var file = new NbtCompound();
        file.putString("File", "herd_checkup");
        file.putInt("Step", 2);
        data.put("RustRuinMedicCase", file);
        MedicCases.progress(player).read(data);
        for (int i = 0; i < 36; i++) player.getInventory().setStack(i, new ItemStack(Items.DIRT, 64));
        player.getInventory().setStack(1, new ItemStack(Items.EMERALD, 62));
        player.getInventory().setStack(2, new ItemStack(Items.GOLDEN_CARROT, 63));
        try {
            player.setSneaking(true);
            UseEntityCallback.EVENT.invoker().interact(player, ctx.getWorld(), Hand.MAIN_HAND, medic, null);
            var drops = ctx.getWorld().getEntitiesByClass(ItemEntity.class, player.getBoundingBox().expand(3),
                    drop -> drop.getStack().isOf(Items.EMERALD) || drop.getStack().isOf(Items.GOLDEN_CARROT));
            try {
                int coins = drops.stream().filter(drop -> drop.getStack().isOf(Items.EMERALD)).mapToInt(drop -> drop.getStack().getCount()).sum();
                int carrots = drops.stream().filter(drop -> drop.getStack().isOf(Items.GOLDEN_CARROT)).mapToInt(drop -> drop.getStack().getCount()).sum();
                ctx.assertTrue(coins == 3 && carrots == 3 && player.getInventory().count(Items.EMERALD) == 64
                        && player.getInventory().count(Items.GOLDEN_CARROT) == 64, "A partially inserted reward disappeared");
                ctx.assertTrue(MedicCases.completedToday(player) && !MedicCases.accept(player, MedicCases.CaseFile.CAMP_RATIONS),
                        "Selecting a new case bypassed the daily limit");
            } finally {
                drops.forEach(ItemEntity::discard);
            }
        } finally {
            player.discard();
            medic.discard();
        }
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void magnetMovesOnlyReachableSalvageAndChargesOneDurability(TestContext ctx) {
        var world = ctx.getWorld();
        var player = ctx.createMockCreativeServerPlayerInWorld();
        player.getAbilities().creativeMode = false;
        var center = ctx.getAbsolute(new Vec3d(5.5, 4, 5.5));
        player.setPosition(center);
        var magnet = new ItemStack(ModItems.RUST_MAGNET);
        player.setStackInHand(Hand.OFF_HAND, magnet);
        var wall = BlockPos.ofFloored(center.add(0, 0, 2));
        world.setBlockState(wall, Blocks.STONE.getDefaultState());
        var items = new ArrayList<ItemEntity>();
        try {
            var nails = drop(ctx, items, center.add(5.5, 0, 0), ModItems.NAIL);
            var rust = drop(ctx, items, center.add(-2, 0, 0), ModItems.RUST);
            rust.setOwner(player.getUuid());
            var copper = drop(ctx, items, center.add(2, 0, -2), Items.COPPER_INGOT);
            var reserved = drop(ctx, items, center.add(-2, 0, -2), ModItems.RUST);
            reserved.setOwner(UUID.randomUUID());
            var delayed = drop(ctx, items, center.add(-3, 0, 1), ModItems.NAIL);
            delayed.setPickupDelay(30);
            var stone = drop(ctx, items, center.add(2, 0, 2), Items.COBBLESTONE);
            var blocked = drop(ctx, items, center.add(0, 0, 3.5), Items.IRON_INGOT);
            var distant = drop(ctx, items, center.add(0, 8, 0), Items.IRON_NUGGET);
            var result = ModItems.RUST_MAGNET.use(world, player, Hand.OFF_HAND);
            ctx.assertTrue(result.getResult().isAccepted() && magnet.getDamage() == 1
                    && nails.getVelocity().x < 0 && rust.getVelocity().x > 0 && copper.getVelocity().x < 0,
                    "Single offhand pulse failed to move eligible metal items");
            for (var excluded : new ItemEntity[]{delayed, reserved, stone, blocked, distant}) {
                ctx.assertTrue(excluded.getVelocity().equals(Vec3d.ZERO), "Pulled reserved, obstructed, delayed, or unrelated items");
            }
            ctx.assertFalse(ModItems.RUST_MAGNET.use(world, player, Hand.OFF_HAND).getResult().isAccepted(), "Cooldown allowed a second pulse");
            ctx.assertTrue(magnet.getDamage() == 1, "Cooldown consumed extra durability");
            double before = nails.squaredDistanceTo(player);
            for (int i = 0; i < 4; i++) world.tickEntity(nails);
            ctx.assertTrue(nails.squaredDistanceTo(player) < before && nails.getStack().getCount() == 1
                    && player.getInventory().count(ModItems.NAIL) == 0, "Pulse moved items without preserving normal pickup and stack count");
            nails.discard();
            rust.discard();
            copper.discard();
            player.getItemCooldownManager().remove(ModItems.RUST_MAGNET);
            ctx.assertFalse(ModItems.RUST_MAGNET.use(world, player, Hand.OFF_HAND).getResult().isAccepted(), "Empty pulse succeeded");
            ctx.assertTrue(magnet.getDamage() == 1, "Empty pulse wore out the magnet");
        } finally {
            items.forEach(ItemEntity::discard);
            player.discard();
            world.setBlockState(wall, Blocks.AIR.getDefaultState());
        }
        ctx.complete();
    }

    private ItemEntity drop(TestContext ctx, ArrayList<ItemEntity> items, Vec3d at, Item item) {
        var entity = new ItemEntity(ctx.getWorld(), at.x, at.y, at.z, new ItemStack(item));
        entity.setVelocity(Vec3d.ZERO);
        ctx.assertTrue(ctx.getWorld().spawnEntity(entity), "Item fixture did not spawn");
        items.add(entity);
        return entity;
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void splintConsumesOnlyDuringTetanusAndPreservesLockjawAndCure(TestContext ctx) {
        var player = ctx.createMockCreativeServerPlayerInWorld();
        player.getAbilities().creativeMode = false;
        var splints = new ItemStack(ModItems.FIELD_SPLINT, 2);
        player.setStackInHand(Hand.MAIN_HAND, splints);
        double frequency = TetanusMod.CONFIG.spasmFrequency;
        try {
            ctx.assertFalse(ModItems.FIELD_SPLINT.use(ctx.getWorld(), player, Hand.MAIN_HAND).getResult().isAccepted(),
                    "A healthy player used a splint");
            ctx.assertTrue(splints.getCount() == 2, "Healthy player lost a splint");
            CommandEffects.apply(player, new StatusEffectInstance(TetanusMod.TETANUS, 9000), null);
            var state = (InfectionState) player;
            TetanusMod.CONFIG.spasmFrequency = 5;
            int baseline = spasms(player, state);
            state.tetanus$setInfection(7000, 9000);
            state.tetanus$setLockjaw(2000);
            state.tetanus$setFreeze(4);
            state.tetanus$setLockjawWindow(2400);
            player.addStatusEffect(new StatusEffectInstance(TetanusMod.LOCKJAW, 2000));
            int remaining = player.getStatusEffect(TetanusMod.TETANUS).getDuration();
            var result = ModItems.FIELD_SPLINT.use(ctx.getWorld(), player, Hand.MAIN_HAND);
            ctx.assertTrue(result.getResult().isAccepted() && splints.getCount() == 1 && state.tetanus$getFreeze() == 0
                    && player.getStatusEffect(ExpansionEffects.SPLINTED).getDuration() == FieldSplintItem.DURATION_SECONDS * 20,
                    "Splint did not relieve the current spasm for one dose");
            ctx.assertTrue(player.getStatusEffect(TetanusMod.TETANUS).getDuration() == remaining
                    && state.tetanus$getElapsed() == 7000 && state.tetanus$getLockjaw() == 2000 && player.hasStatusEffect(TetanusMod.LOCKJAW),
                    "Splint changed infection duration, progress, or Lockjaw");
            ctx.assertFalse(ModItems.FIELD_SPLINT.use(ctx.getWorld(), player, Hand.MAIN_HAND).getResult().isAccepted(),
                    "A second splint refreshed the effect early");
            ctx.assertTrue(splints.getCount() == 1, "An early reapplication consumed a splint");
            int damped = spasms(player, state);
            ctx.assertTrue(baseline > 140 && damped > 10 && damped < baseline / 2, "Splint did not reduce native spasm frequency");
            var saved = player.writeNbt(new NbtCompound());
            player.removeStatusEffect(ExpansionEffects.SPLINTED);
            player.readNbt(saved);
            ctx.assertTrue(player.hasStatusEffect(TetanusMod.TETANUS) && player.hasStatusEffect(ExpansionEffects.SPLINTED),
                    "Native reload lost saved splint state");
            ModItems.ANTITOXIN.finishUsing(new ItemStack(ModItems.ANTITOXIN), ctx.getWorld(), player);
            ctx.assertFalse(player.hasStatusEffect(TetanusMod.TETANUS) || player.hasStatusEffect(ExpansionEffects.SPLINTED),
                    "Antitoxin did not clear the splinted infection");
            CommandEffects.apply(player, new StatusEffectInstance(TetanusMod.TETANUS, 1), null);
            ModItems.FIELD_SPLINT.use(ctx.getWorld(), player, Hand.MAIN_HAND);
            // Effects on a connected player advance through the player tick.
            player.playerTick();
            ctx.assertFalse(player.hasStatusEffect(TetanusMod.TETANUS) || player.hasStatusEffect(ExpansionEffects.SPLINTED),
                    "Native recovery did not clear the infection and splint");
        } finally {
            TetanusMod.CONFIG.spasmFrequency = frequency;
            player.discard();
        }
        ctx.complete();
    }

    private int spasms(ServerPlayerEntity player, InfectionState state) {
        player.getRandom().setSeed(10866L);
        int count = 0;
        for (int i = 0; i < 240; i++) {
            state.tetanus$setFreeze(0);
            state.tetanus$setInfection(3200, 9000);
            TetanusMod.TETANUS.applyUpdateEffect(player, 0);
            if (state.tetanus$getFreeze() > 0) count++;
        }
        return count;
    }
}
