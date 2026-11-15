package com.tetanusmod.compat;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.dragon.DragonContent;
import com.tetanusmod.dragon.CrystalForgingRecipe;
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
public final class DragonReiPlugin implements REIClientPlugin {
    public static final CategoryIdentifier<ForgingDisplay> FORGING=CategoryIdentifier.of(TetanusMod.id("crystal_forging"));
    public static volatile int registeredRecipes;
    @Override public void registerCategories(CategoryRegistry registry){registry.add(new ForgingCategory());registry.addWorkstations(FORGING,EntryStacks.of(DragonContent.FORGE_ITEM));}
    @Override public void registerDisplays(DisplayRegistry registry){
        registeredRecipes=0;var client=MinecraftClient.getInstance();if(client.world==null)return;
        for(var recipe:client.world.getRecipeManager().values())if(recipe instanceof CrystalForgingRecipe writing){registry.add(new ForgingDisplay(writing));registeredRecipes++;}
    }
    public static final class ForgingDisplay extends BasicDisplay {
        ForgingDisplay(CrystalForgingRecipe recipe){super(List.of(EntryIngredient.of(EntryStacks.of(Items.END_CRYSTAL)),EntryIngredient.of(EntryStacks.of(com.tetanusmod.registry.ModItems.RUST)),EntryIngredient.of(EntryStacks.of(Items.NETHERITE_INGOT))),List.of(EntryIngredient.of(EntryStacks.of(DragonContent.CRYSTAL))));}
        @Override public CategoryIdentifier<ForgingDisplay> getCategoryIdentifier(){return FORGING;}
    }
    private static final class ForgingCategory implements me.shedaniel.rei.api.client.registry.display.DisplayCategory<ForgingDisplay> {
        @Override public CategoryIdentifier<ForgingDisplay> getCategoryIdentifier(){return FORGING;}
        @Override public Text getTitle(){return Text.translatable("block.tetanusmod.tetanus_wither_forging_table");}
        @Override public Renderer getIcon(){return EntryStacks.of(DragonContent.FORGE_ITEM);}
        @Override public int getDisplayHeight(){return 58;}
        @Override public int getDisplayWidth(ForgingDisplay display){return 196;}
        @Override public List<Widget> setupDisplay(ForgingDisplay display,Rectangle bounds){
            var widgets=new ArrayList<Widget>();widgets.add(Widgets.createRecipeBase(bounds));
            widgets.add(Widgets.createSlot(new Point(bounds.x+12,bounds.y+12)).entries(display.getInputEntries().get(0)).markInput());
            widgets.add(Widgets.createSlot(new Point(bounds.x+54,bounds.y+12)).entries(display.getInputEntries().get(1)).markInput());
            widgets.add(Widgets.createSlot(new Point(bounds.x+96,bounds.y+12)).entries(display.getInputEntries().get(2)).markInput());
            widgets.add(Widgets.createSlot(new Point(bounds.x+164,bounds.y+12)).entries(display.getOutputEntries().get(0)).markOutput());
            widgets.add(Widgets.createLabel(new Point(bounds.x+40,bounds.y+16),Text.literal("+")).leftAligned().noShadow().color(0xFF415950,0xFFE8EBD8));
            widgets.add(Widgets.createLabel(new Point(bounds.x+82,bounds.y+16),Text.literal("+")).leftAligned().noShadow().color(0xFF415950,0xFFE8EBD8));
            widgets.add(Widgets.createLabel(new Point(bounds.x+140,bounds.y+16),Text.literal(">")).leftAligned().noShadow().color(0xFF415950,0xFFE8EBD8));
            widgets.add(Widgets.createLabel(new Point(bounds.x+5,bounds.y+42),Text.translatable("gui.tetanusmod.forge.hint")).leftAligned().noShadow().color(0xFF415950,0xFFE8EBD8));return widgets;
        }
    }
}
