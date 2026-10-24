package com.tetanusmod.world;

import com.tetanusmod.entity.MobVariants;
import net.minecraft.entity.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.*;
import net.minecraft.structure.processor.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;

/** Uses native clipped structure placement and entity spawning; no chunk-load placement hooks. */
public final class MedicStructurePiece extends SimpleStructurePiece {
    public MedicStructurePiece(StructureTemplateManager manager,Identifier template,BlockPos pos,BlockRotation rotation){
        super(MedicStructures.PIECE,0,manager,template,template.toString(),data(rotation),pos);
    }
    public MedicStructurePiece(StructureTemplateManager manager,NbtCompound nbt){
        super(MedicStructures.PIECE,nbt,manager,id->data(rotation(nbt)));
    }
    private static BlockRotation rotation(NbtCompound nbt){
        try{return BlockRotation.valueOf(nbt.getString("MedicRotation"));}catch(IllegalArgumentException invalid){return BlockRotation.NONE;}
    }
    private static StructurePlacementData data(BlockRotation rotation){
        return new StructurePlacementData().setRotation(rotation).setIgnoreEntities(false)
                .addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS)
                .addProcessor(JigsawReplacementStructureProcessor.INSTANCE);
    }
    @Override protected void writeNbt(StructureContext context,NbtCompound nbt){super.writeNbt(context,nbt);nbt.putString("MedicRotation",placementData.getRotation().name());}
    @Override protected void handleMetadata(String metadata,BlockPos pos,ServerWorldAccess world,Random random,BlockBox bounds){
        if(!metadata.equals("guard")||!bounds.contains(pos))return;
        // The template processor skips DATA markers, so remove any original terrain here.
        world.setBlockState(pos,net.minecraft.block.Blocks.AIR.getDefaultState(),net.minecraft.block.Block.NOTIFY_LISTENERS);
        var golem=EntityType.IRON_GOLEM.create(world.toServerWorld());
        if(golem==null)return;
        golem.refreshPositionAndAngles(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,0,0);
        golem.initialize(world,world.getLocalDifficulty(pos),SpawnReason.STRUCTURE,null,null);
        golem.setPlayerCreated(true);
        MobVariants.makePermanent(golem);
        world.spawnEntity(golem);
    }
}
