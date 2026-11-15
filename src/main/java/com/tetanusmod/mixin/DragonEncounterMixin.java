package com.tetanusmod.mixin;
import com.tetanusmod.dragon.*;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.ArrayDeque;

@Mixin(EnderDragonEntity.class)
public abstract class DragonEncounterMixin extends MobEntity implements DragonEncounterState {
    @Unique private final DragonEncounter.Data rustruin$encounter=new DragonEncounter.Data();
    @Unique private final ArrayDeque<Float> rustruin$damageStack=new ArrayDeque<>();
    protected DragonEncounterMixin(EntityType<? extends MobEntity> type,World world){super(type,world);}
    public DragonEncounter.Data rustruin$dragonEncounter(){return rustruin$encounter;}
    @Inject(method="tickMovement",at=@At("TAIL"))private void rustruin$encounterTick(CallbackInfo ci){DragonEncounter.tick((EnderDragonEntity)(Object)this);}
    @Inject(method="parentDamage",at=@At("HEAD"))private void rustruin$beforeDamage(DamageSource source,float amount,CallbackInfoReturnable<Boolean> result){rustruin$damageStack.push(getHealth());}
    @Inject(method="parentDamage",at=@At("RETURN"))private void rustruin$afterDamage(DamageSource source,float amount,CallbackInfoReturnable<Boolean> result){if(!rustruin$damageStack.isEmpty())DragonEncounter.recordDamage((EnderDragonEntity)(Object)this,source,rustruin$damageStack.pop());}
    @Inject(method="kill",at=@At("HEAD"))private void rustruin$instantDeath(CallbackInfo ci){DragonEncounter.killedInstantly((EnderDragonEntity)(Object)this);}
    @Inject(method="updatePostDeath",at=@At("HEAD"))private void rustruin$death(CallbackInfo ci){DragonEncounter.deathTick((EnderDragonEntity)(Object)this);}
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))private void rustruin$save(NbtCompound nbt,CallbackInfo ci){rustruin$encounter.write(nbt);}
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL"))private void rustruin$load(NbtCompound nbt,CallbackInfo ci){rustruin$encounter.read(nbt,getWorld().getTime());}
    @Override protected void updatePassengerPosition(Entity passenger,PositionUpdater updater){var point=DragonEncounter.passengerPosition((EnderDragonEntity)(Object)this,passenger);if(point!=null)updater.accept(passenger,point.x,point.y,point.z);else super.updatePassengerPosition(passenger,updater);}
}
