package com.tetanusmod.client;

import com.tetanusmod.expansion.MedicTradeOffers;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;

/** Screen metadata is bound to both the originating connection and the native menu sync id. */
public final class MedicTradeScreen {
    public interface View { int rustruin$firstOffer(); }
    private static ClientPlayNetworkHandler connection;
    private static MerchantScreen activeScreen;
    private static boolean field;
    public static void initialize(){
        ClientPlayNetworking.registerGlobalReceiver(MedicTradeOffers.SCREEN_PACKET,(client,handler,buffer,response)->{
            com.tetanusmod.network.ModPacketDecoder.MedicScreen metadata;try{metadata=com.tetanusmod.network.ModPacketDecoder.medicScreen(buffer);}catch(RuntimeException invalid){ClientPacketWarnings.invalid();return;}
            client.execute(()->{if(client.getNetworkHandler()==handler&&client.currentScreen instanceof MerchantScreen screen&&screen.getScreenHandler().syncId==metadata.syncId()){connection=handler;activeScreen=screen;field=metadata.fieldMedic();}});
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{if(connection==handler)clear();});
        ClientTickEvents.END_CLIENT_TICK.register(client->{if(activeScreen!=null&&client.currentScreen!=activeScreen)clear();});
    }
    private static void clear(){connection=null;activeScreen=null;field=false;}
    public static boolean isMedic(MerchantScreen screen){var client=MinecraftClient.getInstance();return connection!=null&&client.getNetworkHandler()==connection&&activeScreen==screen;}
    public static boolean isField(MerchantScreen screen){return isMedic(screen)&&field;}
    public static net.minecraft.text.MutableText title(MerchantScreen screen,net.minecraft.text.Text original){
        if(!isMedic(screen))return original.copy();
        var client=MinecraftClient.getInstance();
        var base=isField(screen)?net.minecraft.text.Text.translatable("gui.tetanusmod.field_medic"):original.copy();
        var rank=net.minecraft.text.Text.translatable(rankKey(screen.getScreenHandler().getLevelProgress()));
        var combined=base.copy().append(" - ").append(rank);
        if(client.textRenderer.getWidth(combined)>164)
            return net.minecraft.text.Text.translatable(isField(screen)?"gui.tetanusmod.field_medic_short":"gui.tetanusmod.medic_short");
        return base;
    }
    public static String rankKey(int level){return "merchant.tetanusmod.rank."+Math.max(1,Math.min(MedicTradeOffers.LAST_RANK,level));}
}
