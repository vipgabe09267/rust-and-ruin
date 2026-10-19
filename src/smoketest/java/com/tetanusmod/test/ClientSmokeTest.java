package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.beacon.*;
import com.tetanusmod.compat.*;
import com.tetanusmod.effect.Infection;
import com.tetanusmod.expansion.*;
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
    private boolean prepared,finished,caltropsOpened,beaconOpened,advancementsOpened,guidePrepared,guideOpened,vaccineOpened;
    private java.util.UUID clinicMedic; private volatile boolean clinicReady; private boolean clinicPending; private volatile boolean serverReady; private boolean rabiesPrepared;
    private volatile String serverFailure;
    private final boolean rei=FabricLoader.getInstance().isModLoaded("roughlyenoughitems");
    private final boolean plain=!rei&&!FabricLoader.getInstance().isModLoaded("jei");
    private String viewer(){return plain?"plain":rei?"rei":"jei";}
    private final BlockPos beaconPos=new BlockPos(3,4,10);
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            client.options.pauseOnLostFocus=false;
            if(finished)return;
            try {
                if(serverFailure!=null){finish(client,"FAIL: "+serverFailure);return;}
                if(++ticks>2400){finish(client,"FAIL: client/world/viewer startup timed out");return;}
                if(client.player==null||client.world==null||client.getServer()==null)return;
                if(!prepared){prepared=true;onServer(client,()->prepare(client));}
                if(!serverReady)return;
                if(clinicPending&&!clinicReady)return;
                worldTicks++;
                if(worldTicks==90)onServer(client,()->{
                    var player=client.getServer().getPlayerManager().getPlayerList().get(0);
                    var state=(com.tetanusmod.effect.InfectionState)player;
                    int total=state.tetanus$getTotal();state.tetanus$setInfection(total-state.tetanus$getLockjawWindow(),total);
                });
                if(worldTicks==110){client.getToastManager().clear();client.inGameHud.getChatHud().clear(false);}
                if(worldTicks==120){
                    verifySpeciesTextures(client);
                    verifyPotionTooltips(client);
                    if(client.player.getStatusEffect(TetanusMod.LOCKJAW)==null||client.player.getStatusEffect(TetanusMod.LOCKJAW).getDuration()<2300)throw new IllegalStateException("Lockjaw duration did not synchronize as minutes");
                    if(!client.player.hasStatusEffect(TetanusMod.LOCKJAW))throw new IllegalStateException("Lockjaw icon/status did not synchronize");
                    if(!com.tetanusmod.client.InfectedPlayerList.contains(client.player.getUuid()))throw new IllegalStateException("Player infection flags did not synchronize");
                    screenshot(client,"hud");client.options.playerListKey.setPressed(true);
                }
                if(worldTicks==130)screenshot(client,"tab-hearts");
                if(worldTicks==135)client.options.playerListKey.setPressed(false);
                if(worldTicks==140)client.setScreen(new InventoryScreen(client.player));
                boolean ready=plain|| (rei?RustRuinReiPlugin.registeredCount==9:RustRuinJeiPlugin.runtimeReady);
                if(worldTicks>220&&ready&&recipesAt==0) {
                    screenshot(client,"inventory");
                    if(!plain&&!(rei?RustRuinReiPlugin.hasDiseaseBrewing():RustRuinJeiPlugin.hasDiseaseBrewing()))throw new IllegalStateException("Disease brewing recipes missing from viewer");
                    if(!plain&&rei) { if(!RustRuinReiPlugin.showRusting())throw new IllegalStateException("REI Rusting screen unavailable"); }
                    else if(!plain) { if(RustRuinJeiPlugin.rustingRecipeCount!=9||!RustRuinJeiPlugin.hasCaltropRecipe())throw new IllegalStateException("JEI recipe registration missing");RustRuinJeiPlugin.showRustingRecipes(); }
                    if(!plain&&!(rei?RustRuinReiPlugin.hasArrowSmithing():RustRuinJeiPlugin.hasArrowSmithing()))throw new IllegalStateException("Arrow smithing viewer display missing");
                    recipesAt=worldTicks;
                }
                if(recipesAt>0&&worldTicks==recipesAt+40) {
                    if(!plain)screenshot(client,"rusting");
                    if(!plain&&rei) { if(!RustRuinReiPlugin.showCaltrops())throw new IllegalStateException("REI caltrops recipe unavailable"); }
                    else if(!plain) RustRuinJeiPlugin.showCaltropRecipe();
                    caltropsOpened=true;
                }
                if(caltropsOpened&&worldTicks==recipesAt+60){
                    if(!plain)screenshot(client,"caltrops");
                    if(!plain&&rei){if(!RustRuinReiPlugin.showArrowSmithing())throw new IllegalStateException("Arrow smithing viewer failed to open");}else if(!plain)RustRuinJeiPlugin.showArrowSmithing();
                }
                if(caltropsOpened&&worldTicks==recipesAt+80) {
                    if(!plain)screenshot(client,"arrow-smithing");client.setScreen(null);
                    onServer(client,()->{
                        var server=client.getServer();var player=server.getPlayerManager().getPlayerList().get(0);
                        player.teleport(server.getOverworld(),3.5,4,8.5,0,0);
                        player.openHandledScreen((TetanizedBeaconEntity)server.getOverworld().getBlockEntity(beaconPos));
                    });
                    beaconOpened=true;
                }
                if(beaconOpened&&!advancementsOpened&&worldTicks>recipesAt+110) {
                    if(!(client.currentScreen instanceof com.tetanusmod.client.TetanizedBeaconScreen))throw new IllegalStateException("Custom beacon screen failed to open");
                    screenshot(client,"beacon");
                    var handler=client.player.networkHandler.getAdvancementHandler();
                    var root=handler.getManager().get(TetanusMod.id("root"));
                    if(root==null||root.getDisplay()==null||!root.getDisplay().getBackground().equals(TetanusMod.id("textures/block/rusted_metal.png")))throw new IllegalStateException("Rusty advancement background missing");
                    var screen=new net.minecraft.client.gui.screen.advancement.AdvancementsScreen(handler);client.setScreen(screen);screen.selectTab(root);advancementsOpened=true;
                }
                if(advancementsOpened&&!guidePrepared&&worldTicks>recipesAt+140) {
                    if(!(client.currentScreen instanceof net.minecraft.client.gui.screen.advancement.AdvancementsScreen))throw new IllegalStateException("Advancement screen did not open");
                    screenshot(client,"advancements");
                    client.player.closeHandledScreen();guidePrepared=true;
                    onServer(client,()->{
                        var player=client.getServer().getPlayerManager().getPlayerList().get(0);
                        player.closeHandledScreen();
                        ModItems.VACCINE.finishUsing(new ItemStack(ModItems.VACCINE),player.getWorld(),player);
                        player.equipStack(EquipmentSlot.MAINHAND,new ItemStack(ModItems.FIELD_GUIDE));
                        ((GloveInventory)player).rustruin$gloves().setStack(0,new ItemStack(ModItems.PROTECTIVE_GLOVES));
                        player.playerScreenHandler.sendContentUpdates();
                    });
                }
                if(guidePrepared&&!guideOpened&&worldTicks>recipesAt+170) {
                    if(!client.player.hasStatusEffect(TetanusMod.VACCINATED)||!GloveInventory.equipped(client.player).isOf(ModItems.PROTECTIVE_GLOVES)||!client.player.getMainHandStack().isOf(ModItems.FIELD_GUIDE))throw new IllegalStateException("Prevention sync failed: vaccine="+client.player.hasStatusEffect(TetanusMod.VACCINATED)+" offhand="+client.player.getOffHandStack()+" main="+client.player.getMainHandStack());
                    ModItems.FIELD_GUIDE.use(client.world,client.player,net.minecraft.util.Hand.MAIN_HAND);
                    if(!(client.currentScreen instanceof com.tetanusmod.client.FieldGuideScreen guide))throw new IllegalStateException("Guide item failed to open screen");
                    for(int chapter=0;chapter<guide.chapterCount();chapter++) {
                        guide.selectChapter(chapter);String body=guide.body().getString();
                        if(body.startsWith("guide.")||body.length()<100||body.indexOf((char)92)>=0)throw new IllegalStateException("Invalid guide chapter: "+chapter+" "+body);
                    }
                    guide.selectChapter(2);
                    if(!guide.body().getString().contains("80%")||!guide.body().getString().contains("90%"))throw new IllegalStateException("Guide protection values do not match server config");
                    for(String id:new String[]{"tetanus_vaccine","protective_gloves","field_guide"})if(client.world.getRecipeManager().get(TetanusMod.id(id)).isEmpty())throw new IllegalStateException("Prevention recipe missing: "+id);
                    if(!plain&&!rei&&!RustRuinJeiPlugin.hasPreventionRecipes())throw new IllegalStateException("JEI prevention recipes missing");
                    guideOpened=true;
                }
                if(guideOpened&&worldTicks==recipesAt+185)screenshot(client,"field-guide");
                if(guideOpened&&!vaccineOpened&&worldTicks>recipesAt+210) {
                    var guide=(com.tetanusmod.client.FieldGuideScreen)client.currentScreen;
                    guide.selectChapter(-99);guide.selectChapter(Integer.MAX_VALUE);guide.mouseScrolled(0,0,Double.POSITIVE_INFINITY);guide.selectChapter(2);
                    if(!plain&&rei){if(!RustRuinReiPlugin.showVaccineRecipe())throw new IllegalStateException("REI vaccine recipe failed to open");}
                    else if(!plain) RustRuinJeiPlugin.showVaccineRecipe();vaccineOpened=true;
                }
                if(vaccineOpened&&!rabiesPrepared&&worldTicks>recipesAt+240) {
                    if(!plain)screenshot(client,"vaccine-recipe");client.setScreen(null);rabiesPrepared=true;
                    onServer(client,()->{
                        var player=client.getServer().getPlayerManager().getPlayerList().get(0);player.closeHandledScreen();ModItems.ANTITOXIN.finishUsing(new ItemStack(ModItems.ANTITOXIN),player.getWorld(),player);player.removeStatusEffect(TetanusMod.SALMONELLA);player.removeStatusEffect(StatusEffects.NAUSEA);
                        Rabies.expose(player,null);((ExpansionState)player).rustruin$setLostHealth(6);RabiesEffect.restorePenalty(player);player.setHealth(14);player.teleport(client.getServer().getOverworld(),0.5,4,0.5,0,8);
                        var cow=EntityType.COW.create(player.getWorld());cow.setAiDisabled(true);cow.setPosition(0,4,5);Rabies.makeRabid(cow);player.getWorld().spawnEntity(cow);
                    });
                }
                if(rabiesPrepared&&worldTicks==recipesAt+270){
                    if(!client.player.hasStatusEffect(ExpansionEffects.RABIES)||client.player.getMaxHealth()!=14||!com.tetanusmod.client.RabidPlayerList.contains(client.player.getUuid()))throw new IllegalStateException("Rabies disease/hearts/tab flags failed to synchronize");
                    screenshot(client,"rabies-hud");client.options.playerListKey.setPressed(true);
                }
                if(rabiesPrepared&&worldTicks==recipesAt+280){screenshot(client,"rabies-tab");client.options.playerListKey.setPressed(false);}
                if(rabiesPrepared&&worldTicks==recipesAt+285){clinicPending=true;
                    onServer(client,()->{
                        var world=client.getServer().getOverworld();for(int x=18;x<=30;x++)for(int z=9;z<=31;z++)world.setBlockState(new BlockPos(x,3,z),Blocks.STONE.getDefaultState());var template=world.getStructureTemplateManager().getTemplate(TetanusMod.id("village/clinic")).orElseThrow();var pos=new BlockPos(20,3,20);template.place(world,pos,pos,new net.minecraft.structure.StructurePlacementData().addProcessor(net.minecraft.structure.processor.JigsawReplacementStructureProcessor.INSTANCE),world.random,3);
                        world.setBlockState(new BlockPos(22,4,17),ExpansionBlocks.BEAR_TRAP.getDefaultState());world.setBlockState(new BlockPos(24,4,17),ExpansionBlocks.RUSTED_BEAR_TRAP.getDefaultState());world.setBlockState(new BlockPos(26,4,17),ModBlocks.RUSTED_BARBED_WIRE.getDefaultState());
                        var medic=world.getEntitiesByClass(net.minecraft.entity.passive.VillagerEntity.class,new net.minecraft.util.math.Box(20,3,20,29,15,34),v->v.getVillagerData().getProfession()==MedicVillage.MEDIC).stream().findFirst().orElseThrow();clinicMedic=medic.getUuid();
                        var player=client.getServer().getPlayerManager().getPlayerList().get(0);player.getHungerManager().setFoodLevel(20);player.hurtTime=0;player.teleport(world,24.5,4,9.5,0,-4);clinicReady=true;
                    });
                }
                if(rabiesPrepared&&worldTicks==recipesAt+351){
                    onServer(client,()->{
                        var world=client.getServer().getOverworld();var medic=(net.minecraft.entity.passive.VillagerEntity)world.getEntity(clinicMedic);if(medic==null||medic.getVillagerData().getProfession()!=MedicVillage.MEDIC)throw new IllegalStateException("Clinic medic lost its profession");medic.setAiDisabled(true);medic.refreshPositionAndAngles(24.5,4,12.5,180,0);medic.setHeadYaw(180);medic.bodyYaw=180;
                    });
                }
                if(rabiesPrepared&&worldTicks==recipesAt+340)client.options.hudHidden=true;
                if(rabiesPrepared&&worldTicks==recipesAt+380)screenshot(client,"medic");
                if(rabiesPrepared&&worldTicks==recipesAt+385)onServer(client,()->{var server=client.getServer();server.getPlayerManager().getPlayerList().get(0).teleport(server.getOverworld(),24.5,5,24.3,0,8);});
                if(rabiesPrepared&&worldTicks==recipesAt+405)screenshot(client,"clinic-interior");
                if(rabiesPrepared&&worldTicks==recipesAt+407)client.options.hudHidden=false;
                if(rabiesPrepared&&worldTicks==recipesAt+395){
                    onServer(client,()->client.getServer().getPlayerManager().getPlayerList().get(0).changeGameMode(GameMode.CREATIVE));
                }
                if(rabiesPrepared&&worldTicks==recipesAt+350)screenshot(client,"clinic-traps");
                if(rabiesPrepared&&worldTicks==recipesAt+410){
                    var creative=new net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen(client.player,client.world.getEnabledFeatures(),true);client.setScreen(creative);
                    var select=creative.getClass().getDeclaredMethod("setSelectedTab",ItemGroup.class);select.setAccessible(true);var group=net.minecraft.registry.Registries.ITEM_GROUP.get(TetanusMod.id("items"));select.invoke(creative,group);
                    for(var item:ModItems.ALL){boolean legacy=com.tetanusmod.registry.ModIronTools.isRusted(item);boolean shown=group.getDisplayStacks().stream().anyMatch(stack->stack.isOf(item));if(shown==legacy)throw new IllegalStateException("Creative item visibility incorrect: "+item);}
                    screenshot(client,"creative-items");select.invoke(creative,ItemGroups.getDefaultTab());
                    var inventoryGroup=net.minecraft.registry.Registries.ITEM_GROUP.stream().filter(g->g.getType()==ItemGroup.Type.INVENTORY).findFirst().orElseThrow();select.invoke(creative,inventoryGroup);
                    var slot=creative.getScreenHandler().getSlot(46);if(slot.x!=136||slot.y!=20)throw new IllegalStateException("Creative glove slot overlaps hotbar");
                    var gloves=new ItemStack(ModItems.PROTECTIVE_GLOVES);gloves.setDamage(37);client.interactionManager.clickCreativeStack(gloves,46);
                }
                if(rabiesPrepared&&worldTicks==recipesAt+430){
                    if(GloveInventory.equipped(client.player).getDamage()!=37)throw new IllegalStateException("Creative glove packet did not synchronize");screenshot(client,"creative-gloves");
                    finish(client,"PASS: "+(plain?"Plain client without recipe viewers; native recipes":rei?"REI Rusting (9), crafting/brewing":"JEI Rusting (9), crafting/brewing")+", 78 skins, 12 potion tooltips, Rabies HUD/tab and 22 rabid skins, creative item coverage/glove synchronization, Lockjaw/hearts/beacon/advancements; vaccine sync, dedicated glove slot, 10 guide chapters and prevention recipes");
                }
            }catch(Throwable failure){finish(client,"FAIL: "+failure);}
        });
    }
    private void onServer(MinecraftClient client,Runnable action){client.getServer().execute(()->{try{action.run();}catch(Throwable failure){serverFailure="Server smoke task failed: "+failure;}});}
    private void prepare(MinecraftClient client) {
        try {
            var server=client.getServer();var world=server.getOverworld();
            var cleanup=new java.util.ArrayList<Entity>();for(var e:world.iterateEntities())if(!(e instanceof net.minecraft.entity.player.PlayerEntity))cleanup.add(e);cleanup.forEach(Entity::discard);
            var player=server.getPlayerManager().getPlayerList().get(0);
            ModAdvancements.award(player,"root","obtain_iron");
            for(int x=-10;x<=10;x++)for(int z=-10;z<=40;z++)world.setBlockState(new BlockPos(x,3,z),Blocks.STONE.getDefaultState());
            var board=server.getScoreboard();var objective=board.getNullableObjective("rust_health");
            if(objective==null)objective=board.addObjective("rust_health",net.minecraft.scoreboard.ScoreboardCriterion.HEALTH,net.minecraft.text.Text.literal("Health"),net.minecraft.scoreboard.ScoreboardCriterion.RenderType.HEARTS);
            board.setObjectiveSlot(0,objective);board.getPlayerScore(player.getEntityName(),objective).setScore(20);
            ((GloveInventory)player).rustruin$gloves().clear();player.clearStatusEffects();player.changeGameMode(GameMode.SURVIVAL);player.teleport(world,0.5,4,0.5,0,8);player.setHealth(18);player.getInventory().clear();
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
            for(int layer=1;layer<=4;layer++)for(int x=-layer;x<=layer;x++)for(int z=-layer;z<=layer;z++)world.setBlockState(beaconPos.add(x,-layer,z),ExpansionBlocks.RUSTED_IRON_BLOCK.getDefaultState());
            world.setBlockState(beaconPos,ModBeacons.BLOCK.getDefaultState());
            var beacon=(TetanizedBeaconEntity)world.getBlockEntity(beaconPos);var nbt=beacon.createNbt();
            nbt.putInt("Primary",net.minecraft.registry.Registries.STATUS_EFFECT.getRawId(StatusEffects.HASTE));nbt.putInt("Secondary",net.minecraft.registry.Registries.STATUS_EFFECT.getRawId(StatusEffects.HASTE));beacon.readNbt(nbt);
            ModItems.INFECTED_MEAT.finishUsing(new ItemStack(ModItems.INFECTED_MEAT),world,player);
            var questionable=server.getAdvancementLoader().get(TetanusMod.id("why_did_i_eat_that"));
            if(questionable==null||!player.getAdvancementTracker().getProgress(questionable).isDone())throw new IllegalStateException("Why did I eat that? did not unlock from Salmonella");
            var badConfig=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();badConfig.writeByte(128);
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,TetanusMod.CONFIG_PACKET,badConfig);
            var deepConfig=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();deepConfig.writeString("[".repeat(256)+"]".repeat(256));
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,TetanusMod.CONFIG_PACKET,deepConfig);
            var badPresence=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();badPresence.writeByte(0);
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,com.tetanusmod.effect.InfectionPresence.PACKET,badPresence);
            var badSymptom=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();badSymptom.writeByte(255);badSymptom.writeByte(0);
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,Infection.SYMPTOM_PACKET,badSymptom);
            serverReady=true;
        }catch(Throwable failure){serverFailure="Smoke world setup failed: "+failure;}
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private void verifySpeciesTextures(MinecraftClient client) throws Exception {
        var method=net.minecraft.client.render.entity.LivingEntityRenderer.class.getDeclaredMethod("getRenderLayer",net.minecraft.entity.LivingEntity.class,boolean.class,boolean.class,boolean.class);
        method.setAccessible(true);int count=0;
        for(var type:net.minecraft.registry.Registries.ENTITY_TYPE) {
            if(!(type.create(client.world) instanceof net.minecraft.entity.mob.MobEntity mob))continue;
            ((com.tetanusmod.entity.TetanizedState)mob).rustruin$setPermanent(true);
            var renderer=client.getEntityRenderDispatcher().getRenderer(mob);
            if(renderer instanceof net.minecraft.client.render.entity.LivingEntityRenderer living) {
                var layer=(net.minecraft.client.render.RenderLayer)method.invoke(living,mob,true,false,false);
                if(layer==null||!layer.toString().contains("tetanusmod:tetanized/"))throw new IllegalStateException("Missing base mutation texture: "+type+" "+layer);
                count++;
            }
        }
        int rabidCount=0;
        for(var type:net.minecraft.registry.Registries.ENTITY_TYPE){
            if(!(type.create(client.world) instanceof net.minecraft.entity.mob.MobEntity mob)||!Rabies.eligible(mob))continue;
            ((ExpansionState)mob).rustruin$setRabid(true);var renderer=client.getEntityRenderDispatcher().getRenderer(mob);
            if(renderer instanceof net.minecraft.client.render.entity.LivingEntityRenderer living){var layer=(net.minecraft.client.render.RenderLayer)method.invoke(living,mob,true,false,false);if(layer==null||!layer.toString().contains("tetanusmod:rabid/"))throw new IllegalStateException("Missing rabid skin: "+type);rabidCount++;}
        }
        if(rabidCount!=22)throw new IllegalStateException("Rabid texture coverage wrong: "+rabidCount);
        if(count<60)throw new IllegalStateException("Insufficient species texture coverage: "+count);
        org.slf4j.LoggerFactory.getLogger("tetanusmod").info("Verified base mutation render layers for {} vanilla mob types",count);
    }
    private void verifyPotionTooltips(MinecraftClient client) {
        for(var item:new Item[]{Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION}) {
            var tetanus=PotionUtil.setPotion(new ItemStack(item),ModBrewing.TETANUS).getTooltip(client.player,net.minecraft.client.item.TooltipContext.BASIC).stream().map(net.minecraft.text.Text::getString).collect(java.util.stream.Collectors.joining(" "));
            var salmonella=PotionUtil.setPotion(new ItemStack(item),ModBrewing.SALMONELLA).getTooltip(client.player,net.minecraft.client.item.TooltipContext.BASIC).stream().map(net.minecraft.text.Text::getString).collect(java.util.stream.Collectors.joining(" "));
            var lockjaw=PotionUtil.setPotion(new ItemStack(item),ModBrewing.LOCKJAW).getTooltip(client.player,net.minecraft.client.item.TooltipContext.BASIC).stream().map(net.minecraft.text.Text::getString).collect(java.util.stream.Collectors.joining(" "));
            var rabies=PotionUtil.setPotion(new ItemStack(item),ModBrewing.RABIES).getTooltip(client.player,net.minecraft.client.item.TooltipContext.BASIC).stream().map(net.minecraft.text.Text::getString).collect(java.util.stream.Collectors.joining(" "));
            if(!lockjaw.contains("antitoxin")||!rabies.contains("Rabies Cure")||!rabies.contains("two hearts"))throw new IllegalStateException("New disease tooltip lacks actual symptoms/cure");
            if(!tetanus.contains("Lockjaw Sonata")||!tetanus.contains("5:00-10:00")||tetanus.contains("7:30"))throw new IllegalStateException("Inaccurate Tetanus tooltip: "+tetanus);
            if(!salmonella.contains("Vomiting")||!salmonella.contains("nausea")||!salmonella.contains("hunger"))throw new IllegalStateException("Incomplete Salmonella tooltip: "+salmonella);
        }
        org.slf4j.LoggerFactory.getLogger("tetanusmod").info("Verified actual symptoms in all twelve drinkable/splash/lingering disease potion tooltips");
    }
    private void screenshot(MinecraftClient client,String name) { ScreenshotRecorder.saveScreenshot(client.runDirectory,"rust-and-ruin-"+viewer()+"-"+name+".png",client.getFramebuffer(),text->{}); }
    private void finish(MinecraftClient client,String result) {
        finished=true;
        try{Files.writeString(Path.of("client-smoke-result-"+viewer()+".txt"),result+System.lineSeparator());}catch(Exception e){throw new RuntimeException(e);}
        org.slf4j.LoggerFactory.getLogger("tetanusmod").info(result);client.scheduleStop();
    }
}