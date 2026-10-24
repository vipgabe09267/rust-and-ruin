package com.tetanusmod.mixin;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.registry.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(WitherSkullBlock.class)
public abstract class WitherSummoningMixin {
    @Unique private static final ThreadLocal<Boolean> rustruin$special=ThreadLocal.withInitial(()->false);
    @Inject(method="onPlaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/SkullBlockEntity;)V",at=@At("HEAD"),cancellable=true)
    private static void rustruin$begin(World world,BlockPos pos,SkullBlockEntity skull,CallbackInfo ci){rustruin$special.remove();if(com.tetanusmod.entity.EnhancedWitherRitual.trySummon(world,pos))ci.cancel();}
    @Redirect(method="onPlaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/SkullBlockEntity;)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/block/CarvedPumpkinBlock;breakPatternBlocks(Lnet/minecraft/world/World;Lnet/minecraft/block/pattern/BlockPattern$Result;)V"))
    private static void rustruin$checkSand(World world,BlockPattern.Result pattern) {
        int special=0;
        for(int x=0;x<3;x++)for(int y=0;y<3;y++)if(pattern.translate(x,y,0).getBlockState().isOf(ModBlocks.TETANIZED_SOUL_SAND))special++;
        rustruin$special.set(special==4);
        CarvedPumpkinBlock.breakPatternBlocks(world,pattern);
    }
    @Redirect(method="onPlaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/SkullBlockEntity;)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private static boolean rustruin$roll(World world,Entity entity) {
        try{if(entity instanceof WitherEntity wither&&rustruin$special.get())MobVariants.rollSpecialWither(wither);return world.spawnEntity(entity);}
        finally{rustruin$special.remove();}
    }
    @Inject(method="canDispense",at=@At("HEAD"),cancellable=true)
    private static void enhancedDispenser(World world,BlockPos pos,net.minecraft.item.ItemStack stack,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> result){
        if(stack.isOf(net.minecraft.item.Items.WITHER_SKELETON_SKULL)&&com.tetanusmod.entity.EnhancedWitherRitual.canDispense(world,pos))result.setReturnValue(true);
    }
}
