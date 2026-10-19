package com.tetanusmod.mixin;
import com.tetanusmod.entity.NailEntity;
import com.tetanusmod.expansion.ModDamage;
import com.tetanusmod.registry.ModAdvancements;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayerEntity.class)
public abstract class ReturningNailAdvancementMixin {
    @Inject(method="onDeath",at=@At("TAIL"))
    private void returnedNail(DamageSource source,CallbackInfo ci){
        var player=(ServerPlayerEntity)(Object)this;
        if(source.isOf(ModDamage.RUSTED_NAIL)&&source.getAttacker()==player&&source.getSource() instanceof NailEntity nail&&nail.wasThrownUpward())ModAdvancements.award(player,"why_did_i_do_that","returned_nail");
    }
}