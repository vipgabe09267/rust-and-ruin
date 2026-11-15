package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.expansion.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import java.util.*;

/** Native spawn predicates stay intact; deliberate variant eggs gain physical placement safety. */
public final class NaturalSpawnRulesGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="variant_egg_safety")
    public void hostileAndRabidEggsRejectLavaAndSolidBlocksWithoutConsumption(TestContext ctx){
        var world=ctx.getWorld();var feet=ctx.getAbsolutePos(new BlockPos(4,3,4));prepareAir(ctx,feet);var player=player(ctx,feet);
        try{
            for(var type:List.of(EntityType.ZOMBIE,EntityType.SKELETON,EntityType.CREEPER,EntityType.ENDERMAN)){
                for(boolean lava:new boolean[]{true,false}){
                    var item=egg(type,false);var stack=new ItemStack(item,2);
                    world.setBlockState(feet,(lava?Blocks.LAVA:Blocks.STONE).getDefaultState());world.setBlockState(feet.up(),(lava?Blocks.LAVA:Blocks.STONE).getDefaultState());
                    ctx.assertFalse(SpawnHelper.canSpawn(SpawnRestriction.getLocation(type),world,feet,type),"Native placement accepted the unsafe hostile location");
                    int before=world.getEntitiesByClass(MobEntity.class,new Box(feet).expand(5),e->e.getType()==type).size();
                    ctx.assertFalse(item.spawn(world,player,stack,feet),"Unsafe variant egg spawned "+type+" into "+(lava?"lava":"solid terrain"));
                    ctx.assertTrue(stack.getCount()==2,"Rejected variant egg consumed an item");
                    ctx.assertTrue(world.getEntitiesByClass(MobEntity.class,new Box(feet).expand(5),e->e.getType()==type).size()==before,"Rejected egg left a mob behind");
                    world.setBlockState(feet,Blocks.AIR.getDefaultState());world.setBlockState(feet.up(),Blocks.AIR.getDefaultState());
                }
            }
            var rabid=egg(EntityType.ZOMBIE,true);var stack=new ItemStack(rabid,2);world.setBlockState(feet,Blocks.LAVA.getDefaultState());
            ctx.assertFalse(rabid.spawn(world,player,stack,feet),"Rabid egg bypassed lava validation");ctx.assertTrue(stack.getCount()==2,"Rejected rabid egg consumed an item");world.setBlockState(feet,Blocks.AIR.getDefaultState());
            var valid=egg(EntityType.ZOMBIE,false);var input=new ItemStack(valid,2);ctx.assertTrue(valid.spawn(world,player,input,feet),"Safe ordinary floor rejected a valid egg");ctx.assertTrue(input.getCount()==1,"Valid Survival egg did not consume exactly one");
            var result=world.getEntitiesByClass(MobEntity.class,new Box(feet).expand(2),e->e.getType()==EntityType.ZOMBIE&&MobVariants.permanent(e));ctx.assertTrue(result.size()==1,"Successful egg lost its normal entity type or mutation");result.forEach(Entity::discard);
        }finally{player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="variant_egg_fluid_species")
    public void waterNativeSpeciesAndStridersRetainAppropriateFluidPlacement(TestContext ctx){
        var world=ctx.getWorld();var feet=ctx.getAbsolutePos(new BlockPos(4,3,4));var player=player(ctx,feet);
        try{
            for(var type:List.of(EntityType.COD,EntityType.AXOLOTL,EntityType.GUARDIAN,EntityType.ELDER_GUARDIAN,EntityType.DROWNED,EntityType.TURTLE,EntityType.FROG,EntityType.STRIDER)){
                prepareAir(ctx,feet);var fluid=type==EntityType.STRIDER?Blocks.LAVA:Blocks.WATER;
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=0;y<=3;y++)world.setBlockState(feet.add(x,y,z),(Math.abs(x)==3||Math.abs(z)==3?Blocks.STONE:y<=2?fluid:Blocks.AIR).getDefaultState());
                var item=egg(type,false);var input=new ItemStack(item,2);ctx.assertTrue(item.spawn(world,player,input,feet),"Appropriate fluid placement rejected "+type);ctx.assertTrue(input.getCount()==1,"Fluid egg did not consume exactly one");
                var result=world.getEntitiesByClass(MobEntity.class,new Box(feet).expand(3),e->e.getType()==type);ctx.assertTrue(result.size()==1&&MobVariants.permanent(result.get(0)),"Fluid egg changed type or omitted mutation");result.forEach(Entity::discard);
            }
        }finally{prepareAir(ctx,feet);player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="variant_egg_body_clearance")
    public void validatorChecksTheWholeBodyAndDoesNotLoadDistantChunks(TestContext ctx){
        var world=ctx.getWorld();var feet=ctx.getAbsolutePos(new BlockPos(4,3,4));prepareAir(ctx,feet);
        var enderman=EntityType.ENDERMAN.create(world);enderman.setPosition(Vec3d.ofBottomCenter(feet));
        ctx.assertTrue(VariantSpawnEggItem.safePlacement(world,enderman),"Open tall-body placement rejected");world.setBlockState(feet.up(2),Blocks.STONE.getDefaultState());ctx.assertFalse(VariantSpawnEggItem.safePlacement(world,enderman),"Tall mob spawned with its head in a ceiling");world.setBlockState(feet.up(2),Blocks.AIR.getDefaultState());
        world.setBlockState(feet.up(),Blocks.LAVA.getDefaultState());ctx.assertFalse(VariantSpawnEggItem.safePlacement(world,enderman),"Mob spawned partly in lava above its feet");world.setBlockState(feet.up(),Blocks.AIR.getDefaultState());
        var distant=new BlockPos(12000000,100,12000000);var player=player(ctx,feet);var item=egg(EntityType.ZOMBIE,false);var stack=new ItemStack(item,2);
        try{ctx.assertFalse(world.isChunkLoaded(distant),"Distant test chunk was unexpectedly loaded");ctx.assertFalse(item.spawn(world,player,stack,distant),"Egg loaded and spawned in an inaccessible chunk");ctx.assertFalse(world.isChunkLoaded(distant),"Rejected egg forced a remote chunk load");ctx.assertTrue(stack.getCount()==2,"Remote rejection consumed an egg");}finally{player.discard();enderman.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="native_lighting_predicates",tickLimit=120)
    public void vanillaHostilePredicatesRejectLightAndMutationKeepsTheirTypesAndRules(TestContext ctx)throws Exception{
        var world=ctx.getWorld();var feet=ctx.getAbsolutePos(new BlockPos(3,2,3));
        for(int x=0;x<=6;x++)for(int z=0;z<=6;z++)for(int y=1;y<=5;y++)ctx.setBlockState(x,y,z,(y==1||y==5||x==0||x==6||z==0||z==6)?Blocks.STONE:Blocks.AIR);
        world.setBlockState(feet.add(-3,0,0),Blocks.SEA_LANTERN.getDefaultState());
        var field=SpawnRestriction.class.getDeclaredField("RESTRICTIONS");field.setAccessible(true);var restrictions=(Map<?,?>)field.get(null);
        var types=List.of(EntityType.ZOMBIE,EntityType.SKELETON,EntityType.CREEPER,EntityType.ENDERMAN);var originals=new HashMap<EntityType<?>,Object>();
        for(var type:types){originals.put(type,restrictions.get(type));var mob=type.create(world);com.tetanusmod.effect.Infection.silently(()->MobVariants.makePermanent(mob));ctx.assertTrue(mob.getType()==type,"Mutation replaced a vanilla entity type");ctx.assertTrue(restrictions.get(type)==originals.get(type),"Mutation replaced a native spawn predicate");mob.discard();}
        var rabid=EntityType.ZOMBIE.create(world);Rabies.makeRabid(rabid);ctx.assertTrue(rabid.getType()==EntityType.ZOMBIE&&restrictions.get(EntityType.ZOMBIE)==originals.get(EntityType.ZOMBIE),"Rabies replaced native spawn rules");rabid.discard();
        // Queue both native-light phases before execution; no callbacks insert tasks into the iterator.
        ctx.runAtTick(40,()->{
            ctx.assertTrue(world.getLightLevel(LightType.BLOCK,feet)>0,"Positive light fixture never reached native block lighting");
            for(var type:types)for(var reason:List.of(SpawnReason.NATURAL,SpawnReason.SPAWNER))for(int seed=0;seed<16;seed++)ctx.assertFalse(SpawnRestriction.canSpawn(type,world,reason,feet,Random.create(seed)),"Vanilla hostile accepted adequate lighting: "+type+" / "+reason);
            var player=player(ctx,feet);player.getAbilities().creativeMode=true;var egg=egg(EntityType.ZOMBIE,false);var stack=new ItemStack(egg,2);
            try{ctx.assertTrue(egg.spawn(world,player,stack,feet),"Deliberate Creative egg incorrectly acquired natural darkness restrictions");ctx.assertTrue(stack.getCount()==2,"Creative egg was consumed");world.getEntitiesByClass(MobEntity.class,new Box(feet).expand(2),e->e.getType()==EntityType.ZOMBIE).forEach(Entity::discard);}finally{player.discard();}
            world.setBlockState(feet.add(-3,0,0),Blocks.STONE.getDefaultState());
        });
        ctx.runAtTick(80,()->{
            ctx.assertTrue(world.getLightLevel(LightType.BLOCK,feet)==0,"Negative fixture retained light after removing its source");
            for(var type:types){boolean allowed=false;for(int seed=0;seed<32;seed++)allowed|=SpawnRestriction.canSpawn(type,world,SpawnReason.NATURAL,feet,Random.create(seed));ctx.assertTrue(allowed,"Dark positive control never allowed a valid vanilla hostile: "+type);ctx.assertTrue(restrictions.get(type)==originals.get(type),"Native spawn predicate changed during the test");}ctx.complete();
        });
    }
    private static VariantSpawnEggItem egg(EntityType<?> type,boolean rabid){return (VariantSpawnEggItem)Registries.ITEM.get(TetanusMod.id((rabid?"rabid_":"tetanized_")+Registries.ENTITY_TYPE.getId(type).getPath()+"_spawn_egg"));}
    private static PlayerEntity player(TestContext ctx,BlockPos feet){var player=ctx.createMockSurvivalPlayer();player.setPosition(Vec3d.ofBottomCenter(feet.add(8,0,8)));return player;}
    private static void prepareAir(TestContext ctx,BlockPos feet){for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-1;y<=4;y++)ctx.getWorld().setBlockState(feet.add(x,y,z),(y==-1?Blocks.STONE:Blocks.AIR).getDefaultState());}
}
