package com.tetanusmod.test.mixin;

import com.tetanusmod.test.GraphicalCampDiscoveryAudit;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Passive, UUID-scoped observation of the actual native feedback sent to the client. */
@Mixin(ServerPlayerEntity.class)
public abstract class CampCommandFeedbackProbeMixin {
    @Inject(method="sendMessageToClient",at=@At("HEAD"))
    private void rustruin$observeNativeCampFeedback(Text text,boolean overlay,CallbackInfo ci){
        if(!overlay)GraphicalCampDiscoveryAudit.observeServerFeedback(((ServerPlayerEntity)(Object)this).getUuid(),text);
    }
}
