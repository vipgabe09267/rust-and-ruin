package com.tetanusmod.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.level.UnmodifiableLevelProperties;
import net.minecraft.world.level.storage.LevelStorage;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * TestServer discards datapack dimensions and disables structure generation globally.
 * Add one isolated, genuinely generated native noise world at SERVER_STARTED, before
 * world iteration begins. No templates, structure starts, POIs or residents are inserted.
 * This development-mod initializer never enters the distributable mod JAR.
 */
public final class TentNoiseTestWorldBootstrap implements ModInitializer {
    // Every run gets fresh native storage; a fixed key would reuse saved old structure starts.
    public static final RegistryKey<World> KEY=RegistryKey.of(RegistryKeys.WORLD,new Identifier("tetanusmod_tests","medic_caves_"+java.util.UUID.randomUUID().toString().replace("-","")));
    private static long nativeTicks;
    public static long nativeTicks(){return nativeTicks;}
    @Override public void onInitialize(){
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_WORLD_TICK.register(world->{if(world.getRegistryKey().equals(KEY))nativeTicks++;});
        ServerLifecycleEvents.SERVER_STARTED.register(TentNoiseTestWorldBootstrap::create);
    }

    @SuppressWarnings("unchecked")
    private static void create(MinecraftServer server){
        try{
            var mapField=MinecraftServer.class.getDeclaredField("worlds");mapField.setAccessible(true);
            var worlds=(Map<RegistryKey<World>,ServerWorld>)mapField.get(server);
            if(worlds.containsKey(KEY))throw new IllegalStateException("Native discovery world already exists before fixture setup");
            var sessionField=MinecraftServer.class.getDeclaredField("session");sessionField.setAccessible(true);
            var executorField=MinecraftServer.class.getDeclaredField("workerExecutor");executorField.setAccessible(true);
            DimensionOptions options;
            try(var stream=TentNoiseTestWorldBootstrap.class.getClassLoader().getResourceAsStream("data/tetanusmod_tests/dimension/medic_caves.json")){
                if(stream==null)throw new IllegalStateException("Missing native noise-world dimension fixture");
                var json=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8));
                options=DimensionOptions.CODEC.parse(RegistryOps.of(JsonOps.INSTANCE,server.getRegistryManager()),json).result().orElseThrow(()->new IllegalStateException("Cannot decode the native noise-world dimension fixture"));
            }
            var global=server.getSaveProperties().getGeneratorOptions();long seed=global.getSeed();
            var progress=new WorldGenerationProgressListener(){
                @Override public void start(ChunkPos spawn){}
                @Override public void setChunkStatus(ChunkPos pos,ChunkStatus status){}
                @Override public void start(){}
                @Override public void stop(){}
            };
            var properties=new UnmodifiableLevelProperties(server.getSaveProperties(),server.getSaveProperties().getMainWorldProperties());
            var world=new ServerWorld(server,(Executor)executorField.get(server),(LevelStorage.Session)sessionField.get(server),properties,KEY,options,progress,false,seed,List.of(),false,new RandomSequencesState(seed));
            // Native feature placement consults the world accessor; enable only this world's
            // copy. ChunkStatus's separate global gate is scoped by NoiseStructureGateMixin.
            var optionsField=StructureAccessor.class.getDeclaredField("options");optionsField.setAccessible(true);
            optionsField.set(world.getStructureAccessor(),new GeneratorOptions(seed,true,false));
            // Keep native player-presence coverage small without touching the arena's distances.
            world.getChunkManager().applyViewDistance(2);world.getChunkManager().applySimulationDistance(2);
            nativeTicks=0;worlds.put(KEY,world);
            if(server.getSaveProperties().getGeneratorOptions()!=global||global.shouldGenerateStructures())throw new IllegalStateException("Native discovery fixture changed the flat arena's global generation options");
        }catch(Exception failure){throw new IllegalStateException("Unable to create isolated native noise-world discovery fixture",failure);}
    }
}