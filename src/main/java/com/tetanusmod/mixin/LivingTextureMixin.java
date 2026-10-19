package com.tetanusmod.mixin;
import com.tetanusmod.client.TetanizedTextures;
import com.tetanusmod.entity.MobVariants;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.*;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(LivingEntityRenderer.class)
public abstract class LivingTextureMixin {
    @SuppressWarnings({"rawtypes","unchecked"})
    @Redirect(method="getRenderLayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/entity/LivingEntityRenderer;getTexture(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/Identifier;"))
    private Identifier rustruin$baseSkin(LivingEntityRenderer renderer,Entity entity) {
        Identifier original=renderer.getTexture(entity);
        if(entity instanceof LivingEntity living){if(com.tetanusmod.expansion.Rabies.rabid(living))return TetanizedTextures.rabid(original);if(MobVariants.permanent(living))return TetanizedTextures.get(original);}return original;
    }
}