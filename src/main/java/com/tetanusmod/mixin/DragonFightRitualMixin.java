package com.tetanusmod.mixin;

import com.tetanusmod.dragon.DragonCrystalRitual;
import com.tetanusmod.dragon.DragonEncounter;
import com.tetanusmod.entity.MobVariants;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.dragon.*;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.List;

@Mixin(EnderDragonFight.class)
public abstract class DragonFightRitualMixin {
    @Shadow private List<EndCrystalEntity> crystals;
    @Shadow private EnderDragonSpawnState dragonSpawnState;
    @Shadow @Final private ServerWorld world;
    @Shadow @Final private ServerBossBar bossBar;
    @Shadow private java.util.UUID dragonUuid;
    @Unique private boolean rustruin$specialBar;

    @Inject(method="tick",at=@At("TAIL"))
    private void rustruin$infectPillars(CallbackInfo ci) {
        if(dragonSpawnState!=null&&DragonEncounter.specialCrystals(crystals))DragonCrystalRitual.convertPillars(world);
    }

    @Inject(method="createDragon",at=@At("RETURN"))
    private void rustruin$specialRitual(CallbackInfoReturnable<EnderDragonEntity> result) {
        if(result.getReturnValue()!=null) {
            DragonEncounter.summoned(result.getReturnValue(),crystals);
            if(MobVariants.permanent(result.getReturnValue()))DragonCrystalRitual.convertPillars(world);
        }
    }

    @Inject(method="updateFight",at=@At("TAIL"))
    private void rustruin$bossBar(EnderDragonEntity dragon,CallbackInfo ci) {
        if(!dragon.getUuid().equals(dragonUuid))return;
        if(MobVariants.permanent(dragon)) {
            bossBar.setName(Text.translatable("entity.tetanusmod.tetanized_ender_dragon"));
            bossBar.setColor(BossBar.Color.PURPLE);
            bossBar.setStyle(BossBar.Style.NOTCHED_10);
            rustruin$specialBar=true;
        } else if(rustruin$specialBar) {
            bossBar.setName(dragon.getDisplayName());
            bossBar.setColor(BossBar.Color.PINK);
            bossBar.setStyle(BossBar.Style.PROGRESS);
            rustruin$specialBar=false;
        }
    }
}
