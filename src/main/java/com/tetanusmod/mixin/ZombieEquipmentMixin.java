package com.tetanusmod.mixin;

import com.tetanusmod.expansion.MobEquipment;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieEntity.class)
public abstract class ZombieEquipmentMixin {
    @Inject(method="initialize",at=@At("RETURN"))
    private void spawnGear(ServerWorldAccess world,LocalDifficulty difficulty,SpawnReason reason,EntityData data,NbtCompound nbt,CallbackInfoReturnable<EntityData> result){MobEquipment.spawned((ZombieEntity)(Object)this,reason);}
}