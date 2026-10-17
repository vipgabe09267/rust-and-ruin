package com.tetanusmod.command;
import com.tetanusmod.entity.MobVariants;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.*;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import static net.minecraft.server.command.CommandManager.*;
public final class RustRuinCommands {
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->dispatcher.register(literal("rustandruin")
            .requires(source->source.hasPermissionLevel(2))
            .then(literal("summon").then(argument("mob",IdentifierArgumentType.identifier())
                .suggests((ctx,builder)->CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getIds(),builder))
                .executes(ctx->summon(ctx.getSource(),IdentifierArgumentType.getIdentifier(ctx,"mob").toString(),ctx.getSource().getPosition()))
                .then(argument("pos",Vec3ArgumentType.vec3()).executes(ctx->summon(ctx.getSource(),IdentifierArgumentType.getIdentifier(ctx,"mob").toString(),Vec3ArgumentType.getVec3(ctx,"pos"))))))
            .then(literal("tetanize").then(argument("targets",EntityArgumentType.entities()).executes(ctx->{
                int changed=0;for(var entity:EntityArgumentType.getEntities(ctx,"targets"))if(entity instanceof MobEntity mob&&!MobVariants.permanent(mob)){MobVariants.makePermanent(mob);changed++;}
                final int count=changed;ctx.getSource().sendFeedback(()->Text.translatable("commands.tetanusmod.tetanized",count),true);return changed;
            })))));
    }
    public static int summon(ServerCommandSource source,String name,Vec3d pos)throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Identifier id=Identifier.tryParse(name);
        var type=id==null?null:Registries.ENTITY_TYPE.getOrEmpty(id).orElse(null);
        if(type==null||!net.minecraft.world.World.isValid(net.minecraft.util.math.BlockPos.ofFloored(pos)))throw error("commands.tetanusmod.invalid");
        var entity=type.create(source.getWorld());
        if(!(entity instanceof MobEntity mob))throw error("commands.tetanusmod.invalid");
        mob.refreshPositionAndAngles(pos.x,pos.y,pos.z,0,0);
        mob.initialize(source.getWorld(),source.getWorld().getLocalDifficulty(mob.getBlockPos()),SpawnReason.COMMAND,null,null);
        MobVariants.makePermanent(mob);
        if(!source.getWorld().spawnEntity(mob))throw error("commands.tetanusmod.failed");
        source.sendFeedback(()->Text.translatable("commands.tetanusmod.summoned",mob.getDisplayName()),true);return 1;
    }
    private static com.mojang.brigadier.exceptions.CommandSyntaxException error(String key){return new SimpleCommandExceptionType(Text.translatable(key)).create();}
}
