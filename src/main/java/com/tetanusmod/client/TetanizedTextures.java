package com.tetanusmod.client;

import com.tetanusmod.TetanusMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.*;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;

/** Recolors each original UV map; animal faces, fur, scales and spider eyes stay aligned. */
public final class TetanizedTextures {
    private static final Map<Identifier,Identifier> CACHE=new HashMap<>();
    public static Identifier get(Identifier original) {
        return CACHE.computeIfAbsent(original,id->create(id,false));
    }
    private static final Map<Identifier,Identifier> RABID_CACHE=new HashMap<>();
    public static Identifier rabid(Identifier original){return RABID_CACHE.computeIfAbsent(original,id->create(id,true));}
    private static Identifier create(Identifier original,boolean rabid) {
        var client=MinecraftClient.getInstance();
        var resource=client.getResourceManager().getResource(original);
        if(resource.isEmpty())return original;
        try(var stream=resource.get().getInputStream()) {
            NativeImage image=NativeImage.read(stream);
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
                int pixel=image.getColor(x,y),alpha=pixel>>>24;
                if(alpha==0)continue;
                int r=pixel&255,g=(pixel>>>8)&255,b=(pixel>>>16)&255;
                int value=(r*3+g*5+b*2)/10;
                int transformed=VariantSkinPalette.tetanized(pixel,x,y,original.hashCode(),(original.getPath().equals("textures/entity/enderman/enderman.png")||original.getPath().equals("textures/entity/enderdragon/dragon.png")));
                int nr=transformed&255;
                int ng=(transformed>>>8)&255;
                int nb=(transformed>>>16)&255;
                if(rabid){
                    nr=(r*2+value*3)/5;ng=(g*2+value*3)*17/100;nb=(b*2+value*3)*17/100;
                    int patch=Math.floorMod((x/3)*83492791^(y/3)*297121507^original.hashCode(),101);
                    if(patch<18&&value>45){nr=Math.min(220,40+value*4/5);ng=value*3/10;nb=value/4;}
                    if(value>215&&patch>=18){nr=230;ng=222;nb=194;}
                }
                image.setColor(x,y,(alpha<<24)|(nb<<16)|(ng<<8)|nr);
            }
            Identifier id=TetanusMod.id((rabid?"rabid/":"tetanized/")+original.getNamespace()+"/"+original.getPath());
            client.getTextureManager().registerTexture(id,new NativeImageBackedTexture(image));
            return id;
        }catch(Exception exception) {
            org.slf4j.LoggerFactory.getLogger("tetanusmod").warn("Could not prepare tetanized skin {}",original,exception);
            return original;
        }
    }
    public static void clear() {
        var manager=MinecraftClient.getInstance().getTextureManager();
        CACHE.forEach((source,generated)->{if(!source.equals(generated))manager.destroyTexture(generated);});
        CACHE.clear();
        RABID_CACHE.forEach((source,generated)->{if(!source.equals(generated))manager.destroyTexture(generated);});RABID_CACHE.clear();
    }
}