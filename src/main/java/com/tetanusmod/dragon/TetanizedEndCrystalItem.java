package com.tetanusmod.dragon;

import net.minecraft.block.Blocks;
import net.minecraft.item.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.event.GameEvent;

/** Keeps the vanilla placement surface, clearance and native respawn animation. */
public final class TetanizedEndCrystalItem extends EndCrystalItem {
    public TetanizedEndCrystalItem(Settings settings){super(settings);}
    @Override public ActionResult useOnBlock(ItemUsageContext context){
        var world=context.getWorld();var base=context.getBlockPos();var state=world.getBlockState(base);var pos=base.up();
        var player=context.getPlayer();if(!world.isInBuildLimit(pos)||!world.getWorldBorder().contains(pos)||player!=null&&(player.isSpectator()||!world.canPlayerModifyAt(player,pos)||!player.canPlaceOn(pos,context.getSide(),context.getStack())))return ActionResult.FAIL;
        if(!state.isOf(Blocks.OBSIDIAN)&&!state.isOf(Blocks.BEDROCK)||!world.isAir(pos)||!world.getOtherEntities(null,new Box(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+1,pos.getY()+2,pos.getZ()+1)).isEmpty())return ActionResult.FAIL;
        if(world instanceof ServerWorld server){
            var crystal=DragonContent.CRYSTAL_ENTITY.create(server);if(crystal==null)return ActionResult.FAIL;
            crystal.setPosition(pos.getX()+.5,pos.getY(),pos.getZ()+.5);crystal.setShowBottom(false);
            if(!server.spawnEntity(crystal))return ActionResult.FAIL;
            world.emitGameEvent(context.getPlayer(),GameEvent.ENTITY_PLACE,pos);
            var fight=server.getEnderDragonFight();if(fight!=null)fight.respawnDragon();
            if(context.getPlayer()==null||!context.getPlayer().getAbilities().creativeMode)context.getStack().decrement(1);
        }
        return ActionResult.success(world.isClient);
    }
}
