package com.tetanusmod.network;

import com.tetanusmod.config.ModConfig;
import net.minecraft.network.PacketByteBuf;
import java.util.UUID;

/** Bounded, flat, primitive-only server messages. Never accepts executable text. */
public final class ModPacketDecoder {
    public record Presence(UUID player,boolean infected){}
    public record Symptom(int stage,boolean shaking){}
    private static boolean flag(PacketByteBuf buffer){int value=buffer.readUnsignedByte();if(value>1)throw new IllegalArgumentException("Invalid flag");return value==1;}
    public static Presence presence(PacketByteBuf buffer) {
        if(buffer.readableBytes()!=17)throw new IllegalArgumentException("Invalid presence length");
        return new Presence(buffer.readUuid(),flag(buffer));
    }
    public static Symptom symptom(PacketByteBuf buffer) {
        if(buffer.readableBytes()!=2)throw new IllegalArgumentException("Invalid symptom length");
        int stage=buffer.readUnsignedByte();if(stage>2)throw new IllegalArgumentException("Invalid stage");
        return new Symptom(stage,flag(buffer));
    }
    public static ModConfig config(PacketByteBuf buffer) {
        if(buffer.readableBytes()<1||buffer.readableBytes()>8192)throw new IllegalArgumentException("Invalid config length");
        String json=buffer.readString(8192);
        if(buffer.isReadable())throw new IllegalArgumentException("Trailing config bytes");
        boolean quoted=false,escaped=false;int depth=0;
        for(int i=0;i<json.length();i++) {
            char ch=json.charAt(i);
            if(quoted){if(escaped)escaped=false;else if(ch=='\\')escaped=true;else if(ch=='"')quoted=false;}
            else if(ch=='"')quoted=true;
            else if(ch=='{'||ch=='['){if(++depth>1)throw new IllegalArgumentException("Nested config rejected");}
            else if(ch=='}'||ch==']'){if(--depth<0)throw new IllegalArgumentException("Unbalanced config");}
        }
        if(quoted||depth!=0)throw new IllegalArgumentException("Unbalanced config");
        return ModConfig.fromJson(json);
    }
}