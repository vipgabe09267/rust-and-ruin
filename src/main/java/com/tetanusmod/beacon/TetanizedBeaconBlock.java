package com.tetanusmod.beacon;

import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class TetanizedBeaconBlock extends BeaconBlock {
    public TetanizedBeaconBlock(Settings settings) { super(settings); }
    @Override public BlockEntity createBlockEntity(BlockPos pos,BlockState state) { return new TetanizedBeaconEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world,BlockState state,BlockEntityType<T> type) {
        return checkType(type,ModBeacons.ENTITY,(w,p,s,be)->BeaconBlockEntity.tick(w,p,s,be));
    }
    @Override public void onPlaced(World world,BlockPos pos,BlockState state,net.minecraft.entity.LivingEntity placer,net.minecraft.item.ItemStack stack){
        super.onPlaced(world,pos,state,placer,stack);
        if(placer instanceof net.minecraft.server.network.ServerPlayerEntity player)com.tetanusmod.registry.ModAdvancements.award(player,"beacon_placed","place_beacon");
    }}