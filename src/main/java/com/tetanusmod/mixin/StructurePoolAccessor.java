package com.tetanusmod.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.structure.pool.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

@Mixin(StructurePool.class)
public interface StructurePoolAccessor {
    @Accessor("elementCounts") List<Pair<StructurePoolElement,Integer>> rustruin$counts();
    @Mutable @Accessor("elementCounts") void rustruin$counts(List<Pair<StructurePoolElement,Integer>> entries);
    @Accessor("elements") it.unimi.dsi.fastutil.objects.ObjectArrayList<StructurePoolElement> rustruin$elements();
    @Accessor("highestY") void rustruin$highest(int value);
}