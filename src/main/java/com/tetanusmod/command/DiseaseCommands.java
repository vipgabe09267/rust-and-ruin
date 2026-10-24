package com.tetanusmod.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.tetanusmod.TetanusMod;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import java.util.*;

public final class DiseaseCommands {
    public static final List<String> EFFECTS=List.of("tetanus","lockjaw","salmonella","rabies","tetanus_resistance","rabies_resistance","disoriented");
    public static void initialize(){
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->{
            var targets=CommandManager.argument("targets",EntityArgumentType.entities());
            for(String name:EFFECTS){
                var effect=CommandManager.literal(name).executes(ctx->apply(ctx,name,name.equals("rabies")?-1:300,0));
                effect.then(CommandManager.argument("seconds",IntegerArgumentType.integer(1,3600))
                        .executes(ctx->apply(ctx,name,IntegerArgumentType.getInteger(ctx,"seconds"),0))
                        .then(CommandManager.argument("amplifier",IntegerArgumentType.integer(0,255)).executes(ctx->apply(ctx,name,IntegerArgumentType.getInteger(ctx,"seconds"),IntegerArgumentType.getInteger(ctx,"amplifier")))));
                effect.then(CommandManager.literal("infinite").executes(ctx->apply(ctx,name,-1,0)));
                targets.then(effect);
            }
            // Adds short names while preserving native namespaced /effect commands and their permissions.
            dispatcher.register(CommandManager.literal("effect").requires(source->source.hasPermissionLevel(2)).then(CommandManager.literal("give").then(targets)));
            var locate=CommandManager.literal("locate");
            for(var entry:Map.of("medic","medic_clinic","tent","medic_tent","workshop","workshop").entrySet()){
                locate.then(CommandManager.literal(entry.getKey()).executes(ctx->ctx.getSource().getServer().getCommandManager().executeWithPrefix(ctx.getSource(),"locate structure tetanusmod:"+entry.getValue())));
            }
            dispatcher.register(CommandManager.literal("rustandruin").requires(source->source.hasPermissionLevel(2)).then(locate));
        });
    }
    private static int apply(com.mojang.brigadier.context.CommandContext<net.minecraft.server.command.ServerCommandSource> ctx,String name,int seconds,int amplifier)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        StatusEffect effect=Registries.STATUS_EFFECT.get(TetanusMod.id(name));int changed=0;
        for(var target:EntityArgumentType.getEntities(ctx,"targets"))if(target instanceof LivingEntity living&&com.tetanusmod.expansion.CommandEffects.apply(living,new StatusEffectInstance(effect,seconds<0?-1:seconds*20,amplifier),ctx.getSource().getEntity()))changed++;
        final int count=changed;ctx.getSource().sendFeedback(()->net.minecraft.text.Text.translatable("commands.tetanusmod.effect_applied",name,count),true);return changed;
    }
}
