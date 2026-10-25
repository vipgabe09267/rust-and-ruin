package com.tetanusmod.world;

import com.mojang.serialization.Codec;
import com.tetanusmod.TetanusMod;
import net.minecraft.block.BlockState;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** One camp per widely separated native grid cell, with a 15% roll at each suitable cave site. */
public final class CaveMedicStructure extends Structure {
    public static final Codec<CaveMedicStructure> CODEC=createCodec(CaveMedicStructure::new);
    private static final float SITE_CHANCE=0.15F;
    private static final int SIZE=13, HEIGHT=8;
    public CaveMedicStructure(Config config){super(config);}
    @Override public StructureType<?> getType(){return MedicStructures.TENT;}

    @Override protected Optional<StructurePosition> getStructurePosition(Context context){
        // Sampling stays inside the original grid candidate's 28x28-block search area.
        // Keeping its salt, spacing and region alignment also preserves spacing from old camps.
        int[] sites=new int[16];for(int i=0;i<sites.length;i++)sites[i]=i;
        for(int i=sites.length-1;i>0;i--){int other=context.random().nextInt(i+1),value=sites[i];sites[i]=sites[other];sites[other]=value;}
        var columns=new Columns(context);
        for(int site:sites){
            int x=context.chunkPos().getStartX()+(site%4)*4,z=context.chunkPos().getStartZ()+(site/4)*4;
            var center=columns.at(x+6,z+6);
            int surface=context.chunkGenerator().getHeight(x+6,z+6,Heightmap.Type.WORLD_SURFACE_WG,context.world(),context.noiseConfig());
            int highest=Math.min(45,surface-18),lowest=Math.max(-56,context.world().getBottomY()+8);
            for(int y=highest;y>=lowest;y--){
                // An actual natural floor gives this site one roll, rather than several rolls
                // for the same air pocket at adjacent heights above a recessed floor.
                if(!solid(center.getState(y),y)||!air(center,y,7))continue;
                var biome=context.biomeSource().getBiome((x+6)>>2,(y+3)>>2,(z+6)>>2,context.noiseConfig().getMultiNoiseSampler());
                if(!getValidBiomes().contains(biome)||!supportedCavern(columns,x,y,z))continue;
                BlockRotation rotation=entrance(columns,x,y,z);
                if(rotation==null||!dryRoom(columns,x,y,z)||context.random().nextFloat()>=SITE_CHANCE)continue;
                var anchor=switch(rotation){
                    case NONE->new BlockPos(x,y,z);
                    case CLOCKWISE_90->new BlockPos(x+12,y,z);
                    case CLOCKWISE_180->new BlockPos(x+12,y,z+12);
                    case COUNTERCLOCKWISE_90->new BlockPos(x,y,z+12);
                };
                return Optional.of(new StructurePosition(new BlockPos(x+6,y+2,z+6),collector->collector.addPiece(
                    new MedicStructurePiece(context.structureTemplateManager(),TetanusMod.id("medic_tent"),anchor,rotation))));
            }
        }
        return Optional.empty();
    }

    private static boolean supportedCavern(Columns columns,int x,int y,int z){
        int open=0;
        for(int dx:new int[]{1,6,11})for(int dz:new int[]{1,6,11}){
            var column=columns.at(x+dx,z+dz);
            if(!supported(column,y))return false;
            if(air(column,y,5))open++;
        }
        return open>=5;
    }

    /** The entrance must open into the existing cave, rather than a sealed excavated pocket. */
    private static BlockRotation entrance(Columns columns,int x,int y,int z){
        for(var rotation:BlockRotation.values()){
            boolean open=true;
            for(int dx=4;dx<=8;dx++){
                int px=x+dx,pz=z-1;
                switch(rotation){
                    case CLOCKWISE_90->{px=x+13;pz=z+dx;}
                    case CLOCKWISE_180->{px=x+12-dx;pz=z+13;}
                    case COUNTERCLOCKWISE_90->{px=x-1;pz=z+12-dx;}
                    default->{}
                }
                var column=columns.at(px,pz);
                if(!supported(column,y)||!air(column,y,3)){open=false;break;}
            }
            if(open)return rotation;
        }
        return null;
    }

    /** Inspect every occupied block and its wet exterior, with bounded edge/roof excavation. */
    private static boolean dryRoom(Columns columns,int x,int y,int z){
        int excavated=0;
        for(int dx=-1;dx<=SIZE;dx++)for(int dz=-1;dz<=SIZE;dz++){
            var column=columns.at(x+dx,z+dz);
            for(int dy=0;dy<=HEIGHT;dy++){
                var state=column.getState(y+dy);
                if(!state.getFluidState().isEmpty())return false;
                if(dx<0||dx>=SIZE||dz<0||dz>=SIZE||dy==0||dy==HEIGHT||state.isAir())continue;
                // Keep a naturally open interior regardless of the final tent's rotation.
                if(dx>=3&&dx<=9&&dz>=3&&dz<=9&&dy<=4)return false;
                if(++excavated>SIZE*SIZE*(HEIGHT-1)/3)return false;
            }
        }
        return true;
    }

    private static boolean supported(VerticalBlockSample column,int y){
        for(int depth=0;depth<4;depth++)if(solid(column.getState(y-depth),y-depth))return true;
        return false;
    }
    private static boolean solid(BlockState state,int y){return state.isSolidBlock(EmptyBlockView.INSTANCE,new BlockPos(0,y,0));}
    private static boolean air(VerticalBlockSample column,int floor,int height){
        for(int dy=1;dy<=height;dy++)if(!column.getState(floor+dy).isAir())return false;
        return true;
    }
    /** Per-attempt cache only: no retained chunks, seeds, worlds, or shared generation state. */
    private static final class Columns {
        private final Context context;
        private final Map<Long,VerticalBlockSample> samples=new HashMap<>();
        private Columns(Context context){this.context=context;}
        private VerticalBlockSample at(int x,int z){
            return samples.computeIfAbsent(ChunkPos.toLong(x,z),key->context.chunkGenerator().getColumnSample(x,z,context.world(),context.noiseConfig()));
        }
    }
}