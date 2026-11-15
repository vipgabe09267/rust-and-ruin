package com.tetanusmod.compat;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.medical.MedicalWorkstations;
import com.tetanusmod.medical.PrescriptionWritingRecipe;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.*;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

/** Optional REI entrypoint for the actual, dedicated writing recipe. */
public final class MedicalReiPlugin implements REIClientPlugin {
    public static final CategoryIdentifier<WritingDisplay> WRITING=CategoryIdentifier.of(TetanusMod.id("prescription_writing"));
    public static volatile int registeredRecipes;
    @Override public void registerCategories(CategoryRegistry registry){registry.add(new WritingCategory());registry.addWorkstations(WRITING,EntryStacks.of(MedicalWorkstations.WRITER_ITEM));}
    @Override public void registerDisplays(DisplayRegistry registry){
        registeredRecipes=0;var client=MinecraftClient.getInstance();if(client.world==null)return;
        for(var recipe:client.world.getRecipeManager().values())if(recipe instanceof PrescriptionWritingRecipe writing){registry.add(new WritingDisplay(writing));registeredRecipes++;}
    }
    public static final class WritingDisplay extends BasicDisplay {
        WritingDisplay(PrescriptionWritingRecipe recipe){super(List.of(EntryIngredient.of(EntryStacks.of(Items.PAPER)),EntryIngredient.of(EntryStacks.of(Items.INK_SAC))),List.of(EntryIngredient.of(EntryStacks.of(MedicalWorkstations.PRESCRIPTION))));}
        @Override public CategoryIdentifier<WritingDisplay> getCategoryIdentifier(){return WRITING;}
    }
    private static final class WritingCategory implements me.shedaniel.rei.api.client.registry.display.DisplayCategory<WritingDisplay> {
        @Override public CategoryIdentifier<WritingDisplay> getCategoryIdentifier(){return WRITING;}
        @Override public Text getTitle(){return Text.translatable("block.tetanusmod.prescription_writer");}
        @Override public Renderer getIcon(){return EntryStacks.of(MedicalWorkstations.WRITER_ITEM);}
        @Override public int getDisplayHeight(){return 58;}
        @Override public int getDisplayWidth(WritingDisplay display){return 166;}
        @Override public List<Widget> setupDisplay(WritingDisplay display,Rectangle bounds){
            var widgets=new ArrayList<Widget>();widgets.add(Widgets.createRecipeBase(bounds));
            widgets.add(Widgets.createSlot(new Point(bounds.x+12,bounds.y+12)).entries(display.getInputEntries().get(0)).markInput());
            widgets.add(Widgets.createSlot(new Point(bounds.x+60,bounds.y+12)).entries(display.getInputEntries().get(1)).markInput());
            widgets.add(Widgets.createSlot(new Point(bounds.x+132,bounds.y+12)).entries(display.getOutputEntries().get(0)).markOutput());
            widgets.add(Widgets.createLabel(new Point(bounds.x+40,bounds.y+16),Text.literal("+")).leftAligned().noShadow().color(0xFF415950,0xFFE8EBD8));
            widgets.add(Widgets.createLabel(new Point(bounds.x+102,bounds.y+16),Text.literal(">")).leftAligned().noShadow().color(0xFF415950,0xFFE8EBD8));
            widgets.add(Widgets.createLabel(new Point(bounds.x+5,bounds.y+42),Text.translatable("gui.tetanusmod.writer.hint")).leftAligned().noShadow().color(0xFF415950,0xFFE8EBD8));return widgets;
        }
    }
}
