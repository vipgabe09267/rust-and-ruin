package com.tetanusmod.expansion;

import com.tetanusmod.TetanusMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.*;

/** Player infection flags for health objectives, including players outside entity tracking range. */
public final class RabiesPresence {
    public static final Identifier PACKET=TetanusMod.id("rabies_presence");
    private static final Map<UUID,Boolean> STATES=new HashMap<>();
    private static void send(ServerPlayerEntity recipient,UUID subject,boolean infected) {
        if(!ServerPlayNetworking.canSend(recipient,PACKET))return;
        var packet=PacketByteBufs.create();packet.writeUuid(subject);packet.writeBoolean(infected);
        ServerPlayNetworking.send(recipient,PACKET,packet);
    }
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.getTicks()%5!=0)return;
            var players=server.getPlayerManager().getPlayerList();
            for(var player:players) {
                boolean infected=player.hasStatusEffect(ExpansionEffects.RABIES);
                boolean previous=STATES.getOrDefault(player.getUuid(),false);
                STATES.put(player.getUuid(),infected);
                if(infected!=previous)for(var recipient:players)send(recipient,player.getUuid(),infected);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            for(var player:server.getPlayerManager().getPlayerList())send(handler.player,player.getUuid(),player.hasStatusEffect(ExpansionEffects.RABIES));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{
            STATES.remove(handler.player.getUuid());
            for(var player:server.getPlayerManager().getPlayerList())send(player,handler.player.getUuid(),false);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->STATES.clear());
    }
}