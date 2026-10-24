package com.tetanusmod.mixin;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.expansion.*;
import com.tetanusmod.registry.ModAdvancements;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class ExpansionLivingMixin implements ExpansionState {
    @Unique private static final net.minecraft.entity.data.TrackedData<Boolean> RABID=net.minecraft.entity.data.DataTracker.registerData(LivingEntity.class,net.minecraft.entity.data.TrackedDataHandlerRegistry.BOOLEAN);
    @Unique private int rabiesTicks,lostHealth,disorientedTicks;
    public boolean rustruin$isRabid(){return ((LivingEntity)(Object)this).getDataTracker().get(RABID);}
    public void rustruin$setRabid(boolean value){((LivingEntity)(Object)this).getDataTracker().set(RABID,value);}
    public int rustruin$getRabiesTicks(){return rabiesTicks;}public void rustruin$setRabiesTicks(int ticks){rabiesTicks=ticks;}
    public int rustruin$getLostHealth(){return lostHealth;}public void rustruin$setLostHealth(int points){lostHealth=points;}
    public int rustruin$getDisorientedTicks(){return disorientedTicks;}public void rustruin$setDisorientedTicks(int ticks){disorientedTicks=ticks;}
    @Inject(method="initDataTracker",at=@At("TAIL"))private void tracker(CallbackInfo ci){((LivingEntity)(Object)this).getDataTracker().startTracking(RABID,false);}
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))private void save(NbtCompound nbt,CallbackInfo ci){nbt.putBoolean("RustRuinRabid",rustruin$isRabid());nbt.putInt("RabiesTicks",rabiesTicks);nbt.putInt("RabiesLostHealth",lostHealth);nbt.putInt("DisorientedTicks",disorientedTicks);}
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL"))private void load(NbtCompound nbt,CallbackInfo ci){rustruin$setRabid(nbt.getBoolean("RustRuinRabid"));rabiesTicks=Math.max(0,Math.min(1199,nbt.getInt("RabiesTicks")));lostHealth=Math.max(0,Math.min(1020,nbt.getInt("RabiesLostHealth")));disorientedTicks=Math.max(0,Math.min(199,nbt.getInt("DisorientedTicks")));}
    @Inject(method="tick",at=@At("TAIL"))private void symptoms(CallbackInfo ci){var self=(LivingEntity)(Object)this;if(self.getWorld().isClient)return;Rabies.flush(self);if(!self.hasStatusEffect(ExpansionEffects.DISORIENTED))disorientedTicks=0;if(!self.hasStatusEffect(ExpansionEffects.RABIES)&&(lostHealth>0||rabiesTicks>0))RabiesEffect.cure(self);}
    @Inject(method="addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",at=@At("HEAD"),cancellable=true)
    private void rabiesVaccine(net.minecraft.entity.effect.StatusEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> result){
        var self=(LivingEntity)(Object)this;
        if(!self.getWorld().isClient&&!CommandEffects.forced()&&effect.getEffectType()==ExpansionEffects.RABIES&&self.hasStatusEffect(ExpansionEffects.RABIES_RESISTANCE)&&self.getRandom().nextFloat()<0.9F)result.setReturnValue(false);
    }
    @Inject(method="onStatusEffectApplied",at=@At("TAIL"))private void rabiesAdvancement(net.minecraft.entity.effect.StatusEffectInstance effect,Entity source,CallbackInfo ci){if(effect.getEffectType()==ExpansionEffects.RABIES&&(Object)this instanceof ServerPlayerEntity player)ModAdvancements.award(player,"rabies","contract_rabies");}
    @Inject(method="damage",at=@At("RETURN"))private void hit(DamageSource source,float amount,CallbackInfoReturnable<Boolean> result){
        var self=(LivingEntity)(Object)this;if(self.getWorld().isClient||!result.getReturnValue()||amount<=0)return;
        CombatInfection.rangedHit(self,source);
        if(source.getAttacker() instanceof LivingEntity attacker&&source.getSource()==attacker&&Rabies.rabid(attacker))Rabies.expose(self,attacker);
    }
    @Inject(method="damage",at=@At(value="INVOKE",target="Lnet/minecraft/entity/LivingEntity;damageShield(F)V"))
    private void shieldBlock(DamageSource source,float amount,CallbackInfoReturnable<Boolean> result){
        var self=(LivingEntity)(Object)this;
        if(self instanceof ServerPlayerEntity player&&CombatInfection.infectious(source)&&net.minecraft.enchantment.EnchantmentHelper.getLevel(ExpansionItems.TETANUS_PROTECTION,self.getActiveItem())>0)ModAdvancements.award(player,"shield_protection","block_tetanus");
    }
}