package com.tetanusmod.test;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.*;
import com.tetanusmod.entity.*;
import com.tetanusmod.expansion.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.test.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class DespawnAuditGameTests implements FabricGameTest {
    private MobEntity variant(TestContext ctx,boolean rabid){
        var mob=EntityType.ZOMBIE.create(ctx.getWorld());
        if(rabid)Rabies.makeRabid(mob);else MobVariants.makePermanent(mob);
        return mob;
    }
    private NbtCompound legacy(MobEntity mob){
        var nbt=mob.writeNbt(new NbtCompound());nbt.remove(VariantPersistence.RULES_KEY);nbt.putBoolean("PersistenceRequired",true);return nbt;
    }
    private MobEntity reload(TestContext ctx,NbtCompound nbt){var copy=EntityType.ZOMBIE.create(ctx.getWorld());copy.readNbt(nbt);return copy;}
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void freshVariantsUseNormalMobCapAndDespawnRules(TestContext ctx){
        for(boolean rabid:new boolean[]{false,true}){
            var mob=variant(ctx,rabid);ctx.assertFalse(mob.isPersistent()||mob.cannotDespawn(),"Mutation exempts ordinary zombies from the mob cap/despawn rules");
            ctx.assertTrue(MobVariants.permanent(mob)||Rabies.rabid(mob),"Mutation did not apply");
        }ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void distantVariantsActuallyDespawn(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();player.setPosition(ctx.getAbsolute(new Vec3d(1,4,1)));
        try{for(boolean rabid:new boolean[]{false,true}){
            var mob=variant(ctx,rabid);mob.setPosition(player.getPos().add(10000,0,10000));mob.checkDespawn();
            ctx.assertTrue(mob.isRemoved(),"Distant variant ignored vanilla hard despawn");
        }}finally{player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void nearbyVariantsAreNotRemovedByRepair(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();player.setPosition(ctx.getAbsolute(new Vec3d(1,4,1)));
        try{for(boolean rabid:new boolean[]{false,true}){var mob=variant(ctx,rabid);mob.setPosition(player.getPos().add(1,0,1));mob.checkDespawn();ctx.assertFalse(mob.isRemoved(),"Nearby living variant despawned");}}finally{player.discard();}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void legacyForcedPersistenceIsRepairedOnLoad(TestContext ctx){
        for(boolean rabid:new boolean[]{false,true}){var copy=reload(ctx,legacy(variant(ctx,rabid)));ctx.assertFalse(copy.isPersistent(),"Legacy forced persistence survived loading");ctx.assertTrue(rabid?Rabies.rabid(copy):MobVariants.permanent(copy),"Repair lost mutation");ctx.assertTrue(copy.getMaxHealth()==(rabid?50:30),"Reload lost variant attributes");}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void intentionalPersistenceSurvivesNewSaveAndReload(TestContext ctx){
        for(boolean rabid:new boolean[]{false,true}){var mob=variant(ctx,rabid);mob.setPersistent();var nbt=mob.writeNbt(new NbtCompound());ctx.assertTrue(nbt.getInt(VariantPersistence.RULES_KEY)==1,"Missing repair version");ctx.assertTrue(reload(ctx,nbt).isPersistent(),"Repair cleared deliberate modern persistence");}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void namedLegacyVariantsRemainPersistent(TestContext ctx){
        for(boolean rabid:new boolean[]{false,true}){var mob=variant(ctx,rabid);mob.setCustomName(Text.literal("Keep me"));ctx.assertTrue(reload(ctx,legacy(mob)).isPersistent(),"Named variant lost persistence");}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void pickedUpEquipmentKeepsLegacyMobPersistent(TestContext ctx){
        var mob=variant(ctx,false);mob.equipStack(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));mob.setEquipmentDropChance(EquipmentSlot.MAINHAND,2);
        ctx.assertTrue(reload(ctx,legacy(mob)).isPersistent(),"Picked-up equipment protection lost");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void ownedLeashedBucketedAndPassengerLegacyDataIsProtected(TestContext ctx){
        for(String key:new String[]{"Tame","Owner","OwnerUUID","FromBucket","NoAI","Leash","Passengers"}){
            var nbt=legacy(variant(ctx,false));
            if(key.equals("Tame")||key.equals("FromBucket")||key.equals("NoAI"))nbt.putBoolean(key,true);
            else if(key.equals("Owner"))nbt.putUuid(key,java.util.UUID.randomUUID());
            else if(key.equals("OwnerUUID"))nbt.putString(key,java.util.UUID.randomUUID().toString());
            else if(key.equals("Passengers"))nbt.put(key,new NbtList());else nbt.put(key,new NbtCompound());
            ctx.assertFalse(VariantPersistence.repairLegacy(variant(ctx,false),nbt),"Player-kept mob data not protected: "+key);
        }ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void ordinaryPersistentMobsAreNeverMigrated(TestContext ctx){
        var mob=EntityType.ZOMBIE.create(ctx.getWorld());mob.setPersistent();ctx.assertTrue(reload(ctx,legacy(mob)).isPersistent(),"Ordinary mob persistence changed");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void naturalAnimalRulesAndTamingRemainIntact(TestContext ctx){
        var cow=EntityType.COW.create(ctx.getWorld());MobVariants.makePermanent(cow);ctx.assertFalse(cow.isPersistent(),"Mutation forced cow persistence");ctx.assertFalse(cow.canImmediatelyDespawn(1000000),"Mutation changed native livestock distance rules");
        var wolf=EntityType.WOLF.create(ctx.getWorld());wolf.setTamed(true);MobVariants.makePermanent(wolf);ctx.assertFalse(wolf.canImmediatelyDespawn(1000000),"Tamed pet can distance-despawn");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void nativeConversionsDoNotReintroducePersistence(TestContext ctx){
        for(boolean rabid:new boolean[]{false,true}){var mob=variant(ctx,rabid);var copy=mob.convertTo(EntityType.DROWNED,true);ctx.assertTrue(copy!=null&&!copy.isPersistent(),"Conversion forced persistence again");}ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void weakExposureCannotDowngradeStrongInfectionOrClearLockjaw(TestContext ctx){
        for(int amplifier:new int[]{0,1})for(int remaining:new int[]{10000,400}){
            var player=ctx.createMockSurvivalPlayer();Infection.applyDurationChecked(player,null,remaining,amplifier);
            var state=(InfectionState)player;state.tetanus$setInfection(10000-remaining,10000);state.tetanus$setLockjaw(remaining);player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(TetanusMod.LOCKJAW,remaining));
            CombatInfection.expose(player,null,0,true);
            var effect=player.getStatusEffect(TetanusMod.TETANUS);ctx.assertTrue(effect.getDuration()==remaining&&effect.getAmplifier()==amplifier,"Weak hit changed existing Tetanus strength/duration");ctx.assertTrue(state.tetanus$getElapsed()==10000-remaining&&state.tetanus$getLockjaw()==remaining&&player.hasStatusEffect(TetanusMod.LOCKJAW),"Weak hit restarted or cleared Lockjaw near recovery");
        }ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void combinedArrowsCannotCompoundInfectionDuration(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();var arrow=new TetanizedArrowEntity(ctx.getWorld(),player);((CombatInfection.RangedCarrier)(Object)arrow).rustruin$weaponLevel(2);
        for(int i=0;i<100;i++)CombatInfection.rangedHit(player,ctx.getWorld().getDamageSources().arrow(arrow,player));
        var effect=player.getStatusEffect(TetanusMod.TETANUS);ctx.assertTrue(effect.getDuration()<=TetanusMod.CONFIG.maxDurationSeconds*25,"Repeated arrows multiplied existing infection without limit");ctx.assertTrue(player.getStatusEffect(ExpansionEffects.DISORIENTED).getDuration()==effect.getDuration(),"Strong symptoms do not match infection");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void longConfiguredAndInfiniteInfectionsArePreserved(TestContext ctx){
        var player=ctx.createMockSurvivalPlayer();Infection.applyDurationChecked(player,null,72000,0);ctx.assertTrue(player.getStatusEffect(TetanusMod.TETANUS).getDuration()==72000,"Valid one-hour duration was silently capped at fifteen minutes");
        InfectionProtection.checked(player,()->player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(TetanusMod.TETANUS,-1,1)));CombatInfection.expose(player,null,0,true);ctx.assertTrue(player.getStatusEffect(TetanusMod.TETANUS).getDuration()==-1,"Finite hit cured an infinite infection");ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void spasmHandlerLeavesMalformedMovementToVanillaValidation(TestContext ctx){
        var player=ctx.createMockCreativeServerPlayerInWorld();((InfectionState)player).tetanus$setFreeze(4);
        final boolean[] rejected={false};
        var handler=new net.minecraft.server.network.ServerPlayNetworkHandler(ctx.getWorld().getServer(),new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND),player){
            @Override public void disconnect(Text reason){rejected[0]=true;}
        };
        for(float invalid:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}){
            rejected[0]=false;handler.onPlayerMove(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(player.getX()+1,player.getY(),player.getZ(),invalid,0,true));
            ctx.assertTrue(rejected[0]&&Float.isFinite(player.getYaw())&&Float.isFinite(player.getPitch()),"Spasm bypassed invalid-movement validation");
        }
        player.discard();ctx.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE)
    public void tetanizeCommandDoesNotCountAnUnchangedRabidMob(TestContext ctx)throws Exception{
        var mob=EntityType.ZOMBIE.create(ctx.getWorld());Rabies.makeRabid(mob);mob.setPosition(ctx.getAbsolute(new Vec3d(3,4,3)));ctx.getWorld().spawnEntity(mob);
        var source=ctx.getWorld().getServer().getCommandSource().withWorld(ctx.getWorld()).withLevel(2);
        int count=ctx.getWorld().getServer().getCommandManager().getDispatcher().execute("rustandruin tetanize "+mob.getUuidAsString(),source);
        ctx.assertTrue(count==0&&Rabies.rabid(mob)&&!MobVariants.permanent(mob),"Command reported a mutation it refused to perform");mob.discard();ctx.complete();
    }
}
