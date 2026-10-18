package com.tetanusmod.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.*;

public final class LockjawEffect extends StatusEffect {
    public static void flush(LivingEntity entity) {
        if(entity.getWorld().isClient)return;
        int remaining=((InfectionState)entity).tetanus$getLockjaw();
        var current=entity.getStatusEffect(com.tetanusmod.TetanusMod.LOCKJAW);
        if(remaining>0 && entity.hasStatusEffect(com.tetanusmod.TetanusMod.TETANUS) && (current==null||current.getDuration()<remaining-1)) {
            entity.addStatusEffect(new StatusEffectInstance(com.tetanusmod.TetanusMod.LOCKJAW,remaining,0,false,true,true));
            if(current==null && entity instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)com.tetanusmod.registry.ModAdvancements.award(serverPlayer,"lockjaw_sonata","get_lockjaw");
            if(current==null && entity instanceof net.minecraft.entity.player.PlayerEntity player)player.sendMessage(net.minecraft.text.Text.translatable("message.tetanusmod.lockjaw"),true);
        }
    }
    public LockjawEffect() { super(StatusEffectCategory.HARMFUL,0xBC7847); }
    @Override public boolean canApplyUpdateEffect(int duration,int amplifier) { return true; }
    @Override public void applyUpdateEffect(LivingEntity entity,int amplifier) {
        if(!entity.getWorld().isClient && entity.isUsingItem() && entity.getActiveItem().isFood())entity.stopUsingItem();
    }
}