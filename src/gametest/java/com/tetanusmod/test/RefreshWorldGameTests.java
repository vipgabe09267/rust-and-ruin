package com.tetanusmod.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.expansion.*;
import com.tetanusmod.registry.*;
import com.tetanusmod.world.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.chunk.placement.*;
import net.minecraft.world.gen.noise.NoiseConfig;
import java.util.*;

public class RefreshWorldGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void tentPlacementGridEnforcesTwoThousandBlockMinimum(TestContext ctx){
        var set=ctx.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE_SET).get(TetanusMod.id("medic_tents"));
        var placement=(RandomSpreadStructurePlacement)set.placement();
        var encoded=StructurePlacement.TYPE_CODEC.encodeStart(JsonOps.INSTANCE,placement).result().orElseThrow().getAsJsonObject();
        ctx.assertTrue(Math.abs(encoded.get("frequency").getAsFloat()-0.15F)<0.00001,"Tent candidate frequency is not 15%");
        for(long seed:new long[]{0,1,918731,Long.MAX_VALUE,-741}){
            var points=new ArrayList<ChunkPos>();
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)points.add(placement.getStartChunk(seed,x*placement.getSpacing(),z*placement.getSpacing()));
            for(int i=0;i<points.size();i++)for(int j=0;j<i;j++){
                long dx=(long)(points.get(i).x-points.get(j).x)*16,dz=(long)(points.get(i).z-points.get(j).z)*16;
                ctx.assertTrue(dx*dx+dz*dz>=2000L*2000,"Tent candidates can spawn within 2,000 blocks");
            }
        }ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void clinicFrequencyMatchesVillageGrid(TestContext ctx){
        var sets=ctx.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE_SET);
        var clinic=(RandomSpreadStructurePlacement)sets.get(TetanusMod.id("medic_clinics")).placement();var vanilla=(RandomSpreadStructurePlacement)sets.get(new net.minecraft.util.Identifier("minecraft","villages")).placement();
        var encoded=StructurePlacement.TYPE_CODEC.encodeStart(JsonOps.INSTANCE,clinic).result().orElseThrow().getAsJsonObject();
        ctx.assertTrue(Math.abs(encoded.get("frequency").getAsFloat()-0.55F)<0.00001,"Clinic candidate frequency is not 55%");
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)ctx.assertTrue(clinic.getStartChunk(8132,x*34,z*34).equals(vanilla.getStartChunk(8132,x*34,z*34)),"Clinic candidates drift from villages");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=1200)
    public void clinicCanGenerateInRealOverworldNoise(TestContext ctx){
        var world=ctx.getWorld();var registries=world.getRegistryManager();
        var ops=RegistryOps.of(JsonOps.INSTANCE,registries);
        var generator=ChunkGenerator.CODEC.parse(ops,JsonParser.parseString("{\"type\":\"minecraft:noise\",\"settings\":\"minecraft:overworld\",\"biome_source\":{\"type\":\"minecraft:multi_noise\",\"preset\":\"minecraft:overworld\"}}")).result().orElseThrow();
        long seed=246813579L;var settings=registries.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS).get(ChunkGeneratorSettings.OVERWORLD);
        var noise=NoiseConfig.create(settings,registries.getWrapperOrThrow(RegistryKeys.NOISE_PARAMETERS),seed);
        var placement=(RandomSpreadStructurePlacement)registries.get(RegistryKeys.STRUCTURE_SET).get(TetanusMod.id("medic_clinics")).placement();
        var clinic=registries.get(RegistryKeys.STRUCTURE).get(TetanusMod.id("medic_clinic"));boolean found=false;
        for(int region=0;region<40&&!found;region++){
            var chunk=placement.getStartChunk(seed,(region%8-4)*34,(region/8-2)*34);
            var start=clinic.createStructureStart(registries,generator,generator.getBiomeSource(),noise,world.getStructureTemplateManager(),seed,chunk,0,world,clinic.getValidBiomes()::contains);
            if(start.hasChildren()){
                found=true;ctx.assertTrue(start.getChildren().size()==1&&start.getChildren().get(0) instanceof MedicStructurePiece,"Clinic did not create a saved native structure piece");
                ctx.assertTrue(start.getChildren().get(0).getBoundingBox().getBlockCountY()==11,"Generated clinic height wrong");
            }
        }
        ctx.assertTrue(found,"No clinic generated in 40 deterministic vanilla village candidates");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void tentGuardMarkerCreatesOneTetanizedDefender(TestContext ctx)throws Exception{
        var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));
        var piece=new MedicStructurePiece(ctx.getWorld().getStructureTemplateManager(),TetanusMod.id("medic_tent"),pos,net.minecraft.util.BlockRotation.NONE);
        var method=MedicStructurePiece.class.getDeclaredMethod("handleMetadata",String.class,BlockPos.class,ServerWorldAccess.class,net.minecraft.util.math.random.Random.class,BlockBox.class);method.setAccessible(true);
        var bounds=new BlockBox(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+12,pos.getY()+7,pos.getZ()+12);
        method.invoke(piece,"guard",pos,ctx.getWorld(),ctx.getWorld().random,bounds);
        var golems=ctx.getWorld().getEntitiesByClass(net.minecraft.entity.passive.IronGolemEntity.class,new Box(pos).expand(2),e->true);
        ctx.assertTrue(golems.size()==1&&MobVariants.permanent(golems.get(0))&&golems.get(0).getMaxHealth()==200&&golems.get(0).isPlayerCreated(),"Tent defender is missing, weak or hostile to its visitors");
        method.invoke(piece,"guard",pos.add(100,0,0),ctx.getWorld(),ctx.getWorld().random,bounds);
        ctx.assertTrue(ctx.getWorld().getEntitiesByClass(net.minecraft.entity.passive.IronGolemEntity.class,new Box(pos.add(100,0,0)).expand(2),e->true).isEmpty(),"Out-of-chunk metadata spawned a duplicate guard");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void breakingEitherCabinetHalfDropsExactlyOne(TestContext ctx){
        var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));ctx.getWorld().setBlockState(pos.down(),Blocks.STONE.getDefaultState());
        var lower=ExpansionBlocks.MEDIC_CABINET.getDefaultState().with(MedicineCabinetBlock.COMPLETE,true);
        ctx.getWorld().setBlockState(pos,lower,2);ctx.getWorld().setBlockState(pos.up(),lower.with(DoorBlock.HALF,net.minecraft.block.enums.DoubleBlockHalf.UPPER),3);
        ctx.getWorld().breakBlock(pos.up(),true);
        var items=ctx.getWorld().getEntitiesByClass(ItemEntity.class,new Box(pos).expand(3),e->e.getStack().isOf(ExpansionBlocks.MEDIC_CABINET.asItem()));
        ctx.assertTrue(ctx.getWorld().isAir(pos)&&ctx.getWorld().isAir(pos.up()),"Breaking upper cabinet left an orphan half");
        ctx.assertTrue(items.stream().mapToInt(e->e.getStack().getCount()).sum()==1,"Cabinet dropped duplicate items or no item");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void gloveUnbreakingReducesWear(TestContext ctx){
        var p=ctx.createMockSurvivalPlayer();var gloves=new ItemStack(ModItems.PROTECTIVE_GLOVES);gloves.addEnchantment(net.minecraft.enchantment.Enchantments.UNBREAKING,3);((GloveInventory)p).rustruin$gloves().setStack(0,gloves);p.getRandom().setSeed(937461);
        for(int i=0;i<64;i++)com.tetanusmod.effect.InfectionProtection.allows(p,true,1);
        ctx.assertTrue(gloves.getDamage()>0&&gloves.getDamage()<40,"Unbreaking did not reduce wear in dedicated glove slot");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void skeletonNailGoalIsRestoredWithoutDuplicating(TestContext ctx){
        var skeleton=EntityType.SKELETON.create(ctx.getWorld());skeleton.equipStack(EquipmentSlot.MAINHAND,new ItemStack(ModItems.RUSTED_NAIL));
        skeleton.updateAttackType();skeleton.updateAttackType();
        var goals=((Rabies.MobGoals)skeleton).rustruin$goals().getGoals();
        ctx.assertTrue(goals.stream().filter(g->g.getGoal() instanceof MobEquipment.NailThrowGoal).count()==1,"Skeleton accumulated duplicate nail goals");
        var loaded=EntityType.SKELETON.create(ctx.getWorld());loaded.readNbt(skeleton.writeNbt(new net.minecraft.nbt.NbtCompound()));
        ctx.assertTrue(((Rabies.MobGoals)loaded).rustruin$goals().getGoals().stream().filter(g->g.getGoal() instanceof MobEquipment.NailThrowGoal).count()==1,"Skeleton lost throwing behavior after load");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rustyNailCooldownIsSlightlyLonger(TestContext ctx){
        var p=ctx.createMockSurvivalPlayer();p.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUSTED_NAIL,3));ModItems.RUSTED_NAIL.use(ctx.getWorld(),p,Hand.MAIN_HAND);
        for(int i=0;i<10;i++)p.getItemCooldownManager().update();ctx.assertTrue(p.getItemCooldownManager().isCoolingDown(ModItems.RUSTED_NAIL),"Rusty nail still uses old ten-tick cooldown");
        for(int i=0;i<4;i++)p.getItemCooldownManager().update();ctx.assertFalse(p.getItemCooldownManager().isCoolingDown(ModItems.RUSTED_NAIL),"Rusty nail cooldown exceeds fourteen ticks");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void arrowTextureKeepsVanillaTransparency(TestContext ctx)throws Exception{
        var loader=getClass().getClassLoader();
        try(var original=loader.getResourceAsStream("assets/minecraft/textures/entity/projectiles/arrow.png");var rusty=loader.getResourceAsStream("assets/tetanusmod/textures/entity/tetanized_arrow.png")){
            var a=javax.imageio.ImageIO.read(original);var b=javax.imageio.ImageIO.read(rusty);
            ctx.assertTrue(a.getWidth()==b.getWidth()&&a.getHeight()==b.getHeight(),"Arrow texture dimensions changed");
            for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++)ctx.assertTrue((a.getRGB(x,y)>>>24)==(b.getRGB(x,y)>>>24),"Arrow sprite has an oversized opaque plane at "+x+","+y);
        }ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=1200)
    public void tentCanGenerateInRealOverworldCaves(TestContext ctx){
        var world=ctx.getWorld();var registries=world.getRegistryManager();var ops=RegistryOps.of(JsonOps.INSTANCE,registries);
        var generator=ChunkGenerator.CODEC.parse(ops,JsonParser.parseString("{\"type\":\"minecraft:noise\",\"settings\":\"minecraft:overworld\",\"biome_source\":{\"type\":\"minecraft:multi_noise\",\"preset\":\"minecraft:overworld\"}}")).result().orElseThrow();
        long seed=246813579L;var settings=registries.get(RegistryKeys.CHUNK_GENERATOR_SETTINGS).get(ChunkGeneratorSettings.OVERWORLD);
        var noise=NoiseConfig.create(settings,registries.getWrapperOrThrow(RegistryKeys.NOISE_PARAMETERS),seed);
        var placement=(RandomSpreadStructurePlacement)registries.get(RegistryKeys.STRUCTURE_SET).get(TetanusMod.id("medic_tents")).placement();
        var tent=registries.get(RegistryKeys.STRUCTURE).get(TetanusMod.id("medic_tent"));boolean found=false;
        for(int region=0;region<256&&!found;region++){
            var chunk=placement.getStartChunk(seed,(region%16-8)*160,(region/16-8)*160);
            var start=tent.createStructureStart(registries,generator,generator.getBiomeSource(),noise,world.getStructureTemplateManager(),seed,chunk,0,world,tent.getValidBiomes()::contains);
            if(start.hasChildren()){
                found=true;var box=start.getChildren().get(0).getBoundingBox();ctx.assertTrue(box.getBlockCountY()==8&&box.getMaxY()<64,"Tent generated outside an underground cavern");
            }
        }
        ctx.assertTrue(found,"No cave tent generated in 256 deterministic noise candidates");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void protectedShieldAwardsForAnUnenchantedRustyAttack(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try{
            player.changeGameMode(GameMode.SURVIVAL);player.getAbilities().creativeMode=false;player.getAbilities().invulnerable=false;((com.tetanusmod.test.mixin.ServerPlayerTestAccessor)player).rustruin$joinInvulnerability(0);player.setNoGravity(true);player.setPosition(ctx.getAbsolute(new Vec3d(3,4,3)));player.setYaw(0);
            var shield=new ItemStack(Items.SHIELD);shield.addEnchantment(ExpansionItems.TETANUS_PROTECTION,1);player.setStackInHand(Hand.OFF_HAND,shield);player.setCurrentHand(Hand.OFF_HAND);
            var zombie=ctx.spawnEntity(EntityType.ZOMBIE,new Vec3d(3,4,5));zombie.setAiDisabled(true);zombie.equipStack(EquipmentSlot.MAINHAND,new ItemStack(ModItems.RUSTED_METAL_PIPE));
            for(int i=0;i<6;i++)player.playerTick();ctx.assertTrue(player.isBlocking(),"Fixture never raised shield");float health=player.getHealth();player.damage(ctx.getWorld().getDamageSources().mobAttack(zombie),4);
            ctx.assertTrue(player.getHealth()==health&&!player.hasStatusEffect(TetanusMod.TETANUS),"Rusty attack crossed shield");
            for(String id:new String[]{"root","shield_protection"})ctx.assertTrue(player.getAdvancementTracker().getProgress(player.getServer().getAdvancementLoader().get(TetanusMod.id(id))).isDone(),"Rusty block failed to reveal "+id);
        }finally{player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void nailSkeletonRespectsWallsAndThrowsRealProjectiles(TestContext ctx){
        var skeleton=ctx.spawnEntity(EntityType.SKELETON,new Vec3d(2.5,3,2.5));skeleton.setAiDisabled(true);skeleton.equipStack(EquipmentSlot.MAINHAND,new ItemStack(ModItems.RUSTED_NAIL));
        var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(2.5,3,6.5));cow.setAiDisabled(true);skeleton.setTarget(cow);var goal=new MobEquipment.NailThrowGoal(skeleton);
        ctx.assertTrue(goal.canStart(),"Nail skeleton cannot target a living enemy");
        for(int x=1;x<=4;x++)for(int y=2;y<=6;y++)ctx.setBlockState(new BlockPos(x,y,4),Blocks.STONE);
        goal.tick();ctx.assertTrue(ctx.getWorld().getEntitiesByClass(com.tetanusmod.entity.NailEntity.class,skeleton.getBoundingBox().expand(10),e->e.getOwner()==skeleton).isEmpty(),"Skeleton shot through a wall");
        for(int x=1;x<=4;x++)for(int y=2;y<=6;y++)ctx.setBlockState(new BlockPos(x,y,4),Blocks.AIR);
        goal.tick();var shots=ctx.getWorld().getEntitiesByClass(com.tetanusmod.entity.NailEntity.class,skeleton.getBoundingBox().expand(10),e->e.getOwner()==skeleton);
        ctx.assertTrue(shots.size()==1&&shots.get(0).getVelocity().length()>1,"Skeleton failed to throw a physical nail");
        ctx.assertFalse(shots.get(0).writeNbt(new net.minecraft.nbt.NbtCompound()).getBoolean("Recoverable"),"Skeleton ammunition can be farmed");
        goal.tick();ctx.assertTrue(ctx.getWorld().getEntitiesByClass(com.tetanusmod.entity.NailEntity.class,skeleton.getBoundingBox().expand(10),e->e.getOwner()==skeleton).size()==1,"Skeleton ignored its throw cooldown");shots.forEach(Entity::discard);ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void cabinetMigrationDoesNotOverwriteCeiling(TestContext ctx){
        var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));ctx.getWorld().setBlockState(pos.down(),Blocks.STONE.getDefaultState());ctx.getWorld().setBlockState(pos.up(),Blocks.DIAMOND_BLOCK.getDefaultState());ctx.getWorld().setBlockState(pos,ExpansionBlocks.MEDIC_CABINET.getDefaultState());
        var p=ctx.createMockSurvivalPlayer();ExpansionBlocks.MEDIC_CABINET.onUse(ctx.getWorld().getBlockState(pos),ctx.getWorld(),pos,p,Hand.MAIN_HAND,new net.minecraft.util.hit.BlockHitResult(Vec3d.ofCenter(pos),Direction.NORTH,pos,false));
        ctx.assertTrue(ctx.getWorld().getBlockState(pos.up()).isOf(Blocks.DIAMOND_BLOCK)&&!ctx.getWorld().getBlockState(pos).get(MedicineCabinetBlock.COMPLETE),"Legacy cabinet overwrote ceiling or lost migration state");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=600)
    public void fullNativeSpawnKeepsRustyWeapons(TestContext ctx){
        var world=ctx.getWorld();var old=world.getDifficulty();int nails=0,zombieWeapons=0;var kinds=new java.util.HashSet<Item>();
        try{
            world.getServer().setDifficulty(Difficulty.HARD,true);
            var random=new java.util.Random(934987L);
            for(int i=0;i<512;i++){
                var skeleton=EntityType.SKELETON.create(world);skeleton.getRandom().setSeed(random.nextLong());skeleton.initialize(world,world.getLocalDifficulty(skeleton.getBlockPos()),SpawnReason.NATURAL,null,null);
                if(skeleton.getMainHandStack().isOf(ModItems.RUSTED_NAIL))nails++;
                var zombie=EntityType.ZOMBIE.create(world);zombie.getRandom().setSeed(random.nextLong());zombie.initialize(world,world.getLocalDifficulty(zombie.getBlockPos()),SpawnReason.NATURAL,null,null);
                var item=zombie.getMainHandStack().getItem();if(Set.of(ModItems.REBAR,ModItems.RUSTED_METAL_PIPE,ExpansionItems.RUSTED_SPEAR).contains(item)){zombieWeapons++;kinds.add(item);}
            }
            ctx.assertTrue(nails>=8&&nails<=65,"Native skeleton spawn overwrote nails or used wrong rate: "+nails);
            ctx.assertTrue(zombieWeapons>=8&&zombieWeapons<=65&&kinds.size()==3,"Native zombie weapons missing/overwritten: "+zombieWeapons+" / "+kinds.size());
            var summoned=EntityType.SKELETON.create(world);summoned.initialize(world,world.getLocalDifficulty(summoned.getBlockPos()),SpawnReason.COMMAND,null,null);ctx.assertTrue(summoned.getMainHandStack().isOf(Items.BOW),"Command skeleton gear was unexpectedly randomized");
            ctx.assertTrue(MobEquipment.chance(Difficulty.EASY)==0.02F&&MobEquipment.chance(Difficulty.NORMAL)==0.04F&&MobEquipment.chance(Difficulty.HARD)==0.06F,"Difficulty weapon rates wrong");
        }finally{world.getServer().setDifficulty(old,true);}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void cabinetItemPlacementConsumesOneAndRespectsHeadroom(TestContext ctx){
        var world=ctx.getWorld();var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));world.setBlockState(pos.down(),Blocks.STONE.getDefaultState());world.setBlockState(pos,Blocks.AIR.getDefaultState());world.setBlockState(pos.up(),Blocks.AIR.getDefaultState());
        var player=ctx.createMockSurvivalPlayer();var item=ExpansionBlocks.MEDIC_CABINET.asItem();player.setStackInHand(Hand.MAIN_HAND,new ItemStack(item,2));
        var hit=new net.minecraft.util.hit.BlockHitResult(Vec3d.ofCenter(pos.down()).add(0,0.5,0),Direction.UP,pos.down(),false);
        var result=item.useOnBlock(new ItemUsageContext(player,Hand.MAIN_HAND,hit));
        ctx.assertTrue(result.isAccepted()&&player.getMainHandStack().getCount()==1,"Cabinet item placement failed or consumed the wrong count");
        ctx.assertTrue(world.getBlockState(pos).isOf(ExpansionBlocks.MEDIC_CABINET)&&world.getBlockState(pos.up()).isOf(ExpansionBlocks.MEDIC_CABINET)&&world.getBlockState(pos).get(MedicineCabinetBlock.COMPLETE)&&world.getBlockState(pos.up()).get(MedicineCabinetBlock.COMPLETE),"New cabinet did not place two complete halves");
        world.breakBlock(pos,true);ctx.assertTrue(world.isAir(pos)&&world.isAir(pos.up()),"Breaking lower cabinet left an orphan upper half");
        int dropped=world.getEntitiesByClass(ItemEntity.class,new Box(pos).expand(3),e->e.getStack().isOf(item)).stream().mapToInt(e->e.getStack().getCount()).sum();ctx.assertTrue(dropped==1,"Breaking lower cabinet lost or duplicated the item");
        world.setBlockState(pos.up(),Blocks.DIAMOND_BLOCK.getDefaultState());int count=player.getMainHandStack().getCount();item.useOnBlock(new ItemUsageContext(player,Hand.MAIN_HAND,hit));
        ctx.assertTrue(world.isAir(pos)&&world.getBlockState(pos.up()).isOf(Blocks.DIAMOND_BLOCK)&&player.getMainHandStack().getCount()==count,"Cabinet ignored blocked headroom or consumed an unplaced item");ctx.complete();
    }
}
