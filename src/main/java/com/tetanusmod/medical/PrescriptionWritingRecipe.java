package com.tetanusmod.medical;

import com.tetanusmod.TetanusMod;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/** A separate recipe type prevents ordinary crafting or smithing from printing prescriptions. */
public final class PrescriptionWritingRecipe implements Recipe<Inventory> {
    public static final RecipeType<PrescriptionWritingRecipe> TYPE=Registry.register(Registries.RECIPE_TYPE,TetanusMod.id("prescription_writing"),new RecipeType<>(){});
    public static final RecipeSerializer<PrescriptionWritingRecipe> SERIALIZER=Registry.register(Registries.RECIPE_SERIALIZER,TetanusMod.id("prescription_writing"),new RecipeSerializer<>(){
        @Override public PrescriptionWritingRecipe read(Identifier id,com.google.gson.JsonObject json){return new PrescriptionWritingRecipe(id);}
        @Override public PrescriptionWritingRecipe read(Identifier id,PacketByteBuf buffer){return new PrescriptionWritingRecipe(id);}
        @Override public void write(PacketByteBuf buffer,PrescriptionWritingRecipe recipe){}
    });
    private final Identifier id;
    public PrescriptionWritingRecipe(Identifier id){this.id=id;}
    @Override public Identifier getId(){return id;}
    @Override public RecipeSerializer<?> getSerializer(){return SERIALIZER;}
    @Override public RecipeType<?> getType(){return TYPE;}
    @Override public boolean matches(Inventory input,World world){return input.size()==2&&input.getStack(0).isOf(Items.PAPER)&&input.getStack(1).isOf(Items.INK_SAC);}
    @Override public ItemStack craft(Inventory inventory,DynamicRegistryManager registry){return getOutput(registry).copy();}
    @Override public ItemStack getOutput(DynamicRegistryManager registry){return new ItemStack(MedicalWorkstations.PRESCRIPTION);}
    @Override public boolean fits(int width,int height){return width>0&&height>0&&(long)width*height>=2;}
    @Override public ItemStack createIcon(){return new ItemStack(MedicalWorkstations.WRITER_ITEM);}
    @Override public net.minecraft.util.collection.DefaultedList<net.minecraft.recipe.Ingredient> getIngredients(){return net.minecraft.util.collection.DefaultedList.copyOf(net.minecraft.recipe.Ingredient.EMPTY,net.minecraft.recipe.Ingredient.ofItems(Items.PAPER),net.minecraft.recipe.Ingredient.ofItems(Items.INK_SAC));}
    @Override public boolean isIgnoredInRecipeBook(){return true;}
    public static void initialize(){}
}
