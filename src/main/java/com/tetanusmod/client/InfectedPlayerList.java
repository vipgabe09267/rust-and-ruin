package com.tetanusmod.client;

import com.tetanusmod.effect.InfectionPresence;
import net.fabricmc.fabric.api.client.networking.v1.*;
import java.util.*;

public final class InfectedPlayerList {
    private static final Set<UUID> INFECTED=new HashSet<>();
    public static boolean contains(UUID uuid) { return INFECTED.contains(uuid); }
    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(InfectionPresence.PACKET,(client,handler,buffer,sender)->{
            UUID uuid=buffer.readUuid();boolean infected=buffer.readBoolean();
            client.execute(()->{if(infected)INFECTED.add(uuid);else INFECTED.remove(uuid);});
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->INFECTED.clear());
    }
}