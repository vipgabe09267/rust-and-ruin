package com.tetanusmod.world;

import com.mojang.serialization.Codec;
import com.tetanusmod.TetanusMod;
import net.minecraft.block.BlockState;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** One camp per widely separated native grid cell, with a 15% roll at each suitable cave site. */
public final class CaveMedicStructure extends Structure {
    public static final Codec<CaveMedicStructure> CODEC=createCodec(CaveMedicStructure::new);
    private static final float SITE_CHANCE=0.15F;
    private static final int SIZE=13, HEIGHT=8, MAX_SITE_OFFSET=48, SITE_STEP=4;
    public CaveMedicStructure(Config config){super(config);}
    @Override public StructureType<?> getType(){return MedicStructures.TENT;}

    @Override protected Optional<StructurePosition> getStructurePosition(Context context){return findSite(context,MAX_SITE_OFFSET);}

    private Optional<StructurePosition> findSite(Context context,int maxOffset){
        // The grid's nearest candidate anchors are 2,064 blocks apart. Origins within
        // 0..48 plus a 13-block template keep even the nearest footprint edges 2,004
        // blocks apart, including saved camps from the previous smaller search.
        // The full search covers 61x61 blocks and at most 63x63 cached columns.
        int steps=maxOffset/SITE_STEP+1,legacySteps=Math.min(steps,4);
        var columns=new Columns(context);columns.fullSampling=true;
        int[] legacy=new int[legacySteps*legacySteps];for(int i=0;i<legacy.length;i++)legacy[i]=i;
        shuffle(context,legacy);
        var found=scanSites(context,columns,legacy,legacySteps,false);
        if(found.isPresent()||legacySteps==steps)return found;
        // Do not consume fallback randomness before the original 16-site attempt.
        // Every previously successful native start retains its exact position/rotation.
        int[] ring=new int[steps*steps-legacy.length];int count=0;
        for(int i=0;i<steps*steps;i++)if(i%steps>=legacySteps||i/steps>=legacySteps)ring[count++]=i;
        shuffle(context,ring);
        columns.fullSampling=false;
        return scanSites(context,columns,ring,steps,true);
    }

    private static void shuffle(Context context,int[] sites){
        for(int i=sites.length-1;i>0;i--){int other=context.random().nextInt(i+1),value=sites[i];sites[i]=sites[other];sites[other]=value;}
    }

    private Optional<StructurePosition> scanSites(Context context,Columns columns,int[] sites,int steps,boolean needsFullProof){
        for(int site:sites){
            int x=context.chunkPos().getStartX()+(site%steps)*SITE_STEP,z=context.chunkPos().getStartZ()+(site/steps)*SITE_STEP;
            var center=columns.center(x+6,z+6);
            int surface=surfaceHeight(center,context.world());
            int highest=Math.min(45,surface-18),lowest=Math.max(-56,context.world().getBottomY()+8);
            for(int y=highest;y>=lowest;y--){
                // An actual natural floor gives this site one roll, rather than several rolls
                // for the same air pocket at adjacent heights above a recessed floor.
                if(!solid(center.getState(y),y)||!air(center,y,7))continue;
                var biome=context.biomeSource().getBiome((x+6)>>2,(y+3)>>2,(z+6)>>2,context.noiseConfig().getMultiNoiseSampler());
                if(!getValidBiomes().contains(biome)||!supportedCavern(columns,x,y,z))continue;
                BlockRotation rotation=entrance(columns,x,y,z);
                if(rotation==null||!dryRoom(columns,x,y,z)||context.random().nextFloat()>=SITE_CHANCE)continue;
                // Bounded samples may differ around aquifer surface estimates. They only
                // screen fallback candidates: every would-be placement is proven again
                // against full native columns after its independent 15% roll succeeds.
                BlockRotation proven=needsFullProof?proveFullSite(context,columns,x,y,z):rotation;
                if(proven==null)continue;
                var anchor=switch(proven){
                    case NONE->new BlockPos(x,y,z);
                    case CLOCKWISE_90->new BlockPos(x+12,y,z);
                    case CLOCKWISE_180->new BlockPos(x+12,y,z+12);
                    case COUNTERCLOCKWISE_90->new BlockPos(x,y,z+12);
                };
                return Optional.of(new StructurePosition(new BlockPos(x+6,y+2,z+6),collector->collector.addPiece(
                    new MedicStructurePiece(context.structureTemplateManager(),TetanusMod.id("medic_tent"),anchor,proven))));
            }
        }
        return Optional.empty();
    }

    private BlockRotation proveFullSite(Context context,Columns columns,int x,int y,int z){
        boolean previous=columns.fullSampling;columns.fullSampling=true;
        try{
            var center=columns.center(x+6,z+6);
            if(y>Math.min(45,surfaceHeight(center,context.world())-18)||!solid(center.getState(y),y)||!air(center,y,7)||!supportedCavern(columns,x,y,z))return null;
            var rotation=entrance(columns,x,y,z);
            return rotation!=null&&dryRoom(columns,x,y,z)?rotation:null;
        }finally{columns.fullSampling=previous;}
    }
    /** WORLD_SURFACE_WG uses the first non-air block, including fluids, plus one. */
    private static int surfaceHeight(VerticalBlockSample column,HeightLimitView world){
        for(int y=world.getTopY()-1;y>=world.getBottomY();y--)if(!column.getState(y).isAir())return y+1;
        return world.getBottomY();
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
        private final Set<Long> fullColumns=new HashSet<>();
        private HeightLimitView caveRange;
        private boolean fullSampling;
        private Columns(Context context){this.context=context;}
        private HeightLimitView caveRange(){
            if(caveRange==null){
                // Fit checks only read Y -59..53. Keep aligned padding for native noise
                // interpolation/aquifers, while respecting a dimension's actual limits.
                int bottom=Math.max(context.world().getBottomY(),-64),top=Math.min(context.world().getTopY(),64);
                caveRange=top>bottom?HeightLimitView.create(bottom,top-bottom):context.world();
            }
            return caveRange;
        }
        private VerticalBlockSample at(int x,int z){
            if(fullSampling)return center(x,z);
            return samples.computeIfAbsent(ChunkPos.toLong(x,z),key->context.chunkGenerator().getColumnSample(x,z,caveRange(),context.noiseConfig()));
        }
        private VerticalBlockSample center(int x,int z){
            long key=ChunkPos.toLong(x,z);
            // An earlier neighbor-fit lookup may have cached this column at limited
            // height. Upgrade it before using the full column to establish surface cover.
            if(!fullColumns.contains(key)){
                samples.put(key,context.chunkGenerator().getColumnSample(x,z,context.world(),context.noiseConfig()));fullColumns.add(key);
            }
            return samples.get(key);
        }
    }
}