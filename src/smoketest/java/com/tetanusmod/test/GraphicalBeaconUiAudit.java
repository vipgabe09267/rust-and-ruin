package com.tetanusmod.test;

import com.google.gson.GsonBuilder;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.beacon.*;
import com.tetanusmod.client.TetanizedBeaconScreen;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Actual native beacon choices, pixels, slot geometry and payment at four uncapped GUI scales. */
public final class GraphicalBeaconUiAudit {
    private static final int[] SCALES={2,3,4,6};
    private static final String[] VIEWS={"ii","iii","iv-unpaid","iv-paid","regeneration"};
    private static final BlockPos BEACON=new BlockPos(480,8,96);
    private static final Vec3d CAMERA=new Vec3d(480.5,9,93.5);
    private final String viewer=FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
    private final List<Map<String,Object>> screenshots=new ArrayList<>(),scales=new ArrayList<>();
    private CompletableFuture<?> pending;
    private Scene scene;
    private UUID actor;
    private int phase,phaseTicks,ticks,scaleIndex,viewIndex,stable;
    private int oldWidth,oldHeight,oldX,oldY,oldScale;
    private boolean oldUnicode,started,complete,restoring;
    private double oldMouseX,oldMouseY;
    private long startupWallMillis,lastDiagnosticMillis;private int lastDiagnosticPhase=-1;private boolean startupRetry;

    public void start(MinecraftClient client) {
        require(!started&&client.player!=null&&client.getServer()!=null,"Beacon UI proof requires a connected integrated player");
        require(!client.getWindow().isFullscreen(),"Use a fresh windowed graphical fixture");
        started=true;actor=client.player.getUuid();startupWallMillis=System.currentTimeMillis();
        var window=client.getWindow();
        oldWidth=window.getWidth();oldHeight=window.getHeight();oldX=window.getX();oldY=window.getY();
        oldScale=client.options.getGuiScale().getValue();oldUnicode=client.options.getForceUnicodeFont().getValue();
        oldMouseX=client.mouse.getX();oldMouseY=client.mouse.getY();
        client.setScreen(null);client.options.useKey.setPressed(false);client.options.attackKey.setPressed(false);
        client.options.getForceUnicodeFont().setValue(false);client.options.getGuiScale().setValue(2);
        window.setWindowedSize(1920,1440);GLFW.glfwSetWindowPos(window.getHandle(),0,0);
        pending=server(client,p->{scene=new Scene(p);scene.prepare();return null;});
    }
    public boolean tick(MinecraftClient client) {
        try{return tickInternal(client);}catch(RuntimeException|Error failure){restore(client);throw failure;}
    }
    private boolean tickInternal(MinecraftClient client) {
        require(started,"Beacon UI proof was not started");if(complete)return true;
        require(++ticks<5000,"Beacon UI proof timeout: phase="+phase+", scale="+scaleIndex+", view="+viewIndex);
        phaseTicks++;
        if(pending!=null&&pending.isCompletedExceptionally())pending.join();
        long now=System.currentTimeMillis();
        if(phase!=lastDiagnosticPhase||now-lastDiagnosticMillis>=2500){
            org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_BEACON_UI_STATE phase={} scale={} view={} elapsedMs={} {}",phase,scaleIndex,viewIndex,now-startupWallMillis,diagnostic(client));
            lastDiagnosticPhase=phase;lastDiagnosticMillis=now;
        }
        if(phase==0)require(now-startupWallMillis<=30000,"Native beacon viewport failed to settle within30seconds: "+diagnostic(client));
        switch(phase) {
            case 0->{
                if(pending!=null){if(!done())return false;pending.join();pending=null;}
                var window=client.getWindow();
                int[] nativeWidth={0},nativeHeight={0};
                GLFW.glfwGetWindowSize(window.getHandle(),nativeWidth,nativeHeight);
                boolean iconified=GLFW.glfwGetWindowAttrib(window.getHandle(),GLFW.GLFW_ICONIFIED)!=0;
                boolean actual=!iconified&&nativeWidth[0]==1920&&nativeHeight[0]==1440
                    &&window.getWidth()==1920&&window.getHeight()==1440
                    &&window.getFramebufferWidth()>=1920&&window.getFramebufferHeight()>=1440
                    &&client.getFramebuffer().textureWidth==window.getFramebufferWidth()
                    &&client.getFramebuffer().textureHeight==window.getFramebufferHeight();
                if(!actual&&!startupRetry&&now-startupWallMillis>=3000){
                    startupRetry=true;
                    org.slf4j.LoggerFactory.getLogger("tetanusmod").warn("GRAPHICAL_BEACON_UI_STARTUP_RETRY measuredIconified={} before={}",iconified,diagnostic(client));
                    if(iconified)GLFW.glfwRestoreWindow(window.getHandle());
                    window.setWindowedSize(1920,1440);
                    GLFW.glfwSetWindowSize(window.getHandle(),1920,1440);
                    client.onResolutionChanged();
                }
                stable=actual?stable+1:0;if(stable<10)return false;
                client.onResolutionChanged();
                require(window.calculateScaleFactor(6,false)==6,"Native window cannot support actual GUI scale6");
                next(1);
            }
            case 1->{
                if(client.player.squaredDistanceTo(CAMERA)>.04||!client.world.getBlockState(BEACON).isOf(ModBeacons.BLOCK))return false;
                look(client,Vec3d.ofCenter(BEACON));
                if(!(client.crosshairTarget instanceof BlockHitResult hit)||!hit.getBlockPos().equals(BEACON)||phaseTicks<25)return false;
                require(client.interactionManager.interactBlock(client.player,Hand.MAIN_HAND,hit).isAccepted(),"Native beacon block click rejected");
                next(2);
            }
            case 2->{
                var menu=menu(client);if(menu==null||menu.getProperties()!=4||phaseTicks<25)return false;
                shift(client,find(menu,Items.NETHERITE_INGOT));
                click(client,Text.translatable(StatusEffects.HASTE.getTranslationKey()).getString());
                scaleIndex=0;viewIndex=0;next(3);
            }
            case 3->{
                client.options.getGuiScale().setValue(SCALES[scaleIndex]);client.onResolutionChanged();
                point(client,8,8);next(4);
            }
            case 4->{
                if(menu(client)==null||phaseTicks<20)return false;
                require(client.getWindow().getScaleFactor()==SCALES[scaleIndex],"Requested GUI scale was capped instead of actually applied");
                select(client,viewIndex);point(client,8,8);next(5);
            }
            case 5->{
                var menu=menu(client);if(menu==null||phaseTicks<25)return false;
                boolean needsStar=viewIndex==3;
                if(menu.hasStar()!=needsStar)return false;
                require(widget(client,tr("gui.tetanusmod.beacon.activate")).active==(viewIndex!=2),"Native confirmation state disagrees with required payment");
                var geometry=geometry(client);
                capture(client,VIEWS[viewIndex],geometry);
                if(++viewIndex<VIEWS.length){next(4);return false;}
                scales.add(Map.of("RequestedGuiScale",SCALES[scaleIndex],"AppliedGuiScale",client.getWindow().getScaleFactor(),
                    "ScaledViewport",List.of(client.currentScreen.width,client.currentScreen.height),"NativeGeometry",geometry));
                if(++scaleIndex<SCALES.length){viewIndex=0;next(3);}
                else{click(client,tr("gui.tetanusmod.beacon.upgrade_iv"));shift(client,find(menu,ModItems.TETANIZED_WITHER_STAR));next(6);}
            }
            case 6->{
                var menu=menu(client);if(menu==null||!menu.hasStar()||phaseTicks<25)return false;
                require(widget(client,tr("gui.tetanusmod.beacon.activate")).active,"Actual IV payment did not enable activation");
                click(client,tr("gui.tetanusmod.beacon.activate"));next(7);
            }
            case 7->{
                var effect=client.player.getStatusEffect(StatusEffects.HASTE);
                if(client.currentScreen!=null||client.player.getInventory().count(Items.NETHERITE_INGOT)!=1
                    ||client.player.getInventory().count(ModItems.TETANIZED_WITHER_STAR)!=1
                    ||effect==null||effect.getAmplifier()!=3||!client.player.hasStatusEffect(TetanusMod.VACCINATED)||phaseTicks<100)return false;
                pending=server(client,p->{require(scene.beacon.isLevelIV(),"Native paid IV flag did not persist");return null;});
                next(8);
            }
            case 8->{
                if(!done())return false;pending.join();pending=null;restore(client);next(9);
            }
            case 9->{
                if(pending!=null){if(!done())return false;pending.join();pending=null;}
                var window=client.getWindow();
                if(!scene.restored||window.getWidth()!=oldWidth||window.getHeight()!=oldHeight
                    ||window.getX()!=oldX||window.getY()!=oldY||phaseTicks<20)return false;
                require(client.player.getInventory().selectedSlot==scene.selected,"Original selected slot did not synchronize");
                pointRaw(client,oldMouseX,oldMouseY);write(client);complete=true;return true;
            }
            default->throw new IllegalStateException("Unknown beacon UI phase "+phase);
        }
        return false;
    }
    private void select(MinecraftClient client,int view) {
        switch(view) {
            case 0->click(client,tr("gui.tetanusmod.beacon.level_ii"));
            case 1->click(client,tr("gui.tetanusmod.beacon.upgrade_iii"));
            case 2->click(client,tr("gui.tetanusmod.beacon.upgrade_iv"));
            case 3->shift(client,find(menu(client),ModItems.TETANIZED_WITHER_STAR));
            case 4->{shift(client,TetanizedBeaconScreenHandler.STAR_SLOT);click(client,Text.translatable(StatusEffects.REGENERATION.getTranslationKey()).append(" II").getString());}
            default->throw new IllegalStateException("Unknown beacon UI view");
        }
    }
    private Map<String,Object> geometry(MinecraftClient client) {
        var screen=client.currentScreen;int left=(screen.width-TetanizedBeaconScreen.PANEL_WIDTH)/2,top=(screen.height-TetanizedBeaconScreen.PANEL_HEIGHT)/2;
        require(left>=2&&top>=2&&left+TetanizedBeaconScreen.PANEL_WIDTH+2<=screen.width
            &&top+TetanizedBeaconScreen.PANEL_HEIGHT+2<=screen.height,"Beacon frame clips the actual scaled viewport");
        var widgets=new ArrayList<PressableWidget>();
        for(var child:screen.children())if(child instanceof PressableWidget widget
            &&widget.getClass().getName().startsWith(TetanizedBeaconScreen.class.getName()+"$"))widgets.add(widget);
        require(widgets.size()==10,"Beacon UI lost a primary, strength, regeneration or confirm control");
        var rects=new ArrayList<int[]>();
        for(var widget:widgets) {
            int padding=widget.getWidth()>=118?29:8;
            require(client.textRenderer.getWidth(widget.getMessage())<=widget.getWidth()-padding,"Beacon button text clips: "+widget.getMessage().getString());
            require(widget.getX()>=left&&widget.getY()>=top&&widget.getX()+widget.getWidth()<=left+TetanizedBeaconScreen.PANEL_WIDTH
                &&widget.getY()+widget.getHeight()<=top+TetanizedBeaconScreen.PANEL_HEIGHT,"Beacon widget is outside its native panel");
            rects.add(new int[]{widget.getX(),widget.getY(),widget.getWidth(),widget.getHeight()});
        }
        for(var slot:menu(client).slots)rects.add(new int[]{left+slot.x,top+slot.y,16,16});
        for(int i=0;i<rects.size();i++)for(int j=i+1;j<rects.size();j++) {
            var a=rects.get(i);var b=rects.get(j);
            require(!(a[0]<b[0]+b[2]&&a[0]+a[2]>b[0]&&a[1]<b[1]+b[3]&&a[1]+a[3]>b[1]),"Native beacon controls or inventory slots overlap");
        }
        return Map.of("Widgets",widgets.size(),"NativeSlots",menu(client).slots.size(),"PanelBounds",List.of(left,top,TetanizedBeaconScreen.PANEL_WIDTH,TetanizedBeaconScreen.PANEL_HEIGHT),
            "NoControlOrSlotOverlap",true,"ButtonLabelsFit",true,"ViewportContainsFrame",true);
    }
    private void capture(MinecraftClient client,String view,Map<String,Object> geometry) {
        String name="rust-and-ruin-"+viewer+"-beacon-ui-scale"+SCALES[scaleIndex]+"-"+view+".png";
        ScreenshotRecorder.saveScreenshot(client.runDirectory,name,client.getFramebuffer(),text->{});
        screenshots.add(Map.of("Screenshot",name,"View",view,"RequestedGuiScale",SCALES[scaleIndex],
            "AppliedGuiScale",client.getWindow().getScaleFactor(),"Geometry",geometry,"NativeScreen",client.currentScreen.getClass().getName()));
        org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_BEACON_UI_VIEW {} scale={}",view,SCALES[scaleIndex]);
    }
    private void write(MinecraftClient client) {
        var report=new LinkedHashMap<String,Object>();report.put("Result","PASS");report.put("ActualGraphicalClient",true);
        report.put("Viewer",viewer);report.put("Scales",scales);report.put("Screenshots",screenshots);
        report.put("NativeCurrencyConsumed",Map.of("NetheriteIngots",1,"TetanizedWitherStars",1));
        report.put("NativeHasteIVApplied",true);report.put("UnpaidIVConfirmationRejected",true);report.put("OriginalPlayerAndWorldRestored",scene.restored);
        report.put("DirectCriterionGrants",0);report.put("ProgressRevocations",0);
        try{Files.writeString(client.runDirectory.toPath().resolve("graphical-beacon-ui-"+viewer+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}
        catch(Exception problem){throw new IllegalStateException("Cannot save actual beacon UI evidence",problem);}
    }
    private void restore(MinecraftClient client) {
        if(restoring)return;restoring=true;client.setScreen(null);client.options.useKey.setPressed(false);client.options.attackKey.setPressed(false);
        client.options.getGuiScale().setValue(oldScale);client.options.getForceUnicodeFont().setValue(oldUnicode);
        client.getWindow().setWindowedSize(oldWidth,oldHeight);GLFW.glfwSetWindowPos(client.getWindow().getHandle(),oldX,oldY);client.onResolutionChanged();
        if(scene!=null)pending=server(client,p->{scene.restore();return null;});else pending=CompletableFuture.completedFuture(null);
    }
    private static TetanizedBeaconScreenHandler menu(MinecraftClient client) {
        return client.currentScreen instanceof TetanizedBeaconScreen&&client.player.currentScreenHandler instanceof TetanizedBeaconScreenHandler menu?menu:null;
    }
    private static PressableWidget widget(MinecraftClient client,String label) {
        require(client.currentScreen!=null,"Native beacon screen closed unexpectedly");
        for(var child:client.currentScreen.children())if(child instanceof PressableWidget widget&&widget.getMessage().getString().equals(label))return widget;
        throw new IllegalStateException("Actual native beacon widget missing: "+label);
    }
    private static void click(MinecraftClient client,String label) {
        var widget=widget(client,label);var screen=client.currentScreen;require(widget.active,"Beacon widget is inactive: "+label);
        double x=widget.getX()+widget.getWidth()/2.0,y=widget.getY()+widget.getHeight()/2.0;
        require(screen.mouseClicked(x,y,0),"Native screen rejected beacon widget click: "+label);
        if(client.currentScreen==screen)screen.mouseReleased(x,y,0);
    }
    private static int find(TetanizedBeaconScreenHandler menu,Item item) {
        for(int i=1;i<37;i++)if(menu.getSlot(i).getStack().isOf(item))return i;
        throw new IllegalStateException("Native inventory lacks beacon currency "+item);
    }
    private static void shift(MinecraftClient client,int slot) {
        client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,slot,0,SlotActionType.QUICK_MOVE,client.player);
    }
    private String diagnostic(MinecraftClient client) {
        var window=client.getWindow();int[] width={0},height={0},frameWidth={0},frameHeight={0};
        GLFW.glfwGetWindowSize(window.getHandle(),width,height);
        GLFW.glfwGetFramebufferSize(window.getHandle(),frameWidth,frameHeight);
        return "native="+width[0]+"x"+height[0]+", cached="+window.getWidth()+"x"+window.getHeight()
            +", nativeFramebuffer="+frameWidth[0]+"x"+frameHeight[0]+", framebuffer="+window.getFramebufferWidth()+"x"+window.getFramebufferHeight()
            +", renderTarget="+client.getFramebuffer().textureWidth+"x"+client.getFramebuffer().textureHeight
            +", iconified="+GLFW.glfwGetWindowAttrib(window.getHandle(),GLFW.GLFW_ICONIFIED)
            +", focused="+GLFW.glfwGetWindowAttrib(window.getHandle(),GLFW.GLFW_FOCUSED)
            +", screen="+(client.currentScreen==null?"HUD":client.currentScreen.getClass().getName())
            +", prepared="+(scene!=null)+", pending="+(pending!=null&&!pending.isDone())
            +", position="+(client.player==null?"absent":client.player.getPos())+", wallUtc="+java.time.Instant.now();
    }
    private static String tr(String key) { return Text.translatable(key).getString(); }
    private static void look(MinecraftClient client,Vec3d target) {
        var delta=target.subtract(client.player.getEyePos());
        client.player.setYaw((float)(Math.toDegrees(Math.atan2(delta.z,delta.x))-90));
        client.player.setPitch((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));
    }
    private static void point(MinecraftClient client,double x,double y) {
        pointRaw(client,x*client.getWindow().getScaleFactor(),y*client.getWindow().getScaleFactor());
    }
    private static void pointRaw(MinecraftClient client,double x,double y) {
        GLFW.glfwSetCursorPos(client.getWindow().getHandle(),x,y);
        try{var callback=net.minecraft.client.Mouse.class.getDeclaredMethod("onCursorPos",long.class,double.class,double.class);callback.setAccessible(true);callback.invoke(client.mouse,client.getWindow().getHandle(),x,y);}
        catch(ReflectiveOperationException problem){throw new IllegalStateException("Cannot synchronize actual native mouse",problem);}
    }
    private boolean done() { return pending!=null&&pending.isDone(); }
    private void next(int value) { phase=value;phaseTicks=0;stable=0; }
    private <T> CompletableFuture<T> server(MinecraftClient client,Function<ServerPlayerEntity,T> action) {
        var future=new CompletableFuture<T>();client.getServer().execute(()->{try{var player=client.getServer().getPlayerManager().getPlayer(actor);require(player!=null,"Beacon UI actor disconnected");future.complete(action.apply(player));}catch(Throwable problem){future.completeExceptionally(problem);}});return future;
    }
    private static void require(boolean value,String message) { if(!value)throw new IllegalStateException(message); }

    private static final class Scene {
        final ServerPlayerEntity player;final ServerWorld world;final Vec3d position;final float yaw,pitch,health;
        final GameMode mode;final boolean gravity,flying,allowFlying,invulnerable;final int selected;
        final NbtList inventory;final NbtCompound hunger=new NbtCompound();final List<StatusEffectInstance> effects;
        final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();
        TetanizedBeaconEntity beacon;boolean restored;
        Scene(ServerPlayerEntity player) {
            this.player=player;world=player.getServerWorld();
            require(world.getChunkManager().getChunkGenerator() instanceof net.minecraft.world.gen.chunk.FlatChunkGenerator,"Beacon UI proof requires a disposable flat world");
            position=player.getPos();yaw=player.getYaw();pitch=player.getPitch();health=player.getHealth();mode=player.interactionManager.getGameMode();
            gravity=player.hasNoGravity();flying=player.getAbilities().flying;allowFlying=player.getAbilities().allowFlying;invulnerable=player.getAbilities().invulnerable;
            selected=player.getInventory().selectedSlot;inventory=player.getInventory().writeNbt(new NbtList());
            player.getHungerManager().writeNbt(hunger);effects=player.getStatusEffects().stream().map(StatusEffectInstance::new).toList();
        }
        void set(BlockPos pos,BlockState state) {
            require(world.getBlockEntity(pos)==null,"Beacon fixture refuses an existing block entity at "+pos);
            blocks.put(pos,world.getBlockState(pos));world.setBlockState(pos,state);
        }
        void prepare() {
            player.closeHandledScreen();
            for(int layer=1;layer<=4;layer++)for(int x=-layer;x<=layer;x++)for(int z=-layer;z<=layer;z++)
                set(BEACON.add(x,-layer,z),com.tetanusmod.expansion.ExpansionBlocks.RUSTED_IRON_BLOCK.getDefaultState());
            set(BEACON,ModBeacons.BLOCK.getDefaultState());beacon=(TetanizedBeaconEntity)world.getBlockEntity(BEACON);
            player.changeGameMode(GameMode.CREATIVE);player.setNoGravity(true);player.getAbilities().flying=true;player.getAbilities().invulnerable=true;player.sendAbilitiesUpdate();
            player.clearStatusEffects();player.teleport(world,CAMERA.x,CAMERA.y,CAMERA.z,180,0);player.setVelocity(Vec3d.ZERO);
            player.getInventory().clear();player.getInventory().selectedSlot=0;
            player.getInventory().setStack(9,new ItemStack(Items.NETHERITE_INGOT,2));
            player.getInventory().setStack(10,new ItemStack(ModItems.TETANIZED_WITHER_STAR,2));
            player.currentScreenHandler.sendContentUpdates();player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(0));
        }
        void restore() {
            if(restored)return;player.closeHandledScreen();
            for(var entry:blocks.entrySet())world.setBlockState(entry.getKey(),entry.getValue());
            player.teleport(world,position.x,position.y,position.z,yaw,pitch);player.changeGameMode(mode);player.setNoGravity(gravity);
            player.getAbilities().flying=flying;player.getAbilities().allowFlying=allowFlying;player.getAbilities().invulnerable=invulnerable;player.sendAbilitiesUpdate();
            player.getInventory().readNbt(inventory.copy());player.getInventory().selectedSlot=selected;player.getHungerManager().readNbt(hunger.copy());
            player.clearStatusEffects();for(var effect:effects)player.addStatusEffect(new StatusEffectInstance(effect));player.setHealth(health);
            player.currentScreenHandler.sendContentUpdates();player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(selected));restored=true;
        }
    }
}
