package com.tetanusmod.mixin;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.expansion.MedicVillage;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.VillagerClothingFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerDataContainer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(VillagerClothingFeatureRenderer.class)
public abstract class MedicClothingMixin {
    @Unique private LivingEntity rustruin$wearer;
    @Inject(method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",at=@At("HEAD"))
    private void wearer(MatrixStack matrices,VertexConsumerProvider vertices,int light,LivingEntity entity,float a,float b,float c,float d,float e,float f,CallbackInfo ci){rustruin$wearer=entity;}
    @Inject(method="findTexture",at=@At("RETURN"),cancellable=true)
    private void uniform(String layer,Identifier id,CallbackInfoReturnable<Identifier> result){
        if(!layer.equals("profession")||!id.equals(TetanusMod.id("medic"))||!(rustruin$wearer instanceof VillagerDataContainer villager)||villager.getVillagerData().getProfession()!=MedicVillage.MEDIC)return;
        String type=Registries.VILLAGER_TYPE.getId(villager.getVillagerData().getType()).getPath();
        if(java.util.Set.of("plains","desert","savanna","snow","taiga","swamp","jungle").contains(type))result.setReturnValue(TetanusMod.id("textures/entity/villager/profession/medic_"+type+".png"));
    }
}
