package com.tetanusmod.dragon;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
public final class TetanizedEndCrystalEntity extends EndCrystalEntity {
    public TetanizedEndCrystalEntity(EntityType<? extends EndCrystalEntity> type,World world){super(type,world);}
    @Override public ItemStack getPickBlockStack(){return new ItemStack(DragonContent.CRYSTAL);}
}
