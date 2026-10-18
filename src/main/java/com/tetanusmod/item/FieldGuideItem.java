package com.tetanusmod.item;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.world.World;
public final class FieldGuideItem extends DescribedItem {
    public static Runnable open=()->{};
    public FieldGuideItem(Settings settings){super(settings,"field_guide");}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand) {
        if(world.isClient)open.run();
        return TypedActionResult.success(player.getStackInHand(hand),world.isClient);
    }
}