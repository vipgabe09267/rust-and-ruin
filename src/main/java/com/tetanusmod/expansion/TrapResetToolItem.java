package com.tetanusmod.expansion;

import com.tetanusmod.item.DescribedItem;
import com.tetanusmod.registry.ModAdvancements;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public final class TrapResetToolItem extends DescribedItem {
    public TrapResetToolItem(Settings settings){super(settings,"trap_reset_tool");}
    @Override public ActionResult useOnBlock(ItemUsageContext context){
        if(!(context.getWorld().getBlockEntity(context.getBlockPos()) instanceof BearTrapBlockEntity trap)||context.getPlayer()==null)return ActionResult.PASS;
        if(context.getWorld().isClient)return ActionResult.SUCCESS;
        if(context.getPlayer().isSpectator()||!trap.reset())return ActionResult.PASS;
        context.getStack().damage(1,context.getPlayer(),p->p.sendToolBreakStatus(context.getHand()));
        if(context.getPlayer() instanceof ServerPlayerEntity player)ModAdvancements.award(player,"trap_reset","reset_trap");
        return ActionResult.CONSUME;
    }
}