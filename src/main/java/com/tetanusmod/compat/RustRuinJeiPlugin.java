package com.tetanusmod.compat;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.registry.*;
import com.tetanusmod.rust.RustingSystem;
import mezz.jei.api.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.*;

@JeiPlugin
public final class RustRuinJeiPlugin implements IModPlugin {
    public static volatile boolean runtimeReady;
    public static long rustingRecipeCount;
    private static mezz.jei.api.runtime.IJeiRuntime activeRuntime;
    public static void showRustingRecipes() { if(activeRuntime!=null) activeRuntime.getRecipesGui().showTypes(List.of(RUSTING)); }
    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        activeRuntime=runtime;
        rustingRecipeCount=runtime.getRecipeManager().createRecipeLookup(RUSTING).includeHidden().get().count();
        runtimeReady=true;
        org.slf4j.LoggerFactory.getLogger("tetanusmod").info("Rust & Ruin JEI runtime initialized, including rusting recipes");
    }
    @Override public void onRuntimeUnavailable() { activeRuntime=null;runtimeReady=false; }
    public record RustRecipe(Item input,Item output) { }
    public static final RecipeType<RustRecipe> RUSTING = RecipeType.create("tetanusmod","rusting",RustRecipe.class);
    @Override public Identifier getPluginUid() { return TetanusMod.id("jei"); }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new RustCategory(registration.getJeiHelpers().getGuiHelper()));
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        List<RustRecipe> recipes = new ArrayList<>();
        for (Item item : net.minecraft.registry.Registries.ITEM) {
            Item rusty = RustingSystem.rusted(item);
            if (rusty != null) recipes.add(new RustRecipe(item,rusty));
        }
        registration.addRecipes(RUSTING,recipes);
        registration.addIngredientInfo(new ItemStack(ModItems.WIRE_BRUSH),VanillaTypes.ITEM_STACK,Text.translatable("jei.tetanusmod.brush"));
        registration.addIngredientInfo(new ItemStack(ModItems.INFECTED_MEAT),VanillaTypes.ITEM_STACK,Text.translatable("jei.tetanusmod.meat"));
    }
    private static final class RustCategory implements IRecipeCategory<RustRecipe> {
        private final IDrawable background,icon;
        private RustCategory(IGuiHelper helper) {
            background=helper.createBlankDrawable(150,52);
            icon=helper.createDrawableItemStack(new ItemStack(ModItems.RUSTED_NAIL));
        }
        @Override public RecipeType<RustRecipe> getRecipeType() { return RUSTING; }
        @Override public Text getTitle() { return Text.translatable("jei.tetanusmod.rusting"); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder,RustRecipe recipe,IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT,4,5).addItemStack(new ItemStack(recipe.input()));
            builder.addSlot(RecipeIngredientRole.OUTPUT,125,5).addItemStack(new ItemStack(recipe.output()));
        }
        @Override public void draw(RustRecipe recipe,IRecipeSlotsView slots,DrawContext context,double mouseX,double mouseY) {
            var font=MinecraftClient.getInstance().textRenderer;
            context.drawText(font,Text.translatable("jei.tetanusmod.water",TetanusMod.CONFIG.waterRustTicks/20.0),26,8,0xFF6E3924,false);
            context.drawText(font,Text.translatable("jei.tetanusmod.drop"),4,29,0xFF444444,false);
            if (TetanusMod.CONFIG.rainRusting) context.drawText(font,Text.translatable("jei.tetanusmod.rain",TetanusMod.CONFIG.rainRustTicks/20.0),4,41,0xFF444444,false);
        }
    }
}
