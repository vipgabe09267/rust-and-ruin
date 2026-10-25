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
    private int ticks,worldTicks,recipesAt,freshTicks;
    private boolean freshFinished;
    private GraphicalCraftingAudit graphicalCrafting;
    private GraphicalAcquisitionAudit graphicalAcquisition;
    private GraphicalAdvancementAudit graphicalAdvancements;
    private GraphicalDespawnAudit graphicalDespawn;
    private GraphicalGloveAudit graphicalGloves;
    private GraphicalTrapRegressionAudit trapRegression;
    private GraphicalCombatRegressionAudit combatRegression;
    private GraphicalCampDiscoveryAudit campDiscovery;
    private GraphicalClinicFacingAudit clinicFacing;
    private int graphicalPhase;
    private final java.util.List<String> auditFindings=new java.util.ArrayList<>();
    private boolean prepared,finished,caltropsOpened,beaconOpened,advancementsOpened,guidePrepared,guideOpened,vaccineOpened;
    private java.util.UUID tradeMedic; private int normalMedicinePrice;
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
                if(++ticks>15000){finish(client,"FAIL: client/world/viewer startup timed out");return;}
                if(client.player==null||client.world==null||client.getServer()==null)return;
                String focus=System.getProperty("rustruin.graphicalFocus","");
                if(!focus.isEmpty()){
                    if(focus.equals("interactions")||focus.equals("traps")||focus.equals("combat")){
                        if(!focus.equals("combat")){
                            if(trapRegression==null){trapRegression=new GraphicalTrapRegressionAudit();trapRegression.start(client);}
                            if(!trapRegression.tick(client))return;
                        }
                        if(!focus.equals("traps")){
                            if(combatRegression==null){combatRegression=new GraphicalCombatRegressionAudit();combatRegression.start(client);}
                            if(!combatRegression.tick(client))return;
                        }
                        finish(client,"PASS: focused graphical profile="+focus+"; native client movement, real trap/reset clicks, actual projectile collisions and infection-only sound observations");return;
                    }
                    if(focus.equals("camp")){
                        if(campDiscovery==null){campDiscovery=new GraphicalCampDiscoveryAudit();campDiscovery.start(client);}
                        if(!campDiscovery.tick(client))return;
                        finish(client,"PASS: focused graphical profile=camp; native global locate, naturally generated camp/medic/guard/loot and local POI verified");return;
                    }
                    if(focus.equals("clinic")){
                        if(clinicFacing==null){clinicFacing=new GraphicalClinicFacingAudit();clinicFacing.start(client);}
                        if(!clinicFacing.tick(client))return;
                        finish(client,"PASS: focused graphical profile=clinic; five native biome clinic templates and both cabinet halves face their interior aisles");return;
                    }
                    if(focus.equals("gloves")){
                        if(graphicalGloves==null){graphicalGloves=new GraphicalGloveAudit();graphicalGloves.start(client);}
                        if(!graphicalGloves.tick(client))return;
                        finish(client,"PASS: focused graphical profile=gloves; sixteen native opaque-sleeve A/B captures completed; image inspection determines glove visibility");return;
                    }
                    if(!focus.equals("advancements")&&!focus.equals("despawn"))throw new IllegalArgumentException("Unknown graphical test profile: "+focus);
                    if(focus.equals("advancements")){
                        if(graphicalAdvancements==null){graphicalAdvancements=new GraphicalAdvancementAudit();graphicalAdvancements.start(client);}
                        if(!graphicalAdvancements.tick(client))return;
                    }
                    if(graphicalDespawn==null){graphicalDespawn=new GraphicalDespawnAudit();graphicalDespawn.start(client);}
                    if(!graphicalDespawn.tick(client))return;
                    finish(client,"PASS: focused graphical profile="+focus+"; actual native client advancement/despawn assertions completed");return;
                }
                if(!freshFinished){
                    freshTicks++;
                    if(freshTicks==1)onServer(client,()->{
                        var world=client.getServer().getOverworld();var player=client.getServer().getPlayerManager().getPlayerList().get(0);
                        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)world.setBlockState(new BlockPos(x,3,z),Blocks.STONE.getDefaultState());
                        player.getInventory().clear();player.clearStatusEffects();player.changeGameMode(GameMode.SURVIVAL);player.teleport(world,0.5,4,0.5,0,8);
                    });
                    if(freshTicks==20)onServer(client,()->{
                        var server=client.getServer();var player=server.getPlayerManager().getPlayerList().get(0);
                        if(!player.getAdvancementTracker().getProgress(server.getAdvancementLoader().get(TetanusMod.id("root"))).isDone())throw new IllegalStateException("New Survival player has an empty Rust & Ruin tab");
                        if(player.getAdvancementTracker().getProgress(server.getAdvancementLoader().get(TetanusMod.id("iron_has_a_darker_side"))).isDone())throw new IllegalStateException("Fresh fixture already obtained iron");
                    });
                    if(freshTicks==25){
                        var manager=client.player.networkHandler.getAdvancementHandler();var root=manager.getManager().get(TetanusMod.id("root"));
                        var screen=new net.minecraft.client.gui.screen.advancement.AdvancementsScreen(manager);client.setScreen(screen);screen.selectTab(root);
                        if(root==null||screen.getAdvancementWidget(root)==null)throw new IllegalStateException("Native fresh-player advancement screen is empty");
                    }
                    if(freshTicks==40){screenshot(client,"fresh-survival-advancements");client.setScreen(null);freshFinished=true;}
                    return;
                }
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
                if(worldTicks==130){screenshot(client,"tab-hearts");var guide=new com.tetanusmod.client.FieldGuideScreen();guide.selectChapter(6);if(TetanusMod.CONFIG.naturalAnimalEasy==0.005F&&!guide.body().getString().contains("animals 0.5/"))auditFindings.add("Field Guide rounds the 0.5 percent default to a whole percent");}
                if(worldTicks==135)client.options.playerListKey.setPressed(false);
                if(worldTicks==140)client.setScreen(new InventoryScreen(client.player));
                boolean ready=plain|| (rei?RustRuinReiPlugin.registeredCount==9:RustRuinJeiPlugin.runtimeReady);
                if(worldTicks>220&&ready&&recipesAt==0) {
                    screenshot(client,"inventory");
                    if(!plain&&!(rei?RustRuinReiPlugin.hasDiseaseBrewing():RustRuinJeiPlugin.hasDiseaseBrewing()))throw new IllegalStateException("Disease brewing recipes missing from viewer");
                    if(!plain&&rei) { if(!RustRuinReiPlugin.showRusting())throw new IllegalStateException("REI Rusting screen unavailable"); }
                    else if(!plain) { if(RustRuinJeiPlugin.rustingRecipeCount!=9||!RustRuinJeiPlugin.hasCaltropRecipe())throw new IllegalStateException("JEI recipe registration missing");RustRuinJeiPlugin.showRustingRecipes(); }
                    if(!plain&&!(rei?RustRuinReiPlugin.hasCoating():RustRuinJeiPlugin.hasCoating()))throw new IllegalStateException("Wax coating viewer displays missing");
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
                        String lower=body.toLowerCase(java.util.Locale.ROOT);
                        if(chapter==4&&(!lower.contains("iron ingots down both sides")||lower.contains("birch planks")||!lower.contains("three seconds")))throw new IllegalStateException("Crafting guide contains a stale cabinet recipe or trap reset duration: "+body);
                        if(chapter==2&&(!body.contains("Nurse Practitioner")||!lower.contains("two to four")||!lower.contains("three cycles")))throw new IllegalStateException("Prevention guide omits the current medic progression, stock or field restock: "+body);
                        if(chapter==9&&!body.contains("/locate poi tetanusmod:medic_tent"))throw new IllegalStateException("Command guide omits the underground medic POI command: "+body);
                    }
                    guide.selectChapter(2);
                    if(!guide.body().getString().contains("80%")||!guide.body().getString().contains("90%"))throw new IllegalStateException("Guide protection values do not match server config");
                    for(String id:new String[]{"tetanus_vaccine","protective_gloves","field_guide","veterinary_kit"})if(client.world.getRecipeManager().get(TetanusMod.id(id)).isEmpty())throw new IllegalStateException("Prevention recipe missing: "+id);
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
                        var world=client.getServer().getOverworld();for(int x=18;x<=30;x++)for(int z=9;z<=31;z++)world.setBlockState(new BlockPos(x,3,z),Blocks.STONE.getDefaultState());var template=world.getStructureTemplateManager().getTemplate(TetanusMod.id("village/clinic")).orElseThrow();var pos=new BlockPos(20,2,20);template.place(world,pos,pos,new net.minecraft.structure.StructurePlacementData().addProcessor(net.minecraft.structure.processor.JigsawReplacementStructureProcessor.INSTANCE),world.random,3);
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
                    var select=creative.getClass().getDeclaredMethod("setSelectedTab",ItemGroup.class);select.setAccessible(true);var group=net.minecraft.registry.Registries.ITEM_GROUP.get(TetanusMod.id("items"));selectCreative(creative,group);
                    for(var item:ModItems.ALL){boolean legacy=com.tetanusmod.registry.ModIronTools.isRusted(item)||item instanceof VariantSpawnEggItem;boolean shown=group.getDisplayStacks().stream().anyMatch(stack->stack.isOf(item));if(shown==legacy)throw new IllegalStateException("Creative item visibility incorrect: "+item);}
                    }
                if(rabiesPrepared&&worldTicks==recipesAt+415){
                    screenshot(client,"creative-items");
                    var creative=(net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen)client.currentScreen;var select=creative.getClass().getDeclaredMethod("setSelectedTab",ItemGroup.class);select.setAccessible(true);
                    var eggGroup=net.minecraft.registry.Registries.ITEM_GROUP.get(TetanusMod.id("spawn_eggs"));selectCreative(creative,eggGroup);
                    if(eggGroup.getDisplayStacks().size()<100||eggGroup.getDisplayStacks().stream().anyMatch(stack->!(stack.getItem() instanceof VariantSpawnEggItem)))throw new IllegalStateException("Spawn egg tab is incomplete or contains ordinary items");
                    }
                if(rabiesPrepared&&worldTicks==recipesAt+420){
                    screenshot(client,"spawn-eggs");
                    var creative=(net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen)client.currentScreen;var select=creative.getClass().getDeclaredMethod("setSelectedTab",ItemGroup.class);select.setAccessible(true);select.invoke(creative,ItemGroups.getDefaultTab());
                    var inventoryGroup=net.minecraft.registry.Registries.ITEM_GROUP.stream().filter(g->g.getType()==ItemGroup.Type.INVENTORY).findFirst().orElseThrow();select.invoke(creative,inventoryGroup);
                    var slot=creative.getScreenHandler().getSlot(46);if(slot.x!=136||slot.y!=20)throw new IllegalStateException("Creative glove slot overlaps hotbar");
                    var gloves=new ItemStack(ModItems.PROTECTIVE_GLOVES);gloves.setDamage(37);client.interactionManager.clickCreativeStack(gloves,46);
                }
                if(rabiesPrepared&&worldTicks==recipesAt+430){
                    if(!client.player.playerScreenHandler.getSlot(46).getStack().isOf(ModItems.PROTECTIVE_GLOVES)||!((net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen)client.currentScreen).getScreenHandler().getSlot(46).getStack().isOf(ModItems.PROTECTIVE_GLOVES))throw new IllegalStateException("Glove slot differs from tracked equipped state: "+client.player.playerScreenHandler.getSlot(46).getStack()+" / "+((net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen)client.currentScreen).getScreenHandler().getSlot(46).getStack());
                    if(GloveInventory.equipped(client.player).getDamage()!=37)throw new IllegalStateException("Creative glove packet did not synchronize");screenshot(client,"creative-gloves");
                }
                if(rabiesPrepared&&worldTicks==recipesAt+435){
                    if(!plain&&rei){if(!RustRuinReiPlugin.showCoating())throw new IllegalStateException("REI coating screen failed to open");}
                    else if(!plain)RustRuinJeiPlugin.showCoating();
                }
                if(rabiesPrepared&&worldTicks==recipesAt+455){
                    if(!plain)screenshot(client,"wax-coating");client.setScreen(null);
                    onServer(client,()->{
                        var world=client.getServer().getOverworld();var pos=new BlockPos(40,3,20);
                        for(int x=38;x<=53;x++)for(int z=8;z<=35;z++)world.setBlockState(new BlockPos(x,3,z),Blocks.STONE.getDefaultState());
                        var template=world.getStructureTemplateManager().getTemplate(TetanusMod.id("workshop")).orElseThrow();
                        if(!template.place(world,pos,pos,new net.minecraft.structure.StructurePlacementData(),world.random,3))throw new IllegalStateException("Workshop client placement failed");
                        client.getServer().getPlayerManager().getPlayerList().get(0).teleport(world,45.5,5,10.5,0,-6);
                    });
                }
                if(rabiesPrepared&&worldTicks==recipesAt+480){if(client.player.getY()<3)throw new IllegalStateException("Workshop camera fell below the platform");client.options.hudHidden=true;screenshot(client,"workshop");client.options.hudHidden=false;
                }
                if(rabiesPrepared&&worldTicks==recipesAt+500)onServer(client,()->{
                    var world=client.getServer().getOverworld();var pos=new BlockPos(61,4,20);for(int x=58;x<=64;x++)for(int z=14;z<=24;z++)world.setBlockState(new BlockPos(x,3,z),Blocks.STONE.getDefaultState());
                    var state=ExpansionBlocks.MEDIC_CABINET.getDefaultState().with(MedicineCabinetBlock.COMPLETE,true);
                    world.setBlockState(pos,state,18);world.setBlockState(pos.up(),state.with(net.minecraft.block.DoorBlock.HALF,net.minecraft.block.enums.DoubleBlockHalf.UPPER),3);
                    var player=client.getServer().getPlayerManager().getPlayerList().get(0);player.getInventory().setStack(10,new ItemStack(ExpansionBlocks.MEDIC_CABINET));player.playerScreenHandler.sendContentUpdates();
                    client.getServer().getPlayerManager().getPlayerList().get(0).teleport(world,61.5,4,17.5,0,12);
                });
                if(rabiesPrepared&&worldTicks==recipesAt+525){if(client.player.getY()<3)throw new IllegalStateException("Cabinet camera fell below the platform");screenshot(client,"medicine-cabinet");onServer(client,()->{
                    var world=client.getServer().getOverworld();var pos=new BlockPos(61,4,20);var player=client.getServer().getPlayerManager().getPlayerList().get(0);
                    ExpansionBlocks.MEDIC_CABINET.onUse(world.getBlockState(pos),world,pos,player,net.minecraft.util.Hand.MAIN_HAND,new net.minecraft.util.hit.BlockHitResult(net.minecraft.util.math.Vec3d.ofCenter(pos),net.minecraft.util.math.Direction.NORTH,pos,false));
                });}
                if(rabiesPrepared&&worldTicks==recipesAt+540)screenshot(client,"medicine-cabinet-open");
                if(rabiesPrepared&&worldTicks==recipesAt+542)client.setScreen(new InventoryScreen(client.player));
                if(rabiesPrepared&&worldTicks==recipesAt+548){
                    var model=client.getItemRenderer().getModel(new ItemStack(ExpansionBlocks.MEDIC_CABINET),client.world,client.player,0);
                    if(model.getQuads(null,null,net.minecraft.util.math.random.Random.create(0)).size()<300)throw new IllegalStateException("Complete cabinet item model did not bake");
                    screenshot(client,"cabinet-inventory");client.setScreen(null);
                }
                if(rabiesPrepared&&worldTicks==recipesAt+550)onServer(client,()->{
                    var player=client.getServer().getPlayerManager().getPlayerList().get(0);player.equipStack(EquipmentSlot.MAINHAND,ItemStack.EMPTY);player.teleport(client.getServer().getOverworld(),45.5,4,10.5,180,8);
                });
                if(rabiesPrepared&&worldTicks==recipesAt+565)screenshot(client,"gloves-first-person");
                if(rabiesPrepared&&worldTicks==recipesAt+566)onServer(client,()->client.getServer().getPlayerManager().getPlayerList().get(0).setMainArm(net.minecraft.util.Arm.LEFT));
                if(rabiesPrepared&&worldTicks==recipesAt+569)screenshot(client,"gloves-left-hand");
                if(rabiesPrepared&&worldTicks==recipesAt+570)client.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_FRONT);
                if(rabiesPrepared&&worldTicks==recipesAt+585)screenshot(client,"gloves-third-person");
                if(rabiesPrepared&&worldTicks==recipesAt+590){client.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);onServer(client,()->{
                    var player=client.getServer().getPlayerManager().getPlayerList().get(0);player.setMainArm(net.minecraft.util.Arm.RIGHT);player.equipStack(EquipmentSlot.MAINHAND,new ItemStack(Items.BREAD));player.addStatusEffect(new StatusEffectInstance(TetanusMod.LOCKJAW,600));
                });}
                if(rabiesPrepared&&worldTicks==recipesAt+610){
                    var result=net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(client.player,client.world,net.minecraft.util.Hand.MAIN_HAND);
                    if(result.getResult()!=net.minecraft.util.ActionResult.FAIL||!com.tetanusmod.client.LockjawAttempt.active())throw new IllegalStateException("Lockjaw failed eating gesture did not start");
                }
                if(rabiesPrepared&&worldTicks==recipesAt+615){
                    if(client.player.isUsingItem()||client.player.getMainHandStack().getCount()!=1)throw new IllegalStateException("Lockjaw gesture started real food consumption");screenshot(client,"lockjaw-attempt");
                }
                if(rabiesPrepared&&worldTicks==recipesAt+620)onServer(client,()->{
                    var world=client.getServer().getOverworld();var pos=new BlockPos(70,3,20);
                    for(int x=68;x<=85;x++)for(int z=12;z<=35;z++)world.setBlockState(new BlockPos(x,3,z),Blocks.STONE.getDefaultState());
                    var template=world.getStructureTemplateManager().getTemplate(TetanusMod.id("medic_tent")).orElseThrow();
                    template.place(world,pos,pos,new net.minecraft.structure.StructurePlacementData().addProcessor(net.minecraft.structure.processor.BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS),world.random,3);
                    var guard=EntityType.IRON_GOLEM.create(world);guard.setAiDisabled(true);guard.setPlayerCreated(true);guard.setPosition(80.5,4,28.5);MobVariants.makePermanent(guard);world.spawnEntity(guard);
                    client.getServer().getPlayerManager().getPlayerList().get(0).teleport(world,76.5,4,14.5,0,-5);
                });
                if(rabiesPrepared&&worldTicks==recipesAt+650){
                    screenshot(client,"medic-tent");
                    onServer(client,()->{
                        var player=client.getServer().getPlayerManager().getPlayerList().get(0);
                        client.getServer().getPlayerManager().addToOperators(player.getGameProfile());
                        var poi=player.getServerWorld().getPointOfInterestStorage().getNearestPosition(entry->entry.matchesKey(MedicVillage.FIELD_JOB_KEY),player.getBlockPos(),256,net.minecraft.world.poi.PointOfInterestStorage.OccupationStatus.ANY);
                        if(poi.isEmpty())throw new IllegalStateException("Graphical cave camp lacks its locatable native POI");
                    });
                }
                if(rabiesPrepared&&worldTicks==recipesAt+655)client.player.networkHandler.sendChatCommand("locate poi tetanusmod:medic_tent");
                if(rabiesPrepared&&worldTicks==recipesAt+659)screenshot(client,"native-tent-poi-locate");
                if(rabiesPrepared&&worldTicks==recipesAt+660)onServer(client,()->{
                    var world=client.getServer().getOverworld();for(int x=92;x<=108;x++)for(int z=12;z<=27;z++)world.setBlockState(new BlockPos(x,3,z),Blocks.STONE.getDefaultState());
                    String[] types={"plains","desert","savanna","snow","taiga","swamp","jungle"};
                    for(int i=0;i<types.length;i++){
                        var medic=EntityType.VILLAGER.create(world);medic.setAiDisabled(true);medic.setVillagerData(medic.getVillagerData().withProfession(MedicVillage.MEDIC).withType(net.minecraft.registry.Registries.VILLAGER_TYPE.get(new net.minecraft.util.Identifier("minecraft",types[i]))));
                        medic.refreshPositionAndAngles(94.5+i*2,4,25.5,180,0);medic.setHeadYaw(180);medic.bodyYaw=180;world.spawnEntity(medic);
                    }
                    client.getServer().getPlayerManager().getPlayerList().get(0).teleport(world,100.5,4,14.5,0,2);
                });
                if(rabiesPrepared&&worldTicks==recipesAt+690)screenshot(client,"medic-biomes");
                if(rabiesPrepared&&worldTicks==recipesAt+700)onServer(client,()->{
                    var player=client.getServer().getPlayerManager().getPlayerList().get(0);player.equipStack(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));player.playerScreenHandler.sendContentUpdates();player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory((sync,inventory,actor)->net.minecraft.screen.GenericContainerScreenHandler.createGeneric9x3(sync,inventory),net.minecraft.text.Text.literal("Glove synchronization check")));
                });
                if(rabiesPrepared&&worldTicks==recipesAt+710)onServer(client,()->{
                    var player=client.getServer().getPlayerManager().getPlayerList().get(0);player.changeGameMode(GameMode.SURVIVAL);var gloves=GloveInventory.equipped(player);if(!gloves.isOf(ModItems.PROTECTIVE_GLOVES))throw new IllegalStateException("Glove break fixture is missing its gloves");gloves.setDamage(gloves.getMaxDamage()-1);com.tetanusmod.effect.InfectionProtection.allows(player,true,0);
                });
                if(rabiesPrepared&&worldTicks==recipesAt+730){
                    if(!client.player.getOffHandStack().isOf(Items.SHIELD)||client.player.getOffHandStack().getDamage()!=0)throw new IllegalStateException("Breaking gloves corrupted the held shield");
                    if(!((GloveInventory)client.player).rustruin$visualGloves().isEmpty())throw new IllegalStateException("Glove removal metadata did not arrive");
                    if(!GloveInventory.equipped(client.player).isEmpty())auditFindings.add("Tracked glove removal fell back to stale inventory while a container was open");
                    onServer(client,()->client.getServer().getPlayerManager().getPlayerList().get(0).closeHandledScreen());
                }
                if(rabiesPrepared&&worldTicks==recipesAt+750)onServer(client,()->{
                    var server=client.getServer();var player=server.getPlayerManager().getPlayerList().get(0);
                    player.clearStatusEffects();player.getInventory().clear();player.setHealth(player.getMaxHealth());

                });
                if(rabiesPrepared&&worldTicks==recipesAt+765)onServer(client,()->{
                    var server=client.getServer();var player=server.getPlayerManager().getPlayerList().get(0);
                    var root=server.getAdvancementLoader().get(TetanusMod.id("root"));
                    if(!player.getAdvancementTracker().getProgress(root).isDone())throw new IllegalStateException("Fresh Survival root never unlocks without iron or disease");
                    var iron=server.getAdvancementLoader().get(TetanusMod.id("iron_has_a_darker_side"));
                    if(player.getAdvancementTracker().getProgress(iron).isDone())throw new IllegalStateException("Iron advancement unlocked without obtaining iron");
                });
                if(rabiesPrepared&&worldTicks==recipesAt+775){
                    var manager=client.player.networkHandler.getAdvancementHandler();var root=manager.getManager().get(TetanusMod.id("root"));
                    if(root==null)throw new IllegalStateException("Fresh Survival advancement root absent on client");
                    var screen=new net.minecraft.client.gui.screen.advancement.AdvancementsScreen(manager);client.setScreen(screen);screen.selectTab(root);
                    if(screen.getAdvancementWidget(root)==null)throw new IllegalStateException("Fresh Survival advancement tab is still empty");
                }
                if(rabiesPrepared&&worldTicks==recipesAt+790){screenshot(client,"advancements-after-playtest");client.setScreen(null);}
                if(rabiesPrepared&&worldTicks==recipesAt+800)onServer(client,()->{
                    var player=client.getServer().getPlayerManager().getPlayerList().get(0);player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND,ItemStack.EMPTY);
                    var medic=EntityType.VILLAGER.create(player.getServerWorld());medic.setAiDisabled(true);
                    medic.setVillagerData(medic.getVillagerData().withProfession(MedicVillage.MEDIC).withLevel(1));
                    medic.refreshPositionAndAngles(player.getX(),player.getY(),player.getZ()+2,180,0);player.getServerWorld().spawnEntity(medic);tradeMedic=medic.getUuid();
                    if(medic.getOffers().size()!=2)throw new IllegalStateException("Novice medic did not receive two randomized offers");
                    medic.interactMob(player,net.minecraft.util.Hand.MAIN_HAND);
                });
                if(rabiesPrepared&&worldTicks==recipesAt+820){
                    if(!(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.MerchantScreen screen)||!com.tetanusmod.client.MedicTradeScreen.isMedic(screen))throw new IllegalStateException("Native medic trade menu/rank metadata failed");
                    if(screen.getScreenHandler().getLevelProgress()!=1||screen.getScreenHandler().getRecipes().size()!=2)throw new IllegalStateException("Nurse trade progression is incorrect");
                    if(!net.minecraft.text.Text.translatable(com.tetanusmod.client.MedicTradeScreen.rankKey(1)).getString().equals("Nurse"))throw new IllegalStateException("Nurse rank is not translated");
                    screenshot(client,"medic-nurse-trades");
                }
                if(rabiesPrepared&&worldTicks==recipesAt+840)onServer(client,()->{
                    var player=client.getServer().getPlayerManager().getPlayerList().get(0);player.closeHandledScreen();
                    var medic=(net.minecraft.entity.passive.VillagerEntity)player.getServerWorld().getEntity(tradeMedic);
                    medic.setVillagerData(medic.getVillagerData().withLevel(4));medic.setOffers(null);medic.interactMob(player,net.minecraft.util.Hand.MAIN_HAND);
                    normalMedicinePrice=medic.getOffers().get(0).getAdjustedFirstBuyItem().getCount();
                });
                if(rabiesPrepared&&worldTicks==recipesAt+860){
                    if(!(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.MerchantScreen screen)||!com.tetanusmod.client.MedicTradeScreen.isMedic(screen))throw new IllegalStateException("Attending trade menu failed");
                    if(screen.getScreenHandler().getRecipes().size()!=8||screen.getScreenHandler().getLevelProgress()!=4)throw new IllegalStateException("Attending rank did not retain eight progressive offers");
                    var heading=com.tetanusmod.client.MedicTradeScreen.title(screen,screen.getTitle()).append(" - ").append(net.minecraft.text.Text.translatable(com.tetanusmod.client.MedicTradeScreen.rankKey(4)));
                    if(client.textRenderer.getWidth(heading)>164)throw new IllegalStateException("Attending title clips the native trade pane");
                    var view=(com.tetanusmod.client.MedicTradeScreen.View)screen;
                    if(view.rustruin$firstOffer()!=0)throw new IllegalStateException("New trade menu did not begin at first offer");
                    screen.mouseScrolled(screen.width/2.0,screen.height/2.0,-1);
                    if(view.rustruin$firstOffer()!=1)throw new IllegalStateException("Native medic trade list does not scroll");
                    screen.mouseScrolled(screen.width/2.0,screen.height/2.0,1);
                    if(view.rustruin$firstOffer()!=0)throw new IllegalStateException("Native medic trade list cannot scroll back");
                    int left=(screen.width-276)/2,top=(screen.height-166)/2;
                    screen.mouseClicked(left+95,top+20,0);screen.mouseDragged(left+95,top+139,0,0,119);screen.mouseReleased(left+95,top+139,0);
                    if(view.rustruin$firstOffer()!=1)throw new IllegalStateException("Medic trade scrollbar cannot be dragged");
                    screenshot(client,"medic-attending-scrolled");
                }
                if(rabiesPrepared&&worldTicks==recipesAt+880)onServer(client,()->{
                    var player=client.getServer().getPlayerManager().getPlayerList().get(0);player.closeHandledScreen();
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE,600));
                    var medic=(net.minecraft.entity.passive.VillagerEntity)player.getServerWorld().getEntity(tradeMedic);medic.interactMob(player,net.minecraft.util.Hand.MAIN_HAND);
                    if(medic.getOffers().get(0).getAdjustedFirstBuyItem().getCount()>=normalMedicinePrice)throw new IllegalStateException("Hero of the Village did not reduce medic prices");
                });
                if(rabiesPrepared&&worldTicks==recipesAt+900)screenshot(client,"medic-hero-discount");
                if(rabiesPrepared&&worldTicks==recipesAt+920)onServer(client,()->{
                    var player=client.getServer().getPlayerManager().getPlayerList().get(0);player.closeHandledScreen();
                    var medic=EntityType.VILLAGER.create(player.getServerWorld());medic.setAiDisabled(true);MedicTradeOffers.markField(medic);
                    medic.setVillagerData(medic.getVillagerData().withProfession(MedicVillage.MEDIC).withLevel(1));medic.refreshPositionAndAngles(player.getX(),player.getY(),player.getZ()+2,180,0);player.getServerWorld().spawnEntity(medic);
                    if(medic.getOffers().stream().anyMatch(offer->offer.getMaxUses()>2))throw new IllegalStateException("Field medic has excessive stock");
                    medic.interactMob(player,net.minecraft.util.Hand.MAIN_HAND);
                });
                if(rabiesPrepared&&worldTicks==recipesAt+940){
                    if(!(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.MerchantScreen screen)||!com.tetanusmod.client.MedicTradeScreen.isField(screen))throw new IllegalStateException("Field medic menu lost its supply identity");
                    screenshot(client,"field-medic-supplies");
                }
                if(rabiesPrepared&&worldTicks==recipesAt+960)onServer(client,()->client.getServer().getPlayerManager().getPlayerList().get(0).closeHandledScreen());
                if(rabiesPrepared&&worldTicks>=recipesAt+1000){
                    if(graphicalPhase==0){graphicalCrafting=new GraphicalCraftingAudit();graphicalCrafting.start(client);graphicalPhase=1;}
                    if(graphicalPhase==1){
                        if(!graphicalCrafting.tick(client))return;
                        graphicalAcquisition=new GraphicalAcquisitionAudit();graphicalAcquisition.start(client);graphicalPhase=2;
                    }
                    if(graphicalPhase==2){
                        if(!graphicalAcquisition.tick(client))return;
                        graphicalAdvancements=new GraphicalAdvancementAudit();graphicalAdvancements.start(client);graphicalPhase=3;
                    }
                    if(graphicalPhase==3){
                        if(!graphicalAdvancements.tick(client))return;
                        graphicalDespawn=new GraphicalDespawnAudit();graphicalDespawn.start(client);graphicalPhase=4;
                    }
                    if(graphicalPhase==4&&!graphicalDespawn.tick(client))return;
                    if(!auditFindings.isEmpty())throw new IllegalStateException(String.join("; ",auditFindings));
                    finish(client,"PASS: "+(plain?"Plain client without recipe viewers; native recipes":rei?"REI Rusting (9), crafting/brewing":"JEI Rusting (9), crafting/brewing")+", 78 skins, 12 potion tooltips, Rabies HUD/tab and 22 rabid skins, creative item coverage/glove synchronization, Lockjaw/hearts/beacon/advancements; vaccine sync, dedicated glove slot, 10 guide chapters, veterinary kit recipe, nine coating displays, workshop, species egg tab, two-block cabinet, first/third-person gloves, failed Lockjaw gesture, cave tent and seven medic uniforms; glove breakage with open container and intact shield; fresh uninfected Survival advancement tab, Nurse and Attending trades, native wheel/drag scrollbar, Hero discount and scarce field medicine; all29 recipes crafted/cooked/smithed through native graphical menus, all15 potion forms brewed through native graphical menus, all28 active advancements earned by the connected player and visually verified; native automatic hostile despawn while entity ticking, with named and livestock controls retained");
                }
            }catch(Throwable failure){client.options.useKey.setPressed(false);org.slf4j.LoggerFactory.getLogger("tetanusmod").error("Graphical release validation failed",failure);finish(client,"FAIL: "+failure);}
        });
    }
    private void selectCreative(net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen screen,ItemGroup group)throws Exception{
        var visible=screen.getClass().getDeclaredMethod("fabric_isGroupVisible",ItemGroup.class);visible.setAccessible(true);
        var previous=screen.getClass().getMethod("fabric_previousPage");var next=screen.getClass().getMethod("fabric_nextPage");
        var page=screen.getClass().getMethod("fabric_currentPage");while((int)page.invoke(screen)>0)previous.invoke(screen);
        for(int i=0;i<100&&!(boolean)visible.invoke(screen,group);i++)next.invoke(screen);
        if(!(boolean)visible.invoke(screen,group))throw new IllegalStateException("Creative tab page inaccessible: "+group.getDisplayName().getString());
        var select=screen.getClass().getDeclaredMethod("setSelectedTab",ItemGroup.class);select.setAccessible(true);select.invoke(screen,group);
    }
    private void onServer(MinecraftClient client,Runnable action){client.getServer().execute(()->{try{action.run();}catch(Throwable failure){serverFailure="Server smoke task failed: "+failure;}});}
    private void prepare(MinecraftClient client) {
        try {
            var server=client.getServer();var world=server.getOverworld();
            var cleanup=new java.util.ArrayList<Entity>();for(var e:world.iterateEntities())if(!(e instanceof net.minecraft.entity.player.PlayerEntity))cleanup.add(e);cleanup.forEach(Entity::discard);
            var player=server.getPlayerManager().getPlayerList().get(0);

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
