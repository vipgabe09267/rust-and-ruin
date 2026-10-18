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
        return CACHE.computeIfAbsent(original,TetanizedTextures::create);
    }
    private static Identifier create(Identifier original) {
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
                int noise=Math.floorMod(x*73856093^y*19349663^original.hashCode(),101);
                boolean rust=noise<24 && value>35;
                int nr=rust?Math.min(230,60+value*3/4):(r+value*2)/3;
                int ng=rust?25+value/3:(g+value*2)/4;
                int nb=rust?12+value/8:(b+value)/3;
                image.setColor(x,y,(alpha<<24)|(nb<<16)|(ng<<8)|nr);
            }
            Identifier id=TetanusMod.id("tetanized/"+original.getNamespace()+"/"+original.getPath());
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
    }
}