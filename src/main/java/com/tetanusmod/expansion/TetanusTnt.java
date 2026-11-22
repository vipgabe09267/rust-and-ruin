package com.tetanusmod.expansion;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.entity.TetanusTntEntity;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.TntBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

public final class TetanusTnt {
    public static final Block BLOCK = Registry.register(Registries.BLOCK, TetanusMod.id("tetanus_tnt"),
            new TntBlock(AbstractBlock.Settings.copy(Blocks.TNT)));
    public static final Item ITEM = ModItems.register("tetanus_tnt", new BlockItem(BLOCK, new Item.Settings()));
    public static final EntityType<TetanusTntEntity> ENTITY = Registry.register(Registries.ENTITY_TYPE,
            TetanusMod.id("tetanus_tnt"),
            FabricEntityTypeBuilder.<TetanusTntEntity>create(SpawnGroup.MISC, TetanusTntEntity::new)
                    .dimensions(EntityDimensions.fixed(0.98F, 0.98F))
                    .trackRangeBlocks(160).trackedUpdateRate(10).build());

    private TetanusTnt() {}

    public static void initialize() {
        FlammableBlockRegistry.getDefaultInstance().add(BLOCK, 15, 100);
        DispenserBlock.registerBehavior(ITEM, new ItemDispenserBehavior() {
            @Override
            protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
                BlockPos pos = pointer.getPos().offset(pointer.getBlockState().get(DispenserBlock.FACING));
                if (prime(pointer.getWorld(), pos, null, false)) stack.decrement(1);
                return stack;
            }
        });
    }

    public static boolean prime(ServerWorld world, BlockPos pos, @Nullable LivingEntity igniter, boolean chained) {
        var tnt = new TetanusTntEntity(ENTITY, world);
        tnt.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        float direction = world.random.nextFloat() * MathHelper.TAU;
        tnt.setVelocity(-MathHelper.sin(direction) * 0.02, 0.2, -MathHelper.cos(direction) * 0.02);
        tnt.setIgniter(igniter);
        if (chained) {
            int fuse = tnt.getFuse();
            tnt.setFuse(fuse / 8 + world.random.nextInt(fuse / 4));
        }
        if (!world.spawnEntity(tnt)) return false;
        if (!chained) {
            world.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.ENTITY_TNT_PRIMED,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.emitGameEvent(igniter, GameEvent.PRIME_FUSE, pos);
        }
        return true;
    }
}
