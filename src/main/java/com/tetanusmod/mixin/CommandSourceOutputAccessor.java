package com.tetanusmod.mixin;

import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Retains the native command source's output and feedback policy for locate aliases. */
@Mixin(ServerCommandSource.class)
public interface CommandSourceOutputAccessor {
    @Accessor("output") CommandOutput rustruin$output();
}