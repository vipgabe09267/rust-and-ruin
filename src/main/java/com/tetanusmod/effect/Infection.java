package com.tetanusmod.effect;

import com.tetanusmod.TetanusMod;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class Infection {
    private static final ThreadLocal<Boolean> MELEE_SUPPRESSED=ThreadLocal.withInitial(()->false);
    public static boolean meleeSuppressed(){return MELEE_SUPPRESSED.get();}
    /** Active items that handle their own exposure must not also inherit main-hand melee infection. */
    public static boolean withoutMeleeExposure(java.util.function.BooleanSupplier damage) {
        boolean previous=MELEE_SUPPRESSED.get();MELEE_SUPPRESSED.set(true);
        try{return damage.getAsBoolean();}finally{if(previous)MELEE_SUPPRESSED.set(true);else MELEE_SUPPRESSED.remove();}
    }
    private static final ThreadLocal<Boolean> SILENT=ThreadLocal.withInitial(()->false);
    private static final ThreadLocal<Origin> ORIGIN=new ThreadLocal<>();
    private record Origin(net.minecraft.world.World world,net.minecraft.util.math.Vec3d pos,net.minecraft.sound.SoundCategory category){}
    public static void silently(Runnable action) {
        boolean previous=SILENT.get();SILENT.set(true);
        try{action.run();}finally{SILENT.set(previous);}
    }
    public static void at(net.minecraft.world.World world,net.minecraft.util.math.Vec3d pos,net.minecraft.sound.SoundCategory category,Runnable action) {
        Origin previous=ORIGIN.get();ORIGIN.set(new Origin(world,pos,category));
        try{action.run();}finally{if(previous==null)ORIGIN.remove();else ORIGIN.set(previous);}
    }
    public static void playAt(net.minecraft.world.World world,net.minecraft.util.math.Vec3d pos,net.minecraft.sound.SoundCategory category) {
        world.playSound(null,pos.x,pos.y,pos.z,TetanusMod.INFECTION_SOUND,category,TetanusMod.CONFIG.infectionSoundVolume,1);
    }
    public static void playSound(LivingEntity target) {
        if(SILENT.get())return;
        Origin origin=ORIGIN.get();
        if(origin!=null){playAt(origin.world(),origin.pos(),origin.category());return;}
        float volume = TetanusMod.CONFIG.infectionSoundVolume;
        if (target instanceof ServerPlayerEntity player) player.playSound(TetanusMod.INFECTION_SOUND,
                net.minecraft.sound.SoundCategory.PLAYERS, volume, 1);
        else target.getWorld().playSound(null,target.getX(),target.getY(),target.getZ(),TetanusMod.INFECTION_SOUND,
                target.getSoundCategory(),volume,1);
    }
    public static final Identifier SYMPTOM_PACKET = TetanusMod.id("symptom");

    public static void expose(LivingEntity target, Entity source) {
        if(target.getWorld().isClient||!target.isAlive()||!InfectionProtection.allows(target,true))return;
        applyChecked(target,source);
        com.tetanusmod.entity.MobVariants.projectileExposure(target,source);
    }
    public static StatusEffectInstance potionEffect(StatusEffectInstance effect) {
        if(effect.getEffectType()!=com.tetanusmod.TetanusMod.TETANUS)return effect;
        int seconds=net.minecraft.util.math.random.Random.create().nextBetween(com.tetanusmod.TetanusMod.CONFIG.minDurationSeconds,com.tetanusmod.TetanusMod.CONFIG.maxDurationSeconds);
        return new StatusEffectInstance(effect.getEffectType(),seconds*20,effect.getAmplifier(),effect.isAmbient(),effect.shouldShowParticles(),effect.shouldShowIcon());
    }
    public static boolean apply(LivingEntity target,Entity source){
        if(target.getWorld().isClient||!target.isAlive()||!InfectionProtection.allows(target,false))return false;
        return applyChecked(target,source);
    }
    private static boolean applyChecked(LivingEntity target, Entity source) {

        int min = TetanusMod.CONFIG.minDurationSeconds;
        int max = TetanusMod.CONFIG.maxDurationSeconds;
        boolean permanent=com.tetanusmod.entity.MobVariants.permanent(target);
        int ticks = permanent ? -1 : (min + target.getRandom().nextInt(max - min + 1)) * 20;
        target.removeStatusEffect(TetanusMod.TETANUS);
        target.removeStatusEffect(TetanusMod.LOCKJAW);
        if (InfectionProtection.checked(target,()->target.addStatusEffect(new StatusEffectInstance(TetanusMod.TETANUS, ticks, 0), source))) {
            InfectionState state = (InfectionState) target;
            state.tetanus$setInfection(0, permanent ? 6000 : ticks);
            state.tetanus$setLockjaw(0);
            state.tetanus$setFreeze(0);
            return true;
        }
        return false;
    }

    public static int stage(int elapsed, int total) {
        return Math.min(2, (int) ((long) Math.max(0, elapsed) * 3 / Math.max(1, total)));
    }

    public static void twitch(LivingEntity target, int stage) {
        if (target instanceof ServerPlayerEntity player && ServerPlayNetworking.canSend(player, SYMPTOM_PACKET)) {
            var packet = PacketByteBufs.create();
            packet.writeByte(stage);
            packet.writeBoolean(TetanusMod.CONFIG.cameraShaking);
            ServerPlayNetworking.send(player, SYMPTOM_PACKET, packet);
        }
    }
}
