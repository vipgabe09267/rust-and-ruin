package com.tetanusmod.test;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.compat.RustRuinJeiPlugin;
import com.tetanusmod.effect.Infection;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.registry.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.world.GameMode;
import java.nio.file.*;

public final class ClientSmokeTest implements ClientModInitializer {
    private int ticks,worldTicks;
    private boolean prepared,finished;
    private int recipesShownAt;
    private volatile boolean serverReady;
    private volatile String serverFailure;
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            client.options.pauseOnLostFocus=false;
            if(finished)return;
            if(serverFailure!=null){finish(client,"FAIL: "+serverFailure);return;}
            ticks++;
            if(ticks>1800) { finish(client,"FAIL: client/world/JEI startup timed out"); return; }
            if(client.player==null||client.world==null||client.getServer()==null)return;
            if(!prepared) {
                prepared=true;
                client.getServer().execute(()->{
                    try {
                    var server=client.getServer(); var world=server.getOverworld();
                    var cleanup=new java.util.ArrayList<Entity>();
                    for(var entity:world.iterateEntities())if(!(entity instanceof net.minecraft.entity.player.PlayerEntity))cleanup.add(entity);
                    cleanup.forEach(Entity::discard);
                    var player=server.getPlayerManager().getPlayerList().get(0);
                    for(int x=-10;x<=10;x++)for(int z=-10;z<=40;z++)world.setBlockState(new BlockPos(x,3,z),Blocks.STONE.getDefaultState());
                    player.changeGameMode(GameMode.SURVIVAL); player.teleport(world,0.5,4,0.5,0,8);
                    player.setHealth(18);player.getInventory().clear();
                    int slot=0;for(var item:ModItems.ALL)player.getInventory().setStack(slot++,new ItemStack(item));
                    player.equipStack(EquipmentSlot.MAINHAND,new ItemStack(ModItems.RUSTED_REBAR));
                    Infection.apply(player,null);
                    player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(TetanusMod.SALMONELLA,1200));
                    world.setBlockState(new BlockPos(1,4,4),ModBlocks.TETANIZED_SOUL_SAND.getDefaultState());
                    world.setTimeOfDay(6000);
                    var zombie=new ZombieEntity(EntityType.ZOMBIE,world);zombie.setAiDisabled(true);zombie.equipStack(EquipmentSlot.HEAD,new ItemStack(net.minecraft.item.Items.LEATHER_HELMET));zombie.setPosition(-2,4,5);MobVariants.makePermanent(zombie);world.spawnEntity(zombie);
                    var cow=new CowEntity(EntityType.COW,world);cow.setAiDisabled(true);cow.setPosition(2,4,5);MobVariants.makePermanent(cow);world.spawnEntity(cow);
                    var wither=new WitherEntity(EntityType.WITHER,world);wither.setAiDisabled(true);wither.setPosition(0,6,24);MobVariants.makePermanent(wither);world.spawnEntity(wither);
                    serverReady=true;
                    } catch(Throwable failure) { serverFailure="Smoke world setup failed: "+failure; }
                });
            }
            if(!serverReady)return;
            worldTicks++;
            if(worldTicks==100)ScreenshotRecorder.saveScreenshot(client.runDirectory,"rust-and-ruin-hud.png",client.getFramebuffer(),text->{});
            if(worldTicks==120)client.setScreen(new InventoryScreen(client.player));
            if(worldTicks>180&&RustRuinJeiPlugin.runtimeReady&&recipesShownAt==0) {
                ScreenshotRecorder.saveScreenshot(client.runDirectory,"rust-and-ruin-jei.png",client.getFramebuffer(),text->{});
                if(RustRuinJeiPlugin.rustingRecipeCount!=11){finish(client,"FAIL: expected 11 JEI rusting recipes, got "+RustRuinJeiPlugin.rustingRecipeCount);return;}
                recipesShownAt=worldTicks;RustRuinJeiPlugin.showRustingRecipes();
            }
            if(recipesShownAt>0&&worldTicks>recipesShownAt+40) {
                ScreenshotRecorder.saveScreenshot(client.runDirectory,"rust-and-ruin-rusting-jei.png",client.getFramebuffer(),text->{});
                finish(client,"PASS: integrated world, entity renderers, rusted HUD, item textures, JEI runtime and 11 rusting recipes displayed");
            }
        });
    }
    private void finish(MinecraftClient client,String result) {
        finished=true;
        try { Files.writeString(Path.of("client-smoke-result.txt"),result+System.lineSeparator()); }
        catch(Exception e) { throw new RuntimeException(e); }
        org.slf4j.LoggerFactory.getLogger("tetanusmod").info(result);
        client.scheduleStop();
    }
}
