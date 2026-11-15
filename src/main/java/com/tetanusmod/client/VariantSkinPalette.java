package com.tetanusmod.client;

/** Pure ABGR palette conversion shared by runtime textures and pixel regressions. */
public final class VariantSkinPalette {
    private VariantSkinPalette() { }
    public static int tetanized(int pixel,int x,int y,int seed,boolean enderman) {
        int alpha=pixel>>>24;
        if(alpha==0)return pixel;
        int r=pixel&255,g=(pixel>>>8)&255,b=(pixel>>>16)&255;
        int value=(r*3+g*5+b*2)/10;
        int noise=Math.floorMod(x*73856093^y*19349663^seed,101);
        if(enderman) {
            // Endermen are almost black, below the ordinary skin's brightness gate.
            // Keep their purple face detail and emissive eye layer, but make rust readable.
            if(value>70&&r+b>g*3)return pixel;
            int patch=Math.floorMod((x/4)*83492791^(y/3)*297121507^seed,101);
            boolean rust=patch<28||(patch<65&&noise<24);
            int nr=rust?Math.min(190,105+value+noise%36):Math.min(55,(r+value*2)/3+7);
            int ng=rust?Math.min(94,38+value/2+noise%19):Math.min(49,(g+value*2)/4+5);
            int nb=rust?Math.min(53,16+value/4+noise%11):Math.min(48,(b+value)/3+4);
            return (alpha<<24)|(nb<<16)|(ng<<8)|nr;
        }
        boolean rust=noise<24&&value>35;
        int nr=rust?Math.min(230,60+value*3/4):(r+value*2)/3;
        int ng=rust?25+value/3:(g+value*2)/4;
        int nb=rust?12+value/8:(b+value)/3;
        return (alpha<<24)|(nb<<16)|(ng<<8)|nr;
    }
}
