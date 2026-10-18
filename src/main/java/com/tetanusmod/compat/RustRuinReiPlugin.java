package com.tetanusmod.compat;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.registry.ModItems;
import com.tetanusmod.rust.RustingSystem;
import me.shedaniel.math.*;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.*;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.*;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import java.util.*;

public final class RustRuinReiPlugin implements REIClientPlugin {
    public static final CategoryIdentifier<RustDisplay> RUSTING=CategoryIdentifier.of(TetanusMod.id("rusting"));
    public static volatile int registeredCount;
    public static boolean hasDiseaseBrewing() {
        var outputs=DisplayRegistry.getInstance().getAll().values().stream().flatMap(List::stream).flatMap(display->display.getOutputEntries().stream()).flatMap(List::stream).map(entry->entry.getValue()).filter(value->value instanceof ItemStack).map(value->(ItemStack)value).toList();
        for(var potion:List.of(com.tetanusmod.registry.ModBrewing.MANURE,com.tetanusmod.registry.ModBrewing.TETANUS,com.tetanusmod.registry.ModBrewing.SALMONELLA)) {
            if(outputs.stream().noneMatch(stack->net.minecraft.potion.PotionUtil.getPotion(stack)==potion && stack.isOf(Items.POTION)))return false;
            if(potion!=com.tetanusmod.registry.ModBrewing.MANURE && outputs.stream().noneMatch(stack->net.minecraft.potion.PotionUtil.getPotion(stack)==potion && stack.isOf(Items.SPLASH_POTION)))return false;
        }
        return true;
    }
    public static boolean showRusting() { return me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder().addCategory(RUSTING).open(); }
    public static boolean showCaltrops() { return me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder().addRecipesFor(EntryStacks.of(com.tetanusmod.registry.ModBlocks.CALTROPS)).open(); }
    public static final class RustDisplay extends BasicDisplay {
        public RustDisplay(Item input,Item output) { super(List.of(EntryIngredient.of(EntryStacks.of(input))),List.of(EntryIngredient.of(EntryStacks.of(output)))); }
        @Override public CategoryIdentifier<RustDisplay> getCategoryIdentifier() { return RUSTING; }
    }
    @Override public void registerCategories(CategoryRegistry registry) { registry.add(new RustCategory()); }
    @Override public void registerDisplays(DisplayRegistry registry) {
        int count=0;
        for(Item item:Registries.ITEM) { Item rusty=RustingSystem.rusted(item);if(rusty!=null){registry.add(new RustDisplay(item,rusty));count++;} }
        registeredCount=count;
    }
    private static final class RustCategory implements DisplayCategory<RustDisplay> {
        @Override public CategoryIdentifier<RustDisplay> getCategoryIdentifier() { return RUSTING; }
        @Override public Text getTitle() { return Text.translatable("jei.tetanusmod.rusting"); }
        @Override public Renderer getIcon() { return EntryStacks.of(ModItems.RUSTED_NAIL); }
        @Override public int getDisplayHeight() { return 70; }
        @Override public int getDisplayWidth(RustDisplay display) { return 178; }
        @Override public List<Widget> setupDisplay(RustDisplay display,Rectangle bounds) {
            var widgets=new ArrayList<Widget>();widgets.add(Widgets.createRecipeBase(bounds));
            widgets.add(Widgets.createSlot(new Point(bounds.x+9,bounds.y+15)).entries(display.getInputEntries().get(0)).markInput());
            widgets.add(Widgets.createSlot(new Point(bounds.x+150,bounds.y+15)).entries(display.getOutputEntries().get(0)).markOutput());
            widgets.add(Widgets.createLabel(new Point(bounds.x+35,bounds.y+12),Text.translatable("jei.tetanusmod.water",TetanusMod.CONFIG.waterRustTicks/20.0)).leftAligned().noShadow());
            if(TetanusMod.CONFIG.rainRusting)widgets.add(Widgets.createLabel(new Point(bounds.x+35,bounds.y+26),Text.translatable("jei.tetanusmod.rain",TetanusMod.CONFIG.rainRustTicks/20.0)).leftAligned().noShadow());
            widgets.add(Widgets.createLabel(new Point(bounds.x+9,bounds.y+51),Text.translatable("jei.tetanusmod.drop")).leftAligned().noShadow());
            return widgets;
        }
    }
}