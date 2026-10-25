package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.entity.RustGolems;
import com.tetanusmod.expansion.*;
import com.tetanusmod.registry.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

public class PlaytestAdvancementTrapGameTests implements FabricGameTest {
    private ServerPlayerEntity player(TestContext ctx) {
        var p = ctx.createMockCreativeServerPlayerInWorld();
        p.changeGameMode(GameMode.SURVIVAL);
        p.getAbilities().creativeMode = false;
        p.getAbilities().invulnerable = false;
        p.setPosition(ctx.getAbsolute(new Vec3d(2, 3, 3)));
        return p;
    }
    private void awarded(TestContext ctx, ServerPlayerEntity p, String id, boolean expected) {
        var advancement = p.getServer().getAdvancementLoader().get(TetanusMod.id(id));
        ctx.assertTrue(advancement != null && p.getAdvancementTracker().getProgress(advancement).isDone() == expected, "Wrong advancement state: " + id);
    }
    private BearTrapBlockEntity trap(TestContext ctx) {
        ctx.setBlockState(3, 2, 3, Blocks.STONE);
        ctx.setBlockState(3, 3, 3, ExpansionBlocks.BEAR_TRAP);
        return (BearTrapBlockEntity)ctx.getBlockEntity(new BlockPos(3, 3, 3));
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void newSurvivalPlayerSeesRustAndRuinWithoutObtainingIron(TestContext ctx) {
        var p = player(ctx);
        try {
            p.getInventory().clear();
            Criteria.TICK.trigger(p);
            awarded(ctx, p, "root", true);
            awarded(ctx, p, "iron_has_a_darker_side", false);
            ctx.assertTrue(!p.getServer().getAdvancementLoader().get(TetanusMod.id("root")).getDisplay().shouldShowToast(), "Automatic root should not spam a toast");
        } finally { p.discard(); }
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void ironDiscoveryUsesNativeInventoryCriterion(TestContext ctx) {
        var p = player(ctx);
        try {
            var iron = new ItemStack(Items.IRON_INGOT);
            p.getInventory().setStack(0, iron);
            Criteria.INVENTORY_CHANGED.trigger(p, p.getInventory(), iron);
            awarded(ctx, p, "iron_has_a_darker_side", true);
        } finally { p.discard(); }
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rustingANailRevealsItsOwnAdvancementTab(TestContext ctx) {
        var p = player(ctx);
        try {
            var root = p.getServer().getAdvancementLoader().get(TetanusMod.id("root"));
            p.getAdvancementTracker().revokeCriterion(root, "obtain_iron");
            var dropped = new ItemEntity(ctx.getWorld(), p.getX(), p.getY(), p.getZ(), new ItemStack(ModItems.RUSTED_NAIL));
            dropped.setThrower(p.getUuid());
            ModAdvancements.rusted(dropped);
            awarded(ctx, p, "root", true);
            awarded(ctx, p, "rusty_business", true);
        } finally { p.discard(); }
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void projectileConvertedGolemAwardsSpecificGuardian(TestContext ctx) {
        var p = player(ctx);
        float old = TetanusMod.CONFIG.mobConversionChance;
        float oldGolem = TetanusMod.CONFIG.golemConversionChance;
        try {
            TetanusMod.CONFIG.mobConversionChance = 1;
            TetanusMod.CONFIG.golemConversionChance = 1;
            var golem = EntityType.IRON_GOLEM.create(ctx.getWorld());
            MobVariants.projectileExposure(golem, p);
            ctx.assertTrue(MobVariants.permanent(golem) && golem.getMaxHealth() == 200, "Golem did not permanently convert");
            awarded(ctx, p, "tetanized_mob", true);
            awarded(ctx, p, "rusty_guardian", true);
        } finally { TetanusMod.CONFIG.mobConversionChance = old; TetanusMod.CONFIG.golemConversionChance = oldGolem; p.discard(); }
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void failedRabidGolemConversionDoesNotConsumeRustOrGrantGuardian(TestContext ctx) {
        var p = player(ctx);
        try {
            p.setStackInHand(Hand.MAIN_HAND, new ItemStack(ModItems.RUST, 2));
            var golem = EntityType.IRON_GOLEM.create(ctx.getWorld());
            Rabies.makeRabid(golem);
            ctx.assertFalse(RustGolems.attempt(p, Hand.MAIN_HAND, golem, 0), "Incompatible mutation reported success");
            ctx.assertTrue(p.getMainHandStack().getCount() == 2 && !MobVariants.permanent(golem), "Rejected conversion consumed Rust or changed variant");
            awarded(ctx, p, "rusty_guardian", false);
        } finally { p.discard(); }
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void eitherActualVaccineAwardsFaucciOuchie(TestContext ctx) {
        for (var vaccine : new Item[]{ModItems.VACCINE, ExpansionItems.RABIES_VACCINE}) {
            var p = player(ctx);
            try {
                vaccine.finishUsing(new ItemStack(vaccine), ctx.getWorld(), p);
                awarded(ctx, p, "vaccinated", true);
                ctx.assertTrue(p.hasStatusEffect(vaccine == ModItems.VACCINE ? TetanusMod.VACCINATED : ExpansionEffects.RABIES_RESISTANCE), "Vaccine did not protect its user");
            } finally { p.discard(); }
        }
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void cureIsNotAVaccine(TestContext ctx) {
        var p = player(ctx);
        try {
            ExpansionItems.RABIES_CURE.finishUsing(new ItemStack(ExpansionItems.RABIES_CURE), ctx.getWorld(), p);
            awarded(ctx, p, "vaccinated", false);
        } finally { p.discard(); }
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void ordinaryWalkingCanEnterAndTriggerTheBearTrap(TestContext ctx) {
        var trap = trap(ctx);
        for (int x=1;x<=5;x++) ctx.setBlockState(x, 2, 3, Blocks.STONE);
        var cow = ctx.spawnEntity(EntityType.COW, new Vec3d(2, 3, 3.5));
        cow.setAiDisabled(true);
        cow.move(MovementType.SELF, new Vec3d(1.2, 0, 0));
        ctx.assertTrue(trap.holds(cow) && trap.getCachedState().get(BearTrapBlock.CLOSED), "Native movement never reached the bear trap trigger");
        ctx.assertTrue(cow.getHealth() == 7, "Walking into the trap did not deal its single initial hit");
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void trappedMobCannotWalkOrJumpOut(TestContext ctx) {
        var trap = trap(ctx);
        var cow = ctx.spawnEntity(EntityType.COW, new Vec3d(3.5, 3, 3.5));
        trap.trigger(cow);
        var anchor = cow.getPos();
        cow.setPosition(anchor.add(0.8, 0.5, 0.4));
        cow.setVelocity(0.4, 0.42, 0.3);
        BearTrapBlockEntity.tick(ctx.getWorld(), trap.getPos(), trap.getCachedState(), trap);
        ctx.assertTrue(cow.getPos().squaredDistanceTo(anchor) < 0.0001 && cow.getVelocity().lengthSquared() == 0, "Trap allowed the captive to walk or jump free");
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void resetToolWorksThroughNativeSurvivalBlockInteraction(TestContext ctx) {
        var trap = trap(ctx);
        var cow = ctx.spawnEntity(EntityType.COW, new Vec3d(3.5, 3, 3.5));
        trap.trigger(cow);
        var p = player(ctx);
        try {
            p.setStackInHand(Hand.MAIN_HAND, new ItemStack(ExpansionItems.TRAP_RESET_TOOL));
            var hit = new BlockHitResult(Vec3d.ofCenter(trap.getPos()), Direction.UP, trap.getPos(), false);
            var result = p.interactionManager.interactBlock(p, ctx.getWorld(), p.getMainHandStack(), Hand.MAIN_HAND, hit);
            ctx.assertTrue(result.isAccepted() && !trap.getCachedState().get(BearTrapBlock.CLOSED), "Survival interaction did not open the trap");
            ctx.assertTrue(!trap.holds(cow) && !cow.hasStatusEffect(StatusEffects.SLOWNESS), "Reset retained the captive or its trap effect");
            ctx.assertTrue(p.getMainHandStack().getDamage() == 1, "Reset did not use exactly one tool durability");
            awarded(ctx, p, "trap_reset", true);
            ctx.assertFalse(trap.trigger(cow), "Victim was recaptured immediately after resetting");
            for (int i=0;i<BearTrapBlockEntity.RESET_GRACE_TICKS;i++) BearTrapBlockEntity.tick(ctx.getWorld(), trap.getPos(), trap.getCachedState(), trap);
            ctx.assertTrue(trap.trigger(cow), "Trap failed to rearm after the reset grace period");
        } finally { p.discard(); }
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void breakingTrapReleasesTheCaptiveAndRemovesItsSlowness(TestContext ctx) {
        var trap = trap(ctx);
        var cow = ctx.spawnEntity(EntityType.COW, new Vec3d(3.5, 3, 3.5));
        trap.trigger(cow);
        ctx.getWorld().setBlockState(trap.getPos(), Blocks.AIR.getDefaultState());
        ctx.assertFalse(cow.hasStatusEffect(StatusEffects.SLOWNESS), "Broken trap left the victim immobilized");
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void resettingDoesNotCureAnUnrelatedLongOrInfiniteSlowness(TestContext ctx) {
        var trap = trap(ctx);
        var cow = ctx.spawnEntity(EntityType.COW, new Vec3d(3.5, 3, 3.5));
        cow.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 600, 9));
        trap.trigger(cow);
        trap.reset();
        ctx.assertTrue(cow.hasStatusEffect(StatusEffects.SLOWNESS) && cow.getStatusEffect(StatusEffects.SLOWNESS).getDuration() == 600, "Reset cured an unrelated longer Slowness effect");
        for (int i=0;i<BearTrapBlockEntity.RESET_GRACE_TICKS;i++) BearTrapBlockEntity.tick(ctx.getWorld(), trap.getPos(), trap.getCachedState(), trap);
        cow.removeStatusEffect(StatusEffects.SLOWNESS);
        cow.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, -1, 9));
        trap.trigger(cow);
        trap.reset();
        ctx.assertTrue(cow.hasStatusEffect(StatusEffects.SLOWNESS) && cow.getStatusEffect(StatusEffects.SLOWNESS).isInfinite(), "Reset cured an unrelated infinite Slowness effect");
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void externalTeleportDoesNotDragTheCaptiveBack(TestContext ctx) {
        var trap = trap(ctx);
        var cow = ctx.spawnEntity(EntityType.COW, new Vec3d(3.5, 3, 3.5));
        trap.trigger(cow);
        var destination = cow.getPos().add(10, 0, 0);
        cow.setPosition(destination);
        BearTrapBlockEntity.tick(ctx.getWorld(), trap.getPos(), trap.getCachedState(), trap);
        ctx.assertTrue(!trap.holds(cow) && cow.getPos().equals(destination), "Trap overrode an external teleport");
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void captureAndResetGraceSurviveNbtAndHaveClientUpdates(TestContext ctx) {
        var trap = trap(ctx);
        var cow = ctx.spawnEntity(EntityType.COW, new Vec3d(3.5, 3, 3.5));
        trap.trigger(cow);
        var copy = new BearTrapBlockEntity(trap.getPos(), trap.getCachedState());
        copy.readNbt(trap.createNbt());
        ctx.assertTrue(copy.holds(cow) && trap.toUpdatePacket() != null && trap.toInitialChunkDataNbt().containsUuid("Captured"), "Capture UUID was not persisted or synchronized");
        trap.reset();
        var openCopy = new BearTrapBlockEntity(trap.getPos(), trap.getCachedState());
        openCopy.readNbt(trap.createNbt());
        ctx.assertFalse(openCopy.armed(), "Reload removed the escape grace period");
        ctx.complete();
    }
}
