package com.tetanusmod.test;

import com.google.gson.GsonBuilder;
import com.tetanusmod.TetanusMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.*;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.advancement.*;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/** Actual native screen/drag/hover proof, following the real all-advancement journey. */
public final class GraphicalAdvancementLayoutAudit {
    private static GraphicalAdvancementLayoutAudit activeCapture;
    private boolean captureRequested;private int captureWaitFrames;private long captureRequestFrame;
    private static final List<String> VANILLA_ROOTS=List.of("story/root","adventure/root","husbandry/root","nether/root","end/root");
    private static final int[] SCALES={2,3,4,6};
    private static final Vec3d LAB=new Vec3d(256.5,4,128.5);
    private final AtomicReference<Throwable> failure=new AtomicReference<>();
    private final AtomicBoolean pending=new AtomicBoolean();
    private final List<Map<String,Object>> images=new ArrayList<>();
    private final List<Map<String,Object>> scaleEvidence=new ArrayList<>();
    private volatile Scene scene;
    private String viewer;private int oldScale,phase,ticks,scaleIndex,nodeIndex,viewTicks;private boolean started,finished,useStarted,attacked,restoring;
    private AdvancementsScreen screen;private List<Advancement> nodes=List.of();private String currentView;private boolean captured;
    private int oldWidth,oldHeight,oldX,oldY,oldWindowedWidth,oldWindowedHeight,oldWindowedX,oldWindowedY,resizeWait,stableViewport;
    private boolean oldFullscreen,oldFullscreenOption,oldUnicode,viewportReady,windowRestoreRequested;private double oldMouseX,oldMouseY;
    private final Map<String,Object> windowEvidence=new LinkedHashMap<>();
    private net.minecraft.client.util.InputUtil.Key oldAdvancementKey;private final Map<String,Object> keyboardEvidence=new LinkedHashMap<>();
    private AdvancementTooltipClientProbe.State oldTooltipProbe;private int hoverX,hoverY,restoreStable;private long tooltipBaseline;

    public void start(MinecraftClient client) {
        if(started||client.player==null||client.getServer()==null)throw new IllegalStateException("Layout proof requires one actual integrated player");
        if(activeCapture!=null)throw new IllegalStateException("A graphical layout capture is already active");activeCapture=this;
        started=true;viewer=FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
        oldScale=client.options.getGuiScale().getValue();oldAdvancementKey=net.minecraft.client.util.InputUtil.fromTranslationKey(client.options.advancementsKey.getBoundKeyTranslationKey());keyboardEvidence.put("OriginalAdvancementsKey",oldAdvancementKey.getTranslationKey());client.setScreen(null);
        oldTooltipProbe=AdvancementTooltipClientProbe.snapshot();AdvancementTooltipClientProbe.enabled=true;
        snapshotAndEnlarge(client);
        var uuid=client.player.getUuid();
        submit(client,()->{scene=new Scene(client.getServer().getPlayerManager().getPlayer(uuid));scene.prepare();});
    }

    public boolean tick(MinecraftClient client) {
        try{return tickInternal(client);}
        catch(RuntimeException|Error problem){activeCapture=null;captureRequested=false;client.options.useKey.setPressed(false);client.setScreen(null);requestWindowRestore(client);AdvancementTooltipClientProbe.restore(oldTooltipProbe);if(scene!=null&&client.getServer()!=null)client.getServer().execute(scene::restore);throw problem;}
    }
    private boolean tickInternal(MinecraftClient client) {
        if(!started)throw new IllegalStateException("Layout proof was not started");
        if(failure.get()!=null)throw new IllegalStateException("Native layout fixture failed",failure.get());
        if(finished)return true;
        if(client.player==null||client.world==null||scene==null)return false;
        if(++ticks>5000)throw new IllegalStateException("Layout proof timeout: phase="+phase+", scale="+scaleIndex+", view="+currentView+", server="+scene.diagnostic);
        if(!viewportReady){if(!awaitLargeViewport(client))return false;}
        if(phase<7&&pending.compareAndSet(false,true))client.getServer().execute(()->{try{scene.observe();}catch(Throwable problem){failure.set(problem);}finally{pending.set(false);}});
        switch(phase) {
            case 0 -> {
                if(!scene.prepared||!scene.tablePickedUp||clientRoot(client,"story/root")==null)return false;
                if(!client.player.getMainHandStack().isOf(Items.APPLE))return false;
                client.player.setYaw(180);client.player.setPitch(-30);client.options.useKey.setPressed(true);
                var result=client.interactionManager.interactItem(client.player,Hand.MAIN_HAND);
                if(!result.isAccepted()||!client.player.isUsingItem())throw new IllegalStateException("Native client did not start the apple");
                useStarted=true;phase=1;
            }
            case 1 -> {
                client.options.useKey.setPressed(useStarted&&!scene.appleConsumed&&client.player.getMainHandStack().isOf(Items.APPLE));
                if(!scene.appleConsumed||clientRoot(client,"husbandry/root")==null)return false;
                client.options.useKey.setPressed(false);
                submit(client,scene::prepareCombat);phase=2;
            }
            case 2 -> {
                if(scene.cowKilled&&clientRoot(client,"adventure/root")!=null){send(client,"execute in minecraft:the_nether run tp @s 0 90 0");phase=3;return false;}
                if(attacked||!scene.combatReady||!client.player.getMainHandStack().isOf(Items.IRON_SWORD))return false;
                var target=client.world.getEntityById(scene.cowId);
                if(!(target instanceof CowEntity))return false;
                if(!attacked){client.interactionManager.attackEntity(client.player,target);client.player.swingHand(Hand.MAIN_HAND);attacked=true;}
                if(!scene.cowKilled||clientRoot(client,"adventure/root")==null)return false;
                send(client,"execute in minecraft:the_nether run tp @s 0 90 0");phase=3;
            }
            case 3 -> {
                if(!client.world.getRegistryKey().equals(World.NETHER)||!scene.netherEarned||clientRoot(client,"nether/root")==null)return false;
                send(client,"execute in minecraft:the_end run tp @s 0 90 0");phase=4;
            }
            case 4 -> {
                if(!client.world.getRegistryKey().equals(World.END)||!scene.endEarned||clientRoot(client,"end/root")==null)return false;
                send(client,"execute in "+scene.world.getRegistryKey().getValue()+" run tp @s "+LAB.x+" "+LAB.y+" "+LAB.z);phase=5;
            }
            case 5 -> {
                if(!client.world.getRegistryKey().equals(scene.world.getRegistryKey())||client.player.squaredDistanceTo(LAB)>1)return false;
                for(String root:VANILLA_ROOTS)if(clientRoot(client,root)==null)return false;
                var manager=client.player.networkHandler.getAdvancementHandler().getManager();
                nodes=new ArrayList<>();for(var advancement:manager.getAdvancements())if(advancement.getId().getNamespace().equals(TetanusMod.MOD_ID)&&advancement.getDisplay()!=null)nodes.add(advancement);
                nodes.sort(Comparator.comparing(a->a.getId().toString()));
                if(nodes.size()!=27)throw new IllegalStateException("Actual synchronized mod tree lacks nodes: "+nodes.size());
                beginScale(client);phase=6;
            }
            case 6 -> {if(runViews(client)){scaleIndex++;if(scaleIndex<SCALES.length)beginScale(client);else{client.setScreen(null);requestWindowRestore(client);submit(client,scene::verifyAndRestore);restoring=true;phase=7;}}}
            case 7 -> {
                if(!restoring||!scene.restored||!windowRestored(client))return false;
                var report=new LinkedHashMap<String,Object>();report.put("Result","PASS");report.put("ActualConnectedPlayer",true);report.put("DirectCriterionGrants",0);report.put("ProgressRevocations",0);
                report.put("ExistingCriteriaDatesPreserved",scene.preservedCriteria);report.put("VisibleModNodes",nodes.size());report.put("VanillaRootActions",Map.of("story/root","Automatic real crafting-table item pickup","adventure/root","Real client sword attack killed the fixture cow","husbandry/root","Real client held-use apple consumption","nether/root","Native network dimension teleport to the Nether","end/root","Native network dimension teleport to the End"));
                report.put("NativeActionEvidence",Map.of("CraftingTablePickedUp",scene.tablePickedUp,"AppleUsedStatDelta",scene.appleUsedDelta,"CowKilled",scene.cowKilled,"NetherRootComplete",scene.netherEarned,"EndRootComplete",scene.endEarned));
                report.put("Scales",scaleEvidence);report.put("Window",windowEvidence);report.put("Keyboard",keyboardEvidence);report.put("Screenshots",images);report.put("RetiredHidden","variant_egg remains hidden; recipe unlock remains a functional hidden helper.");
                try{Files.writeString(Path.of("graphical-advancement-layout-"+viewer+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}catch(java.io.IOException problem){throw new IllegalStateException(problem);}
                AdvancementTooltipClientProbe.restore(oldTooltipProbe);activeCapture=null;
                org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_ADVANCEMENT_LAYOUT_PASS nodes={} scales={} preserved={} captures={}",nodes.size(),scaleEvidence,scene.preservedCriteria,images.size());finished=true;return true;
            }
            default -> throw new IllegalStateException("Unknown layout proof phase "+phase);
        }
        return false;
    }

    private void snapshotAndEnlarge(MinecraftClient client) {
        var window=client.getWindow();oldWidth=window.getWidth();oldHeight=window.getHeight();oldX=window.getX();oldY=window.getY();
        oldWindowedWidth=((Number)field(window,"windowedWidth")).intValue();oldWindowedHeight=((Number)field(window,"windowedHeight")).intValue();oldWindowedX=((Number)field(window,"windowedX")).intValue();oldWindowedY=((Number)field(window,"windowedY")).intValue();
        oldFullscreen=window.isFullscreen();oldFullscreenOption=client.options.getFullscreen().getValue();oldUnicode=client.options.getForceUnicodeFont().getValue();oldMouseX=client.mouse.getX();oldMouseY=client.mouse.getY();
        windowEvidence.put("OriginalWindowSize",List.of(oldWidth,oldHeight));windowEvidence.put("OriginalWindowPosition",List.of(oldX,oldY));windowEvidence.put("OriginalFullscreen",oldFullscreen);windowEvidence.put("RequestedNativeWindowSize",List.of(1920,1440));
        client.options.getFullscreen().setValue(false);client.options.getForceUnicodeFont().setValue(false);window.setWindowedSize(1920,1440);GLFW.glfwSetWindowPos(window.getHandle(),0,0);
    }
    private boolean awaitLargeViewport(MinecraftClient client) {
        var window=client.getWindow();
        boolean actual=window.getWidth()==1920&&window.getHeight()==1440&&window.getFramebufferWidth()>=1920&&window.getFramebufferHeight()>=1440&&client.getFramebuffer().textureWidth==window.getFramebufferWidth()&&client.getFramebuffer().textureHeight==window.getFramebufferHeight();
        stableViewport=actual?stableViewport+1:0;
        if(++resizeWait>240)throw new IllegalStateException("Native framebuffer resize did not settle: window="+window.getWidth()+"x"+window.getHeight()+", framebuffer="+window.getFramebufferWidth()+"x"+window.getFramebufferHeight()+", renderTarget="+client.getFramebuffer().textureWidth+"x"+client.getFramebuffer().textureHeight);
        if(stableViewport<10)return false;
        client.onResolutionChanged();
        if(window.calculateScaleFactor(6,false)!=6)throw new IllegalStateException("Actual native framebuffer cannot support GUI6 after resize");
        windowEvidence.put("ObservedNativeWindowSize",List.of(window.getWidth(),window.getHeight()));windowEvidence.put("ObservedFramebufferSize",List.of(window.getFramebufferWidth(),window.getFramebufferHeight()));windowEvidence.put("StableNativeResizeTicks",stableViewport);viewportReady=true;return true;
    }
    private void requestWindowRestore(MinecraftClient client) {
        if(windowRestoreRequested)return;windowRestoreRequested=true;
        client.setScreen(null);client.options.advancementsKey.setBoundKey(oldAdvancementKey);net.minecraft.client.option.KeyBinding.updateKeysByCode();client.options.getGuiScale().setValue(oldScale);client.options.getForceUnicodeFont().setValue(oldUnicode);
        var window=client.getWindow();window.setWindowedSize(oldFullscreen?oldWindowedWidth:oldWidth,oldFullscreen?oldWindowedHeight:oldHeight);
        GLFW.glfwSetWindowPos(window.getHandle(),oldFullscreen?oldWindowedX:oldX,oldFullscreen?oldWindowedY:oldY);
        client.options.getFullscreen().setValue(oldFullscreenOption);if(window.isFullscreen()!=oldFullscreen)window.toggleFullscreen();
        client.onResolutionChanged();client.mouse.setResolutionChanged();pointNativeMouseRaw(client,oldMouseX,oldMouseY);
    }
    private boolean windowRestored(MinecraftClient client) {
        var window=client.getWindow();
        if(window.getWidth()!=oldWidth||window.getHeight()!=oldHeight||window.isFullscreen()!=oldFullscreen){restoreStable=0;return false;}
        if(!oldFullscreen&&(window.getX()!=oldX||window.getY()!=oldY)){restoreStable=0;return false;}
        if(client.options.getGuiScale().getValue()!=oldScale||client.options.getForceUnicodeFont().getValue()!=oldUnicode||client.options.getFullscreen().getValue()!=oldFullscreenOption)throw new IllegalStateException("Native window options were not restored");
        if(client.getFramebuffer().textureWidth!=window.getFramebufferWidth()||client.getFramebuffer().textureHeight!=window.getFramebufferHeight())return false;
        if(!client.options.advancementsKey.getBoundKeyTranslationKey().equals(oldAdvancementKey.getTranslationKey()))throw new IllegalStateException("Original Advancements binding was not restored");
        keyboardEvidence.put("RestoredAdvancementsKey",client.options.advancementsKey.getBoundKeyTranslationKey());
        if(++restoreStable<10)return false;
        client.mouse.setResolutionChanged();pointNativeMouseRaw(client,oldMouseX,oldMouseY);
        windowEvidence.put("OriginalNativeCursor",List.of(oldMouseX,oldMouseY));windowEvidence.put("RestoredNativeCursor",List.of(client.mouse.getX(),client.mouse.getY()));
        windowEvidence.put("RestoredNativeWindowSize",List.of(window.getWidth(),window.getHeight()));windowEvidence.put("RestoredWindowPosition",List.of(window.getX(),window.getY()));windowEvidence.put("RestoredFullscreen",window.isFullscreen());windowEvidence.put("RestoredGuiScaleOption",client.options.getGuiScale().getValue());
        return true;
    }

    private void beginScale(MinecraftClient client) {
        client.setScreen(null);client.options.getGuiScale().setValue(SCALES[scaleIndex]);client.onResolutionChanged();
        if(client.getWindow().getScaleFactor()!=SCALES[scaleIndex])throw new IllegalStateException("Native GUI scale capped unexpectedly after real 1920x1440 resize: requested="+SCALES[scaleIndex]+", applied="+client.getWindow().getScaleFactor());
        screen=new AdvancementsScreen(client.player.networkHandler.getAdvancementHandler());client.setScreen(screen);
        if(scaleIndex==0)verifyHomeRebinding(client);
        currentView=null;viewTicks=0;nodeIndex=-8;captured=false;
        var evidence=new LinkedHashMap<String,Object>();evidence.put("RequestedGuiScale",SCALES[scaleIndex]);evidence.put("NativeAppliedScale",client.getWindow().getScaleFactor());evidence.put("NativeCapRespected",true);evidence.put("ScaledSize",List.of(screen.width,screen.height));
        var tab=tabs(screen).get(client.player.networkHandler.getAdvancementHandler().getManager().get(TetanusMod.id("root")));
        if(tab==null||tabs(screen).size()<6)throw new IllegalStateException("Native UI did not expose all six legitimately unlocked tabs: "+tabs(screen).keySet());
        evidence.put("NativeTabs",tabs(screen).keySet().stream().map(a->a.getId().toString()).sorted().toList());scaleEvidence.add(evidence);
    }

    private boolean runViews(MinecraftClient client) {
        if(client.currentScreen!=screen)throw new IllegalStateException("Actual advancement screen unexpectedly closed during UI proof");
        if(currentView==null) {
            if(nodeIndex==-8){clickTab(screen,TetanusMod.id("root"));currentView="overview";}
            else if(nodeIndex==-7){
                var tab=selected(screen);double before=number(tab,"originY");
                int x=(screen.width-252)/2+120,y=(screen.height-140)/2+70;
                if(!screen.mouseScrolled(x,y,-30))throw new IllegalStateException("Native mod canvas did not consume wheel navigation");
                if(Math.abs(number(tab,"originY")-before)<1)throw new IllegalStateException("Wheel did not actually pan the tall mod tree");
                currentView="scrolled";
            } else if(nodeIndex==-6){
                if(!screen.keyPressed(GLFW.GLFW_KEY_HOME,0,0))throw new IllegalStateException("Home did not recenter the mod tree");
                var root=screen.getAdvancementWidget(client.player.networkHandler.getAdvancementHandler().getManager().get(TetanusMod.id("root")));
                assertVisible(selected(screen),root,"root after Home");keyboardEvidence.put("NativeHomeRecenteredAtScale"+SCALES[scaleIndex],true);currentView="home";
            } else if(nodeIndex<0){
                String id=VANILLA_ROOTS.get(nodeIndex+5);clickTab(screen,new Identifier("minecraft",id));
                var tab=selected(screen);double x=number(tab,"originX"),y=number(tab,"originY");
                screen.mouseScrolled((screen.width-252)/2+120,(screen.height-140)/2+70,-2);
                if(number(tab,"originX")!=x||number(tab,"originY")!=y)throw new IllegalStateException("Mod navigation changed native vanilla wheel behavior: "+id);
                currentView="vanilla-"+id.replace('/','-');
            } else {
                if(nodeIndex>=nodes.size())return true;
                clickTab(screen,TetanusMod.id("root"));
                var advancement=nodes.get(nodeIndex);var widget=screen.getAdvancementWidget(advancement);
                if(widget==null)throw new IllegalStateException("Actual screen omitted advancement widget "+advancement.getId());
                int displayedTitleWidth=client.textRenderer.getWidth(advancement.getDisplay().getTitle()),nativeTitleWidth=client.textRenderer.getWidth((net.minecraft.text.OrderedText)field(widget,"title"));
                if(nativeTitleWidth<displayedTitleWidth)throw new IllegalStateException("Native advancement title was truncated: "+advancement.getId());
                var progress=(AdvancementProgress)field(widget,"progress");
                if(progress==null||!progress.isDone())throw new IllegalStateException("Actual native gold frame is not complete: "+advancement.getId());
                var tab=selected(screen);double dx=117.0-widget.getX()-13-number(tab,"originX"),dy=56.0-widget.getY()-13-number(tab,"originY");
                int cx=(screen.width-252)/2+120,cy=(screen.height-140)/2+70;
                screen.mouseDragged(cx,cy,0,0,0);screen.mouseDragged(cx,cy,0,dx,dy);screen.mouseReleased(cx,cy,0);
                assertVisible(tab,widget,advancement.getId().toString());
                int hx=(screen.width-252)/2+9+(int)Math.floor(number(tab,"originX"))+widget.getX()+13;
                int hy=(screen.height-140)/2+18+(int)Math.floor(number(tab,"originY"))+widget.getY()+13;
                if(!widget.shouldRender((int)Math.floor(number(tab,"originX")),(int)Math.floor(number(tab,"originY")),hx-((screen.width-252)/2+9),hy-((screen.height-140)/2+18)))throw new IllegalStateException("Native tooltip hit test rejected the panned node: "+advancement.getId());
                hoverX=hx;hoverY=hy;tooltipBaseline=AdvancementTooltipClientProbe.frames;pointNativeMouse(client,hx,hy);
                currentView=advancement.getId().getPath();
            }
            viewTicks=0;captured=false;return false;
        }
        if(nodeIndex>=0&&!observeCurrentNodeHover(client))return false;
        viewTicks++;
        if(viewTicks>=14&&!captured&&!captureRequested){captureRequested=true;captureWaitFrames=0;captureRequestFrame=AdvancementTooltipClientProbe.frameNumber;}
        if(viewTicks>=20&&captured){currentView=null;nodeIndex++;}
        return false;
    }

    private void verifyHomeRebinding(MinecraftClient client) {
        clickTab(screen,TetanusMod.id("root"));
        var binding=client.options.advancementsKey;
        var prior=net.minecraft.client.util.InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey());
        try {
            binding.setBoundKey(net.minecraft.client.util.InputUtil.fromKeyCode(GLFW.GLFW_KEY_HOME,0));net.minecraft.client.option.KeyBinding.updateKeysByCode();
            client.keyboard.onKey(client.getWindow().getHandle(),GLFW.GLFW_KEY_HOME,0,GLFW.GLFW_PRESS,0);
            client.keyboard.onKey(client.getWindow().getHandle(),GLFW.GLFW_KEY_HOME,0,GLFW.GLFW_RELEASE,0);
            if(client.currentScreen!=null)throw new IllegalStateException("Home rebound as the Advancements key did not close its native screen");
            keyboardEvidence.put("NativeReboundHomeClosesScreen",true);
        } finally {binding.setBoundKey(prior);net.minecraft.client.option.KeyBinding.updateKeysByCode();}
        // If the original user binding is Home, exercise the optional recenter
        // shortcut with a temporary L binding, then restore the exact user key.
        if(binding.matchesKey(GLFW.GLFW_KEY_HOME,0)){binding.setBoundKey(net.minecraft.client.util.InputUtil.fromKeyCode(GLFW.GLFW_KEY_L,0));net.minecraft.client.option.KeyBinding.updateKeysByCode();keyboardEvidence.put("TemporaryNavigationBinding","key.keyboard.l");}
        screen=new AdvancementsScreen(client.player.networkHandler.getAdvancementHandler());client.setScreen(screen);
    }

    private boolean observeCurrentNodeHover(MinecraftClient client) {
        var id=nodes.get(nodeIndex).getId();
        if(selected(screen)==null||!selected(screen).getRoot().getId().equals(TetanusMod.id("root")))clickTab(screen,TetanusMod.id("root"));
        var tab=selected(screen);
        if(!(Boolean)field(tab,"initialized"))return false;
        var widget=screen.getAdvancementWidget(nodes.get(nodeIndex));
        if(widget==null)throw new IllegalStateException("Native widget disappeared while hovering "+id);
        double ox=number(tab,"originX"),oy=number(tab,"originY");
        double wx=ox+widget.getX(),wy=oy+widget.getY();
        if(wx<0||wy<0||wx+26>234||wy+26>113){
            int cx=(screen.width-252)/2+120,cy=(screen.height-140)/2+70;
            screen.mouseDragged(cx,cy,0,0,0);screen.mouseDragged(cx,cy,0,117.0-widget.getX()-13-ox,56.0-widget.getY()-13-oy);screen.mouseReleased(cx,cy,0);
        }
        assertVisible(tab,widget,id.toString());
        hoverX=(screen.width-252)/2+9+(int)Math.floor(number(tab,"originX"))+widget.getX()+13;
        hoverY=(screen.height-140)/2+18+(int)Math.floor(number(tab,"originY"))+widget.getY()+13;
        pointNativeMouse(client,hoverX,hoverY);return true;
    }

    /** Invoked only after native GameRenderer finished the current graphical frame. */
    public static void afterNativeFrame(MinecraftClient client) {
        var controller=activeCapture;
        if(controller==null||!controller.captureRequested)return;
        try{controller.captureRenderedFrame(client);}catch(Throwable problem){controller.captureRequested=false;controller.failure.set(problem);}
    }
    private void captureRenderedFrame(MinecraftClient client) {
        if(client.currentScreen!=screen||phase!=6)throw new IllegalStateException("Expected native advancement screen changed before framebuffer capture");
        if(AdvancementTooltipClientProbe.frameNumber<=captureRequestFrame)return;
        if(++captureWaitFrames>240)throw new IllegalStateException("Expected tooltip never appeared in an actual rendered frame: view="+currentView+", frameId="+AdvancementTooltipClientProbe.frameAdvancement+", cursor="+client.mouse.getX()+","+client.mouse.getY());
        if(nodeIndex>=0){
            var expected=nodes.get(nodeIndex).getId();
            if(!expected.equals(AdvancementTooltipClientProbe.frameAdvancement)||!expected.equals(AdvancementTooltipClientProbe.frameBoundsAdvancement))return;
        }
            if(nodeIndex>=0){
                var expected=nodes.get(nodeIndex).getId();
                if(!expected.equals(AdvancementTooltipClientProbe.lastAdvancement)||AdvancementTooltipClientProbe.frames<=tooltipBaseline)throw new IllegalStateException("Native tooltip was not actually rendered for "+expected+"; last="+AdvancementTooltipClientProbe.lastAdvancement+", frames="+AdvancementTooltipClientProbe.frames+", cursor="+client.mouse.getX()+","+client.mouse.getY());
            }
            if(nodeIndex>=0){
                var expected=nodes.get(nodeIndex).getId();var bounds=AdvancementTooltipClientProbe.lastBounds;
                if(!expected.equals(AdvancementTooltipClientProbe.lastBoundsAdvancement)||bounds==null)throw new IllegalStateException("Native tooltip background was never observed for "+expected);
                if(bounds.left()<0||bounds.top()<0||bounds.right()>screen.width||bounds.bottom()>screen.height)throw new IllegalStateException("Native tooltip clipped outside the actual scaled viewport: "+expected+" bounds="+bounds+" viewport="+screen.width+"x"+screen.height);
            }
            String name="rust-and-ruin-"+viewer+"-layout-scale"+SCALES[scaleIndex]+"-"+currentView+".png";
            ScreenshotRecorder.saveScreenshot(client.runDirectory,name,client.getFramebuffer(),text->{});
            var receipt=new LinkedHashMap<String,Object>();receipt.put("RequestedGuiScale",SCALES[scaleIndex]);receipt.put("AppliedGuiScale",client.getWindow().getScaleFactor());receipt.put("View",currentView);receipt.put("Screenshot",name);receipt.put("CapturePhase","GameRenderer.render:TAIL");receipt.put("CaptureFrame",AdvancementTooltipClientProbe.frameNumber);
            if(nodeIndex>=0){receipt.put("NativeTooltipRenderedFrames",AdvancementTooltipClientProbe.frames-tooltipBaseline);receipt.put("NativeTooltipFrame",AdvancementTooltipClientProbe.frameNumber);receipt.put("NativeTooltipAdvancementId",AdvancementTooltipClientProbe.lastAdvancement.toString());receipt.put("NativeTooltipBounds",AdvancementTooltipClientProbe.lastBounds);receipt.put("AcknowledgedNativeCursorScaled",List.of(client.mouse.getX()*screen.width/client.getWindow().getWidth(),client.mouse.getY()*screen.height/client.getWindow().getHeight()));}
            images.add(receipt);captured=true;
            org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_ADVANCEMENT_LAYOUT_VIEW {} scale={} applied={}",currentView,SCALES[scaleIndex],client.getWindow().getScaleFactor());

        captureRequested=false;
    }

    private void pointNativeMouse(MinecraftClient client,int scaledX,int scaledY) {
        double rawX=scaledX*(double)client.getWindow().getWidth()/screen.width,rawY=scaledY*(double)client.getWindow().getHeight()/screen.height;
        pointNativeMouseRaw(client,rawX,rawY);
    }
    private static void pointNativeMouseRaw(MinecraftClient client,double rawX,double rawY) {
        GLFW.glfwSetCursorPos(client.getWindow().getHandle(),rawX,rawY);
        // Feed the same native input callback after moving the actual cursor.
        // Remote/window focus can suppress GLFW's queued callback; acknowledge
        // native Mouse state rather than substituting render coordinates.
        try {
            var move=net.minecraft.client.Mouse.class.getDeclaredMethod("onCursorPos",long.class,double.class,double.class);move.setAccessible(true);move.invoke(client.mouse,client.getWindow().getHandle(),rawX,rawY);
        }catch(ReflectiveOperationException problem){throw new IllegalStateException("Native mouse callback could not receive the UI input",problem);}
        if(Math.abs(client.mouse.getX()-rawX)>.01||Math.abs(client.mouse.getY()-rawY)>.01)throw new IllegalStateException("Native mouse did not acknowledge the requested cursor position");
    }

    private static void assertVisible(AdvancementTab tab,AdvancementWidget widget,String id) {
        if(widget==null)throw new IllegalStateException("Missing native widget "+id);
        double x=number(tab,"originX")+widget.getX(),y=number(tab,"originY")+widget.getY();
        if(x<-.01||y<-.01||x+26>234.01||y+26>113.01)throw new IllegalStateException("Panning could not expose the complete native frame "+id+": "+x+","+y);
    }
    private static Advancement clientRoot(MinecraftClient client,String id){return client.player.networkHandler.getAdvancementHandler().getManager().get(new Identifier("minecraft",id));}
    private static void clickTab(AdvancementsScreen screen,Identifier id) {
        var entry=tabs(screen).entrySet().stream().filter(e->e.getKey().getId().equals(id)).findFirst().orElseThrow(()->new IllegalStateException("Legitimately earned native tab missing: "+id));
        int left=(screen.width-252)/2,top=(screen.height-140)/2;
        boolean clicked=false;
        for(int y=top-30;y<top+170&&!clicked;y+=2)for(int x=left-30;x<left+282&&!clicked;x+=2)if(entry.getValue().isClickOnTab(left,top,x,y)){screen.mouseClicked(x,y,0);screen.mouseReleased(x,y,0);clicked=true;}
        if(!clicked||selected(screen)!=entry.getValue())throw new IllegalStateException("Native mouse click could not select actual tab "+id);
    }
    @SuppressWarnings("unchecked") private static Map<Advancement,AdvancementTab> tabs(AdvancementsScreen screen){return(Map<Advancement,AdvancementTab>)field(screen,"tabs");}
    private static AdvancementTab selected(AdvancementsScreen screen){return(AdvancementTab)field(screen,"selectedTab");}
    private static double number(Object target,String name){return((Number)field(target,name)).doubleValue();}
    private static Object field(Object target,String name){try{var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(ReflectiveOperationException problem){throw new IllegalStateException("Cannot observe native UI "+name,problem);}}
    private void submit(MinecraftClient client,Runnable action){client.getServer().execute(()->{try{action.run();}catch(Throwable problem){failure.set(problem);}});}
    private static void send(MinecraftClient client,String command){client.setScreen(null);client.player.networkHandler.sendChatCommand(command);org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_ADVANCEMENT_LAYOUT_NATIVE_COMMAND {}",command);}

    private static final class Scene {
        final ServerPlayerEntity player;final ServerWorld world;final RegistryKey<World> originalDimension;final Vec3d oldPosition;final float yaw,pitch,health;final GameMode mode;final boolean noGravity,flying,allowFlying,invulnerable,wasOp;
        final int selectedSlot;final NbtList inventory;final NbtCompound hunger=new NbtCompound();final List<StatusEffectInstance> effects;final Map<String,String> before;final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();
        volatile boolean prepared,tablePickedUp,appleConsumed,combatReady,cowKilled,netherEarned,endEarned,restored;volatile int cowId,appleUsedDelta,preservedCriteria;volatile String diagnostic="setup";
        CowEntity cow;ItemEntity table;int appleBefore;boolean cleanup;
        Scene(ServerPlayerEntity player) {
            if(player==null)throw new IllegalStateException("Real connected player disappeared");this.player=player;world=player.getServerWorld();originalDimension=world.getRegistryKey();oldPosition=player.getPos();yaw=player.getYaw();pitch=player.getPitch();health=player.getHealth();mode=player.interactionManager.getGameMode();noGravity=player.hasNoGravity();flying=player.getAbilities().flying;allowFlying=player.getAbilities().allowFlying;invulnerable=player.getAbilities().invulnerable;wasOp=player.getServer().getPlayerManager().isOperator(player.getGameProfile());
            selectedSlot=player.getInventory().selectedSlot;inventory=player.getInventory().writeNbt(new NbtList());player.getHungerManager().writeNbt(hunger);effects=player.getStatusEffects().stream().map(StatusEffectInstance::new).toList();before=dates(player);
            var visible=player.getServer().getAdvancementLoader().getAdvancements().stream().filter(a->a.getId().getNamespace().equals(TetanusMod.MOD_ID)&&a.getDisplay()!=null).toList();
            if(visible.size()!=27||visible.stream().anyMatch(a->!player.getAdvancementTracker().getProgress(a).isDone()))throw new IllegalStateException("Run the actual all-advancement journey before layout coverage; existing progress is never granted or reset here");
        }
        void prepare() {
            if(player.getServer().getWorld(World.NETHER)==null||player.getServer().getWorld(World.END)==null)throw new IllegalStateException("Native Nether and End worlds are required for normal vanilla tab proof");
            if(!wasOp)player.getServer().getPlayerManager().addToOperators(player.getGameProfile());
            for(int x=254;x<=261;x++)for(int z=126;z<=132;z++)for(int y=3;y<=6;y++){
                var pos=new BlockPos(x,y,z);if(world.getBlockEntity(pos)!=null)throw new IllegalStateException("Layout fixture refuses to overwrite an existing block entity at "+pos);blocks.put(pos,world.getBlockState(pos));world.setBlockState(pos,y==3?Blocks.STONE.getDefaultState():Blocks.AIR.getDefaultState());
            }
            player.changeGameMode(GameMode.SURVIVAL);player.clearStatusEffects();player.setNoGravity(true);player.getAbilities().invulnerable=true;player.sendAbilitiesUpdate();player.teleport(world,LAB.x,LAB.y,LAB.z,180,-30);player.getInventory().clear();
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.APPLE));player.getHungerManager().setFoodLevel(0);player.getHungerManager().setSaturationLevel(0);appleBefore=player.getStatHandler().getStat(Stats.USED.getOrCreateStat(Items.APPLE));
            table=new ItemEntity(world,LAB.x,LAB.y+.1,LAB.z,new ItemStack(Items.CRAFTING_TABLE));table.setPickupDelay(0);world.spawnEntity(table);player.currentScreenHandler.sendContentUpdates();prepared=true;
        }
        void observe() {
            tablePickedUp=table!=null&&table.isRemoved()&&player.getInventory().count(Items.CRAFTING_TABLE)==1&&done("story/root");
            appleUsedDelta=player.getStatHandler().getStat(Stats.USED.getOrCreateStat(Items.APPLE))-appleBefore;
            appleConsumed=appleUsedDelta==1&&!player.getMainHandStack().isOf(Items.APPLE)&&done("husbandry/root");
            cowKilled=cow!=null&&!cow.isAlive()&&done("adventure/root");netherEarned=done("nether/root");endEarned=done("end/root");
            diagnostic="dimension="+player.getWorld().getRegistryKey().getValue()+", position="+player.getPos()+", table="+tablePickedUp+", appleUsed="+appleUsedDelta+", hand="+player.getMainHandStack()+", cowKilled="+cowKilled;
        }
        void prepareCombat() {
            if(combatReady)return;player.changeGameMode(GameMode.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;player.getAbilities().invulnerable=true;player.sendAbilitiesUpdate();player.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));
            cow=EntityType.COW.create(world);if(cow==null)throw new IllegalStateException("Native cow creation failed");cow.setPosition(LAB.add(1.5,0,0));cow.setAiDisabled(true);cow.setBaby(true);cow.setHealth(1);world.spawnEntity(cow);cowId=cow.getId();player.currentScreenHandler.sendContentUpdates();combatReady=true;
        }
        boolean done(String id){var advancement=player.getServer().getAdvancementLoader().get(new Identifier("minecraft",id));return advancement!=null&&player.getAdvancementTracker().getProgress(advancement).isDone();}
        void verifyAndRestore() {
            observe();if(!tablePickedUp||!appleConsumed||!cowKilled||!netherEarned||!endEarned)throw new IllegalStateException("Native vanilla root action coverage incomplete: "+diagnostic);
            var after=dates(player);if(!after.entrySet().containsAll(before.entrySet()))throw new IllegalStateException("Screen/navigation update lost or changed an existing earned criterion date");preservedCriteria=before.size();
            restore();restored=true;
        }
        void restore() {
            if(cleanup)return;cleanup=true;
            if(cow!=null&&!cow.isRemoved())cow.discard();if(table!=null&&!table.isRemoved())table.discard();
            for(var entry:blocks.entrySet())world.setBlockState(entry.getKey(),entry.getValue());
            player.teleport(world,oldPosition.x,oldPosition.y,oldPosition.z,yaw,pitch);player.changeGameMode(mode);player.setNoGravity(noGravity);player.getAbilities().flying=flying;player.getAbilities().allowFlying=allowFlying;player.getAbilities().invulnerable=invulnerable;player.sendAbilitiesUpdate();
            player.getInventory().readNbt(inventory.copy());player.getInventory().selectedSlot=selectedSlot;player.getHungerManager().readNbt(hunger.copy());player.clearStatusEffects();for(var effect:effects)player.addStatusEffect(new StatusEffectInstance(effect));player.setHealth(health);player.currentScreenHandler.sendContentUpdates();
            if(!wasOp)player.getServer().getPlayerManager().removeFromOperators(player.getGameProfile());
        }
        static Map<String,String> dates(ServerPlayerEntity player) {
            var result=new TreeMap<String,String>();for(var advancement:player.getServer().getAdvancementLoader().getAdvancements()){var progress=player.getAdvancementTracker().getProgress(advancement);for(String criterion:progress.getObtainedCriteria())result.put(advancement.getId()+"/"+criterion,progress.getCriterionProgress(criterion).toJson().toString());}return result;
        }
    }
}
