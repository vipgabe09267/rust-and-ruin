package com.tetanusmod.test;

import com.mojang.authlib.GameProfile;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.expansion.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.network.*;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.util.UUID;

/** Native raycast, interaction packets, and connected-player movement regressions. */
public class TrapInteractionRegressionGameTests implements FabricGameTest {
    private static final BlockPos TRAP=new BlockPos(3,3,3);

    private static final class Connection extends ClientConnection {
        private int teleportId=-1;
        Connection(){super(NetworkSide.SERVERBOUND);}
        @Override public void send(Packet<?> packet,PacketCallbacks callbacks){
            if(packet instanceof PlayerPositionLookS2CPacket position)teleportId=position.getTeleportId();
            super.send(packet,callbacks);
        }
    }
    private static final class Actor implements AutoCloseable {
        final ServerPlayerEntity player;
        final Connection connection=new Connection();
        int sequence;
        Actor(TestContext ctx,GameMode mode){
            var world=ctx.getWorld();
            player=new ServerPlayerEntity(world.getServer(),world,new GameProfile(UUID.randomUUID(),"trap-click-test"));
            world.getServer().getPlayerManager().onPlayerConnect(connection,player);
            player.changeGameMode(mode);player.getAbilities().flying=false;player.setNoGravity(true);
            ((com.tetanusmod.test.mixin.ServerPlayerTestAccessor)player).rustruin$joinInvulnerability(0);
            player.setHealth(player.getMaxHealth());
            acknowledge();
        }
        void acknowledge(){
            if(connection.teleportId>=0){int id=connection.teleportId;connection.teleportId=-1;player.networkHandler.onTeleportConfirm(new TeleportConfirmC2SPacket(id));}
        }
        void at(Vec3d position){
            player.networkHandler.requestTeleport(position.x,position.y,position.z,0,0);acknowledge();player.networkHandler.syncWithPlayerPosition();
        }
        void eyeAt(Vec3d eye){at(eye.add(0,-(player.getEyeY()-player.getY()),0));}
        void move(Vec3d delta){
            acknowledge();player.networkHandler.syncWithPlayerPosition();
            var next=player.getPos().add(delta);
            player.networkHandler.onPlayerMove(new PlayerMoveC2SPacket.PositionAndOnGround(next.x,next.y,next.z,true));
            acknowledge();
        }
        void click(BlockHitResult hit){
            acknowledge();player.networkHandler.onPlayerInteractBlock(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND,hit,++sequence));
        }
        void click(Entity entity){
            acknowledge();player.networkHandler.onPlayerInteractEntity(PlayerInteractEntityC2SPacket.interact(entity,false,Hand.MAIN_HAND));
        }
        @Override public void close(){player.closeHandledScreen();player.getServer().getPlayerManager().remove(player);player.discard();}
    }
    private BearTrapBlockEntity place(TestContext ctx,Block block){
        for(int x=0;x<=6;x++)for(int z=0;z<=6;z++){ctx.setBlockState(x,2,z,Blocks.STONE);for(int y=3;y<=6;y++)ctx.setBlockState(x,y,z,Blocks.AIR);}
        ctx.setBlockState(TRAP,block);
        return (BearTrapBlockEntity)ctx.getBlockEntity(TRAP);
    }
    private CowEntity walkCow(TestContext ctx,BearTrapBlockEntity trap){
        var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(2.1,3,3.5));cow.setAiDisabled(true);cow.setNoGravity(true);
        cow.move(MovementType.SELF,new Vec3d(1.4,0,0));
        ctx.assertTrue(trap.holds(cow),"Native cow movement did not close the trap fixture");return cow;
    }
    private BlockHitResult ray(TestContext ctx,Actor actor,Vec3d end){
        return ctx.getWorld().raycast(new RaycastContext(actor.player.getEyePos(),end,RaycastContext.ShapeType.OUTLINE,RaycastContext.FluidHandling.NONE,actor.player));
    }
    private void checkReset(TestContext ctx,Actor actor,BearTrapBlockEntity trap,boolean survival){
        ctx.assertTrue(!trap.getCachedState().get(BearTrapBlock.CLOSED)&&!trap.armed(),"Native reset did not open jaws with a safe rearm grace period");
        ctx.assertTrue(actor.player.getMainHandStack().getDamage()==(survival?1:0),"Reset charged the wrong native durability cost");
        var advancement=actor.player.getServer().getAdvancementLoader().get(TetanusMod.id("trap_reset"));
        ctx.assertTrue(actor.player.getAdvancementTracker().getProgress(advancement).isDone(),"Native reset did not award Mind Your Fingers");
    }
    private void raycastReset(TestContext ctx,Block block,Direction side){
        var trap=place(ctx,block);var cow=walkCow(ctx,trap);cow.discard();
        try(var actor=new Actor(ctx,GameMode.SURVIVAL)){
            actor.player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ExpansionItems.TRAP_RESET_TOOL));
            var p=Vec3d.of(trap.getPos());
            Vec3d start,end;
            if(side==Direction.UP){start=p.add(0.35,2,0.5);end=p.add(0.35,-0.1,0.5);}
            else{start=p.add(0.5+side.getOffsetX()*2,6.5/16.0,0.5+side.getOffsetZ()*2);end=p.add(0.5,6.5/16.0,0.5);}
            actor.eyeAt(start);var hit=ray(ctx,actor,end);
            ctx.assertTrue(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(trap.getPos()),"Visible closed jaw was not selectable from "+side+": "+hit.getBlockPos());
            ctx.assertTrue(hit.getSide()==side,"Ray selected wrong jaw face from "+side+": "+hit.getSide());
            if(side==Direction.UP)ctx.assertTrue(Math.abs(hit.getPos().y-trap.getPos().getY()-7/16.0)<0.00001,"Top selection does not cover actual jaw height");
            actor.click(hit);checkReset(ctx,actor,trap,true);
            ctx.assertTrue(ctx.getWorld().getBlockState(trap.getPos().down()).isOf(Blocks.STONE),"Reset required removing the support block");
        }finally{cow.discard();}
    }

    @GameTest(templateName=EMPTY_STRUCTURE) public void topJawRaycastResetsCleanTrapWithoutBreakingSupport(TestContext ctx){raycastReset(ctx,ExpansionBlocks.BEAR_TRAP,Direction.UP);ctx.complete();}
    @GameTest(templateName=EMPTY_STRUCTURE) public void topJawRaycastResetsRustyTrapWithoutBreakingSupport(TestContext ctx){raycastReset(ctx,ExpansionBlocks.RUSTED_BEAR_TRAP,Direction.UP);ctx.complete();}
    @GameTest(templateName=EMPTY_STRUCTURE) public void allFourSideJawRaycastsResetCleanTrap(TestContext ctx){for(var side:Direction.Type.HORIZONTAL)raycastReset(ctx,ExpansionBlocks.BEAR_TRAP,side);ctx.complete();}
    @GameTest(templateName=EMPTY_STRUCTURE) public void allFourSideJawRaycastsResetRustyTrap(TestContext ctx){for(var side:Direction.Type.HORIZONTAL)raycastReset(ctx,ExpansionBlocks.RUSTED_BEAR_TRAP,side);ctx.complete();}

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void nativeEntityClickReleasesCowObscuringTheJaws(TestContext ctx){
        var trap=place(ctx,ExpansionBlocks.BEAR_TRAP);var cow=walkCow(ctx,trap);
        try(var actor=new Actor(ctx,GameMode.SURVIVAL)){
            actor.at(ctx.getAbsolute(new Vec3d(1.5,3,3.5)));actor.player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ExpansionItems.TRAP_RESET_TOOL));
            var start=actor.player.getEyePos();var end=cow.getPos().add(0,0.8,0);
            var entityHit=ProjectileUtil.raycast(actor.player,start,end,actor.player.getBoundingBox().stretch(end.subtract(start)).expand(1),e->e==cow,start.squaredDistanceTo(end));
            ctx.assertTrue(entityHit!=null&&entityHit.getEntity()==cow,"Fixture does not reproduce the captive entity intercepting the crosshair");
            actor.click(cow);checkReset(ctx,actor,trap,true);ctx.assertFalse(trap.holds(cow),"Native entity click retained the captive cow");
        }finally{cow.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void captiveVillagerResetsBeforeOpeningItsTradingMenu(TestContext ctx){
        var trap=place(ctx,ExpansionBlocks.BEAR_TRAP);var villager=ctx.spawnEntity(EntityType.VILLAGER,new Vec3d(2.1,3,3.5));
        villager.setAiDisabled(true);villager.setNoGravity(true);villager.move(MovementType.SELF,new Vec3d(1.4,0,0));
        try(var actor=new Actor(ctx,GameMode.SURVIVAL)){
            ctx.assertTrue(trap.holds(villager),"Native villager movement did not trigger trap");villager.setVillagerData(villager.getVillagerData().withProfession(net.minecraft.village.VillagerProfession.FARMER));
            actor.at(ctx.getAbsolute(new Vec3d(1.5,3,3.5)));actor.player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ExpansionItems.TRAP_RESET_TOOL));
            actor.click(villager);checkReset(ctx,actor,trap,true);ctx.assertTrue(actor.player.currentScreenHandler==actor.player.playerScreenHandler,"Captive villager opened trading instead of releasing jaws");
        }finally{villager.discard();}ctx.complete();
    }
    private void connectedWalk(TestContext ctx,GameMode mode,boolean expected){
        var trap=place(ctx,ExpansionBlocks.BEAR_TRAP);
        try(var actor=new Actor(ctx,mode)){
            actor.at(ctx.getAbsolute(new Vec3d(2.1,3,3.5)));actor.move(new Vec3d(1.4,0,0));
            ctx.assertTrue(trap.holds(actor.player)==expected,"Native "+mode+" movement had wrong player capture state");
            ctx.assertTrue(actor.player.getHealth()==(mode==GameMode.SURVIVAL?17:20),"Trap damage did not respect native "+mode+" immunity");
            if(expected){
                var anchor=actor.player.getPos();actor.move(new Vec3d(0.4,0.42,0));
                BearTrapBlockEntity.tick(ctx.getWorld(),trap.getPos(),trap.getCachedState(),trap);actor.acknowledge();
                ctx.assertTrue(actor.player.squaredDistanceTo(anchor)<0.0005&&trap.holds(actor.player),"Native player move/jump escaped the trap restraint");
                actor.player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ExpansionItems.TRAP_RESET_TOOL));
                var hit=ray(ctx,actor,Vec3d.of(trap.getPos()).add(0.5,0.1,0.5));
                ctx.assertTrue(hit.getBlockPos().equals(trap.getPos()),"Captured player cannot see the trap for self-reset");actor.click(hit);checkReset(ctx,actor,trap,mode==GameMode.SURVIVAL);
                ctx.assertFalse(trap.holds(actor.player),"Reset kept the native connected player captive");
            }
        }
    }
    @GameTest(templateName=EMPTY_STRUCTURE) public void nativeSurvivalMovementCapturesAndAllowsSelfReset(TestContext ctx){connectedWalk(ctx,GameMode.SURVIVAL,true);ctx.complete();}
    @GameTest(templateName=EMPTY_STRUCTURE) public void nativeCreativeMovementRemainsImmuneToCaptureAndDamage(TestContext ctx){connectedWalk(ctx,GameMode.CREATIVE,false);ctx.complete();}
    @GameTest(templateName=EMPTY_STRUCTURE) public void nativeSpectatorMovementStaysUncaptured(TestContext ctx){connectedWalk(ctx,GameMode.SPECTATOR,false);ctx.complete();}

    @GameTest(templateName=EMPTY_STRUCTURE)
    public void entityResetDoesNotPassThroughSolidWalls(TestContext ctx){
        var trap=place(ctx,ExpansionBlocks.BEAR_TRAP);var cow=walkCow(ctx,trap);
        try(var actor=new Actor(ctx,GameMode.SURVIVAL)){
            actor.at(ctx.getAbsolute(new Vec3d(1.5,3,3.5)));actor.player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ExpansionItems.TRAP_RESET_TOOL));
            for(int y=3;y<=5;y++)ctx.setBlockState(2,y,3,Blocks.STONE);
            actor.click(cow);ctx.assertTrue(trap.holds(cow)&&actor.player.getMainHandStack().getDamage()==0,"Entity click reset a trap through a solid wall");
        }finally{cow.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void entityResetOnlyReleasesTheActuallyClickedCaptive(TestContext ctx){
        var trap=place(ctx,ExpansionBlocks.BEAR_TRAP);var cow=walkCow(ctx,trap);var other=ctx.spawnEntity(EntityType.COW,new Vec3d(3.5,3,4.5));other.setAiDisabled(true);
        try(var actor=new Actor(ctx,GameMode.SURVIVAL)){
            actor.at(ctx.getAbsolute(new Vec3d(1.5,3,4.5)));actor.player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ExpansionItems.TRAP_RESET_TOOL));
            actor.click(other);ctx.assertTrue(trap.holds(cow)&&actor.player.getMainHandStack().getDamage()==0,"An unrelated entity click reset another creature's trap");
        }finally{cow.discard();other.discard();}ctx.complete();
    }
}
