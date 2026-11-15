package com.tetanusmod.compat;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.medical.MedicalWorkstations;
import com.tetanusmod.medical.PrescriptionWritingRecipe;
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
public final class MedicalJeiPlugin implements IModPlugin {
    public static final RecipeType<PrescriptionWritingRecipe> WRITING=RecipeType.create("tetanusmod","prescription_writing",PrescriptionWritingRecipe.class);
    public static volatile long registeredRecipes;
    private static mezz.jei.api.runtime.IJeiRuntime activeRuntime;
    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime){activeRuntime=runtime;}
    @Override public void onRuntimeUnavailable(){activeRuntime=null;}
    public static boolean showWriting(){if(activeRuntime==null)return false;activeRuntime.getRecipesGui().showTypes(java.util.List.of(WRITING));return true;}
    @Override public Identifier getPluginUid(){return TetanusMod.id("medical_jei");}
    @Override public void registerCategories(IRecipeCategoryRegistration registration){registration.addRecipeCategories(new WritingCategory(registration.getJeiHelpers().getGuiHelper()));}
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration){registration.addRecipeCatalyst(new ItemStack(MedicalWorkstations.WRITER_ITEM),WRITING);}
    @Override public void registerRecipes(IRecipeRegistration registration){
        var client=MinecraftClient.getInstance();
        var recipes=client.world==null?List.<PrescriptionWritingRecipe>of():client.world.getRecipeManager().values().stream().filter(PrescriptionWritingRecipe.class::isInstance).map(PrescriptionWritingRecipe.class::cast).toList();
        registration.addRecipes(WRITING,recipes);registeredRecipes=recipes.size();
        registration.addIngredientInfo(new ItemStack(MedicalWorkstations.PRESCRIPTION),VanillaTypes.ITEM_STACK,Text.translatable("jei.tetanusmod.prescription_use"));
    }
    private static final class WritingCategory implements IRecipeCategory<PrescriptionWritingRecipe> {
        private final IDrawable background,icon;
        WritingCategory(IGuiHelper helper){background=helper.createBlankDrawable(166,58);icon=helper.createDrawableItemStack(new ItemStack(MedicalWorkstations.WRITER_ITEM));}
        @Override public RecipeType<PrescriptionWritingRecipe> getRecipeType(){return WRITING;}
        @Override public Text getTitle(){return Text.translatable("block.tetanusmod.prescription_writer");}
        @Override public IDrawable getBackground(){return background;}
        @Override public IDrawable getIcon(){return icon;}
        @Override public void setRecipe(IRecipeLayoutBuilder builder,PrescriptionWritingRecipe recipe,IFocusGroup focuses){
            builder.addSlot(RecipeIngredientRole.INPUT,12,12).addItemStack(new ItemStack(Items.PAPER));
            builder.addSlot(RecipeIngredientRole.INPUT,60,12).addItemStack(new ItemStack(Items.INK_SAC));
            builder.addSlot(RecipeIngredientRole.OUTPUT,132,12).addItemStack(new ItemStack(MedicalWorkstations.PRESCRIPTION));
        }
        @Override public void draw(PrescriptionWritingRecipe recipe,IRecipeSlotsView slots,DrawContext draw,double mouseX,double mouseY){
            var font=MinecraftClient.getInstance().textRenderer;draw.drawText(font,"+",40,16,0xFF415950,false);draw.drawText(font,">",102,16,0xFF415950,false);
            draw.drawText(font,Text.translatable("gui.tetanusmod.writer.hint"),5,42,0xFF415950,false);
        }
    }
}
