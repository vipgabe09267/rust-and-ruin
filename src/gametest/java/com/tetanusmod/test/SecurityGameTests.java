package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.config.ModConfig;
import com.tetanusmod.network.ModPacketDecoder;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.test.*;
import net.minecraft.util.math.Vec3d;

public class SecurityGameTests implements FabricGameTest {
    private void rejected(TestContext ctx,Runnable action){boolean failed=false;try{action.run();}catch(RuntimeException expected){failed=true;}ctx.assertTrue(failed,"Malformed input was accepted");}
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void presenceAndSymptomPacketBoundaries(TestContext ctx) {
        var uuid=java.util.UUID.randomUUID();var presence=PacketByteBufs.create();presence.writeUuid(uuid);presence.writeBoolean(true);
        var parsed=ModPacketDecoder.presence(presence);ctx.assertTrue(parsed.player().equals(uuid)&&parsed.infected(),"Valid presence rejected");presence.release();
        for(int length=0;length<65;length++)if(length!=17){var bytes=PacketByteBufs.create();bytes.writeZero(length);rejected(ctx,()->ModPacketDecoder.presence(bytes));bytes.release();}
        var invalid=PacketByteBufs.create();invalid.writeUuid(uuid);invalid.writeByte(2);rejected(ctx,()->ModPacketDecoder.presence(invalid));invalid.release();
        for(int stage=0;stage<=2;stage++){var bytes=PacketByteBufs.create();bytes.writeByte(stage);bytes.writeBoolean(true);ctx.assertTrue(ModPacketDecoder.symptom(bytes).stage()==stage,"Valid symptom rejected");bytes.release();}
        var bad=PacketByteBufs.create();bad.writeByte(3);bad.writeByte(0);rejected(ctx,()->ModPacketDecoder.symptom(bad));bad.release();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void boundedConfigParsingAndValidation(TestContext ctx) {
        var valid=PacketByteBufs.create();valid.writeString(new com.google.gson.Gson().toJson(new ModConfig()));var config=ModPacketDecoder.config(valid);valid.release();
        ctx.assertTrue(config.vaccineDurationSeconds==1200&&config.minDurationSeconds==300,"Valid synchronized config rejected");
        var clamped=PacketByteBufs.create();clamped.writeString("{\"vaccineDurationSeconds\":999999,\"vaccineProtectionChance\":2,\"gloveProtectionChance\":-1}");var limits=ModPacketDecoder.config(clamped);clamped.release();
        ctx.assertTrue(limits.vaccineDurationSeconds==7200&&limits.vaccineProtectionChance==1&&limits.gloveProtectionChance==0,"Network config escaped bounds");
        for(String input:new String[]{"null","[]","true","{","{\"nested\":{}}","[".repeat(3000)+"]".repeat(3000)}){var bytes=PacketByteBufs.create();bytes.writeString(input);rejected(ctx,()->ModPacketDecoder.config(bytes));bytes.release();}
        var huge=PacketByteBufs.create();huge.writeZero(8193);rejected(ctx,()->ModPacketDecoder.config(huge));huge.release();
        var trailing=PacketByteBufs.create();trailing.writeString("{}");trailing.writeByte(0);rejected(ctx,()->ModPacketDecoder.config(trailing));trailing.release();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void randomMalformedPacketsCannotEscapeWithFatalErrors(TestContext ctx) {
        var random=new java.util.Random(42);
        for(int i=0;i<2000;i++) {
            byte[] data=new byte[random.nextInt(96)];random.nextBytes(data);
            var bytes=PacketByteBufs.create();bytes.writeBytes(data);
            try{ModPacketDecoder.config(bytes);}catch(RuntimeException expected){}finally{bytes.release();}
        }
        ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void configurationStringsCannotInjectStructure(TestContext ctx) {
        var bytes=PacketByteBufs.create();bytes.writeString("{\"ignored\":\"escaped \\\" { } [ ]\",\"vaccineProtectionChance\":0.8}");
        var config=ModPacketDecoder.config(bytes);bytes.release();ctx.assertTrue(config.vaccineProtectionChance==0.8F,"Quoted braces confused config validation");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void commandHelpersRejectNonOperatorsAndNonFinitePositions(TestContext ctx) {
        var server=ctx.getWorld().getServer();var source=server.getCommandSource().withWorld(ctx.getWorld());
        boolean denied=false;try{com.tetanusmod.command.RustRuinCommands.summon(source.withLevel(0),"minecraft:zombie",ctx.getAbsolute(new Vec3d(3,3,3)));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
        ctx.assertTrue(denied,"Non-operator bypassed command helper permission");
        for(double coordinate:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
            boolean invalid=false;try{com.tetanusmod.command.RustRuinCommands.summon(source,"minecraft:zombie",new Vec3d(coordinate,3,3));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){invalid=true;}ctx.assertTrue(invalid,"Non-finite summon coordinate accepted");
        }
        boolean hidden=false;try{server.getCommandManager().getDispatcher().execute("rustandruin tetanize @e",source.withLevel(0));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){hidden=true;}ctx.assertTrue(hidden,"Command tree accepted non-operator");ctx.complete();
    }
}