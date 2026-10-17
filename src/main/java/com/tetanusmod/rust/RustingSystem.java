package com.tetanusmod.rust;

import com.tetanusmod.registry.ModItems;
import com.tetanusmod.registry.ModBlocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import java.util.Map;

public final class RustingSystem {
    private static final class Pairs {
        private static final Map<Item, Item> ITEMS = Map.of(ModItems.NAIL, ModItems.RUSTED_NAIL, ModItems.REBAR, ModItems.RUSTED_REBAR,
                ModItems.SHARPENED_REBAR, ModItems.RUSTED_SHARPENED_REBAR,
                ModItems.BENT_NAIL, ModItems.RUSTED_BENT_NAIL, ModItems.METAL_PIPE, ModItems.RUSTED_METAL_PIPE,
                ModBlocks.CALTROPS.asItem(), ModBlocks.RUSTED_CALTROPS.asItem());
    }

    public static Item rusted(Item item) {
        Item normal = Pairs.ITEMS.get(item);
        return normal != null ? normal : com.tetanusmod.registry.ModIronTools.PAIRS.get(item);
    }
    public static Item clean(Item item) {
        for (var entry : Pairs.ITEMS.entrySet()) if (entry.getValue() == item) return entry.getKey();
        for (var entry : com.tetanusmod.registry.ModIronTools.PAIRS.entrySet()) if (entry.getValue() == item) return entry.getKey();
        return null;
    }

    public static ItemStack convert(ItemStack original, Item result) {
        ItemStack converted = new ItemStack(result, original.getCount());
        if (original.hasNbt()) converted.setNbt(original.getNbt().copy());
        return converted;
    }

    public static void particles(World world, Vec3d pos) {
        if (world instanceof ServerWorld server) server.spawnParticles(
                new DustParticleEffect(new Vector3f(0.60F, 0.25F, 0.09F), 0.75F),
                pos.x, pos.y, pos.z, 8, 0.15, 0.12, 0.15, 0.01);
    }

    public static void rustFeedback(World world, Vec3d pos) {
        particles(world, pos);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.BLOCKS, 0.15F, 0.65F);
    }
}
