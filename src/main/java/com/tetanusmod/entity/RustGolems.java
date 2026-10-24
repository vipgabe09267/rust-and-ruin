package com.tetanusmod.entity;

import com.tetanusmod.registry.*;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.*;

public final class RustGolems {
    public static final float CHANCE=0.18F;
    public static void initialize() {
        UseEntityCallback.EVENT.register((player,world,hand,entity,hit)->{
            if(player.isSpectator() || !(entity instanceof IronGolemEntity golem)||!player.getStackInHand(hand).isOf(ModItems.RUST)||MobVariants.permanent(golem))return ActionResult.PASS;
            if(!world.isClient)attempt(player,hand,golem,golem.getRandom().nextFloat());
            return ActionResult.success(world.isClient);
        });
    }
    public static boolean attempt(PlayerEntity player,Hand hand,IronGolemEntity golem,float roll) {
        var stack=player.getStackInHand(hand);
        if(player.isSpectator()||golem.getWorld().isClient||!golem.isAlive()||MobVariants.permanent(golem)||!stack.isOf(ModItems.RUST))return false;
        if(!player.getAbilities().creativeMode)stack.decrement(1);
        com.tetanusmod.rust.RustingSystem.rustFeedback(golem.getWorld(),golem.getPos());
        if(roll>=CHANCE){player.sendMessage(net.minecraft.text.Text.translatable("message.tetanusmod.golem_failed"),true);return false;}
        MobVariants.makePermanent(golem);player.sendMessage(net.minecraft.text.Text.translatable("message.tetanusmod.golem_success"),true);
        if(player instanceof ServerPlayerEntity serverPlayer) {
            ModAdvancements.award(serverPlayer,"rusty_guardian","rust_golem");
            ModAdvancements.award(serverPlayer,"tetanized_mob","convert_mob");
        }
        return true;
    }
}