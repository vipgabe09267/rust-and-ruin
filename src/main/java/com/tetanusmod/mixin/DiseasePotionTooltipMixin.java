package com.tetanusmod.mixin;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.registry.ModBrewing;
import net.minecraft.item.*;
import net.minecraft.potion.PotionUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.List;

@Mixin({PotionItem.class,LingeringPotionItem.class})
public abstract class DiseasePotionTooltipMixin {
    private static String clock(int seconds){return String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60);}
    @Redirect(method="appendTooltip",at=@At(value="INVOKE",target="Lnet/minecraft/potion/PotionUtil;buildTooltip(Lnet/minecraft/item/ItemStack;Ljava/util/List;F)V"))
    private void rustruin$actualSymptoms(ItemStack stack,List<Text> lines,float multiplier) {
        var potion=PotionUtil.getPotion(stack);
        if(potion==ModBrewing.TETANUS) {
            lines.add(Text.translatable("tooltip.tetanusmod.potion_duration",clock(TetanusMod.CONFIG.minDurationSeconds),clock(TetanusMod.CONFIG.maxDurationSeconds)).formatted(Formatting.RED));
            lines.add(Text.translatable("tooltip.tetanusmod.potion_stiffness").formatted(Formatting.GRAY));
            lines.add(Text.translatable("tooltip.tetanusmod.potion_lockjaw").formatted(Formatting.GRAY));
            lines.add(Text.translatable("tooltip.tetanusmod.potion_cure").formatted(Formatting.DARK_GREEN));
        }else {
            PotionUtil.buildTooltip(stack,lines,multiplier);
            if(potion==ModBrewing.SALMONELLA) {
                lines.add(Text.translatable("tooltip.tetanusmod.potion_salmonella").formatted(Formatting.GRAY));
                lines.add(Text.translatable("tooltip.tetanusmod.potion_sickness").formatted(Formatting.GRAY));
            }
        }
    }
}