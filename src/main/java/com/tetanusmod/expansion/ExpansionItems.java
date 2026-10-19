package com.tetanusmod.expansion;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.registry.ModItems;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;

public final class ExpansionItems {
    public static final Item SPEAR=ModItems.register("iron_spear",new SpearItem(false,new Item.Settings().maxDamage(320)));
    public static final Item RUSTED_SPEAR=ModItems.register("rusted_iron_spear",new SpearItem(true,new Item.Settings().maxDamage(320)));
    public static final Item TRAP_RESET_TOOL=ModItems.register("trap_reset_tool",new TrapResetToolItem(new Item.Settings().maxDamage(128)));
    public static final Item RABIES_CURE=ModItems.register("rabies_cure",new RabiesTonicItem(false,new Item.Settings().maxCount(16)));
    public static final Item RABIES_VACCINE=ModItems.register("rabies_vaccine",new RabiesTonicItem(true,new Item.Settings().maxCount(16)));
    public static final Item RABID_MEAT=ModItems.register("rabid_meat",new Item(new Item.Settings().food(new FoodComponent.Builder().hunger(3).saturationModifier(0.2F).meat().build())){
        @Override public ItemStack finishUsing(ItemStack stack,World world,LivingEntity user){var result=super.finishUsing(stack,world,user);if(!world.isClient)Rabies.expose(user,null);return result;}
    });
    public static final Item COOKED_RABID_MEAT=ModItems.register("cooked_rabid_meat",new Item(new Item.Settings().food(new FoodComponent.Builder().hunger(6).saturationModifier(0.6F).meat().build())));
    public static final net.minecraft.enchantment.Enchantment TETANUS_PROTECTION=Registry.register(Registries.ENCHANTMENT,TetanusMod.id("tetanus_protection"),new TetanusProtectionEnchantment());
    public static ItemStack tetanusBook(int level){
        var book=EnchantedBookItem.forEnchantment(new net.minecraft.enchantment.EnchantmentLevelEntry(ModItems.TETANUS_ENCHANTMENT,level));
        book.setCustomName(net.minecraft.text.Text.translatable(level>=2?"item.tetanusmod.tetanus_book_ii":"item.tetanusmod.tetanus_book"));return book;
    }
    public static void initialize(){ExpansionBlocks.initialize();VariantSpawnEggItem.registerAll();}
}