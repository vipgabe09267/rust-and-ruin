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
    public static final String CAMP_GUARD_TAG="rustruin_camp_guard";
    private boolean campGuardSpawned;
    public MedicStructurePiece(StructureTemplateManager manager,Identifier template,BlockPos pos,BlockRotation rotation){
        super(MedicStructures.PIECE,0,manager,template,template.toString(),data(rotation,template),pos);
    }
    public MedicStructurePiece(StructureTemplateManager manager,NbtCompound nbt){
        super(MedicStructures.PIECE,nbt,manager,id->data(rotation(nbt),id));
        campGuardSpawned=nbt.getBoolean("CampGuardSpawned");
    }
    /** Walkable gate vestibule, using the same native rotation as the template. */
    public BlockPos campEntrance(){return StructureTemplate.transform(placementData,new BlockPos(6,1,1)).add(pos);}
    /** Full porch floor, including the native template rotation. */
    public BlockPos clinicEntrance(){return StructureTemplate.transform(placementData,new BlockPos(4,2,1)).add(pos);}
    public BlockPos campPatrolHome(){return StructureTemplate.transform(placementData,new BlockPos(6,1,6)).add(pos);}
    private static BlockRotation rotation(NbtCompound nbt){
        try{return BlockRotation.valueOf(nbt.getString("MedicRotation"));}catch(IllegalArgumentException invalid){return BlockRotation.NONE;}
    }
    private static StructurePlacementData data(BlockRotation rotation,Identifier template){
        var placement=new StructurePlacementData().setRotation(rotation).setIgnoreEntities(false)
                .addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS)
                .addProcessor(JigsawReplacementStructureProcessor.INSTANCE);
        // Late cave decoration can contain pools. A dry camp must replace those
        // fluids rather than waterlogging its fences, chest and light fixtures.
        // The water source explicitly present in the template is still placed.
        if(template.equals(com.tetanusmod.TetanusMod.id("medic_tent"))){
            placement.setPlaceFluids(false);
            // Every connection/half is authored in the camp template. Native
            // neighbor postprocessing runs before chunk LIGHT and would reject
            // its underground crops while their lantern light is still zero.
            placement.setUpdateNeighbors(true);
        }
        return placement;
    }
    @Override protected void writeNbt(StructureContext context,NbtCompound nbt){super.writeNbt(context,nbt);nbt.putString("MedicRotation",placementData.getRotation().name());nbt.putBoolean("CampGuardSpawned",campGuardSpawned);}
    @Override protected void handleMetadata(String metadata,BlockPos pos,ServerWorldAccess world,Random random,BlockBox bounds){
        if(!metadata.equals("guard")||!bounds.contains(pos)||campGuardSpawned)return;
        // The template processor skips DATA markers, so remove any original terrain here.
        world.setBlockState(pos,net.minecraft.block.Blocks.AIR.getDefaultState(),net.minecraft.block.Block.NOTIFY_LISTENERS);
        var golem=EntityType.IRON_GOLEM.create(world.toServerWorld());
        if(golem==null)return;
        golem.refreshPositionAndAngles(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,0,0);
        golem.initialize(world,world.getLocalDifficulty(pos),SpawnReason.STRUCTURE,null,null);
        golem.setPlayerCreated(true);
        golem.setPersistent();golem.addCommandTag(CAMP_GUARD_TAG);((CampGuardHome)golem).rustruin$campHome(campPatrolHome());
        com.tetanusmod.effect.Infection.silently(()->MobVariants.makePermanent(golem));
        campGuardSpawned=world.spawnEntity(golem);
        if(!campGuardSpawned)org.slf4j.LoggerFactory.getLogger("tetanusmod").warn("Could not place the camp guardian at {}",pos);
    }
}
