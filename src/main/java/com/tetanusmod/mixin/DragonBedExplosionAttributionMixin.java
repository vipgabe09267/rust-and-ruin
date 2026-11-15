package com.tetanusmod.mixin;

import com.tetanusmod.dragon.DragonExplosionAttribution;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.explosion.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(BedBlock.class)
public abstract class DragonBedExplosionAttributionMixin {
    @Redirect(method="onUse",at=@At(value="INVOKE",target="Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/world/explosion/ExplosionBehavior;Lnet/minecraft/util/math/Vec3d;FZLnet/minecraft/world/World$ExplosionSourceType;)Lnet/minecraft/world/explosion/Explosion;"))
    private Explosion rustruin$bedActor(World target,Entity entity,DamageSource source,ExplosionBehavior behavior,Vec3d point,float power,boolean fire,World.ExplosionSourceType type,
            BlockState state,World world,BlockPos pos,PlayerEntity player,Hand hand,BlockHitResult hit) {
        return DragonExplosionAttribution.run(player,target,source,()->target.createExplosion(entity,source,behavior,point,power,fire,type));
    }
}
