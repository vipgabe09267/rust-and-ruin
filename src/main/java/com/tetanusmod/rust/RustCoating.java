package com.tetanusmod.rust;

import com.tetanusmod.TetanusMod;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

/** Coating is item NBT, so splitting, saving and merging follow vanilla stack rules. */
public final class RustCoating {
    private static final String KEY = "RustRuinCoating";
    public static int remaining(ItemStack stack) {
        return stack.hasNbt() ? Math.max(0, Math.min(72000, stack.getNbt().getInt(KEY))) : 0;
    }
    public static boolean coat(ItemStack stack) {
        if (stack.isEmpty() || RustingSystem.rusted(stack.getItem()) == null || remaining(stack) > 0) return false;
        stack.getOrCreateNbt().putInt(KEY, TetanusMod.CONFIG.coatingWeatherTicks);
        return true;
    }
    public static void wear(ItemStack stack, int ticks) {
        int before = remaining(stack);
        if (before == 0 || ticks <= 0) return;
        int after = Math.max(0, before - Math.min(ticks, 72000));
        if (after == 0) stack.removeSubNbt(KEY);
        else stack.getOrCreateNbt().putInt(KEY, after);
    }
    public static void initialize() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack wax = player.getStackInHand(hand);
            Hand other = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
            ItemStack metal = player.getStackInHand(other);
            if (!wax.isOf(Items.HONEYCOMB) || player.isSpectator() || metal.isEmpty()
                    || RustingSystem.rusted(metal.getItem()) == null || remaining(metal) > 0) return TypedActionResult.pass(wax);
            // One honeycomb per item prevents cheap coating of entire projectile stacks.
            if (!player.getAbilities().creativeMode && wax.getCount() < metal.getCount()) return TypedActionResult.fail(wax);
            if (!world.isClient && coat(metal)) {
                if (!player.getAbilities().creativeMode) wax.decrement(metal.getCount());
                player.sendMessage(Text.translatable("message.tetanusmod.coated"), true);
            }
            return TypedActionResult.success(wax, world.isClient);
        });
    }
}