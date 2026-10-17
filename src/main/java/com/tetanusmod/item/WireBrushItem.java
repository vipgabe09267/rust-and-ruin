package com.tetanusmod.item;

import com.tetanusmod.rust.RustingSystem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class WireBrushItem extends Item {
    public WireBrushItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack brush = user.getStackInHand(hand);
        Hand otherHand = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack rusty = user.getStackInHand(otherHand);
        Item clean = RustingSystem.clean(rusty.getItem());
        if (clean == null) return TypedActionResult.pass(brush);
        // Clean one stack, requiring one brush durability per item. Refuse partial/ambiguous conversions.
        int cost = rusty.getCount();
        if (!user.getAbilities().creativeMode && brush.getMaxDamage() - brush.getDamage() < cost) {
            return TypedActionResult.fail(brush);
        }
        if (!world.isClient) {
            user.setStackInHand(otherHand, RustingSystem.convert(rusty, clean));
            ItemStack flakes=new ItemStack(com.tetanusmod.registry.ModItems.RUST);
            if(!user.getInventory().insertStack(flakes))user.dropItem(flakes,false);
            if (!user.getAbilities().creativeMode) brush.damage(cost, user, player -> player.sendToolBreakStatus(hand));
            world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC,
                    SoundCategory.PLAYERS, 0.7F, 1.0F);
        }
        return TypedActionResult.success(brush, world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.tetanusmod.brush").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.tetanusmod.durability", stack.getMaxDamage() - stack.getDamage(), stack.getMaxDamage()).formatted(Formatting.GRAY));
    }
}
