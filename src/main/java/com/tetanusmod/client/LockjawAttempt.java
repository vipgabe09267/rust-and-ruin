package com.tetanusmod.client;

import net.minecraft.util.Hand;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** A short failed eating gesture; it never starts item use or consumes food. */
public final class LockjawAttempt {
    private static Hand hand=Hand.MAIN_HAND;
    private static int ticks;
    public static void begin(Hand attempted){hand=attempted;ticks=14;}
    public static void tick(){if(ticks>0)ticks--;}
    public static void clear(){ticks=0;}
    public static boolean active(){return ticks>0;}
    public static void pose(Hand rendered,net.minecraft.util.Arm arm,float delta,MatrixStack matrices){
        if(ticks<=0||hand!=rendered)return;
        float lift=(float)Math.sin(Math.PI*(14-ticks+Math.min(1,Math.max(0,delta)))/14);
        int side=arm==net.minecraft.util.Arm.RIGHT?1:-1;
        matrices.translate(-side*0.22F*lift,0.48F*lift,-0.12F*lift);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side*18*lift));
    }
}
