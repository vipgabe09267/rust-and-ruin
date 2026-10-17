package com.tetanusmod.registry;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.item.*;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    public static final List<Item> ALL = new ArrayList<>();
    public static final Item NAIL = register("nail", new NailItem(false, false, new FabricItemSettings()));
    public static final Item RUSTED_NAIL = register("rusted_nail", new NailItem(true, false, new FabricItemSettings()));
    public static final Item REBAR = register("rebar", new RebarItem(false, 1, false, new FabricItemSettings().maxDamage(250)));
    public static final Item RUSTED_REBAR = register("rusted_rebar", new RebarItem(true, 1, false, new FabricItemSettings().maxDamage(250)));
    public static final Item SHARPENED_REBAR = register("sharpened_rebar", new RebarItem(false, 3, false, new FabricItemSettings().maxDamage(200)));
    public static final Item RUSTED_SHARPENED_REBAR = register("rusted_sharpened_rebar", new RebarItem(true, 3, false, new FabricItemSettings().maxDamage(200)));
    public static final Item BENT_NAIL = register("bent_nail", new NailItem(false, true, new FabricItemSettings()));
    public static final Item RUSTED_BENT_NAIL = register("rusted_bent_nail", new NailItem(true, true, new FabricItemSettings()));
    public static final Item METAL_PIPE = register("metal_pipe", new RebarItem(false, 2, true, new FabricItemSettings().maxDamage(160)));
    public static final Item RUSTED_METAL_PIPE = register("rusted_metal_pipe", new RebarItem(true, 2, true, new FabricItemSettings().maxDamage(160)));
    public static final Item WIRE_BRUSH = register("wire_brush", new WireBrushItem(new FabricItemSettings().maxDamage(128)));
    public static final Item ANTITOXIN = register("tetanus_antitoxin", new AntitoxinItem(new FabricItemSettings().maxCount(16)));
    public static final Item TETANIZED_ARROW = register("tetanized_arrow", new TetanizedArrowItem(new FabricItemSettings()));
    public static final Item RUST=register("rust",new Item(new FabricItemSettings()));
    public static final Item RUSTY_FISHING_HOOK=register("rusty_fishing_hook",new Item(new FabricItemSettings()));
    public static final Item INFECTED_MEAT = register("infected_meat", new InfectedMeatItem(new FabricItemSettings().food(
            new net.minecraft.item.FoodComponent.Builder().hunger(3).saturationModifier(0.3F).meat().build())));
    public static final Item COOKED_INFECTED_MEAT = register("cooked_infected_meat", new Item(new FabricItemSettings().food(
            new net.minecraft.item.FoodComponent.Builder().hunger(7).saturationModifier(0.8F).meat().build())));
    public static final net.minecraft.enchantment.Enchantment TETANUS_ENCHANTMENT = Registry.register(
            Registries.ENCHANTMENT, TetanusMod.id("tetanus"), new TetanusEnchantment());

    public static <T extends Item> T register(String name, T item) {
        Registry.register(Registries.ITEM, TetanusMod.id(name), item);
        ALL.add(item);
        return item;
    }

    public static void initialize() {
        ModIronTools.initialize();
        ModBlocks.initialize();
        Registry.register(Registries.ITEM_GROUP, TetanusMod.id("items"), FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.tetanusmod"))
                .icon(() -> new ItemStack(RUSTED_NAIL))
                .entries((context, entries) -> {
                    ALL.forEach(entries::add);
                    entries.add(net.minecraft.item.EnchantedBookItem.forEnchantment(
                            new net.minecraft.enchantment.EnchantmentLevelEntry(TETANUS_ENCHANTMENT,1)));
                }).build());
    }
}
