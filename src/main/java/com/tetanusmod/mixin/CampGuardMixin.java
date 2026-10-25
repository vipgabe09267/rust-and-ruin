package com.tetanusmod.mixin;
import com.tetanusmod.world.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Persist a camp guardian's small patrol area without changing wild golem behavior. */
@Mixin(IronGolemEntity.class)
public abstract class CampGuardMixin implements CampGuardHome {
    @Unique private BlockPos rustruin$campHome;
    public void rustruin$campHome(BlockPos pos){rustruin$campHome=pos.toImmutable();((IronGolemEntity)(Object)this).setPositionTarget(rustruin$campHome,CampGuardHome.PATROL_RADIUS);}
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))
    private void saveCampHome(NbtCompound nbt,CallbackInfo ci){if(rustruin$campHome!=null&&((IronGolemEntity)(Object)this).getCommandTags().contains(MedicStructurePiece.CAMP_GUARD_TAG))nbt.putLong("RustRuinCampHome",rustruin$campHome.asLong());}
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL"))
    private void readCampHome(NbtCompound nbt,CallbackInfo ci){rustruin$campHome=null;if(nbt.contains("RustRuinCampHome",4))rustruin$campHome(BlockPos.fromLong(nbt.getLong("RustRuinCampHome")));}
    @Inject(method="tickMovement",at=@At("TAIL"))
    private void keepCampPatrol(CallbackInfo ci){var self=(IronGolemEntity)(Object)this;if(!self.getWorld().isClient&&rustruin$campHome!=null&&self.getCommandTags().contains(MedicStructurePiece.CAMP_GUARD_TAG))self.setPositionTarget(rustruin$campHome,CampGuardHome.PATROL_RADIUS);}
}