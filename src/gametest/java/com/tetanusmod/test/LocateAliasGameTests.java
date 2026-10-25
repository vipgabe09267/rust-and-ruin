package com.tetanusmod.test;

import com.tetanusmod.command.LocateAliases;
import com.tetanusmod.expansion.ExpansionBlocks;
import com.tetanusmod.expansion.MedicineCabinetBlock;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.test.*;
import net.minecraft.text.*;
import net.minecraft.util.math.*;
import java.util.*;

/** Brand aliases keep native permission, POI indexing, feedback and saved identifiers. */
public class LocateAliasGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE,tickLimit=40)
    public void brandedPoiUsesTheSavedWorkstationAndPreservesCoordinateLinks(TestContext ctx){
        var pos=ctx.getAbsolutePos(new BlockPos(3,3,3));
        ctx.getWorld().setBlockState(pos.down(),Blocks.STONE.getDefaultState());
        var state=ExpansionBlocks.FIELD_MEDIC_CABINET.getDefaultState().with(MedicineCabinetBlock.COMPLETE,true);
        ctx.getWorld().setBlockState(pos,state,18);ctx.getWorld().setBlockState(pos.up(),state.with(DoorBlock.HALF,DoubleBlockHalf.UPPER),3);
        ctx.runAtTick(3,()->{
            var messages=new ArrayList<Text>();var source=source(ctx,Vec3d.ofCenter(pos),messages,true);
            execute(ctx,source,"locate poi tetanusmod:medic_tent");
            var legacy=success(ctx,messages,"tetanusmod:medic_tent");var nativeCoords=coordinates(legacy);messages.clear();
            execute(ctx,source,"locate poi rustandruin:medic_tent");
            var branded=success(ctx,messages,"rustandruin:medic_tent");
            ctx.assertTrue(!branded.getString().contains("tetanusmod:"),"Branded alias leaked the old name in displayed feedback");
            ctx.assertTrue(nativeCoords!=null&&nativeCoords.equals(coordinates(branded)),"Alias changed native teleport link, coordinates or styling");messages.clear();
            execute(ctx,source,"locate poi #rustandruin:medic_tent");
            ctx.assertTrue(messages.stream().anyMatch(text->key(text).equals("commands.locate.poi.success")),"Branded native POI tag failed");
            messages.clear();execute(ctx,source.withSilent(),"locate poi rustandruin:medic_tent");
            ctx.assertTrue(messages.isEmpty(),"Alias bypassed native silent output");
            execute(ctx,source(ctx,Vec3d.ofCenter(pos),messages,false),"locate poi rustandruin:medic_tent");
            ctx.assertTrue(messages.isEmpty(),"Alias bypassed command feedback policy");ctx.complete();
        });
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void aliasesAndShortcutRetainOperatorPermissionsAndRejectUnknownTargets(TestContext ctx){
        var messages=new ArrayList<Text>();var source=source(ctx,ctx.getAbsolute(new Vec3d(1,2,1)),messages,true).withLevel(0);
        var dispatcher=ctx.getWorld().getServer().getCommandManager().getDispatcher();
        for(String command:List.of("locate structure rustandruin:medic_tent","locate structure rustandruin:medic_clinic","locate structure rustandruin:workshop","locate poi rustandruin:medic_tent","locate poi rustandruin:medic","rustandruin locate tent")){
            boolean denied=false;try{dispatcher.execute(command,source);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
            ctx.assertTrue(denied,"Non-operator ran "+command);
        }
        boolean helperDenied=false;try{LocateAliases.execute(source,"structure","medic_tent");}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){helperDenied=true;}
        ctx.assertTrue(helperDenied,"Direct alias helper bypassed permission");
        for(String bad:List.of("medic_tent extra","../medic_tent","minecraft:village_plains")){
            boolean denied=false;try{LocateAliases.execute(source.withLevel(4),"structure",bad);}catch(IllegalArgumentException expected){denied=true;}catch(com.mojang.brigadier.exceptions.CommandSyntaxException failure){throw new IllegalStateException(failure);}
            ctx.assertTrue(denied,"Alias helper accepted an unlisted registry target: "+bad);
        }
        ctx.assertTrue(messages.isEmpty(),"Rejected alias executed a locate action");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void missingNearbyPoiExplainsTheNativeRangeAndBrandedStructureCommand(TestContext ctx){
        var messages=new ArrayList<Text>();var source=source(ctx,new Vec3d(900000,-40,900000),messages,true);
        execute(ctx,source,"locate poi rustandruin:medic_tent");
        ctx.assertTrue(messages.stream().anyMatch(text->containsKey(text,"commands.locate.poi.not_found")),"Missing POI did not retain native failure");
        ctx.assertTrue(messages.stream().anyMatch(text->containsKey(text,"commands.rustandruin.poi_hint")),"Missing POI failed to explain the128-block range");
        ctx.assertTrue(messages.stream().noneMatch(text->text.getString().contains("tetanusmod:")),"Missing-POI alias displayed the old brand");ctx.complete();
    }
    private static ServerCommandSource source(TestContext ctx,Vec3d position,List<Text> messages,boolean feedback){
        CommandOutput output=new CommandOutput(){
            @Override public void sendMessage(Text message){messages.add(message);}
            @Override public boolean shouldReceiveFeedback(){return feedback;}
            @Override public boolean shouldTrackOutput(){return true;}
            @Override public boolean shouldBroadcastConsoleToOps(){return false;}
        };
        return ctx.getWorld().getServer().getCommandSource().withWorld(ctx.getWorld()).withPosition(position).withLevel(4).withOutput(output);
    }
    private static void execute(TestContext ctx,ServerCommandSource source,String command){
        try{ctx.getWorld().getServer().getCommandManager().getDispatcher().execute(command,source);}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException failure){throw new IllegalStateException("Alias parse failed: "+command,failure);}
    }
    private static Text success(TestContext ctx,List<Text> messages,String id){
        var result=messages.stream().filter(text->key(text).equals("commands.locate.poi.success")&&text.getString().contains(id)).findFirst();
        ctx.assertTrue(result.isPresent(),"Missing native POI success for "+id+": "+messages);return result.orElseThrow();
    }
    private static String key(Text text){return text.getContent() instanceof TranslatableTextContent content?content.getKey():"";}
    private static boolean containsKey(Text text,String key){if(key(text).equals(key))return true;return text.getSiblings().stream().anyMatch(child->containsKey(child,key));}
    private static Text coordinates(Text text){
        if(text.getContent() instanceof TranslatableTextContent content){if(content.getKey().equals("chat.coordinates"))return text;for(var arg:content.getArgs())if(arg instanceof Text child){var found=coordinates(child);if(found!=null)return found;}}
        for(var child:text.getSiblings()){var found=coordinates(child);if(found!=null)return found;}return null;
    }
}