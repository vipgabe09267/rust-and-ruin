package com.tetanusmod.item;
import com.tetanusmod.effect.Infection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.world.World;
public final class InfectedMeatItem extends Item {
    public InfectedMeatItem(Settings settings) { super(settings); }
    @Override public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if(stack.isEmpty()||!stack.isOf(this)||!user.isAlive()||user.isSpectator())return stack;
        ItemStack result = super.finishUsing(stack, world, user);
        if (!world.isClient) {

            // Raw contaminated meat causes Salmonella; metal injuries cause Tetanus.
            if(user.getRandom().nextFloat()<com.tetanusmod.TetanusMod.CONFIG.salmonellaChance)
                user.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(com.tetanusmod.TetanusMod.SALMONELLA,1200));
        }
        return result;
    }
}
