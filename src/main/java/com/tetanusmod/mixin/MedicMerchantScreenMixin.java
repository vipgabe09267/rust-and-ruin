package com.tetanusmod.mixin;

import com.tetanusmod.client.MedicTradeScreen;
import com.tetanusmod.expansion.MedicTradeOffers;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MedicMerchantScreenMixin implements MedicTradeScreen.View {
    @Shadow int indexStartOffset;
    public int rustruin$firstOffer(){return indexStartOffset;}
    @Redirect(method="drawForeground",at=@At(value="INVOKE",target="Lnet/minecraft/text/Text;translatable(Ljava/lang/String;)Lnet/minecraft/text/MutableText;"))
    private MutableText medicRank(String key){
        var screen=(MerchantScreen)(Object)this;
        return Text.translatable(MedicTradeScreen.isMedic(screen)&&key.startsWith("merchant.level.")?MedicTradeScreen.rankKey(screen.getScreenHandler().getLevelProgress()):key);
    }
    @Redirect(method="drawForeground",at=@At(value="INVOKE",target="Lnet/minecraft/text/Text;copy()Lnet/minecraft/text/MutableText;"))
    private MutableText fieldTitle(Text original){
        return MedicTradeScreen.title((MerchantScreen)(Object)this,original);
    }
    @Inject(method="drawLevelInfo",at=@At("HEAD"),cancellable=true)
    private void finalRank(DrawContext context,int x,int y,TradeOffer offer,CallbackInfo ci){
        var screen=(MerchantScreen)(Object)this;
        if(MedicTradeScreen.isMedic(screen)&&screen.getScreenHandler().getLevelProgress()>=MedicTradeOffers.LAST_RANK)ci.cancel();
    }
}
