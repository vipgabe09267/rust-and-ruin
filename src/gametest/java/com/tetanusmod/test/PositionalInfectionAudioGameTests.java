package com.tetanusmod.test;

import com.mojang.authlib.GameProfile;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.config.ModConfig;
import com.tetanusmod.effect.Infection;
import com.tetanusmod.entity.*;
import com.tetanusmod.expansion.*;
import com.tetanusmod.registry.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.*;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.*;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import java.util.*;

/** Native packet delivery verifies listeners, gain, origins and silent lifecycle paths. */
public class PositionalInfectionAudioGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="positional_infection_audio",tickLimit=80)
    public void proximityLifecycleAndMedicWorkUseRealNativeSoundPackets(TestContext ctx){new Scene(ctx).start();}

    private static final class Connection extends ClientConnection {
        final List<PlaySoundS2CPacket> sounds=new ArrayList<>();
        final List<PlaySoundFromEntityS2CPacket> entitySounds=new ArrayList<>();
        Connection(){super(NetworkSide.SERVERBOUND);}
        @Override public void send(Packet<?> packet,PacketCallbacks callbacks){
            if(packet instanceof PlaySoundS2CPacket sound)sounds.add(sound);
            if(packet instanceof PlaySoundFromEntityS2CPacket sound)entitySounds.add(sound);
            super.send(packet,callbacks);
        }
    }
    private static final class Actor implements AutoCloseable {
        final Connection connection=new Connection();final ServerPlayerEntity player;
        Actor(TestContext ctx,Vec3d position,String suffix){
            var world=ctx.getWorld();player=new ServerPlayerEntity(world.getServer(),world,new GameProfile(UUID.randomUUID(),"audio-"+suffix));
            try{world.getServer().getPlayerManager().onPlayerConnect(connection,player);player.changeGameMode(GameMode.CREATIVE);player.setNoGravity(true);player.setPosition(position);}catch(RuntimeException|Error failure){close();throw failure;}
        }
        List<PlaySoundS2CPacket> packets(SoundEvent event){return connection.sounds.stream().filter(packet->packet.getSound().value()==event).toList();}
        @Override public void close(){if(player.networkHandler!=null)player.closeHandledScreen();var manager=player.getServer().getPlayerManager();if(manager.getPlayerList().contains(player))manager.remove(player);player.discard();}
    }
    private static final class Scene {
        private final TestContext ctx;private final ModConfig originalConfig;private final Difficulty originalDifficulty;
        private final List<Actor> actors=new ArrayList<>();private final List<Entity> fixtures=new ArrayList<>();
        private final Vec3d center;private Actor victim,near,edge,far,distant;private CowEntity nailed;private boolean cleaned;
        Scene(TestContext ctx){
            this.ctx=ctx;center=ctx.getAbsolute(new Vec3d(3.5,3,3.5));originalConfig=TetanusMod.CONFIG;originalDifficulty=ctx.getWorld().getDifficulty();
        }
        void start(){
            try{
                var config=new ModConfig();config.infectionSoundVolume=4;config.mobConversionChance=0;config.animalConversionChance=0;
                config.naturalMobEasy=config.naturalMobNormal=config.naturalMobHard=1;config.naturalAnimalEasy=config.naturalAnimalNormal=config.naturalAnimalHard=1;
                config.spawnerEasy=config.spawnerNormal=config.spawnerHard=1;TetanusMod.CONFIG=config;ctx.getWorld().getServer().setDifficulty(Difficulty.NORMAL,true);
                victim=actor(0,"victim");near=actor(8,"near");edge=actor(15.5,"edge");far=actor(16.5,"far");distant=actor(32,"distant");clear();
                naturalAndReloadRemainQuiet();
                ctx.assertTrue(total(TetanusMod.INFECTION_SOUND)==0,"Natural initialization, NBT load, permanent restoration or inherited conversion announced an infection");proof("natural-spawner-chunk-generation-NBT-maintenance-inherited-conversion-quiet");
                nailed=cow(center);var nail=new NailEntity(ctx.getWorld(),distant.player);fixtures.add(nail);nail.setItem(new ItemStack(ModItems.RUSTED_NAIL));nail.setPosition(center.add(4,0.7,0));nail.setVelocity(-1,0,0);nail.setNoGravity(true);ctx.getWorld().spawnEntity(nail);
                for(int i=0;i<8&&nail.isAlive();i++)nail.tick();
                ctx.assertTrue(!nail.isAlive()&&nailed.hasStatusEffect(TetanusMod.TETANUS),"Real native rusty-nail collision failed to infect the target");
                expectVoice(center,SoundCategory.NEUTRAL);proof("native-rusty-nail-hit-victim-origin-near-edge-only");clear();
                queue(2,()->{
                    Infection.applyDurationChecked(nailed,distant.player,18000,0);nailed.addStatusEffect(new StatusEffectInstance(TetanusMod.TETANUS,22000,1));
                    ctx.assertTrue(total(TetanusMod.INFECTION_SOUND)==0,"An existing infection refresh or native upgrade replayed the voice");proof("refresh-and-native-upgrade-quiet");
                });
                queue(4,()->{command("effect give @s tetanusmod:tetanus 300");ctx.assertTrue(victim.player.hasStatusEffect(TetanusMod.TETANUS),"Native effect command did not apply Tetanus");expectVoice(victim.player.getPos(),SoundCategory.PLAYERS);proof("native-player-effect-command-fresh-episode-nearby-positional");clear();});
                queue(6,()->{command("effect give @s tetanusmod:tetanus 600 1");ctx.assertTrue(total(TetanusMod.INFECTION_SOUND)==0,"Native effect-command refresh replayed the voice");proof("native-player-command-refresh-quiet");});
                queue(8,()->{command("effect clear @s tetanusmod:tetanus");command("effect give @s tetanusmod:tetanus 300");expectVoice(victim.player.getPos(),SoundCategory.PLAYERS);proof("native-player-command-reinfection-after-cure");clear();});
                queue(10,()->{
                    ctx.setBlockState(3,2,3,Blocks.STONE);ctx.setBlockState(3,3,3,ExpansionBlocks.RUSTED_BEAR_TRAP);
                    var trapped=cow(center.add(-1.4,0,0));trapped.move(MovementType.SELF,new Vec3d(1.4,0,0));
                    var trap=(BearTrapBlockEntity)ctx.getBlockEntity(new BlockPos(3,3,3));ctx.assertTrue(trap.holds(trapped)&&trapped.hasStatusEffect(TetanusMod.TETANUS),"Native walking contact did not activate the rusty bear trap");
                    expectVoice(Vec3d.ofCenter(trap.getPos()),SoundCategory.BLOCKS);proof("native-walking-rusty-trap-contact-block-origin");clear();
                });
                queue(12,()->{
                    var medic=EntityType.VILLAGER.create(ctx.getWorld());fixtures.add(medic);medic.setPosition(center);medic.setVillagerData(medic.getVillagerData().withProfession(MedicVillage.MEDIC));
                    ctx.assertTrue(MedicVillage.MEDIC.workSound()==ModSounds.MEDIC_WORK&&ModSounds.MEDIC_WORK.getDistanceToTravel(4)==12,"Medic profession does not have its own bounded work sound");
                    medic.playWorkSound();
                    for(var actor:actors){int expected=actor.player.squaredDistanceTo(center)<144?1:0;var packets=actor.packets(ModSounds.MEDIC_WORK);ctx.assertTrue(packets.size()==expected,"Native medic work packets reached wrong listener: "+actor.player.getEntityName());}
                    ctx.assertTrue(total(TetanusMod.INFECTION_SOUND)==0,"Ordinary medic work announced an infection");proof("native-medic-work-custom-event-12-block-listeners");clear();
                });
                queue(14,()->{
                    var target=cow(center);command("rustandruin tetanize "+target.getUuid());ctx.assertTrue(MobVariants.permanent(target),"Native operator conversion did not mutate its actual target");expectVoice(center,SoundCategory.NEUTRAL);proof("native-explicit-operator-conversion-fresh-voice");clear();
                });
                queue(16,()->{TetanusMod.CONFIG.infectionSoundVolume=0;var quiet=cow(center);ctx.assertTrue(Infection.apply(quiet,null),"Zero-volume setup did not actually infect");ctx.assertTrue(total(TetanusMod.INFECTION_SOUND)==0,"Zero sound volume still sent infection packets");proof("zero-volume-sends-no-packets");cleanup();ctx.complete();});
            }catch(RuntimeException|Error failure){cleanup();throw failure;}
        }
        private void proof(String action){org.slf4j.LoggerFactory.getLogger("RustRuinPositionalAudio").info("POSITIONAL_AUDIO_PASS {}",action);}
        private Actor actor(double x,String name){var actor=new Actor(ctx,center.add(x,0,2),name);actors.add(actor);return actor;}
        private CowEntity cow(Vec3d position){var cow=EntityType.COW.create(ctx.getWorld());cow.setAiDisabled(true);cow.setNoGravity(true);cow.setPosition(position);fixtures.add(cow);ctx.getWorld().spawnEntity(cow);return cow;}
        private void naturalAndReloadRemainQuiet(){
            CowEntity original=null;
            for(var reason:List.of(SpawnReason.NATURAL,SpawnReason.CHUNK_GENERATION,SpawnReason.SPAWNER))for(var type:List.of(EntityType.COW,EntityType.VILLAGER,EntityType.ZOMBIE)){
                var mob=(MobEntity)type.create(ctx.getWorld());fixtures.add(mob);mob.setAiDisabled(true);mob.setNoGravity(true);mob.setPosition(center);mob.initialize(ctx.getWorld(),ctx.getWorld().getLocalDifficulty(mob.getBlockPos()),reason,null,null);
                ctx.assertTrue(MobVariants.permanent(mob)&&mob.hasStatusEffect(TetanusMod.TETANUS),"Guaranteed native "+reason+" mutation was not actually initialized");if(mob instanceof CowEntity cow)original=cow;
            }
            var saved=original.writeNbt(new NbtCompound());
            var loaded=EntityType.COW.create(ctx.getWorld());fixtures.add(loaded);loaded.readNbt(saved);MobVariants.loaded(loaded);ctx.assertTrue(ctx.getWorld().spawnEntity(loaded),"Native NBT reload was not inserted into entity tracking");
            ctx.assertTrue(MobVariants.permanent(loaded)&&loaded.hasStatusEffect(TetanusMod.TETANUS),"Actual NBT reload lost mutation or infection");loaded.discard();
            saved.remove("ActiveEffects");saved.putUuid("UUID",UUID.randomUUID());var legacy=EntityType.COW.create(ctx.getWorld());fixtures.add(legacy);legacy.readNbt(saved);ctx.assertTrue(ctx.getWorld().spawnEntity(legacy),"Native legacy entity was not inserted into tracking");legacy.age=39;ctx.getWorld().tickEntity(legacy);ctx.assertTrue(legacy.age==40,"Native world tick did not advance the legacy entity to its maintenance age");
            ctx.assertTrue(MobVariants.permanent(legacy)&&legacy.hasStatusEffect(TetanusMod.TETANUS),"Permanent legacy effect was not actually restored by a native living tick");legacy.discard();
            var zombie=EntityType.ZOMBIE.create(ctx.getWorld());fixtures.add(zombie);zombie.setPosition(center);zombie.initialize(ctx.getWorld(),ctx.getWorld().getLocalDifficulty(zombie.getBlockPos()),SpawnReason.NATURAL,null,null);ctx.getWorld().spawnEntity(zombie);
            var converted=zombie.convertTo(EntityType.HUSK,false);ctx.assertTrue(converted!=null&&MobVariants.permanent(converted)&&converted.hasStatusEffect(TetanusMod.TETANUS),"Native mob conversion did not preserve the actual mutation");fixtures.add(converted);converted.discard();
        }
        private void expectVoice(Vec3d origin,SoundCategory category){
            for(var actor:actors){
                int expected=actor.player.squaredDistanceTo(origin)<256?1:0;var packets=actor.packets(TetanusMod.INFECTION_SOUND);
                ctx.assertTrue(packets.size()==expected,"Infection packet count/radius wrong for "+actor.player.getEntityName()+": "+packets.size()+" expected "+expected);
                for(var packet:packets){ctx.assertTrue(packet.getVolume()>0&&packet.getVolume()<=1,"Native gain exceeded one and enlarged the sound radius");ctx.assertTrue(new Vec3d(packet.getX(),packet.getY(),packet.getZ()).squaredDistanceTo(origin)<0.02,"Infection packet was not located at actual victim/hit/trap");ctx.assertTrue(packet.getCategory()==category,"Infection used wrong native source category");}
                ctx.assertTrue(actor.connection.entitySounds.stream().noneMatch(packet->packet.getSound().value()==TetanusMod.INFECTION_SOUND),"Infection incorrectly used an entity-relative private sound path");
            }
        }
        private int total(SoundEvent event){return actors.stream().mapToInt(actor->actor.packets(event).size()).sum();}
        private void clear(){for(var actor:actors){actor.connection.sounds.clear();actor.connection.entitySounds.clear();}}
        private void command(String command){try{ctx.getWorld().getServer().getCommandManager().getDispatcher().execute(command,victim.player.getCommandSource().withLevel(4));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException failure){throw new IllegalStateException("Native audio command failed: "+command,failure);}}
        private void queue(int tick,Runnable action){ctx.runAtTick(tick,()->{try{action.run();}catch(RuntimeException|Error failure){cleanup();throw failure;}});}
        private void cleanup(){
            if(cleaned)return;cleaned=true;TetanusMod.CONFIG=originalConfig;RuntimeException failure=null;
            try{ctx.getWorld().getServer().setDifficulty(originalDifficulty,true);}catch(RuntimeException problem){failure=problem;}
            for(var entity:fixtures)try{entity.discard();}catch(RuntimeException problem){if(failure==null)failure=problem;else failure.addSuppressed(problem);}
            for(var actor:actors)try{actor.close();}catch(RuntimeException problem){if(failure==null)failure=problem;else failure.addSuppressed(problem);}
            if(failure!=null)throw failure;
        }
    }
}
