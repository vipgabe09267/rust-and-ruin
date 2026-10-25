package com.tetanusmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.tetanusmod.mixin.CommandSourceOutputAccessor;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.math.BlockPos;
import java.util.Map;

/** Branded command names resolve existing saved registry entries; no duplicate structures or POIs. */
public final class LocateAliases {
    private static final Map<String,String> STRUCTURES=Map.of("medic","medic_clinic","medic_clinic","medic_clinic","medic_tent","medic_tent","workshop","workshop");
    private static final Map<String,String> POIS=Map.of("medic","medic","medic_tent","medic_tent");
    private LocateAliases(){}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
        var root=CommandManager.literal("locate").requires(source->source.hasPermissionLevel(2));
        var structures=CommandManager.literal("structure");
        STRUCTURES.forEach((alias,id)->structures.then(CommandManager.literal("rustandruin:"+alias).executes(ctx->execute(ctx.getSource(),"structure",id))));
        var pois=CommandManager.literal("poi");
        POIS.forEach((alias,id)->pois.then(CommandManager.literal("rustandruin:"+alias).executes(ctx->execute(ctx.getSource(),"poi",id))));
        dispatcher.register(root.then(structures).then(pois));
    }

    public static int execute(ServerCommandSource source,String kind,String path)throws CommandSyntaxException{
        if(!source.hasPermissionLevel(2))throw new SimpleCommandExceptionType(Text.translatable("commands.generic.permission")).create();
        var allowed=switch(kind){case "structure"->STRUCTURES;case "poi"->POIS;default->throw new IllegalArgumentException("Unknown locate kind");};
        if(!allowed.containsValue(path))throw new IllegalArgumentException("Unknown Rust & Ruin locate target");
        var original=((CommandSourceOutputAccessor)source).rustruin$output();
        var located=new java.util.concurrent.atomic.AtomicReference<BlockPos>();
        CommandOutput branded=new CommandOutput(){
            @Override public void sendMessage(Text text){
                if(kind.equals("structure")&&(path.equals("medic_tent")||path.equals("medic_clinic"))&&hasKey(text,"commands.locate.structure.success"))located.set(coordinates(text));
                var rewritten=rewrite(text);
                if(kind.equals("poi")&&hasKey(text,"commands.locate.poi.not_found"))
                    rewritten.append(Text.translatable("commands.rustandruin.poi_hint"));
                original.sendMessage(rewritten);
            }
            @Override public boolean shouldReceiveFeedback(){return original.shouldReceiveFeedback();}
            @Override public boolean shouldTrackOutput(){return original.shouldTrackOutput();}
            @Override public boolean shouldBroadcastConsoleToOps(){return original.shouldBroadcastConsoleToOps();}
            @Override public boolean cannotBeSilenced(){return original.cannotBeSilenced();}
        };
        int result=source.getServer().getCommandManager().executeWithPrefix(source.withOutput(branded),"locate "+kind+" tetanusmod:"+path);
        if(located.get()!=null)sendStructureEntrance(source,located.get(),path);
        return result;
    }

    /** A structure start is already planned by native locate. Read only that start chunk,
     * then offer the actual clinic/camp entrance instead of the start-chunk ~Y hint. */
    private static void sendStructureEntrance(ServerCommandSource source,BlockPos located,String path){
        var world=source.getWorld();
        var type=world.getRegistryManager().get(net.minecraft.registry.RegistryKeys.STRUCTURE).get(com.tetanusmod.TetanusMod.id(path));
        if(type==null)return;
        var chunk=world.getChunk(located.getX()>>4,located.getZ()>>4,net.minecraft.world.chunk.ChunkStatus.STRUCTURE_STARTS);
        var start=chunk.getStructureStart(type);
        if(start==null||!start.hasChildren()||!(start.getChildren().get(0) instanceof com.tetanusmod.world.MedicStructurePiece piece))return;
        var entrance=path.equals("medic_tent")?piece.campEntrance():piece.clinicEntrance();
        var text=Text.translatable("chat.coordinates",entrance.getX(),entrance.getY(),entrance.getZ()).styled(style->style
                .withColor(net.minecraft.util.Formatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/tp @s "+entrance.getX()+" "+entrance.getY()+" "+entrance.getZ()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,Text.translatable("chat.coordinates.tooltip"))));
        source.sendFeedback(()->Text.translatable(path.equals("medic_tent")?"commands.rustandruin.camp_entrance":"commands.rustandruin.clinic_entrance",text),false);
    }
    private static BlockPos coordinates(Text text){
        if(text.getContent() instanceof TranslatableTextContent content){
            var args=content.getArgs();
            if(content.getKey().equals("chat.coordinates")&&args.length>=3&&args[0] instanceof Number x&&args[2] instanceof Number z)return new BlockPos(x.intValue(),0,z.intValue());
            for(var arg:args)if(arg instanceof Text child){var found=coordinates(child);if(found!=null)return found;}
        }
        for(var child:text.getSiblings()){var found=coordinates(child);if(found!=null)return found;}
        return null;
    }
    /** Preserve translation arguments, coordinate links, colors and hover styles. */
    private static MutableText rewrite(Text text){
        MutableText result;
        if(text.getContent() instanceof TranslatableTextContent translated){
            var args=translated.getArgs().clone();
            for(int i=0;i<args.length;i++){
                if(args[i] instanceof Text child)args[i]=rewrite(child);
                else if(args[i] instanceof String value)args[i]=branded(value);
            }
            result=MutableText.of(new TranslatableTextContent(translated.getKey(),translated.getFallback(),args));
        }else if(text.getContent() instanceof LiteralTextContent literal)result=Text.literal(branded(literal.string()));
        else result=MutableText.of(text.getContent());
        result.setStyle(text.getStyle());
        for(var sibling:text.getSiblings())result.append(rewrite(sibling));
        return result;
    }
    private static boolean hasKey(Text text,String key){
        if(text.getContent() instanceof TranslatableTextContent content){
            if(content.getKey().equals(key))return true;
            for(var argument:content.getArgs())if(argument instanceof Text child&&hasKey(child,key))return true;
        }
        return text.getSiblings().stream().anyMatch(child->hasKey(child,key));
    }
    private static String branded(String value){
        for(String path:new String[]{"medic_clinic","medic_tent","medic","workshop"})value=value.replace("tetanusmod:"+path,"rustandruin:"+path);
        return value;
    }
}