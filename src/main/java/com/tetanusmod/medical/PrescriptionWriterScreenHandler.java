package com.tetanusmod.medical;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/** Native slots and server recipe lookup own every input/output change. */
public final class PrescriptionWriterScreenHandler extends ScreenHandler {
    private final PlayerEntity owner;
    private final ScreenHandlerContext context;
    private final SimpleInventory input=new SimpleInventory(2){@Override public void markDirty(){super.markDirty();PrescriptionWriterScreenHandler.this.onContentChanged(this);}};
    private final SimpleInventory output=new SimpleInventory(1);
    private long lastSoundTick=Long.MIN_VALUE;
    public PrescriptionWriterScreenHandler(int syncId,PlayerInventory inventory){this(syncId,inventory,ScreenHandlerContext.EMPTY);}
    public PrescriptionWriterScreenHandler(int syncId,PlayerInventory inventory,ScreenHandlerContext context){
        super(MedicalWorkstations.SCREEN,syncId);owner=inventory.player;this.context=context;
        addSlot(new Slot(input,0,35,38){@Override public boolean canInsert(ItemStack stack){return stack.isOf(Items.PAPER);}});
        addSlot(new Slot(input,1,71,38){@Override public boolean canInsert(ItemStack stack){return stack.isOf(Items.INK_SAC);}});
        addSlot(new Slot(output,0,131,38){
            @Override public boolean canInsert(ItemStack stack){return false;}
            @Override public boolean canTakeItems(PlayerEntity player){return player==owner&&matched()!=null&&!getStack().isEmpty();}
            @Override public void onTakeItem(PlayerEntity player,ItemStack stack){consume(player,stack);super.onTakeItem(player,stack);}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,88+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,146));
    }
    private PrescriptionWritingRecipe matched(){return owner.getWorld().getRecipeManager().getFirstMatch(PrescriptionWritingRecipe.TYPE,input,owner.getWorld()).orElse(null);}
    @Override public void onContentChanged(Inventory inventory){
        if(owner==null||owner.getWorld().isClient)return;
        var recipe=matched();output.setStack(0,recipe==null?ItemStack.EMPTY:recipe.craft(input,owner.getWorld().getRegistryManager()));sendContentUpdates();
    }
    private void consume(PlayerEntity player,ItemStack crafted){
        if(player!=owner||player.getWorld().isClient||matched()==null)return;
        input.removeStack(0,1);input.removeStack(1,1);onContentChanged(input);
        crafted.onCraft(player.getWorld(),player,1);
        context.run((world,pos)->{if(world.getTime()!=lastSoundTick){world.playSound(null,pos,SoundEvents.ITEM_BOOK_PAGE_TURN,SoundCategory.BLOCKS,0.65F,1.1F);lastSoundTick=world.getTime();}});
    }
    @Override public boolean canUse(PlayerEntity player){return player==owner&&!player.isSpectator()&&canUse(context,player,MedicalWorkstations.WRITER);}
    @Override public void onClosed(PlayerEntity player){if(player!=owner)return;super.onClosed(player);context.run((world,pos)->dropInventory(player,input));output.clear();}
    @Override public ItemStack quickMove(PlayerEntity player,int index){
        if(!canUse(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasStack())return ItemStack.EMPTY;
        ItemStack stack=slot.getStack(),original=stack.copy();
        if(index==2){
            if(!slot.canTakeItems(player)||!insertItem(stack,3,39,true))return ItemStack.EMPTY;
            slot.onTakeItem(player,original);return original;
        }
        boolean moved;
        if(index<2)moved=insertItem(stack,3,39,false);
        else if(stack.isOf(Items.PAPER))moved=insertItem(stack,0,1,false);
        else if(stack.isOf(Items.INK_SAC))moved=insertItem(stack,1,2,false);
        else moved=index<30?insertItem(stack,30,39,false):insertItem(stack,3,30,false);
        if(!moved)return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setStack(ItemStack.EMPTY);else slot.markDirty();
        if(stack.getCount()==original.getCount())return ItemStack.EMPTY;
        slot.onTakeItem(player,stack);return original;
    }
}
