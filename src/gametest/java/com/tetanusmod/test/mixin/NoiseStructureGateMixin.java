package com.tetanusmod.test.mixin;

import com.tetanusmod.test.TentNoiseTestWorldBootstrap;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Function;

/** Preserve the flat test arena while allowing native structure-start generation in the noise fixture. */
@Mixin(ChunkStatus.class)
public abstract class NoiseStructureGateMixin {
    @Redirect(method="method_39464",at=@At(value="INVOKE",target="Lnet/minecraft/world/gen/GeneratorOptions;shouldGenerateStructures()Z"))
    private static boolean rustruin$nativeStructuresOnlyInNoiseFixture(GeneratorOptions options,ChunkStatus status,Executor executor,ServerWorld world,ChunkGenerator generator,StructureTemplateManager templates,ServerLightingProvider lighting,Function<?,?> fullChunkConverter,List<Chunk> neighbors,Chunk chunk){
        return world.getRegistryKey().equals(TentNoiseTestWorldBootstrap.KEY)||options.shouldGenerateStructures();
    }
}