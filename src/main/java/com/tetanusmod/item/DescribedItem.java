package com.tetanusmod.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import java.util.List;

public class DescribedItem extends Item {
    private final String description;
    public DescribedItem(Settings settings, String description) { super(settings); this.description=description; }
    @Override public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.tetanusmod."+description).formatted(Formatting.GRAY));
    }
}