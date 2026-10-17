package com.tetanusmod.registry;
import net.minecraft.item.*;
import java.util.Map;
public final class ModIronTools {
    public static final Item SWORD = ModItems.register("rusted_iron_sword", new SwordItem(ToolMaterials.IRON,3,-2.4F,new Item.Settings()));
    public static final Item PICKAXE = ModItems.register("rusted_iron_pickaxe", new PickaxeItem(ToolMaterials.IRON,1,-2.8F,new Item.Settings()){});
    public static final Item AXE = ModItems.register("rusted_iron_axe", new AxeItem(ToolMaterials.IRON,6,-3.1F,new Item.Settings()){});
    public static final Item SHOVEL = ModItems.register("rusted_iron_shovel", new ShovelItem(ToolMaterials.IRON,1.5F,-3,new Item.Settings()));
    public static final Item HOE = ModItems.register("rusted_iron_hoe", new HoeItem(ToolMaterials.IRON,-2,-1,new Item.Settings()){});
    public static final Map<Item,Item> PAIRS = Map.of(Items.IRON_SWORD,SWORD,Items.IRON_PICKAXE,PICKAXE,Items.IRON_AXE,AXE,Items.IRON_SHOVEL,SHOVEL,Items.IRON_HOE,HOE);
    public static boolean isRusted(Item item) { return PAIRS.containsValue(item); }
    public static void initialize() { }
}
