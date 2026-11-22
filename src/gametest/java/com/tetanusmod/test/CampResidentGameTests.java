package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.expansion.MedicVillage;
import com.tetanusmod.medical.MedicalWorkstations;
import com.tetanusmod.world.MedicStructurePiece;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.test.*;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.*;
import net.minecraft.world.ServerWorldAccess;
import java.util.*;

/** Real native template residents, chunk clipping, bedtime and hostile pathfinding. */
public class CampResidentGameTests implements FabricGameTest {
    private static final String SPACE="tetanusmod:test/camp_empty";
    private record Camp(MedicStructurePiece piece,BlockBox bounds,List<VillagerEntity> villagers,IronGolemEntity guard){}
    @GameTest(templateName=SPACE,batchId="camp_layout",tickLimit=200)
    public void everyRotatedChunkBoundaryHasOnePersistentGuardAndTwoResidents(TestContext ctx)throws Exception{
        for(var rotation:BlockRotation.values())for(int offset:new int[]{0,8}){
            var camp=place(ctx,rotation,offset);var world=ctx.getWorld();
            var entrance=camp.piece.campEntrance();ctx.assertTrue(world.getBlockState(entrance).isAir()&&world.getBlockState(entrance.up()).isAir()&&world.getBlockState(entrance.down()).isSolidBlock(world,entrance.down()),"Rotated camp entrance is not walkable: "+rotation);
            ctx.assertTrue(camp.guard.isPersistent()&&camp.guard.isPlayerCreated()&&MobVariants.permanent(camp.guard)&&camp.guard.getMaxHealth()==200,"Camp guard missing persistence, friendliness or200HP");
            ctx.assertTrue(camp.guard.getPositionTarget().equals(camp.piece.campPatrolHome()),"Guard patrol is not centered on its camp");for(var villager:camp.villagers)ctx.assertTrue(camp.guard.getPositionTarget().getSquaredDistance(villager.getBlockPos())<64,"Guard home range excludes a camp resident");ctx.assertTrue(camp.guard.getPositionTarget().getSquaredDistance(camp.piece.campEntrance())<64,"Guard range excludes the entrance");
            camp.guard.checkDespawn();ctx.assertTrue(camp.guard.isAlive(),"Resident camp guard despawned");
            var saved=camp.guard.writeNbt(new NbtCompound());var loaded=Objects.requireNonNull(EntityType.IRON_GOLEM.create(world));loaded.readNbt(saved);MobVariants.loaded(loaded);
            ctx.assertTrue(loaded.isPersistent()&&MobVariants.permanent(loaded)&&loaded.getMaxHealth()==200&&loaded.getCommandTags().contains(MedicStructurePiece.CAMP_GUARD_TAG),"Guard persistence/variant failed native NBT round trip");
            ctx.assertTrue(loaded.getPositionTarget().equals(camp.guard.getPositionTarget())&&loaded.getPositionTargetRange()==com.tetanusmod.world.CampGuardHome.PATROL_RADIUS,"Camp guard lost its patrol home on native reload");
            var pieceNbt=camp.piece.toNbt(StructureContext.from(world));var reread=new MedicStructurePiece(world.getStructureTemplateManager(),pieceNbt);
            var method=MedicStructurePiece.class.getDeclaredMethod("handleMetadata",String.class,BlockPos.class,ServerWorldAccess.class,net.minecraft.util.math.random.Random.class,BlockBox.class);method.setAccessible(true);
            method.invoke(reread,"guard",camp.guard.getBlockPos(),world,world.random,camp.bounds);
            ctx.assertTrue(world.getEntitiesByClass(IronGolemEntity.class,box(camp.bounds),e->e.getCommandTags().contains(MedicStructurePiece.CAMP_GUARD_TAG)).size()==1,"Reloaded structure metadata duplicated its camp guard");
            var wild=Objects.requireNonNull(EntityType.ZOMBIE.create(world));com.tetanusmod.effect.Infection.silently(()->MobVariants.makePermanent(wild));ctx.assertTrue(!wild.isPersistent(),"Camp-only persistence leaked to ordinary variants");
            remove(camp);
        }
        ctx.complete();
    }
    @GameTest(templateName=SPACE,batchId="camp_sleep",tickLimit=650)
    public void bothNativeVillagersFindTheirWorkstationsAndSeparateBeds(TestContext ctx){
        var world=ctx.getWorld();long oldTime=world.getTimeOfDay();var camp=place(ctx,BlockRotation.NONE,0);world.setTimeOfDay(3000);
        ctx.runAtTick(250,()->{
            try{
                for(var villager:camp.villagers)ctx.assertTrue(villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE).isPresent(),"Resident never found its actual workstation: "+villager.getVillagerData().getProfession());
                world.setTimeOfDay(13000);
            }catch(RuntimeException|Error failure){remove(camp);world.setTimeOfDay(oldTime);throw failure;}
        });
        ctx.runAtTick(600,()->{
            try{
                var homes=new HashSet<BlockPos>();
                for(var villager:camp.villagers){ctx.assertTrue(villager.isSleeping(),"Camp resident did not use its bed at night: "+villager.getVillagerData().getProfession());homes.add(villager.getBrain().getOptionalRegisteredMemory(MemoryModuleType.HOME).orElseThrow().getPos());}
                ctx.assertTrue(homes.size()==2,"Residents claimed the same bed");ctx.complete();
            }finally{remove(camp);world.setTimeOfDay(oldTime);}
        });
    }
    @GameTest(templateName=SPACE,batchId="camp_defence",tickLimit=340)
    public void closedVestibuleProtectsBothResidentsFromAnAggressiveZombie(TestContext ctx){
        var world=ctx.getWorld();long oldTime=world.getTimeOfDay();var camp=place(ctx,BlockRotation.NONE,0);world.setTimeOfDay(13000);
        var farmer=camp.villagers.stream().filter(v->v.getCommandTags().contains("rustruin_camp_support")).findFirst().orElseThrow();
        var zombie=Objects.requireNonNull(EntityType.ZOMBIE.create(world));var outside=camp.piece.getPos().add(6,1,-2);zombie.setPosition(Vec3d.ofBottomCenter(outside));zombie.setCanBreakDoors(true);zombie.setTarget(farmer);world.spawnEntity(zombie);
        ctx.runAtTick(300,()->{
            try{for(var villager:camp.villagers)ctx.assertTrue(villager.isAlive()&&villager.getHealth()==20,"Zombie reached a protected camp resident");ctx.assertTrue(camp.guard.isAlive()&&camp.guard.isPersistent(),"Defending camp guard disappeared");ctx.complete();}
            finally{zombie.discard();remove(camp);world.setTimeOfDay(oldTime);}
        });
    }

    @GameTest(templateName=SPACE,batchId="camp_writer_navigation",tickLimit=200)
    public void villagersNavigateAroundTheRaisedPrescriptionWriter(TestContext ctx) {
        var world = ctx.getWorld();
        for (var rotation : BlockRotation.values()) {
            var camp = place(ctx, rotation, 0);
            try {
                var placement = camp.piece.getPlacementData();
                var anchor = camp.piece.getPos();
                var writer = StructureTemplate.transform(placement, new BlockPos(2, 2, 5)).add(anchor);
                ctx.assertTrue(LandPathNodeMaker.getLandNodeType(world, writer.mutableCopy()) == PathNodeType.BLOCKED,
                        "Native navigation treated the raised writer as a step: " + rotation);

                var approach = StructureTemplate.transform(placement, new BlockPos(2, 1, 4)).add(anchor);
                var beyondWriter = StructureTemplate.transform(placement, new BlockPos(2, 1, 6)).add(anchor);
                var villager = camp.villagers.get(0);
                villager.setPosition(Vec3d.ofBottomCenter(approach));
                villager.setOnGround(true);
                var path = villager.getNavigation().findPathTo(beyondWriter, 0);
                ctx.assertTrue(path != null && path.reachesTarget(),
                        "Villager could not find a path around the raised writer: " + rotation);
            } finally {
                remove(camp);
            }
        }
        ctx.complete();
    }

    @GameTest(templateName=SPACE,batchId="camp_fluid_replacement",tickLimit=200)
    public void dryCampReplacesAmbientWaterWithoutWaterloggingItsFixtures(TestContext ctx){
        var world=ctx.getWorld();
        var clinic=new MedicStructurePiece(world.getStructureTemplateManager(),TetanusMod.id("village/plains_clinic"),ctx.getAbsolutePos(BlockPos.ORIGIN),BlockRotation.NONE);
        ctx.assertTrue(clinic.getPlacementData().shouldPlaceFluids()&&!clinic.getPlacementData().shouldUpdateNeighbors(),"Cave-only replacement changed clinic placement policy");
        var litCrops=new ArrayList<BlockPos>();
        for(var rotation:BlockRotation.values())for(boolean reload:new boolean[]{false,true}){
            var camp=place(ctx,rotation,0,true,reload);
            try{
                ctx.assertTrue(!camp.piece.getPlacementData().shouldPlaceFluids()&&camp.piece.getPlacementData().shouldUpdateNeighbors(),"Camp lost dry/known-shape placement policy after NBT reload="+reload);
                var planned=net.minecraft.structure.StructureTemplate.transform(camp.piece.getPlacementData(),new BlockPos(10,1,4)).add(camp.piece.getPos());
                int fluidCount=0,crops=0;litCrops.clear();
                for(var mutable:BlockPos.iterate(camp.bounds.getMinX(),camp.bounds.getMinY(),camp.bounds.getMinZ(),camp.bounds.getMaxX(),camp.bounds.getMaxY(),camp.bounds.getMaxZ())){
                    var pos=mutable.toImmutable();var state=world.getBlockState(pos);if(state.isOf(Blocks.WHEAT)){crops++;litCrops.add(pos);}if(state.getFluidState().isEmpty())continue;fluidCount++;
                    ctx.assertTrue(pos.equals(planned)&&state.isOf(Blocks.WATER)&&state.getFluidState().isStill(),"Ambient water survived in a dry camp fixture: "+pos+" "+state);
                }
                ctx.assertTrue(fluidCount==1,"Native placement lost or duplicated explicit irrigation water");ctx.assertTrue(crops==8,"Native prepared camp lost its eight crop states: actual="+crops+", rotation="+rotation+", reload="+reload);
                ctx.assertTrue(world.getBlockState(planned.down()).isSolidBlock(world,planned.down()),"Irrigation source has no watertight foundation");
            }finally{remove(camp);}
        }
        var cropsAfterPlacement=List.copyOf(litCrops);
        ctx.runAtTick(40,()->{
            for(var crop:cropsAfterPlacement){
                ctx.assertTrue(world.getBaseLightLevel(crop,0)>=8,"Native camp lanterns did not light their crops: "+crop);
                ctx.assertTrue(world.getBlockState(crop).isOf(Blocks.WHEAT),"Camp wheat disappeared after native lighting/neighbor ticks: "+crop);
                ctx.assertTrue(world.getBlockState(crop.down()).isOf(Blocks.FARMLAND),"Camp wheat lost its hydrated farmland after placement: "+crop);
            }
            ctx.complete();
        });
    }
    private Camp place(TestContext ctx,BlockRotation rotation,int offset){return place(ctx,rotation,offset,false,false);}
    private Camp place(TestContext ctx,BlockRotation rotation,int offset,boolean flooded,boolean reload){
        var world=ctx.getWorld();var min=ctx.getAbsolutePos(new BlockPos(3+offset,2,3+offset));
        var anchor=min.add(rotation==BlockRotation.CLOCKWISE_90||rotation==BlockRotation.CLOCKWISE_180?12:0,0,rotation==BlockRotation.CLOCKWISE_180||rotation==BlockRotation.COUNTERCLOCKWISE_90?12:0);
        var piece=new MedicStructurePiece(world.getStructureTemplateManager(),TetanusMod.id("medic_tent"),anchor,rotation);var bounds=piece.getBoundingBox();
        if(reload)piece=new MedicStructurePiece(world.getStructureTemplateManager(),piece.toNbt(StructureContext.from(world)));
        if(flooded)for(var cell:BlockPos.iterate(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX(),bounds.getMaxY(),bounds.getMaxZ()))world.setBlockState(cell,Blocks.WATER.getDefaultState(),Block.NOTIFY_LISTENERS);
        for(int x=bounds.getMinX()>>4;x<=bounds.getMaxX()>>4;x++)for(int z=bounds.getMinZ()>>4;z<=bounds.getMaxZ()>>4;z++){
            var clip=new BlockBox(x*16,bounds.getMinY(),z*16,x*16+15,bounds.getMaxY(),z*16+15);
            piece.generate(world,world.getStructureAccessor(),world.getChunkManager().getChunkGenerator(),world.random,clip,new ChunkPos(x,z),anchor);
        }
        int foundation=0;var exactPlacement=piece.getPlacementData().copy().setBoundingBox(null);
        for(var material:List.of(Blocks.STONE_BRICKS,Blocks.MOSSY_STONE_BRICKS,Blocks.POLISHED_ANDESITE))for(var info:piece.getTemplate().getInfosForBlock(piece.getPos(),exactPlacement,material)){
            if(info.pos().getY()!=piece.getPos().getY())continue;foundation++;
            ctx.assertTrue(world.getBlockState(info.pos()).equals(info.state()),"Native camp foundation differs from its authored template: "+info.pos());
        }
        ctx.assertTrue(foundation==169,"Template does not provide a complete169-block constructed foundation");
        var writers=piece.getTemplate().getInfosForBlock(piece.getPos(),exactPlacement,MedicalWorkstations.WRITER);
        ctx.assertTrue(writers.size()==1,"Native camp lost its Prescription Writer: "+rotation);
        var writer=writers.get(0);var aisle=writer.pos().offset(writer.state().get(HorizontalFacingBlock.FACING)).down();
        ctx.assertTrue(world.getBlockState(writer.pos()).equals(writer.state())&&world.getBlockState(writer.pos().down()).isSolidBlock(world,writer.pos().down()),"Native camp writer is missing/misoriented or unsupported: "+rotation);
        ctx.assertTrue(world.isAir(aisle)&&world.isAir(aisle.up())&&world.getBlockState(aisle.down()).isSolidBlock(world,aisle.down()),"Native camp writer blocks its Survival approach: "+rotation);
        var villagers=world.getEntitiesByClass(VillagerEntity.class,box(bounds),v->v.getCommandTags().contains("rustruin_camp_medic")||v.getCommandTags().contains("rustruin_camp_support"));
        var guards=world.getEntitiesByClass(IronGolemEntity.class,box(bounds),v->v.getCommandTags().contains(MedicStructurePiece.CAMP_GUARD_TAG));
        ctx.assertTrue(villagers.size()==2&&guards.size()==1,"Clipped camp lost or duplicated its villagers/guard: "+rotation+" / "+offset);
        ctx.assertTrue(villagers.stream().filter(v->v.getVillagerData().getProfession()==MedicVillage.MEDIC).count()==1,"Field medic not present");
        return new Camp(piece,bounds,villagers,guards.get(0));
    }
    private static Box box(BlockBox bounds){return new Box(bounds.getMinX(),bounds.getMinY(),bounds.getMinZ(),bounds.getMaxX()+1,bounds.getMaxY()+1,bounds.getMaxZ()+1);}
    private static void remove(Camp camp){camp.villagers.forEach(Entity::discard);camp.guard.discard();}
}
