package com.tetanusmod.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import com.tetanusmod.TetanusMod;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import java.util.UUID;

public final class TetanusEffect extends StatusEffect {
    private static final UUID SPEED_ID = UUID.fromString("af68b7d6-1c7f-47cf-9405-58eea7042da7");
    public TetanusEffect() {
        super(StatusEffectCategory.HARMFUL, 0x9B5635);
        addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED,
                SPEED_ID.toString(), -0.15, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) { return true; }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity.getWorld().isClient) return;
        InfectionState state = (InfectionState) entity;
        var effect = entity.getStatusEffect(this);
        if (effect == null) return;
        if (state.tetanus$getTotal() <= 0) state.tetanus$setInfection(0, effect.getDuration() < 0 ? 6000 : effect.getDuration());
        int elapsed = state.tetanus$getElapsed();
        int stage = Infection.stage(elapsed, state.tetanus$getTotal());
        state.tetanus$setInfection(elapsed + 1, state.tetanus$getTotal());
        if (!(entity instanceof net.minecraft.entity.player.PlayerEntity) && elapsed > 0 && elapsed % 200 == 0) {
            float floor = com.tetanusmod.entity.MobVariants.permanent(entity) && !com.tetanusmod.entity.MobVariants.isAnimal(entity)
                    ? entity.getMaxHealth() * 0.5F : 1.0F;
            float damage = Math.min(1.0F, Math.max(0,entity.getHealth()-floor));
            if (damage > 0) entity.damage(entity.getDamageSources().magic(), damage);
        }
        var speed = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        double slowdown = stage == 2 ? -0.30 : -0.15;
        if (speed != null && (speed.getModifier(SPEED_ID) == null || speed.getModifier(SPEED_ID).getValue() != slowdown)) {
            speed.removeModifier(SPEED_ID);
            speed.addTemporaryModifier(new EntityAttributeModifier(SPEED_ID, "Tetanus stiffness", slowdown,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if (state.tetanus$getLockjaw() > 0) {
            state.tetanus$setLockjaw(state.tetanus$getLockjaw() - 1);
            if (entity.isUsingItem() && entity.getActiveItem().isFood()) entity.stopUsingItem();
        }
        if (state.tetanus$getFreeze() > 0) {
            state.tetanus$setFreeze(state.tetanus$getFreeze() - 1);
            entity.setVelocity(0, entity.getVelocity().y, 0);
            entity.velocityModified = true;
        }
        // One continuous, persisted 2-4 minute final phase, independent of random spasms.
        if(state.tetanus$getLockjawWindow()<=0)state.tetanus$setLockjawWindow((120+entity.getRandom().nextInt(121))*20);
        int remaining=Math.max(0,state.tetanus$getTotal()-elapsed);
        if(entity instanceof net.minecraft.entity.player.PlayerEntity && remaining>0 && remaining<=state.tetanus$getLockjawWindow()) {
            state.tetanus$setLockjaw(remaining);
            if(entity.isUsingItem()&&entity.getActiveItem().isFood())entity.stopUsingItem();
        }
        // Randomized bursts, rolled once per second, keep symptoms intermittent.
        if (elapsed % 20 != 0 || entity.getRandom().nextDouble()
                >= Math.min(0.8, (0.10 + stage * 0.10) * TetanusMod.CONFIG.spasmFrequency)) return;
        Infection.twitch(entity, stage);
        entity.swingHand(Hand.MAIN_HAND);
        if (stage >= 1) {
            if (!entity.hasVehicle()) {
                double angle = entity.getRandom().nextDouble() * Math.PI * 2;
                entity.addVelocity(Math.cos(angle) * 0.06, 0, Math.sin(angle) * 0.06);
                entity.velocityModified = true;
                state.tetanus$setFreeze(stage == 2 ? 4 : 2);
            }
            entity.getWorld().playSound(null, entity.getBlockPos(), TetanusMod.SPASM_SOUND,
                    SoundCategory.PLAYERS, 0.15F, 0.8F + entity.getRandom().nextFloat() * 0.2F);
        }
        if (stage == 2 && entity instanceof net.minecraft.entity.player.PlayerEntity) {

            if (entity.getRandom().nextFloat() < 0.25F) {
                float damage = TetanusMod.CONFIG.periodicDamage;
                if (!TetanusMod.CONFIG.tetanusCanKill) damage = Math.min(damage, Math.max(0, entity.getHealth() - 1.0F));
                if (damage > 0) entity.damage(entity.getDamageSources().magic(), damage);
            }
        }
    }
}
