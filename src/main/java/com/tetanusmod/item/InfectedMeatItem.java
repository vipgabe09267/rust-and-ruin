package com.tetanusmod.item;
import com.tetanusmod.effect.Infection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.world.World;
public final class InfectedMeatItem extends Item {
    public InfectedMeatItem(Settings settings) { super(settings); }
    @Override public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack result = super.finishUsing(stack, world, user);
        if (!world.isClient) {
            Infection.apply(user, null);
            if(user.getRandom().nextFloat()<com.tetanusmod.TetanusMod.CONFIG.salmonellaChance)
                user.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(com.tetanusmod.TetanusMod.SALMONELLA,1200));
        }
        return result;
    }
}
