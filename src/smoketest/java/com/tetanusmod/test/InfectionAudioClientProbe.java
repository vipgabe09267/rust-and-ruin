package com.tetanusmod.test;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.math.Vec3d;

/** Inert unless a visible regression scene explicitly requests client sound observations. */
public final class InfectionAudioClientProbe {
    public static boolean enabled;
    public static int count;
    public static Vec3d lastPosition;
    public static void observe(SoundInstance sound){
        if(enabled&&sound.getId().equals(com.tetanusmod.TetanusMod.id("tetanus_effect"))){
            count++;lastPosition=new Vec3d(sound.getX(),sound.getY(),sound.getZ());
        }
    }
}
