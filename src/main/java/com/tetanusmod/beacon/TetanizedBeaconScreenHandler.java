package com.tetanusmod.beacon;

import com.tetanusmod.registry.ModItems;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import java.util.Optional;

/** Native beacon payments plus an additional, single-use star payment for level IV. */
public final class TetanizedBeaconScreenHandler extends BeaconScreenHandler {
    public static final int STAR_SLOT=37;
    public static final int SELECT_STANDARD=0;
    public static final int SELECT_LEVEL_IV=1;
    private final ScreenHandlerContext location;
    private final TetanizedBeaconEntity beacon;
    private final PlayerEntity owner;
    private final SimpleInventory starInventory=new SimpleInventory(1);
    private boolean requestedLevelIV;
    private int syncedLevelIV;

    public TetanizedBeaconScreenHandler(int syncId,PlayerInventory inventory) {
        this(syncId,inventory,new ArrayPropertyDelegate(3),ScreenHandlerContext.EMPTY,null);
    }
    public TetanizedBeaconScreenHandler(int syncId,PlayerInventory inventory,PropertyDelegate properties,ScreenHandlerContext context) {
        this(syncId,inventory,properties,context,null);
    }
    public TetanizedBeaconScreenHandler(int syncId,PlayerInventory inventory,PropertyDelegate properties,ScreenHandlerContext context,TetanizedBeaconEntity beacon) {
        super(syncId,inventory,properties,context);
        this.location=context;this.beacon=beacon;this.owner=inventory.player;
        Slot old=getSlot(0);
        Slot payment=new Slot(old.inventory,0,134,111) {
            @Override public boolean canInsert(ItemStack stack) { return stack.isOf(Items.NETHERITE_INGOT); }
            @Override public int getMaxItemCount() { return 1; }
        };
        payment.id=0;slots.set(0,payment);

        addSlot(new Slot(starInventory,0,155,111) {
            @Override public boolean canInsert(ItemStack stack) { return stack.isOf(ModItems.TETANIZED_WITHER_STAR); }
            @Override public int getMaxItemCount() { return 1; }
        });
        addProperty(new Property() {
            @Override public int get() { return TetanizedBeaconScreenHandler.this.beacon==null?syncedLevelIV:TetanizedBeaconScreenHandler.this.beacon.isLevelIV()?1:0; }
            @Override public void set(int value) { syncedLevelIV=value==1?1:0; }
        });
    }
    @Override public ScreenHandlerType<?> getType() { return ModBeacons.SCREEN; }
    @Override public boolean canUse(PlayerEntity player) {
        return location.get((world,pos)->world.getBlockState(pos).isOf(ModBeacons.BLOCK)&&player.squaredDistanceTo(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5)<=64,true);
    }
    public boolean isLevelIV() { return beacon==null?syncedLevelIV==1:beacon.isLevelIV(); }
    public boolean hasStar() { return getSlot(STAR_SLOT).getStack().isOf(ModItems.TETANIZED_WITHER_STAR); }
    @Override public boolean onButtonClick(PlayerEntity player,int id) {
        if(player.isSpectator()||!canUse(player)||(id!=SELECT_STANDARD&&id!=SELECT_LEVEL_IV))return false;
        requestedLevelIV=id==SELECT_LEVEL_IV;
        return true;
    }
    @Override public ItemStack quickMove(PlayerEntity player,int slot) {
        if(slot<0||slot>=slots.size())return ItemStack.EMPTY;
        Slot source=getSlot(slot);
        if(!source.hasStack())return ItemStack.EMPTY;
        ItemStack stack=source.getStack(),original=stack.copy();
        if(slot==0||slot==STAR_SLOT) {
            if(!insertItem(stack,1,37,true))return ItemStack.EMPTY;
        }else if(stack.isOf(ModItems.TETANIZED_WITHER_STAR)&&!getSlot(STAR_SLOT).hasStack()) {
            if(!insertItem(stack,STAR_SLOT,STAR_SLOT+1,false))return ItemStack.EMPTY;
        }else if(stack.isOf(Items.NETHERITE_INGOT)&&!getSlot(0).hasStack()) {
            if(!insertItem(stack,0,1,false))return ItemStack.EMPTY;
        }else if(slot<28) {
            if(!insertItem(stack,28,37,false))return ItemStack.EMPTY;
        }else if(!insertItem(stack,1,28,false))return ItemStack.EMPTY;
        if(stack.isEmpty())source.setStack(ItemStack.EMPTY);else source.markDirty();
        if(stack.getCount()==original.getCount())return ItemStack.EMPTY;
        source.onTakeItem(player,stack);
        return original;
    }
    @Override public void setEffects(Optional<StatusEffect> primary,Optional<StatusEffect> secondary) {
        boolean upgrade=requestedLevelIV;
        // An invalid packet must not leave a stale upgrade request for a later confirmation.
        requestedLevelIV=false;
        if(owner.isSpectator()||!canUse(owner))return;
        if(beacon!=null&&!location.get((world,pos)->world.getBlockEntity(pos)==beacon,false))return;
        int level=location.get((world,pos)->BeaconPower.level(world,pos),getProperties());
        if(!getSlot(0).getStack().isOf(Items.NETHERITE_INGOT)
                ||!BeaconPower.selectionAllowed(level,primary.orElse(null),secondary.orElse(null)))return;
        if(upgrade&&(level<4||primary.isEmpty()||secondary.orElse(null)!=primary.get()||!hasStar()))return;
        // Native setEffects sets both delegate values and consumes exactly one ingot.
        super.setEffects(primary,secondary);
        if(upgrade)getSlot(STAR_SLOT).takeStack(1);
        if(beacon!=null)beacon.setLevelIV(upgrade);else syncedLevelIV=upgrade?1:0;
        sendContentUpdates();
    }
    @Override public void onClosed(PlayerEntity player) {
        requestedLevelIV=false;
        super.onClosed(player);
        if(!player.getWorld().isClient)dropInventory(player,starInventory);
    }
}
