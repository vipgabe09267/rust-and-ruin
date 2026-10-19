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
    @Override public void appendTooltip(ItemStack stack,World world,java.util.List<net.minecraft.text.Text> lines,net.minecraft.client.item.TooltipContext context){
        lines.add(net.minecraft.text.Text.translatable("tooltip.tetanusmod.arrow").formatted(net.minecraft.util.Formatting.DARK_RED));
    }}
