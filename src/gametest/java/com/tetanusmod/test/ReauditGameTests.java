package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.*;
import com.tetanusmod.expansion.*;
import com.tetanusmod.registry.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;

public class ReauditGameTests implements FabricGameTest {
    private net.minecraft.server.network.ServerPlayerEntity survivalPlayer(TestContext ctx){
        var p=new net.minecraft.server.network.ServerPlayerEntity(ctx.getWorld().getServer(),ctx.getWorld(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"reaudit-player"));
        p.getServer().getPlayerManager().onPlayerConnect(new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND),p);p.changeGameMode(GameMode.SURVIVAL);p.getAbilities().creativeMode=false;p.getAbilities().invulnerable=false;((com.tetanusmod.test.mixin.ServerPlayerTestAccessor)p).rustruin$joinInvulnerability(0);p.setNoGravity(true);return p;
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void standaloneLockjawAwardsItsAdvancement(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();
        try{
            player.addStatusEffect(new StatusEffectInstance(TetanusMod.LOCKJAW,600));
            var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id("lockjaw_sonata"));
            ctx.assertTrue(player.getAdvancementTracker().getProgress(advancement).isDone(),"Standalone Lockjaw did not award Lockjaw Sonata");
            ctx.assertTrue(!player.hasStatusEffect(TetanusMod.TETANUS),"Fixture accidentally used the Tetanus progression path");
        }finally{player.getServer().getPlayerManager().remove(player);player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void rabidHuntStopsWhenItsTargetChangesDimension(TestContext ctx){
        var player=survivalPlayer(ctx);
        try{
            player.changeGameMode(GameMode.SURVIVAL);player.setPosition(ctx.getAbsolute(new Vec3d(3,4,3)));
            var cow=EntityType.COW.create(ctx.getWorld());cow.setPosition(player.getPos().add(0,0,1));Rabies.makeRabid(cow);
            var goal=new Rabies.RabidHuntGoal(cow);ctx.assertTrue(goal.canStart(),"Rabid cow did not acquire the survival player");
            var destination=ctx.getWorld().getServer().getWorld(World.NETHER);ctx.assertTrue(destination!=null,"Native Nether missing");
            player.teleport(destination,player.getX(),player.getY(),player.getZ(),0,0);
            ctx.assertFalse(goal.shouldContinue(),"Rabid mob retained a player in another dimension");
        }finally{player.getServer().getPlayerManager().remove(player);player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void nailGoalRechecksRevokedTargetsBeforeFiring(TestContext ctx){
        var skeleton=ctx.spawnEntity(EntityType.SKELETON,new Vec3d(2.5,3,2.5));skeleton.setAiDisabled(true);skeleton.equipStack(EquipmentSlot.MAINHAND,new ItemStack(ModItems.RUSTED_NAIL));
        var player=survivalPlayer(ctx);
        try{
            player.changeGameMode(GameMode.SURVIVAL);player.setPosition(skeleton.getPos().add(0,0,3));skeleton.setTarget(player);
            var goal=new MobEquipment.NailThrowGoal(skeleton);ctx.assertTrue(goal.canStart(),"Fixture did not start a nail attack");
            player.changeGameMode(GameMode.CREATIVE);ctx.assertFalse(goal.canStart(),"Creative player is still a valid target");goal.tick();
            ctx.assertTrue(ctx.getWorld().getEntitiesByClass(com.tetanusmod.entity.NailEntity.class,skeleton.getBoundingBox().expand(12),n->n.getOwner()==skeleton).isEmpty(),"Active goal fired after target became Creative");
        }finally{player.getServer().getPlayerManager().remove(player);player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void spasmMovementKeepsVanillaRotationNormalization(TestContext ctx){
        var player=survivalPlayer(ctx);
        try{
            player.setPosition(ctx.getAbsolute(new Vec3d(3,4,3)));((InfectionState)player).tetanus$setFreeze(4);
            player.networkHandler.onPlayerMove(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(player.getX()+1,player.getY(),player.getZ(),777,225,false));
            ctx.assertTrue(player.getYaw()>=-180&&player.getYaw()<180&&player.getPitch()>=-180&&player.getPitch()<180,"Spasm teleport retained unnormalized client angles: "+player.getYaw()+" / "+player.getPitch());
        }finally{player.getServer().getPlayerManager().remove(player);player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void mutationDoesNotWeakenCustomCreeperExplosions(TestContext ctx){
        var creeper=EntityType.CREEPER.create(ctx.getWorld());((com.tetanusmod.mixin.CreeperAccessor)creeper).rustruin$setExplosionRadius(9);com.tetanusmod.entity.MobVariants.makePermanent(creeper);
        var nbt=creeper.writeNbt(new net.minecraft.nbt.NbtCompound());ctx.assertTrue(nbt.getByte("ExplosionRadius")==9,"Mutation weakened a stronger custom creeper explosion");
        com.tetanusmod.entity.MobVariants.loaded(creeper);ctx.assertTrue(creeper.writeNbt(new net.minecraft.nbt.NbtCompound()).getByte("ExplosionRadius")==9,"Reload weakened custom explosion");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void expiredCoatingMarksDroppedStackForSynchronization(TestContext ctx){
        var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));ctx.getWorld().setBlockState(pos,net.minecraft.block.Blocks.WATER.getDefaultState());
        var stack=new ItemStack(ModItems.NAIL);stack.getOrCreateNbt().putInt("RustRuinCoating",1);
        var item=new ItemEntity(ctx.getWorld(),pos.getX()+0.5,pos.getY()+0.25,pos.getZ()+0.5,stack);item.setNoGravity(true);item.age=1;item.getDataTracker().getDirtyEntries();item.tick();
        ctx.assertTrue(com.tetanusmod.rust.RustCoating.remaining(item.getStack())==0,"Coating did not expire");ctx.assertTrue(item.getDataTracker().isDirty(),"Final coating expiration was never synchronized to clients");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void jumpingAboveCaltropsDoesNotTouchTheirSpikes(TestContext ctx){
        var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));var block=ModBlocks.CALTROPS;ctx.getWorld().setBlockState(pos,block.getDefaultState());
        var cow=EntityType.COW.create(ctx.getWorld());cow.setPosition(pos.getX()+0.5,pos.getY()+0.8,pos.getZ()+0.5);float health=cow.getHealth();block.onEntityCollision(ctx.getWorld().getBlockState(pos),ctx.getWorld(),pos,cow);
        ctx.assertTrue(cow.getHealth()==health,"Caltrops damaged a creature above their physical shape");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void jumpingAboveBearTrapDoesNotCloseItsJaws(TestContext ctx){
        var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));var block=ExpansionBlocks.BEAR_TRAP;ctx.getWorld().setBlockState(pos,block.getDefaultState());
        var cow=EntityType.COW.create(ctx.getWorld());cow.setPosition(pos.getX()+0.5,pos.getY()+0.8,pos.getZ()+0.5);block.onEntityCollision(ctx.getWorld().getBlockState(pos),ctx.getWorld(),pos,cow);
        ctx.assertFalse(ctx.getWorld().getBlockState(pos).get(BearTrapBlock.CLOSED),"Bear trap caught a creature above its physical shape");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void legacyCabinetAtWorldCeilingDoesNotBecomeAnOrphan(TestContext ctx){
        var world=ctx.getWorld();var base=ctx.getAbsolutePos(new BlockPos(3,3,3));var pos=new BlockPos(base.getX(),world.getTopY()-1,base.getZ());var saved=world.getBlockState(pos);var floor=world.getBlockState(pos.down());
        try{
            world.setBlockState(pos.down(),net.minecraft.block.Blocks.STONE.getDefaultState());world.setBlockState(pos,ExpansionBlocks.MEDIC_CABINET.getDefaultState());var player=ctx.createMockSurvivalPlayer();
            ExpansionBlocks.MEDIC_CABINET.onUse(world.getBlockState(pos),world,pos,player,net.minecraft.util.Hand.MAIN_HAND,new net.minecraft.util.hit.BlockHitResult(Vec3d.ofCenter(pos),Direction.NORTH,pos,false));
            ctx.assertTrue(world.getBlockState(pos).isOf(ExpansionBlocks.MEDIC_CABINET)&&!world.getBlockState(pos).get(MedicineCabinetBlock.COMPLETE),"Legacy cabinet at build ceiling was marked complete without an upper half");
        }finally{world.setBlockState(pos,saved,18);world.setBlockState(pos.down(),floor,18);}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void trapsStillActivateAtTheirTopSurface(TestContext ctx){
        var world=ctx.getWorld();var first=ctx.getAbsolutePos(new BlockPos(3,3,3));world.setBlockState(first,ModBlocks.CALTROPS.getDefaultState());
        var cow=EntityType.COW.create(world);cow.setPosition(first.getX()+0.5,first.getY()+3/16.0,first.getZ()+0.5);float health=cow.getHealth();ModBlocks.CALTROPS.onEntityCollision(world.getBlockState(first),world,first,cow);ctx.assertTrue(cow.getHealth()<health,"Caltrops no longer hurt creatures at the spike surface");
        var second=first.add(3,0,0);world.setBlockState(second,ExpansionBlocks.BEAR_TRAP.getDefaultState());cow.setPosition(second.getX()+0.5,second.getY()+2/16.0,second.getZ()+0.5);ExpansionBlocks.BEAR_TRAP.onEntityCollision(world.getBlockState(second),world,second,cow);ctx.assertTrue(world.getBlockState(second).get(BearTrapBlock.CLOSED),"Bear trap no longer catches surface contact");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void medicineCabinetIsAnObstacleToNativePathfinding(TestContext ctx)throws Exception{
        var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));var world=ctx.getWorld();world.setBlockState(pos.down(),net.minecraft.block.Blocks.STONE.getDefaultState());
        var method=net.minecraft.entity.ai.pathing.LandPathNodeMaker.class.getDeclaredMethod("getCommonNodeType",net.minecraft.world.BlockView.class,BlockPos.class);method.setAccessible(true);
        for(boolean open:new boolean[]{false,true}){
            world.setBlockState(pos,ExpansionBlocks.MEDIC_CABINET.getDefaultState().with(net.minecraft.block.DoorBlock.OPEN,open));
            ctx.assertTrue(method.invoke(null,world,pos)==net.minecraft.entity.ai.pathing.PathNodeType.BLOCKED,"Native AI treats the solid medicine cabinet as a walk-through door, open="+open);
        }ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void tentGuardMarkerClearsItsOriginalTerrain(TestContext ctx)throws Exception{
        var world=ctx.getWorld();var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));world.setBlockState(pos,net.minecraft.block.Blocks.STONE.getDefaultState());
        var piece=new com.tetanusmod.world.MedicStructurePiece(world.getStructureTemplateManager(),TetanusMod.id("medic_tent"),pos,net.minecraft.util.BlockRotation.NONE);
        var method=piece.getClass().getDeclaredMethod("handleMetadata",String.class,BlockPos.class,ServerWorldAccess.class,net.minecraft.util.math.random.Random.class,BlockBox.class);method.setAccessible(true);
        method.invoke(piece,"guard",pos,world,world.random,new BlockBox(pos));ctx.assertTrue(world.isAir(pos),"Ignored DATA marker left original terrain inside the guard's feet");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void nonDeathPlayerReplacementRetainsDiseaseProgress(TestContext ctx){
        var original=survivalPlayer(ctx);var replacement=survivalPlayer(ctx);
        try{
            Infection.applyDurationChecked(original,null,9000,1);original.addStatusEffect(new StatusEffectInstance(TetanusMod.LOCKJAW,2800));original.addStatusEffect(new StatusEffectInstance(ExpansionEffects.DISORIENTED,9000));Rabies.expose(original,null);
            original.addStatusEffect(new StatusEffectInstance(TetanusMod.TETANUS,20000,0,false,false,false));original.addStatusEffect(new StatusEffectInstance(TetanusMod.SALMONELLA,600,0,true,false,false));
            var before=(InfectionState)original;before.tetanus$setInfection(6200,9000);before.tetanus$setLockjawWindow(3600);before.tetanus$setLockjaw(2800);((ExpansionState)original).rustruin$setLostHealth(6);((ExpansionState)original).rustruin$setRabiesTicks(450);RabiesEffect.restorePenalty(original);

            var active=original.getStatusEffect(TetanusMod.TETANUS).writeNbt(new net.minecraft.nbt.NbtCompound());
            replacement.copyFrom(original,true);
            ctx.assertTrue(active.equals(replacement.getStatusEffect(TetanusMod.TETANUS).writeNbt(new net.minecraft.nbt.NbtCompound())),"Non-death recreation lost active/hidden effect flags or duration");
            ctx.assertTrue(replacement.hasStatusEffect(TetanusMod.SALMONELLA)&&replacement.getStatusEffect(TetanusMod.SALMONELLA).isAmbient()&&!replacement.getStatusEffect(TetanusMod.SALMONELLA).shouldShowParticles(),"Non-death recreation lost Salmonella or its presentation flags");
            ctx.assertTrue(replacement.hasStatusEffect(TetanusMod.TETANUS)&&replacement.hasStatusEffect(TetanusMod.LOCKJAW)&&replacement.hasStatusEffect(ExpansionEffects.RABIES)&&replacement.hasStatusEffect(ExpansionEffects.DISORIENTED),"Non-death player recreation cured protected diseases");
            var after=(InfectionState)replacement;ctx.assertTrue(after.tetanus$getElapsed()==6200&&after.tetanus$getLockjaw()==2800&&after.tetanus$getLockjawWindow()==3600,"Non-death recreation reset disease progression");
            ctx.assertTrue(replacement.getMaxHealth()==14&&((ExpansionState)replacement).rustruin$getRabiesTicks()==450,"Non-death recreation reset Rabies capacity loss or minute timer");
        }finally{original.getServer().getPlayerManager().remove(original);original.discard();replacement.getServer().getPlayerManager().remove(replacement);replacement.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void medicinesRecordNativeConsumptionStatistics(TestContext ctx){
        var player=survivalPlayer(ctx);
        try{
            for(var item:new Item[]{ModItems.ANTITOXIN,ModItems.VACCINE,ExpansionItems.RABIES_CURE,ExpansionItems.RABIES_VACCINE}){
                var stat=net.minecraft.stat.Stats.USED.getOrCreateStat(item);int before=player.getStatHandler().getStat(stat);item.finishUsing(new ItemStack(item),ctx.getWorld(),player);
                ctx.assertTrue(player.getStatHandler().getStat(stat)==before+1,"Medicine consumption failed to record native item use: "+item);
            }
        }finally{player.getServer().getPlayerManager().remove(player);player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void deathStillCuresDiseasesWithKeepInventoryEnabled(TestContext ctx){
        var original=survivalPlayer(ctx);var replacement=survivalPlayer(ctx);var rules=ctx.getWorld().getGameRules();boolean keep=rules.getBoolean(GameRules.KEEP_INVENTORY);
        try{
            rules.get(GameRules.KEEP_INVENTORY).set(true,ctx.getWorld().getServer());Infection.applyDurationChecked(original,null,9000,1);Rabies.expose(original,null);((ExpansionState)original).rustruin$setLostHealth(8);RabiesEffect.restorePenalty(original);
            replacement.copyFrom(original,false);
            ctx.assertTrue(!replacement.hasStatusEffect(TetanusMod.TETANUS)&&!replacement.hasStatusEffect(ExpansionEffects.RABIES)&&replacement.getMaxHealth()==20,"Death incorrectly retained diseases or Rabies health penalty");
        }finally{rules.get(GameRules.KEEP_INVENTORY).set(keep,ctx.getWorld().getServer());original.getServer().getPlayerManager().remove(original);original.discard();replacement.getServer().getPlayerManager().remove(replacement);replacement.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void enhancedRitualSupportsNativeWallSkullsAndBothAxes(TestContext ctx){
        float original=TetanusMod.CONFIG.witherConversionChance;
        try{
            TetanusMod.CONFIG.witherConversionChance=1;
            for(boolean enhanced:new boolean[]{false,true})for(var axis:new Direction[]{Direction.EAST,Direction.SOUTH})for(boolean mixed:new boolean[]{false,true}){
                for(int x=1;x<=5;x++)for(int y=2;y<=5;y++)for(int z=1;z<=5;z++)ctx.setBlockState(x,y,z,net.minecraft.block.Blocks.AIR);
                var center=ctx.getAbsolutePos(new BlockPos(3,3,3));var world=ctx.getWorld();var sand=ModBlocks.TETANIZED_SOUL_SAND.getDefaultState();
                world.setBlockState(center.down(),sand,2);for(int i=-1;i<=1;i++)world.setBlockState(center.offset(axis,i),i==0&&enhanced?net.minecraft.block.Blocks.NETHERITE_BLOCK.getDefaultState():sand,2);
                var facing=axis==Direction.EAST?Direction.NORTH:Direction.EAST;
                for(int i=-1;i<=1;i++){
                    var head=center.up().offset(axis,i);world.setBlockState(head.offset(facing.getOpposite()),net.minecraft.block.Blocks.STONE.getDefaultState(),2);
                    var state=mixed&&i==0?net.minecraft.block.Blocks.WITHER_SKELETON_SKULL.getDefaultState():net.minecraft.block.Blocks.WITHER_SKELETON_WALL_SKULL.getDefaultState().with(net.minecraft.block.WallSkullBlock.FACING,facing);world.setBlockState(head,state,2);
                }
                var last=center.up().offset(axis);net.minecraft.block.WitherSkullBlock.onPlaced(world,last,(net.minecraft.block.entity.SkullBlockEntity)world.getBlockEntity(last));
                var bosses=world.getEntitiesByClass(net.minecraft.entity.boss.WitherEntity.class,new Box(center).expand(4),e->true);
                ctx.assertTrue(bosses.size()==1&&com.tetanusmod.entity.MobVariants.permanent(bosses.get(0)),"Native wall-skull ritual failed, netherite="+enhanced+" axis="+axis+" mixed="+mixed);
                bosses.forEach(Entity::discard);ctx.assertTrue(world.isAir(center),"Ritual did not consume its center block");
            }
        }finally{TetanusMod.CONFIG.witherConversionChance=original;}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void mutatedPackAnimalRetainsPlayerEquipmentDrops(TestContext ctx){
        for(boolean rabid:new boolean[]{false,true}){
            var donkey=ctx.spawnEntity(EntityType.DONKEY,new Vec3d(3,4,3));var nbt=donkey.writeNbt(new net.minecraft.nbt.NbtCompound());nbt.putBoolean("ChestedHorse",true);nbt.put("SaddleItem",new ItemStack(Items.SADDLE).writeNbt(new net.minecraft.nbt.NbtCompound()));
            var cargo=new net.minecraft.nbt.NbtList();var diamonds=new ItemStack(Items.DIAMOND,3).writeNbt(new net.minecraft.nbt.NbtCompound());diamonds.putByte("Slot",(byte)2);cargo.add(diamonds);nbt.put("Items",cargo);donkey.readNbt(nbt);
            if(rabid)Rabies.makeRabid(donkey);else com.tetanusmod.entity.MobVariants.makePermanent(donkey);donkey.damage(ctx.getWorld().getDamageSources().genericKill(),10000);
            var drops=ctx.getWorld().getEntitiesByClass(ItemEntity.class,donkey.getBoundingBox().expand(3),e->true);
            for(var expected:new Item[]{Items.DIAMOND,Items.SADDLE,Items.CHEST})ctx.assertTrue(drops.stream().filter(e->e.getStack().isOf(expected)).mapToInt(e->e.getStack().getCount()).sum()==(expected==Items.DIAMOND?3:1),"Mutation lost player-owned pack animal equipment: "+expected);
            ctx.assertTrue(drops.stream().noneMatch(e->e.getStack().isOf(Items.LEATHER)),"Mutated pack animal retained ordinary animal resources");drops.forEach(Entity::discard);
        }ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void mutatedFoxKeepsCarriedEquipmentAndReplacesOnlyAnimalLoot(TestContext ctx){
        for(boolean rabid:new boolean[]{false,true}){
            var fox=ctx.spawnEntity(EntityType.FOX,new Vec3d(3,4,3));fox.equipStack(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND,2));fox.setEquipmentDropChance(EquipmentSlot.MAINHAND,2);
            if(rabid)Rabies.makeRabid(fox);else com.tetanusmod.entity.MobVariants.makePermanent(fox);fox.damage(ctx.getWorld().getDamageSources().genericKill(),10000);
            var drops=ctx.getWorld().getEntitiesByClass(ItemEntity.class,fox.getBoundingBox().expand(3),e->true);
            ctx.assertTrue(drops.stream().filter(e->e.getStack().isOf(Items.DIAMOND)).mapToInt(e->e.getStack().getCount()).sum()==2,"Mutated fox deleted carried player equipment");
            var meat=rabid?ExpansionItems.RABID_MEAT:ModItems.INFECTED_MEAT;int count=drops.stream().filter(e->e.getStack().isOf(meat)).mapToInt(e->e.getStack().getCount()).sum();ctx.assertTrue(count>=1&&count<=3,"Animal loot was missing or duplicated: "+count);drops.forEach(Entity::discard);
        }ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void enhancedRitualWorksThroughNativeDispenser(TestContext ctx)throws Exception{
        var world=ctx.getWorld();var center=ctx.getAbsolutePos(new BlockPos(3,3,3));var sand=ModBlocks.TETANIZED_SOUL_SAND.getDefaultState();
        world.setBlockState(center.down(),sand,2);for(int i=-1;i<=1;i++)world.setBlockState(center.offset(Direction.EAST,i),i==0?net.minecraft.block.Blocks.NETHERITE_BLOCK.getDefaultState():sand,2);
        for(int i=-1;i<=0;i++)world.setBlockState(center.up().offset(Direction.EAST,i),net.minecraft.block.Blocks.WITHER_SKELETON_SKULL.getDefaultState(),2);
        var last=center.up().east();var machine=last.north();world.setBlockState(machine,net.minecraft.block.Blocks.DISPENSER.getDefaultState().with(net.minecraft.block.DispenserBlock.FACING,Direction.SOUTH),2);
        var inventory=(net.minecraft.block.entity.DispenserBlockEntity)world.getBlockEntity(machine);inventory.setStack(0,new ItemStack(Items.WITHER_SKELETON_SKULL));
        ctx.assertTrue(net.minecraft.block.WitherSkullBlock.canDispense(world,last,inventory.getStack(0)),"Native dispenser refuses the netherite ritual base");
        var method=net.minecraft.block.DispenserBlock.class.getDeclaredMethod("dispense",net.minecraft.server.world.ServerWorld.class,BlockPos.class);method.setAccessible(true);method.invoke(net.minecraft.block.Blocks.DISPENSER,world,machine);
        var withers=world.getEntitiesByClass(net.minecraft.entity.boss.WitherEntity.class,new Box(center).expand(4),e->true);
        ctx.assertTrue(withers.size()==1&&inventory.getStack(0).isEmpty()&&world.isAir(center),"Dispenser did not complete/consume the enhanced ritual");withers.forEach(Entity::discard);ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void dedicatedGloveBreakDoesNotAnimateTheShieldSlot(TestContext ctx){
        final boolean[] wrongSlot={false};
        var player=new net.minecraft.entity.player.PlayerEntity(ctx.getWorld(),BlockPos.ORIGIN,0,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"glove-break-audit")){
            public boolean isSpectator(){return false;}public boolean isCreative(){return false;}
            @Override public void sendEquipmentBreakStatus(EquipmentSlot slot){wrongSlot[0]=true;}
        };
        var shield=new ItemStack(Items.SHIELD);player.setStackInHand(net.minecraft.util.Hand.OFF_HAND,shield);var gloves=new ItemStack(ModItems.PROTECTIVE_GLOVES);gloves.setDamage(gloves.getMaxDamage()-1);((GloveInventory)player).rustruin$gloves().setStack(0,gloves);
        InfectionProtection.allows(player,true,0);ctx.assertTrue(gloves.isEmpty(),"Fixture did not exhaust gloves");ctx.assertFalse(wrongSlot[0],"Breaking dedicated gloves used a vanilla equipment slot and animated another held item");ctx.assertTrue(player.getOffHandStack()==shield&&shield.getDamage()==0,"Glove break changed shield data");ctx.complete();
    }
}
