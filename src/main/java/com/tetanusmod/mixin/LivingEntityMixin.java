package com.tetanusmod.mixin;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.InfectionState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements InfectionState, com.tetanusmod.entity.TetanizedState {
    @Inject(method="addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",at=@At("HEAD"),cancellable=true)
    private void rustruin$preventDisease(net.minecraft.entity.effect.StatusEffectInstance effect,net.minecraft.entity.Entity source,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> result) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(!self.getWorld().isClient && effect.getEffectType()==TetanusMod.TETANUS && !com.tetanusmod.effect.InfectionProtection.checked(self)
                && !com.tetanusmod.effect.InfectionProtection.allows(self,false))result.setReturnValue(false);
    }
    @org.spongepowered.asm.mixin.Shadow protected abstract void dropXp();
    @org.spongepowered.asm.mixin.Shadow protected net.minecraft.entity.player.PlayerEntity attackingPlayer;
    @Inject(method="drop",at=@At("HEAD"),cancellable=true)
    private void rustruin$infectedAnimalOnlyMeat(net.minecraft.entity.damage.DamageSource source,CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(com.tetanusmod.entity.MobVariants.infectedAnimal(self)) {
            com.tetanusmod.entity.MobVariants.drops(self);
            dropXp();
            ci.cancel();
        }
    }
    @Inject(method="onDeath",at=@At("TAIL"))
    private void rustruin$niceOne(net.minecraft.entity.damage.DamageSource source,CallbackInfo ci) {
        LivingEntity self=(LivingEntity)(Object)this;
        if(self instanceof net.minecraft.entity.boss.WitherEntity&&com.tetanusmod.entity.MobVariants.permanent(self)) {
            var killer=source.getAttacker() instanceof net.minecraft.server.network.ServerPlayerEntity player?player:attackingPlayer;
            if(killer instanceof net.minecraft.server.network.ServerPlayerEntity player)
                com.tetanusmod.registry.ModAdvancements.award(player,"nice_one","defeat_wither");
        }
    }
    @Unique private static final net.minecraft.entity.data.TrackedData<Boolean> RUST_RUIN_PERMANENT =
            net.minecraft.entity.data.DataTracker.registerData(LivingEntity.class, net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    @Unique private boolean rustruin$rolled;
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void rustruin$initTracker(CallbackInfo ci) {
        ((LivingEntity)(Object)this).getDataTracker().startTracking(RUST_RUIN_PERMANENT,false);
    }
    public boolean rustruin$isPermanent() { return ((LivingEntity)(Object)this).getDataTracker().get(RUST_RUIN_PERMANENT); }
    public void rustruin$setPermanent(boolean value) { ((LivingEntity)(Object)this).getDataTracker().set(RUST_RUIN_PERMANENT,value); }
    public boolean rustruin$hasRolled() { return rustruin$rolled; }
    public void rustruin$setRolled(boolean value) { rustruin$rolled=value; }

    @Inject(method = "damage", at = @At("RETURN"))
    private void rustruin$meleeInfection(net.minecraft.entity.damage.DamageSource source, float amount,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity)(Object)this;
        if (!cir.getReturnValue() || target.getWorld().isClient || amount <= 0 || com.tetanusmod.effect.Infection.meleeSuppressed()
                || source.isOf(net.minecraft.entity.damage.DamageTypes.THORNS)
                || !(source.getAttacker() instanceof LivingEntity attacker) || source.getSource() != attacker) return;
        var stack = attacker.getMainHandStack();
        boolean rusty = stack.getItem() instanceof com.tetanusmod.item.RebarItem rebar && rebar.isRusty();
        boolean enchanted = net.minecraft.enchantment.EnchantmentHelper.getLevel(com.tetanusmod.registry.ModItems.TETANUS_ENCHANTMENT,stack)>0;
        boolean tool = com.tetanusmod.registry.ModIronTools.isRusted(stack.getItem()) && target.getRandom().nextFloat()<0.2F;
        if (rusty || enchanted || tool) com.tetanusmod.effect.Infection.expose(target,attacker);
    }
    @Inject(method = "dropLoot", at = @At("TAIL"))
    private void rustruin$variantDrops(net.minecraft.entity.damage.DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
        com.tetanusmod.entity.MobVariants.drops((LivingEntity)(Object)this);
    }
    @Inject(method = "onStatusEffectApplied", at = @At("TAIL"))
    private void tetanus$infectionAudio(net.minecraft.entity.effect.StatusEffectInstance effect,
            net.minecraft.entity.Entity source, CallbackInfo ci) {
        tetanus$playInfectionAudio(effect);
    }

    @Inject(method = "onStatusEffectUpgraded", at = @At("TAIL"))
    private void tetanus$refreshedAudio(net.minecraft.entity.effect.StatusEffectInstance effect,
            boolean reapplyEffect, net.minecraft.entity.Entity source, CallbackInfo ci) {
        if (reapplyEffect) tetanus$playInfectionAudio(effect);
    }

    @Unique
    private void tetanus$playInfectionAudio(net.minecraft.entity.effect.StatusEffectInstance effect) {
        if (effect.getEffectType() != TetanusMod.TETANUS) return;
        if (!((LivingEntity) (Object) this).getWorld().isClient) {
            tetanus$elapsed = tetanus$lockjaw = tetanus$freeze = 0;
            tetanus$total = effect.getDuration();
            tetanus$lockjawWindow = (120 + ((LivingEntity)(Object)this).getRandom().nextInt(121)) * 20;
        }
        LivingEntity self=(LivingEntity)(Object)this;
        if (!self.getWorld().isClient && rustruin$lastAudioTick != self.getWorld().getTime()) {
            rustruin$lastAudioTick=self.getWorld().getTime();
            com.tetanusmod.effect.Infection.playSound(self);
        }
    }
    @Unique private long rustruin$lastAudioTick=Long.MIN_VALUE;

    @Unique private int tetanus$elapsed;
    @Unique private int tetanus$total;
    @Unique private int tetanus$lockjaw;
    @Unique private int tetanus$freeze;
    @Unique private int tetanus$lockjawWindow;
    public int tetanus$getLockjawWindow(){return tetanus$lockjawWindow;}
    public void tetanus$setLockjawWindow(int ticks){tetanus$lockjawWindow=ticks;}
    public int tetanus$getElapsed() { return tetanus$elapsed; }
    public int tetanus$getTotal() { return tetanus$total; }
    public int tetanus$getLockjaw() { return tetanus$lockjaw; }
    public int tetanus$getFreeze() { return tetanus$freeze; }
    public void tetanus$setInfection(int elapsed, int total) { tetanus$elapsed = elapsed; tetanus$total = total; }
    public void tetanus$setLockjaw(int ticks) { tetanus$lockjaw = ticks; }
    public void tetanus$setFreeze(int ticks) { tetanus$freeze = ticks; }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tetanus$clearRecoveredState(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        com.tetanusmod.entity.MobVariants.tick(self);
        com.tetanusmod.effect.LockjawEffect.flush(self);
        com.tetanusmod.effect.SalmonellaEffect.flush(self);
        if (!self.getWorld().isClient && !self.hasStatusEffect(TetanusMod.TETANUS)) {
            tetanus$elapsed = tetanus$total = tetanus$lockjaw = tetanus$freeze = tetanus$lockjawWindow = 0;
            self.removeStatusEffect(TetanusMod.LOCKJAW);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void tetanus$save(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("TetanusElapsed", tetanus$elapsed);
        nbt.putInt("TetanusTotal", tetanus$total);
        nbt.putInt("TetanusLockjaw", tetanus$lockjaw);
        nbt.putInt("TetanusLockjawWindow",tetanus$lockjawWindow);
        nbt.putInt("TetanusFreeze", tetanus$freeze);
        nbt.putBoolean("RustRuinPermanent",rustruin$isPermanent());
        nbt.putBoolean("RustRuinRolled",rustruin$rolled);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void tetanus$load(NbtCompound nbt, CallbackInfo ci) {
        tetanus$elapsed = Math.max(0, nbt.getInt("TetanusElapsed"));
        tetanus$total = Math.max(0, nbt.getInt("TetanusTotal"));
        tetanus$lockjaw = Math.max(0, Math.min(4800, nbt.getInt("TetanusLockjaw")));
        tetanus$lockjawWindow=Math.max(0,Math.min(4800,nbt.getInt("TetanusLockjawWindow")));
        tetanus$freeze = Math.max(0, Math.min(10, nbt.getInt("TetanusFreeze")));
        rustruin$setPermanent(nbt.getBoolean("RustRuinPermanent"));
        rustruin$rolled = nbt.getBoolean("RustRuinRolled");
    }
}
