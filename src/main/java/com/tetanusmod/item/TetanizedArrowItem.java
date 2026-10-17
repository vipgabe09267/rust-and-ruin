package com.tetanusmod.item;
import com.tetanusmod.entity.TetanizedArrowEntity;
import net.minecraft.item.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.world.World;
public final class TetanizedArrowItem extends ArrowItem {
    public TetanizedArrowItem(Settings settings) { super(settings); }
    @Override public PersistentProjectileEntity createArrow(World world, ItemStack stack, LivingEntity shooter) {
        return new TetanizedArrowEntity(world,shooter);
    }
}
