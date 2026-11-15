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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(RespawnAnchorBlock.class)
public abstract class DragonAnchorExplosionAttributionMixin {
    @Shadow private void explode(BlockState state,World world,BlockPos pos) { throw new AssertionError("Shadow method"); }
    @Redirect(method="onUse",at=@At(value="INVOKE",target="Lnet/minecraft/block/RespawnAnchorBlock;explode(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"))
    private void rustruin$anchorActor(RespawnAnchorBlock anchor,BlockState state,World world,BlockPos pos,
            BlockState originalState,World originalWorld,BlockPos originalPos,PlayerEntity player,Hand hand,BlockHitResult hit) {
        DragonExplosionAttribution.run(player,world,null,()->{explode(state,world,pos);return null;});
    }
    @Redirect(method="explode",at=@At(value="INVOKE",target="Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/world/explosion/ExplosionBehavior;Lnet/minecraft/util/math/Vec3d;FZLnet/minecraft/world/World$ExplosionSourceType;)Lnet/minecraft/world/explosion/Explosion;"))
    private Explosion rustruin$anchorSource(World world,Entity entity,DamageSource source,ExplosionBehavior behavior,Vec3d point,float power,boolean fire,World.ExplosionSourceType type) {
        return DragonExplosionAttribution.runCurrent(world,source,()->world.createExplosion(entity,source,behavior,point,power,fire,type));
    }
}
