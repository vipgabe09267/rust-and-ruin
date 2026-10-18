package com.tetanusmod.mixin;

import com.tetanusmod.beacon.*;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
    @Inject(method="updateLevel",at=@At("HEAD"),cancellable=true)
    private static void base(World world,int x,int y,int z,CallbackInfoReturnable<Integer> cir) {
        var pos=new BlockPos(x,y,z);
        if(world.getBlockState(pos).isOf(ModBeacons.BLOCK))cir.setReturnValue(BeaconPower.level(world,pos));
    }
    @Inject(method="applyPlayerEffects",at=@At("HEAD"),cancellable=true)
    private static void powers(World world,BlockPos pos,int level,StatusEffect primary,StatusEffect secondary,CallbackInfo ci) {
        if(world.getBlockState(pos).isOf(ModBeacons.BLOCK)) { BeaconPower.apply(world,pos,level,primary,secondary);ci.cancel(); }
    }
}