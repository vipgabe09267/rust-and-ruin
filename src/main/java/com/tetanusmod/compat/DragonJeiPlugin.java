package com.tetanusmod.compat;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.dragon.DragonContent;
import com.tetanusmod.dragon.CrystalForgingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
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
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.List;

/** Optional JEI entrypoint; never loaded by the dedicated server or plain client. */
@JeiPlugin
public final class DragonJeiPlugin implements IModPlugin {
    public static final RecipeType<CrystalForgingRecipe> FORGING=RecipeType.create("tetanusmod","crystal_forging",CrystalForgingRecipe.class);
    public static volatile long registeredRecipes;
    private static mezz.jei.api.runtime.IJeiRuntime activeRuntime;
    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime){activeRuntime=runtime;}
    @Override public void onRuntimeUnavailable(){activeRuntime=null;}
    public static boolean showForging(){if(activeRuntime==null)return false;activeRuntime.getRecipesGui().showTypes(java.util.List.of(FORGING));return true;}
    @Override public Identifier getPluginUid(){return TetanusMod.id("dragon_forging_jei");}
    @Override public void registerCategories(IRecipeCategoryRegistration registration){registration.addRecipeCategories(new ForgingCategory(registration.getJeiHelpers().getGuiHelper()));}
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration){registration.addRecipeCatalyst(new ItemStack(DragonContent.FORGE_ITEM),FORGING);}
    @Override public void registerRecipes(IRecipeRegistration registration){
        var client=MinecraftClient.getInstance();
        var recipes=client.world==null?List.<CrystalForgingRecipe>of():client.world.getRecipeManager().values().stream().filter(CrystalForgingRecipe.class::isInstance).map(CrystalForgingRecipe.class::cast).toList();
        registration.addRecipes(FORGING,recipes);registeredRecipes=recipes.size();
        registration.addIngredientInfo(new ItemStack(DragonContent.CRYSTAL),VanillaTypes.ITEM_STACK,Text.translatable("jei.tetanusmod.forging_use"));
    }
    private static final class ForgingCategory implements IRecipeCategory<CrystalForgingRecipe> {
        private final IDrawable background,icon;
        ForgingCategory(IGuiHelper helper){background=helper.createBlankDrawable(196,58);icon=helper.createDrawableItemStack(new ItemStack(DragonContent.FORGE_ITEM));}
        @Override public RecipeType<CrystalForgingRecipe> getRecipeType(){return FORGING;}
        @Override public Text getTitle(){return Text.translatable("block.tetanusmod.tetanus_wither_forging_table");}
        @Override public IDrawable getBackground(){return background;}
        @Override public IDrawable getIcon(){return icon;}
        @Override public void setRecipe(IRecipeLayoutBuilder builder,CrystalForgingRecipe recipe,IFocusGroup focuses){
            builder.addSlot(RecipeIngredientRole.INPUT,12,12).addItemStack(new ItemStack(Items.END_CRYSTAL));
            builder.addSlot(RecipeIngredientRole.INPUT,54,12).addItemStack(new ItemStack(com.tetanusmod.registry.ModItems.RUST));
            builder.addSlot(RecipeIngredientRole.INPUT,96,12).addItemStack(new ItemStack(Items.NETHERITE_INGOT));
            builder.addSlot(RecipeIngredientRole.OUTPUT,164,12).addItemStack(new ItemStack(DragonContent.CRYSTAL));
        }
        @Override public void draw(CrystalForgingRecipe recipe,IRecipeSlotsView slots,DrawContext draw,double mouseX,double mouseY){
            var font=MinecraftClient.getInstance().textRenderer;draw.drawText(font,"+",38,16,0xFF415950,false);draw.drawText(font,"+",80,16,0xFF415950,false);draw.drawText(font,">",140,16,0xFF415950,false);
            draw.drawText(font,Text.translatable("gui.tetanusmod.forge.hint"),5,42,0xFF415950,false);
        }
    }
}
