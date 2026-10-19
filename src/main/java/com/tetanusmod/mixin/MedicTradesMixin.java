package com.tetanusmod.mixin;
import com.tetanusmod.expansion.MedicVillage;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(VillagerEntity.class)
public abstract class MedicTradesMixin {
    @Inject(method="fillRecipes",at=@At("TAIL"))private void medicines(CallbackInfo ci){MedicVillage.ensureStarterTrades((VillagerEntity)(Object)this);}
}