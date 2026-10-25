package com.tetanusmod.test;

import com.tetanusmod.expansion.GloveInventory;
import com.tetanusmod.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.world.GameMode;
import net.minecraft.world.LightType;
import net.minecraft.util.math.Vec3d;
import java.nio.file.*;
import java.util.*;

/** Development-only opaque-sleeve A/B probe with native Night Vision lighting; both mixins are inert otherwise. */
public final class GraphicalGloveAudit {
    public static boolean active;
    public static Identifier skin;
    public static String model="default";
    private record Scene(String width,String view,Arm arm,boolean gloves){}
    private final List<Scene> scenes=new ArrayList<>();
    private final List<String> metrics=new ArrayList<>();
    private static final BlockPos CENTER=new BlockPos(96,4,20);
    private int index,ticks,settled,lightingSettled;
    private volatile boolean serverWarm,serverRestored;
    private long serverPreparedAt;
    private boolean sceneReady,restoreQueued;
    private boolean oldNoGravity,oldFlying;
    private GameMode oldMode;
    private ItemStack oldGloves=ItemStack.EMPTY;
    private StatusEffectInstance oldNightVision;
    private boolean started,complete,prepared;
    private volatile boolean serverReady;
    private volatile Throwable failure;
    private Perspective oldPerspective;
    private Arm oldArm;
    private int oldFov;
    public void start(MinecraftClient client){
        if(started)throw new IllegalStateException("Glove probe started twice");started=true;
        oldPerspective=client.options.getPerspective();oldArm=client.options.getMainArm().getValue();oldFov=client.options.getFov().getValue();
        var image=new NativeImage(64,64,false);
        for(int y=0;y<64;y++)for(int x=0;x<64;x++)image.setColor(x,y,0xFFFF00FF);
        // Native image colors are ABGR. Both full sleeve atlases are opaque blue, including the hand end caps.
        for(int y=32;y<48;y++)for(int x=40;x<56;x++)image.setColor(x,y,0xFFFF0000);
        for(int y=48;y<64;y++)for(int x=48;x<64;x++)image.setColor(x,y,0xFFFF0000);
        try{var folder=client.runDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);image.writeTo(folder.resolve("glove-probe-test-skin.png"));}catch(Exception problem){image.close();throw new IllegalStateException(problem);}
        skin=client.getTextureManager().registerDynamicTexture("rustruin_glove_probe",new NativeImageBackedTexture(image));active=true;
        for(String width:new String[]{"default","slim"})for(String view:new String[]{"first-right","first-left","third-front","third-back"})for(boolean worn:new boolean[]{false,true})scenes.add(new Scene(width,view,view.equals("first-left")?Arm.LEFT:Arm.RIGHT,worn));
        metrics.add("scene,gloves,brownPixels,bluePixels,magentaPixels,skyLight,blockLight,nativeNightVision,playerY,width,height");
        client.setScreen(null);client.options.getFov().setValue(40);
        client.getServer().execute(()->{
            try{
                var player=client.getServer().getPlayerManager().getPlayerList().get(0);var world=player.getServerWorld();oldMode=player.interactionManager.getGameMode();oldNoGravity=player.hasNoGravity();oldFlying=player.getAbilities().flying;oldGloves=GloveInventory.equipped(player).copy();var originalVision=player.getStatusEffect(StatusEffects.NIGHT_VISION);oldNightVision=originalVision==null?null:new StatusEffectInstance(originalVision);player.closeHandledScreen();player.clearStatusEffects();player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,12000,0,false,false,false));player.changeGameMode(GameMode.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;player.sendAbilitiesUpdate();player.getInventory().clear();
                var center=CENTER;
                for(int x=center.getX()-16;x<=center.getX()+16;x+=16)for(int z=center.getZ()-16;z<=center.getZ()+16;z+=16)world.getChunk(new BlockPos(x,center.getY(),z));
                for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){world.setBlockState(center.add(x,-1,z),Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());for(int y=0;y<8;y++)world.setBlockState(center.add(x,y,z),Blocks.AIR.getDefaultState());}
                // Physical light sources provide a deterministic native lightmap even in void-world chunks with zero skylight.
                for(int dx:new int[]{-3,3})for(int dz:new int[]{-3,3})world.setBlockState(center.add(dx,-1,dz),Blocks.SEA_LANTERN.getDefaultState());
                for(int dy:new int[]{1,3})for(int dx:new int[]{-2,0,2})for(int dz:new int[]{-2,0,2})world.setBlockState(center.add(dx,dy,dz),Blocks.LIGHT.getDefaultState());
                world.setTimeOfDay(6000);world.setWeather(0,6000,false,false);player.teleport(world,96.5,4,20.5,180,15);player.setVelocity(Vec3d.ZERO);player.setHealth(player.getMaxHealth());serverPreparedAt=world.getTime();
                ((GloveInventory)player).rustruin$gloves().clear();
            }catch(Throwable problem){failure=problem;}finally{serverReady=true;}
        });
    }
    public boolean tick(MinecraftClient client){
        try{return tickInternal(client);}catch(RuntimeException|Error problem){restore(client);throw problem;}
    }
    private boolean tickInternal(MinecraftClient client){
        if(!started)throw new IllegalStateException("Glove probe not started");if(complete)return true;
        if(failure!=null){restore(client);throw new IllegalStateException("Glove probe server setup failed",failure);}
        if(++ticks>1800){restore(client);throw new IllegalStateException("Glove probe timed out at scene "+index);}
        if(!serverReady)return false;
        if(!serverWarm){
            serverReady=false;
            client.getServer().execute(()->{try{var world=client.getServer().getOverworld();serverWarm=world.getTime()-serverPreparedAt>=20;}catch(Throwable problem){failure=problem;}finally{serverReady=true;}});
            return false;
        }
        if(!sceneReady){
            boolean floor=client.world.getBlockState(CENTER.down()).isOf(Blocks.LIGHT_GRAY_CONCRETE);
            int light=client.world.getLightLevel(LightType.SKY,CENTER.up());int blockLight=client.world.getLightLevel(LightType.BLOCK,CENTER.up());
            boolean position=client.player.squaredDistanceTo(Vec3d.ofBottomCenter(CENTER))<0.04;boolean vision=client.player.hasStatusEffect(StatusEffects.NIGHT_VISION);
            if(!floor||!vision||!position){lightingSettled=0;if(ticks>400)throw new IllegalStateException("Opaque-sleeve fixture is not lit/positioned: floor="+floor+", sky="+light+", block="+blockLight+", nativeNightVision="+vision+", player="+client.player.getPos());return false;}
            if(++lightingSettled<20)return false;
            client.player.setYaw(180);client.player.setPitch(15);sceneReady=true;
            org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GLOVE_PROBE_LIT_FIXTURE floor={} sky={} block={} player={} nativeWarmTicks>=20 nativeNightVision=true",floor,light,blockLight,client.player.getPos());
        }
        if(index>=scenes.size()){
            try{Files.write(client.runDirectory.toPath().resolve("glove-probe-results.csv"),metrics,java.nio.charset.StandardCharsets.UTF_8);}catch(Exception problem){restore(client);throw new IllegalStateException(problem);}
            if(!restoreQueued){restore(client);return false;}if(!serverRestored)return false;complete=true;return true;
        }
        var scene=scenes.get(index);
        if(!prepared){
            model=scene.width();client.options.getMainArm().setValue(scene.arm());client.player.setMainArm(scene.arm());
            client.options.setPerspective(scene.view().equals("third-front")?Perspective.THIRD_PERSON_FRONT:scene.view().equals("third-back")?Perspective.THIRD_PERSON_BACK:Perspective.FIRST_PERSON);
            prepared=true;serverReady=false;settled=0;
            client.getServer().execute(()->{
                try{var player=client.getServer().getPlayerManager().getPlayerList().get(0);player.setMainArm(scene.arm());((GloveInventory)player).rustruin$gloves().setStack(0,scene.gloves()?new ItemStack(ModItems.PROTECTIVE_GLOVES):ItemStack.EMPTY);}
                catch(Throwable problem){failure=problem;}finally{serverReady=true;}
            });return false;
        }
        if(GloveInventory.equipped(client.player).isEmpty()==scene.gloves())return false;
        if(!client.player.getModel().equals(scene.width()))throw new IllegalStateException("Probe skin-width mixin was not applied");
        if(!client.player.isPartVisible(PlayerModelPart.LEFT_SLEEVE)||!client.player.isPartVisible(PlayerModelPart.RIGHT_SLEEVE))throw new IllegalStateException("Probe sleeve visibility mixin was not applied");
        if(++settled<15)return false;
        String name=(scene.width().equals("default")?"wide":"slim")+"-"+scene.view()+"-"+(scene.gloves()?"gloves":"baseline");
        try(var frame=ScreenshotRecorder.takeScreenshot(client.getFramebuffer())){
            long brown=0,blue=0,magenta=0;
            for(int y=0;y<frame.getHeight()-60;y++)for(int x=0;x<frame.getWidth();x++){
                int pixel=frame.getColor(x,y),r=pixel&255,g=(pixel>>>8)&255,b=(pixel>>>16)&255;
                if(r>g+15&&g>b+8&&r>b+30)brown++;
                if(b>80&&b>r+40&&b>g+40)blue++;
                if(r>80&&b>80&&r>g+40&&b>g+40)magenta++;
            }
            if(!scene.gloves()&&blue<20)throw new IllegalStateException("Opaque blue sleeves are not illuminated in baseline "+name+": bluePixels="+blue);
            if(!scene.gloves()&&scene.view().startsWith("third-")&&magenta<20)throw new IllegalStateException("Opaque magenta body is not illuminated in baseline "+name+": magentaPixels="+magenta);
            frame.writeTo(client.runDirectory.toPath().resolve("screenshots").resolve("rust-and-ruin-glove-probe-"+name+".png"));
            metrics.add(name+","+scene.gloves()+","+brown+","+blue+","+magenta+","+client.world.getLightLevel(LightType.SKY,CENTER.up())+","+client.world.getLightLevel(LightType.BLOCK,CENTER.up())+","+client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)+","+client.player.getY()+","+frame.getWidth()+","+frame.getHeight());
            org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GLOVE_PROBE_FRAME {} brownPixels={}",name,brown);
        }catch(Exception problem){restore(client);throw new IllegalStateException("Cannot capture native glove probe",problem);}
        index++;prepared=false;return false;
    }
    private void restore(MinecraftClient client){
        active=false;model="default";client.options.setPerspective(oldPerspective);client.options.getMainArm().setValue(oldArm);if(client.player!=null)client.player.setMainArm(oldArm);client.options.getFov().setValue(oldFov);
        if(!restoreQueued){restoreQueued=true;if(client.getServer()!=null){Arm restoreArm=oldArm;client.getServer().execute(()->{try{if(!client.getServer().getPlayerManager().getPlayerList().isEmpty()){var player=client.getServer().getPlayerManager().getPlayerList().get(0);player.setMainArm(restoreArm);if(oldMode!=null)player.changeGameMode(oldMode);player.setNoGravity(oldNoGravity);player.getAbilities().flying=oldFlying;player.sendAbilitiesUpdate();((GloveInventory)player).rustruin$gloves().setStack(0,oldGloves);player.removeStatusEffect(StatusEffects.NIGHT_VISION);if(oldNightVision!=null)player.addStatusEffect(oldNightVision);}}catch(Throwable problem){failure=problem;}finally{serverRestored=true;}});}else serverRestored=true;}
        if(skin!=null){client.getTextureManager().destroyTexture(skin);skin=null;}
    }
}
