package com.tetanusmod.test;

import com.google.gson.GsonBuilder;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.beacon.*;
import com.tetanusmod.entity.*;
import com.tetanusmod.expansion.*;
import com.tetanusmod.registry.*;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.block.*;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.item.*;
import net.minecraft.network.*;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import java.nio.file.*;
import java.util.*;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.server.world.ServerWorld;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

/** Actual graphical client proof: native actions, synchronized gold nodes, per-node screenshots. */
public final class GraphicalAdvancementAudit {
    private final ConcurrentLinkedQueue<String> proofs = new ConcurrentLinkedQueue<>();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private final AtomicBoolean serverDone = new AtomicBoolean();
    private final Map<String,Object> clientEvidence = new LinkedHashMap<>();
    private volatile Fixture fixture;
    private String viewer, capturing;
    private int captureTicks, waitTicks;
    private boolean started, respawnRequested, finalWritten, physicsQueued;

    public void start(MinecraftClient client) {
        if(started)throw new IllegalStateException("Graphical advancement audit started twice");started=true;
        if(client.getServer()==null||client.player==null)throw new IllegalStateException("An integrated player is required for graphical advancement testing");
        viewer=net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("roughlyenoughitems")?"rei":net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei")?"jei":"plain";
        var playerId=client.player.getUuid();
        client.setScreen(null);
        client.getServer().execute(()->{
            try {
                var actor=client.getServer().getPlayerManager().getPlayer(playerId);
                if(actor==null)throw new IllegalStateException("The real integrated player disappeared");
                var source=actor.getCommandSource().withLevel(4).withSilent();
                if(source.getPlayerOrThrow()!=actor)throw new IllegalStateException("Fresh reset command source is not the actual graphical player");
                var dispatcher=actor.getServer().getCommandManager().getDispatcher();
                for(String id:ACTIONS.keySet()){
                    var advancement=actor.getServer().getAdvancementLoader().get(TetanusMod.id(id));
                    if(advancement==null)throw new IllegalStateException("Active advancement missing before fresh graphical run: "+id);
                    if(actor.getAdvancementTracker().getProgress(advancement).getObtainedCriteria().iterator().hasNext())
                        dispatcher.execute("advancement revoke @s only tetanusmod:"+id,source);
                }
                var recipes=actor.getServerWorld().getRecipeManager().values().stream().filter(recipe->recipe.getId().getNamespace().equals(TetanusMod.MOD_ID)).toList();
                actor.lockRecipes(recipes);
                if(recipes.stream().anyMatch(actor.getRecipeBook()::contains))throw new IllegalStateException("Native recipe reset left a mod recipe unlocked");
                for(String id:ACTIONS.keySet())if(actor.getAdvancementTracker().getProgress(actor.getServer().getAdvancementLoader().get(TetanusMod.id(id))).getObtainedCriteria().iterator().hasNext())throw new IllegalStateException("Native reset left prior criterion progress for "+id);
                if(!actor.getStatusEffects().isEmpty())dispatcher.execute("effect clear @s",source);
                fixture=new Fixture(actor,viewer,proofs,serverDone);
                new Journey(fixture).start();
            } catch(Throwable problem){if(fixture!=null)fixture.restorePhysics();failure.set(problem);}
        });
    }

    public boolean tick(MinecraftClient client) {
        try{return tickInternal(client);}
        catch(RuntimeException|Error problem){client.options.useKey.setPressed(false);if(fixture!=null&&client.getServer()!=null)client.getServer().execute(()->fixture.restorePhysics());throw problem;}
    }
    private boolean tickInternal(MinecraftClient client) {
        if(!started)throw new IllegalStateException("Graphical advancement audit was not started");
        if(failure.get()!=null){client.options.useKey.setPressed(false);throw new IllegalStateException("Graphical advancement action failed",failure.get());}
        if(finalWritten){client.options.useKey.setPressed(false);return true;}
        if(client.player==null||client.world==null||fixture==null)return false;
        var dose=fixture.pendingDose;
        client.options.useKey.setPressed(dose!=null&&dose.clientStarted&&!dose.nativeComplete&&ItemStack.areEqual(client.player.getMainHandStack(),dose.expected));
        if(!client.player.isAlive()) {
            client.options.useKey.setPressed(false);
            if(!respawnRequested){respawnRequested=true;client.setScreen(null);client.player.requestRespawn();}
            return false;
        }
        respawnRequested=false;
        client.getServer().execute(()->{try{fixture.advance();}catch(Throwable problem){failure.set(problem);}});
        if(capturing!=null) {
            boolean expectedScreen=capturing.equals("recipes/unlock")?client.currentScreen instanceof InventoryScreen:client.currentScreen instanceof FocusedAdvancementsScreen;
            if(!expectedScreen){if(openProof(client,capturing))captureTicks=0;return false;}
            captureTicks++;
            if(captureTicks==20) {
                String file="rust-and-ruin-"+viewer+"-adv-"+capturing.replace('/','-')+".png";
                ScreenshotRecorder.saveScreenshot(client.runDirectory,file,client.getFramebuffer(),text->{});
                var proof=new LinkedHashMap<String,Object>();proof.put("Action",fixture.causes.getOrDefault(capturing,ACTIONS.get(capturing)));proof.put("ClientProgress","COMPLETE");proof.put("Screenshot",file);
                if(capturing.equals("recipes/unlock"))proof.put("ClientUnlockedRecipeIds",client.world.getRecipeManager().values().stream().filter(recipe->recipe.getId().getNamespace().equals(TetanusMod.MOD_ID)&&client.player.getRecipeBook().contains(recipe)).map(recipe->recipe.getId().toString()).sorted().toList());
                clientEvidence.put(capturing,proof);
                org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_ADVANCEMENT_PASS {} {}",capturing,file);
            }
            if(captureTicks>=40){client.setScreen(null);capturing=null;captureTicks=0;}
            return false;
        }
        if(fixture.pendingDose!=null){handleDose(client,fixture.pendingDose);return false;}
        String next=proofs.peek();
        if(next!=null) {
            if(openProof(client,next)){proofs.remove();capturing=next;captureTicks=0;waitTicks=0;}
            else if(++waitTicks>300)throw new IllegalStateException("Earned advancement never reached the graphical client: "+next);
            return false;
        }
        if(serverDone.get()) {
            if(!fixture.finalPhysicsRestored){
                if(!physicsQueued){physicsQueued=true;client.getServer().execute(()->{fixture.restorePhysics();fixture.finalPhysicsRestored=true;});}
                return false;
            }
            if(clientEvidence.size()!=28)throw new IllegalStateException("Graphical advancement evidence incomplete: "+clientEvidence.keySet());
            var report=new LinkedHashMap<String,Object>();report.put("Result","PASS");report.put("ActualGraphicalPlayer",true);report.put("ActiveAdvancements",28);report.put("DirectCriterionGrants",0);report.put("Advancements",clientEvidence);report.put("Retired",Map.of("variant_egg","Hidden save-compatibility ID; no longer earnable by design. Actual variant spawn eggs were exercised."));
            try{Files.writeString(Path.of("graphical-advancements-"+viewer+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}catch(java.io.IOException problem){throw new IllegalStateException(problem);}
            client.options.useKey.setPressed(false);finalWritten=true;return true;
        }
        return false;
    }

    private void handleDose(MinecraftClient client,DoseRequest dose) {
        var held=client.player.getMainHandStack();
        fixture.clientDoseDiagnostic="clientHand="+held+", nbt="+held.getNbt()+", potion="+Registries.POTION.getId(PotionUtil.getPotion(held))+", active="+client.player.isUsingItem()+", remaining="+client.player.getItemUseTimeLeft()+", screen="+(client.currentScreen==null?"NONE":client.currentScreen.getClass().getSimpleName())+", camera="+client.player.getYaw()+"/"+client.player.getPitch();
        if(dose.nativeComplete){
            client.options.useKey.setPressed(false);
            boolean correct=dose.expected.isFood()?held.isEmpty():held.isOf(Items.GLASS_BOTTLE)&&held.getCount()==1;
            if(correct&&!client.player.isUsingItem())client.getServer().execute(()->fixture.acknowledgeDose(dose));
            return;
        }
        if(dose.clientStarted)return;
        if(client.currentScreen!=null){client.setScreen(null);return;}
        if(!ItemStack.areEqual(held,dose.expected))return;
        if(dose.expected.isFood()&&!client.player.canConsume(false))return;
        client.player.setYaw(180);client.player.setPitch(-30);client.options.useKey.setPressed(true);
        // Initiation is a real sequenced client interaction after the expected
        // stack/HUD arrived. No server-side use timer is started preemptively.
        var result=client.interactionManager.interactItem(client.player,Hand.MAIN_HAND);
        if(!result.isAccepted()||!client.player.isUsingItem())throw new IllegalStateException("Native client did not start its dose: "+fixture.clientDoseDiagnostic+", result="+result);
        dose.clientStarted=true;
        org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_DOSE_CLIENT_START {} {}",dose.expected,fixture.clientDoseDiagnostic);
    }
    private static final class DoseRequest {
        final ItemStack expected;final int statBefore;final long requestedAt;
        volatile boolean clientStarted,nativeComplete;
        DoseRequest(ItemStack expected,int statBefore,long requestedAt){this.expected=expected.copy();this.statBefore=statBefore;this.requestedAt=requestedAt;}
    }
    private boolean openProof(MinecraftClient client,String id) {
        var handler=client.player.networkHandler.getAdvancementHandler();
        if(id.equals("recipes/unlock")) {
            // Recipe advancements have no display or native client advancement node.
            // Their functional client evidence is the synchronized recipe-book reward.
            var recipes=client.world.getRecipeManager().values().stream().filter(recipe->recipe.getId().getNamespace().equals(TetanusMod.MOD_ID)).toList();
            if(recipes.isEmpty()||recipes.stream().anyMatch(recipe->!client.player.getRecipeBook().contains(recipe)))return false;
            org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_ADVANCEMENT_RECIPE_BOOK_PASS {} runtime mod recipes unlocked",recipes.size());
            client.setScreen(new InventoryScreen(client.player));
            return true;
        }
        var advancement=handler.getManager().get(TetanusMod.id(id));
        var root=handler.getManager().get(TetanusMod.id("root"));
        if(advancement==null||root==null)return false;
        var screen=new FocusedAdvancementsScreen(handler);
        client.setScreen(screen);screen.selectTab(root);
        var widget=screen.getAdvancementWidget(advancement);
        if(widget==null){client.setScreen(null);return false;}
        try {
            var progressField=AdvancementWidget.class.getDeclaredField("progress");progressField.setAccessible(true);
            var progress=(AdvancementProgress)progressField.get(widget);
            if(progress==null||!progress.isDone()){client.setScreen(null);return false;}
            var tabField=AdvancementsScreen.class.getDeclaredField("selectedTab");tabField.setAccessible(true);
            var tab=(AdvancementTab)tabField.get(screen);
            setField(tab,"originX",117.0-widget.getX()-13);setField(tab,"originY",56.0-widget.getY()-13);setField(tab,"initialized",true);
            return true;
        }catch(ReflectiveOperationException problem){throw new IllegalStateException("Cannot inspect the actual advancement widget",problem);}
    }
    private static void setField(Object target,String name,Object value)throws ReflectiveOperationException {
        var field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
    private static final class FocusedAdvancementsScreen extends AdvancementsScreen {
        FocusedAdvancementsScreen(net.minecraft.client.network.ClientAdvancementManager handler){super(handler);}
        @Override public void render(net.minecraft.client.gui.DrawContext draw,int mouseX,int mouseY,float delta) {
            super.render(draw,(width-252)/2+9+117,(height-140)/2+18+56,delta);
        }
    }

    private static final class Fixture {
        private final ServerPlayerEntity player;
        private final ServerWorld world;
        private final BlockPos origin;
        private long start,logicalTick,lastClock;private boolean clockStarted;
        private final String viewer;
        private final TreeMap<Long,List<Runnable>> tasks=new TreeMap<>();
        private final Map<String,String> causes=new java.util.concurrent.ConcurrentHashMap<>();
        private volatile boolean finalPhysicsRestored;
        private volatile DoseRequest pendingDose;private volatile String clientDoseDiagnostic="No client dose input yet";
        private final boolean originalNoGravity,originalFlying,originalInvulnerable,originalPvp;
        private final GameMode originalMode;
        private final ConcurrentLinkedQueue<String> proofs;
        private final AtomicBoolean done;
        Fixture(ServerPlayerEntity player,String viewer,ConcurrentLinkedQueue<String> proofs,AtomicBoolean done) {
            this.player=player;this.world=player.getServerWorld();this.viewer=viewer;this.proofs=proofs;this.done=done;
            originalNoGravity=player.hasNoGravity();originalFlying=player.getAbilities().flying;originalInvulnerable=player.getAbilities().invulnerable;originalMode=player.interactionManager.getGameMode();originalPvp=world.getServer().isPvpEnabled();
            origin=new BlockPos(player.getBlockX()+256,Math.min(world.getTopY()-100,Math.max(world.getSeaLevel()+12,player.getBlockY()+12)),player.getBlockZ()+256);
            for(int x=0;x<=16;x+=8)for(int z=0;z<=16;z+=8)world.getChunk(origin.add(x,0,z));
            start=world.getTime()+40;
        }
        ServerWorld getWorld(){return world;}
        BlockPos getAbsolutePos(BlockPos relative){return origin.add(relative);}
        Vec3d getAbsolute(Vec3d relative){return relative.add(origin.getX(),origin.getY(),origin.getZ());}
        void setBlockState(int x,int y,int z,Block block){world.setBlockState(origin.add(x,y,z),block.getDefaultState());}
        void setBlockState(BlockPos pos,Block block){world.setBlockState(origin.add(pos),block.getDefaultState());}
        <T extends Entity>T spawnEntity(EntityType<T> type,Vec3d relative){var entity=type.create(world);if(entity==null)throw new IllegalStateException("Cannot create fixture "+type);var pos=getAbsolute(relative);entity.refreshPositionAndAngles(pos.x,pos.y,pos.z,0,0);world.spawnEntity(entity);return entity;}
        void runAtTick(long tick,Runnable task){tasks.computeIfAbsent(tick,unused->new ArrayList<>()).add(task);}
        void advance(){
            long now=world.getTime();
            if(!clockStarted){
                if(now<start)return;
                var root=world.getServer().getAdvancementLoader().get(TetanusMod.id("root"));
                if(root==null||!player.getAdvancementTracker().getProgress(root).isDone()){
                    if(now>start+200)throw new IllegalStateException("Actual player did not naturally receive the root advancement after chunk warmup");
                    return;
                }
                start=now;lastClock=now;clockStarted=true;
            }
            long delta=Math.max(0,now-lastClock);lastClock=now;
            if(pendingDose!=null){observeDose(now);return;}
            logicalTick+=delta;
            while(!tasks.isEmpty()&&tasks.firstKey()<=logicalTick){
                var entry=tasks.pollFirstEntry();for(var task:entry.getValue())task.run();
                if(pendingDose!=null)break;
            }
        }
        void requestDose(ItemStack expected,int statBefore){
            if(pendingDose!=null)throw new IllegalStateException("Two graphical doses overlap");
            pendingDose=new DoseRequest(expected,statBefore,world.getTime());
            player.playerScreenHandler.sendContentUpdates();player.currentScreenHandler.sendContentUpdates();
        }
        private void observeDose(long now){
            var dose=pendingDose;if(dose==null)return;
            int delta=player.getStatHandler().getStat(net.minecraft.stat.Stats.USED.getOrCreateStat(dose.expected.getItem()))-dose.statBefore;
            var held=player.getMainHandStack();
            String diagnostic="item="+dose.expected+", usedDelta="+delta+", serverHand="+held+", serverNbt="+held.getNbt()+", serverPotion="+Registries.POTION.getId(PotionUtil.getPotion(held))+", active="+player.isUsingItem()+", remaining="+player.getItemUseTimeLeft()+", logicalPhase="+logicalTick+", "+clientDoseDiagnostic;
            if(delta>1)throw new IllegalStateException("More than one native dose was consumed: "+diagnostic);
            if(delta==1&&!player.isUsingItem()){
                boolean correct=dose.expected.isFood()?held.isEmpty():held.isOf(Items.GLASS_BOTTLE)&&held.getCount()==1;
                if(!correct)throw new IllegalStateException("Native dose completed with the wrong remainder: "+diagnostic);
                if(!dose.nativeComplete)org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_DOSE_NATIVE_COMPLETE {}",diagnostic);
                dose.nativeComplete=true;
            }
            if(now-dose.requestedAt>400)throw new IllegalStateException("Native graphical dose did not finish/acknowledge within20seconds: "+diagnostic);
        }
        void acknowledgeDose(DoseRequest dose){
            if(pendingDose==dose&&dose.nativeComplete){pendingDose=null;lastClock=world.getTime();}
        }
        void assertTrue(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
        void proof(String id,String cause){causes.put(id,cause);proofs.add(id);org.slf4j.LoggerFactory.getLogger("tetanusmod").info("GRAPHICAL_ADVANCEMENT_SERVER_PASS {} {}",id,cause);}
        void restorePhysics(){
            pendingDose=null;
            var actual=world.getServer().getPlayerManager().getPlayer(player.getUuid());
            if(actual!=null){actual.changeGameMode(originalMode);actual.setNoGravity(originalNoGravity);actual.getAbilities().flying=originalFlying;actual.getAbilities().invulnerable=originalInvulnerable;actual.sendAbilitiesUpdate();actual.setVelocity(Vec3d.ZERO);}
            world.getServer().setPvpEnabled(originalPvp);
        }
        void complete(){done.set(true);}
    }
    private static final Map<String,String> ACTIONS = actions();
    private static Map<String,String> actions() {
        var result = new LinkedHashMap<String,String>();
        for (String[] row : new String[][]{
                {"root", "Fresh connected Survival player receives the native tick advancement."},
                {"iron_has_a_darker_side", "Pick up an iron ingot and synchronize the native inventory screen."},
                {"recipes/unlock", "The same real iron pickup unlocks the mod's recipes."},
                {"rusty_business", "Drop a nail into water and wait for the complete configured rust duration."},
                {"rusty_hook", "Drop fishing rods into water, allow native salvage rolls, and pick up a recovered hook."},
                {"glove_protection", "Equip gloves by right-clicking and withstand real rusty zombie melee attacks until one exposure is prevented."},
                {"shield_protection", "Raise a Tetanus Protection shield and block an actual enchanted zombie melee attack."},
                {"tetanized_mob", "Use Rust on an iron golem until a real conversion roll succeeds."},
                {"rusty_guardian", "The same successful, causal iron-golem conversion awards its specific advancement."},
                {"strange_meat", "Kill an egg-spawned Tetanized cow with a real player attack and pick up its infected meat."},
                {"clean_bill_of_health", "Apply a veterinary kit to a second egg-spawned Tetanized cow."},
                {"nice_one", "Kill an egg-spawned Tetanized Wither with a Creative-prepared test sword using the native attack method."},
                {"clinic", "Place a two-block Medicine Cabinet through the Survival block-item interaction."},
                {"trap_reset", "A cow walks into a placed trap; use its reset tool through the native block interaction."},
                {"rust_arrows", "Take a real 64-arrow/Rust smithing result through the native output slot."},
                {"medic", "Open a medic's actual merchant menu, supply its generated barter inputs, and take its output."},
                {"supply_run", "Sneak-interact with that medic holding its requested daily supply delivery."},
                {"spear", "Right-click a spear at a live indexed cow and land its actual raycast thrust."},
                {"infected", "Contract Tetanus from an actual rusty zombie injury or a real timed Tetanus potion."},
                {"why_did_i_eat_that", "Eat raw infected meat using the real item-use timer."},
                {"rabies", "Eat raw Rabid Meat using the real item-use timer."},
                {"rabies_cured", "Drink Rabies Cure after the raw-meat infection."},
                {"lockjaw_sonata", "Drink a Lockjaw Sonata potion using the real item-use timer."},
                {"antitoxin", "Drink antitoxin while Tetanus and Lockjaw are active."},
                {"vaccinated", "Drink a vaccine using the real item-use timer."},
                {"beacon_placed", "Place the Tetanized Beacon with its BlockItem."},
                {"beacon_full", "Build its complete rusty-iron pyramid, pay Netherite in the native menu, and wait for real beacon ticks."},
                {"why_did_i_do_that", "Throw a rusty nail upward and catch its returning real projectile with a one-health test player."}
        }) result.put(row[0], row[1]);
        return Collections.unmodifiableMap(result);
    }


    private static final class Journey {
        private final Fixture ctx;
        private final ServerPlayerEntity player;
        private final List<Entity> fixtures = new ArrayList<>();
        private final Map<String,String> completed = new LinkedHashMap<>();
        private final boolean originalPvp;
        private boolean finished, pvpChanged;
        private ZombieEntity attacker;
        private CowEntity spearTarget,meatCowFixture,treatmentCowFixture;
        private WitherEntity witherFixture;
        private VillagerEntity medic;
        private ItemEntity droppedNail;
        private BlockPos beaconPos;
        private NailEntity returningNail;
        private Item consuming;private int usageBefore;
        private final List<Map<String,Object>> consumptionEvidence=new ArrayList<>();

        Journey(Fixture ctx) {
            this.ctx = ctx;
            var world = ctx.getWorld();
            originalPvp = world.getServer().isPvpEnabled();
            player = ctx.player;
            player.changeGameMode(GameMode.SURVIVAL);
            player.getAbilities().creativeMode = false;
            player.getAbilities().invulnerable = false;
            player.setNoGravity(true);
            player.getInventory().clear();
            position(ctx.getAbsolute(new Vec3d(3.5, 3.2, 2.5)));
        }
        void start() {
            for(String id:ACTIONS.keySet())if(!id.equals("root"))ctx.assertTrue(!isDone(id),"Journey did not start with fresh advancement progress: "+id);
            var active = new TreeSet<String>();
            for (var advancement : player.getServer().getAdvancementLoader().getAdvancements()) {
                var id = advancement.getId();
                if (id.getNamespace().equals(TetanusMod.MOD_ID) && !id.getPath().equals("variant_egg")) active.add(id.getPath());
            }
            ctx.assertTrue(active.equals(new TreeSet<>(ACTIONS.keySet())), "End-to-end coverage omits a live mod advancement: " + active);
            var retired = player.getServer().getAdvancementLoader().get(TetanusMod.id("variant_egg"));
            ctx.assertTrue(retired != null && retired.getDisplay() == null, "Retired Creative-only Bad Egg unexpectedly became an active achievement");
            for (int x=0;x<=12;x++) for (int z=0;z<=12;z++) {
                ctx.setBlockState(x, 2, z, Blocks.STONE);
                for (int y=3;y<=8;y++) ctx.setBlockState(x, y, z, Blocks.AIR);
            }
            phase(4, this::waterAndInventory);
            phase(155, this::prepareCreatures);
            phase(160, this::worldInteractions);
            phase(165, this::spear);
            phase(170, () -> drink(ModBrewing.TETANUS));
            phase(204, () -> { consumed(TetanusMod.TETANUS);earned("infected"); eat(ModItems.INFECTED_MEAT); });
            phase(238, () -> { consumed(TetanusMod.SALMONELLA);earned("why_did_i_eat_that"); eat(ExpansionItems.RABID_MEAT); });
            phase(272, () -> { consumed(ExpansionEffects.RABIES);earned("rabies"); use(new ItemStack(ExpansionItems.RABIES_CURE)); });
            phase(306, () -> { consumed(null);ctx.assertTrue(!player.hasStatusEffect(ExpansionEffects.RABIES),"Consumed Rabies Cure left Rabies active");earned("rabies_cured"); drink(ModBrewing.LOCKJAW); });
            phase(340, () -> { consumed(TetanusMod.LOCKJAW);earned("lockjaw_sonata"); use(new ItemStack(ModItems.ANTITOXIN)); });
            phase(368, () -> { consumed(null);ctx.assertTrue(!player.hasStatusEffect(TetanusMod.TETANUS)&&!player.hasStatusEffect(TetanusMod.LOCKJAW),"Consumed antitoxin left Tetanus or Lockjaw active");earned("antitoxin"); use(new ItemStack(ModItems.VACCINE)); });
            phase(403, () -> { consumed(TetanusMod.VACCINATED);earned("vaccinated"); buildBeacon(); });
            phase(510, this::selectBeacon);
            phase(610, () -> { earned("beacon_full"); beginReturningNail(); });
            for (int tick=611;tick<=850;tick++) {
                final int now=tick;
                phase(tick, () -> catchReturningNail(now));
            }
        }
        private void position(Vec3d point) {
            player.teleport(ctx.getWorld(),point.x,point.y,point.z,player.getYaw(),player.getPitch());
        }
        private void phase(long tick, Runnable action) {
            ctx.runAtTick(tick, () -> {
                if (finished) return;
                try { action.run(); }
                catch (RuntimeException | Error failure) { cleanup(); throw failure; }
            });
        }
        private void earned(String id) {earned(id,ACTIONS.get(id));}
        private void earned(String id,String actualCause) {
            var advancement = player.getServer().getAdvancementLoader().get(TetanusMod.id(id));
            ctx.assertTrue(advancement != null && player.getAdvancementTracker().getProgress(advancement).isDone(), "Gameplay action failed to earn " + id + ": " + ACTIONS.get(id));
            if(completed.putIfAbsent(id, actualCause)==null)ctx.proof(id,actualCause);
        }
        private void pickup(ItemEntity item) {
            item.setPickupDelay(0);
            item.onPlayerCollision(player);
            player.playerScreenHandler.sendContentUpdates();
        }
        private void waterAndInventory() {
            earned("root");
            var iron = new ItemEntity(ctx.getWorld(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.IRON_INGOT));
            ctx.getWorld().spawnEntity(iron); pickup(iron);
            earned("iron_has_a_darker_side"); earned("recipes/unlock");
            for (int x=1;x<=5;x++) for (int z=1;z<=5;z++) for (int y=3;y<=5;y++)
                ctx.setBlockState(x,y,z, x==1||x==5||z==1||z==5 ? Blocks.STONE : y<5?Blocks.WATER:Blocks.AIR);
            player.setYaw(0);
            droppedNail = player.dropItem(new ItemStack(ModItems.NAIL), false, true);
            ctx.assertTrue(droppedNail != null, "Native nail drop failed");
            droppedNail.setPickupDelay(180); fixtures.add(droppedNail);
            for (int i=0;i<64;i++) {
                var rod=player.dropItem(new ItemStack(Items.FISHING_ROD), false, true);
                ctx.assertTrue(rod!=null,"Native fishing-rod drop failed");rod.setPickupDelay(180);fixtures.add(rod);
            }
        }
        private void prepareCreatures() {
            ctx.assertTrue(droppedNail.getStack().isOf(ModItems.RUSTED_NAIL), "A real dropped nail did not rust after five seconds in water");
            earned("rusty_business"); pickup(droppedNail);
            var hooks=ctx.getWorld().getEntitiesByClass(ItemEntity.class,new Box(ctx.getAbsolutePos(new BlockPos(1,2,1))).expand(8),item->item.getStack().isOf(ModItems.RUSTY_FISHING_HOOK));
            for(var hook:hooks)pickup(hook);
            player.playerScreenHandler.sendContentUpdates();
            ctx.assertTrue(player.getInventory().contains(new ItemStack(ModItems.RUSTY_FISHING_HOOK)), "64 real fishing-rod salvage rolls produced no collectible hook");
            earned("rusty_hook");
            player.getInventory().clear();
            position(ctx.getAbsolute(new Vec3d(7.5,3,7.5)));
            meatCowFixture=(CowEntity)spawnEgg("tetanized_cow_spawn_egg",new BlockPos(1,2,11));
            treatmentCowFixture=(CowEntity)spawnEgg("tetanized_cow_spawn_egg",new BlockPos(4,2,11));
            witherFixture=(WitherEntity)spawnEgg("tetanized_wither_spawn_egg",new BlockPos(10,2,11));
            attacker=ctx.spawnEntity(EntityType.ZOMBIE,new Vec3d(7.5,3,9.5));attacker.setAiDisabled(true);attacker.setNoGravity(true);fixtures.add(attacker);
            spearTarget=ctx.spawnEntity(EntityType.COW,new Vec3d(1.5,3.5,9.5));spearTarget.setAiDisabled(true);spearTarget.setNoGravity(true);fixtures.add(spearTarget);
        }
        private MobEntity spawnEgg(String id,BlockPos floor) {
            var egg=Registries.ITEM.get(TetanusMod.id(id));
            ctx.assertTrue(egg instanceof VariantSpawnEggItem,"Missing variant spawn egg "+id);
            var expected=((VariantSpawnEggItem)egg).entityType();
            var before=new HashSet<UUID>();
            for(var entity:ctx.getWorld().iterateEntities())before.add(entity.getUuid());
            ctx.setBlockState(floor,Blocks.STONE);
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(egg));
            var result=egg.useOnBlock(new ItemUsageContext(player,Hand.MAIN_HAND,hit(ctx.getAbsolutePos(floor))));
            var actual=new ArrayList<MobEntity>();
            // Capture native new instances before any assertion can fail. This also
            // records additional initialization spawns for reliable fixture cleanup.
            for(var entity:ctx.getWorld().iterateEntities())if(!before.contains(entity.getUuid())){
                fixtures.add(entity);
                if(entity instanceof MobEntity mob&&entity.getType()==expected)actual.add(mob);
            }
            ctx.assertTrue(result.isAccepted(),"Actual spawn egg action failed: "+id);
            ctx.assertTrue(actual.size()==1,"Spawn egg did not create exactly one new native instance: "+id+" / "+actual.size());
            var spawned=actual.get(0);
            var position=Vec3d.ofBottomCenter(ctx.getAbsolutePos(floor.up()));
            ctx.assertTrue(spawned.squaredDistanceTo(position)<0.01&&MobVariants.permanent(spawned),"Actual egg instance has the wrong location or mutation: "+id);
            return spawned;
        }
        private void worldInteractions() {
            gloveAndShield();
            golem();
            var meatCow=meatCowFixture;ctx.assertTrue(meatCow!=null&&ctx.getWorld().getEntity(meatCow.getUuid())==meatCow,"Exact egg-spawned meat cow was lost");meatCow.setAiDisabled(true);
            position(meatCow.getPos().add(0,0,-2));
            var sword=new ItemStack(Items.NETHERITE_SWORD);sword.addAttributeModifier(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE,new net.minecraft.entity.attribute.EntityAttributeModifier(UUID.fromString("1355a1a6-5c4c-4d5e-9291-35139008fef7"),"Creative advancement test sword",20000,net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADDITION),EquipmentSlot.MAINHAND);
            player.setStackInHand(Hand.MAIN_HAND,sword);player.playerTick();player.attack(meatCow);
            ctx.assertTrue(!meatCow.isAlive(),"Native player attack did not kill the infected cow");
            var meat=ctx.getWorld().getEntitiesByClass(ItemEntity.class,meatCow.getBoundingBox().expand(3),item->item.getStack().isOf(ModItems.INFECTED_MEAT));
            ctx.assertTrue(!meat.isEmpty(),"Killed Tetanized cow did not drop infected meat");for(var drop:meat)pickup(drop);earned("strange_meat");
            var treated=treatmentCowFixture;ctx.assertTrue(treated!=null&&ctx.getWorld().getEntity(treated.getUuid())==treated,"Exact egg-spawned treatment cow was lost");treated.setAiDisabled(true);position(treated.getPos().add(0,0,-1));
            var kit=new ItemStack(ExpansionItems.VETERINARY_KIT);player.setStackInHand(Hand.MAIN_HAND,kit);kit.useOnEntity(player,treated,Hand.MAIN_HAND);
            ctx.assertTrue(!MobVariants.permanent(treated),"Veterinary kit did not cure its actual egg-spawned animal");earned("clean_bill_of_health");
            var wither=witherFixture;ctx.assertTrue(wither!=null&&ctx.getWorld().getEntity(wither.getUuid())==wither,"Exact egg-spawned Wither was lost");
            wither.setAiDisabled(true);wither.setInvulTimer(0);position(wither.getPos().add(0,0,-2));player.setStackInHand(Hand.MAIN_HAND,sword);for(int cooldown=0;cooldown<10;cooldown++)player.playerTick();
            ctx.assertTrue(player.getAttackCooldownProgress(1)>0.95F,"Native test sword attack cooldown did not recover");
            ctx.assertTrue(player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE)>600,"Creative test sword attribute was not equipped");
            player.attack(wither);ctx.assertTrue(!wither.isAlive(),"Native player attack did not kill the egg-spawned Tetanized Wither");earned("nice_one");
            place(ExpansionBlocks.MEDIC_CABINET.asItem(),new BlockPos(8,2,2));earned("clinic");
            trapAndSmithing();
            tradeAndDeliver();
        }
        private void gloveAndShield() {
            ctx.assertTrue(!isDone("glove_protection"),"Glove achievement was already complete before the actual contact test");
            position(ctx.getAbsolute(new Vec3d(7.5,3,7.5)));player.setYaw(0);
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.PROTECTIVE_GLOVES));ModItems.PROTECTIVE_GLOVES.use(ctx.getWorld(),player,Hand.MAIN_HAND);
            ctx.assertTrue(GloveInventory.equipped(player).isOf(ModItems.PROTECTIVE_GLOVES),"Right-click did not equip gloves");
            attacker.equipStack(EquipmentSlot.MAINHAND,new ItemStack(ModItems.RUSTED_REBAR));
            for(int i=0;i<64&&!isDone("glove_protection");i++) {player.setHealth(player.getMaxHealth());player.timeUntilRegen=0;attacker.tryAttack(player);}
            if(isDone("infected"))earned("infected","Actual rusty zombie melee injury during the native glove-contact test.");
            earned("glove_protection");((GloveInventory)player).rustruin$gloves().clear();player.setHealth(player.getMaxHealth());player.timeUntilRegen=0;
            var shield=new ItemStack(Items.SHIELD);shield.addEnchantment(ExpansionItems.TETANUS_PROTECTION,1);player.setStackInHand(Hand.OFF_HAND,shield);player.setCurrentHand(Hand.OFF_HAND);
            var weapon=new ItemStack(Items.IRON_SWORD);weapon.addEnchantment(ModItems.TETANUS_ENCHANTMENT,2);attacker.equipStack(EquipmentSlot.MAINHAND,weapon);
            for(int i=0;i<6;i++)player.playerTick();position(ctx.getAbsolute(new Vec3d(7.5,3,7.5)));player.setYaw(0);player.timeUntilRegen=0;
            float health=player.getHealth();attacker.tryAttack(player);
            ctx.assertTrue(player.isBlocking()&&player.getHealth()==health,"Enchanted zombie attack did not meet the raised shield");earned("shield_protection");
            player.stopUsingItem();player.setStackInHand(Hand.OFF_HAND,ItemStack.EMPTY);attacker.discard();
        }
        private void golem() {
            var golem=ctx.spawnEntity(EntityType.IRON_GOLEM,new Vec3d(7.5,3,10.5));golem.setAiDisabled(true);fixtures.add(golem);position(golem.getPos().add(0,0,-2));player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUST,64));
            for(int i=0;i<64&&!MobVariants.permanent(golem);i++)UseEntityCallback.EVENT.invoker().interact(player,ctx.getWorld(),Hand.MAIN_HAND,golem,null);
            ctx.assertTrue(MobVariants.permanent(golem)&&golem.getMaxHealth()==200,"Real Rust interactions failed to produce a Tetanized Guardian");earned("tetanized_mob");earned("rusty_guardian");
        }
        private BlockHitResult hit(BlockPos floor) {return new BlockHitResult(Vec3d.ofCenter(floor).add(0,0.5,0),Direction.UP,floor,false);}
        private void place(Item item,BlockPos floor) {
            ctx.setBlockState(floor,Blocks.STONE);position(Vec3d.ofCenter(ctx.getAbsolutePos(floor)).add(0,1,-2));player.setStackInHand(Hand.MAIN_HAND,new ItemStack(item));
            var result=player.interactionManager.interactBlock(player,ctx.getWorld(),player.getMainHandStack(),Hand.MAIN_HAND,hit(ctx.getAbsolutePos(floor)));
            ctx.assertTrue(result.isAccepted(),"Native block placement failed for "+Registries.ITEM.getId(item));
        }
        private void trapAndSmithing() {
            var floor=new BlockPos(8,2,6);place(ExpansionBlocks.BEAR_TRAP.asItem(),floor);var pos=ctx.getAbsolutePos(floor.up());
            var cow=ctx.spawnEntity(EntityType.COW,new Vec3d(7,3,6.5));cow.setAiDisabled(true);fixtures.add(cow);cow.move(MovementType.SELF,new Vec3d(1.5,0,0));
            var trap=(BearTrapBlockEntity)ctx.getWorld().getBlockEntity(pos);ctx.assertTrue(trap!=null&&trap.holds(cow),"A native walking cow did not activate the placed trap");
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ExpansionItems.TRAP_RESET_TOOL));player.interactionManager.interactBlock(player,ctx.getWorld(),player.getMainHandStack(),Hand.MAIN_HAND,hit(pos));earned("trap_reset");
            var menu=new SmithingScreenHandler(1,player.getInventory());menu.getSlot(1).setStack(new ItemStack(Items.ARROW,64));menu.getSlot(2).setStack(new ItemStack(ModItems.RUST));
            ctx.assertTrue(menu.getSlot(3).getStack().getCount()==64,"Smithing output did not offer a 64-arrow batch");menu.onSlotClick(3,0,SlotActionType.QUICK_MOVE,player);
            ctx.assertTrue(player.getInventory().count(ModItems.TETANIZED_ARROW)==64&&menu.getSlot(1).getStack().isEmpty()&&menu.getSlot(2).getStack().isEmpty(),"Native smithing extraction lost inputs/output");earned("rust_arrows");
        }
        private void tradeAndDeliver() {
            medic=ctx.spawnEntity(EntityType.VILLAGER,new Vec3d(8.5,3,9.5));medic.setAiDisabled(true);medic.setVillagerData(medic.getVillagerData().withProfession(MedicVillage.MEDIC));medic.setExperience(1);fixtures.add(medic);
            position(medic.getPos().add(0,0,-1));player.setSneaking(false);player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);medic.interactMob(player,Hand.MAIN_HAND);
            ctx.assertTrue(player.currentScreenHandler instanceof MerchantScreenHandler,"Native medic interaction did not open a merchant screen");
            var menu=(MerchantScreenHandler)player.currentScreenHandler;var offer=medic.getOffers().get(0);menu.setRecipeIndex(0);menu.getSlot(0).setStack(offer.getAdjustedFirstBuyItem().copy());menu.getSlot(1).setStack(offer.getSecondBuyItem().copy());
            ctx.assertTrue(!menu.getSlot(2).getStack().isEmpty(),"Generated medic barter did not produce an output");menu.onSlotClick(2,0,SlotActionType.QUICK_MOVE,player);
            ctx.assertTrue(offer.getUses()==1,"Native medic result extraction did not consume stock");earned("medic");player.closeHandledScreen();medic.setCustomer(null);
            player.setSneaking(true);player.setStackInHand(Hand.MAIN_HAND,MedicSupplies.request(medic).copy());UseEntityCallback.EVENT.invoker().interact(player,ctx.getWorld(),Hand.MAIN_HAND,medic,null);player.setSneaking(false);earned("supply_run");
        }
        private void spear() {
            position(ctx.getAbsolute(new Vec3d(1.5,3,6.5)));player.setYaw(0);player.setPitch(0);player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ExpansionItems.SPEAR));
            ExpansionItems.SPEAR.use(ctx.getWorld(),player,Hand.MAIN_HAND);ctx.assertTrue(spearTarget.getHealth()<10,"Spear thrust missed its live indexed target");earned("spear");
        }
        private void use(ItemStack stack) {
            player.stopUsingItem();player.setStackInHand(Hand.MAIN_HAND,stack);
            consuming=stack.getItem();usageBefore=player.getStatHandler().getStat(net.minecraft.stat.Stats.USED.getOrCreateStat(consuming));
            player.teleport(ctx.getWorld(),player.getX(),player.getY(),player.getZ(),180,-30);
            ctx.requestDose(stack,usageBefore);
        }
        private void consumed(net.minecraft.entity.effect.StatusEffect expected) {
            ctx.assertTrue(consuming!=null&&!player.isUsingItem(),"Native consumption timer did not complete: item="+(consuming==null?"NONE":Registries.ITEM.getId(consuming))+", active="+player.isUsingItem()+", remaining="+player.getItemUseTimeLeft()+", usedDelta="+(consuming==null?-1:player.getStatHandler().getStat(net.minecraft.stat.Stats.USED.getOrCreateStat(consuming))-usageBefore));
            var remainder=player.getMainHandStack();
            ctx.assertTrue(consuming.isFood()?remainder.isEmpty():remainder.isOf(Items.GLASS_BOTTLE)&&remainder.getCount()==1,"Consumed item has the wrong count or bottle remainder: "+Registries.ITEM.getId(consuming)+" -> "+remainder+", heldNbt="+remainder.getNbt()+", heldPotion="+Registries.POTION.getId(PotionUtil.getPotion(remainder))+", usedDelta="+(player.getStatHandler().getStat(net.minecraft.stat.Stats.USED.getOrCreateStat(consuming))-usageBefore)+", active="+player.isUsingItem()+", remaining="+player.getItemUseTimeLeft());
            int delta=player.getStatHandler().getStat(net.minecraft.stat.Stats.USED.getOrCreateStat(consuming))-usageBefore;
            ctx.assertTrue(delta==1,"Native item-use statistics do not show exactly one consumed dose: "+Registries.ITEM.getId(consuming)+" / "+delta);
            if(expected!=null)ctx.assertTrue(player.hasStatusEffect(expected),"Consumed dose failed to apply "+Registries.STATUS_EFFECT.getId(expected));
            var record=new LinkedHashMap<String,Object>();record.put("Item",Registries.ITEM.getId(consuming).toString());record.put("DoseCount",1);record.put("NativeUsageStatDelta",delta);record.put("Remainder",remainder.isEmpty()?"EMPTY":Registries.ITEM.getId(remainder.getItem()).toString());record.put("ExpectedEffect",expected==null?"CURE":Registries.STATUS_EFFECT.getId(expected).toString());consumptionEvidence.add(record);
            consuming=null;
        }
        private void eat(Item item) {player.getHungerManager().setFoodLevel(8);use(new ItemStack(item));}
        private void drink(Potion potion) {use(PotionUtil.setPotion(new ItemStack(Items.POTION),potion));}
        private void buildBeacon() {
            beaconPos=ctx.getAbsolutePos(new BlockPos(5,8,5));
            for(int layer=1;layer<=4;layer++)for(int x=-layer;x<=layer;x++)for(int z=-layer;z<=layer;z++)ctx.getWorld().setBlockState(beaconPos.add(x,-layer,z),ExpansionBlocks.RUSTED_IRON_BLOCK.getDefaultState());
            for(int y=beaconPos.getY();y<ctx.getWorld().getTopY();y++)ctx.getWorld().setBlockState(new BlockPos(beaconPos.getX(),y,beaconPos.getZ()),Blocks.AIR.getDefaultState());
            position(Vec3d.ofCenter(beaconPos).add(0,1,-2));player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModBeacons.BLOCK));
            var below=beaconPos.down();var result=player.interactionManager.interactBlock(player,ctx.getWorld(),player.getMainHandStack(),Hand.MAIN_HAND,hit(below));ctx.assertTrue(result.isAccepted()&&ctx.getWorld().getBlockState(beaconPos).isOf(ModBeacons.BLOCK),"Native Tetanized Beacon placement failed");earned("beacon_placed");
        }
        private void selectBeacon() {
            player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);player.interactionManager.interactBlock(player,ctx.getWorld(),player.getMainHandStack(),Hand.MAIN_HAND,hit(beaconPos));
            ctx.assertTrue(player.currentScreenHandler instanceof TetanizedBeaconScreenHandler,"Real placed beacon did not open its native menu");
            var menu=(TetanizedBeaconScreenHandler)player.currentScreenHandler;menu.getSlot(0).setStack(new ItemStack(Items.NETHERITE_INGOT));menu.setEffects(Optional.of(StatusEffects.HASTE),Optional.of(StatusEffects.HASTE));ctx.assertTrue(menu.getSlot(0).getStack().isEmpty(),"Full beacon selection did not accept/consume Netherite");player.closeHandledScreen();
        }
        private void beginReturningNail() {
            ctx.assertTrue(player.hasStatusEffect(StatusEffects.HASTE)&&player.getStatusEffect(StatusEffects.HASTE).getAmplifier()==2,"Native full beacon failed to provide Haste III");
            for(String id:ACTIONS.keySet())if(!id.equals("why_did_i_do_that"))earned(id);
            ctx.getWorld().getServer().setPvpEnabled(true);pvpChanged=true;
            position(Vec3d.ofCenter(beaconPos).add(0,2,0));player.setHealth(1);player.timeUntilRegen=0;player.setPitch(-90);player.setYaw(0);player.setVelocity(Vec3d.ZERO);
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUSTED_NAIL,16));
            ModItems.RUSTED_NAIL.use(ctx.getWorld(),player,Hand.MAIN_HAND);
        }
        private void catchReturningNail(int tick) {
            if(!player.isAlive()) {earned("why_did_i_do_that");finish();return;}
            if(returningNail==null||!returningNail.isAlive())returningNail=ctx.getWorld().getEntitiesByClass(NailEntity.class,player.getBoundingBox().expand(80),nail->nail.getOwner()==player&&nail.wasThrownUpward()).stream().findFirst().orElse(null);
            if(returningNail!=null&&returningNail.getVelocity().y<0) {
                // Creative fixture positioning catches the naturally returning projectile;
                // no projectile damage/collision method is invoked by the test.
                position(new Vec3d(returningNail.getX(),beaconPos.getY()+2.5,returningNail.getZ()));player.setVelocity(Vec3d.ZERO);
            }
            if((returningNail==null||!returningNail.isAlive())&&tick%20==0) {player.setPitch(-90);ModItems.RUSTED_NAIL.use(ctx.getWorld(),player,Hand.MAIN_HAND);}
            if(tick==850)ctx.assertTrue(false,"Actual upward rusty nails never returned to kill their owner");
        }
        private boolean isDone(String id) {
            var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id(id));return advancement!=null&&player.getAdvancementTracker().getProgress(advancement).isDone();
        }
        private void finish() {
            for(String id:ACTIONS.keySet())earned(id);
            ctx.assertTrue(completed.size()==28,"Expected all 28 active mod advancements, found "+completed.size());
            var report=new LinkedHashMap<String,Object>();report.put("Result","PASS");report.put("ConnectedPlayers",1);report.put("ActiveAdvancements",28);report.put("Completed",completed);report.put("DirectCriterionGrants",0);report.put("NativeConsumptions",consumptionEvidence);report.put("Retired",Map.of("variant_egg","Creative-only Bad Egg removed at the owner's request; hidden ID preserved solely for old saves. Actual variant eggs were still exercised."));
            try {Files.writeString(Path.of("graphical-advancement-server-coverage-"+ctx.viewer+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}
            catch(java.io.IOException failure){throw new IllegalStateException("Cannot save exhaustive advancement evidence",failure);}
            cleanup();ctx.complete();
        }
        private void cleanup() {
            if(finished)return;finished=true;ctx.restorePhysics();
            if(pvpChanged)ctx.getWorld().getServer().setPvpEnabled(originalPvp);
            if(medic!=null)medic.setCustomer(null);
            player.closeHandledScreen();
            fixtures.forEach(Entity::discard);
            if(returningNail!=null)returningNail.discard();
        }
    }

}
