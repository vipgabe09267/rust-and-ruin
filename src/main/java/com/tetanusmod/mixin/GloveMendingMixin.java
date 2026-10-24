package com.tetanusmod.mixin;

import com.tetanusmod.expansion.GloveMending;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.*;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(ExperienceOrbEntity.class)
public abstract class GloveMendingMixin {
    @Redirect(method="repairPlayerGears",at=@At(value="INVOKE",target="Lnet/minecraft/enchantment/EnchantmentHelper;chooseEquipmentWith(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Map$Entry;"))
    private Map.Entry<EquipmentSlot,ItemStack> gloves(Enchantment enchantment,LivingEntity entity,Predicate<ItemStack> predicate){return GloveMending.choose(enchantment,entity,predicate);}
}
