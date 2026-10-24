package com.tetanusmod.item;

import com.tetanusmod.entity.NailEntity;
import net.minecraft.client.item.TooltipContext;
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

public final class NailItem extends Item {
    private final boolean rusty;
    private final boolean bent;

    public NailItem(boolean rusty, boolean bent, Settings settings) {
        super(settings);
        this.rusty = rusty;
        this.bent = bent;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if(user.isSpectator()||!user.isAlive()||!stack.isOf(this)||user.getItemCooldownManager().isCoolingDown(this))return TypedActionResult.pass(stack);
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW,
                SoundCategory.PLAYERS, 0.6F, 0.85F + world.random.nextFloat() * 0.25F);
        if (!world.isClient) {
            NailEntity nail = new NailEntity(world, user);
            nail.setItem(stack);nail.setThrownUpward(user.getPitch()<-60);
            nail.setRecoverable(!user.getAbilities().creativeMode);
            nail.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, bent ? 1.3F : 1.5F, bent ? 4.0F : 1.0F);

            world.spawnEntity(nail);
        }
        for (Item nailItem : new Item[]{com.tetanusmod.registry.ModItems.NAIL, com.tetanusmod.registry.ModItems.RUSTED_NAIL,
                com.tetanusmod.registry.ModItems.BENT_NAIL, com.tetanusmod.registry.ModItems.RUSTED_BENT_NAIL}) {
            user.getItemCooldownManager().set(nailItem, rusty ? 14 : 10);
        }
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        if (!user.getAbilities().creativeMode) stack.decrement(1);
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.tetanusmod.damage", bent ? 1 : com.tetanusmod.TetanusMod.CONFIG.nailDamage).formatted(Formatting.GRAY));
        TooltipHelper.metal(tooltip, rusty);
    }
    public boolean isRusty() { return rusty; }
    public boolean isBent() { return bent; }
}
