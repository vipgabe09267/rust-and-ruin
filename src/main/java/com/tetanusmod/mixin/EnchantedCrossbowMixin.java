package com.tetanusmod.mixin;

import com.tetanusmod.expansion.CombatInfection;
import net.minecraft.entity.*;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(CrossbowItem.class)
public abstract class EnchantedCrossbowMixin {
    @Redirect(method="shoot",at=@At(value="INVOKE",target="Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private static boolean snapshot(World world,Entity projectile,World unused,LivingEntity shooter,Hand hand,ItemStack weapon,ItemStack ammo,float pitch,boolean creative,float speed,float divergence,float angle){CombatInfection.prepare(projectile,weapon);return world.spawnEntity(projectile);}
}