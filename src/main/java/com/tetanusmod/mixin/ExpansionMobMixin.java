package com.tetanusmod.mixin;

import com.tetanusmod.expansion.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(MobEntity.class)
public abstract class ExpansionMobMixin implements Rabies.MobGoals {
    @Shadow protected GoalSelector goalSelector;
    public GoalSelector rustruin$goals(){return goalSelector;}
    @Inject(method="initialize",at=@At("RETURN"))private void natural(ServerWorldAccess world,LocalDifficulty difficulty,SpawnReason reason,EntityData data,NbtCompound nbt,CallbackInfoReturnable<EntityData> result){NaturalVariants.spawned((MobEntity)(Object)this,reason);}
    @Inject(method="tryAttack",at=@At("HEAD"),cancellable=true)private void miss(Entity target,CallbackInfoReturnable<Boolean> result){var self=(MobEntity)(Object)this;if(self.hasStatusEffect(ExpansionEffects.DISORIENTED)&&self.getRandom().nextFloat()<0.45F)result.setReturnValue(false);}
}