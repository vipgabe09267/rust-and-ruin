package com.tetanusmod.mixin;

import com.tetanusmod.expansion.MedicSupplies;
import com.tetanusmod.expansion.MedicTradeOffers;
import com.tetanusmod.expansion.MedicVillage;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(VillagerEntity.class)
public abstract class MedicTradesMixin implements MedicSupplies.State, MedicTradeOffers.State {
    @Unique private long rustruin$supplyDay=Long.MIN_VALUE;
    @Unique private long rustruin$restock=Long.MIN_VALUE;
    @Unique private boolean rustruin$fieldMedic;
    public long rustruin$lastSupplyDay(){return rustruin$supplyDay;}
    public void rustruin$lastSupplyDay(long day){rustruin$supplyDay=day;}
    public long rustruin$lastRestock(){return rustruin$restock;}
    public void rustruin$lastRestock(long tick){rustruin$restock=tick;}
    public boolean rustruin$isFieldMedic(){return rustruin$fieldMedic;}
    public void rustruin$isFieldMedic(boolean value){rustruin$fieldMedic=value;}
    @Unique private VillagerEntity rustruin$medic(){return (VillagerEntity)(Object)this;}
    @Unique private boolean rustruin$isMedic(){return rustruin$medic().getVillagerData().getProfession()==MedicVillage.MEDIC;}
    @Inject(method="fillRecipes",at=@At("HEAD"))
    private void earlierRanks(CallbackInfo ci){MedicTradeOffers.addEarlierRanks(rustruin$medic());}
    @Inject(method="fillRecipes",at=@At("TAIL"))
    private void generated(CallbackInfo ci){MedicTradeOffers.generated(rustruin$medic());}
    @Inject(method="canLevelUp",at=@At("HEAD"),cancellable=true)
    private void finalRank(CallbackInfoReturnable<Boolean> cir){if(rustruin$isMedic()&&rustruin$medic().getVillagerData().getLevel()>=MedicTradeOffers.LAST_RANK)cir.setReturnValue(false);}
    @Inject(method="shouldRestock",at=@At("HEAD"),cancellable=true)
    private void supplySchedule(CallbackInfoReturnable<Boolean> cir){if(rustruin$isMedic()&&!MedicTradeOffers.canRestock(rustruin$medic()))cir.setReturnValue(false);}
    @Inject(method={"restock","restockAndUpdateDemandBonus"},at=@At("HEAD"),cancellable=true)
    private void preventEarlyRestock(CallbackInfo ci){if(rustruin$isMedic()&&!MedicTradeOffers.canRestock(rustruin$medic()))ci.cancel();}
    @Inject(method={"restock","restockAndUpdateDemandBonus"},at=@At("TAIL"))
    private void rememberRestock(CallbackInfo ci){if(rustruin$isMedic())MedicTradeOffers.restocked(rustruin$medic());}
    @Inject(method="beginTradeWith",at=@At("TAIL"))
    private void tradeMetadata(net.minecraft.entity.player.PlayerEntity player,CallbackInfo ci){MedicTradeOffers.sendScreenMetadata(rustruin$medic(),player);}
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))
    private void saveSupplies(NbtCompound nbt,CallbackInfo ci){nbt.putLong("RustRuinSupplyDay",rustruin$supplyDay);MedicTradeOffers.save(rustruin$medic(),nbt);}
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL"))
    private void loadSupplies(NbtCompound nbt,CallbackInfo ci){rustruin$supplyDay=nbt.contains("RustRuinSupplyDay",net.minecraft.nbt.NbtElement.LONG_TYPE)?nbt.getLong("RustRuinSupplyDay"):Long.MIN_VALUE;MedicTradeOffers.load(rustruin$medic(),nbt);}
}
