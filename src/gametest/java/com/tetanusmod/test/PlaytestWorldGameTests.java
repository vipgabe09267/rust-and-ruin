package com.tetanusmod.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.expansion.*;
import com.tetanusmod.world.MedicStructurePiece;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.*;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.noise.NoiseConfig;

/** Regressions for the owner's October 24 placement and discovery reports. */
public class PlaytestWorldGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=1200)
    public void clinicEntranceMatchesNaturalGround(TestContext ctx){
        var world=ctx.getWorld();var registries=world.getRegistryManager();
        var ops=RegistryOps.of(JsonOps.INSTANCE,registries);
        var generator=ChunkGenerator.CODEC.parse(ops,JsonParser.parseString("{\"type\":\"minecraft:noise\",\"settings\":\"minecraft:overworld\",\"biome_source\":{\"type\":\"minecraft:multi_noise\",\"preset\":\"minecraft:overworld\"}}")).result().orElseThrow();
        long seed=246813579L;
        var settings=registries.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS).get(ChunkGeneratorSettings.OVERWORLD);
        var noise=NoiseConfig.create(settings,registries.getWrapperOrThrow(RegistryKeys.NOISE_PARAMETERS),seed);
        var grid=(RandomSpreadStructurePlacement)registries.get(RegistryKeys.STRUCTURE_SET).get(TetanusMod.id("medic_clinics")).placement();
        var clinic=registries.get(RegistryKeys.STRUCTURE).get(TetanusMod.id("medic_clinic"));int checked=0;
        for(int region=0;region<64&&checked<3;region++){
            var chunk=grid.getStartChunk(seed,(region%8-4)*34,(region/8-4)*34);
            var start=clinic.createStructureStart(registries,generator,generator.getBiomeSource(),noise,world.getStructureTemplateManager(),seed,chunk,0,world,clinic.getValidBiomes()::contains);
            if(!start.hasChildren())continue;
            var piece=(MedicStructurePiece)start.getChildren().get(0);var pos=piece.getPos();
            int front=generator.getHeightOnGround(pos.getX()+4,pos.getZ(),Heightmap.Type.WORLD_SURFACE_WG,world,noise);
            ctx.assertTrue(pos.getY()+2==front,"Clinic porch stands above or below its natural entrance: "+pos+" / "+front);
            int min=front,max=front;
            for(int dx:new int[]{1,4,7})for(int dz:new int[]{1,6,11}){
                int ground=generator.getHeightOnGround(pos.getX()+dx,pos.getZ()+dz,Heightmap.Type.WORLD_SURFACE_WG,world,noise);
                min=Math.min(min,ground);max=Math.max(max,ground);
            }
            ctx.assertTrue(max-min<=1,"Clinic accepted a steep plot with an exposed tall foundation");checked++;
        }
        ctx.assertTrue(checked>0,"No clinic could be checked in deterministic natural terrain");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void caveTemplateMarksItsMedicAndDedicatedWorkstation(TestContext ctx){
        var template=ctx.getWorld().getStructureTemplateManager().getTemplate(TetanusMod.id("medic_tent")).orElseThrow();
        var data=template.writeNbt(new NbtCompound());var entities=data.getList("entities",10);
        ctx.assertTrue(entities.size()==2,"Camp must contain one field medic and one support farmer");
        ctx.assertTrue(entities.getCompound(0).getCompound("nbt").getBoolean("RustRuinFieldMedic"),"Camp medic uses village stock and restock rules");
        var support=entities.getCompound(1).getCompound("nbt");ctx.assertTrue(support.getCompound("VillagerData").getString("profession").equals("minecraft:farmer")&&support.getInt("Xp")>0,"Camp support villager is not an established farmer");
        var palette=data.getList("palette",10);boolean field=false;
        for(int i=0;i<palette.size();i++)if(palette.getCompound(i).getString("Name").equals("tetanusmod:field_medic_cabinet"))field=true;
        ctx.assertTrue(field,"Camp does not have a distinct native POI workstation");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void allClinicThemesPreserveGroundOutsideTheFoundation(TestContext ctx){
        var world=ctx.getWorld();var origin=ctx.getAbsolutePos(new BlockPos(1,1,1));var edge=origin.add(0,1,6);
        for(String biome:new String[]{"plains","desert","savanna","snowy","taiga"}){
            world.setBlockState(edge,Blocks.DIAMOND_BLOCK.getDefaultState());
            var template=world.getStructureTemplateManager().getTemplate(TetanusMod.id("village/"+biome+"_clinic")).orElseThrow();
            var clip=new BlockBox(edge.getX(),edge.getY(),edge.getZ(),edge.getX(),edge.getY(),edge.getZ());
            var placement=new net.minecraft.structure.StructurePlacementData().setIgnoreEntities(true).setBoundingBox(clip);
            template.place(world,origin,origin,placement,world.random,3);
            ctx.assertTrue(world.getBlockState(edge).isOf(Blocks.DIAMOND_BLOCK),"Clinic cleared a trench beside its foundation: "+biome);
            var nbt=template.writeNbt(new NbtCompound());var palette=nbt.getList("palette",10);var blocks=nbt.getList("blocks",10);
            for(int i=0;i<blocks.size();i++){
                var block=blocks.getCompound(i);var coordinates=block.getList("pos",3);
                if(coordinates.getInt(1)<2)ctx.assertTrue(!palette.getCompound(block.getInt("state")).getString("Name").equals("minecraft:air"),"Underground air carving remains in "+biome);
            }
        }
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=40)
    public void nativeLocatePoiFindsUndergroundMedicCabinet(TestContext ctx){
        var world=ctx.getWorld();var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));
        world.setBlockState(pos.down(),Blocks.STONE.getDefaultState());
        var lower=ExpansionBlocks.FIELD_MEDIC_CABINET.getDefaultState().with(MedicineCabinetBlock.COMPLETE,true);
        world.setBlockState(pos,lower,18);world.setBlockState(pos.up(),lower.with(DoorBlock.HALF,DoubleBlockHalf.UPPER),3);
        ctx.runAtTick(3,()->{
            ctx.assertTrue(world.getPointOfInterestStorage().getType(pos).filter(entry->entry.matchesKey(MedicVillage.FIELD_JOB_KEY)).isPresent(),"Native POI storage did not index field cabinet");
            ctx.assertTrue(world.getPointOfInterestStorage().getType(pos.up()).isEmpty(),"Upper cabinet registered a duplicate POI");
            var source=world.getServer().getCommandSource().withWorld(world).withPosition(Vec3d.ofCenter(pos)).withLevel(4);
            try{world.getServer().getCommandManager().getDispatcher().execute("locate poi tetanusmod:medic_tent",source);}
            catch(com.mojang.brigadier.exceptions.CommandSyntaxException failure){throw new IllegalStateException("Native cave-medic POI command failed",failure);}
            ctx.complete();
        });
    }
}
