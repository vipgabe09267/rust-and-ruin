package com.tetanusmod.world;

import com.mojang.serialization.Codec;
import com.tetanusmod.TetanusMod;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.structure.*;
import java.util.*;

/** Native structure grid: 15% candidate frequency, minimum horizontal spacing above 2,000 blocks. */
public final class CaveMedicStructure extends Structure {
    public static final Codec<CaveMedicStructure> CODEC=createCodec(CaveMedicStructure::new);
    public CaveMedicStructure(Config config){super(config);}
    @Override public StructureType<?> getType(){return MedicStructures.TENT;}
    @Override protected Optional<StructurePosition> getStructurePosition(Context context){
        int x=context.chunkPos().getStartX()+2,z=context.chunkPos().getStartZ()+2;
        var center=context.chunkGenerator().getColumnSample(x+6,z+6,context.world(),context.noiseConfig());
        int surface=context.chunkGenerator().getHeight(x+6,z+6,Heightmap.Type.WORLD_SURFACE_WG,context.world(),context.noiseConfig());
        int max=Math.min(45,surface-18),min=Math.max(-48,context.world().getBottomY()+8);
        VerticalBlockSample[] columns=null;
        for(int y=max;y>=min;y--){
            if(!clear(center,y))continue;
            var biome=context.biomeSource().getBiome((x+6)>>2,(y+3)>>2,(z+6)>>2,context.noiseConfig().getMultiNoiseSampler());
            if(!getValidBiomes().contains(biome))continue;
            if(columns==null){
                columns=new VerticalBlockSample[9];int i=0;
                for(int dx:new int[]{1,6,11})for(int dz:new int[]{1,6,11})columns[i++]=context.chunkGenerator().getColumnSample(x+dx,z+dz,context.world(),context.noiseConfig());
            }
            boolean fits=true;for(var column:columns)if(!clear(column,y)){fits=false;break;}
            if(!fits)continue;
            var pos=new BlockPos(x,y,z);
            return Optional.of(new StructurePosition(pos.add(6,2,6),collector->collector.addPiece(new MedicStructurePiece(context.structureTemplateManager(),TetanusMod.id("medic_tent"),pos,BlockRotation.NONE))));
        }
        return Optional.empty();
    }
    private static boolean clear(VerticalBlockSample column,int floor){
        boolean support=false;
        for(int depth=0;depth<4;depth++)if(column.getState(floor-depth).isSolidBlock(net.minecraft.world.EmptyBlockView.INSTANCE,new BlockPos(0,floor-depth,0))){support=true;break;}
        if(!support)return false;
        for(int dy=1;dy<=7;dy++)if(!column.getState(floor+dy).isAir())return false;
        return true;
    }
}
