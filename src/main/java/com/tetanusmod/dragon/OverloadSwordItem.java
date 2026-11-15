package com.tetanusmod.dragon;

import com.tetanusmod.registry.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import java.util.List;

public final class OverloadSwordItem extends SwordItem {
    private static final ToolMaterial MATERIAL=new ToolMaterial(){
        public int getDurability(){return 4062;}
        public float getMiningSpeedMultiplier(){return 9;}
        public float getAttackDamage(){return 0;}
        public int getMiningLevel(){return 4;}
        public int getEnchantability(){return 15;}
        public Ingredient getRepairIngredient(){return Ingredient.ofItems(Items.NETHERITE_BLOCK);}
    };
    public OverloadSwordItem(){super(MATERIAL,11,-1.7F,new Item.Settings().fireproof().rarity(Rarity.EPIC));}
    @Override public ItemStack getDefaultStack(){var stack=new ItemStack(this);stack.addEnchantment(ModItems.TETANUS_ENCHANTMENT,2);return stack;}
    @Override public boolean canRepair(ItemStack stack,ItemStack ingredient){return ingredient.isOf(Items.NETHERITE_BLOCK);}
    @Override public void appendTooltip(ItemStack stack,World world,List<Text> lines,TooltipContext context){super.appendTooltip(stack,world,lines,context);lines.add(Text.translatable("tooltip.tetanusmod.overload_repair"));}
}
