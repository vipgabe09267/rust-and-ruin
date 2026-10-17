package com.tetanusmod.mixin;
import com.tetanusmod.entity.MobVariants;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.BossBar;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(WitherEntity.class)
public abstract class WitherEntityMixin {
    @Shadow @Final private ServerBossBar bossBar;
    @Inject(method = "mobTick", at = @At("TAIL"))
    private void rustruin$bossBar(CallbackInfo ci) {
        if (MobVariants.permanent((WitherEntity)(Object)this)) {
            bossBar.setColor(BossBar.Color.YELLOW);
            bossBar.setStyle(BossBar.Style.NOTCHED_10);
        }
    }
}
