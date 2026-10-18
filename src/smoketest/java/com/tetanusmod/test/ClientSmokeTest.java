package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.beacon.*;
import com.tetanusmod.compat.*;
import com.tetanusmod.effect.Infection;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.registry.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.item.*;
import net.minecraft.potion.PotionUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.world.GameMode;
import java.nio.file.*;

public final class ClientSmokeTest implements ClientModInitializer {
    private int ticks,worldTicks,recipesAt;
    private boolean prepared,finished,caltropsOpened,beaconOpened;
    private volatile boolean serverReady;
    private volatile String serverFailure;
    private final boolean rei=FabricLoader.getInstance().isModLoaded("roughlyenoughitems");
    private final BlockPos beaconPos=new BlockPos(3,4,10);
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            client.options.pauseOnLostFocus=false;
            if(finished)return;
            try {
                if(serverFailure!=null){finish(client,"FAIL: "+serverFailure);return;}
                if(++ticks>2400){finish(client,"FAIL: client/world/viewer startup timed out");return;}
                if(client.player==null||client.world==null||client.getServer()==null)return;
                if(!prepared){prepared=true;client.getServer().execute(()->prepare(client));}
                if(!serverReady)return;
                worldTicks++;
                if(worldTicks==90)client.getServer().execute(()->{
                    var player=client.getServer().getPlayerManager().getPlayerList().get(0);
                    var state=(com.tetanusmod.effect.InfectionState)player;
                    int total=state.tetanus$getTotal();state.tetanus$setInfection((int)((total*2L+2)/3),total);
                });
                if(worldTicks==110){client.getToastManager().clear();client.inGameHud.getChatHud().clear(false);}
                if(worldTicks==120){
                    if(!client.player.hasStatusEffect(TetanusMod.LOCKJAW))throw new IllegalStateException("Lockjaw icon/status did not synchronize");
                    if(!com.tetanusmod.client.InfectedPlayerList.contains(client.player.getUuid()))throw new IllegalStateException("Player infection flags did not synchronize");
                    screenshot(client,"hud");client.options.playerListKey.setPressed(true);
                }
                if(worldTicks==130)screenshot(client,"tab-hearts");
                if(worldTicks==135)client.options.playerListKey.setPressed(false);
                if(worldTicks==140)client.setScreen(new InventoryScreen(client.player));
                boolean ready=rei?RustRuinReiPlugin.registeredCount==11:RustRuinJeiPlugin.runtimeReady;
                if(worldTicks>220&&ready&&recipesAt==0) {
                    screenshot(client,"inventory");
                    if(!(rei?RustRuinReiPlugin.hasDiseaseBrewing():RustRuinJeiPlugin.hasDiseaseBrewing()))throw new IllegalStateException("Disease brewing recipes missing from viewer");
                    if(rei) { if(!RustRuinReiPlugin.showRusting())throw new IllegalStateException("REI Rusting screen unavailable"); }
                    else { if(RustRuinJeiPlugin.rustingRecipeCount!=11||!RustRuinJeiPlugin.hasCaltropRecipe())throw new IllegalStateException("JEI recipe registration missing");RustRuinJeiPlugin.showRustingRecipes(); }
                    recipesAt=worldTicks;
                }
                if(recipesAt>0&&worldTicks==recipesAt+40) {
                    screenshot(client,"rusting");
                    if(rei) { if(!RustRuinReiPlugin.showCaltrops())throw new IllegalStateException("REI caltrops recipe unavailable"); }
                    else RustRuinJeiPlugin.showCaltropRecipe();
                    caltropsOpened=true;
                }
                if(caltropsOpened&&worldTicks==recipesAt+80) {
                    screenshot(client,"caltrops");client.setScreen(null);
                    client.getServer().execute(()->{
                        var server=client.getServer();var player=server.getPlayerManager().getPlayerList().get(0);
                        player.teleport(server.getOverworld(),3.5,4,8.5,0,0);
                        player.openHandledScreen((TetanizedBeaconEntity)server.getOverworld().getBlockEntity(beaconPos));
                    });
                    beaconOpened=true;
                }
                if(beaconOpened&&worldTicks>recipesAt+110) {
                    if(!(client.currentScreen instanceof com.tetanusmod.client.TetanizedBeaconScreen))throw new IllegalStateException("Custom beacon screen failed to open");
                    screenshot(client,"beacon");
                    finish(client,"PASS: "+(rei?"REI":"JEI")+" Rusting (11 recipes), caltrop crafting, corrected item sprites, species skins, potions, and custom beacon screen rendered");
                }
            }catch(Throwable failure){finish(client,"FAIL: "+failure);}
        });
    }
    private void prepare(MinecraftClient client) {
        try {
            var server=client.getServer();var world=server.getOverworld();
            var cleanup=new java.util.ArrayList<Entity>();for(var e:world.iterateEntities())if(!(e instanceof net.minecraft.entity.player.PlayerEntity))cleanup.add(e);cleanup.forEach(Entity::discard);
            var player=server.getPlayerManager().getPlayerList().get(0);
            for(int x=-10;x<=10;x++)for(int z=-10;z<=40;z++)world.setBlockState(new BlockPos(x,3,z),Blocks.STONE.getDefaultState());
            var board=server.getScoreboard();var objective=board.getNullableObjective("rust_health");
            if(objective==null)objective=board.addObjective("rust_health",net.minecraft.scoreboard.ScoreboardCriterion.HEALTH,net.minecraft.text.Text.literal("Health"),net.minecraft.scoreboard.ScoreboardCriterion.RenderType.HEARTS);
            board.setObjectiveSlot(0,objective);board.getPlayerScore(player.getEntityName(),objective).setScore(20);
            player.changeGameMode(GameMode.SURVIVAL);player.teleport(world,0.5,4,0.5,0,8);player.setHealth(18);player.getInventory().clear();
            int slot=0;for(var item:ModItems.ALL)if(slot<36)player.getInventory().setStack(slot++,new ItemStack(item));
            player.getInventory().setStack(32,PotionUtil.setPotion(new ItemStack(Items.POTION),ModBrewing.MANURE));
            player.getInventory().setStack(33,PotionUtil.setPotion(new ItemStack(Items.POTION),ModBrewing.TETANUS));
            player.getInventory().setStack(34,PotionUtil.setPotion(new ItemStack(Items.SPLASH_POTION),ModBrewing.SALMONELLA));
            player.equipStack(EquipmentSlot.MAINHAND,new ItemStack(ModItems.RUSTED_REBAR));Infection.apply(player,null);
            player.addStatusEffect(new StatusEffectInstance(TetanusMod.SALMONELLA,1200));world.setTimeOfDay(6000);
            EntityType<?>[] types={EntityType.COW,EntityType.PIG,EntityType.SHEEP,EntityType.SPIDER,EntityType.IRON_GOLEM,EntityType.ZOMBIE};
            double[] xs={-3,-1.5,1,3.5,5,6.5};
            for(int i=0;i<types.length;i++) {
                var mob=(net.minecraft.entity.mob.MobEntity)types[i].create(world);mob.setAiDisabled(true);mob.setPosition(xs[i],4,6);
                if(mob instanceof net.minecraft.entity.mob.ZombieEntity)mob.equipStack(EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));
                MobVariants.makePermanent(mob);world.spawnEntity(mob);
            }
            var wither=EntityType.WITHER.create(world);wither.setAiDisabled(true);wither.setPosition(0,7,24);MobVariants.makePermanent(wither);world.spawnEntity(wither);
            for(int layer=1;layer<=4;layer++)for(int x=-layer;x<=layer;x++)for(int z=-layer;z<=layer;z++)world.setBlockState(beaconPos.add(x,-layer,z),Blocks.COPPER_BLOCK.getDefaultState());
            world.setBlockState(beaconPos,ModBeacons.BLOCK.getDefaultState());
            var beacon=(TetanizedBeaconEntity)world.getBlockEntity(beaconPos);var nbt=beacon.createNbt();
            nbt.putInt("Primary",net.minecraft.registry.Registries.STATUS_EFFECT.getRawId(StatusEffects.HASTE));nbt.putInt("Secondary",net.minecraft.registry.Registries.STATUS_EFFECT.getRawId(StatusEffects.HASTE));beacon.readNbt(nbt);
            serverReady=true;
        }catch(Throwable failure){serverFailure="Smoke world setup failed: "+failure;}
    }
    private void screenshot(MinecraftClient client,String name) { ScreenshotRecorder.saveScreenshot(client.runDirectory,"rust-and-ruin-"+(rei?"rei":"jei")+"-"+name+".png",client.getFramebuffer(),text->{}); }
    private void finish(MinecraftClient client,String result) {
        finished=true;
        try{Files.writeString(Path.of("client-smoke-result-"+(rei?"rei":"jei")+".txt"),result+System.lineSeparator());}catch(Exception e){throw new RuntimeException(e);}
        org.slf4j.LoggerFactory.getLogger("tetanusmod").info(result);client.scheduleStop();
    }
}