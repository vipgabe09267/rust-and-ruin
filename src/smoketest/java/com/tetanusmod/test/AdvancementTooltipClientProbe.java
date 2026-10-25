package com.tetanusmod.test;

import net.minecraft.util.Identifier;

/** Passive, smoke-only observation of real native widget drawing and its actual background bounds. */
public final class AdvancementTooltipClientProbe {
    public static boolean enabled;
    public static long frames;
    public static Identifier lastAdvancement,activeAdvancement,lastBoundsAdvancement;
    public static Bounds lastBounds;
    public static long frameNumber;public static Identifier frameAdvancement,frameBoundsAdvancement;public static Bounds frameBounds;
    public record Bounds(float left,float top,float right,float bottom){}
    public record State(boolean enabled,long frames,Identifier lastAdvancement,Identifier activeAdvancement,Identifier lastBoundsAdvancement,Bounds lastBounds,long frameNumber,Identifier frameAdvancement,Identifier frameBoundsAdvancement,Bounds frameBounds){}
    public static State snapshot(){return new State(enabled,frames,lastAdvancement,activeAdvancement,lastBoundsAdvancement,lastBounds,frameNumber,frameAdvancement,frameBoundsAdvancement,frameBounds);}
    public static void restore(State state){if(state==null)return;enabled=state.enabled;frames=state.frames;lastAdvancement=state.lastAdvancement;activeAdvancement=state.activeAdvancement;lastBoundsAdvancement=state.lastBoundsAdvancement;lastBounds=state.lastBounds;frameNumber=state.frameNumber;frameAdvancement=state.frameAdvancement;frameBoundsAdvancement=state.frameBoundsAdvancement;frameBounds=state.frameBounds;}
    public static void frameStart(){if(!enabled)return;frameNumber++;frameAdvancement=null;frameBoundsAdvancement=null;frameBounds=null;}
    public static void begin(Identifier advancement){if(enabled)activeAdvancement=advancement;}
    public static void background(float left,float top,float right,float bottom){if(!enabled||activeAdvancement==null)return;lastBoundsAdvancement=activeAdvancement;lastBounds=new Bounds(left,top,right,bottom);frameBoundsAdvancement=activeAdvancement;frameBounds=lastBounds;}
    public static void rendered(Identifier advancement){if(!enabled)return;frames++;lastAdvancement=advancement;frameAdvancement=advancement;activeAdvancement=null;}
    private AdvancementTooltipClientProbe(){}
}
