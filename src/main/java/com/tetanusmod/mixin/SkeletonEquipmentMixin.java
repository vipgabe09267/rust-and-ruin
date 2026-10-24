package com.tetanusmod.mixin;

import com.tetanusmod.expansion.MobEquipment;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeletonEntity.class)
public abstract class SkeletonEquipmentMixin {
    @Inject(method="initialize",at=@At("RETURN"))
    private void spawnGear(net.minecraft.world.ServerWorldAccess world,net.minecraft.world.LocalDifficulty difficulty,net.minecraft.entity.SpawnReason reason,net.minecraft.entity.EntityData data,net.minecraft.nbt.NbtCompound nbt,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.entity.EntityData> result){MobEquipment.spawned((AbstractSkeletonEntity)(Object)this,reason);}
    @Inject(method="updateAttackType",at=@At("TAIL"))
    private void rustyNails(CallbackInfo ci){MobEquipment.refreshSkeleton((AbstractSkeletonEntity)(Object)this);}
}
