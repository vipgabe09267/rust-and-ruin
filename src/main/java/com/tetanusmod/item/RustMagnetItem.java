package com.tetanusmod.item;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.mixin.ItemEntityAccessor;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public final class RustMagnetItem extends Item {
    private static final TagKey<Item> SALVAGE = TagKey.of(RegistryKeys.ITEM, TetanusMod.id("magnetic_salvage"));
    private static final int RANGE = 7;
    private static final int MAX_STACKS = 24;
    private static final int COOLDOWN = 25;

    public RustMagnetItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(this) || player.isSpectator() || !player.isAlive()
                || player.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        if (world.isClient) return TypedActionResult.success(stack, true);

        Vec3d destination = new Vec3d(player.getX(), player.getBodyY(0.25), player.getZ());
        var targets = world.getEntitiesByClass(ItemEntity.class, player.getBoundingBox().expand(RANGE),
                        item -> canPull(player, item, destination)).stream()
                .sorted(Comparator.comparingDouble(item -> item.squaredDistanceTo(destination)))
                .filter(item -> world.raycast(new RaycastContext(destination, item.getPos().add(0, 0.10, 0),
                        RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player)).getType() == HitResult.Type.MISS)
                .limit(MAX_STACKS).toList();
        player.getItemCooldownManager().set(this, targets.isEmpty() ? 10 : COOLDOWN);
        if (targets.isEmpty()) {
            player.sendMessage(Text.translatable("message.tetanusmod.magnet_empty"), true);
            return TypedActionResult.pass(stack);
        }
        for (var item : targets) {
            Vec3d direction = destination.subtract(item.getPos());
            double distance = direction.length();
            Vec3d pulse = direction.normalize().multiply(Math.min(0.85, distance * 0.35));
            item.setVelocity(pulse.add(0, Math.min(0.22, distance * 0.04), 0));
            item.velocityModified = true;
        }
        stack.damage(1, player, user -> user.sendToolBreakStatus(hand));
        player.incrementStat(Stats.USED.getOrCreateStat(this));
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_CHAIN_HIT, SoundCategory.PLAYERS, 0.75F, 0.85F);
        return TypedActionResult.consume(stack);
    }

    private boolean canPull(PlayerEntity player, ItemEntity item, Vec3d destination) {
        double distance = item.squaredDistanceTo(destination);
        var owner = ((ItemEntityAccessor) item).rustruin$getOwner();
        return item.isAlive() && distance >= 0.25 && distance <= RANGE * RANGE
                && item.getStack().isIn(SALVAGE) && !item.cannotPickup()
                && (owner == null || owner.equals(player.getUuid()));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.tetanusmod.rust_magnet", RANGE).formatted(Formatting.GRAY));
    }
}
