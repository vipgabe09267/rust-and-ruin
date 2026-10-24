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
        for(var potion:List.of(com.tetanusmod.registry.ModBrewing.MANURE,com.tetanusmod.registry.ModBrewing.TETANUS,com.tetanusmod.registry.ModBrewing.SALMONELLA,com.tetanusmod.registry.ModBrewing.LOCKJAW,com.tetanusmod.registry.ModBrewing.RABIES)) {
            if(outputs.stream().noneMatch(stack->net.minecraft.potion.PotionUtil.getPotion(stack)==potion && stack.isOf(Items.POTION)))return false;
            if(potion!=com.tetanusmod.registry.ModBrewing.MANURE && outputs.stream().noneMatch(stack->net.minecraft.potion.PotionUtil.getPotion(stack)==potion && stack.isOf(Items.SPLASH_POTION)))return false;
        }
        return true;
    }
    public static boolean showRusting() { return me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder().addCategory(RUSTING).open(); }
    public static boolean showVaccineRecipe(){return me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder().addRecipesFor(EntryStacks.of(ModItems.VACCINE)).open();}
    public static boolean showCaltrops() { return me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder().addRecipesFor(EntryStacks.of(com.tetanusmod.registry.ModBlocks.CALTROPS)).open(); }
    public static final class RustDisplay extends BasicDisplay {
        public RustDisplay(Item input,Item output) { super(List.of(EntryIngredient.of(EntryStacks.of(input))),List.of(EntryIngredient.of(EntryStacks.of(output)))); }
        @Override public CategoryIdentifier<RustDisplay> getCategoryIdentifier() { return RUSTING; }
    }
    @Override public void registerCategories(CategoryRegistry registry) { registry.add(new RustCategory());registry.add(new ArrowCategory());registry.add(new WaxCategory());registry.addWorkstations(ARROW_SMITHING,EntryStacks.of(Items.SMITHING_TABLE)); }
    @Override public void registerDisplays(DisplayRegistry registry) {
        int count=0;
        for(Item item:Registries.ITEM) { Item rusty=RustingSystem.rusted(item);if(rusty!=null){registry.add(new RustDisplay(item,rusty));registry.add(new WaxDisplay(item));count++;} }
        registeredCount=count;registry.add(new ArrowDisplay());
    }
    private static void addWrappedLabel(List<Widget> widgets, int x, int y, int width, Text message) {
        var handler = net.minecraft.client.MinecraftClient.getInstance().textRenderer.getTextHandler();
        for (var line : handler.wrapLines(message, width, net.minecraft.text.Style.EMPTY)) {
            widgets.add(Widgets.createLabel(new Point(x, y), Text.literal(line.getString()))
                    .leftAligned().noShadow().color(0xFF443328,0xFFF2DEC6));
            y += 10;
        }
    }
    private static final class RustCategory implements DisplayCategory<RustDisplay> {
        @Override public CategoryIdentifier<RustDisplay> getCategoryIdentifier() { return RUSTING; }
        @Override public Text getTitle() { return Text.translatable("jei.tetanusmod.rusting"); }
        @Override public Renderer getIcon() { return EntryStacks.of(ModItems.RUSTED_NAIL); }
        @Override public int getDisplayHeight() { return 84; }
        @Override public int getDisplayWidth(RustDisplay display) { return 178; }
        @Override public List<Widget> setupDisplay(RustDisplay display,Rectangle bounds) {
            var widgets=new ArrayList<Widget>();widgets.add(Widgets.createRecipeBase(bounds));
            widgets.add(Widgets.createSlot(new Point(bounds.x+9,bounds.y+15)).entries(display.getInputEntries().get(0)).markInput());
            widgets.add(Widgets.createSlot(new Point(bounds.x+150,bounds.y+15)).entries(display.getOutputEntries().get(0)).markOutput());
            widgets.add(Widgets.createLabel(new Point(bounds.x+35,bounds.y+12),Text.translatable("jei.tetanusmod.water",TetanusMod.CONFIG.waterRustTicks/20.0)).leftAligned().noShadow().color(0xFF443328,0xFFF2DEC6));
            if(TetanusMod.CONFIG.rainRusting)widgets.add(Widgets.createLabel(new Point(bounds.x+35,bounds.y+26),Text.translatable("jei.tetanusmod.rain",TetanusMod.CONFIG.rainRustTicks/20.0)).leftAligned().noShadow().color(0xFF443328,0xFFF2DEC6));
            addWrappedLabel(widgets,bounds.x+9,bounds.y+51,bounds.width-18,Text.translatable("jei.tetanusmod.drop"));
            return widgets;
        }
    }
    public static final CategoryIdentifier<ArrowDisplay> ARROW_SMITHING=CategoryIdentifier.of(TetanusMod.id("rust_arrows"));
    public static boolean hasArrowSmithing(){return DisplayRegistry.getInstance().getAll().getOrDefault(ARROW_SMITHING,List.of()).size()==1;}
    public static boolean showArrowSmithing(){return me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder().addCategory(ARROW_SMITHING).open();}
    public static final class ArrowDisplay extends BasicDisplay {
        ArrowDisplay(){super(List.of(EntryIngredient.of(EntryStacks.of(new ItemStack(Items.ARROW,64))),EntryIngredient.of(EntryStacks.of(ModItems.RUST))),List.of(EntryIngredient.of(EntryStacks.of(new ItemStack(ModItems.TETANIZED_ARROW,64)))));}
        @Override public CategoryIdentifier<ArrowDisplay> getCategoryIdentifier(){return ARROW_SMITHING;}
    }
    private static final class ArrowCategory implements DisplayCategory<ArrowDisplay> {
        @Override public CategoryIdentifier<ArrowDisplay> getCategoryIdentifier(){return ARROW_SMITHING;}
        @Override public Text getTitle(){return Text.translatable("jei.tetanusmod.batch_smithing");}
        @Override public Renderer getIcon(){return EntryStacks.of(Items.SMITHING_TABLE);}
        @Override public int getDisplayHeight(){return 74;}
        @Override public int getDisplayWidth(ArrowDisplay display){return 178;}
        @Override public List<Widget> setupDisplay(ArrowDisplay display,Rectangle bounds){
            var widgets=new ArrayList<Widget>();widgets.add(Widgets.createRecipeBase(bounds));
            widgets.add(Widgets.createSlot(new Point(bounds.x+9,bounds.y+13)).entries(display.getInputEntries().get(0)).markInput());
            widgets.add(Widgets.createSlot(new Point(bounds.x+73,bounds.y+13)).entries(display.getInputEntries().get(1)).markInput());
            widgets.add(Widgets.createSlot(new Point(bounds.x+149,bounds.y+13)).entries(display.getOutputEntries().get(0)).markOutput());
            widgets.add(Widgets.createLabel(new Point(bounds.x+8,bounds.y+42),Text.translatable("jei.tetanusmod.batch_hint")).leftAligned().noShadow().color(0xFF443328,0xFFF2DEC6));
            widgets.add(Widgets.createLabel(new Point(bounds.x+8,bounds.y+57),Text.translatable("jei.tetanusmod.no_template")).leftAligned().noShadow().color(0xFF443328,0xFFF2DEC6));return widgets;
        }
    }
    public static final CategoryIdentifier<WaxDisplay> COATING=CategoryIdentifier.of(TetanusMod.id("coating"));
    public static boolean showCoating(){return me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder().addCategory(COATING).open();}
    public static boolean hasCoating(){return DisplayRegistry.getInstance().getAll().getOrDefault(COATING,List.of()).size()==9;}
    public static final class WaxDisplay extends BasicDisplay {
        private final Item item;
        WaxDisplay(Item item){super(List.of(EntryIngredient.of(EntryStacks.of(Items.HONEYCOMB)),EntryIngredient.of(EntryStacks.of(item))),List.of(EntryIngredient.of(EntryStacks.of(waxed(item)))));this.item=item;}
        @Override public List<EntryIngredient> getOutputEntries(){return List.of(EntryIngredient.of(EntryStacks.of(waxed(item))));}
        private static ItemStack waxed(Item item){var stack=new ItemStack(item);com.tetanusmod.rust.RustCoating.coat(stack);return stack;}
        public CategoryIdentifier<WaxDisplay> getCategoryIdentifier(){return COATING;}
    }
    private static final class WaxCategory implements DisplayCategory<WaxDisplay> {
        public CategoryIdentifier<WaxDisplay> getCategoryIdentifier(){return COATING;}
        public Text getTitle(){return Text.translatable("jei.tetanusmod.coating_title");}
        public Renderer getIcon(){return EntryStacks.of(Items.HONEYCOMB);}
        public int getDisplayHeight(){return 90;}
        public int getDisplayWidth(WaxDisplay display){return 178;}
        public List<Widget> setupDisplay(WaxDisplay display,Rectangle bounds){
            var widgets=new ArrayList<Widget>();widgets.add(Widgets.createRecipeBase(bounds));
            widgets.add(Widgets.createSlot(new Point(bounds.x+8,bounds.y+8)).entries(display.getInputEntries().get(0)).markInput());
            widgets.add(Widgets.createSlot(new Point(bounds.x+70,bounds.y+8)).entries(display.getInputEntries().get(1)).markInput());
            widgets.add(Widgets.createSlot(new Point(bounds.x+150,bounds.y+8)).entries(display.getOutputEntries().get(0)).markOutput());
            widgets.add(Widgets.createLabel(new Point(bounds.x+4,bounds.y+36),Text.translatable("jei.tetanusmod.coating_use")).leftAligned().noShadow().color(0xFF443328,0xFFF2DEC6));
            widgets.add(Widgets.createLabel(new Point(bounds.x+4,bounds.y+49),Text.translatable("jei.tetanusmod.coating_cost")).leftAligned().noShadow().color(0xFF443328,0xFFF2DEC6));
            addWrappedLabel(widgets,bounds.x+4,bounds.y+62,bounds.width-8,Text.translatable("jei.tetanusmod.coating_wear",TetanusMod.CONFIG.coatingWeatherTicks/20));
            return widgets;
        }
    }
}