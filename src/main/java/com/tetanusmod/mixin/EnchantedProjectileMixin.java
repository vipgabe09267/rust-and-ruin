package com.tetanusmod.mixin;

import com.tetanusmod.expansion.CombatInfection;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public abstract class EnchantedProjectileMixin implements CombatInfection.RangedCarrier {
    @Unique private int tetanusLevel;
    public int rustruin$weaponLevel(){return tetanusLevel;}public void rustruin$weaponLevel(int level){tetanusLevel=Math.max(0,Math.min(2,level));}
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))private void save(NbtCompound nbt,CallbackInfo ci){nbt.putInt("RustRuinTetanusLevel",tetanusLevel);}
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL"))private void load(NbtCompound nbt,CallbackInfo ci){rustruin$weaponLevel(nbt.getInt("RustRuinTetanusLevel"));}
}