package com.tetanusmod.mixin;

import com.tetanusmod.expansion.MedicSupplies;
import com.tetanusmod.expansion.MedicVillage;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public abstract class MedicTradesMixin implements MedicSupplies.State {
    @Unique private long rustruin$supplyDay = Long.MIN_VALUE;
    public long rustruin$lastSupplyDay() { return rustruin$supplyDay; }
    public void rustruin$lastSupplyDay(long day) { rustruin$supplyDay = day; }
    @Inject(method="fillRecipes",at=@At("TAIL"))
    private void medicines(CallbackInfo ci) { MedicVillage.ensureStarterTrades((VillagerEntity)(Object)this); }
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))
    private void saveSupplies(NbtCompound nbt, CallbackInfo ci) { nbt.putLong("RustRuinSupplyDay", rustruin$supplyDay); }
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL"))
    private void loadSupplies(NbtCompound nbt, CallbackInfo ci) {
        rustruin$supplyDay = nbt.contains("RustRuinSupplyDay", net.minecraft.nbt.NbtElement.LONG_TYPE)
                ? nbt.getLong("RustRuinSupplyDay") : Long.MIN_VALUE;
    }
}