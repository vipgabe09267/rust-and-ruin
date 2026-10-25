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
    public static boolean silent(){return SILENT.get();}
    public static void silently(Runnable action) {
        boolean previous=SILENT.get();SILENT.set(true);
        try{action.run();}finally{if(previous)SILENT.set(true);else SILENT.remove();}
    }
    public static void at(net.minecraft.world.World world,net.minecraft.util.math.Vec3d pos,net.minecraft.sound.SoundCategory category,Runnable action) {
        Origin previous=ORIGIN.get();ORIGIN.set(new Origin(world,pos,category));
        try{action.run();}finally{if(previous==null)ORIGIN.remove();else ORIGIN.set(previous);}
    }
    public static void playAt(net.minecraft.world.World world,net.minecraft.util.math.Vec3d pos,net.minecraft.sound.SoundCategory category) {
        if(world.isClient||SILENT.get())return;
        // Vanilla clamps the mixer gain to one, but uses gain above one to extend
        // the delivery/attenuation radius. Keep the same local loudness without
        // broadcasting a voice forty blocks away at the legacy default of2.5.
        float configured=TetanusMod.CONFIG.infectionSoundVolume;
        float volume=Float.isFinite(configured)?Math.max(0,Math.min(1,configured)):1;
        if(volume<=0)return;
        world.playSound(null,pos.x,pos.y,pos.z,TetanusMod.INFECTION_SOUND,category,volume,1);
    }
    public static void playSound(LivingEntity target) {
        if(SILENT.get()||target.getWorld().isClient||!target.hasStatusEffect(TetanusMod.TETANUS))return;
        Origin origin=ORIGIN.get();
        if(origin!=null){playAt(origin.world(),origin.pos(),origin.category());return;}
        // Players and mobs share the native positional world-sound path. Nearby
        // listeners hear the actual victim, not a private or shooter-centered cue.
        playAt(target.getWorld(),target.getPos(),target.getSoundCategory());
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
        return applyDurationChecked(target,source,ticks,0);
    }
    public static boolean applyDurationChecked(LivingEntity target,Entity source,int duration,int amplifier) {
        boolean permanent=com.tetanusmod.entity.MobVariants.permanent(target);
        int ticks=permanent?-1:Math.max(20,Math.min(90000,duration));
        var current=target.getStatusEffect(TetanusMod.TETANUS);
        if(current!=null) {
            // A weaker or shorter exposure cannot cure an advanced infection or restart Lockjaw.
            boolean weaker=current.getAmplifier()>amplifier || duration<TetanusMod.CONFIG.minDurationSeconds*20;
            if(current.getAmplifier()>=amplifier&&(weaker||current.getDuration()==-1))return true;
            if(current.getDuration()==-1)ticks=-1;
            else if(ticks!=-1)ticks=Math.max(ticks,current.getDuration());
            amplifier=Math.max(amplifier,current.getAmplifier());
        }
        target.removeStatusEffect(TetanusMod.TETANUS);
        target.removeStatusEffect(TetanusMod.LOCKJAW);
        var replacement=new StatusEffectInstance(TetanusMod.TETANUS,ticks,amplifier);
        if (addReplacement(target,source,replacement,current!=null)) {
            InfectionState state = (InfectionState) target;
            state.tetanus$setInfection(0, ticks < 0 ? 6000 : ticks);
            state.tetanus$setLockjaw(0);
            state.tetanus$setFreeze(0);
            return true;
        }
        return false;
    }

    /** Refreshes are real effect updates, not a new infection announcement. */
    private static boolean addReplacement(LivingEntity target,Entity source,StatusEffectInstance effect,boolean refresh) {
        boolean previous=SILENT.get();
        if(refresh)SILENT.set(true);
        try{return InfectionProtection.checked(target,()->target.addStatusEffect(effect,source));}
        finally{if(previous)SILENT.set(true);else SILENT.remove();}
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
