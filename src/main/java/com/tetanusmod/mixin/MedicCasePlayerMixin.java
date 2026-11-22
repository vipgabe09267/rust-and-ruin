package com.tetanusmod.mixin;

import com.tetanusmod.expansion.MedicCases;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MedicCasePlayerMixin implements MedicCases.State {
    @Unique private final MedicCases.Progress rustruin$caseProgress = new MedicCases.Progress();

    @Override
    public MedicCases.Progress rustruin$medicCase() {
        return rustruin$caseProgress;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void saveMedicCase(NbtCompound nbt, CallbackInfo ci) {
        rustruin$caseProgress.write(nbt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void loadMedicCase(NbtCompound nbt, CallbackInfo ci) {
        rustruin$caseProgress.read(nbt);
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void copyMedicCase(ServerPlayerEntity old, boolean alive, CallbackInfo ci) {
        NbtCompound data = new NbtCompound();
        MedicCases.progress(old).write(data);
        rustruin$caseProgress.read(data);
    }
}
