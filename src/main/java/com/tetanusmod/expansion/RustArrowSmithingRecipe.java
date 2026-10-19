package com.tetanusmod.expansion;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.registry.ModItems;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.recipe.*;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public final class RustArrowSmithingRecipe implements SmithingRecipe {
    public static final RecipeSerializer<RustArrowSmithingRecipe> SERIALIZER=Registry.register(Registries.RECIPE_SERIALIZER,TetanusMod.id("rust_arrows"),new RecipeSerializer<>(){
        @Override public RustArrowSmithingRecipe read(Identifier id,com.google.gson.JsonObject json){return new RustArrowSmithingRecipe(id);}
        @Override public RustArrowSmithingRecipe read(Identifier id,net.minecraft.network.PacketByteBuf buffer){return new RustArrowSmithingRecipe(id);}
        @Override public void write(net.minecraft.network.PacketByteBuf buffer,RustArrowSmithingRecipe recipe){}
    });
    private final Identifier id;
    public RustArrowSmithingRecipe(Identifier id){this.id=id;}
    @Override public Identifier getId(){return id;}
    @Override public RecipeSerializer<?> getSerializer(){return SERIALIZER;}
    @Override public boolean testTemplate(ItemStack stack){return stack.isEmpty();}
    @Override public boolean testBase(ItemStack stack){return stack.isOf(Items.ARROW);}
    @Override public boolean testAddition(ItemStack stack){return stack.isOf(ModItems.RUST);}
    @Override public boolean matches(Inventory inventory,World world){return inventory.size()>=3&&testTemplate(inventory.getStack(0))&&testBase(inventory.getStack(1))&&testAddition(inventory.getStack(2));}
    @Override public ItemStack craft(Inventory inventory,net.minecraft.registry.DynamicRegistryManager registry){return new ItemStack(ModItems.TETANIZED_ARROW,Math.min(64,inventory.getStack(1).getCount()));}
    @Override public ItemStack getOutput(net.minecraft.registry.DynamicRegistryManager registry){return new ItemStack(ModItems.TETANIZED_ARROW,64);}
    public static void initialize(){}
}