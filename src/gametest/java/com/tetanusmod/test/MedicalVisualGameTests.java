package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.expansion.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.*;
import net.minecraft.block.enums.*;
import net.minecraft.item.Items;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.test.*;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

/** Native lifecycle checks for both medical cabinet job sites. Visuals are checked in clients. */
public class MedicalVisualGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void steelCabinetsOpenByHandForEveryFacingAndHinge(TestContext ctx){
        var world=ctx.getWorld();var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));var player=ctx.createMockSurvivalPlayer();
        world.setBlockState(pos.down(),Blocks.STONE.getDefaultState());
        for(var block:new Block[]{ExpansionBlocks.MEDIC_CABINET,ExpansionBlocks.FIELD_MEDIC_CABINET}){
            for(var direction:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST})for(var hinge:DoorHinge.values()){
                var state=block.getDefaultState().with(MedicineCabinetBlock.COMPLETE,true).with(DoorBlock.FACING,direction).with(DoorBlock.HINGE,hinge);
                world.setBlockState(pos,state,18);world.setBlockState(pos.up(),state.with(DoorBlock.HALF,DoubleBlockHalf.UPPER),18);
                ctx.assertTrue(DoorBlock.canOpenByHand(state),"Metal medicine cabinet was accidentally made redstone-only");
                block.onUse(state,world,pos,player,Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(pos),direction,pos,false));
                ctx.assertTrue(world.getBlockState(pos).get(DoorBlock.OPEN)&&world.getBlockState(pos.up()).get(DoorBlock.OPEN),"Cabinet door halves did not open together: "+direction+" / "+hinge);
                block.onUse(world.getBlockState(pos.up()),world,pos.up(),player,Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(pos.up()),direction,pos.up(),false));
                ctx.assertTrue(!world.getBlockState(pos).get(DoorBlock.OPEN)&&!world.getBlockState(pos.up()).get(DoorBlock.OPEN),"Upper-half use did not close both door halves");
                ctx.assertTrue(world.getBlockState(pos).isOf(block)&&world.getBlockState(pos.up()).isOf(block),"Toggling changed the cabinet registry identity");
            }
        }ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void fieldCabinetHasOneDistinctJobSiteAndNormalItemLoot(TestContext ctx){
        var block=ExpansionBlocks.FIELD_MEDIC_CABINET;var lower=block.getDefaultState();var upper=lower.with(DoorBlock.HALF,DoubleBlockHalf.UPPER);
        ctx.assertTrue(MedicVillage.FIELD_JOB.contains(lower)&&!MedicVillage.FIELD_JOB.contains(upper),"Tent cabinet must expose exactly its lower-half job site");
        ctx.assertTrue(!MedicVillage.JOB.contains(lower)&&!MedicVillage.FIELD_JOB.contains(ExpansionBlocks.MEDIC_CABINET.getDefaultState()),"Village and field job sites are not distinct");
        var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));
        var drops=Block.getDroppedStacks(lower,ctx.getWorld(),pos,null);
        ctx.assertTrue(drops.size()==1&&drops.get(0).isOf(ExpansionBlocks.MEDIC_CABINET.asItem())&&drops.get(0).getCount()==1,"Field cabinet does not return one ordinary cabinet item");
        ctx.assertTrue(Block.getDroppedStacks(upper,ctx.getWorld(),pos.up(),null).isEmpty(),"Upper cabinet drops a duplicate item");
        ctx.assertTrue(block.asItem()==Items.AIR,"Internal field workstation leaked an unimplemented inventory item");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void fieldCabinetRemainsSolidForNativePathfinding(TestContext ctx)throws Exception{
        var world=ctx.getWorld();var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));world.setBlockState(pos.down(),Blocks.STONE.getDefaultState());
        var method=net.minecraft.entity.ai.pathing.LandPathNodeMaker.class.getDeclaredMethod("getCommonNodeType",net.minecraft.world.BlockView.class,BlockPos.class);method.setAccessible(true);
        for(boolean open:new boolean[]{false,true}){
            world.setBlockState(pos,ExpansionBlocks.FIELD_MEDIC_CABINET.getDefaultState().with(DoorBlock.OPEN,open));
            ctx.assertTrue(method.invoke(null,world,pos)==net.minecraft.entity.ai.pathing.PathNodeType.BLOCKED,"Field cabinet is a pass-through path node, open="+open);
        }ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void legacyFieldCabinetUpgradePreservesItsJobSite(TestContext ctx){
        var world=ctx.getWorld();var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));world.setBlockState(pos.down(),Blocks.STONE.getDefaultState());world.setBlockState(pos.up(),Blocks.AIR.getDefaultState());
        world.setBlockState(pos,ExpansionBlocks.FIELD_MEDIC_CABINET.getDefaultState());
        ExpansionBlocks.FIELD_MEDIC_CABINET.onUse(world.getBlockState(pos),world,pos,ctx.createMockSurvivalPlayer(),Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(pos),Direction.NORTH,pos,false));
        ctx.assertTrue(world.getBlockState(pos).get(MedicineCabinetBlock.COMPLETE)&&world.getBlockState(pos.up()).isOf(ExpansionBlocks.FIELD_MEDIC_CABINET),"Legacy field cabinet migration replaced its POI with the village type");
        ctx.assertTrue(MedicVillage.FIELD_JOB.contains(world.getBlockState(pos))&&!MedicVillage.FIELD_JOB.contains(world.getBlockState(pos.up())),"Migrated field cabinet has an invalid POI count");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void allClinicCabinetsFaceTheirInteriorAisleAfterRotation(TestContext ctx){
        var world=ctx.getWorld();var cabinetPos=ctx.getAbsolutePos(new BlockPos(1,3,1));
        var clip=new BlockBox(cabinetPos.getX()-1,cabinetPos.getY()-1,cabinetPos.getZ()-1,cabinetPos.getX()+1,cabinetPos.getY()+2,cabinetPos.getZ()+1);
        for(String biome:new String[]{"plains","desert","savanna","snowy","taiga"}){
            var template=world.getStructureTemplateManager().getTemplate(TetanusMod.id("village/"+biome+"_clinic")).orElseThrow();
            for(var rotation:BlockRotation.values()){
                // Clip the native rotated template to its cabinet, the back wall and the aisle.
                var placement=new StructurePlacementData().setIgnoreEntities(true).setRotation(rotation).setBoundingBox(clip);
                var origin=cabinetPos.subtract(StructureTemplate.transform(placement,new BlockPos(6,2,6)));
                ctx.assertTrue(template.place(world,origin,origin,placement,world.random,18),"Clinic template placement failed: "+biome+" / "+rotation);
                var facing=rotation.rotate(Direction.WEST);
                for(int half=0;half<2;half++){
                    var pos=cabinetPos.up(half);var state=world.getBlockState(pos);var label=biome+" / "+rotation+" / half "+half;
                    ctx.assertTrue(state.isOf(ExpansionBlocks.MEDIC_CABINET),"Clinic cabinet missing after native placement: "+label);
                    ctx.assertTrue(state.get(DoorBlock.FACING)==facing,"Clinic cabinet faces along the wall instead of into its aisle: "+label);
                    ctx.assertTrue(state.get(DoorBlock.HALF)==(half==0?DoubleBlockHalf.LOWER:DoubleBlockHalf.UPPER)&&state.get(MedicineCabinetBlock.COMPLETE),"Clinic cabinet halves are inconsistent: "+label);
                    var front=pos.offset(facing);var back=pos.offset(facing.getOpposite());
                    ctx.assertTrue(world.getBlockState(front).isAir(),"Clinic cabinet door opens into an obstruction: "+label);
                    ctx.assertTrue(world.getBlockState(back).isFullCube(world,back),"Clinic cabinet does not back onto its exterior wall: "+label);
                }
            }
        }
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void actualCabinetItemPlacementFacesThePlacerAndUsesNativeHinges(TestContext ctx){
        var world=ctx.getWorld();var pos=ctx.getAbsolutePos(new BlockPos(1,3,1));var player=ctx.createMockSurvivalPlayer();
        for(var look:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}){
            var front=look.getOpposite();
            for(double hit:new double[]{0.25,0.75})for(int obstruction:new int[]{-1,0,1}){
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=1;y++)world.setBlockState(pos.add(x,y,z),Blocks.AIR.getDefaultState(),18);
                world.setBlockState(pos.down(),Blocks.STONE.getDefaultState());
                if(obstruction!=0){var side=obstruction<0?front.rotateYCounterclockwise():front.rotateYClockwise();world.setBlockState(pos.offset(side),Blocks.STONE.getDefaultState());world.setBlockState(pos.offset(side).up(),Blocks.STONE.getDefaultState());}
                player.setYaw(look.asRotation());player.setPitch(0);player.setPosition(Vec3d.ofBottomCenter(pos).add(-look.getOffsetX()*2,0,-look.getOffsetZ()*2));
                var stack=new net.minecraft.item.ItemStack(ExpansionBlocks.MEDIC_CABINET);player.setStackInHand(Hand.MAIN_HAND,stack);
                var point=new Vec3d(pos.getX()+(front.getAxis()==Direction.Axis.Z?hit:0.5),pos.getY(),pos.getZ()+(front.getAxis()==Direction.Axis.X?hit:0.5));
                var context=new net.minecraft.item.ItemPlacementContext(player,Hand.MAIN_HAND,stack,new BlockHitResult(point,Direction.UP,pos.down(),false));
                // Vanilla provides the wall/click hinge oracle, with the cabinet's outward front.
                var outward=new net.minecraft.item.ItemPlacementContext(context){@Override public Direction getHorizontalPlayerFacing(){return front;}};
                var nativeDoor=Blocks.OAK_DOOR.getPlacementState(outward);ctx.assertTrue(nativeDoor!=null,"Native hinge fixture could not place a door");
                var result=((net.minecraft.item.BlockItem)stack.getItem()).place(context);var label=look+" / click="+hit+" / wall="+obstruction;
                ctx.assertTrue(result.isAccepted()&&stack.isEmpty(),"Actual Survival cabinet placement did not consume one item: "+label);
                for(int half=0;half<2;half++){
                    var state=world.getBlockState(pos.up(half));
                    ctx.assertTrue(state.isOf(ExpansionBlocks.MEDIC_CABINET)&&state.get(DoorBlock.FACING)==front,"Placed cabinet front faces away from its placer: "+label);
                    ctx.assertTrue(state.get(DoorBlock.HINGE)==nativeDoor.get(DoorBlock.HINGE)&&state.get(MedicineCabinetBlock.COMPLETE),"Placed cabinet hinge did not use its corrected facing: "+label);
                }
                ExpansionBlocks.MEDIC_CABINET.onUse(world.getBlockState(pos),world,pos,player,Hand.MAIN_HAND,new BlockHitResult(Vec3d.ofCenter(pos),front,pos,false));
                ctx.assertTrue(world.getBlockState(pos).get(DoorBlock.OPEN)&&world.getBlockState(pos.up()).get(DoorBlock.OPEN),"Player-facing cabinet halves did not open together: "+label);
            }
        }
        world.setBlockState(pos,Blocks.AIR.getDefaultState(),18);world.setBlockState(pos.up(),Blocks.STONE.getDefaultState(),18);
        var blocked=new net.minecraft.item.ItemStack(ExpansionBlocks.MEDIC_CABINET);player.setStackInHand(Hand.MAIN_HAND,blocked);
        var context=new net.minecraft.item.ItemPlacementContext(player,Hand.MAIN_HAND,blocked,new BlockHitResult(Vec3d.ofBottomCenter(pos),Direction.UP,pos.down(),false));
        ctx.assertTrue(!((net.minecraft.item.BlockItem)blocked.getItem()).place(context).isAccepted()&&blocked.getCount()==1&&world.getBlockState(pos).isAir(),"Correcting cabinet facing bypassed native upper-half obstruction checks");
        ctx.complete();
    }
}
