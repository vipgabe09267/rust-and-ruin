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
    public ClinicStructure(Config config){super(config);}
    @Override public StructureType<?> getType(){return MedicStructures.CLINIC;}
    @Override protected Optional<StructurePosition> getStructurePosition(Context context){
        var structures=context.dynamicRegistryManager().get(RegistryKeys.STRUCTURE);
        for(String type:TYPES){
            var village=structures.get(new Identifier("minecraft","village_"+type));
            if(village==null)continue;
            // Reconstruct the deterministic village layout without reading or generating neighbor chunks.
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
        // Prefer a vacant plot close to existing paths, then try the village's outer edge.
        for(int radius:new int[]{24,36,48,64,80,96}){
            for(int side=0;side<8;side++){
                int[][] offsets={{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
                int x=root.getX()+offsets[side][0]*radius-4,z=root.getZ()+offsets[side][1]*radius-6;
                int y=context.chunkGenerator().getHeightOnGround(x+4,z+6,Heightmap.Type.WORLD_SURFACE_WG,context.world(),context.noiseConfig());
                var pos=new BlockPos(x,y-1,z);var box=new BlockBox(x-1,y-1,z-1,x+9,y+11,z+13);
                if(y<context.chunkGenerator().getSeaLevel()||StructurePiece.firstIntersecting(pieces,box)!=null)continue;
                int corner=context.chunkGenerator().getHeightOnGround(x+8,z+12,Heightmap.Type.WORLD_SURFACE_WG,context.world(),context.noiseConfig());
                if(Math.abs(corner-y)>4)continue;
                var floor=context.chunkGenerator().getColumnSample(x+4,z+6,context.world(),context.noiseConfig()).getState(y-1);
                if(!floor.getFluidState().isEmpty())continue;
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }
}
