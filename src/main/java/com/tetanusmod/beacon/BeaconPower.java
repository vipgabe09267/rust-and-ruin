package com.tetanusmod.beacon;

import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

public final class BeaconPower {
    public static boolean primaryAllowed(int level,StatusEffect effect) {
        return level>=1 && (effect==StatusEffects.SPEED || effect==StatusEffects.HASTE
                || level>=2 && (effect==StatusEffects.RESISTANCE || effect==StatusEffects.JUMP_BOOST)
                || level>=3 && effect==StatusEffects.STRENGTH);
    }
    public static boolean selectionAllowed(int level,StatusEffect primary,StatusEffect secondary) {
        return primaryAllowed(level,primary) && (secondary==null
                || level>=4 && (secondary==primary || secondary==StatusEffects.REGENERATION));
    }
    public static int level(World world,BlockPos pos) {
        int result=0;
        for(int layer=1;layer<=4;layer++) {
            if(pos.getY()-layer<world.getBottomY())break;
            for(int x=-layer;x<=layer;x++)for(int z=-layer;z<=layer;z++) {
                var state=world.getBlockState(pos.add(x,-layer,z));
                if(!state.isIn(ModBeacons.BASES))return result;
            }
            result=layer;
        }
        return result;
    }
    public static void apply(World world,BlockPos pos,int level,StatusEffect primary,StatusEffect secondary) {
        if(world.isClient||level<=0)return;
        boolean validPrimary=primaryAllowed(level,primary);
        if(!selectionAllowed(level,primary,secondary))secondary=null;
        double range=level*10+10;
        var box=new Box(pos).expand(range).stretch(0,world.getHeight(),0);
        int duration=(9+level*2)*20;
        int amplifier=level>=4&&primary==secondary?2:1;
        for(var player:world.getNonSpectatingEntities(PlayerEntity.class,box)) {
            player.addStatusEffect(new StatusEffectInstance(com.tetanusmod.TetanusMod.VACCINATED,duration,0,true,true));
            if(validPrimary)player.addStatusEffect(new StatusEffectInstance(primary,duration,amplifier,true,true));
            if(level>=4&&validPrimary&&player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)com.tetanusmod.registry.ModAdvancements.award(serverPlayer,"beacon_full","activate_full");
            if(validPrimary&&level>=4&&secondary!=null&&secondary!=primary)player.addStatusEffect(new StatusEffectInstance(secondary,duration,1,true,true));
        }
    }
}