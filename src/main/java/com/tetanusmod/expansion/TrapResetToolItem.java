package com.tetanusmod.expansion;

import com.tetanusmod.item.DescribedItem;
import com.tetanusmod.registry.ModAdvancements;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class TrapResetToolItem extends DescribedItem {
    public TrapResetToolItem(Settings settings) {
        super(settings, "trap_reset_tool");
        // This registered item is constructed once. A captive's body normally
        // intercepts clicks on the jaws, so handle that native entity interaction
        // before ordinary cow/villager/player interactions consume the click.
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!player.getStackInHand(hand).isOf(this) || player.isSpectator()) return ActionResult.PASS;
            return resetCaptive(player, hand, entity);
        });
    }

    @Override public ActionResult useOnBlock(ItemUsageContext context) {
        var player = context.getPlayer();
        if (player == null || !(context.getWorld().getBlockEntity(context.getBlockPos()) instanceof BearTrapBlockEntity trap)) return ActionResult.PASS;
        return reset(player, context.getHand(), trap);
    }

    private ActionResult resetCaptive(PlayerEntity player, Hand hand, Entity target) {
        for (var pos : BlockPos.iterateOutwards(target.getBlockPos(), 1, 1, 1)) {
            if (!(player.getWorld().getBlockEntity(pos) instanceof BearTrapBlockEntity trap) || !trap.holds(target)) continue;
            // The entity packet's reach check also runs on the server. Restrict
            // the corresponding block action and respect solid walls/protection.
            if (player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) > 36) return ActionResult.PASS;
            var visible = player.getWorld().raycast(new RaycastContext(player.getEyePos(),
                    Vec3d.of(pos).add(0.5, 0.2, 0.5), RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE, player));
            if (visible.getType() != HitResult.Type.BLOCK || !visible.getBlockPos().equals(pos)) return ActionResult.PASS;
            return reset(player, hand, trap);
        }
        return ActionResult.PASS;
    }

    private ActionResult reset(PlayerEntity player, Hand hand, BearTrapBlockEntity trap) {
        if (player.isSpectator() || !player.canModifyBlocks()
                || !player.getWorld().canPlayerModifyAt(player, trap.getPos())
                || !trap.getCachedState().get(BearTrapBlock.CLOSED)) return ActionResult.PASS;
        if (player.getWorld().isClient) return ActionResult.SUCCESS;
        if (!trap.reset()) return ActionResult.PASS;
        player.getStackInHand(hand).damage(1, player, p -> p.sendToolBreakStatus(hand));
        if (player instanceof ServerPlayerEntity serverPlayer) ModAdvancements.award(serverPlayer, "trap_reset", "reset_trap");
        player.sendMessage(Text.translatable("message.tetanusmod.trap_reset"), true);
        return ActionResult.CONSUME;
    }
}
