package com.tetanusmod.registry;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.entity.MobVariants;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ModAdvancements {
    public static void award(ServerPlayerEntity player, String id, String criterion) {
        var advancement = player.getServer().getAdvancementLoader().get(TetanusMod.id(id));
        if (advancement == null) return;
        // Also reveal the tab before the first native advancement tick has run.
        var root = player.getServer().getAdvancementLoader().get(TetanusMod.id("root"));
        if (root != null) player.getAdvancementTracker().grantCriterion(root, "obtain_iron");
        player.getAdvancementTracker().grantCriterion(advancement, criterion);
    }

    public static void converted(ServerPlayerEntity player, LivingEntity target) {
        if (!MobVariants.permanent(target)) return;
        award(player, "tetanized_mob", "convert_mob");
        // Projectile conversion and using Rust must award the same specific achievement.
        if (target instanceof IronGolemEntity) award(player, "rusty_guardian", "rust_golem");
    }

    public static void vaccinated(LivingEntity target) {
        if (target instanceof ServerPlayerEntity player) award(player, "vaccinated", "drink_vaccine");
    }

    public static void fishingHook(ItemEntity item) {
        var thrower = ((com.tetanusmod.mixin.ItemEntityAccessor) item).rustruin$getThrower();
        var player = thrower != null ? item.getWorld().getPlayerByUuid(thrower) : item.getWorld().getClosestPlayer(item, 16);
        if (player instanceof ServerPlayerEntity serverPlayer) award(serverPlayer, "rusty_hook", "find_hook");
    }

    public static void rusted(ItemEntity item) {
        var thrower = ((com.tetanusmod.mixin.ItemEntityAccessor) item).rustruin$getThrower();
        var player = thrower != null ? item.getWorld().getPlayerByUuid(thrower) : item.getWorld().getClosestPlayer(item, 16);
        if (player instanceof ServerPlayerEntity serverPlayer) award(serverPlayer, "rusty_business", "rust_a_nail");
    }
}
