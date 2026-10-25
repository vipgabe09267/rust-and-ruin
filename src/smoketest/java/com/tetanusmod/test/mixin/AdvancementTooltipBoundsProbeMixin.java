package com.tetanusmod.test.mixin;

import com.tetanusmod.test.AdvancementTooltipClientProbe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the native tooltip box call; no duplicated placement formula or altered drawing. */
@Mixin(DrawContext.class)
public abstract class AdvancementTooltipBoundsProbeMixin {
    @Inject(method="drawNineSlicedTexture(Lnet/minecraft/util/Identifier;IIIIIIIII)V",at=@At("HEAD"))
    private void actualTooltipBox(Identifier texture,int x,int y,int width,int height,int slice,int textureWidth,int textureHeight,int u,int v,CallbackInfo ci){
        if(!AdvancementTooltipClientProbe.enabled||AdvancementTooltipClientProbe.activeAdvancement==null)return;
        var matrix=((DrawContext)(Object)this).getMatrices().peek().getPositionMatrix();
        float left=Float.POSITIVE_INFINITY,top=Float.POSITIVE_INFINITY,right=Float.NEGATIVE_INFINITY,bottom=Float.NEGATIVE_INFINITY;
        for(int dx:new int[]{0,width})for(int dy:new int[]{0,height}){
            var corner=new Vector4f(x+dx,y+dy,0,1).mul(matrix);
            left=Math.min(left,corner.x);top=Math.min(top,corner.y);right=Math.max(right,corner.x);bottom=Math.max(bottom,corner.y);
        }
        AdvancementTooltipClientProbe.background(left,top,right,bottom);
    }
}
