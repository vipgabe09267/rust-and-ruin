package com.tetanusmod.dragon;

import com.google.gson.JsonObject;
import com.tetanusmod.registry.ModItems;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/** One fixed, disclosed forging process; arbitrary inputs are never accepted. */
public final class CrystalForgingRecipe implements Recipe<Inventory> {
    private final Identifier id;
    public CrystalForgingRecipe(Identifier id){this.id=id;}
    public boolean matches(Inventory inventory,World world){return inventory.size()>=3&&inventory.getStack(0).isOf(Items.END_CRYSTAL)&&inventory.getStack(1).isOf(ModItems.RUST)&&inventory.getStack(2).isOf(Items.NETHERITE_INGOT);}
    public ItemStack craft(Inventory inventory,DynamicRegistryManager registry){return new ItemStack(DragonContent.CRYSTAL);}
    public boolean fits(int width,int height){return width>0&&height>0&&(long)width*height>=3;}
    public ItemStack getOutput(DynamicRegistryManager registry){return new ItemStack(DragonContent.CRYSTAL);}
    public Identifier getId(){return id;}
    public RecipeSerializer<?> getSerializer(){return DragonContent.RECIPE_SERIALIZER;}
    public RecipeType<?> getType(){return DragonContent.RECIPE_TYPE;}
    public boolean isIgnoredInRecipeBook(){return true;}
    @Override public net.minecraft.util.collection.DefaultedList<Ingredient> getIngredients(){return net.minecraft.util.collection.DefaultedList.copyOf(Ingredient.EMPTY,Ingredient.ofItems(Items.END_CRYSTAL),Ingredient.ofItems(ModItems.RUST),Ingredient.ofItems(Items.NETHERITE_INGOT));}
    public static final class Serializer implements RecipeSerializer<CrystalForgingRecipe>{
        public CrystalForgingRecipe read(Identifier id,JsonObject json){return new CrystalForgingRecipe(id);}
        public CrystalForgingRecipe read(Identifier id,PacketByteBuf packet){return new CrystalForgingRecipe(id);}
        public void write(PacketByteBuf packet,CrystalForgingRecipe recipe){}
    }
}
