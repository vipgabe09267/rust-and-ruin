package com.tetanusmod.mixin;

import com.tetanusmod.entity.*;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.nbt.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;

@Mixin(WitherEntity.class)
public abstract class WitherEntityMixin implements WitherBossState {
    @Shadow @Final private ServerBossBar bossBar;
    @Unique private int rustruin$waveCooldown=-1;
    @Unique private final Map<UUID,Long> rustruin$minions=new LinkedHashMap<>();
    public int rustruin$minionCooldown(){return rustruin$waveCooldown;}
    public void rustruin$minionCooldown(int ticks){rustruin$waveCooldown=Math.max(-1,Math.min(WitherBossEncounter.MAX_COOLDOWN,ticks));}
    public Map<UUID,Long> rustruin$minionLeases(){return rustruin$minions;}
    @Inject(method="mobTick",at=@At("TAIL"))
    private void rustruin$bossBar(CallbackInfo ci){
        var self=(WitherEntity)(Object)this;
        if(MobVariants.permanent(self)){
            bossBar.setColor(BossBar.Color.YELLOW);bossBar.setStyle(BossBar.Style.NOTCHED_10);
            WitherBossEncounter.tick(self);
        }
    }
    @ModifyArg(method="dropEquipment",at=@At(value="INVOKE",target="Lnet/minecraft/entity/boss/WitherEntity;dropItem(Lnet/minecraft/item/ItemConvertible;)Lnet/minecraft/entity/ItemEntity;"),index=0)
    private net.minecraft.item.ItemConvertible rustruin$replaceVariantStar(net.minecraft.item.ItemConvertible ordinary){
        return MobVariants.permanent((WitherEntity)(Object)this)?com.tetanusmod.registry.ModItems.TETANIZED_WITHER_STAR:ordinary;
    }
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))
    private void rustruin$saveWave(NbtCompound nbt,CallbackInfo ci){
        nbt.putInt("RustRuinMinionCooldown",rustruin$waveCooldown);var leases=new NbtList();
        int count=0;for(var entry:rustruin$minions.entrySet()){
            if(count++>=WitherBossEncounter.MAX_ACTIVE)break;
            var value=new NbtCompound();value.putUuid("Id",entry.getKey());value.putLong("Expires",entry.getValue());leases.add(value);
        }
        nbt.put("RustRuinMinionLeases",leases);
    }
    @Inject(method="readCustomDataFromNbt",at=@At("TAIL"))
    private void rustruin$loadWave(NbtCompound nbt,CallbackInfo ci){
        rustruin$minionCooldown(nbt.contains("RustRuinMinionCooldown")?nbt.getInt("RustRuinMinionCooldown"):-1);
        rustruin$minions.clear();long now=((WitherEntity)(Object)this).getWorld().getTime();var values=nbt.getList("RustRuinMinionLeases",10);
        for(int i=0;i<Math.min(values.size(),WitherBossEncounter.MAX_ACTIVE);i++){
            var value=values.getCompound(i);long expires=Math.min(now+WitherBossEncounter.MINION_LIFETIME,value.getLong("Expires"));
            if(value.containsUuid("Id")&&expires>now)rustruin$minions.put(value.getUuid("Id"),expires);
        }
    }
}
