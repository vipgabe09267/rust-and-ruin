package com.tetanusmod.item;

import com.tetanusmod.TetanusMod;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;

public final class TooltipHelper {
    public static boolean details() { return net.minecraft.client.gui.screen.Screen.hasShiftDown(); }
    public static void metal(List<Text> tooltip, boolean rusty) {
        if (!details()) {
            tooltip.add(Text.translatable(rusty ? "tooltip.tetanusmod.rusty_short" : "tooltip.tetanusmod.clean_short").formatted(Formatting.GRAY));
            return;
        }
        if (rusty) tooltip.add(Text.translatable("tooltip.tetanusmod.infection",
                TetanusMod.CONFIG.minDurationSeconds, TetanusMod.CONFIG.maxDurationSeconds).formatted(Formatting.DARK_RED));
        else {
            tooltip.add(Text.translatable("tooltip.tetanusmod.clean").formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.tetanusmod.water", TetanusMod.CONFIG.waterRustTicks / 20.0).formatted(Formatting.GRAY));
            if (TetanusMod.CONFIG.rainRusting) tooltip.add(Text.translatable("tooltip.tetanusmod.rain",
                    TetanusMod.CONFIG.rainRustTicks / 20.0).formatted(Formatting.GRAY));
        }
    }
}
