package com.tetanusmod.dragon;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public final class WitherForgingBlock extends Block {
    public WitherForgingBlock(Settings settings){super(settings);}
    @Override public ActionResult onUse(BlockState state,World world,BlockPos pos,PlayerEntity player,Hand hand,BlockHitResult hit){
        if(player.isSpectator())return ActionResult.FAIL;
        if(!world.isClient)player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId,inventory,p)->new WitherForgingScreenHandler(syncId,inventory,ScreenHandlerContext.create(world,pos)),Text.translatable("block.tetanusmod.tetanus_wither_forging_table")));
        return ActionResult.success(world.isClient);
    }
}
