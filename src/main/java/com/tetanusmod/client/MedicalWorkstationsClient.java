package com.tetanusmod.client;

import com.tetanusmod.medical.MedicalWorkstations;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;

public final class MedicalWorkstationsClient {
    private MedicalWorkstationsClient(){}
    public static void initialize(){
        HandledScreens.register(MedicalWorkstations.SCREEN,PrescriptionWriterScreen::new);
        HandledScreens.register(MedicalWorkstations.BOARD_SCREEN, ClinicNoticeBoardScreen::new);
        BlockRenderLayerMap.INSTANCE.putBlock(MedicalWorkstations.WRITER,RenderLayer.getCutout());
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack,context,lines)->{
            if(stack.isOf(com.tetanusmod.expansion.ExpansionItems.RABID_MEAT))lines.add(net.minecraft.text.Text.translatable("tooltip.tetanusmod.rabid_meat_origin").formatted(net.minecraft.util.Formatting.GRAY));
        });
    }
}
