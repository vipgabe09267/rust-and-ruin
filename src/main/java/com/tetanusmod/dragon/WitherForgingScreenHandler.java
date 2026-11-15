package com.tetanusmod.dragon;

import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;

/** Server-authoritative four-slot table; closing returns only the real inputs. */
public final class WitherForgingScreenHandler extends ScreenHandler {
    private final ScreenHandlerContext context;
    private final PlayerEntity player;
    private final Inventory input=new SimpleInventory(3){@Override public void markDirty(){super.markDirty();WitherForgingScreenHandler.this.onContentChanged(this);}};
    private final Inventory output=new SimpleInventory(1);
    public WitherForgingScreenHandler(int id,PlayerInventory inventory){this(id,inventory,ScreenHandlerContext.EMPTY);}
    public WitherForgingScreenHandler(int id,PlayerInventory inventory,ScreenHandlerContext context){
        super(DragonContent.SCREEN,id);this.context=context;player=inventory.player;
        for(int i=0;i<3;i++){final int slot=i;addSlot(new Slot(input,i,18+i*38,37){@Override public boolean canInsert(ItemStack stack){return allowed(slot,stack);}});}
        addSlot(new Slot(output,0,144,37){
            @Override public boolean canInsert(ItemStack stack){return false;}
            @Override public boolean canTakeItems(PlayerEntity player){return player==WitherForgingScreenHandler.this.player&&!player.isSpectator()&&canUse(player)&&valid()&&hasStack();}
            @Override public void onTakeItem(PlayerEntity player,ItemStack stack){if(player==WitherForgingScreenHandler.this.player&&!player.isSpectator()&&canUse(player)&&!player.getWorld().isClient&&valid()){
                for(int i=0;i<3;i++)input.removeStack(i,1);onContentChanged(input);
                if(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)com.tetanusmod.registry.ModAdvancements.award(serverPlayer,"forge_crystal","forge_crystal");
            }super.onTakeItem(player,stack);}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
    }
    public static boolean allowed(int slot,ItemStack stack){return switch(slot){case 0->stack.isOf(Items.END_CRYSTAL);case 1->stack.isOf(com.tetanusmod.registry.ModItems.RUST);case 2->stack.isOf(Items.NETHERITE_INGOT);default->false;};}
    private boolean valid(){return player.getWorld().getRecipeManager().getFirstMatch(DragonContent.RECIPE_TYPE,input,player.getWorld()).isPresent();}
    @Override public void onContentChanged(Inventory inventory){if(player==null||player.getWorld().isClient)return;output.setStack(0,valid()?new ItemStack(DragonContent.CRYSTAL):ItemStack.EMPTY);sendContentUpdates();}
    @Override public boolean canUse(PlayerEntity player){return player==this.player&&!player.isSpectator()&&canUse(context,player,DragonContent.FORGE);}
    @Override public void onClosed(PlayerEntity player){if(player!=this.player)return;super.onClosed(player);if(!player.getWorld().isClient){dropInventory(player,input);output.clear();}}
    @Override public ItemStack quickMove(PlayerEntity player,int index){
        if(!canUse(player)||index<0||index>=slots.size())return ItemStack.EMPTY;var source=getSlot(index);if(!source.hasStack())return ItemStack.EMPTY;
        var stack=source.getStack();var original=stack.copy();
        if(index==3){if(!source.canTakeItems(player)||!insertItem(stack,4,40,true))return ItemStack.EMPTY;source.onTakeItem(player,original);return original;}
        if(index<3){if(!insertItem(stack,4,40,false))return ItemStack.EMPTY;}
        else {boolean moved=false;for(int i=0;i<3;i++)if(allowed(i,stack)&&insertItem(stack,i,i+1,false)){moved=true;break;}if(!moved&&!(index<31?insertItem(stack,31,40,false):insertItem(stack,4,31,false)))return ItemStack.EMPTY;}
        if(stack.isEmpty())source.setStack(ItemStack.EMPTY);else source.markDirty();source.onTakeItem(player,stack);onContentChanged(input);return original;
    }
}
