package com.tetanusmod.world;

import com.mojang.serialization.Codec;
import com.tetanusmod.TetanusMod;
import net.minecraft.registry.*;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.*;
import java.util.*;

/** Shares vanilla village candidate chunks; its structure-set frequency is 55%. */
public final class ClinicStructure extends Structure {
    public static final Codec<ClinicStructure> CODEC=createCodec(ClinicStructure::new);
    private static final String[] TYPES={"plains","desert","savanna","snowy","taiga"};
    private static final int[][] OFFSETS={{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    public ClinicStructure(Config config){super(config);}
    @Override public StructureType<?> getType(){return MedicStructures.CLINIC;}
    @Override protected Optional<StructurePosition> getStructurePosition(Context context){
        var structures=context.dynamicRegistryManager().get(RegistryKeys.STRUCTURE);
        for(String type:TYPES){
            var village=structures.get(new Identifier("minecraft","village_"+type));
            if(village==null)continue;
            StructureStart parent=village.createStructureStart(context.dynamicRegistryManager(),context.chunkGenerator(),context.biomeSource(),
                    context.noiseConfig(),context.structureTemplateManager(),context.seed(),context.chunkPos(),0,context.world(),village.getValidBiomes()::contains);
            if(!parent.hasChildren())continue;
            var position=findSite(context,parent.getChildren());
            if(position.isEmpty())return Optional.empty();
            var pos=position.get();var template=TetanusMod.id("village/"+type+"_clinic");
            return Optional.of(new StructurePosition(pos,collector->collector.addPiece(new MedicStructurePiece(context.structureTemplateManager(),template,pos,BlockRotation.NONE))));
        }
        return Optional.empty();
    }
    private Optional<BlockPos> findSite(Context context,List<StructurePiece> pieces){
        var root=pieces.get(0).getBoundingBox().getCenter();
        for(int radius:new int[]{24,36,48,64,80,96}){
            for(var offset:OFFSETS){
                int x=root.getX()+offset[0]*radius-4,z=root.getZ()+offset[1]*radius-6;
                // Native references inspect only eight neighboring start chunks. A
                // displaced village root must not put any clinic block beyond that.
                if(!withinReferenceWindow(context.chunkPos(),x,z))continue;
                // Native heights are the first air block. The template has TWO floor
                // layers: origin+1 is the walking surface block, origin+2 its top.
                int entry=height(context,x+4,z);
                if(entry<context.chunkGenerator().getSeaLevel())continue;
                int min=entry,max=entry;boolean dry=true;
                for(int dx:new int[]{1,4,7})for(int dz:new int[]{1,6,11}){
                    int ground=height(context,x+dx,z+dz);min=Math.min(min,ground);max=Math.max(max,ground);
                    var floor=context.chunkGenerator().getColumnSample(x+dx,z+dz,context.world(),context.noiseConfig()).getState(ground-1);
                    if(!floor.getFluidState().isEmpty()||!floor.isSolidBlock(net.minecraft.world.EmptyBlockView.INSTANCE,new BlockPos(x+dx,ground-1,z+dz)))dry=false;
                }
                // Reject steep plots rather than leaving a tall, inaccessible plinth.
                if(!dry||max-min>1)continue;
                var pos=new BlockPos(x,entry-2,z);
                var box=new BlockBox(x-1,pos.getY(),z-1,x+9,pos.getY()+10,z+13);
                if(StructurePiece.firstIntersecting(pieces,box)!=null)continue;
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }
    private static boolean withinReferenceWindow(ChunkPos start,int x,int z){
        return (x>>4)>=start.x-8&&((x+8)>>4)<=start.x+8
                &&(z>>4)>=start.z-8&&((z+12)>>4)<=start.z+8;
    }
    private static int height(Context context,int x,int z){
        return context.chunkGenerator().getHeightOnGround(x,z,Heightmap.Type.WORLD_SURFACE_WG,context.world(),context.noiseConfig());
    }
}
