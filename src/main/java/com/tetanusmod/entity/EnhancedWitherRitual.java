package com.tetanusmod.entity;

import com.tetanusmod.registry.ModBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

public final class EnhancedWitherRitual {
    public static boolean trySummon(World world,BlockPos skull) {
        if(world.isClient||world.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)return false;
        for(var axis:new Direction[]{Direction.EAST,Direction.SOUTH})for(int offset=-1;offset<=1;offset++) {
            BlockPos center=skull.down().offset(axis,offset);
            if(!baseMatches(world,center,axis))continue;
            boolean heads=true;
            for(int x=-1;x<=1;x++){var head=world.getBlockState(center.up().offset(axis,x));if(!head.isOf(Blocks.WITHER_SKELETON_SKULL)&&!head.isOf(Blocks.WITHER_SKELETON_WALL_SKULL))heads=false;}
            if(!heads)continue;

            var wither=EntityType.WITHER.create(world);if(wither==null)return false;
            for(int x=-1;x<=1;x++){world.breakBlock(center.offset(axis,x),false);world.breakBlock(center.up().offset(axis,x),false);}
            world.breakBlock(center.down(),false);
            wither.refreshPositionAndAngles(center.getX()+0.5,center.getY()-0.45,center.getZ()+0.5,axis==Direction.EAST?0:90,0);
            wither.onSummoned();MobVariants.rollSpecialWither(wither,0.25F);world.spawnEntity(wither);
            for(var player:world.getNonSpectatingEntities(net.minecraft.server.network.ServerPlayerEntity.class,wither.getBoundingBox().expand(50)))
                net.minecraft.advancement.criterion.Criteria.SUMMONED_ENTITY.trigger(player,wither);
            return true;
        }
        return false;
    }
    /** Mirrors the native dispenser's base-only check before it places the last skull. */
    public static boolean canDispense(World world,BlockPos skull){
        if(world.isClient||world.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)return false;
        for(var axis:new Direction[]{Direction.EAST,Direction.SOUTH})for(int offset=-1;offset<=1;offset++)
            if(baseMatches(world,skull.down().offset(axis,offset),axis))return true;
        return false;
    }
    private static boolean baseMatches(World world,BlockPos center,Direction axis){
        return world.getBlockState(center).isOf(Blocks.NETHERITE_BLOCK)
                &&world.getBlockState(center.down()).isOf(ModBlocks.TETANIZED_SOUL_SAND)
                &&world.getBlockState(center.offset(axis)).isOf(ModBlocks.TETANIZED_SOUL_SAND)
                &&world.getBlockState(center.offset(axis,-1)).isOf(ModBlocks.TETANIZED_SOUL_SAND)
                &&world.isAir(center.down().offset(axis))&&world.isAir(center.down().offset(axis,-1));
    }
}
