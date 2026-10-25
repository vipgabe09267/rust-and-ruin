package com.tetanusmod.test;

import com.mojang.authlib.GameProfile;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.config.ModConfig;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.entity.NailEntity;
import com.tetanusmod.item.NailItem;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Item-use launches and normal server projectile ticks; no direct hit/exposure/criterion calls. */
public class NailGolemRegressionGameTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="nail_golem_regression")
    public void dedicatedGolemChanceIsBoundedAndKeepsOtherMobRates(TestContext ctx){
        var defaults=ModConfig.fromJson("{\"balanceRevision\":5}");
        ctx.assertTrue(defaults.golemConversionChance==0.18F&&defaults.mobConversionChance==0.01F&&defaults.animalConversionChance==0.02F,"Older configs lost the new golem default or existing rates");
        ctx.assertTrue(ModConfig.fromJson("{\"balanceRevision\":5,\"golemConversionChance\":0.07}").golemConversionChance==0.07F,"Custom golem chance was overwritten");
        ctx.assertTrue(ModConfig.fromJson("{\"balanceRevision\":5,\"golemConversionChance\":-1}").golemConversionChance==0,"Negative golem chance was not bounded");
        ctx.assertTrue(ModConfig.fromJson("{\"balanceRevision\":5,\"golemConversionChance\":2}").golemConversionChance==1,"Golem chance above one was not bounded");
        ctx.assertTrue(ModConfig.fromJson("{\"balanceRevision\":5,\"golemConversionChance\":1e400}").golemConversionChance==0.18F,"Non-finite golem chance did not use its safe default");
        float oldMob=TetanusMod.CONFIG.mobConversionChance,oldGolem=TetanusMod.CONFIG.golemConversionChance,oldAnimal=TetanusMod.CONFIG.animalConversionChance;
        try{
            TetanusMod.CONFIG.mobConversionChance=0.01F;TetanusMod.CONFIG.golemConversionChance=0.18F;TetanusMod.CONFIG.animalConversionChance=0.02F;
            var golem=Objects.requireNonNull(EntityType.IRON_GOLEM.create(ctx.getWorld()));
            for(boolean created:new boolean[]{false,true}){
                golem.setPlayerCreated(created);
                ctx.assertTrue(MobVariants.eligibleRoll(golem,0.1799F)&&!MobVariants.eligibleRoll(golem,0.18F),"Golem 18% boundary differs by creator flag");
            }
            var zombie=Objects.requireNonNull(EntityType.ZOMBIE.create(ctx.getWorld()));
            var cow=Objects.requireNonNull(EntityType.COW.create(ctx.getWorld()));
            ctx.assertTrue(MobVariants.eligibleRoll(zombie,0.0099F)&&!MobVariants.eligibleRoll(zombie,0.01F),"Golem change altered other mob rates");
            ctx.assertTrue(MobVariants.eligibleRoll(cow,0.0199F)&&!MobVariants.eligibleRoll(cow,0.02F),"Golem change altered animal rates");
            TetanusMod.CONFIG.mobConversionChance=0;TetanusMod.CONFIG.golemConversionChance=1;
            ctx.assertTrue(!MobVariants.eligibleRoll(golem,0),"General projectile-mutation disable switch was bypassed");
            TetanusMod.CONFIG.mobConversionChance=1;TetanusMod.CONFIG.golemConversionChance=0;
            ctx.assertTrue(!MobVariants.eligibleRoll(golem,0),"Dedicated zero golem chance was bypassed");
        }finally{TetanusMod.CONFIG.mobConversionChance=oldMob;TetanusMod.CONFIG.golemConversionChance=oldGolem;TetanusMod.CONFIG.animalConversionChance=oldAnimal;}
        ctx.complete();
    }

    @GameTest(templateName=EMPTY_STRUCTURE,batchId="nail_golem_regression",tickLimit=160)
    public void realFlyingNailsInfectAndConvertBothGolemOrigins(TestContext ctx){
        var journey=new ProjectileJourney(ctx);
        // Schedule before ticking starts; do not mutate the native GameTest schedule from a callback.
        for(int index=0;index<ProjectileJourney.CASES.size();index++){
            final int current=index;
            ctx.runAtTick(1+index*12,()->journey.guard(()->journey.begin(current)));
            ctx.runAtTick(9+index*12,()->journey.guard(journey::verify));
        }
    }

    private record Scenario(Item item,boolean playerCreated,boolean conversion,boolean invulnerable){
        boolean rusty(){return ((NailItem)item).isRusty();}
        String label(){return item+" / playerCreated="+playerCreated+" / convert="+conversion+" / invulnerable="+invulnerable;}
    }
    private static final class ProjectileJourney {
        private static final List<Scenario> CASES=List.of(
                new Scenario(ModItems.RUSTED_NAIL,false,false,false),
                new Scenario(ModItems.RUSTED_BENT_NAIL,false,false,false),
                new Scenario(ModItems.RUSTED_NAIL,true,false,false),
                new Scenario(ModItems.RUSTED_BENT_NAIL,true,false,false),
                new Scenario(ModItems.RUSTED_NAIL,false,true,false),
                new Scenario(ModItems.RUSTED_BENT_NAIL,false,true,false),
                new Scenario(ModItems.RUSTED_NAIL,true,true,false),
                new Scenario(ModItems.RUSTED_BENT_NAIL,true,true,false),
                new Scenario(ModItems.NAIL,false,false,false),
                new Scenario(ModItems.BENT_NAIL,true,false,false),
                new Scenario(ModItems.RUSTED_NAIL,false,false,true));
        private final TestContext ctx;
        private final float oldMob,oldGolem,oldDamage;
        private ServerPlayerEntity player;
        private IronGolemEntity golem;
        private NailEntity nail;
        private Scenario scenario;
        private Vec3d launch;
        private int completed;
        private boolean finished;

        ProjectileJourney(TestContext ctx){
            this.ctx=ctx;oldMob=TetanusMod.CONFIG.mobConversionChance;oldGolem=TetanusMod.CONFIG.golemConversionChance;oldDamage=TetanusMod.CONFIG.nailDamage;
        }
        void guard(Runnable action){
            if(finished)return;
            try{action.run();}
            catch(RuntimeException|Error failure){finished=true;try{cleanupScene();}catch(RuntimeException|Error cleanup){failure.addSuppressed(cleanup);}throw failure;}
        }
        void begin(int index){
            ctx.assertTrue(player==null&&golem==null&&nail==null,"Previous native projectile scene was not cleaned up");
            scenario=CASES.get(index);
            // Controlled rates isolate hit delivery from chance. The disabled cases prove Tetanus
            // still applies without permanent mutation; clean/immune cases must never expose.
            TetanusMod.CONFIG.mobConversionChance=scenario.conversion()||!scenario.rusty()?1:0;
            TetanusMod.CONFIG.golemConversionChance=1;TetanusMod.CONFIG.nailDamage=2;
            var world=ctx.getWorld();
            player=new ServerPlayerEntity(world.getServer(),world,new GameProfile(UUID.randomUUID(),"nail-golem-"+index));
            world.getServer().getPlayerManager().onPlayerConnect(new ClientConnection(NetworkSide.SERVERBOUND),player);
            player.changeGameMode(GameMode.SURVIVAL);player.getAbilities().creativeMode=false;player.getAbilities().invulnerable=false;
            ((com.tetanusmod.test.mixin.ServerPlayerTestAccessor)player).rustruin$joinInvulnerability(0);
            player.setNoGravity(true);player.setPosition(ctx.getAbsolute(new Vec3d(1.5,3,0.5)));player.setVelocity(Vec3d.ZERO);player.setYaw(0);player.setPitch(0);
            ctx.assertTrue(!done("tetanized_mob")&&!done("rusty_guardian"),"Native projectile player did not start with fresh advancement progress");
            golem=Objects.requireNonNull(EntityType.IRON_GOLEM.create(world));golem.setPlayerCreated(scenario.playerCreated());golem.setAiDisabled(true);golem.setNoGravity(true);golem.setInvulnerable(scenario.invulnerable());
            golem.setPosition(ctx.getAbsolute(new Vec3d(1.5,3,2.5)));
            ctx.assertTrue(world.spawnEntity(golem),"Native golem fixture could not spawn");
            player.setStackInHand(Hand.MAIN_HAND,new ItemStack(scenario.item(),2));
            int used=player.getStatHandler().getStat(Stats.USED.getOrCreateStat(scenario.item()));
            var result=scenario.item().use(world,player,Hand.MAIN_HAND);
            ctx.assertTrue(result.getResult().isAccepted()&&player.getMainHandStack().getCount()==1,"Actual Survival item use did not consume exactly one nail: "+scenario.label());
            ctx.assertTrue(player.getStatHandler().getStat(Stats.USED.getOrCreateStat(scenario.item()))==used+1,"Actual nail use statistic did not increase once");
            var projectiles=world.getEntitiesByClass(NailEntity.class,player.getBoundingBox().expand(4),projectile->projectile.isAlive()&&projectile.getOwner()==player);
            ctx.assertTrue(projectiles.size()==1,"Item use did not create exactly one owned flying nail");
            nail=projectiles.get(0);launch=nail.getPos();
            ctx.assertTrue(nail.getStack().isOf(scenario.item())&&nail.getVelocity().z>1,"Item use created the wrong projectile or launch direction");
        }
        void verify(){
            ctx.assertTrue(nail.isRemoved()&&nail.age>0&&nail.getPos().distanceTo(launch)>1,"Thrown nail did not travel and finish a native collision: "+scenario.label());
            ctx.assertTrue(golem.getBoundingBox().expand(0.5).contains(nail.getPos()),"Native nail collision ended away from the intended golem: "+scenario.label());
            if(!scenario.invulnerable()){
                var damage=golem.getRecentDamageSource();
                ctx.assertTrue(damage!=null&&damage.getSource()==nail&&damage.getAttacker()==player,"Native impact did not damage the golem with this exact owned projectile: "+scenario.label());
            }
            boolean infected=scenario.rusty()&&!scenario.invulnerable();
            ctx.assertTrue(golem.hasStatusEffect(TetanusMod.TETANUS)==infected,"Native golem hit produced the wrong disease state: "+scenario.label());
            boolean converted=infected&&scenario.conversion();
            ctx.assertTrue(MobVariants.permanent(golem)==converted&&golem.getMaxHealth()==(converted?200:100),"Native golem hit produced the wrong permanent variant/health: "+scenario.label());
            ctx.assertTrue(done("tetanized_mob")==converted&&done("rusty_guardian")==converted,"Native projectile conversion did not causally award both mob and specific Guardian advancements: "+scenario.label());
            if(converted)ctx.assertTrue(golem.getStatusEffect(TetanusMod.TETANUS).getDuration()==-1,"Converted golem did not keep permanent Tetanus");
            else{
                float expected=scenario.invulnerable()?100:100-(((NailItem)scenario.item()).isBent()?1:2);
                ctx.assertTrue(golem.getHealth()==expected,"Native nail dealt the wrong golem damage: "+scenario.label());
            }
            org.slf4j.LoggerFactory.getLogger("tetanusmod").info("NAIL_GOLEM_NATIVE_PASS: {} exactProjectile={} nativeTicks={} disease={} permanent={} guardian={}",scenario.label(),nail.getUuid(),nail.age,infected,converted,done("rusty_guardian"));
            completed++;cleanupScene();
            if(completed==CASES.size()){finished=true;ctx.complete();}
        }
        private boolean done(String id){
            var advancement=player.getServer().getAdvancementLoader().get(TetanusMod.id(id));
            ctx.assertTrue(advancement!=null,"Missing native advancement "+id);
            return player.getAdvancementTracker().getProgress(advancement).isDone();
        }
        private void cleanupScene(){
            TetanusMod.CONFIG.mobConversionChance=oldMob;TetanusMod.CONFIG.golemConversionChance=oldGolem;TetanusMod.CONFIG.nailDamage=oldDamage;
            if(nail!=null){nail.discard();nail=null;}
            if(golem!=null){golem.discard();golem=null;}
            if(player!=null){player.closeHandledScreen();player.getServer().getPlayerManager().remove(player);player.discard();player=null;}
        }
    }
}