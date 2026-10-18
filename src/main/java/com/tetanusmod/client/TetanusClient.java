package com.tetanusmod.client;

import com.tetanusmod.TetanusMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import com.tetanusmod.config.ModConfig;
import com.tetanusmod.effect.Infection;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.Hand;

public final class TetanusClient implements ClientModInitializer {
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void registerBeacon() {
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register((net.minecraft.block.entity.BlockEntityType)com.tetanusmod.beacon.ModBeacons.ENTITY,(net.minecraft.client.render.block.entity.BlockEntityRendererFactory<net.minecraft.block.entity.BeaconBlockEntity>)net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer::new);
        net.minecraft.client.gui.screen.ingame.HandledScreens.<net.minecraft.screen.BeaconScreenHandler,TetanizedBeaconScreen>register((net.minecraft.screen.ScreenHandlerType<net.minecraft.screen.BeaconScreenHandler>)(Object)com.tetanusmod.beacon.ModBeacons.SCREEN,
            (handler,inventory,title)->new TetanizedBeaconScreen((com.tetanusmod.beacon.TetanizedBeaconScreenHandler)handler,inventory,title));
    }
    private int twitchTicks;
    private float previousOffset;
    private float amplitude;
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(TetanusMod.NAIL_ENTITY, FlyingItemEntityRenderer::new);
        registerBeacon();
        com.tetanusmod.item.FieldGuideItem.open=()->net.minecraft.client.MinecraftClient.getInstance().setScreen(new FieldGuideScreen());
        InfectedPlayerList.initialize();
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(com.tetanusmod.beacon.ModBeacons.BLOCK,net.minecraft.client.render.RenderLayer.getTranslucent());
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES).registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
            public net.minecraft.util.Identifier getFabricId(){return TetanusMod.id("tetanized_textures");}
            public void reload(net.minecraft.resource.ResourceManager manager){TetanizedTextures.clear();}
        });
        EntityRendererRegistry.register(TetanusMod.TETANIZED_ARROW_ENTITY,TetanizedArrowRenderer::new);


        ModConfig local = TetanusMod.CONFIG;
        ClientPlayNetworking.registerGlobalReceiver(TetanusMod.CONFIG_PACKET, (client, handler, packet, sender) -> {
            try {
                var config=com.tetanusmod.network.ModPacketDecoder.config(packet);
                client.execute(()->TetanusMod.CONFIG=config);
            }catch(RuntimeException invalid){ClientPacketWarnings.invalid();}
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TetanusMod.CONFIG = local;
            ClientPacketWarnings.reset();
            twitchTicks = 0;
            previousOffset = 0;
        });
        ClientPlayNetworking.registerGlobalReceiver(Infection.SYMPTOM_PACKET, (client, handler, packet, sender) -> {
            final com.tetanusmod.network.ModPacketDecoder.Symptom symptom;
            try{symptom=com.tetanusmod.network.ModPacketDecoder.symptom(packet);}catch(RuntimeException invalid){ClientPacketWarnings.invalid();return;}
            int stage=symptom.stage();boolean enabled=symptom.shaking();
            client.execute(() -> {
                if (client.player == null) return;
                client.player.swingHand(Hand.MAIN_HAND);
                if (enabled && local.cameraShaking && twitchTicks == 0) {
                    amplitude = (0.15F + Math.min(stage, 2) * 0.15F) * (client.player.getRandom().nextBoolean() ? 1 : -1);
                    twitchTicks = 6;
                }
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) { twitchTicks = 0; previousOffset = 0; return; }
            if (twitchTicks > 0 || previousOffset != 0) {
                float offset = twitchTicks > 0 ? (float) Math.sin((6 - twitchTicks) * Math.PI / 3) * amplitude : 0;
                client.player.setYaw(client.player.getYaw() + offset - previousOffset);
                previousOffset = offset;
                if (twitchTicks > 0) twitchTicks--;
            }
        });
    }
}
