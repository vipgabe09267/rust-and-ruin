package com.tetanusmod.mixin;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.rust.RustingSystem;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method="damage",at=@At("HEAD"),cancellable=true)
    private void rustruin$starExplosionProtection(net.minecraft.entity.damage.DamageSource source,float amount,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        ItemEntity entity=(ItemEntity)(Object)this;
        if(entity.getStack().isOf(com.tetanusmod.registry.ModItems.TETANIZED_WITHER_STAR) && source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_EXPLOSION))cir.setReturnValue(false);
    }
    @Unique private int tetanus$waterTicks;
    @Unique private int tetanus$rainTicks;
    @Unique private int rustruin$rodTicks;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tetanus$rustInWater(CallbackInfo ci) {
        ItemEntity entity = (ItemEntity) (Object) this;
        if (entity.getWorld().isClient || !entity.isAlive()) return;
        ItemStack stack = entity.getStack();
        if(stack.isOf(net.minecraft.item.Items.FISHING_ROD)) {
            if(RustingSystem.touchingWater(entity)){
                if(++rustruin$rodTicks>=TetanusMod.CONFIG.waterRustTicks)
                    com.tetanusmod.rust.FishingSalvage.breakRod(entity,entity.getWorld().random.nextFloat());
            }else rustruin$rodTicks=0;
            return;
        }
        rustruin$rodTicks=0;
        Item rusty = RustingSystem.rusted(stack.getItem());
        boolean convert = false;
        if (rusty == null) {
            tetanus$waterTicks = tetanus$rainTicks = 0;
        } else if (RustingSystem.touchingWater(entity)) {
            tetanus$rainTicks = 0;
            convert = ++tetanus$waterTicks >= TetanusMod.CONFIG.waterRustTicks;
        } else {
            tetanus$waterTicks = 0;
            if (TetanusMod.CONFIG.rainRusting && entity.getWorld().hasRain(entity.getBlockPos())) {
                convert = ++tetanus$rainTicks >= TetanusMod.CONFIG.rainRustTicks;
            } else tetanus$rainTicks = 0;
        }
        if (convert) {
            entity.setStack(RustingSystem.convert(stack, rusty));
            if (rusty == com.tetanusmod.registry.ModItems.RUSTED_NAIL || rusty == com.tetanusmod.registry.ModItems.RUSTED_BENT_NAIL) {
                com.tetanusmod.registry.ModAdvancements.rusted(entity);
            }
            RustingSystem.rustFeedback(entity.getWorld(), entity.getPos());
            tetanus$waterTicks = tetanus$rainTicks = 0;
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void tetanus$saveWaterTime(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("TetanusWaterTicks", tetanus$waterTicks);
        nbt.putInt("TetanusRainTicks", tetanus$rainTicks);
        nbt.putInt("RustRuinRodTicks",rustruin$rodTicks);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void tetanus$loadWaterTime(NbtCompound nbt, CallbackInfo ci) {
        tetanus$waterTicks = Math.max(0, Math.min(TetanusMod.CONFIG.waterRustTicks - 1, nbt.getInt("TetanusWaterTicks")));
        tetanus$rainTicks = Math.max(0, Math.min(TetanusMod.CONFIG.rainRustTicks - 1, nbt.getInt("TetanusRainTicks")));
        rustruin$rodTicks=Math.max(0,Math.min(TetanusMod.CONFIG.waterRustTicks-1,nbt.getInt("RustRuinRodTicks")));
    }
}
