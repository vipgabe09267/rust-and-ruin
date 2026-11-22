package com.tetanusmod.test;

import com.tetanusmod.entity.TetanusTntEntity;
import com.tetanusmod.expansion.TetanusTnt;
import com.tetanusmod.test.mixin.FireBlockTestInvoker;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class TetanusTntGameTests implements FabricGameTest {
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void spreadingFirePrimesTheCorrectKindOfTnt(TestContext ctx) {
        var world = ctx.getWorld();
        var fire = (FireBlockTestInvoker) Blocks.FIRE;
        var at = ctx.getAbsolutePos(new BlockPos(2, 9, 2));
        for (var block : new net.minecraft.block.Block[]{Blocks.TNT, TetanusTnt.BLOCK}) {
            world.setBlockState(at, block.getDefaultState());
            try {
                // The native spread roll always succeeds here if the block is flammable.
                fire.rustruin$checkFireSpread(world, at, 1, net.minecraft.util.math.random.Random.create(27), 0);
                var lit = world.getEntitiesByClass(TntEntity.class, new Box(at).expand(0.5), tnt -> true);
                try {
                    ctx.assertTrue(lit.size() == 1 && !world.getBlockState(at).isOf(block), "Fire did not prime TNT: " + block);
                    var tnt = lit.get(0);
                    ctx.assertTrue(tnt.getFuse() == 80 && tnt.getOwner() == null
                            && tnt.getType() == (block == TetanusTnt.BLOCK ? TetanusTnt.ENTITY : EntityType.TNT),
                            "Fire primed the wrong TNT or fuse: " + block);
                } finally {
                    lit.forEach(TntEntity::discard);
                }
            } finally {
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }
        }
        ctx.complete();
    }

    private TetanusTntEntity primedAt(TestContext ctx, BlockPos pos) {
        var entities = ctx.getWorld().getEntitiesByClass(TetanusTntEntity.class, new Box(pos).expand(0.5), tnt -> true);
        ctx.assertTrue(entities.size() == 1, "The ignition path did not spawn exactly one custom TNT entity");
        return entities.get(0);
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void redstoneAndFlintUseVanillaFuseAndPersistIgniter(TestContext ctx) {
        var player = ctx.createMockCreativeServerPlayerInWorld();
        try {
            var world = ctx.getWorld();
            var redstone = ctx.getAbsolutePos(new BlockPos(2, 3, 2));
            world.setBlockState(redstone, TetanusTnt.BLOCK.getDefaultState());
            world.setBlockState(redstone.down(), Blocks.REDSTONE_BLOCK.getDefaultState());
            var powered = primedAt(ctx, redstone);
            ctx.assertTrue(world.isAir(redstone) && powered.getFuse() == 80 && powered.getOwner() == null,
                    "Redstone priming left its block or changed the vanilla fuse");
            powered.discard();
            world.setBlockState(redstone.down(), Blocks.AIR.getDefaultState());

            var lit = redstone.east();
            world.setBlockState(lit, TetanusTnt.BLOCK.getDefaultState());
            player.setPosition(Vec3d.ofCenter(lit.east()));
            player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
            var state = world.getBlockState(lit);
            var result = state.onUse(world, player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(lit), Direction.EAST, lit, false));
            var ignited = primedAt(ctx, lit);
            try {
                ctx.assertTrue(result.isAccepted() && world.isAir(lit) && ignited.getFuse() == 80 && ignited.getOwner() == player,
                        "Flint ignition did not preserve TNT ownership and fuse");
                var saved = ignited.writeNbt(new NbtCompound());
                var loaded = new TetanusTntEntity(TetanusTnt.ENTITY, world);
                loaded.readNbt(saved);
                ctx.assertTrue(loaded.getOwner() == player && loaded.getFuse() == 80, "Igniter or fuse was lost on reload");
            } finally {
                ignited.discard();
            }
        } finally {
            player.discard();
        }
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void dispenserConsumesItemAndPrimesTetanusTnt(TestContext ctx) {
        var world = ctx.getWorld();
        var at = ctx.getAbsolutePos(new BlockPos(2, 3, 2));
        world.setBlockState(at, Blocks.DISPENSER.getDefaultState().with(DispenserBlock.FACING, Direction.EAST));
        var dispenser = (DispenserBlockEntity) world.getBlockEntity(at);
        dispenser.setStack(0, new ItemStack(TetanusTnt.ITEM, 2));
        world.setBlockState(at.down(), Blocks.REDSTONE_BLOCK.getDefaultState());
        ctx.runAtTick(7, () -> {
            var fired = primedAt(ctx, at.east());
            try {
                ctx.assertTrue(dispenser.getStack(0).getCount() == 1 && fired.getFuse() > 65 && fired.getFuse() <= 80,
                        "Dispensing did not consume one item or started an invalid fuse");
            } finally {
                fired.discard();
                world.setBlockState(at.down(), Blocks.AIR.getDefaultState());
                world.setBlockState(at, Blocks.AIR.getDefaultState());
            }
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void chainedBlastUsesTheShortenedFuse(TestContext ctx) {
        var world = ctx.getWorld();
        var at = ctx.getAbsolutePos(new BlockPos(2, 32, 2));
        world.setBlockState(at, TetanusTnt.BLOCK.getDefaultState());
        var center = Vec3d.ofCenter(at.west());
        world.createExplosion(null, center.x, center.y, center.z, 4, World.ExplosionSourceType.TNT);
        var chained = primedAt(ctx, at);
        try {
            ctx.assertTrue(world.isAir(at) && chained.getFuse() >= 10 && chained.getFuse() < 30,
                    "Blast ignition did not use the vanilla short fuse");
        } finally {
            chained.discard();
        }
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void largerBlastPlaysCueAndLeavesTemporaryCosmeticRust(TestContext ctx) {
        var world = ctx.getWorld();
        var center = ctx.getAbsolute(new Vec3d(2.5, 32, 2.5));
        var bounds = new Box(center, center.add(1, 1, 1)).expand(4);
        var cow = EntityType.COW.create(world);
        cow.setPosition(center.add(11, 0, 0));
        cow.setNoGravity(true);
        ctx.assertTrue(world.spawnEntity(cow), "Could not spawn blast-distance fixture");
        SoundProbe.begin(world, bounds);
        try {
            detonate(new TntEntity(EntityType.TNT, world), center);
            ctx.assertTrue(cow.getHealth() == cow.getMaxHealth() && SoundProbe.count == 0
                    && world.getEntitiesByClass(AreaEffectCloudEntity.class, bounds, cloud -> true).isEmpty(),
                    "Vanilla TNT gained the custom TNT reach, cue, or particles");

            detonate(new TetanusTntEntity(TetanusTnt.ENTITY, world), center);
            ctx.assertTrue(cow.getHealth() < cow.getMaxHealth(), "Tetanus TNT missed an exposed entity eleven blocks away");
            ctx.assertTrue(SoundProbe.count == 1 && SoundProbe.position.squaredDistanceTo(center) < 1,
                    "Tetanus TNT did not play the infection voice at the explosion");
            var clouds = world.getEntitiesByClass(AreaEffectCloudEntity.class, bounds, cloud -> true);
            ctx.assertTrue(clouds.size() == 1, "Tetanus TNT did not create its lingering rust cloud");
            var rust = clouds.get(0);
            int duration = rust.getDuration();
            var cloudNbt = rust.writeNbt(new NbtCompound());
            ctx.assertTrue(rust.getParticleType() instanceof DustParticleEffect && duration >= 100 && duration <= 300
                    && cloudNbt.getList("Effects", NbtElement.COMPOUND_TYPE).isEmpty() && cloudNbt.getString("Potion").isEmpty()
                    && rust.getWaitTime() == 0, "Rust cloud has the wrong particles, lifespan, or effects");
            for (int tick = 0; tick < duration - 1; tick++) world.tickEntity(rust);
            ctx.assertTrue(!rust.isRemoved(), "Rust cloud ended early");
            world.tickEntity(rust);
            ctx.assertTrue(rust.isRemoved(), "Rust cloud did not expire");
        } finally {
            SoundProbe.enabled = false;
            cow.discard();
            world.getEntitiesByClass(AreaEffectCloudEntity.class, bounds, cloud -> true).forEach(AreaEffectCloudEntity::discard);
        }
        ctx.complete();
    }

    private void detonate(TntEntity entity, Vec3d center) {
        entity.setPosition(center);
        entity.setVelocity(Vec3d.ZERO);
        entity.setNoGravity(true);
        entity.setFuse(1);
        entity.tick();
    }
}
