package com.tetanusmod.test;

import net.minecraft.util.math.*;
import net.minecraft.world.World;

/** Test-only observation; optional scene scope excludes unrelated natural world-generation audio. */
public final class SoundProbe {
    public static volatile boolean enabled;
    public static int count;
    public static Vec3d position;
    private static volatile World scopeWorld;
    private static volatile Box scopeBounds;
    public static void begin(){scopeWorld=null;scopeBounds=null;count=0;position=null;enabled=true;}
    public static void begin(World world,Box bounds){enabled=false;scopeWorld=world;scopeBounds=bounds;count=0;position=null;enabled=true;}
    public static boolean accepts(World world,double x,double y,double z){
        return enabled&&(scopeWorld==null||scopeWorld==world)&&(scopeBounds==null||scopeBounds.contains(x,y,z));
    }
}
