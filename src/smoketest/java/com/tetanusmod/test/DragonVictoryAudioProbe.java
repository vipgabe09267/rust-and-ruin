package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.math.Vec3d;

/** Passive observation of the actual resolved SoundManager playback, test-mod only. */
public final class DragonVictoryAudioProbe {
    public static boolean enabled;
    public static int count;
    public static Vec3d position;
    public static boolean relative;
    public static String attenuation;
    private DragonVictoryAudioProbe(){}
    public static void reset(){count=0;position=null;relative=false;attenuation=null;enabled=true;}
    public static void observe(SoundInstance sound){
        if(!enabled||!sound.getId().equals(TetanusMod.id("dragon_defeated")))return;
        if(sound.getSound()==null)throw new IllegalStateException("Dragon victory sound did not resolve its actual OGG asset");
        count++;position=new Vec3d(sound.getX(),sound.getY(),sound.getZ());relative=sound.isRelative();attenuation=sound.getAttenuationType().name();
    }
}
