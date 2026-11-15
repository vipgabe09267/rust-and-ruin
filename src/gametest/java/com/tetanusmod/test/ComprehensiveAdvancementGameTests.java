package com.tetanusmod.test;

import com.google.gson.GsonBuilder;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.beacon.*;
import com.tetanusmod.entity.*;
import com.tetanusmod.expansion.*;
import com.tetanusmod.registry.*;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
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
import net.minecraft.test.*;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import java.nio.file.*;
import java.util.*;

/** One connected player preserves all 28 existing progression actions; new dragon/forge coverage is separate. */
public class ComprehensiveAdvancementGameTests implements FabricGameTest {
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

    @GameTest(templateName=EMPTY_STRUCTURE, batchId="release_progression", tickLimit=1600)
    public void everyExistingAdvancementRemainsEarnableByOnePlayer(TestContext ctx) {
        new Journey(ctx).start();
    }

    private static final class Journey {
        private final TestContext ctx;
        private final ServerPlayerEntity player;
        private final List<Entity> fixtures = new ArrayList<>();
        private final Map<String,String> completed = new LinkedHashMap<>();
        private final boolean originalPvp;
        private boolean finished, pvpChanged;private long sceneStart;
        private ZombieEntity attacker;
        private CowEntity spearTarget,meatCowFixture,treatmentCowFixture;
        private WitherEntity witherFixture;
        private VillagerEntity medic;
        private ItemEntity droppedNail;
        private BlockPos beaconPos;
        private NailEntity returningNail;
        private Item consuming;private int usageBefore;
        private final List<Map<String,Object>> consumptionEvidence=new ArrayList<>();

        Journey(TestContext ctx) {
            this.ctx = ctx;
            var world = ctx.getWorld();
            originalPvp = world.getServer().isPvpEnabled();
            player = new ServerPlayerEntity(world.getServer(), world,
                    new com.mojang.authlib.GameProfile(UUID.randomUUID(), "all-advancements"));
            world.getServer().getPlayerManager().onPlayerConnect(new ClientConnection(NetworkSide.SERVERBOUND), player);
            player.changeGameMode(GameMode.SURVIVAL);
            player.getAbilities().creativeMode = false;
            player.getAbilities().invulnerable = false;
            ((com.tetanusmod.test.mixin.ServerPlayerTestAccessor)player).rustruin$joinInvulnerability(0);
            player.setNoGravity(true);
            player.getInventory().clear();
            player.setPosition(ctx.getAbsolute(new Vec3d(3.5, 3.2, 2.5)));
        }
        void start() {
            for(String id:ACTIONS.keySet())if(!id.equals("root"))ctx.assertTrue(!isDone(id),"Journey did not start with fresh advancement progress: "+id);
            var active = new TreeSet<String>();
            for (var advancement : player.getServer().getAdvancementLoader().getAdvancements()) {
                var id = advancement.getId();
                if (id.getNamespace().equals(TetanusMod.MOD_ID) && !id.getPath().equals("variant_egg")) active.add(id.getPath());
            }
            var expected=new TreeSet<>(ACTIONS.keySet());expected.addAll(java.util.Set.of("forge_crystal","dragon_summoned","dragon_defeated"));
            ctx.assertTrue(active.equals(expected), "Existing journey or separate dragon coverage omits a live mod advancement: " + active);
            var retired = player.getServer().getAdvancementLoader().get(TetanusMod.id("variant_egg"));
            ctx.assertTrue(retired != null && retired.getDisplay() == null, "Retired Creative-only Bad Egg unexpectedly became an active achievement");
            for (int x=0;x<=12;x++) for (int z=0;z<=12;z++) {
                ctx.setBlockState(x, 2, z, Blocks.STONE);
                for (int y=3;y<=8;y++) ctx.setBlockState(x, y, z, Blocks.AIR);
            }
            // Register every callback before GameTestState iterates its native
            // Object2LongOpenHashMap. Adding callbacks from inside another
            // runAtTick callback can rehash that map while its iterator is live.
            for (long tick=1;tick<=1100;tick++) {
                final long now=tick;
                ctx.runAtTick(now,()->advance(now));
            }
        }
        private void advance(long absoluteTick) {
            if(finished)return;
            try {
                if(sceneStart==0) {
                    if(absoluteTick<40)return;
                    if(isDone("root"))sceneStart=absoluteTick;
                    else ctx.assertTrue(absoluteTick<240,"Connected headless actor did not naturally receive the root advancement after chunk warmup");
                    return;
                }
                long tick=absoluteTick-sceneStart;
                // The headless connection is not accepted by ServerNetworkIo,
                // so its handler does not call ServerPlayerEntity.playerTick.
                // Supply that native tick once per simulated active-use tick.
                // The graphical journey relies on its real connection instead.
                if(tick>=1&&tick<=850&&player.isAlive()&&player.isUsingItem())player.playerTick();
                switch((int)tick) {
                    case 4 -> waterAndInventory();
                    case 155 -> prepareCreatures();
                    case 160 -> worldInteractions();
                    case 165 -> spear();
                    case 170 -> drink(ModBrewing.TETANUS);
                    case 204 -> { consumed(TetanusMod.TETANUS);earned("infected"); eat(ModItems.INFECTED_MEAT); }
                    case 238 -> { consumed(TetanusMod.SALMONELLA);earned("why_did_i_eat_that"); eat(ExpansionItems.RABID_MEAT); }
                    case 272 -> { consumed(ExpansionEffects.RABIES);earned("rabies"); use(new ItemStack(ExpansionItems.RABIES_CURE)); }
                    case 306 -> { consumed(null);ctx.assertTrue(!player.hasStatusEffect(ExpansionEffects.RABIES),"Consumed Rabies Cure left Rabies active");earned("rabies_cured"); drink(ModBrewing.LOCKJAW); }
                    case 340 -> { consumed(TetanusMod.LOCKJAW);earned("lockjaw_sonata"); use(new ItemStack(ModItems.ANTITOXIN)); }
                    case 368 -> { consumed(null);ctx.assertTrue(!player.hasStatusEffect(TetanusMod.TETANUS)&&!player.hasStatusEffect(TetanusMod.LOCKJAW),"Consumed antitoxin left Tetanus or Lockjaw active");earned("antitoxin"); use(new ItemStack(ModItems.VACCINE)); }
                    case 403 -> { consumed(TetanusMod.VACCINATED);earned("vaccinated"); buildBeacon(); }
                    case 510 -> selectBeacon();
                    case 610 -> { earned("beacon_full"); beginReturningNail(); }
                }
                if(tick>=611&&tick<=850)catchReturningNail((int)tick);
            } catch(RuntimeException|Error failure) {
                cleanup();throw failure;
            }
        }
        private void earned(String id) {earned(id,ACTIONS.get(id));}
        private void earned(String id,String actualCause) {
            var advancement = player.getServer().getAdvancementLoader().get(TetanusMod.id(id));
            ctx.assertTrue(advancement != null && player.getAdvancementTracker().getProgress(advancement).isDone(), "Gameplay action failed to earn " + id + ": " + ACTIONS.get(id));
            completed.putIfAbsent(id, actualCause);
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
            player.setPosition(ctx.getAbsolute(new Vec3d(7.5,3,7.5)));
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
            player.setPosition(meatCow.getPos().add(0,0,-2));
            var sword=new ItemStack(Items.NETHERITE_SWORD);sword.addAttributeModifier(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE,new net.minecraft.entity.attribute.EntityAttributeModifier(UUID.fromString("1355a1a6-5c4c-4d5e-9291-35139008fef7"),"Creative advancement test sword",20000,net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADDITION),EquipmentSlot.MAINHAND);
            player.setStackInHand(Hand.MAIN_HAND,sword);player.playerTick();player.attack(meatCow);
            ctx.assertTrue(!meatCow.isAlive(),"Native player attack did not kill the infected cow");
            var meat=ctx.getWorld().getEntitiesByClass(ItemEntity.class,meatCow.getBoundingBox().expand(3),item->item.getStack().isOf(ModItems.INFECTED_MEAT));
            ctx.assertTrue(!meat.isEmpty(),"Killed Tetanized cow did not drop infected meat");for(var drop:meat)pickup(drop);earned("strange_meat");
            var treated=treatmentCowFixture;ctx.assertTrue(treated!=null&&ctx.getWorld().getEntity(treated.getUuid())==treated,"Exact egg-spawned treatment cow was lost");treated.setAiDisabled(true);player.setPosition(treated.getPos().add(0,0,-1));
            var kit=new ItemStack(ExpansionItems.VETERINARY_KIT);player.setStackInHand(Hand.MAIN_HAND,kit);kit.useOnEntity(player,treated,Hand.MAIN_HAND);
            ctx.assertTrue(!MobVariants.permanent(treated),"Veterinary kit did not cure its actual egg-spawned animal");earned("clean_bill_of_health");
            var wither=witherFixture;ctx.assertTrue(wither!=null&&ctx.getWorld().getEntity(wither.getUuid())==wither,"Exact egg-spawned Wither was lost");
            wither.setAiDisabled(true);wither.setInvulTimer(0);player.setPosition(wither.getPos().add(0,0,-2));player.setStackInHand(Hand.MAIN_HAND,sword);for(int cooldown=0;cooldown<10;cooldown++)player.playerTick();
            ctx.assertTrue(player.getAttackCooldownProgress(1)>0.95F,"Native test sword attack cooldown did not recover");
            ctx.assertTrue(player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE)>600,"Creative test sword attribute was not equipped");
            player.attack(wither);ctx.assertTrue(!wither.isAlive(),"Native player attack did not kill the egg-spawned Tetanized Wither");earned("nice_one");
            place(ExpansionBlocks.MEDIC_CABINET.asItem(),new BlockPos(8,2,2));earned("clinic");
            trapAndSmithing();
            tradeAndDeliver();
        }
        private void gloveAndShield() {
            ctx.assertTrue(!isDone("glove_protection"),"Glove achievement was already complete before the actual contact test");
            player.setPosition(ctx.getAbsolute(new Vec3d(7.5,3,7.5)));player.setYaw(0);
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.PROTECTIVE_GLOVES));ModItems.PROTECTIVE_GLOVES.use(ctx.getWorld(),player,Hand.MAIN_HAND);
            ctx.assertTrue(GloveInventory.equipped(player).isOf(ModItems.PROTECTIVE_GLOVES),"Right-click did not equip gloves");
            attacker.equipStack(EquipmentSlot.MAINHAND,new ItemStack(ModItems.RUSTED_REBAR));
            for(int i=0;i<64&&!isDone("glove_protection");i++) {player.setHealth(player.getMaxHealth());player.timeUntilRegen=0;attacker.tryAttack(player);}
            if(isDone("infected"))earned("infected","Actual rusty zombie melee injury during the native glove-contact test.");
            earned("glove_protection");((GloveInventory)player).rustruin$gloves().clear();player.setHealth(player.getMaxHealth());player.timeUntilRegen=0;
            var shield=new ItemStack(Items.SHIELD);shield.addEnchantment(ExpansionItems.TETANUS_PROTECTION,1);player.setStackInHand(Hand.OFF_HAND,shield);player.setCurrentHand(Hand.OFF_HAND);
            var weapon=new ItemStack(Items.IRON_SWORD);weapon.addEnchantment(ModItems.TETANUS_ENCHANTMENT,2);attacker.equipStack(EquipmentSlot.MAINHAND,weapon);
            for(int i=0;i<6;i++)player.playerTick();player.setPosition(ctx.getAbsolute(new Vec3d(7.5,3,7.5)));player.setYaw(0);player.timeUntilRegen=0;
            float health=player.getHealth();attacker.tryAttack(player);
            ctx.assertTrue(player.isBlocking()&&player.getHealth()==health,"Enchanted zombie attack did not meet the raised shield");earned("shield_protection");
            player.stopUsingItem();player.setStackInHand(Hand.OFF_HAND,ItemStack.EMPTY);attacker.discard();
        }
        private void golem() {
            var golem=ctx.spawnEntity(EntityType.IRON_GOLEM,new Vec3d(7.5,3,10.5));golem.setAiDisabled(true);fixtures.add(golem);player.setPosition(golem.getPos().add(0,0,-2));player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUST,64));
            for(int i=0;i<64&&!MobVariants.permanent(golem);i++)UseEntityCallback.EVENT.invoker().interact(player,ctx.getWorld(),Hand.MAIN_HAND,golem,null);
            ctx.assertTrue(MobVariants.permanent(golem)&&golem.getMaxHealth()==200,"Real Rust interactions failed to produce a Tetanized Guardian");earned("tetanized_mob");earned("rusty_guardian");
        }
        private BlockHitResult hit(BlockPos floor) {return new BlockHitResult(Vec3d.ofCenter(floor).add(0,0.5,0),Direction.UP,floor,false);}
        private void place(Item item,BlockPos floor) {
            ctx.setBlockState(floor,Blocks.STONE);player.setPosition(Vec3d.ofCenter(ctx.getAbsolutePos(floor)).add(0,1,-2));player.setStackInHand(Hand.MAIN_HAND,new ItemStack(item));
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
            player.setPosition(medic.getPos().add(0,0,-1));player.setSneaking(false);player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);medic.interactMob(player,Hand.MAIN_HAND);
            ctx.assertTrue(player.currentScreenHandler instanceof MerchantScreenHandler,"Native medic interaction did not open a merchant screen");
            var menu=(MerchantScreenHandler)player.currentScreenHandler;var offer=medic.getOffers().get(0);menu.setRecipeIndex(0);menu.getSlot(0).setStack(offer.getAdjustedFirstBuyItem().copy());menu.getSlot(1).setStack(offer.getSecondBuyItem().copy());
            ctx.assertTrue(!menu.getSlot(2).getStack().isEmpty(),"Generated medic barter did not produce an output");menu.onSlotClick(2,0,SlotActionType.QUICK_MOVE,player);
            ctx.assertTrue(offer.getUses()==1,"Native medic result extraction did not consume stock");earned("medic");player.closeHandledScreen();medic.setCustomer(null);
            player.setSneaking(true);player.setStackInHand(Hand.MAIN_HAND,MedicSupplies.request(medic).copy());UseEntityCallback.EVENT.invoker().interact(player,ctx.getWorld(),Hand.MAIN_HAND,medic,null);player.setSneaking(false);earned("supply_run");
        }
        private void spear() {
            player.setPosition(ctx.getAbsolute(new Vec3d(1.5,3,6.5)));player.setYaw(0);player.setPitch(0);player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ExpansionItems.SPEAR));
            ExpansionItems.SPEAR.use(ctx.getWorld(),player,Hand.MAIN_HAND);ctx.assertTrue(spearTarget.getHealth()<10,"Spear thrust missed its live indexed target");earned("spear");
        }
        private void use(ItemStack stack) {
            player.stopUsingItem();player.setStackInHand(Hand.MAIN_HAND,stack);
            consuming=stack.getItem();usageBefore=player.getStatHandler().getStat(net.minecraft.stat.Stats.USED.getOrCreateStat(consuming));
            var result=stack.getItem().use(ctx.getWorld(),player,Hand.MAIN_HAND);
            ctx.assertTrue(result.getResult().isAccepted()&&player.isUsingItem(),"Real item-use timer did not start for "+stack);
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
            player.setPosition(Vec3d.ofCenter(beaconPos).add(0,1,-2));player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModBeacons.BLOCK));
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
            player.setPosition(Vec3d.ofCenter(beaconPos).add(0,2,0));player.setHealth(1);player.timeUntilRegen=0;player.setPitch(-90);player.setYaw(0);player.setVelocity(Vec3d.ZERO);
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(ModItems.RUSTED_NAIL,16));
            ModItems.RUSTED_NAIL.use(ctx.getWorld(),player,Hand.MAIN_HAND);
        }
        private void catchReturningNail(int tick) {
            if(!player.isAlive()) {earned("why_did_i_do_that");finish();return;}
            if(returningNail==null||!returningNail.isAlive())returningNail=ctx.getWorld().getEntitiesByClass(NailEntity.class,player.getBoundingBox().expand(80),nail->nail.getOwner()==player&&nail.wasThrownUpward()).stream().findFirst().orElse(null);
            if(returningNail!=null&&returningNail.getVelocity().y<0) {
                // Creative fixture positioning catches the naturally returning projectile;
                // no projectile damage/collision method is invoked by the test.
                player.setPosition(returningNail.getX(),beaconPos.getY()+2.5,returningNail.getZ());player.setVelocity(Vec3d.ZERO);
            }
            if((returningNail==null||!returningNail.isAlive())&&tick%20==0) {player.setPitch(-90);ModItems.RUSTED_NAIL.use(ctx.getWorld(),player,Hand.MAIN_HAND);}
            if(tick==850)ctx.assertTrue(false,"Actual upward rusty nails never returned to kill their owner");
        }
        private boolean isDone(String id) {
            var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id(id));return advancement!=null&&player.getAdvancementTracker().getProgress(advancement).isDone();
        }
        private void finish() {
            for(String id:ACTIONS.keySet())earned(id);
            ctx.assertTrue(completed.size()==28,"Expected all 28 existing mod advancements, found "+completed.size());
            var report=new LinkedHashMap<String,Object>();report.put("Result","PASS");report.put("ConnectedPlayers",1);report.put("ExistingAdvancements",28);report.put("AdditionalCoverage","New dragon and forging progression is verified by dedicated native and graphical encounter tests");report.put("Completed",completed);report.put("DirectCriterionGrants",0);report.put("NativeConsumptions",consumptionEvidence);report.put("Retired",Map.of("variant_egg","Creative-only Bad Egg removed at the owner's request; hidden ID preserved solely for old saves. Actual variant eggs were still exercised."));
            try {Files.writeString(Path.of("comprehensive-advancement-coverage.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}
            catch(java.io.IOException failure){throw new IllegalStateException("Cannot save exhaustive advancement evidence",failure);}
            cleanup();ctx.complete();
        }
        private void cleanup() {
            if(finished)return;finished=true;
            if(pvpChanged)ctx.getWorld().getServer().setPvpEnabled(originalPvp);
            if(medic!=null)medic.setCustomer(null);
            player.closeHandledScreen();player.getServer().getPlayerManager().remove(player);player.discard();
            fixtures.forEach(Entity::discard);
            if(returningNail!=null)returningNail.discard();
        }
    }
}
