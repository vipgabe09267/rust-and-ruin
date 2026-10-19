package com.tetanusmod.mixin;

import com.tetanusmod.expansion.ExpansionItems;
import net.minecraft.item.ItemStack;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class EnchantmentTooltipMixin {
    @Inject(method="getTooltip",at=@At("RETURN"))
    private void description(PlayerEntity player,TooltipContext context,CallbackInfoReturnable<List<Text>> result){
        ItemStack stack=(ItemStack)(Object)this;
        boolean present=EnchantmentHelper.getLevel(ExpansionItems.TETANUS_PROTECTION,stack)>0;
        if(!present&&stack.getItem() instanceof EnchantedBookItem)
            present=EnchantmentHelper.fromNbt(EnchantedBookItem.getEnchantmentNbt(stack)).containsKey(ExpansionItems.TETANUS_PROTECTION);
        if(present)for(String key:List.of("shield_protection","shield_protection_i","shield_protection_ii"))result.getReturnValue().add(Text.translatable("tooltip.tetanusmod."+key).formatted(net.minecraft.util.Formatting.DARK_AQUA));
    }
}