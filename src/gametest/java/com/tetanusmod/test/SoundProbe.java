package com.tetanusmod.test;

import net.minecraft.util.math.Vec3d;

/** Test-only observation of positional infection sounds. */
public final class SoundProbe {
    public static boolean enabled;
    public static int count;
    public static Vec3d position;

    public static void begin() {
        count = 0;
        position = null;
        enabled = true;
    }
}
