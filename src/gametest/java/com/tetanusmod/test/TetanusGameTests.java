package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.block.CaltropsBlock;
import com.tetanusmod.block.CaltropsBlockEntity;
import com.tetanusmod.effect.Infection;
import com.tetanusmod.effect.InfectionState;
import com.tetanusmod.entity.NailEntity;
import com.tetanusmod.registry.ModBlocks;
import com.tetanusmod.registry.ModItems;
import com.tetanusmod.rust.RustingSystem;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class TetanusGameTests implements FabricGameTest {
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void craftingRecipesAndContainers(TestContext ctx) {
        var player = ctx.createMockSurvivalPlayer();
        var grid = new CraftingInventory(new CraftingScreenHandler(1, player.getInventory()), 3, 3);
        var manager = ctx.getWorld().getRecipeManager();
        String[] names = {"nail", "rebar", "metal_pipe", "caltrops", "wire_brush", "tetanus_antitoxin", "sharpened_rebar", "bent_nail"};
        for (String name : names) ctx.assertTrue(manager.get(TetanusMod.id(name)).isPresent(), "Missing recipe " + name);
        grid.setStack(0, new ItemStack(Items.IRON_INGOT));
        grid.setStack(3, new ItemStack(Items.IRON_INGOT));
        grid.setStack(1, new ItemStack(Items.FLINT));
        var recipe = manager.getFirstMatch(RecipeType.CRAFTING, grid, ctx.getWorld()).orElseThrow();
        var result = recipe.craft(grid, ctx.getWorld().getRegistryManager());
        ctx.assertTrue(result.isOf(ModItems.NAIL) && result.getCount() == 4, "Two vertical iron must yield four nails");
        grid.clear();
        grid.setStack(0, new ItemStack(Items.HONEY_BOTTLE));
        grid.setStack(1, new ItemStack(Items.GOLDEN_CARROT));
        grid.setStack(2, new ItemStack(Items.FERMENTED_SPIDER_EYE));
        grid.setStack(4, new ItemStack(Items.GLASS_BOTTLE));
        recipe = manager.getFirstMatch(RecipeType.CRAFTING, grid, ctx.getWorld()).orElseThrow();
        ctx.assertTrue(recipe.craft(grid, ctx.getWorld().getRegistryManager()).isOf(ModItems.ANTITOXIN), "Antitoxin recipe mismatch");
        ctx.assertTrue(recipe.getRemainder(grid).get(0).isOf(Items.GLASS_BOTTLE), "Honey bottle container not returned");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE, batchId = "weather", tickLimit = 400)
    public void rainRustedAndRoofProtected(TestContext ctx) {
        ctx.getWorld().setWeather(0, 1000, true, false);
        Vec3d open = ctx.getAbsolute(new Vec3d(2.5, 5, 2.5));
        Vec3d covered = ctx.getAbsolute(new Vec3d(5.5, 5, 5.5));
        ItemEntity exposed = new ItemEntity(ctx.getWorld(), open.x, open.y, open.z, new ItemStack(ModItems.METAL_PIPE));
        ItemEntity roofed = new ItemEntity(ctx.getWorld(), covered.x, covered.y, covered.z, new ItemStack(ModItems.NAIL));
        for (var item : new ItemEntity[]{exposed, roofed}) { item.setNoGravity(true); item.setVelocity(Vec3d.ZERO); ctx.getWorld().spawnEntity(item); }
        ctx.setBlockState(5, 6, 5, Blocks.STONE);
        ctx.runAtTick(290, () -> ctx.assertTrue(exposed.getStack().isOf(ModItems.METAL_PIPE), "Rain rusted item before 300 ticks"));
        ctx.runAtTick(360, () -> {
            ctx.assertTrue(exposed.getStack().isOf(ModItems.RUSTED_METAL_PIPE), "Exposed item did not rust in rain");
            ctx.assertTrue(roofed.getStack().isOf(ModItems.NAIL), "Roofed item rusted from rain");
            ctx.getWorld().setWeather(1000, 0, false, false);
            ctx.complete();
        });
    }

    private CowEntity cow(TestContext ctx, int x, int z) {
        CowEntity cow = ctx.spawnEntity(EntityType.COW, new Vec3d(x + 0.5, 3, z + 0.5));
        cow.setAiDisabled(true);
        cow.setNoGravity(true);
        return cow;
    }

    private ItemEntity wetItem(TestContext ctx, ItemStack stack, BlockPos pos) {
        ctx.setBlockState(pos.down(), Blocks.STONE);
        for (int y = 0; y <= 4; y++) {
            for (var direction : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
                ctx.setBlockState(pos.up(y).offset(direction), Blocks.GLASS);
            }
        }
        ctx.setBlockState(pos.up(5), Blocks.GLASS);
        ctx.setBlockState(pos, Blocks.WATER);
        for (int y = 1; y <= 4; y++) ctx.setBlockState(pos.up(y), Blocks.WATER);
        Vec3d absolute = Vec3d.ofCenter(ctx.getAbsolutePos(pos));
        ItemEntity item = new ItemEntity(ctx.getWorld(), absolute.x, absolute.y, absolute.z, stack);
        item.setNoGravity(true);
        item.setVelocity(Vec3d.ZERO);
        item.setPickupDelay(32767);
        ctx.getWorld().spawnEntity(item);
        return item;
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 140)
    public void submergedStackAndDurability(TestContext ctx) {
        ItemStack weapon = new ItemStack(ModItems.REBAR);
        weapon.setDamage(71);
        weapon.setCustomName(Text.literal("Factory bar"));
        weapon.addEnchantment(Enchantments.UNBREAKING, 2);
        ItemEntity rebar = wetItem(ctx, weapon, new BlockPos(2, 3, 2));
        ItemEntity nails = wetItem(ctx, new ItemStack(ModItems.NAIL, 17), new BlockPos(5, 3, 5));
        ctx.runAtTick(95, () -> {
            ctx.assertTrue(rebar.getStack().isOf(ModItems.REBAR), "Rebar rusted before 100 ticks");
            ctx.assertTrue(nails.getStack().isOf(ModItems.NAIL), "Nails rusted before 100 ticks");
        });
        ctx.runAtTick(115, () -> {
            ctx.assertTrue(rebar.getStack().isOf(ModItems.RUSTED_REBAR), "Submerged rebar did not rust");
            ctx.assertTrue(rebar.getStack().getDamage() == 71, "Rusting reset durability");
            ctx.assertTrue(rebar.getStack().getName().getString().equals("Factory bar"), "Rusting lost name");
            ctx.assertTrue(rebar.getStack().hasEnchantments(), "Rusting lost enchantments");
            ctx.assertTrue(nails.getStack().isOf(ModItems.RUSTED_NAIL) && nails.getStack().getCount() == 17, "Rusting lost stack count");
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 220)
    public void interruptedWaterExposureResets(TestContext ctx) {
        ItemEntity nail = wetItem(ctx, new ItemStack(ModItems.BENT_NAIL), new BlockPos(3, 3, 3));
        Vec3d wet = nail.getPos();
        ctx.runAtTick(60, () -> nail.setPosition(ctx.getAbsolute(new Vec3d(6.5, 6, 6.5))));
        ctx.runAtTick(65, () -> { nail.setPosition(wet); nail.setVelocity(Vec3d.ZERO); });
        ctx.runAtTick(140, () -> ctx.assertTrue(nail.getStack().isOf(ModItems.BENT_NAIL), "Water timer failed to reset"));
        ctx.runAtTick(185, () -> {
            ctx.assertTrue(nail.getStack().isOf(ModItems.RUSTED_BENT_NAIL), "Second continuous exposure failed");
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void randomDurationProgressionAndRefresh(TestContext ctx) {
        CowEntity target = cow(ctx, 3, 3);
        InfectionState state = (InfectionState) target;
        int min = Integer.MAX_VALUE, max = 0;
        for (int i = 0; i < 256; i++) {
            Infection.apply(target, null);
            int duration = target.getStatusEffect(TetanusMod.TETANUS).getDuration();
            ctx.assertTrue(duration >= 6000 && duration <= 12000 && duration % 20 == 0, "Duration outside 300-600 seconds");
            min = Math.min(min, duration); max = Math.max(max, duration);
        }
        ctx.assertTrue(max > min, "Duration did not randomize");
        int total = state.tetanus$getTotal();
        ctx.assertTrue(Infection.stage(0, total) == 0 && Infection.stage(total / 2, total) == 1
                && Infection.stage(total - 1, total) == 2, "Progressive thirds incorrect");
        state.tetanus$setInfection(total - 1, total);
        TetanusMod.TETANUS.applyUpdateEffect(target, 0);
        var speed = target.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        double severeSpeed = speed.getValue();
        Infection.apply(target, null);
        ctx.assertTrue(state.tetanus$getElapsed() == 0 && state.tetanus$getLockjaw() == 0 && state.tetanus$getFreeze() == 0,
                "Reinfection did not reset symptoms");
        ctx.assertTrue(speed.getValue() > severeSpeed, "Reinfection did not restore mild stiffness");
        NbtCompound saved = new NbtCompound();
        state.tetanus$setInfection(500, state.tetanus$getTotal());
        target.writeNbt(saved);
        CowEntity loaded = new CowEntity(EntityType.COW, ctx.getWorld());
        loaded.readNbt(saved);
        ctx.assertTrue(((InfectionState) loaded).tetanus$getElapsed() == 500, "Progress lost across save/load");
        target.removeStatusEffect(TetanusMod.TETANUS);
        ctx.assertTrue(speed.getValue() > severeSpeed, "Cure left a speed modifier behind");
        ctx.complete();
    }

    private void projectileHit(TestContext ctx, Item item, float damage, boolean rusty) {
        CowEntity target = cow(ctx, 3, 5);
        float health = target.getHealth();
        NailEntity nail = new NailEntity(TetanusMod.NAIL_ENTITY, ctx.getWorld());
        nail.setItem(new ItemStack(item));
        nail.setPosition(ctx.getAbsolute(new Vec3d(3.5, 3.7, 2.5)));
        nail.setVelocity(0, 0, 0.7);
        nail.setRecoverable(true);
        ctx.getWorld().spawnEntity(nail);
        ctx.runAtTick(12, () -> {
            ctx.assertTrue(target.getHealth() == (com.tetanusmod.entity.MobVariants.permanent(target) ? target.getMaxHealth() : health - damage), "Projectile damage incorrect: " + target.getHealth());
            ctx.assertTrue(target.hasStatusEffect(TetanusMod.TETANUS) == rusty, "Projectile infection incorrect");
            ctx.assertFalse(nail.isAlive(), "Projectile survived entity impact");
            ctx.assertTrue(ctx.getWorld().getEntitiesByClass(ItemEntity.class, target.getBoundingBox().expand(3),
                    e -> e.getStack().isOf(item)).isEmpty(), "Entity impact duplicated nail");
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void normalNailDamage(TestContext ctx) { projectileHit(ctx, ModItems.NAIL, 2, false); }
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void rustyNailDamageAndInfection(TestContext ctx) { projectileHit(ctx, ModItems.RUSTED_NAIL, 2, true); }
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bentNailDamage(TestContext ctx) { projectileHit(ctx, ModItems.BENT_NAIL, 1, false); }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void blockImpactDropsExactlyOneNail(TestContext ctx) {
        ctx.setBlockState(3, 3, 5, Blocks.STONE);
        NailEntity nail = new NailEntity(TetanusMod.NAIL_ENTITY, ctx.getWorld());
        nail.setItem(new ItemStack(ModItems.NAIL, 12));
        nail.setPosition(ctx.getAbsolute(new Vec3d(3.5, 3.7, 2.5)));
        nail.setVelocity(0, 0, 0.7);
        nail.setRecoverable(true);
        ctx.getWorld().spawnEntity(nail);
        ctx.runAtTick(12, () -> {
            var items = ctx.getWorld().getEntitiesByClass(ItemEntity.class,
                    new Box(ctx.getAbsolutePos(new BlockPos(1, 1, 1)), ctx.getAbsolutePos(new BlockPos(7, 7, 7))),
                    e -> e.getStack().isOf(ModItems.NAIL));
            ctx.assertTrue(items.size() == 1 && items.get(0).getStack().getCount() == 1, "Block hit duplicated or lost nail");
            ctx.assertFalse(nail.isAlive(), "Projectile not discarded after block collision");
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void throwingConsumesOnlyInSurvival(TestContext ctx) {
        var player = ctx.createMockSurvivalPlayer();
        player.setPosition(ctx.getAbsolute(new Vec3d(3, 3, 3)));
        player.setStackInHand(Hand.MAIN_HAND, new ItemStack(ModItems.NAIL, 4));
        ModItems.NAIL.use(ctx.getWorld(), player, Hand.MAIN_HAND);
        ctx.assertTrue(player.getMainHandStack().getCount() == 3, "Survival throw did not consume one nail");
        player.getAbilities().creativeMode = true;
        ModItems.NAIL.use(ctx.getWorld(), player, Hand.MAIN_HAND);
        ctx.assertTrue(player.getMainHandStack().getCount() == 3, "Creative throw consumed nail");
        ctx.getWorld().getEntitiesByType(TetanusMod.NAIL_ENTITY, player.getBoundingBox().expand(4), e -> true).forEach(NailEntity::discard);
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void meleeTotalDamageAndRust(TestContext ctx) {
        Item[] weapons = {ModItems.REBAR, ModItems.SHARPENED_REBAR, ModItems.METAL_PIPE, ModItems.RUSTED_REBAR};
        float[] damage = {1, 3, 2, 1};
        for (int i = 0; i < weapons.length; i++) {
            var player = ctx.createMockSurvivalPlayer();
            player.setPosition(ctx.getAbsolute(new Vec3d(3, 3, 2)));
            player.setStackInHand(Hand.MAIN_HAND, new ItemStack(weapons[i]));
            player.setOnGround(true);
            for (int t = 0; t < 25; t++) player.tick();
            CowEntity target = cow(ctx, 3, 4);
            player.attack(target);
            ctx.assertTrue(target.getHealth() == 10 - damage[i], "Wrong total melee damage for " + weapons[i] + ": " + target.getHealth());
            ctx.assertTrue(target.hasStatusEffect(TetanusMod.TETANUS) == (i == 3), "Melee infection mismatch");
            ctx.assertTrue(player.getMainHandStack().getDamage() == 1, "Melee durability not consumed");
            target.discard();
        }
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 140)
    public void placedCaltropsRustAndCooldown(TestContext ctx) {
        BlockPos pos = new BlockPos(3, 3, 3);
        ctx.setBlockState(pos, ModBlocks.CALTROPS.getDefaultState().with(CaltropsBlock.WATERLOGGED, true));
        ctx.runAtTick(115, () -> {
            ctx.expectBlock(ModBlocks.RUSTED_CALTROPS, pos);
            CaltropsBlockEntity trap = (CaltropsBlockEntity) ctx.getBlockEntity(pos);
            CowEntity target = cow(ctx, 5, 5);
            trap.stepOn(target, true);
            ctx.assertTrue(target.getHealth() == 9 && target.hasStatusEffect(TetanusMod.TETANUS), "Rusty trap did not damage/infect");
            target.timeUntilRegen = 0;
            trap.stepOn(target, true);
            ctx.assertTrue(target.getHealth() == 9, "Trap cooldown bypassed");
            ctx.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void brushAndAntitoxinPreserveOtherData(TestContext ctx) {
        var player = ctx.createMockSurvivalPlayer();
        ItemStack rebar = new ItemStack(ModItems.RUSTED_REBAR);
        rebar.setDamage(82);
        rebar.addEnchantment(Enchantments.UNBREAKING, 2);
        player.setStackInHand(Hand.MAIN_HAND, new ItemStack(ModItems.WIRE_BRUSH));
        player.setStackInHand(Hand.OFF_HAND, rebar);
        ModItems.WIRE_BRUSH.use(ctx.getWorld(), player, Hand.MAIN_HAND);
        ctx.assertTrue(player.getOffHandStack().isOf(ModItems.REBAR) && player.getOffHandStack().getDamage() == 82
                && player.getOffHandStack().hasEnchantments(), "Brush lost weapon data");
        ctx.assertTrue(player.getMainHandStack().getDamage() == 1, "Brush durability incorrect");
        Infection.apply(player, null);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 600));
        ItemStack empty = ModItems.ANTITOXIN.finishUsing(new ItemStack(ModItems.ANTITOXIN), ctx.getWorld(), player);
        ctx.assertFalse(player.hasStatusEffect(TetanusMod.TETANUS), "Antitoxin failed");
        ctx.assertTrue(player.hasStatusEffect(StatusEffects.NIGHT_VISION), "Antitoxin removed unrelated effect");
        ctx.assertTrue(empty.isOf(Items.GLASS_BOTTLE), "Antitoxin did not return bottle");
        Infection.apply(player, null);
        Items.MILK_BUCKET.finishUsing(new ItemStack(Items.MILK_BUCKET), ctx.getWorld(), player);
        ctx.assertTrue(player.hasStatusEffect(TetanusMod.TETANUS), "Milk cured Tetanus");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void grindstoneConversionAndNoDuplication(TestContext ctx) {
        var player = ctx.createMockSurvivalPlayer();
        var handler = new GrindstoneScreenHandler(1, player.getInventory());
        ItemStack rusty = new ItemStack(ModItems.RUSTED_REBAR);
        rusty.setDamage(90);
        rusty.addEnchantment(Enchantments.UNBREAKING, 2);
        handler.getSlot(0).setStack(rusty);
        ItemStack result = handler.getSlot(2).getStack();
        ctx.assertTrue(result.isOf(ModItems.REBAR) && result.getDamage() == 90 && !result.hasEnchantments(), "Grindstone conversion incorrect");
        handler.getSlot(2).onTakeItem(player, result);
        ctx.assertTrue(handler.getSlot(0).getStack().isEmpty() && handler.getSlot(2).getStack().isEmpty(), "Grindstone duplicated input/output");
        ctx.assertTrue(handler.getSlot(0).canInsert(new ItemStack(ModItems.RUSTED_NAIL)), "Grindstone refuses rusty nails");
        handler.getSlot(0).setStack(new ItemStack(ModItems.RUSTED_NAIL, 32));
        ctx.assertTrue(handler.getSlot(2).getStack().isOf(ModItems.NAIL) && handler.getSlot(2).getStack().getCount() == 32, "Grindstone lost nail stack");
        handler.getSlot(0).setStack(new ItemStack(Items.IRON_SWORD));
        handler.getSlot(0).getStack().addEnchantment(Enchantments.UNBREAKING, 1);
        handler.getSlot(0).markDirty();
        ctx.assertTrue(handler.getSlot(2).getStack().isOf(Items.IRON_SWORD), "Vanilla grindstone broken");
        ctx.complete();
    }
}
