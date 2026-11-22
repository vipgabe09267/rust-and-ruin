package com.tetanusmod.item;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.InfectionState;
import com.tetanusmod.expansion.ExpansionEffects;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class FieldSplintItem extends Item {
    public static final int DURATION_SECONDS = 90;

    public FieldSplintItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(this) || player.isSpectator() || !player.isAlive()) return TypedActionResult.fail(stack);
        if (!player.hasStatusEffect(TetanusMod.TETANUS)) {
            if (!world.isClient) player.sendMessage(Text.translatable("message.tetanusmod.splint_healthy"), true);
            return TypedActionResult.fail(stack);
        }
        if (player.hasStatusEffect(ExpansionEffects.SPLINTED)) {
            if (!world.isClient) player.sendMessage(Text.translatable("message.tetanusmod.splint_active"), true);
            return TypedActionResult.fail(stack);
        }
        if (world.isClient) return TypedActionResult.success(stack, true);
        if (!player.addStatusEffect(new StatusEffectInstance(ExpansionEffects.SPLINTED, DURATION_SECONDS * 20, 0, false, false, true))) {
            return TypedActionResult.fail(stack);
        }
        ((InfectionState) player).tetanus$setFreeze(0);
        if (!player.getAbilities().creativeMode) stack.decrement(1);
        player.incrementStat(Stats.USED.getOrCreateStat(this));
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 0.75F, 1.12F);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.tetanusmod.field_splint", DURATION_SECONDS).formatted(Formatting.GRAY));
    }
}
