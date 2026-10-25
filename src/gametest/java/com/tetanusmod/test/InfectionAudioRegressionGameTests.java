package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.Infection;
import com.tetanusmod.entity.*;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import java.util.*;

/** One isolated scene observes native mob-world and player-S2C infection audio. */
public class InfectionAudioRegressionGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="infection_audio_regression",tickLimit=40)
    public void onlyNewInfectionsAnnounceOnBothNativeSoundPaths(TestContext ctx){new Scene(ctx).start();}
    private static final class Scene {
        private final TestContext ctx;
        private final ServerPlayerEntity player;
        private final net.minecraft.entity.passive.CowEntity cow;
        private final List<Entity> fixtures=new ArrayList<>();
        Scene(TestContext ctx){
            this.ctx=ctx;player=ctx.createMockCreativeServerPlayerInWorld();player.setPosition(ctx.getAbsolute(new Vec3d(2,3,2)));
            cow=EntityType.COW.create(ctx.getWorld());cow.setPosition(ctx.getAbsolute(new Vec3d(3,3,3)));fixtures.add(cow);
        }
        void start(){
            SoundProbe.begin(ctx.getWorld(),new Box(ctx.getAbsolute(new Vec3d(-4,-32,-4)),ctx.getAbsolute(new Vec3d(16,32,16))));
            try{
                for(var item:new Item[]{ModItems.NAIL,ModItems.RUSTED_NAIL,ModItems.BENT_NAIL,ModItems.RUSTED_BENT_NAIL}){
                    player.getItemCooldownManager().set(item,0);player.setStackInHand(Hand.MAIN_HAND,new ItemStack(item));
                    item.use(ctx.getWorld(),player,Hand.MAIN_HAND);
                }
                var thrown=ctx.getWorld().getEntitiesByClass(NailEntity.class,player.getBoundingBox().expand(8),e->e.getOwner()==player);
                ctx.assertTrue(thrown.size()==4,"All four real nail uses must spawn projectiles");thrown.forEach(Entity::discard);
                ctx.assertTrue(SoundProbe.count==0,"A throw without infection played the voice");
                var wither=EntityType.WITHER.create(ctx.getWorld());fixtures.add(wither);wither.setPosition(ctx.getAbsolute(new Vec3d(4,3,4)));MobVariants.makePermanent(wither);
                ctx.assertTrue(!wither.hasStatusEffect(TetanusMod.TETANUS)&&SoundProbe.count==0,"A non-infecting boss mutation played the voice");
                for(int y=3;y<=6;y++)for(int z=1;z<=3;z++)ctx.setBlockState(7,y,z,Blocks.STONE);
                var arrow=new TetanizedArrowEntity(ctx.getWorld(),player);fixtures.add(arrow);arrow.setPosition(ctx.getAbsolute(new Vec3d(3.5,4,2.5)));arrow.setVelocity(1.2,0,0);arrow.setNoGravity(true);ctx.getWorld().spawnEntity(arrow);
                for(int i=0;i<8;i++)arrow.tick();
                ctx.assertTrue(arrow.writeNbt(new net.minecraft.nbt.NbtCompound()).getBoolean("inGround"),"Arrow did not actually hit its stone wall");
                ctx.assertTrue(SoundProbe.count==0,"A missed arrow hitting a block played the infection voice");arrow.discard();
                ctx.assertTrue(Infection.apply(cow,null)&&cow.hasStatusEffect(TetanusMod.TETANUS)&&SoundProbe.count==1,"New mob infection did not announce exactly once");
                queue(2,()->{Infection.applyDurationChecked(cow,null,18000,0);ctx.assertTrue(SoundProbe.count==1,"A later repeat exposure replayed the voice: count="+SoundProbe.count+" last="+SoundProbe.position);});
                queue(3,()->{cow.addStatusEffect(new StatusEffectInstance(TetanusMod.TETANUS,22000,1));ctx.assertTrue(SoundProbe.count==1,"Native effect upgrade replayed the voice");});
                queue(4,()->{cow.removeStatusEffect(TetanusMod.TETANUS);ctx.assertTrue(SoundProbe.count==1,"Recovery played an infection announcement");});
                queue(6,()->{
                    ctx.assertTrue(Infection.apply(cow,null),"Second genuine mob infection failed");
                    ctx.assertTrue(Infection.apply(player,null)&&player.hasStatusEffect(TetanusMod.TETANUS),"New player infection failed");
                    ctx.assertTrue(SoundProbe.count==3,"New mob/player episodes did not each announce once");
                });
                queue(8,()->{Infection.applyDurationChecked(player,null,18000,0);ctx.assertTrue(SoundProbe.count==3,"Player refresh replayed its private S2C voice");});
                queue(9,()->{player.addStatusEffect(new StatusEffectInstance(TetanusMod.TETANUS,22000,1));ctx.assertTrue(SoundProbe.count==3,"Player native upgrade replayed the voice");});
                queue(10,()->{
                    player.removeStatusEffect(TetanusMod.TETANUS);ctx.assertTrue(Infection.apply(player,null),"Fresh player reinfection failed");
                    ctx.assertTrue(SoundProbe.count==4,"Fresh player reinfection did not announce once");
                    var protectedCow=EntityType.COW.create(ctx.getWorld());fixtures.add(protectedCow);protectedCow.addStatusEffect(new StatusEffectInstance(TetanusMod.VACCINATED,1000));
                    float chance=TetanusMod.CONFIG.vaccineProtectionChance;
                    try{TetanusMod.CONFIG.vaccineProtectionChance=1;ctx.assertTrue(!Infection.apply(protectedCow,null),"Protected infection was not blocked");}
                    finally{TetanusMod.CONFIG.vaccineProtectionChance=chance;}
                    ctx.assertTrue(SoundProbe.count==4,"A rejected infection played the voice");cleanup();ctx.complete();
                });
            }catch(RuntimeException|Error failure){cleanup();throw failure;}
        }
        private void queue(int tick,Runnable action){ctx.runAtTick(tick,()->{try{action.run();}catch(RuntimeException|Error failure){cleanup();throw failure;}});}
        private void cleanup(){SoundProbe.enabled=false;fixtures.forEach(Entity::discard);player.closeHandledScreen();var manager=player.getServer().getPlayerManager();if(manager.getPlayerList().contains(player))manager.remove(player);player.discard();}
    }
}
