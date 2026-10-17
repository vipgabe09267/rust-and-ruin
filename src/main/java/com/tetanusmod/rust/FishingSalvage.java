package com.tetanusmod.rust;
import com.tetanusmod.registry.*;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.*;
public final class FishingSalvage {
    public static boolean breakRod(ItemEntity rod,float roll) {
        if(rod.getWorld().isClient||!rod.isAlive()||!rod.getStack().isOf(Items.FISHING_ROD))return false;
        int count=rod.getStack().getCount();
        rod.discard();
        spawn(rod,new ItemStack(Items.STICK,2*count));
        spawn(rod,new ItemStack(Items.STRING,2*count));
        boolean hook=roll<0.15F;
        if(hook){spawn(rod,new ItemStack(ModItems.RUSTY_FISHING_HOOK));ModAdvancements.fishingHook(rod);}
        RustingSystem.rustFeedback(rod.getWorld(),rod.getPos());
        return hook;
    }
    private static void spawn(ItemEntity source,ItemStack stack) {
        ItemEntity drop=new ItemEntity(source.getWorld(),source.getX(),source.getY(),source.getZ(),stack);
        drop.setToDefaultPickupDelay();
        var thrower=((com.tetanusmod.mixin.ItemEntityAccessor)source).rustruin$getThrower();
        if(thrower!=null)drop.setThrower(thrower);
        source.getWorld().spawnEntity(drop);
    }
}
