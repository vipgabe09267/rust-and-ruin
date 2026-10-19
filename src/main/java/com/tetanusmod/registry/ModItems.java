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
    public static final Item ANTITOXIN = register("tetanus_antitoxin", new AntitoxinItem(new FabricItemSettings().maxCount(16).recipeRemainder(net.minecraft.item.Items.GLASS_BOTTLE)));
    public static final Item VACCINE=register("tetanus_vaccine",new VaccineItem(new FabricItemSettings().maxCount(16)));
    public static final Item PROTECTIVE_GLOVES=register("protective_gloves",new com.tetanusmod.expansion.GlovesItem(new FabricItemSettings().maxDamage(192)));
    public static final Item FIELD_GUIDE=register("field_guide",new FieldGuideItem(new FabricItemSettings().maxCount(1)));
    public static final Item TETANIZED_ARROW = register("tetanized_arrow", new TetanizedArrowItem(new FabricItemSettings()));
    public static final Item RUST=register("rust",new net.minecraft.item.BlockItem(com.tetanusmod.expansion.ExpansionBlocks.RUST_PILE,new FabricItemSettings()));
    public static final Item RUSTY_FISHING_HOOK=register("rusty_fishing_hook",new com.tetanusmod.item.DescribedItem(new FabricItemSettings(),"hook"));
    public static final Item COW_MANURE=register("cow_manure",new com.tetanusmod.item.DescribedItem(new FabricItemSettings(),"manure"));
    public static final Item TETANIZED_WITHER_STAR=register("tetanized_wither_star",new com.tetanusmod.item.DescribedItem(new FabricItemSettings().rarity(net.minecraft.util.Rarity.EPIC),"star"));
    public static final Item RUSTY_HOOKLINE=register("rusty_hookline",new com.tetanusmod.item.HooklineItem(new FabricItemSettings().maxDamage(96)));
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
        com.tetanusmod.expansion.ExpansionItems.initialize();
        Registry.register(Registries.ITEM_GROUP, TetanusMod.id("items"), FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.tetanusmod"))
                .icon(() -> new ItemStack(RUSTED_NAIL))
                .entries((context, entries) -> {
                    ALL.stream().filter(item->!ModIronTools.isRusted(item)).forEach(entries::add);
                    entries.add(net.minecraft.item.EnchantedBookItem.forEnchantment(
                            new net.minecraft.enchantment.EnchantmentLevelEntry(TETANUS_ENCHANTMENT,1)));
                    entries.add(net.minecraft.item.EnchantedBookItem.forEnchantment(new net.minecraft.enchantment.EnchantmentLevelEntry(TETANUS_ENCHANTMENT,2)));
                    entries.add(net.minecraft.item.EnchantedBookItem.forEnchantment(new net.minecraft.enchantment.EnchantmentLevelEntry(com.tetanusmod.expansion.ExpansionItems.TETANUS_PROTECTION,1)));
                    for(var potion:java.util.List.of(ModBrewing.MANURE,ModBrewing.TETANUS,ModBrewing.SALMONELLA,ModBrewing.LOCKJAW,ModBrewing.RABIES))for(var bottle:java.util.List.of(net.minecraft.item.Items.POTION,net.minecraft.item.Items.SPLASH_POTION,net.minecraft.item.Items.LINGERING_POTION))entries.add(net.minecraft.potion.PotionUtil.setPotion(new ItemStack(bottle),potion));
                }).build());
    }
}
