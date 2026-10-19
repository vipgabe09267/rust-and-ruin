package com.tetanusmod.client;

import com.tetanusmod.expansion.RabiesPresence;
import net.fabricmc.fabric.api.client.networking.v1.*;
import java.util.*;

public final class RabidPlayerList {
    private static final Set<UUID> INFECTED=new HashSet<>();
    public static boolean contains(UUID uuid) { return INFECTED.contains(uuid); }
    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(RabiesPresence.PACKET,(client,handler,buffer,sender)->{
            final com.tetanusmod.network.ModPacketDecoder.Presence presence;
            try{presence=com.tetanusmod.network.ModPacketDecoder.presence(buffer);}catch(RuntimeException invalid){ClientPacketWarnings.invalid();return;}
            UUID uuid=presence.player();boolean infected=presence.infected();
            client.execute(()->{if(infected){if(INFECTED.size()<4096)INFECTED.add(uuid);}else INFECTED.remove(uuid);});
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->INFECTED.clear());
    }
}