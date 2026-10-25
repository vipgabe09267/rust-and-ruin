package com.tetanusmod.test;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.math.Vec3d;

/** Passive client observations, inert outside an explicitly enabled visible test. */
public final class InfectionAudioClientProbe {
    public static boolean enabled;
    public static int count,medicWorkCount;
    public static Vec3d lastPosition,lastMedicWorkPosition;
    public static float lastVolume;
    public static boolean lastRelative;
    public static String lastAttenuation;
    public record State(boolean enabled,int count,Vec3d position,int workCount,Vec3d workPosition,float volume,boolean relative,String attenuation){}
    public static State snapshot(){return new State(enabled,count,lastPosition,medicWorkCount,lastMedicWorkPosition,lastVolume,lastRelative,lastAttenuation);}
    public static void restore(State state){enabled=state.enabled();count=state.count();lastPosition=state.position();medicWorkCount=state.workCount();lastMedicWorkPosition=state.workPosition();lastVolume=state.volume();lastRelative=state.relative();lastAttenuation=state.attenuation();}
    public static void reset(){count=medicWorkCount=0;lastPosition=lastMedicWorkPosition=null;lastVolume=0;lastRelative=false;lastAttenuation=null;}
    private static void requireResolved(SoundInstance sound){if(sound.getSound()==null)throw new IllegalStateException("Native sound playback did not resolve its audio asset before observation: "+sound.getId());}
    public static void observe(SoundInstance sound){
        if(!enabled)return;
        if(sound.getId().equals(com.tetanusmod.TetanusMod.id("tetanus_effect"))){requireResolved(sound);count++;lastPosition=new Vec3d(sound.getX(),sound.getY(),sound.getZ());lastVolume=sound.getVolume();lastRelative=sound.isRelative();lastAttenuation=sound.getAttenuationType().name();}
        else if(sound.getId().equals(com.tetanusmod.TetanusMod.id("medic_work"))){requireResolved(sound);medicWorkCount++;lastMedicWorkPosition=new Vec3d(sound.getX(),sound.getY(),sound.getZ());}
    }
}
