package com.tetanusmod.registry;
import com.tetanusmod.TetanusMod;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
public final class ModAdvancements {
    public static void award(ServerPlayerEntity player,String id,String criterion) {
        var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id(id));
        if(advancement!=null)player.getAdvancementTracker().grantCriterion(advancement,criterion);
    }
    public static void fishingHook(ItemEntity item) {
        var thrower=((com.tetanusmod.mixin.ItemEntityAccessor)item).rustruin$getThrower();
        var player=thrower!=null?item.getWorld().getPlayerByUuid(thrower):item.getWorld().getClosestPlayer(item,16);
        if(player instanceof ServerPlayerEntity serverPlayer)award(serverPlayer,"rusty_hook","find_hook");
    }
    public static void rusted(ItemEntity item) {
        var thrower = ((com.tetanusmod.mixin.ItemEntityAccessor)item).rustruin$getThrower();
        var closest = thrower != null ? item.getWorld().getPlayerByUuid(thrower) : item.getWorld().getClosestPlayer(item,16);
        if (closest instanceof ServerPlayerEntity player) {
            var advancement = player.getServer().getAdvancementLoader().get(TetanusMod.id("rusty_business"));
            if (advancement != null) player.getAdvancementTracker().grantCriterion(advancement,"rust_a_nail");
        }
    }
}
