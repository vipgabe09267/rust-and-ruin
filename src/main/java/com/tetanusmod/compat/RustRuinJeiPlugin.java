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
    public static boolean hasDiseaseBrewing() {
        var recipes=activeRuntime.getRecipeManager().createRecipeLookup(mezz.jei.api.constants.RecipeTypes.BREWING).includeHidden().get().toList();
        for(var potion:List.of(ModBrewing.MANURE,ModBrewing.TETANUS,ModBrewing.SALMONELLA,ModBrewing.LOCKJAW,ModBrewing.RABIES)) {
            if(recipes.stream().noneMatch(recipe->net.minecraft.potion.PotionUtil.getPotion(recipe.getPotionOutput())==potion && recipe.getPotionOutput().isOf(Items.POTION)))return false;
            if(potion!=ModBrewing.MANURE && recipes.stream().noneMatch(recipe->net.minecraft.potion.PotionUtil.getPotion(recipe.getPotionOutput())==potion && recipe.getPotionOutput().isOf(Items.SPLASH_POTION)))return false;
        }
        return true;
    }
    public static boolean hasCaltropRecipe() { return activeRuntime.getRecipeManager().createRecipeLookup(mezz.jei.api.constants.RecipeTypes.CRAFTING).includeHidden().get().anyMatch(recipe->recipe.getId().equals(TetanusMod.id("caltrops"))); }
    public static void showCaltropRecipe() { activeRuntime.getRecipesGui().show(List.of(activeRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT,VanillaTypes.ITEM_STACK,new ItemStack(ModBlocks.CALTROPS)))); }
    public static boolean hasPreventionRecipes() {
        var recipes=activeRuntime.getRecipeManager().createRecipeLookup(mezz.jei.api.constants.RecipeTypes.CRAFTING).includeHidden().get().toList();
        return List.of("tetanus_vaccine","protective_gloves","field_guide").stream().allMatch(id->recipes.stream().anyMatch(recipe->recipe.getId().equals(TetanusMod.id(id))));
    }
    public static void showVaccineRecipe(){activeRuntime.getRecipesGui().show(List.of(activeRuntime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT,VanillaTypes.ITEM_STACK,new ItemStack(ModItems.VACCINE))));}
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
    public static final RecipeType<com.tetanusmod.expansion.RustArrowSmithingRecipe> ARROW_SMITHING=RecipeType.create("tetanusmod","rust_arrows",com.tetanusmod.expansion.RustArrowSmithingRecipe.class);
    public static boolean hasArrowSmithing(){return activeRuntime!=null&&activeRuntime.getRecipeManager().createRecipeLookup(ARROW_SMITHING).get().count()==1;}
    public static void showArrowSmithing(){activeRuntime.getRecipesGui().showTypes(List.of(ARROW_SMITHING));}
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration){registration.addRecipeCatalyst(new ItemStack(Items.SMITHING_TABLE),ARROW_SMITHING);}
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new RustCategory(registration.getJeiHelpers().getGuiHelper()),new ArrowCategory(registration.getJeiHelpers().getGuiHelper()));
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        List<RustRecipe> recipes = new ArrayList<>();
        for (Item item : net.minecraft.registry.Registries.ITEM) {
            Item rusty = RustingSystem.rusted(item);
            if (rusty != null) recipes.add(new RustRecipe(item,rusty));
        }
        registration.addRecipes(RUSTING,recipes);
        registration.addRecipes(ARROW_SMITHING,List.of(new com.tetanusmod.expansion.RustArrowSmithingRecipe(TetanusMod.id("tetanized_arrow"))));
        registration.addIngredientInfo(new ItemStack(ModItems.WIRE_BRUSH),VanillaTypes.ITEM_STACK,Text.translatable("jei.tetanusmod.brush"));
        registration.addIngredientInfo(new ItemStack(ModItems.INFECTED_MEAT),VanillaTypes.ITEM_STACK,Text.translatable("jei.tetanusmod.meat"));
    }
    private static final class RustCategory implements IRecipeCategory<RustRecipe> {
        private final IDrawable background,icon;
        private RustCategory(IGuiHelper helper) {
            background=helper.createBlankDrawable(178,70);
            icon=helper.createDrawableItemStack(new ItemStack(ModItems.RUSTED_NAIL));
        }
        @Override public RecipeType<RustRecipe> getRecipeType() { return RUSTING; }
        @Override public Text getTitle() { return Text.translatable("jei.tetanusmod.rusting"); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder,RustRecipe recipe,IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT,8,14).addItemStack(new ItemStack(recipe.input()));
            builder.addSlot(RecipeIngredientRole.OUTPUT,150,14).addItemStack(new ItemStack(recipe.output()));
        }
        @Override public void draw(RustRecipe recipe,IRecipeSlotsView slots,DrawContext context,double mouseX,double mouseY) {
            var font=MinecraftClient.getInstance().textRenderer;
            context.drawText(font,Text.translatable("jei.tetanusmod.water",TetanusMod.CONFIG.waterRustTicks/20.0),35,12,0xFF6E3924,false);
            context.drawText(font,Text.translatable("jei.tetanusmod.drop"),8,51,0xFF444444,false);
            if (TetanusMod.CONFIG.rainRusting) context.drawText(font,Text.translatable("jei.tetanusmod.rain",TetanusMod.CONFIG.rainRustTicks/20.0),35,27,0xFF444444,false);
        }
    }
    private static final class ArrowCategory implements IRecipeCategory<com.tetanusmod.expansion.RustArrowSmithingRecipe> {
        private final IDrawable background,icon;
        ArrowCategory(IGuiHelper helper){background=helper.createBlankDrawable(178,74);icon=helper.createDrawableItemStack(new ItemStack(Items.SMITHING_TABLE));}
        @Override public RecipeType<com.tetanusmod.expansion.RustArrowSmithingRecipe> getRecipeType(){return ARROW_SMITHING;}
        @Override public Text getTitle(){return Text.translatable("jei.tetanusmod.batch_smithing");}
        @Override public IDrawable getBackground(){return background;}
        @Override public IDrawable getIcon(){return icon;}
        @Override public void setRecipe(IRecipeLayoutBuilder builder,com.tetanusmod.expansion.RustArrowSmithingRecipe recipe,IFocusGroup focuses){
            builder.addSlot(RecipeIngredientRole.INPUT,9,13).addItemStack(new ItemStack(Items.ARROW,64));
            builder.addSlot(RecipeIngredientRole.INPUT,73,13).addItemStack(new ItemStack(ModItems.RUST));
            builder.addSlot(RecipeIngredientRole.OUTPUT,149,13).addItemStack(new ItemStack(ModItems.TETANIZED_ARROW,64));
        }
        @Override public void draw(com.tetanusmod.expansion.RustArrowSmithingRecipe recipe,IRecipeSlotsView slots,DrawContext context,double mouseX,double mouseY){
            var font=MinecraftClient.getInstance().textRenderer;
            context.drawText(font,Text.translatable("jei.tetanusmod.batch_hint"),8,42,0xFF694026,false);
            context.drawText(font,Text.translatable("jei.tetanusmod.no_template"),8,57,0xFF444444,false);
        }
    }}
