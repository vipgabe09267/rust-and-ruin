package com.tetanusmod.mixin;

import com.tetanusmod.expansion.CombatInfection;
import net.minecraft.entity.*;
import net.minecraft.item.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin({BowItem.class,TridentItem.class})
public abstract class EnchantedBowMixin {
    @Redirect(method="onStoppedUsing",at=@At(value="INVOKE",target="Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean snapshot(World world,Entity projectile,ItemStack weapon,World unused,LivingEntity shooter,int remaining){CombatInfection.prepare(projectile,weapon);return world.spawnEntity(projectile);}
}