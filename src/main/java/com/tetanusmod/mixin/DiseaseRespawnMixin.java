package com.tetanusmod.mixin;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.*;
import com.tetanusmod.expansion.*;
import net.minecraft.entity.effect.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The End return recreates a living player; that must not act as an unearned disease cure. */
@Mixin(ServerPlayerEntity.class)
public abstract class DiseaseRespawnMixin {
    @Inject(method="copyFrom",at=@At("TAIL"))
    private void diseaseProgress(ServerPlayerEntity old,boolean alive,CallbackInfo ci){
        if(!alive)return;
        var self=(ServerPlayerEntity)(Object)this;
        Infection.silently(()->{
            for(var type:new StatusEffect[]{TetanusMod.TETANUS,TetanusMod.LOCKJAW,TetanusMod.SALMONELLA,ExpansionEffects.RABIES,ExpansionEffects.DISORIENTED}){
                var previous=old.getStatusEffect(type);
                if(previous==null||previous.getDuration()==0)continue;
                // Native NBT cloning also retains hidden weaker effects and particle/icon flags.
                var copy=StatusEffectInstance.fromNbt(previous.writeNbt(new NbtCompound()));
                if(copy!=null)self.setStatusEffect(copy,null);
            }
        });
        if(self.hasStatusEffect(TetanusMod.TETANUS)){
            var before=(InfectionState)old;var after=(InfectionState)self;
            after.tetanus$setInfection(before.tetanus$getElapsed(),before.tetanus$getTotal());
            after.tetanus$setLockjawWindow(before.tetanus$getLockjawWindow());after.tetanus$setLockjaw(before.tetanus$getLockjaw());after.tetanus$setFreeze(before.tetanus$getFreeze());
        }
        var before=(ExpansionState)old;var after=(ExpansionState)self;
        if(self.hasStatusEffect(ExpansionEffects.RABIES)){
            after.rustruin$setRabiesTicks(before.rustruin$getRabiesTicks());after.rustruin$setLostHealth(before.rustruin$getLostHealth());RabiesEffect.restorePenalty(self);
        }
        if(self.hasStatusEffect(ExpansionEffects.DISORIENTED))after.rustruin$setDisorientedTicks(before.rustruin$getDisorientedTicks());
    }
}